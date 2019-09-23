package auto

import main.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.lang.Math.pow

fun main() {
    val engines = listOf(
        //Engine("kttt-1x-250", "java -jar build/libs/kttt-1.0.jar 250 1 --brp --persistent -d"),
        Engine("kttt-4x-250", "java -jar build/libs/kttt-1.0.jar 250 4 --brp --persistent -d"),
        //Engine("kttt-1x-250-cachebug", "java -jar build/libs/kttt-1.0-cachebug.jar 250 1 --brp --persistent -d"),
        Engine("kttt-4x-250-cachebug", "java -jar build/libs/kttt-1.0-cachebug.jar 250 4 --brp --persistent -d")

        //Strength variations
        //Engine("kttt-1x-250", "java -jar build/libs/kttt-1.0.jar 250 1 --brp --persistent -d"),
        //Engine("kttt-1x-1000", "java -jar build/libs/kttt-1.0.jar 1000 1 --brp --persistent -d"),
        //Engine("kttt-4x-250", "java -jar build/libs/kttt-1.0.jar 250 4 --brp --persistent -d"),
        //Engine("kttt-4x-1000", "java -jar build/libs/kttt-1.0.jar 1000 4 --brp --persistent -d"),
        //Engine("kttt-8x-250", "java -jar build/libs/kttt-1.0.jar 250 8 --brp --persistent -d"),
        //Engine("kttt-8x-1000", "java -jar build/libs/kttt-1.0.jar 1000 8 --brp --persistent -d"),

        //Rust
        //Engine("Rttt-1x-1500", "../UltimateTicTacToe/target/release/RTTT engine")

        // Neurals
        //Engine("nn-8x-250", "java -jar build/libs/kttt-1.0.jar 250 8 --nn --persistent -d"),
        //Engine("nn-8x-1000", "java -jar build/libs/kttt-1.0.jar 1000 8 --nn --persistent -d")
    )

    val fightsPerMatch = 10

    for(e0 in engines){
        for(e1 in engines){
            if(e0 == e1) continue

            var starter = random(2)
            var additionalMatches = 0
            for (i in 1..(fightsPerMatch + additionalMatches)){
                val success = Fight(arrayOf(e0, e1), starter).start()
                if(!success){
                    additionalMatches++
                } else {
                    starter = 1 - starter
                }
            }
        }
    }

    println("=== Results ===")
    engines.sortedBy { it.elo }.forEach {
        println("${it.name}: ${it.elo}")
    }

}

class Fight(private val e: Array<Engine>, private val starter: Int = 0){

    fun start(): Boolean {
        println("*** ${e[0].name} (${e[0].elo}) vs ${e[1].name} (${e[1].elo}) ***")

        e.forEach {
            it.start()
            it.write("reset")
            it.waitFor()
            println(it.read())
        }

        val board = Bitboard()
        var turn = starter
        e[turn].write("start")

        while(!board.isGameOver()){
            e[turn].waitFor()
            val read = e[turn].read()
            var move: Int

            println(board.toPrettyString())

            try {
                move = read!!.toInt()
            } catch (ex: Exception){
                println("\n***")
                println("Got exception while trying to parse program response: $ex")
                println("\nResponsible program:")
                println(e[turn].name)
                println("\nFull stack trace:")
                ex.printStackTrace()
                println("\nFull program input:")
                println(e[turn].inr.readLines().joinToString("\n"))
                println("***")
                return false
            }

            if(board.isLegal(move)) {
                board.makeMove(move)
                println("${e[turn].name} made move ${Bitboard.moveToString(move)}.")
            } else {
                println("***")
                println("$turn attempted illegal move: ${Bitboard.moveToString(move)}")
                e[turn].write("draw")
                for(o in (0..11))
                    println(e[turn].read())
                println("***")
                return false
            }

            if(board.isGameOver()) {
                println(board.toPrettyString())
                break
            }
            turn = 1 - turn
            e[turn].write("${Bitboard.toIndex(Bitboard.field(move))} ${Bitboard.toIndex(Bitboard.square(move))}")
        }

        e[0].stop()
        e[1].stop()

        val result = board.getGameState()
        val expectedA = 1.0 / (1.0 + pow(10.0, (e[1].elo - e[0].elo) / 400.0))
        val expectedB = 1.0 - expectedA
        when(result){
            is Tied -> {
                println("*** Game ${e[0].name} vs ${e[1].name} was tied! ***")
                e[0].elo += (40 * (0.5 - expectedA)).toInt()
                e[1].elo += (40 * (0.5 - expectedB)).toInt()
            }
            is Won -> {
                val winner: Int
                if(result.who == 0){
                    println("Program $starter (${e[starter].name}) won!")
                    winner = starter
                } else {
                    println("Program ${1 - starter} (${e[1 - starter].name}) won!")
                    winner = 1 - starter
                }

                if(winner == 0){
                    e[0].elo += (40 * (1.0 - expectedA)).toInt()
                    e[1].elo += (40 * (0.0 - expectedB)).toInt()
                } else {
                    e[0].elo += (40 * (0.0 - expectedA)).toInt()
                    e[1].elo += (40 * (1.0 - expectedB)).toInt()
                }
            }
        }

        println("New ELO-rating:")
        println("${e[0].name}: ${e[0].elo}")
        println("${e[1].name}: ${e[1].elo}")

        return true
    }

}

class Engine(val name: String, val command: String, var elo: Int = 1000) {

    var running = false

    private lateinit var process: Process
    lateinit var inr: BufferedReader
    private lateinit var err: BufferedReader
    private lateinit var out: BufferedWriter

    fun start(){
        if(running) return

        benchmark("Starting $name"){
            process = ProcessBuilder(command.split(" ")).start()
            inr = process.inputStream.bufferedReader()
            err = process.errorStream.bufferedReader()
            out = process.outputStream.bufferedWriter()
        }

        running = true
    }

    fun stop(){
        if(!running) return

        write("exit")
        process.destroy()

        running = false
    }

    fun write(s: String): Boolean {
        try {
            out.write(s)
            out.newLine()
            out.flush()

            return true
        } catch(e: Exception){
            println("\n***")
            println("Got exception while trying to write to program stdin: $e")
            println("\nResponsible program:")
            println(name)
            println("\nFull stack trace:")
            e.printStackTrace()
            println("***")

            return false
        }
    }

    fun read(): String? {
        var str = inr.readLine()

        if(str == null) {
            return str
        }

        while(str.startsWith("!dbg")){
            println("> $name: ${str.substring(5)}")
            str = inr.readLine()
        }
        return str
    }

    fun waitFor(){
        benchmark("Waiting for $name") {
            while (!inr.ready() && !err.ready()) {
            }
            if (err.ready()) {
                println(err.readText())
            }
        }
    }

}