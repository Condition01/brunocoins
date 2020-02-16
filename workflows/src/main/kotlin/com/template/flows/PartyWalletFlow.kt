//package com.template.flows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.template.states.BrunoCoinState
//import net.corda.core.contracts.StateAndRef
//import net.corda.core.contracts.StateRef
//import net.corda.core.crypto.SecureHash
//import net.corda.core.flows.*
//import net.corda.core.identity.Party
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.ProgressTracker
//import net.corda.core.utilities.unwrap
//import javax.swing.plaf.nimbus.State
//
//object PartyWalletFlow{
//    @InitiatingFlow
//    @StartableByRPC
//    class MoneyStateRequestFlow(val otherParty : Party) : FlowLogic<StateAndRef<BrunoCoinState>?>() {
//        companion object {
//            object REQUESTING_PARTNER_INPUT : ProgressTracker.Step("Requesting to partner to put inputs in the transaction")
//            object SENDING_BACK_TO_TRANSCTION : ProgressTracker.Step("Sending back to the transaction")
//
//
//            fun tracker() = ProgressTracker(
//                    REQUESTING_PARTNER_INPUT,
//                    SENDING_BACK_TO_TRANSCTION
//            )
//        }
//
//        override val progressTracker = tracker()
//
//        @Suspendable
//        override fun call(): StateAndRef<BrunoCoinState>? {
//            val otherPartySession = initiateFlow(otherParty)
//            progressTracker.currentStep = REQUESTING_PARTNER_INPUT
//            val otherPartyWallet = otherPartySession.receive<StateAndRef<BrunoCoinState>>().unwrap{ it }
//            progressTracker.currentStep = SENDING_BACK_TO_TRANSCTION
//
//            if(otherPartyWallet.state.data){
//                return otherPartyWallet
//            }else{
//                return  null
//            }
//
//        }
//    }
//
//    @InitiatedBy(MoneyStateRequestFlow::class)
//    class MoneyStateReponseFlow(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
//        override fun call() {
//            val listMoneyStateAndRef = serviceHub.vaultService.queryBy(BrunoCoinState::class.java).states
//
//            if(listMoneyStateAndRef.isNotEmpty()){
//                otherPartySession.send(listMoneyStateAndRef.single())
//            }else{
//
//            }
//
//        }
//    }
//}