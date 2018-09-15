package net.torvald.terrarum.modulebasegame

import net.torvald.random.HQRNG
import java.util.*

internal interface RNGConsumer {

    val RNG: HQRNG

    fun loadFromSave(s0: Long, s1: Long) {
        RNG.reseed(s0, s1)
    }

}
