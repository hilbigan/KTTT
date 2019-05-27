package auto

import main.random
import main.Bitboard
import main.Tied
import main.Won

fun main(args: Array<String>) {

    if(args.isEmpty() || args.contains("help")){
        println("Usage: java -jar auto_fighter.jar name0 'command' name1 'command'")
        System.exit(0)
    }

    val fights = args[0].toInt()
    val prog0name = args[1]
    val prog0 = args[2].replace("\"","")
    val prog1name = args[3]
    val prog1 = args[4].replace("\"","")

    var score0 = 0
    var score1 = 0
    var starter = random(1)
    var turn: Int
    println("Starting $prog0name ...")
    val process0 = ProcessBuilder(prog0.split(" ")).start()
    val in0 = process0.inputStream.bufferedReader()
    val err0 = process0.errorStream.bufferedReader()
    val out0 = process0.outputStream.bufferedWriter()

    println("Starting $prog1name ...")
    val process1 = ProcessBuilder(prog1.split(" ")).start()
    val in1 = process1.inputStream.bufferedReader()
    val err1 = process1.errorStream.bufferedReader()
    val out1 = process1.outputStream.bufferedWriter()

    Thread.sleep(1000)

    println("Ready.")

    fun w(i: Int, s: String){
        listOf(out0, out1)[i].apply {
            write(s)
            newLine()
            flush()
        }
    }

    fun r(i: Int): String {
        return if(i == 0) in0.readLine() else in1.readLine()
    }

    fun waitFor(i: Int){
        println("Waiting for $i (${listOf(prog0name, prog1name)[i]})")
        val start = System.currentTimeMillis()
        if(i == 0) {
            while (!in0.ready() && !err0.ready()) {
            }
            if(err0.ready()){
                println(err0.readText())
            }
        } else {
            while (!in1.ready() && !err1.ready()) {
            }
            if(err1.ready()){
                println(err1.readText())
            }
        }
        println("$i (${listOf(prog0name, prog1name)[i]}) ready, waited ${System.currentTimeMillis() - start}")
    }

    for (i in 0 until fights){
        starter = 1 - starter
        turn = starter
        val board = Bitboard()

        println("Game $i")

        w(0, "reset")
        waitFor(0)
        var rec = r(0)
        println(rec)
        w(1, "reset")
        waitFor(1)
        rec = r(1)
        println(rec)

        println("${listOf(prog0name, prog1name)[starter]} is starting!")
        w(starter, "start")

        while(!board.isGameOver()){
            waitFor(turn)
            val move = r(turn).toInt()

            if(board.isLegal(move))
                board.makeMove(move)
            else {
                println("==========")
                println("$turn attempted illegal move: ${Bitboard.moveToString(move)}")
                w(turn, "draw")
                for(o in (0..11))
                    println(r(turn))
                println("==========")
                System.exit(1)
            }
            println("$turn (${listOf(prog0name, prog1name)[turn]}) made move ${Bitboard.moveToString(move)}.")

            if(board.isGameOver()) break

            turn = 1 - turn
            val write = "${Bitboard.toIndex(Bitboard.field(move))} ${Bitboard.toIndex(Bitboard.square(move))}"
            println("Writing '$write'")
            println(board.toPrettyString())
            w(turn, "${Bitboard.toIndex(Bitboard.field(move))} ${Bitboard.toIndex(Bitboard.square(move))}")
        }

        val result = board.getGameState()
        when(result){
            is Tied -> println("Game was tied!")
            is Won -> {
                var winner: Int
                if(result.who == 0){
                    println("Program $starter won!")
                    winner = starter
                } else {
                    println("Program ${1 - starter} won!")
                    winner = 1 - starter
                }
                if(winner == 0)
                    score0++
                else score1++
                println("Score: $score0 -- $score1 ($prog0name -- $prog1name)")
            }
        }
    }
}