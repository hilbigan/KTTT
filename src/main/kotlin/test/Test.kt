package test

import ai.MCTS
import main.Bitboard

fun main() {
    testLegalMove2()
}

fun testLegalMove2(){
    val board = Bitboard.fromString("XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|OOOOOOOOO|XXXXXXXXX|XXXXXXXXX|XXXXXXXXX|OOOOOOOOO".toLowerCase())
    val mcts = MCTS(board, 500, 1, 0, true, true)
    mcts.nextMove(board)
}

fun testLegalMove(){
    val board = Bitboard.fromString("__OO__O__|X_OO_____|XXXXXXXXX|XXXX_____|OOOOOOOOO|_____O_X_|XXXXXXXXX|__O_____X|OOOOOOOOO".toLowerCase())
    board.validField = 1
    println(board.turn)
    println(board.toPrettyString())
    val move = board.buildMove(0,0)
    assert(board.isLegal(move))
    board.makeMove(move)
    println(board.toPrettyString())
    println(board.isLegal(board.buildMove(0, 4)))
}