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

            val otherPartySession = initiateFlow(newOwner)

//            val otherP = otherPartySession.receive<StateAndRef<BrunoCoinState>>().unwrap{ it }

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            progressTracker.currentStep = GENERATING_TRANSACTION

            var txBuilder = buildTransaction(listMoneyStateAndRef, notary)
//            txBuilder.addInputState(otherP)

            progressTracker.currentStep = VERIFYING_TRANSACTION

            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION

            val signedTransaction = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = GETTING_OTHER_SIGNATURES


            progressTracker.currentStep = FINALISING_TRANSACTION


            otherPartySession.send(signedTransaction)
            return subFlow(FinalityFlow(signedTransaction, setOf(otherPartySession)))
        }

        private fun buildTransaction(listMoneyStateAndRef: List<StateAndRef<BrunoCoinState>>, notary: Party): TransactionBuilder {
            val bCoinOutPutState = BrunoCoinState(amount = amount, owner = newOwner)
            val bCoinTransferOutputState = BrunoCoinTransferState(
                    owner = serviceHub.myInfo.legalIdentities.first(),
                    newOwner = newOwner,
                    amount = amount
            )
            val txCommand = Command(BrunoCoinContract.Commands.Transfer(), serviceHub.myInfo.legalIdentities.first().owningKey)
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(bCoinOutPutState)
                    .addOutputState(bCoinTransferOutputState, BrunoCoinContract.ID)
                    .addCommand(txCommand)
            if (listMoneyStateAndRef.isNotEmpty()) {
                val ownerBCoinState = listMoneyStateAndRef.single().state.data
                txBuilder.addInputState(listMoneyStateAndRef.single())
                txBuilder.addOutputState(
                        BrunoCoinState(owner = ownerBCoinState.owner, amount = ownerBCoinState.amount - amount))
            }
            return txBuilder
        }

    }

    @InitiatedBy(CoinTransferFlow::class)
    class TransferResponderFlow(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        companion object {

            object VERIFYING_TRANSACTION : ProgressTracker.Step("Counterparty verifying contract constraints.")
            object  FINALISING_TRANSACTION :  ProgressTracker.Step("Counterparty finalising the transaction")

            fun tracker() = ProgressTracker(
                    VERIFYING_TRANSACTION,
                    FINALISING_TRANSACTION
            )
        }
        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

//
//            val listMoneyStateAndRef = serviceHub.vaultService.queryBy(BrunoCoinState::class.java).states
//
//            otherPartySession.send(listMoneyStateAndRef.single())

            fun verifyTx(sgdTx : SignedTransaction) = requireThat {
                "O output precisa ser do tipo BrunoCoinState"  using (sgdTx.tx.outputStates[0] is BrunoCoinState)

                val bCoinState = sgdTx.tx.outputStates[0] as BrunoCoinState

                "O output2 precisa ser do tipo BrunoCoinTransferState" using (sgdTx.tx.outputStates[1] is BrunoCoinTransferState)

                val bCoinTransferState = sgdTx.tx.outputStates[1] as BrunoCoinTransferState

                "Os valores propostos na transação deve ser maior que 0" using (bCoinState.amount > 0
                        && bCoinTransferState.amount > 0)

                "Os valores propostos na transação e o valor enviados devem ser iguais" using (bCoinState.amount == bCoinTransferState.amount)
            }


            val sgdTx = otherPartySession.receive<SignedTransaction>().unwrap{ it }

            progressTracker.currentStep = VERIFYING_TRANSACTION

            verifyTx(sgdTx)

            progressTracker.currentStep = FINALISING_TRANSACTION

            return subFlow(ReceiveFinalityFlow(otherPartySession/*, expectedTxId = txId*/))
        }
    }

//    @InitiatedBy(CoinTransferFlow::class)
//    class MoneyReferenceFlow(val otherPartySession: FlowSession) : FlowLogic<TransactionBuilder>() {
//        override fun call(): TransactionBuilder {
//            val listMoneyStateAndRef = serviceHub.vaultService.queryBy(BrunoCoinState::class.java).states
//            val txBuilder = otherPartySession.receive<TransactionBuilder>().unwrap{ it }
//            return if(listMoneyStateAndRef.isNotEmpty()){
//                txBuilder.addInputState(listMoneyStateAndRef.single())
//                txBuilder
//            }else{
//                txBuilder
//            }
//        }
//    }
}