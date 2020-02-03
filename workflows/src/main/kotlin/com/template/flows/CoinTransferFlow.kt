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


        override fun call(): SignedTransaction {
            val listMoneyStateAndRef = serviceHub.vaultService.queryBy(BrunoCoinState::class.java).states

            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            progressTracker.currentStep = GENERATING_TRANSACTION
            val txBuilder = buildTransaction(listMoneyStateAndRef, notary)

            progressTracker.currentStep = VERIFYING_TRANSACTION

            txBuilder.verify(serviceHub)

            progressTracker.currentStep = SIGNING_TRANSACTION

            val partialSignedTransaction = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = GETTING_OTHER_SIGNATURES

            val otherPartySession = initiateFlow(newOwner)

            val fullySignedTransaction = subFlow(
                    CollectSignaturesFlow(
                            partialSignedTransaction,
                            setOf(otherPartySession)
//                            GETTING_OTHER_SIGNATURES.childProgressTracker()
                    ))

            progressTracker.currentStep = FINALISING_TRANSACTION

            return subFlow(FinalityFlow(fullySignedTransaction, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))
        }

        private fun buildTransaction(listMoneyStateAndRef: List<StateAndRef<BrunoCoinState>>, notary: Party): TransactionBuilder {
            val bCoinOutPutState = BrunoCoinState(amount = amount, owner = newOwner)
            val bCoinTransferOutputState = BrunoCoinTransferState(
                    owner = serviceHub.myInfo.legalIdentities.first(),
                    newOwner = newOwner,
                    amount = amount
            )
            val txCommand = Command(BrunoCoinContract.Commands.Transfer(), bCoinTransferOutputState.participants.map { it.owningKey })
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

//    private fun gettingStatesToPay() : MutableCollection<StateAndRef<BrunoCoinState>>{
//        var listOfMoneyStatesFiltered = mutableListOf<StateAndRef<BrunoCoinState>>()
//        var totalAmount = 0.00
//        val unconsumedCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
//        val listOfMoneyStates = serviceHub.vaultService.queryBy<BrunoCoinState>(unconsumedCriteria).states.forEach {
//            totalAmount += it.state.data.amount
//            listOfMoneyStatesFiltered.add(it)
//            if (totalAmount >= amount) return@forEach
//        }
//        return listOfMoneyStatesFiltered
//    }

    }

    @InitiatedBy(CoinTransferFlow::class)
    class TransferResponderFlow(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        companion object {
            object SIGNING_TRANSACTION : ProgressTracker.Step("CounterParty singing the transaction..")

            fun tracker() = ProgressTracker(
                    SIGNING_TRANSACTION
            )
        }
        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            progressTracker.currentStep = SIGNING_TRANSACTION
            val signedTransactionFlow = object : SignTransactionFlow(otherPartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
//                    val output = stx.tx.outputs[0].data
//                    "This must be a BrunoCoin transaction" using (output is BrunoCoinState)
//
//                    val bCoinOutput = output as BrunoCoinState
//                    "The value of the transaction must be positive" using (bCoinOutput.amount > 0)
                }
            }
            val txId = subFlow(signedTransactionFlow).id
            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
        }
    }
}