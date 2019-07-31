package neural

import main.getResultAsInt
import main.random
import main.randomBitboard
import main.toCSVString
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
