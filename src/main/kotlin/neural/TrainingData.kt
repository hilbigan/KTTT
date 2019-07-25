package neural

import main.Bitboard
import main.Bitboard.Companion.ALL_FIELDS
import main.Won
import main.random
import java.io.File

fun main() {
    /*val board = Bitboard()
    while(!board.isGameOver()){
        val legalMoves = board.getAllMoves()
        val move = legalMoves[random(legalMoves.size)]
        board.makeMove(move)
    }
    println(board.toPrettyString())
    println(board.toCSVString())*/

    val out = File("ludwig/training_data.csv").bufferedWriter()
    for(i in 0..80){
        out.write("v$i,")
    }
    out.write("result\n")
    for(i in 0..0){
        if(i % 10000 == 0){
            println(i)
        }
        val board = randomBitboard()
        while(!board.isGameOver()){
            val legalMoves = board.getAllMoves()
            val move = legalMoves[random(legalMoves.size)]
            board.makeMove(move)
        }
        out.write(board.toCSVString())
        val vec = board.getResultAsInt()
        out.write("$vec\n")
    }
    out.flush()
    out.close()
}

fun randomBitboard(density: Double = 0.3): Bitboard {
    val values = arrayOf(IntArray(9), IntArray(9))

    for (field in 0..8){
        for(square in 0..8){
            val random = random()
            if(random > density){
                var b = 0
                if(random > density + (1 - density)/2.0){
                    b = 1
                }
                values[b][field] = values[b][field] or (1 shl square)
            }
        }
    }

    val board = Bitboard(ALL_FIELDS, values)
    board.turn = board.calculateTurn()
    return board
}

fun Bitboard.toCSVString(): String {
    var ret = ""
    for (field in 0..8){
        for(square in 0..8){
            ret += ((this.board[0][field] and (1 shl square)) shr square) - ((this.board[1][field] and (1 shl square)) shr square)
            ret += (",")
        }
    }
    return ret
}

fun Bitboard.getResultAsInt() : Int {
    val result = getGameState()
    return if(result is Won){
        if(result.who == 0){
            1
        } else {
            -1
        }
    } else {
        0
    }
}