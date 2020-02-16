package com.template

import com.template.flows.CoinIssueFlow
import com.template.states.BrunoCoinState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.utilities.getOrThrow
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class CoinIssueFlowTest : BaseTest(){

    @Before
    override fun setup() {
      super.setup()
    }

    @After
    override fun tearDown() = super.tearDown()

    @Test
    fun vanillaCoinIssueTest(){
        val coinIssueFlow = CoinIssueFlow(300.00)
        val future = nodeA.startFlow(coinIssueFlow)
        network.runNetwork()
        val signedTransaction = future.getOrThrow()


        val allStates = nodeA.getAllLinearStatesWithAnyStatus<BrunoCoinState>()

    }

    @Test
    fun illegalNegativeCoinIssueTest(){
        val coinIssueFlow = CoinIssueFlow(-300.00)
        val future = nodeA.startFlow(coinIssueFlow)
        network.runNetwork()
        assertFailsWith< TransactionVerificationException> { future.getOrThrow() }
    }

}