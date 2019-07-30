package main

class Bitboard(var validField: Int = ALL_FIELDS, var board: Array<IntArray> = arrayOf(IntArray(9), IntArray(9)), var turn: Int = 0) {

    var cachedMetaField: IntArray = intArrayOf(-1, -1)
    var dirty = true

    fun clone(): Bitboard {
        val boardCopy = arrayOf(IntArray(9), IntArray(9))
        for(i in 0..1){
            for(j in 0..8){
                boardCopy[i][j] = board[i][j]
            }
        }
        return Bitboard(validField, boardCopy, turn)
    }

    fun taken(pos: Int): Boolean {
        return (board[0][toIndex(field(pos))] or board[1][toIndex(field(pos))]) and square(pos) != 0
    }

    fun takenBy(player: Int, pos: Int): Boolean {
        return board[player][toIndex(field(pos))] and square(pos) != 0
    }

    fun isLegal(move: Int): Boolean {
        return field(move) and validField != 0 && !taken(move) && !fieldIsBlocked(toIndex(field(move)))
    }

    fun buildMove(field: Int, square: Int): Int {
        return ((1 shl field) shl 16) or (1 shl square) or (if(validField == ALL_FIELDS) ALL_FIELDS_LEGAL else 0)
    }

    fun isChance(move: Int): Boolean = fieldIsBlocked(toIndex(square(move)))

    fun makeMove(move: Int){
        val field = field(move)
        val square = square(move)
        board[turn][toIndex(field)] = board[turn][toIndex(field)] or square

        if(fieldIsBlocked(toIndex(square)) || isWon(field)){
            validField = ALL_FIELDS
        } else {
            validField = square
        }
        turn = 1 - turn
        dirty = true
    }

    fun getAllMoves(): List<Int> {
        val list = mutableListOf<Int>()

        for(i in 0..8){
            if(((1 shl i) and validField) != 0){
                for(s in 0..8){
                    val move = (1 shl s) or ((1 shl i) shl 16)
                    if(!taken(move) && !fieldIsBlocked(i)){
                        list.add(move or (if(validField == ALL_FIELDS) ALL_FIELDS_LEGAL else 0))
                    }
                }
            }
        }

        return list
    }

    fun undoMove(move: Int){
        val field = field(move)
        val square = square(move)
        board[1 - turn][toIndex(field)] = board[1 - turn][toIndex(field)] and square.inv()
        if((move and ALL_FIELDS_LEGAL) != 0){
            validField = ALL_FIELDS
        } else {
            validField = field
        }
        turn = 1 - turn
        dirty = true
    }

    fun fieldIsBlocked(field: Int): Boolean {
        val whiteField = board[WHITE][field]
        val blackField = board[BLACK][field]
        return isWon(whiteField) || isWon(blackField) || isTied(whiteField or blackField)
    }

    fun getGameState(): GameState {
        val metaField = getMetaField()
        return if(isWon(metaField[WHITE])) Won(WHITE)
        else if(isWon(metaField[BLACK])) Won(BLACK)
        else if(isGameTied()) Tied()
        else Running()
    }

    fun isGameOver(): Boolean {
        val sum = board[0].map { popcnt(it) }.sum()
        if(sum < 9) return false

        val metaField = getMetaField()
        return isWon(metaField[WHITE]) || isWon(metaField[BLACK]) || isGameTied()
    }

    fun hasPlayerWon(player: Int) = isWon(getMetaField()[player])

    fun isGameTied(): Boolean =
        (0..8).all {
            isWon(board[0][it]) || isWon(board[1][it]) || isTied(
                board[0][it] or board[1][it]
            )
        }

    fun getMetaField(): IntArray {
        if(!dirty)
            return cachedMetaField

        val field = IntArray(2)
        for (p in 0..1){
            field[p] = ((isWon(board[p][0])).toInt() shl 8) or ((isWon(board[p][1]))
                .toInt() shl 7) or ((isWon(board[p][2]))
                .toInt() shl 6) or ((isWon(board[p][3]))
                .toInt() shl 5) or ((isWon(board[p][4]))
                .toInt() shl 4) or ((isWon(board[p][5]))
                .toInt() shl 3) or ((isWon(board[p][6]))
                .toInt() shl 2) or ((isWon(board[p][7]))
                .toInt() shl 1) or ((isWon(board[p][8]))
                .toInt() shl 0)
        }

        cachedMetaField = field
        dirty = false
        return field
    }

