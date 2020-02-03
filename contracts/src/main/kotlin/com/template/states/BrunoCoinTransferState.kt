package com.template.states

import com.template.contracts.BrunoCoinContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub

@BelongsToContract(BrunoCoinContract::class)
data class BrunoCoinTransferState(
        val owner : Party,
        val newOwner : Party,
        val amount : Double
) : ContractState{
    override val participants: List<AbstractParty> get() = listOf(owner,newOwner)
}
