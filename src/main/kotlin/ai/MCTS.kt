package ai

import main.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.ln
import kotlin.math.sqrt

class MCTS(
    board: Bitboard,
    val time: Int,
    private val numThreads: Int,
    val player: Int,
    val debug: Boolean = false,
    val persistent: Boolean = true,
    val ponder: Boolean = true
) {

    var root: Node
    val strategy = Strategy.getRandom()
    val threads = mutableListOf<MCTSThread>()
    var changingTree = false

    init {
        root = Node(board.clone(), parent = null)
    }

    private fun start() {
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
            println("!dbg Best move: ${Bitboard.moveToString(bestMove.move)} (${bestMove.q}/${bestMove.n}); Total iterations: ${threads.map { it.iter }.sum()}; Avg: ${avg.format(1)}%; Stdev: ${stdev.format(1)}%; MaxDepth: ${threads.map { it.maxDepth }.max()}")
        }

        if(persistent){
            changingTree = true

            bestMove.parent = null
            root = bestMove

            changingTree = false
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
                if(debug) println("!dbg Found new position in existing tree. New root node is $newRoot (${newRoot.q}/${newRoot.n})")
                root = newRoot
            } else {
                if(debug) println("!dbg No matching position found, creating new root node (old tree will be lost).")
                root = Node(newPosition, parent = null)
            }
        } else {
            if(debug) println("!dbg Board position has not changed. Using already existant root node.")
        }

        root.generateMoves()

        changingTree = false

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

            println("!dbg Thread $id starting...")

            val now = System.currentTimeMillis()

            while((System.currentTimeMillis() - now < parent.time) || (parent.ponder && !stop)){
                while (parent.changingTree){
                    Thread.sleep(0, 50)
                }

                var depth = 0
                var child: Node? = parent.root.traverse()//parent.root.traverseAndRollout(parent.player)
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
            println("!dbg Thread $id stopped.")
        }

    }

    class Node(val board: Bitboard, var move: Int = 0, var parent: Node?) {
        val children: CopyOnWriteArrayList<Node> = CopyOnWriteArrayList()
        var q: Int = 0
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

        fun rollout(player: Int, strategy: Strategy): Int {
            val boardCopy = board.clone()
            var moves = board.getTotalPopcnt()

            while(!boardCopy.isGameOver()){
                val legalMoves = boardCopy.getAllMoves()
                val move = legalMoves[random(legalMoves.size)]
                boardCopy.makeMove(move)
                if(boardCopy.validField == Bitboard.ALL_FIELDS && legalMoves.size > 10 && moves < 25){
                    boardCopy.undoMove(move)
                }
                moves++
            }


            val state = boardCopy.getGameState()
            if(state is Won && state.who == player){ // Won
                if(strategy.isAchieved(boardCopy, player)){
                    return 2
                } else {
                    return 1
                }
            } else if(state is Won) { // Lost
                    return -1
            } else { // Tie
                return 0
            }
        }

        fun update(result: Int) = synchronized(this){
            q += result
            n += 1
        }

        private fun getUCT(parent: Node): Double = synchronized(this){
            val virtualLossScalar = if(virtualLoss) 0.5 else 1.0
            return ((q.toDouble() / n) + UCT_EXPLORATION_SCALAR * sqrt(ln(parent.n.toDouble()) / n)) * virtualLossScalar
        }
    }

    companion object {
        val UCT_EXPLORATION_SCALAR = sqrt(2.0)
    }
}