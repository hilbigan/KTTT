package mcts

import main.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Semaphore
import kotlin.math.ln
import kotlin.math.sqrt

class MCTS(
    board: Bitboard,
    val time: Int,
    private val threads: Int,
    val player: Int,
    val debug: Boolean = false,
    val persistent: Boolean = false
) {

    var root: Node

    init {
        root = Node(board.clone(), parent = null)
    }

    private fun start(): Int {
        val threads = (0 until threads).map { MCTSThread(this, it) }

        if(root.children.isEmpty()) {
            root.generateMoves(true)
        } else {
            root.annotateMoves()
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val bestMove = root.children.maxBy { it.n }!!
        if(debug) {
            val avg = root.children.sumBy { it.n } / root.children.size.toDouble()
            val stdev = sqrt(root.children.map { (avg - it.n) * (avg - it.n) }.sum() / root.children.size.toDouble())
            println("Best move: ${Bitboard.moveToString(bestMove.move)} (${bestMove.q}/${bestMove.n}); Total iterations: ${threads.map { it.iter }.sum()}; Avg: $avg; Stdev: $stdev; MaxDepth: ${threads.map { it.maxDepth }.max()}")
        }

        if(persistent){
            bestMove.parent = null
            root = bestMove
        }

        return bestMove.move
    }

    fun nextMove(newPosition: Bitboard? = null): Int {
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
                if(debug) println("Found new position in existing tree. New root node is $newRoot (${newRoot.q}/${newRoot.n})")
                root = newRoot
            } else {
                if(debug) println("No matching position found, creating new root node (old tree will be lost).")
                root = Node(newPosition, parent = null)
            }
        } else {
            if(debug) println("Board position has not changed. Using already existant root node.")
        }

        return start()
    }

    class MCTSThread(val parent: MCTS, val id: Int) : Thread() {

        var iter = 0
        var maxDepth = 0

        override fun run() {
            iter = 0
            //println("Thread $id starting...")
            val now = System.currentTimeMillis()

            while(System.currentTimeMillis() - now < parent.time){
                var depth = 0
                var child: Node? = parent.root.traverse()//parent.root.traverseAndRollout(parent.player)
                child!!.virtualLoss = true
                child.parent?.virtualLoss = true
                val result = child.rollout(parent.player)
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
            //println("Thread $id stopped. Iterations: $iter")
        }

    }

    class Node(val board: Bitboard, var move: Int = 0, var parent: Node?) {
        val children: CopyOnWriteArrayList<Node> = CopyOnWriteArrayList()
        var q: Int = 0
        var n: Int = 0
        
        val mutex = Semaphore(1, true)

        var virtualLoss: Boolean = false
            set(value) = synchronized(this){
                field = value
            }

        fun generateMoves(annotated: Boolean = false) = synchronized(children){
            if(children.size > 0){
                return
            }

            board.getAllMoves().forEach {
                board.makeMove(it)
                val clone = board.clone()
                if(!annotated)
                    children.add(Node(clone, parent = this))
                else
                    children.add(Node(clone, parent = this, move = it/*, depth = depth + 1*/))
                board.undoMove(it)
            }
        }

        fun annotateMoves(){
            board.getAllMoves().forEach {
                board.makeMove(it)
                children.first { it.board == board }.move = it
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

        fun rollout(player: Int): Int {
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
            if(state is Won && state.who == player){
                return 1
            } else if(state is Won) {
                return -1
            } else {
                return 0
            }
        }

        fun update(result: Int) = synchronized(this){
            q += result
            n++
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