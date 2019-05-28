package main

import mcts.MCTS
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
        var mcts = MCTS(board, time, threads, 1 - board.turn, debug = true)

        val scanner = Scanner(System.`in`)
        while(true){
            println(board.toPrettyString())
            val input = scanner.nextLine()

            if(input == "start"){
                mcts = MCTS(board, time, threads, board.turn, debug = true)
            } else if(input == "draw"){
                println(board.toPrettyString())
                continue
            } else if(input == "exit"){
                System.exit(0)
            } else if(input == "reset"){
                board = Bitboard()
                mcts = MCTS(board, time, threads, 1, debug = true)
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

            val move = mcts.nextMove(board)
            board.makeMove(move)

            // TODO remove
            val stack = Stack<MCTS.Node>()
            var visits = 0
            var count = 0
            stack.push(mcts.root)
            while(stack.isNotEmpty()){
                val node = stack.pop()
                if(node.n > 0) {
                    //println("" + node.q + "/" + node.n)
                    count++
                    visits += node.n
                }
                stack.addAll(node.children)
            }
            println("Node Count: $count; Visit Count: $visits; Root Node Visit Count: ${mcts.root.n}; Memory Usage: ${Runtime.getRuntime().totalMemory()/10e6}mb")
        }
    } else {
        val scanner = Scanner(System.`in`)
        var mcts = MCTS(board, time, threads, 1 - board.turn)
        while(true){
            val input = scanner.nextLine()

            if(input == "start"){
                mcts = MCTS(board, time, threads, board.turn)
            } else if(input == "draw"){
                println(board.toPrettyString())
                continue
            } else if(input == "exit"){
                System.exit(0)
            } else if(input == "reset"){
                board = Bitboard()
                mcts = MCTS(board, time, threads, 1)
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

            val move = mcts.nextMove(board)
            board.makeMove(move)

            println(move)
        }
    }
}
