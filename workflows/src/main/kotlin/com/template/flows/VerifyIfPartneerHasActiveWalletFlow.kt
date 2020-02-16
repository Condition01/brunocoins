package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.BrunoCoinState
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

object VerifyIfPartnerHasActiveWalletFlow{
    @InitiatingFlow
    @StartableByRPC
    class VerifyPartnerWalletFlowRequest(val otherParty : Party) : FlowLogic<Boolean>() {
        companion object {
            object REQUESTING_PARTNER_INPUT : ProgressTracker.Step("Requesting to partner to put inputs in the transaction")
            object SENDING_BACK_TO_TRANSCTION : ProgressTracker.Step("Sending back to the transaction")


            fun tracker() = ProgressTracker(
                    REQUESTING_PARTNER_INPUT,
                    SENDING_BACK_TO_TRANSCTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): Boolean{
            val otherPartySession = initiateFlow(otherParty)
            progressTracker.currentStep = REQUESTING_PARTNER_INPUT
            val otherPartyHasActiveWallet = otherPartySession.sendAndReceive<Boolean>("1").unwrap{ it }
            progressTracker.currentStep = SENDING_BACK_TO_TRANSCTION
            return otherPartyHasActiveWallet
        }
    }

    @InitiatedBy(VerifyPartnerWalletFlowRequest::class)
    class VerifyPartnerWalletFlowResponse(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            otherPartySession.receive<String>().unwrap{it}

            val listMoneyStateAndRef = serviceHub.vaultService.queryBy(BrunoCoinState::class.java).states

            if(listMoneyStateAndRef.isNotEmpty()){
                otherPartySession.send(true)
            }else{
                otherPartySession.send(false)
            }
        }
    }
}