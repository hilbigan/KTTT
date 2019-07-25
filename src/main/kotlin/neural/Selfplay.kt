package neural

import ai.MCTS
import ai.NeuralAgentStrategy
import main.Bitboard
import main.getResultAsInt
import main.random
import main.toCSVString
import java.io.File

fun startSelfplay(modelFile: File, trainingDataOut: File, gamesToPlay: Int, samplesPerGame: Int){
    val agent = Agent.loadFromFile(modelFile)

    val csvWriter = trainingDataOut.bufferedWriter()

    for(i in 1..gamesToPlay) {
        println("Selfplay game $i...")
        val start = System.currentTimeMillis()
        val board = Bitboard()
        val player0 = MCTS(board, 200, 4, 0, ponder = false, strategy = NeuralAgentStrategy(agent), debug = false)
        val player1 = MCTS(board, 200, 4, 1, ponder = false, strategy = NeuralAgentStrategy(agent), debug = false)
        var turn = 0
        val positions = mutableListOf<Bitboard>()
        while (!board.isGameOver()){
            val move = if(turn == 0){
                player0.nextMove(board)
            } else {
                player1.nextMove(board)
            }
            board.makeMove(move)
            positions.add(board.clone())
            turn = 1 - turn
        }

        println("Game $i over, writing to file. (${System.currentTimeMillis() - start}ms)")

        val chosen = mutableListOf<Int>()
        val result = board.getResultAsInt()
        for(j in 1..samplesPerGame){
            val idx = random(positions.size)

            if(idx in chosen && positions.size >= samplesPerGame){
                continue
            }

            chosen.add(idx)
            val pos = positions[random(positions.size)]
            csvWriter.write(pos.toCSVString())
            csvWriter.write("$result\n")
        }

        csvWriter.flush()
    }

    csvWriter.close()
}