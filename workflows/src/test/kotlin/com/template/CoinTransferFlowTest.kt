package com.template

import com.template.flows.IssueFlowInitiator
import com.template.flows.TransferFlow
import com.template.states.BrunoCoinState
import com.template.states.BrunoCoinTransferState
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CoinTransferFlowTest : BaseTest(){

    @Before
    override fun setup() {
        super.setup()
        preIssueAValue()
    }

    fun preIssueAValue(){
        val coinIssueFlow = IssueFlowInitiator(300.00)
        val future = nodeA.startFlow(coinIssueFlow)
        network.runNetwork()
        val signedTransaction = future.getOrThrow()
    }

    @After
    override fun tearDown() = super.tearDown()


    @Test
    fun testTheVaultQuery(){
        val queryAllStates = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
//        services.
        val states = nodeA.services.vaultService.queryBy<BrunoCoinState>(queryAllStates)
        assert(states.states.isNotEmpty())
        assertEquals(1, states.states.size)
    }

    @Test
    fun transferCoinVanillaTest(){

        val future = nodeA.startFlow(TransferFlow.CoinTransferFlow(200.00, nodeB.services.myInfo.legalIdentities.first()))
        network.runNetwork()
        val signedTransaction = future.getOrThrow()

        val bCoinStates = nodeA.allStates<BrunoCoinState>()
        val bCoinTransferStatesNodeA = nodeA.allContractStates<BrunoCoinTransferState>()
        val bCoinTransferStatesNodeB = nodeB.allContractStates<BrunoCoinTransferState>()

        assertEquals(2, bCoinStates.states.size)
        assertEquals(1, bCoinTransferStatesNodeA.states.size)
        assertNotEquals(bCoinTransferStatesNodeA, bCoinTransferStatesNodeB)
        assertEquals(bCoinTransferStatesNodeA.states.size, bCoinTransferStatesNodeB.states.size)
        assertEquals(bCoinTransferStatesNodeA.states.first(), bCoinTransferStatesNodeB.states.first())

    }

}