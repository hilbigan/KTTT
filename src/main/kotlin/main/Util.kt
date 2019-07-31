package main

import java.io.File
import kotlin.random.Random

fun oct(i: Int): Int {
    return Integer.valueOf(i.toString(), 8)!!
}

fun popcnt(i: Int): Int = Integer.bitCount(i)

fun Boolean.toInt(): Int {
    return if(this) 1 else 0
}

/**
 * Prints timing information on the given task
 *
 * @param name Name of the task
 * @param runnable Task to be executed
 */
fun benchmark(name: String, runnable: () -> Unit){
    println("Running $name ...")
    val start = System.currentTimeMillis()
    runnable.invoke()
    println("Time: ${System.currentTimeMillis() - start}ms")
}

var totalGlobalTime: Long = 0
fun globalTime(runnable: () -> Unit){
    val start = System.currentTimeMillis()
    runnable.invoke()
    totalGlobalTime += System.currentTimeMillis() - start
}

private val random = Random(System.currentTimeMillis())
fun random(max: Int) = random.nextInt(max)
fun random() = random.nextDouble()

fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
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

    val board = Bitboard(Bitboard.ALL_FIELDS, values)
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

fun mergeCSVFiles(out: File, vararg files: File){
    files.filter { it.exists() }.flatMap {
        it.readLines()
    }.forEach {
        out.appendText(it + "\n")
    }

    files.filter { it.exists() }.forEach { it.delete() }
}