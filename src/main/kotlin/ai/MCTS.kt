package ai

import main.Bitboard
import main.format
import main.random
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.ln
import kotlin.math.sqrt

class MCTS(
    /**
     * The starting position.
     */
    board: Bitboard,

    /**
     * Thinking time
     */
    val time: Int,

    /**
     * Number of threads. For NN-Strategy, use less threads (until we got nd4j running on GPU)
     */
    private val numThreads: Int,

    /**
     * The player that the AI will find moves for.
     */
    val player: Int,

    /**
     * Print debug messages?
     */
    val debug: Boolean = false,

    /**
     * Wether to store and keep the game tree between moves. If enabled, old computations can be reused.
     */
    val persistent: Boolean = true,

    /**
     * Calculate moves with less CPU-usage while waiting for opponents move.
     * Adjustable with PONDER_TIMEOUT_NS.
     * Should be disabled for NN-Strategy.
     */
    val ponder: Boolean = true,

    /**
     * The strategy, random playout by default
     */
    val strategy: Strategy = RandomPlayStrategy()
) {
    var root: Node
    val threads = mutableListOf<MCTSThread>()
    var changingTree = false
    private var pondering = false

    init {
        root = Node(board.clone(), parent = null)
    }

    private fun start() {
        log("Starting $numThreads threads with ${strategy.javaClass.name} strategy.")
        threads.clear()
        threads.addAll((0 until numThreads).map { MCTSThread(this, it) })

        threads.forEach { it.start() }

        if(!ponder) { // If ponder disabled, wait for all threads to finish
            threads.forEach { it.join() }
        } else { // else, wait for the time to run out and leave the threads running
            Thread.sleep(time.toLong())
        }
    }

    fun stop(){
        threads.forEach { it.stop = true }
        threads.forEach { it.join() }
    }

    private fun getBestMove(): Int {
        val bestMove = root.children.maxBy { it.n }!!
        if(debug) {
            val avg = root.children.sumBy { it.n } / root.children.size.toDouble() / bestMove.n * 100
            val stdev = sqrt(root.children.map { (avg - it.n) * (avg - it.n) }.sum() / root.children.size.toDouble()) / bestMove.n * 100
            log("Best move: ${Bitboard.moveToString(bestMove.move)} (${bestMove.q.format(2)}/${bestMove.n}); Total iterations: ${threads.map { it.iter }.sum()}; Avg: ${avg.format(1)}%; Stdev: ${stdev.format(1)}%; MaxDepth: ${threads.map { it.maxDepth }.max()}")
        }

        if(persistent){
            changingTree = true

            bestMove.parent = null
            root = bestMove

            changingTree = false
        }

        if(ponder) {
            pondering = true
            log("now pondering")
        }

        return bestMove.move
    }

    fun nextMove(newPosition: Bitboard? = null): Int {
        changingTree = true
        if(!persistent){
            root = Node(newPosition!!, parent = null)
        } else if(newPosition != null && newPosition != root.board){
            val queue = ArrayDeque<Node>()
            var newRoot: Node? = null
            queue.addAll(root.children)
            while(queue.isNotEmpty()){
                val node = queue.poll()
                if(node.board == newPosition){
                    newRoot = node
                    break
                } else {
                    queue.addAll(node.children)
                }
            }

            if(newRoot != null){
                log("Found new position in existing tree. New root node is $newRoot (${newRoot.q.format(2)}/${newRoot.n})")
                root = newRoot
            } else {
                log("No matching position found, creating new root node (old tree will be lost).")
                root = Node(newPosition, parent = null)
            }
        } else {
            log("Board position has not changed. Using already existant root node.")
        }

        root.generateMoves()

        changingTree = false

        if(ponder && pondering) {
            pondering = false
            log("stopped pondering")
        }

        if(!threads.isEmpty() && threads.all { it.running }){
            Thread.sleep(time.toLong())
            return getBestMove()
        } else {
            start()
            return getBestMove()
        }
    }

    class MCTSThread(val parent: MCTS, val id: Int) : Thread() {
        var iter = 0
        var maxDepth = 0
        var stop = false
        var running = false

        override fun run() {
            running = true
            iter = 0

            if(parent.debug) println("!dbg Thread $id starting...")

            val now = System.currentTimeMillis()

            while((System.currentTimeMillis() - now < parent.time) || (parent.ponder && !stop)){
                while (parent.changingTree){
                    Thread.sleep(0, 50)
                }

                if(parent.pondering){ //TODO improve this behaviour?
                    Thread.sleep(0, PONDER_TIMEOUT_NS)
                }

                var depth = 0
                var child: Node? = parent.root.traverse()
                child!!.virtualLoss = true
                child.parent?.virtualLoss = true
                val result = child.rollout(parent.player, parent.strategy)
                child.virtualLoss = false
                child.parent?.virtualLoss = false

                while(child != null){
                    child.update(result)
                    child = child.parent
                    depth++
                }
                iter++

                if(depth > maxDepth)
                    maxDepth = depth
            }

            running = false
            if(parent.debug) println("!dbg Thread $id stopped.")
        }

    }

    class Node(val board: Bitboard, var move: Int = 0, var parent: Node?) {
        val children: CopyOnWriteArrayList<Node> = CopyOnWriteArrayList()
        var q: Double = 0.0
        var n: Int = 0

        var virtualLoss: Boolean = false
            set(value) = synchronized(this){
                field = value
            }

        fun generateMoves() = synchronized(children){
            if(children.size > 0){
                return
            }

            board.getAllMoves().forEach {
                board.makeMove(it)

                val clone = board.clone()

                children.add(Node(clone, parent = this, move = it))

                board.undoMove(it)
            }
        }

        fun isFullyExpanded(): Boolean = synchronized(children) {
            children.all { it.n > 0 } // All children visited
        }

        fun traverse(): Node {
            if(children.size == 0)
                generateMoves()

            if(children.size > 0 && isFullyExpanded()){
                val bestChild = synchronized(children) {
                    children.maxBy { it.getUCT(this) }!!
                }

                return bestChild.traverse()
            } else {
                val unvisitedChildren = synchronized(children) {
                    children.filter { it.n == 0 }
                }

                if(unvisitedChildren.isNotEmpty()){
                    return unvisitedChildren[random(unvisitedChildren.size)]
                } else {
                    return this
                }
            }
        }

        fun rollout(player: Int, strategy: Strategy): Double {
            return strategy.rollout(board, player)
        }

        fun update(result: Double) = synchronized(this){
            q += result
            n += 1
        }

        private fun getUCT(parent: Node): Double = synchronized(this){
            val virtualLossScalar = if(virtualLoss) 0.5 else 1.0
            return ((q / n) + UCT_EXPLORATION_SCALAR * sqrt(ln(parent.n.toDouble()) / n)) * virtualLossScalar
        }
    }

    companion object {
        val UCT_EXPLORATION_SCALAR = sqrt(2.0)
        const val PONDER_TIMEOUT_NS = 1
    }

    private fun log(s: String){
        if(debug) {
            println("!dbg $s")
        }
    }
}