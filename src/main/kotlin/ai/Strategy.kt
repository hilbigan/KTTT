package ai

import main.Bitboard
import main.random

enum class Strategy(val bitmask: Int) {

    DIAG0(Bitboard.DIAGS[0]), DIAG1(Bitboard.DIAGS[1]);

    fun isAchievable(board: Bitboard, player: Int): Boolean {
        return (board.getMetaField()[1 - player] and bitmask) == 0
    }

    fun isAchieved(board: Bitboard, player: Int): Boolean {
        return (board.getMetaField()[player] and bitmask) == bitmask
    }

    fun isViable(board: Bitboard, player: Int): Boolean {
        return isAchievable(board, player) && !isAchieved(board, player)
    }

    companion object {
        fun getRandom(): Strategy {
            val vals = values()
            return vals[random(vals.size)]
        }
    }

}