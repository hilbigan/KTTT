package neural

import main.Bitboard
import main.random
import org.deeplearning4j.nn.conf.CacheMode
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.Sgd
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File

class Agent(val network: MultiLayerNetwork, val explorationRate: Double = 0.05) {

    init {
        network.setCacheMode(CacheMode.DEVICE)
        Nd4j.getMemoryManager().togglePeriodicGc(false)
    }

    fun eval(board: Bitboard): Double {
        if (random() < explorationRate) {
            return (random() * 2) - 1
        }

        val array = IntArray(81)
        for (field in 0..8) {
            for (square in 0..8) {
                array[field * 9 + square] =
                    ((board.board[0][field] and (1 shl square)) shr square) - ((board.board[1][field] and (1 shl square)) shr square)
            }
        }
        val arr = Nd4j.createFromArray(Array(1) {array})
        val out = network.output(arr)
        return out.getDouble(0)
    }

    companion object {
        fun loadFromFile(file: File, explorationRate: Double = 0.05): Agent {
            val network = KerasModelImport.importKerasSequentialModelAndWeights(file.absolutePath)
            return Agent(network, explorationRate)
        }

        fun buildModel(explorationRate: Double = 0.05): Agent {
            val network: MultiLayerConfiguration = NeuralNetConfiguration.Builder().
                seed(0x1337)
                .activation(Activation.TANH)
                .weightInit(WeightInit.XAVIER)
                .updater(Sgd())
                .list()
                .layer(DenseLayer.Builder().apply {
                    nIn = 81
                    nOut = 81 * 4
                }.build())
                .layer(DenseLayer.Builder().apply {
                    nIn = 81 * 4
                    nOut = 81 * 16
                }.build())
                .layer(DenseLayer.Builder().apply {
                    nIn = 81 * 16
                    nOut = 81 * 4
                }.build())
                .layer(DenseLayer.Builder().apply {
                    nIn = 81 * 4
                    nOut = 9
                }.build())
                .layer(OutputLayer.Builder(LossFunctions.LossFunction.MEAN_ABSOLUTE_ERROR).apply {
                    activation(Activation.TANH)
                    nIn = 9
                    nOut = 1
                }.build())
                .build()

            val model = MultiLayerNetwork(network)
            model.init()

            return Agent(model, explorationRate)
        }
    }

}