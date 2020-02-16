package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BrunoCoinContract
import com.template.states.BrunoCoinState
import com.template.states.BrunoCoinTransferState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.lang.IllegalArgumentException

object TransferFlow{
    @InitiatingFlow
    @StartableByRPC
    class CoinTransferFlow(var amount: Double, val newOwner: Party) : FlowLogic<SignedTransaction>() {
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new BrunoCoin.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GETTING_OTHER_SIGNATURES : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GETTING_OTHER_SIGNATURES,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            val listMoneyStateAndRef = serviceHub.vaultService.queryBy(BrunoCoinState::class.java).states
//            val newOwnerNodeInfo = serviceHub.networkMapCache.getNodeByLegalIdentity(newOwner)

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            progressTracker.currentStep = GENERATING_TRANSACTION
            val txBuilder = buildTransaction(listMoneyStateAndRef, notary)

            progressTracker.currentStep = VERIFYING_TRANSACTION

            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION

            val signedTransaction = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = FINALISING_TRANSACTION

            val otherPartySession = initiateFlow(newOwner)
            otherPartySession.send(signedTransaction)
            return subFlow(FinalityFlow(signedTransaction, setOf(otherPartySession)))
        }

        private fun buildTransaction(listMoneyStateAndRef: List<StateAndRef<BrunoCoinState>>, notary: Party): TransactionBuilder {
            val bCoinTransferOutputState = BrunoCoinTransferState(
                    owner = serviceHub.myInfo.legalIdentities.first(),
                    newOwner = newOwner,
                    amount = amount
            )
            val txCommand = Command(BrunoCoinContract.Commands.Transfer(), serviceHub.myInfo.legalIdentities.first().owningKey)
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(bCoinTransferOutputState, BrunoCoinContract.ID)
                    .addCommand(txCommand)

            if (listMoneyStateAndRef.isEmpty()){
                throw IllegalArgumentException("Você não tem saldo valido nesta conta para realizar uma transfêrencia")
            }

            val ownerBCoinState = listMoneyStateAndRef.single().state.data
            txBuilder.addInputState(listMoneyStateAndRef.single())
            txBuilder.addOutputState(
                    BrunoCoinState(owner = ownerBCoinState.owner, amount = ownerBCoinState.amount - amount))

            return txBuilder
        }

    }

    @InitiatedBy(CoinTransferFlow::class)
    class TransferResponderFlow(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        companion object {

            object VERIFYING_TRANSACTION : ProgressTracker.Step("Counterparty verifying contract constraints.")
            object  FINALISING_TRANSACTION :  ProgressTracker.Step("Counterparty finalising the transaction")
            object ISSUING_THE_VALUES : ProgressTracker.Step("Issuing the values")

            fun tracker() = ProgressTracker(
                    VERIFYING_TRANSACTION,
                    FINALISING_TRANSACTION,
                    ISSUING_THE_VALUES
            )
        }
        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            fun verifyTx(sgdTx : SignedTransaction) = requireThat {
                val bCoinTransferState = sgdTx.tx.outputStates[0] as BrunoCoinTransferState

                "Os valores propostos na transação deve ser maior que 0" using (bCoinTransferState.amount > 0)
            }

            val sgdTx = otherPartySession.receive<SignedTransaction>().unwrap{ it }

            progressTracker.currentStep = VERIFYING_TRANSACTION

            verifyTx(sgdTx)

            progressTracker.currentStep = ISSUING_THE_VALUES

            val bCoinTransferState = sgdTx.tx.outputStates[1] as BrunoCoinTransferState

//            subFlow(CoinIssueFlow(bCoinTransferState.amount))

            progressTracker.currentStep = FINALISING_TRANSACTION


            return subFlow(ReceiveFinalityFlow(otherPartySession/*, expectedTxId = txId*/))
        }
    }
}