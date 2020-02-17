package com.template.contracts

import com.template.states.BrunoCoinState
import com.template.states.BrunoCoinTransferState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class BrunoCoinContract : Contract {
    companion object{
        const val ID = "com.template.contracts.BrunoCoinContract"
    }

    override fun verify(tx: LedgerTransaction){
        //shape

        val command = tx.commands.requireSingleCommand<Commands>()

        when(command.value){
            is Commands.Issue -> requireThat {
                //shape
                "Nenhum brunocoin pode ser referenciado nessa transação" using (tx.inputStates.isEmpty())

                "Deve ser gerado apenas uma saída unica" using (tx.outputStates.size == 1)
                println("passou shape")
                //content
                val outputState = tx.getOutput(0)

                "A saída deve ser um BrunoCoin" using (outputState is BrunoCoinState)

                val bCoinState = outputState as BrunoCoinState

                "O valor inserido deve ser maior que 0" using (bCoinState.amount > 0)
                println("passou content")
                //required signer

                val ownerKey = bCoinState.owner.owningKey

                "O dono é um assinante requerido para essa transação" using (command.signers.contains(ownerKey))
                println("passou signer")
            }
            is Commands.Transfer -> requireThat {
               val outPutState = tx.getOutput(0)

                val bCoinTransferState = outPutState as BrunoCoinTransferState

                "O dono do dinheiro deve assinar a transação de transferência" using (command.signers.contains(bCoinTransferState.owner.owningKey))
            }
            else -> throw IllegalArgumentException("Comando invalido e não reconhecido pelo sistema")
        }
    }


    interface Commands: CommandData{
        class Issue : Commands
        class Transfer : Commands
    }
}