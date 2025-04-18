package net.torvald.terrarum

import net.torvald.random.HQRNG
import java.util.*

internal interface RNGConsumer {

    val RNG: HQRNG

    fun loadFromSave(ingame: IngameInstance, s0: Long, s1: Long) {
        RNG.setSeed(s0, s1)
    }

}
