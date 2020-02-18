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
                "Pode haver apenas um input na transação inicial" using (tx.inputStates.size <= 1)

                "Deve apenas haver um output unico" using (tx.outputStates.size == 1)
                println("passou shape")
                //content
                val outputState = tx.getOutput(0)

                "O output deve ser do tipo BrunoCoinState" using (outputState is BrunoCoinState)

                val bCoinState = outputState as BrunoCoinState

                "O valor inserido deve ser maior que 0" using (bCoinState.amount > 0)
                println("passou content")
                //required signer

                val ownerKey = bCoinState.owner.owningKey

                "O owner é um assinante requerido para essa transação" using (command.signers.contains(ownerKey))
                println("passou signer")
            }
            is Commands.Transfer -> requireThat {
                //shape
                "Não deve haver um inputState" using (tx.inputStates.size == 1)


                "Devem haver 3 outputStates" using (tx.outputStates.size == 3)
                println("passou shape")
                //content
                val inputStateOne = tx.getInput(0)


                "O inputStateOne deve ser do tipo BrunoCoinState" using (inputStateOne is BrunoCoinState)

                val outputStateOne = tx.getOutput(0)

                "O primeiro outputState deve ser do tipo BrunoCoinState" using (outputStateOne is BrunoCoinState)

                val outputStateTwo = tx.getOutput(1)

                "O segundo outputState deve ser do tipo BrunoCoinTransferState" using (outputStateTwo is BrunoCoinTransferState)

                val outPutStateThree = tx.getOutput(2)
                "O terceiro outputState deve ser do tipo BrunoCoinState" using (outPutStateThree is BrunoCoinState)

                val bCoinInpState = inputStateOne as BrunoCoinState
                val bCoinOutState = outputStateOne as BrunoCoinState
                val bCoinTransferState = outputStateTwo as BrunoCoinTransferState
                val bCoinOutStateTwo = outPutStateThree as BrunoCoinState

                "O owner do input deve ser diferente do owner do output" using (bCoinInpState.owner != bCoinOutState.owner)

                "O valor transferido deve ser maior que 0" using (bCoinOutState.amount > 0)

                "O inputState deve ter valor igual ou maior que o outputState" using (bCoinInpState.amount >= bCoinOutState.amount)

                "o outputStateOne deve ter o mesmo valor que o outputStateTwo" using (bCoinOutState.amount == bCoinTransferState.amount)

                "O owner do input deve ser igual ao owner do outputStateTwo" using (bCoinOutStateTwo.owner == bCoinInpState.owner)

                "O owner do input não pode ficar com um valor negativo na conta" using (bCoinOutStateTwo.amount >= 0)
                println("passou content")
                //signers
                val oldOwner = bCoinTransferState.owner

                "O dono do dinheiro deve assinar" using (command.signers.contains(oldOwner.owningKey))
                println("passou signer")
            }
            else -> throw IllegalArgumentException("Comando invalido e não reconhecido pelo sistema")
        }
    }


    interface Commands: CommandData{
        class Issue : Commands
        class Transfer : Commands
    }
}