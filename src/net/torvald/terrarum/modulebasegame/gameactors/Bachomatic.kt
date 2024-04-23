package net.torvald.terrarum.modulebasegame.gameactors

/**
 * Created by minjaesong on 2024-04-18.
 */
interface Bachomatic {

    operator fun invoke(): List<Long>

}

object PreludeInCMaj : Bachomatic {

    private val TICK_DIVISOR = 10

    override fun invoke() = List(16*TICK_DIVISOR) { 0L } +
            prel(24,28,31,36,40) +
            prel(24,26,33,38,41) +
            prel(23,26,31,38,41) +
            prel(24,28,31,36,40) +
            prel(24,28,33,40,45) +
            prel(24,26,30,33,38) +
            prel(23,26,31,38,43) +
            prel(23,24,28,31,36) +
            prel(21,24,28,31,36) +
            prel(14,21,26,30,36) +
            prel(19,23,26,31,35) +
            prel(19,22,28,31,37) +
            prel(17,21,26,33,38) +
            prel(17,20,26,29,35) +
            prel(16,19,24,31,36) +
            prel(16,17,21,24,29) +
            prel(14,17,21,24,29) +
            prel( 7,14,19,23,29) +
            prel(12,16,19,24,28) +
            prel(12,19,22,24,28) +
            prel( 5,17,21,24,28) +
            prel( 6,12,21,24,27) +
            prel( 8,17,23,24,26) +
            prel( 7,17,19,23,26) +
            prel( 7,16,19,24,28) +
            prel( 7,14,19,24,29) +
            prel( 7,14,19,23,29) +
            prel( 7,15,21,24,30) +
            prel( 7,16,19,24,31) +
            prel( 7,14,19,24,29) +
            prel( 7,14,19,23,29) +
            prel( 0,12,19,22,28) +
            end1( 0,12,17,21,24,29,21,17,14) +
            end2( 0,11,31,35,38,41,26,29,28) +
            end3( 0,12,28,31,36) + List(16*TICK_DIVISOR - 5) { 0L }

    private fun prel(n1: Int, n2: Int, n3: Int, n4: Int, n5: Int): List<Long> {
        return toPianoRoll(
            1L shl n1 to TICK_DIVISOR+2, 1L shl n2 to TICK_DIVISOR, 1L shl n3 to TICK_DIVISOR, 1L shl n4 to TICK_DIVISOR-1,
            1L shl n5 to TICK_DIVISOR, 1L shl n3 to TICK_DIVISOR, 1L shl n4 to TICK_DIVISOR, 1L shl n5 to TICK_DIVISOR,
            1L shl n1 to TICK_DIVISOR, 1L shl n2 to TICK_DIVISOR, 1L shl n3 to TICK_DIVISOR, 1L shl n4 to TICK_DIVISOR-1,
            1L shl n5 to TICK_DIVISOR, 1L shl n3 to TICK_DIVISOR, 1L shl n4 to TICK_DIVISOR, 1L shl n5 to TICK_DIVISOR)
    }

    private fun end1(n1: Int, n2: Int, n3: Int, n4: Int, n5: Int, n6: Int, n7: Int, n8: Int, n9: Int): List<Long> {
        return toPianoRoll(
            1L shl n1 to TICK_DIVISOR+2, 1L shl n2 to TICK_DIVISOR, 1L shl n3 to TICK_DIVISOR, 1L shl n4 to TICK_DIVISOR-1,
            1L shl n5 to TICK_DIVISOR, 1L shl n6 to TICK_DIVISOR, 1L shl n5 to TICK_DIVISOR, 1L shl n4 to TICK_DIVISOR,
            1L shl n5 to TICK_DIVISOR, 1L shl n7 to TICK_DIVISOR, 1L shl n8 to TICK_DIVISOR, 1L shl n7 to TICK_DIVISOR-1,
            1L shl n8 to TICK_DIVISOR, 1L shl n9 to TICK_DIVISOR, 1L shl n8 to TICK_DIVISOR, 1L shl n9 to TICK_DIVISOR)
    }

