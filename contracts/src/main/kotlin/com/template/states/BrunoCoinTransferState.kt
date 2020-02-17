package com.template.states

import com.template.contracts.BrunoCoinContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.internal.hash
import net.corda.core.node.ServiceHub
import net.corda.core.schemas.QueryableState
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import  kotlin.collections.Iterable

@BelongsToContract(BrunoCoinContract::class)
data class BrunoCoinTransferState(
        val owner : Party,
        val newOwner : Party,
        val amount : Double
) : ContractState /*, QueryableState*/{
    override val participants: List<AbstractParty> get() = listOf(owner,newOwner)


//    override fun generateMappedObject(schema: MappedSchema): PersistentState {
//        return when(schema){
//            is BrunoCoinTransferSchemaV1 -> {
//                BrunoCoinTransferSchemaV1.PersistentBrunoCoinTransferState(
//                        owner = owner,
//                        newOwner = newOwner,
//                        amount = amount,
//                        ownerKeyHash = owner.owningKey.hash.toString(),
//                        newOwnerKeyHash = newOwner.owningKey.hash.toString()
//                )
//            }
//            else -> throw IllegalArgumentException("Not supported schema $schema")
//        }
//    }
//
//    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BrunoCoinTransferSchemaV1)

}
