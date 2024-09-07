package net.torvald.terrarum.gameworld

import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.Clustfile
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.toUint
import net.torvald.unsafe.UnsafeHelper
import net.torvald.unsafe.UnsafePtr
import java.util.TreeMap

/**
 * Single layer gets single Chunk Pool.
 *
 * Created by minjaesong on 2024-09-07.
 */
open class ChunkPool(
    val DOM: ClusteredFormatDOM,
    val wordSizeInBytes: Int,
    val chunkNumToFileNum: (Int) -> String,
    val renumberFun: (Int) -> Int,
) {
    private val pointers = TreeMap<Int, Long>()
    private var allocCap = 32
    private var allocMap = ArrayList<Int>(allocCap)
    private var allocCounter = 0

    private val chunkSize = (wordSizeInBytes.toLong() * CHUNK_W * CHUNK_H)
    private val pool = UnsafeHelper.allocate(chunkSize * allocCap)

    init {
        allocMap.fill(-1)
    }

    private fun createPointerViewOfChunk(chunkNumber: Int): UnsafePtr {
        val baseAddr = pointers[chunkNumber]!!
        return UnsafePtr(baseAddr, chunkSize)
    }

    private fun createPointerViewOfChunk(chunkNumber: Int, offsetX: Int, offsetY: Int): Pair<UnsafePtr, Long> {
        val baseAddr = pointers[chunkNumber]!!
        return UnsafePtr(baseAddr, chunkSize) to wordSizeInBytes.toLong() * (offsetY * CHUNK_W + offsetX)
    }

    private fun allocate(chunkNumber: Int): UnsafePtr {
        // expand the pool if needed
        if (allocCounter >= allocCap) {
            allocCap *= 2
            allocMap.ensureCapacity(allocCap)
            pool.realloc(chunkSize * allocCap)
        }

        // find the empty spot
        val idx = allocMap.indexOfFirst { it == -1 }
        val ptr = pool.ptr + idx * chunkSize

        allocCounter += 1

        pointers[chunkNumber] = ptr
        return UnsafePtr(ptr, chunkSize)
    }

    private fun deallocate(chunkNumber: Int) {
        val ptr = pointers[chunkNumber] ?: return
        storeToDisk(chunkNumber)
        pointers.remove(chunkNumber)
        allocMap[chunkNumber] = -1
        allocCounter -= 1
    }

    private fun renumber(ptr: UnsafePtr) {
        for (i in 0 until ptr.size step wordSizeInBytes.toLong()) {
            val numIn = (0 until wordSizeInBytes).fold(0) { acc, off ->
                acc or (ptr[i + off].toUint().shl(8 * off))
            }
            val numOut = renumberFun(numIn)
            (0 until wordSizeInBytes).forEach { off ->
                ptr[i + off] = numOut.ushr(8 * off).toByte()
            }
        }
    }

    private fun fetchFromDisk(chunkNumber: Int) {
        // read data from the disk
        val fileName = chunkNumToFileNum(chunkNumber)
        Clustfile(DOM, fileName).let {
            val bytes = Common.unzip(it.readBytes())
            val ptr = allocate(chunkNumber)
            UnsafeHelper.memcpyFromArrToPtr(bytes, 0, ptr.ptr, bytes.size)
            renumber(ptr)
        }
    }

    private fun storeToDisk(chunkNumber: Int) {
        TODO()
    }

    private fun checkForChunk(chunkNumber: Int) {
        if (!pointers.containsKey(chunkNumber)) {
            fetchFromDisk(chunkNumber)
        }
    }

    fun getTileRaw(chunkNumber: Int, offX: Int, offY: Int): Int {
        checkForChunk(chunkNumber)
        val (ptr, ptrOff) = createPointerViewOfChunk(chunkNumber, offX, offY)
        val numIn = (0 until wordSizeInBytes).fold(0) { acc, off ->
            acc or (ptr[ptrOff].toUint().shl(8 * off))
        }
        return numIn
    }

}