    private fun end2(n1: Int, n2: Int, n3: Int, n4: Int, n5: Int, n6: Int, n7: Int, n8: Int, n9: Int): List<Long> {
        return toPianoRoll(
            1L shl n1 to TICK_DIVISOR+2, 1L shl n2 to TICK_DIVISOR+1, 1L shl n3 to TICK_DIVISOR+1, 1L shl n4 to TICK_DIVISOR+1,
            1L shl n5 to TICK_DIVISOR+1, 1L shl n6 to TICK_DIVISOR+2, 1L shl n5 to TICK_DIVISOR+2, 1L shl n4 to TICK_DIVISOR+2,
            1L shl n5 to TICK_DIVISOR+3, 1L shl n4 to TICK_DIVISOR+3, 1L shl n3 to TICK_DIVISOR+4, 1L shl n4 to TICK_DIVISOR+4,
            1L shl n7 to TICK_DIVISOR+6, 1L shl n8 to TICK_DIVISOR+8, 1L shl n9 to TICK_DIVISOR+12, 1L shl n7 to TICK_DIVISOR+24)
    }

    private fun end3(vararg ns: Int): List<Long> {
        return ns.map { 1L shl it } // arpeggiate
    }

    fun toPianoRoll(vararg noteAndLen: Pair<Long, Int>): List<Long> {
        val ret = MutableList<Long>(noteAndLen.sumOf { it.second }) { 0 }
        var c = 0
        noteAndLen.forEach { (note, len) ->
            ret[c] = note
            c += len
        }
        return ret
    }

    fun toPianoRoll(vararg notes: Long) = List<Long>(notes.size * TICK_DIVISOR) {
        if (it % TICK_DIVISOR == 0) notes[it / TICK_DIVISOR] else 0
    }

}

object PreludeInCshMaj : Bachomatic {
    private val TICK_DIVISOR = 8

    private val TICK_GAP = List(TICK_DIVISOR - 1) { 0L }

    override fun invoke() = List(16* TICK_DIVISOR) { 0L } +
            n321232(32,37,41,13,25) +
            n121212(37,42,15,25) +
            n121212(37,44,17,25) +
            n121212(37,46,18,25) +
            n121212(37,44,17,25) +
            n321231(39,41,42,15,24) +
            n321231(37,39,41,13,25) +
            tqSSSS454321(34,36,37,39,41,20,22,24) +

            p321232(15,20,24,32,44) +
            p121212(20,25,34,44) +
            p121212(20,27,36,44) +
            p121212(20,29,37,44) +
            p121212(20,27,36,44) +
            p321231(22,24,25,32,43) +
            p321231(20,22,24,32,44) +
            tqSSSS454321(17,18,20,22,23,39,41,42) +

            n321232(34,39,42,15,27) +
            n121212(39,44,17,27) +
            n121212(39,46,18,27) +
            n121212(39,47,20,27) +
            n121212(39,46,18,27) +
            n321231(41,42,44,17,26) +
            n321231(39,41,42,15,27) +
            tqSSSS454321(36,37,39,41,42,22,24,25) +

            p321232(17,22,25,34,46) +
            p121212(22,27,36,46) +
            p121212(22,29,37,46) +
            p121212(22,30,39,46) +
            p121212(22,29,37,46) +
            p321231(24,25,27,36,45) +
            p321231(22,24,25,34,46) +
            tqQQ321231(24,26,28,44,43) +

            tQQQ212321b(28,29,31,44,32,44) +
            tqQQ321231(22,24,26,43,41) +
            tQQQ212321(39,41,42,27,15,27) +
            tqQQ321231(41,43,45,25,24) +
            tQQQ212321b(45,46,48,25,13,25) +
            tqQQ321231(39,41,43,24,22) +
            tQQQ321231(20,22,24,44,32,44) +

            /* 40 */
            tqQQ321231(22,24,26,42,41) +
            tQQQ212321b(26,27,29,42,37,42) +
            tqQQ321231(20,22,24,41,39) +
            tQQQ321231(37,39,41,25,13,25) +
            tqQQ321231(39,41,43,28,27) +
            tQQQ212321b(43,44,46,23,11,23) +
            tqQQ321231(37,39,41,22,20) +

            /* 47 */



            List(16* TICK_DIVISOR) { 0L }

    private fun n321232(n1: Int, n2: Int, n3: Int, p1: Int, p2: Int) =
        listOf((1L shl n3) or (1L shl p1)) + TICK_GAP +
        listOf( 1L shl n2) + TICK_GAP +
        listOf( 1L shl n1) + TICK_GAP +
        listOf( 1L shl n2) + TICK_GAP +
        listOf((1L shl n3) or (1L shl p2)) + TICK_GAP +
        listOf( 1L shl n2) + TICK_GAP

