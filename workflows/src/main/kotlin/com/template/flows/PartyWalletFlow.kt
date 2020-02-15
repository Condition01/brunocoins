//package com.template.flows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.contracts.BrunoCoinContract
//import com.template.states.BrunoCoinState
//import com.template.states.BrunoCoinTransferState
//import net.corda.core.contracts.Command
//import net.corda.core.contracts.StateAndRef
//import net.corda.core.contracts.requireThat
//import net.corda.core.flows.*
//import net.corda.core.identity.Party
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.ProgressTracker
//import net.corda.core.utilities.unwrap
//
//object PartyWalletFlow{
//    @InitiatingFlow
//    @StartableByRPC
//    class CoinTransferFlow(var amount: Double, val newOwner: Party) : FlowLogic<List<StateAndRef<BrunoCoinState>>>() {
//        companion object {
//            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new BrunoCoin.")
//            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
//            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
//            object GETTING_OTHER_SIGNATURES : ProgressTracker.Step("Gathering the counterparty's signature.") {
//                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
//            }
//
//            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
//                override fun childProgressTracker() = FinalityFlow.tracker()
//            }
//
//            fun tracker() = ProgressTracker(
//                    GENERATING_TRANSACTION,
//                    VERIFYING_TRANSACTION,
//                    SIGNING_TRANSACTION,
//                    GETTING_OTHER_SIGNATURES,
//                    FINALISING_TRANSACTION
//            )
//        }
//
//        override val progressTracker = tracker()
//
//        @Suspendable
//        override fun call(): List<StateAndRef<BrunoCoinState>> {
//            return BrunoCoinState()
//        }
//    }
//
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
//}