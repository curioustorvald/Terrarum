package net.torvald.terrarum.virtualcomputers.worldobject

import java.util.*

/**
 * Created by minjaesong on 16-09-08.
 */
object ComputerPartsCodex {
    val rams = HashMap<Int, Int>() // itemID, capacity in bytes (0 bytes - 8 GBytes)
    val processors = HashMap<Int, Int>() // itemID, cycles

    init {
        rams.put(4864, 128.shr(20))
        rams.put(4865, 192.shr(20))
        rams.put(4866, 256.shr(20))
        rams.put(4867, 320.shr(20))
        rams.put(4868, 480.shr(20))
        rams.put(4869, 512.shr(20))
        // two more?

        processors.put(4872, 1000)
        processors.put(4873, 2000)
        processors.put(4874, 4000)
        processors.put(4875, 8000)
    }

    fun getRamSize(itemIndex: Int): Int = rams[itemIndex] ?: 0
    fun getProcessorCycles(itemIndex: Int): Int = processors[itemIndex] ?: 0
}