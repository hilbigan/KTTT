package main

import main.Bitboard.Companion.DIAGS

private val CROSS = DIAGS[0] or DIAGS[1]
private val MIDS = CROSS.inv()
private val CROSS_SCALE = 5
private val MIDS_SCALE = 3

fun rate(board: Bitboard, player: Int): Int {
    if(board.getTotalPopcnt() < 10) return 0
    val meta = board.getMetaField()
    val score = intArrayOf(0, 0)
    for (p in 0..1){
        score[p] += popcnt(meta[p] and CROSS) * CROSS_SCALE
        score[p] += popcnt(meta[p] and MIDS) * MIDS_SCALE
        score[p] += popcnt(meta[p] and 0b10_000) * CROSS_SCALE
    }
    return (score[0] - score[1]) * (if(player == 0) 1 else -1)
}