package main

import ai.MCTS
import ai.NeuralAgentStrategy
import ai.NeuralAgentStrategyPlaceholder
import ai.RandomPlayStrategy
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import neural.Agent
import java.io.File
import java.util.*

/*
java -jar AutoFighter.jar 5 kt0 "java -jar ../KTTT_jar/KTTT.jar 1500 1" rttt "..\..\..\..\GitHub Projects\UltimateTicTacToe\target\release\RTTT engine"
java -jar AutoFighter.jar 5 kt0 "java -jar ../KTTT_jar/KTTT.jar 1500 8" ktold"java -jar ../KTTT_jar/KTTT_breadcrumbs.jar 1500 8"
 */

class CLIParser(parser: ArgParser) {
    val time by parser.positional("AI thinking time") { toInt() }.default(1500)
    val threads by parser.positional("CPU threads to use") { toInt() }.default(8)
    val aiStrategy by parser.mapping("--mcts" to RandomPlayStrategy(), "--nn" to NeuralAgentStrategyPlaceholder(), help = "ai type").default(RandomPlayStrategy())
    val movegen by parser.flagging("movegen benchmark (overrides other options)").default(false)
    val movegenDepth by parser.storing("movegen depth") { toInt() }.default(-1)
    val repl by parser.flagging("-r", "--repl", help = "REPL mode (human-readable interface)").default(false)
    val persistent by parser.flagging("enable mcts game tree persistence").default(true)
    val ponder by parser.flagging("-p", "--ponder", help = "enable mcts low-cpu pondering").default(true)
    val modelFile by parser.storing("-f", "--model-file", help = "model file path").default("models/model.h5")
    val fromPosition by parser.storing(help = "starting position").default("start")
    val debug by parser.flagging("-d", "--debug", help = "enable verbose mode").default(true)

    fun log(s: String){
        if(debug){
            println(s)
        }
    }

    fun buildAI(board: Bitboard): MCTS {
        if(aiStrategy is NeuralAgentStrategyPlaceholder){
            if(ponder) {
                log("Pondering not recommended for NN-Strategy.")
            }

            return MCTS(board, time, threads, 1 - board.turn, debug, persistent, ponder, NeuralAgentStrategy(Agent.loadFromFile(File(modelFile), 0.0)))
        }
        return MCTS(board, time, threads, 1 - board.turn, debug, persistent, ponder, aiStrategy)
    }
}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::CLIParser).run {

        var board: Bitboard
        if(fromPosition == "start" || fromPosition.isEmpty()){
            board = Bitboard()
        } else {
            board = Bitboard.fromString(fromPosition)
        }

        if(movegen){
            benchmark("Movegen depth=$movegenDepth"){
                println(Bitboard.moveGen(board, movegenDepth))
            }
            System.exit(0)
        }

        var mcts = buildAI(board)
        val scanner = Scanner(System.`in`)

        while(true){
            if(repl) {
                println(board.toPrettyString())
            }
            val input = scanner.nextLine()

            if(input == "start"){
                mcts = buildAI(board)
            } else if(input == "draw"){
                println(board.toPrettyString())
                continue
            } else if(input == "exit"){
                mcts.stop()
                System.exit(0)
            } else if(input == "reset"){
                board = Bitboard()
                mcts = buildAI(board)
                println("ok")
                continue
            } else {
                try {
                    val split = input.split(" ")
                    val move = board.buildMove(split[0].toInt(), split[1].toInt())
                    if(board.isLegal(move))
                        board.makeMove(move)
                    else {
                        if(!repl){
                            error("Illegal move input")
                        } else {
                            println("Illegal move!")
                            continue
                        }
                    }
                } catch (ignored: Exception){
                    println("Invalid move!")
                    if(!repl){
                        error("Illegal move input")
                    } else {
                        continue
                    }
                }

                if(board.isGameOver()){
                    log("Game ended.")
                    continue
                }
            }

            val move = mcts.nextMove(board)
            board.makeMove(move)

            if(repl && debug) {
                // TODO remove
                val stack = Stack<MCTS.Node>()
                var visits = 0
                var count = 0
                stack.push(mcts.root)
                while (stack.isNotEmpty()) {
                    val node = stack.pop()
                    if (node.n > 0) {
                        //println("" + node.q + "/" + node.n)
                        count++
                        visits += node.n
                    }
                    stack.addAll(node.children)
                }
                log("Node Count: $count; Visit Count: $visits; Root Node Visit Count: ${mcts.root.n}; Memory Usage: ${Runtime.getRuntime().totalMemory() / 10e6}mb")
            } else if(!repl) {
                println(move)
            }
        }
    }
}
