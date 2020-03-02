package com.template.contracts

import com.template.states.BrunoCoinState
import com.template.states.BrunoCoinTransferState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class BrunoCoinContract : Contract {
    companion object {
        const val ID = "com.template.contracts.BrunoCoinContract"
    }

    override fun verify(tx: LedgerTransaction) {
        //shape

        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Issue -> requireThat {
                //shape
                "Nenhum brunocoin pode ser referenciado nessa transação" using (tx.inputStates.isEmpty())

                "Deve ser gerado apenas um saída unica" using (tx.outputStates.size == 1)
                println("passou shape")
                //content
                val outputState = tx.getOutput(0)

                "A saída deve ser do tipo BrunoCoin" using (outputState is BrunoCoinState)

                val bCoinState = outputState as BrunoCoinState

                "O valor inserido deve ser maior que 0" using (bCoinState.amount > 0)
                println("passou content")
                //required signer

                val ownerKey = bCoinState.owner.owningKey

                "O dono é um assinante requerido para essa transação" using (command.signers.contains(ownerKey))
                println("passou signer")
            }
            is Commands.Transfer -> requireThat {

                //shape

                "Para transferir BrunoCoins deve-se utiliza-los na transação" using (tx.inputs.isNotEmpty())

                "Para transferência ser realizada é necessario ter uma saida de seus BrunoCoins" using (tx.outputs.isNotEmpty())

                "É necessario ter contido na transação a transferência e as moedas usadas para a mesma, " +
                        "não havendo nada a mais nem a menos que isso" using (tx.outputs.size == 3) //são 1 output de transfer e 2 de moedas (sendo 1 o valor transferido e o outro a evolução dos usados)

                //content

                val outPutState1 = tx.getOutput(0)

                "É necessario salvar a transferência das moedas na transação" using (outPutState1 is BrunoCoinTransferState)

                val bCoinTransferState = outPutState1 as BrunoCoinTransferState

                val outPutState2 = tx.getOutput(1)

                "É necessario ter o valor transferido para o counterparty incluso na transação" using (outPutState2 is BrunoCoinState)

                val cPartyOutstate = outPutState2 as BrunoCoinState

                val outPutState3 = tx.getOutput(2)

                "Deve-se utilizar BrunoCoins da sua carteira para realizar uma transferencia" using (outPutState3 is BrunoCoinState)

                val yourOutstate = outPutState3 as BrunoCoinState

                var value = 0.0

                tx.inputStates.forEach{
                    "Não devem haver elementos de entrada que não sejam BrunoCoins" using (it is BrunoCoinState)
                    val bCoin = it as BrunoCoinState
                    "É necessario que todos os custos da transação sejam ligados a você" using (bCoin.owner == yourOutstate.owner)
                    value += bCoin.amount
                }

                "O valor dos BrunoCoinStates usados devem ser maior ou igual ao valor transferido" using (value >= cPartyOutstate.amount)

                //signers

                "O dono do dinheiro deve assinar a transação de transferência" using (command.signers.contains(bCoinTransferState.owner.owningKey))
            }
            else -> throw IllegalArgumentException("Comando invalido e não reconhecido pelo sistema")
        }
    }


    interface Commands : CommandData {
        class Issue : Commands
        class Transfer : Commands
    }
}