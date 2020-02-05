package com.template

import com.template.flows.IssueFlowInitiator
import com.template.flows.TransferFlow
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class CoinIssueFlowTest{
    private lateinit var network : MockNetwork

    private lateinit var nodeA : StartedMockNode
    private lateinit var nodeB : StartedMockNode
    private lateinit var notaryA : StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows")
        )))
        nodeA = network.createPartyNode(CordaX500Name("BrunoCompany", "São Paulo", "BR"))
        nodeB = network.createPartyNode(CordaX500Name("CarlãoCompany", "Rio de Janeiro", "BR"))
        notaryA = network.createPartyNode(CordaX500Name("Notary", "São Paulo", "BR"))

//        listOf(nodeA, nodeB).forEach { it.registerInitiatedFlow( CoinIssueFlow::class.java ) }
        listOf(nodeA, nodeB).forEach { it.registerInitiatedFlow(TransferFlow.TransferResponderFlow::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun vanillaCoinIssueTest(){
        val coinIssueFlow = IssueFlowInitiator(300.00)
        val future = nodeA.startFlow(coinIssueFlow)
        network.runNetwork()
        val signedTransaction = future.getOrThrow()
    }

    @Test
    fun illegalNegativeCoinIssueTest(){
        val coinIssueFlow = IssueFlowInitiator(-300.00)
        val future = nodeA.startFlow(coinIssueFlow)
        network.runNetwork()
        assertFailsWith< TransactionVerificationException> { future.getOrThrow() }
    }

}