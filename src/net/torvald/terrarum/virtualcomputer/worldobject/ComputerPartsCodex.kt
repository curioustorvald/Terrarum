package net.torvald.terrarum.virtualcomputer.worldobject

import java.util.*

/**
 * Created by minjaesong on 16-09-08.
 */
object ComputerPartsCodex {
    val rams = HashMap<Int, Int>() // itemID, capacity in bytes (0 bytes - 8 GBytes)
    val processors = HashMap<Int, Int>() // itemID, cycles
    val harddisks = HashMap<Int, Int>() // itemID, capacity in bytes
    val diskettes = HashMap<Int, Int>() // itemID, capacity in bytes
    val opticaldiscs = HashMap<Int, Int>() // itemID, capacity in bytes

    init {
        // in kilobytes
        rams.put(4864,  128.KiB())
        rams.put(4865,  192.KiB())
        rams.put(4866,  256.KiB())
        rams.put(4867,  384.KiB())
        rams.put(4868,  512.KiB())
        rams.put(4869,  768.KiB())
        rams.put(4870, 1024.KiB())
        rams.put(4871, 2048.KiB())

        processors.put(4872, 1000)
        processors.put(4873, 2000)
        processors.put(4874, 4000)
        processors.put(4875, 8000) // this is totally OP

        harddisks.put(4876,  1.MB())
        harddisks.put(4877,  2.MB())
        harddisks.put(4878,  5.MB())
        harddisks.put(4879, 10.MB())

        // Floppy disk: your primitive and only choice of removable storage
        diskettes.put(4880,  360.kB()) // single-sided
        diskettes.put(4881,  720.kB()) // double-sided
        diskettes.put(4882, 1440.kB()) // 3.5" HD
        diskettes.put(4883, 2880.kB()) // 3.5" ED

        // CD-Rs
        opticaldiscs.put(4884, 8.MB()) // arbitrary size
    }

    fun getRamSize(itemIndex: Int): Int = rams[itemIndex] ?: 0
    fun getProcessorCycles(itemIndex: Int): Int = processors[itemIndex] ?: 0
    fun getHDDSize(itemIndex: Int): Int = harddisks[itemIndex] ?: 0
    fun getFDDSize(itemIndex: Int): Int = diskettes[itemIndex] ?: 0
    fun getODDSize(itemIndex: Int): Int = opticaldiscs[itemIndex] ?: 0

    private fun Int.MB() = this * 1000000 // 1 MB == 1 000 000 bytes, bitches!
    private fun Int.kB() = this * 1000
    private fun Int.KiB() = this.shl(10)
    private fun Int.MiB() = this.shl(20)
}