package com.template


import com.template.flows.IssueFlowInitiator
import com.template.flows.TransferFlow
import com.template.states.BrunoCoinTransferState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

open class BaseTest{

    protected lateinit var network : MockNetwork
    protected lateinit var nodeA : StartedMockNode
    protected lateinit var nodeB : StartedMockNode
    protected lateinit var notaryA : StartedMockNode

    @Before
    open fun setup() {
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
    open fun tearDown() = network.stopNodes()

    protected inline fun <reified T : LinearState> StartedMockNode.allStates(linearId : UniqueIdentifier? = null) : Vault.Page<T>{
        val queryAllStatusServiceProvider =
                if(linearId != null)
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId), status = Vault.StateStatus.ALL)
                else
                    QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.ALL)
        return services.vaultService.queryBy<T>(queryAllStatusServiceProvider)
    }

    protected inline fun <reified T : ContractState> StartedMockNode.allContractStates() : Vault.Page<T>{
        val queryAllStatusServiceProvider = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return services.vaultService.queryBy<T>(queryAllStatusServiceProvider)
    }


}