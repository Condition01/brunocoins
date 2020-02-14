package com.template.schemas

import com.template.states.BrunoCoinTransferState
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.MAX_HASH_HEX_SIZE
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object BrunoCoinTranseferSchema

@CordaSerializable
object BrunoCoinTransferSchemaV1 : MappedSchema(schemaFamily = BrunoCoinTranseferSchema.javaClass, version = 1, mappedTypes = listOf(BrunoCoinTransferState::class.java)){

    override val migrationResource = "bruno.changelog-master"

    @Entity
    @Table(name = "bruno_coin_transfer_states")
    class PersistentBrunoCoinTransferState(
            @Column(name = "owner_name", nullable = true)
            var owner : AbstractParty?,

            @Column(name = "new_owner_name", nullable = true)
            var newOwner : AbstractParty?,

            @Column(name = "amount", nullable = false)
            var amount : Double,

            @Column(name = "owner_key_hash", length = MAX_HASH_HEX_SIZE, nullable = false)
            var ownerKeyHash : String,

            @Column(name = "new_owner_key_hash", length = MAX_HASH_HEX_SIZE, nullable = false)
            var newOwnerKeyHash : String

    ) : PersistentState()
}