    private fun n321231(n1: Int, n2: Int, n3: Int, p1: Int, p2: Int) =
        listOf((1L shl n3) or (1L shl p1)) + TICK_GAP +
            listOf( 1L shl n2) + TICK_GAP +
            listOf( 1L shl n1) + TICK_GAP +
            listOf( 1L shl n2) + TICK_GAP +
            listOf((1L shl n3) or (1L shl p2)) + TICK_GAP +
            listOf( 1L shl n1) + TICK_GAP

    private fun n121212(n1: Int, n2: Int, p1: Int, p2: Int) =
        listOf((1L shl n2) or (1L shl p1)) + TICK_GAP +
        listOf( 1L shl n1) + TICK_GAP +
        listOf( 1L shl n2) + TICK_GAP +
        listOf( 1L shl n1) + TICK_GAP +
        listOf((1L shl n2) or (1L shl p2)) + TICK_GAP +
        listOf( 1L shl n1) + TICK_GAP

    private fun tqSSSS454321(n1: Int, n2: Int, n3: Int, n4: Int, n5: Int, p1: Int, p2: Int, p3: Int) =
        listOf( 1L shl n4) + TICK_GAP +
        listOf( 1L shl n5) + TICK_GAP +
        listOf((1L shl n4) or (1L shl p3)) + TICK_GAP +
        listOf((1L shl n3) or (1L shl p2)) + TICK_GAP +
        listOf((1L shl n2) or (1L shl p1)) + TICK_GAP +
        listOf((1L shl n1) or (1L shl p2)) + TICK_GAP


    private fun p321232(n1: Int, n2: Int, n3: Int, p1: Int, p2: Int) = n321232(n1, n2, n3, p1, p2)
    private fun p321231(n1: Int, n2: Int, n3: Int, p1: Int, p2: Int) = n321231(n1, n2, n3, p1, p2)
    private fun p121212(n1: Int, n2: Int, p1: Int, p2: Int) = n121212(n1, n2, p1, p2)

    private fun tqQQ321231(p1: Int, p2: Int, p3: Int, n1: Int, n2: Int) =
        listOf( 1L shl p3) + TICK_GAP +
        listOf( 1L shl p2) + TICK_GAP +
        listOf((1L shl p1) or (1L shl n1)) + TICK_GAP +
        listOf( 1L shl p2) + TICK_GAP +
        listOf((1L shl p3) or (1L shl n2)) + TICK_GAP +
        listOf( 1L shl p1) + TICK_GAP

    private fun tQQQ321231(p1: Int, p2: Int, p3: Int, n1: Int, n2: Int, n3: Int) =
        listOf((1L shl p3) or (1L shl n1)) + TICK_GAP +
        listOf( 1L shl p2) + TICK_GAP +
        listOf((1L shl p1) or (1L shl n2)) + TICK_GAP +
        listOf( 1L shl p2) + TICK_GAP +
        listOf((1L shl p3) or (1L shl n3)) + TICK_GAP +
        listOf( 1L shl p1) + TICK_GAP

    private fun tQQQ212321b(p1: Int, p2: Int, p3: Int, n1: Int, n2: Int, n3: Int) =
        listOf((1L shl p2) or (1L shl n1)) + TICK_GAP +
        listOf( 1L shl p1) + TICK_GAP +
        listOf((1L shl p2) or (1L shl n2)) + TICK_GAP +
        listOf( 1L shl p3) + TICK_GAP +
        listOf((1L shl p2) or (1L shl n3)) + TICK_GAP +
        listOf( 1L shl(p1-1)) + TICK_GAP

    private fun tQQQ212321(p1: Int, p2: Int, p3: Int, n1: Int, n2: Int, n3: Int) =
        listOf((1L shl p2) or (1L shl n1)) + TICK_GAP +
        listOf( 1L shl p1) + TICK_GAP +
        listOf((1L shl p2) or (1L shl n2)) + TICK_GAP +
        listOf( 1L shl p3) + TICK_GAP +
        listOf((1L shl p2) or (1L shl n3)) + TICK_GAP +
        listOf( 1L shl p1) + TICK_GAP



}