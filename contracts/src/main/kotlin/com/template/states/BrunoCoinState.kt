package com.template.states

import com.template.contracts.BrunoCoinContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

// *********
// * State *
// *********
@BelongsToContract(BrunoCoinContract::class)
data class BrunoCoinState(
        val owner : Party,
        val amount : Double,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : ContractState, LinearState{//,  QueryableState{
    override val participants: List<AbstractParty> get() = listOf(owner)
//    override fun generateMappedObject(schema: MappedSchema): PersistentState {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//    override fun supportedSchemas(): Iterable<MappedSchema> {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

}