    fun toPrettyString(): String {
        var ret = ""
        for(row in (0..8).step(3)){
            for(subRow in (0..8).step(3)){
                for(i in (0..1))
                    ret += "${toSymbol(buildMove(row + i, subRow))}|${toSymbol(buildMove(row + i, subRow + 1))}|${toSymbol(buildMove(row + i, subRow + 2))} || "
                ret += "${toSymbol(buildMove(row + 2, subRow))}|${toSymbol(buildMove(row + 2, subRow + 1))}|${toSymbol(buildMove(row + 2, subRow + 2))}\n"
            }
            if(row < 6) ret += "========    ========    ========\n"
        }
        ret += "GameState = ${getGameState()}"
        return ret
    }

    fun toSymbol(pos: Int): String {
        if(isWon(board[0][toIndex(field(pos))])) {
            return if(square(pos) and (DIAGS[0] or DIAGS[1]) != 0){
                "XX"
            } else {
                "  "
            }
        }
        if(isWon(board[1][toIndex(field(pos))])){
            return if(square(pos) and (ROWS[0] or ROWS[2] or O_O) != 0){
                "OO"
            } else {
                "  "
            }
        }
        return if(!taken(pos)){
            if(isLegal(pos)){
                "[]"
            } else {
                "  "
            }
        } else if(takenBy(WHITE, pos)) {
            "X "
        } else {
            "O "
        }
    }

    override fun equals(other: Any?): Boolean {
        if(other == null || other !is Bitboard) return false

        if(turn != other.turn) return false
        if(validField != other.validField) return false
        if(!board.contentDeepEquals(other.board)) return false

        return true
    }

    companion object {

        val WHITE = 0
        val BLACK = 1

        val FIELD = 0x1FF shl 16
        val SQUARE = 0xFFFF
        val ALL_FIELDS_LEGAL = 1 shl 25

        /**
         * 0o777
         */
        val ALL_FIELDS = oct(777)

        val DIAGS = intArrayOf(oct(421), oct(124))
        val ROWS = intArrayOf(oct(700), oct(70), oct(7))
        val COLS = intArrayOf(oct(111), oct(222), oct(444))
        val O_O = oct(50)

        fun moveToString(move: Int): String {
            return "[a${((move and ALL_FIELDS_LEGAL) shr 25)} f${toIndex(
                field(move)
            )} s${toIndex(square(move))}]"
        }

        fun fromString(str: String): Bitboard {
            val board = Bitboard()
            var field = 0
            var square = 0
            for(c in str){
                when(c){
                    '|' -> { field++; square=0 }
                    'x' -> { board.board[WHITE][field] = board.board[WHITE][field] or (1 shl square); square++ }
                    'o' -> { board.board[BLACK][field] = board.board[BLACK][field] or (1 shl square); square++ }
                    else-> square++
                }
            }
            board.turn = board.calculateTurn()
            return board
        }

        fun moveGen(board: Bitboard, depth: Int): Long {
            if(board.isGameOver()) return 0
            val moves = board.getAllMoves()
            return moves.size + if(depth > 0){
                moves.map {
                    board.makeMove(it)
                    val ret = moveGen(board, depth - 1)
                    board.undoMove(it)
                    ret
                }.sum()
            } else { 0 }
        }

        fun field(i: Int) = (i and FIELD) shr 16
        fun square(i: Int) = (i and SQUARE)

        /**
         * Basically fast log2 for powers of two
         */
        fun toIndex(i: Int) = 31 - Integer.numberOfLeadingZeros(i)

        fun isWon(field: Int) =
            (field and DIAGS[0]) == DIAGS[0]
                    || (field and DIAGS[1]) == DIAGS[1]
                    || (field and  ROWS[0]) ==  ROWS[0]
                    || (field and  ROWS[1]) ==  ROWS[1]
                    || (field and  ROWS[2]) ==  ROWS[2]
                    || (field and  COLS[0]) ==  COLS[0]
                    || (field and  COLS[1]) ==  COLS[1]
                    || (field and  COLS[2]) ==  COLS[2]

        fun isTied(field: Int) = field == ALL_FIELDS
    }

    fun calculateTurn(): Int {
        val popc0 = board[0].map { popcnt(it) }.sum()
        val popc1 = board[1].map { popcnt(it) }.sum()
        return if(popc0 == popc1) 0 else 1
    }

    fun getTotalPopcnt(): Int = board[0].sumBy { popcnt(it) } + board[1].sumBy { popcnt(it) }

    override fun hashCode(): Int {
        var result = validField
        result = 31 * result + board.contentDeepHashCode()
        result = 31 * result + turn
        return result
    }
}