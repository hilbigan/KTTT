package neural

import main.Bitboard
import main.random
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.factory.Nd4j
import java.io.File

class Agent(val network: MultiLayerNetwork, val explorationRate: Double = 0.05) {

    fun eval(board: Bitboard): Double {
        if(random() < explorationRate){
            return (random() * 2) - 1
        }

        val array = IntArray(81)
        for (field in 0..8){
            for(square in 0..8){
                array[field * 9 + square] = ((board.board[0][field] and (1 shl square)) shr square) - ((board.board[1][field] and (1 shl square)) shr square)
            }
        }
        val out = network.output(Nd4j.createFromArray(Array(1) {array} ))
        return out.getDouble(0)
    }

    companion object {
        fun loadFromFile(file: File, explorationRate: Double = 0.05): Agent {
            val network = KerasModelImport.importKerasSequentialModelAndWeights(file.absolutePath)
            return Agent(network, explorationRate)
        }
    }

}