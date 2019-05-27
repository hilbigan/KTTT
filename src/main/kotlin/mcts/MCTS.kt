package mcts

import main.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Semaphore
import kotlin.math.ln
import kotlin.math.sqrt

class MCTS(
    board: Bitboard,
    val time: Int,
    private val threads: Int,
    val player: Int,
    val debug: Boolean = false
) {

    val root: Node

    init {
        root = Node(board.clone(), parent = null)
    }

    fun start(): Int {
        val threads = (0 until threads).map { MCTSThread(this, it) }

        root.generateMoves(true)

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val bestMove = root.children.maxBy { it.n }!!
        if(debug) {
            val avg = root.children.sumBy { it.n } / root.children.size.toDouble()
            val stdev = sqrt(root.children.map { (avg - it.n) * (avg - it.n) }.sum() / root.children.size.toDouble())
            println("Best move: ${Bitboard.moveToString(bestMove.move)} (${bestMove.q}/${bestMove.n}); Total iterations: ${threads.map { it.iter }.sum()}; Avg: $avg; Stdev: $stdev")
        }
        return bestMove.move
    }

    class MCTSThread(val parent: MCTS, val id: Int) : Thread() {

        var iter = 0

        override fun run() {
            iter = 0
            //println("Thread $id starting...")
            val now = System.currentTimeMillis()

            while(System.currentTimeMillis() - now < parent.time){
                var child: Node? = parent.root.traverse()//parent.root.traverseAndRollout(parent.player)
                child!!.virtualLoss = true
                child.parent?.virtualLoss = true
                val result = child.rollout(parent.player)
                child.virtualLoss = false
                child.parent?.virtualLoss = false

                while(child != null){
                    child.update(result)
                    child = child.parent
                }
                iter++
            }
            //println("Thread $id stopped. Iterations: $iter")
        }

    }

    class Node(val board: Bitboard, var q: Int = 0, var n: Int = 0, val children: CopyOnWriteArrayList<Node> = CopyOnWriteArrayList(), val move: Int = 0, val parent: Node?) {
        
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