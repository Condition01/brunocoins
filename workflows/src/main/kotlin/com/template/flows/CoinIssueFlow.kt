package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.BrunoCoinContract
import com.template.states.BrunoCoinState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class CoinIssueFlow(val amount : Double) : FlowLogic<UniqueIdentifier>() {

    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new BrunoCoin.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.")
        fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call() : UniqueIdentifier {
        // Initiator flow logic goes here.
        val listMoneyStateAndRef = serviceHub.vaultService.queryBy(BrunoCoinState::class.java).states

        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        progressTracker.currentStep = GENERATING_TRANSACTION

        val bIssueState = brunoCoinStateBuilding(listMoneyStateAndRef)

        val tx = buildTransaction(bIssueState, notary, listMoneyStateAndRef)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        tx.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTransaction = serviceHub.signInitialTransaction(tx)

        subFlow(FinalityFlow(signedTransaction, listOf()))

        return bIssueState.linearId
    }

    private fun buildTransaction(bIssueState: BrunoCoinState,
                                        notary: Party,
                                        listMoneyStateAndRef: List<StateAndRef<BrunoCoinState>>): TransactionBuilder {
        val txCommand = Command(BrunoCoinContract.Commands.Issue(), bIssueState.participants.map { it.owningKey })
        val tx = TransactionBuilder(notary)
                .addOutputState(bIssueState, BrunoCoinContract.ID)
                .addCommand(txCommand)
        if (listMoneyStateAndRef.isNotEmpty()) tx.addInputState(listMoneyStateAndRef.single())
        return tx
    }

    private fun brunoCoinStateBuilding(bCoinStateAndRefList: List<StateAndRef<BrunoCoinState>>) =
            if (bCoinStateAndRefList.isNotEmpty())
                BrunoCoinState(owner = serviceHub.myInfo.legalIdentities.first(), amount = bCoinStateAndRefList.first().state.data.amount + amount)
            else
                BrunoCoinState(owner = serviceHub.myInfo.legalIdentities.first(), amount = amount)
}


