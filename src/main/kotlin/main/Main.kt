package main

import java.lang.Exception
import java.util.*

/*
java -jar AutoFighter.jar 5 kt0 "java -jar ../KTTT_jar/KTTT.jar 1500 1" rttt "..\..\..\..\GitHub Projects\UltimateTicTacToe\target\release\RTTT engine"
java -jar AutoFighter.jar 5 kt0 "java -jar ../KTTT_jar/KTTT.jar 1500 8" ktold"java -jar ../KTTT_jar/KTTT_breadcrumbs.jar 1500 8"
 */

fun main(args: Array<String>) {
    var board = Bitboard()

    if(args.contains("movegen")){
        val depth = args[1].toInt()
        benchmark("Movegen depth=$depth"){
            println(Bitboard.moveGen(board, depth))
        }
        System.exit(0)
    }

    val time = if(args.isNotEmpty()) args[0].toInt() else 1500
    val threads = if(args.size >= 2) args[1].toInt() else 8

    if(args.contains("repl")){
        val scanner = Scanner(System.`in`)
        while(true){
            println(board.toPrettyString())
            val input = scanner.nextLine()

            if(input == "start"){

            } else if(input == "draw"){
                println(board.toPrettyString())
                continue
            } else if(input == "exit"){
                System.exit(0)
            } else if(input == "reset"){
                board = Bitboard()
                println("ok")
                continue
            } else {
                try {
                    val split = input.split(" ")
                    val move = board.buildMove(split[0].toInt(), split[1].toInt())
                    if(board.isLegal(move))
                        board.makeMove(move)
                    else {
                        println("Illegal move!")
                        continue
                    }
                } catch (ignored: Exception){
                    println("Invalid move!")
                    continue
                }
            }

            val mcts = mcts.MCTS(board, time, threads, board.turn, debug = true)
            val move = mcts.start()
            board.makeMove(move)
        }
    } else {
        val scanner = Scanner(System.`in`)
        while(true){
            val input = scanner.nextLine()

            if(input == "start"){

            } else if(input == "draw"){
                println(board.toPrettyString())
                continue
            } else if(input == "exit"){
                System.exit(0)
            } else if(input == "reset"){
                board = Bitboard()
                println("ok")
                continue
            } else {
                val split = input.split(" ")
                val move = board.buildMove(split[0].toInt(), split[1].toInt())
                if(board.isLegal(move))
                    board.makeMove(move)
                else {
                    error("Illegal move input")
                }
            }

            val mcts = mcts.MCTS(board, time, threads, board.turn)
            val move = mcts.start()
            board.makeMove(move)

            println(move)
        }
    }
}
