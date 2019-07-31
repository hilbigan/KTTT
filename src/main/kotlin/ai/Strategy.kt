package ai

import main.Bitboard
import main.Won
import main.random
import neural.Agent

abstract class Strategy {
    abstract fun rollout(board: Bitboard, player: Int): Double
}

class RandomPlayStrategy : Strategy() {
    override fun rollout(board: Bitboard, player: Int): Double {
        val boardCopy = board.clone()
        var moves = board.getTotalPopcnt()

        while (!boardCopy.isGameOver()) {
            val legalMoves = boardCopy.getAllMoves()
            val move = legalMoves[random(legalMoves.size)]
            boardCopy.makeMove(move)
            if (boardCopy.validField == Bitboard.ALL_FIELDS && legalMoves.size > 3 && moves < 30) {
                boardCopy.undoMove(move)
            }
            moves++
        }

        val state = boardCopy.getGameState()
        if (state is Won && state.who == player) { // Won
            return 1.0
        } else if (state is Won) { // Lost
            return -1.0
        } else { // Tie
            return 0.0
        }
    }
}

class NeuralAgentStrategy(val agent: Agent) : Strategy() {
    override fun rollout(board: Bitboard, player: Int): Double {
        // 1 means player 0 has an advantage, -1 means player 1 has an advantage.
        // Therefore, if we are player 1, flip the eval score.
        return if(player == 1){
            -agent.eval(board)
        } else {
            agent.eval(board)
        }
    }
}

class NeuralAgentStrategyPlaceholder : Strategy() {
    override fun rollout(board: Bitboard, player: Int): Double {
        error("NeuralAgentStrategyPlaceholder should not be used for rollout!")
    }
}