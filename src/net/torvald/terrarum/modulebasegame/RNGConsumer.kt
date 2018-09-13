package net.torvald.terrarum.modulebasegame

import java.util.*

internal interface RNGConsumer {

    val RNG: Random
    var seed: Long
    var iterations: Int

    fun loadFromSave(seed: Long, iterations: Int) {
        this.seed = seed
        this.iterations = iterations

        repeat(iterations, { RNG.nextInt() })
    }

    private fun incIterations() {
        iterations++

        if (iterations < 0) iterations = 0
    }

    fun getRandomLong(): Long {
        iterations++
        return RNG.nextLong()
    }

}
