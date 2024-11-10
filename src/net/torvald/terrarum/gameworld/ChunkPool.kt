package net.torvald.terrarum.gameworld

import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.Clustfile
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.savegame.DiskEntry
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.savegame.EntryFile
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.toUint
import net.torvald.unsafe.UnsafeHelper
import net.torvald.unsafe.UnsafePtr
import net.torvald.util.Float16
import java.util.TreeMap

/**
 * Created by minjaesong on 2024-10-07.
 */
data class ChunkAllocation(
    val chunkNumber: Long,
    var classifier: ChunkAllocClass,
    var lastAccessTime: Long = System.nanoTime()
)

/**
 * Created by minjaesong on 2024-10-07.
 */
enum class ChunkAllocClass {
    PERSISTENT, TEMPORARY
}

/**
 * FIXME: loading a chunk from disk will attempt to create a chunk because the chunk-to-be-loaded is not on the pointers map, and this operation will want to create a new chunk file but the file already exists
 *
 * Single layer gets single Chunk Pool.
 *
 * Created by minjaesong on 2024-09-07.
 */
open class ChunkPool {

    // `DiskSkimmer` or `ClusteredFormatDOM`
    private val disk: Any
    private val layerIndex: Int
    private val wordSizeInBytes: Long
    private val world: TheGameWorld
    private val initialValue: Int // bytes to fill the new chunk
    private val renumberFun: (Int) -> Int

    private val pointers = TreeMap<Long, Long>()
    private var allocCap = 32
    private var allocMap = Array<ChunkAllocation?>(allocCap) { null }
    private var allocCounter = 0
    private val chunkSize: Long
    private val pool: UnsafePtr

    constructor(
        disk: DiskSkimmer,
        layerIndex: Int,
        wordSizeInBytes: Long,
        world: TheGameWorld,
        initialValue: Int,
        renumberFun: (Int) -> Int,
    ) {
        this.disk = disk
        this.layerIndex = layerIndex
        this.wordSizeInBytes = wordSizeInBytes
        this.world = world
        this.initialValue = initialValue
        this.renumberFun = renumberFun

        chunkSize = wordSizeInBytes * CHUNK_W * CHUNK_H
        pool = UnsafeHelper.allocate(chunkSize * allocCap)
    }

    constructor(
        disk: ClusteredFormatDOM,
        layerIndex: Int,
        wordSizeInBytes: Long,
        world: TheGameWorld,
        initialValue: Int,
        renumberFun: (Int) -> Int,
    ) {
        this.disk = disk
        this.layerIndex = layerIndex
        this.wordSizeInBytes = wordSizeInBytes
        this.world = world
        this.initialValue = initialValue
        this.renumberFun = renumberFun

        chunkSize = wordSizeInBytes * CHUNK_W * CHUNK_H
        pool = UnsafeHelper.allocate(chunkSize * allocCap)
    }

    init {
        allocMap.fill(null)
    }

    private fun createPointerViewOfChunk(chunkNumber: Long): UnsafePtr {
        val baseAddr = pointers[chunkNumber]!!
        return UnsafePtr(baseAddr, chunkSize)
    }

    /**
     * Returns a pointer and the offset from the pointer. The offset is given in bytes, but always word-aligned (if block offset is `(2, 0)`, the returning offset will be `8`, assuming word size of 4)
     */
    private fun createPointerViewOfChunk(chunkNumber: Long, offsetX: Int, offsetY: Int): Pair<UnsafePtr, Long> {
        val baseAddr = pointers[chunkNumber]!!
        return UnsafePtr(baseAddr, chunkSize) to wordSizeInBytes * (offsetY * CHUNK_W + offsetX)
    }

    /**
     * If the chunk number == playerChunkNum, the result will be `false`
     */
    private fun Long.isChunkNearPlayer(playerChunkNum: Long): Boolean {
        if (this == playerChunkNum) return false

        val pxy = LandUtil.chunkNumToChunkXY(world, playerChunkNum)
        val validChunknums = chunkOffsetsNearPlayer.map { (it + pxy).also {
            it.x = it.x fmod (world.width / CHUNK_W) // wrap around X as per ROUNDWORLD
        } }.filter { it.y in 0 until (world.height / CHUNK_H) }.map { // filter values that is outside of world's Y-range
            LandUtil.chunkXYtoChunkNum(world, it.x, it.y) // convert back to chunk numbers
        }
        return validChunknums.contains(this)
    }

    /**
     * Tries to make a spot for one (1) new chunk.
     *
     * Procedure:
     * 1. Check if there are at least one out-of-reach and `TEMPORARY` chunk.
     * 2. If Check #1 passes, that chunk will be unloaded — end.
     * 3. Otherwise, the pool will be extended.
     */
    private fun updateAllocMapUsingIngamePlayer() {
        val playerChunkNum = INGAME.actorNowPlaying?.intTilewiseHitbox?.let {
            val cx = (it.canonicalX.toInt() fmod world.width) / CHUNK_W
            val cy = (it.canonicalY.toInt() / CHUNK_H).coerceIn(0 until world.height / CHUNK_H)
            LandUtil.chunkXYtoChunkNum(world, cx, cy)
        }

        if (playerChunkNum != null) {
            // get list of chunks that passes Check #1
            val remCandidate: List<ChunkAllocation> = allocMap.filterNotNull().filter {
                it.chunkNumber.isChunkNearPlayer(playerChunkNum) &&
                it.classifier == ChunkAllocClass.TEMPORARY
            }

            if (remCandidate.isNotEmpty()) {
                // try to deallocate the oldest allocation (having the smallest lastAccessTime)
                if (deallocate(remCandidate.minByOrNull { it.lastAccessTime }!!))
                    return // exit the function ONLY IF the deallocation was successful
            }
            // if there is no candidate, proceed to the next line
        }

        // expand the pool and allocMap if needed
        while (allocCounter >= allocCap) {
            allocCap *= 2
            val newAllocMap = Array<ChunkAllocation?>(allocCap) { null }
            System.arraycopy(allocMap, 0, newAllocMap, 0, allocMap.size)
            allocMap = newAllocMap
            pool.realloc(chunkSize * allocCap)
        }
    }

    private fun allocate(chunkNumber: Long, allocClass: ChunkAllocClass = ChunkAllocClass.TEMPORARY): UnsafePtr {
        updateAllocMapUsingIngamePlayer()

        // find the empty spot within the pool
        val idx = allocMap.indexOfFirst { it == null }
        val ptr = pool.ptr + idx * chunkSize

        allocMap[idx] = ChunkAllocation(chunkNumber, allocClass)

        allocCounter += 1

        pointers[chunkNumber] = ptr
        return UnsafePtr(ptr, chunkSize)
    }

    private fun deallocate(allocation: ChunkAllocation) = deallocate(allocation.chunkNumber)

    private fun deallocate(chunkNumber: Long): Boolean {
        val ptr = pointers[chunkNumber] ?: return false

        storeToDisk(chunkNumber)
        pointers.remove(chunkNumber)
        UnsafeHelper.unsafe.freeMemory(ptr)

        allocMap.indexOfFirst { it?.chunkNumber == chunkNumber }.let {
            allocMap[it] = null
            allocCounter -= 1
        }

        return true
    }

    private fun renumber(ptr: UnsafePtr) {
        for (i in 0 until ptr.size step wordSizeInBytes) {
            val numIn = (0 until wordSizeInBytes.toInt()).fold(0) { acc, off ->
                acc or (ptr[i + off].toUint().shl(8 * off))
            }
            val numOut = renumberFun(numIn)
            (0 until wordSizeInBytes.toInt()).forEach { off ->
                ptr[i + off] = numOut.ushr(8 * off).toByte()
            }
        }
    }

    /**
     * @return `unit` if IO operation was successful, `null` if failed (e.g. file not exists)
     */
    private fun fetchFromDisk(chunkNumber: Long): Unit? {
        val fileName = chunkNumToFileNum(layerIndex, chunkNumber)

        // read data from the disk
        return if (disk is ClusteredFormatDOM) {
            Clustfile(disk, fileName).let {
                if (!it.exists()) return@let null

                val bytes = Common.unzip(it.readBytes())
                val ptr = allocate(chunkNumber)
                UnsafeHelper.memcpyFromArrToPtr(bytes, 0, ptr.ptr, bytes.size)
                renumber(ptr)
            }
        }
        else if (disk is DiskSkimmer) {
            val fileID = fileName.toLong()
            disk.getFile(fileID).let {
                if (it == null) return@let null

                val bytes = Common.unzip(it.bytes)
                val ptr = allocate(chunkNumber)
                UnsafeHelper.memcpyFromArrToPtr(bytes, 0, ptr.ptr, bytes.size)
                renumber(ptr)
            }
        }
        else {
            throw IllegalStateException()
        }
    }

    /**
     * @return `unit` if IO operation was successful, `null` if failed (e.g. file not exists)
     */
    private fun createNewChunkFile(chunkNumber: Long): Unit? {
        TODO()
    }

    private fun storeToDisk(chunkNumber: Long) {
        val fileName = chunkNumToFileNum(layerIndex, chunkNumber)

        // write to the disk (the disk must be an autosaving copy of the original)
        if (disk is ClusteredFormatDOM) {
            Clustfile(disk, fileName).let {
                val bytes = Common.zip(serialise(chunkNumber).iterator())
                it.overwrite(bytes.toByteArray())
            }
        }
        // append the new entry
        else if (disk is DiskSkimmer) {
            val fileID = fileName.toLong()

            val bytes = Common.zip(serialise(chunkNumber).iterator())
            val oldEntry = disk.getEntry(fileID)
            val timeNow = App.getTIME_T()
            disk.appendEntry(DiskEntry(fileID, 0L, oldEntry?.creationDate ?: timeNow, timeNow, EntryFile(bytes)))
        }
    }

    private fun checkForChunk(chunkNumber: Long) {
        if (!pointers.containsKey(chunkNumber)) {
            fetchFromDisk(chunkNumber) ?: createNewChunkFile(chunkNumber) ?: TODO("handle IO error")
        }
    }

    private fun serialise(chunkNumber: Long): ByteArray {
        val ptr = pointers[chunkNumber]!!
        val out = ByteArray(chunkSize.toInt())
        UnsafeHelper.memcpyFromPtrToArr(ptr, out, 0, chunkSize)
        return out
    }

    fun worldXYChunkNumAndOffset(worldx: Int, worldy: Int): Triple<Long, Int, Int> {
        val chunkX = worldx / CHUNK_W
        val chunkY = worldy / CHUNK_H
        val chunkOx = worldx % CHUNK_W
        val chunkOy = worldy % CHUNK_H

        val chunkNum = LandUtil.chunkXYtoChunkNum(world, chunkX, chunkY)
        return Triple(chunkNum, chunkOx, chunkOy)
    }

    private fun updateAccessTime(chunkNumber: Long) {
        allocMap.find { it?.chunkNumber == chunkNumber }!!.let { it.lastAccessTime = System.nanoTime() }
    }

    /**
     * Given the word-aligned byte sequence of `[B0, B1, B2, B3, ...]`,
     * Return format:
     * - word size is 4: Int `B3_B2_B1_B0`
     * - word size is 3: Int `00_B2_B1_B0`
     * - word size is 2: Int `00_00_B1_B0`
     */
    fun getTileRaw(chunkNumber: Long, offX: Int, offY: Int): Int {
        checkForChunk(chunkNumber)
        updateAccessTime(chunkNumber)
        val (ptr, ptrOff) = createPointerViewOfChunk(chunkNumber, offX, offY)
        val numIn = (0 until wordSizeInBytes.toInt()).fold(0) { acc, off ->
            acc or (ptr[ptrOff].toUint().shl(8 * off))
        }
        return numIn
    }

    /**
     * Given the bytes of Int `B3_B2_B1_B0`
     * Saved as:
     * - word size is 4: `[B0, B1, B2, B3]`
     * - word size is 3: `[B0, B1, B2]` (B3 is ignored)
     * - word size is 2: `[B0, B1]` (B2 and B3 are ignored)
     */
    fun setTileRaw(chunkNumber: Long, offX: Int, offY: Int, bytes: Int) {
        checkForChunk(chunkNumber)
        updateAccessTime(chunkNumber)
        val (ptr, ptrOff) = createPointerViewOfChunk(chunkNumber, offX, offY)
        for (i in 0 until wordSizeInBytes.toInt()) {
            val b = bytes.ushr(8*i).and(255)
            ptr[ptrOff + i] = b.toByte()
        }
    }

    /**
     * Given the word-aligned byte sequence of `[B0, B1, B2, B3, ...]`,
     * Return format:
     * - First element: Int `00_00_B1_B0`
     * - Second element: Float16(`B3_B2`).toFloat32
     */
    fun getTileI16F16(chunkNumber: Long, offX: Int, offY: Int): Pair<Int, Float> {
        val raw = getTileRaw(chunkNumber, offX, offY)
        val ibits = raw.get1SS()
        val fbits = raw.get2SS().toShort()
        return ibits to Float16.toFloat(fbits)
    }

    /**
     * Given the word-aligned byte sequence of `[B0, B1, B2, B3, ...]`,
     * Return format:
     * - Int `00_00_B1_B0`
     */
    fun getTileI16(chunkNumber: Long, offX: Int, offY: Int): Int {
        val raw = getTileRaw(chunkNumber, offX, offY)
        val ibits = raw.get1SS()
        return ibits
    }

    /**
     * Given the word-aligned byte sequence of `[B0, B1, B2, B3, ...]`,
     * Return format:
     * - First element: Int `00_00_B1_B0`
     * - Second element: Int `00_00_00_B2`
     */
    fun getTileI16I8(chunkNumber: Long, offX: Int, offY: Int): Pair<Int, Int> {
        val raw = getTileRaw(chunkNumber, offX, offY)
        val ibits = raw.get1SS()
        val jbits = raw.get2SS() and 255
        return ibits to jbits
    }

    fun dispose() {

    }

    companion object {
        fun chunkNumToFileNum(layerNum: Int, chunkNum: Long): String {
            val entryID = Common.layerAndChunkNumToEntryID(layerNum, chunkNum)
            return Common.type254EntryIDtoType17Filename(entryID)
        }
        
        private fun Int.get1SS() = this and 65535
        private fun Int.get2SS() = (this ushr 16) and 65535


        private fun Int.getMSB() = (this ushr 24) and 255
        private fun Int.get2MSB() = (this ushr 16) and 255
        private fun Int.get3MSB() = (this ushr 8) and 255
        private fun Int.get4MSB() = this and 255

        private fun Int.get4LSB() = this.getMSB()
        private fun Int.get3LSB() = this.get2MSB()
        private fun Int.get2LSB() = this.get3MSB()
        private fun Int.getLSB() = this.get4MSB()

        fun getRenameFunTerrain(world: TheGameWorld): (Int) -> Int {
            // word size: 2
            return { oldTileNum ->
                val oldOreName = world.oldTileNumberToNameMap[oldTileNum.toLong()]

                world.tileNameToNumberMap[oldOreName]!!
            }
        }

        fun getRenameFunOres(world: TheGameWorld): (Int) -> Int {
            // word size: 3
            return { oldTileNumRaw ->
                val oldOreNum = oldTileNumRaw and 0x0000FFFF
                val oldOrePlacement = oldTileNumRaw and 0xFFFF0000.toInt()
                val oldOreName = world.oldTileNumberToNameMap[oldOreNum.toLong()]!!

                world.tileNameToNumberMap[oldOreName]!! or oldOrePlacement
            }
        }

        fun getRenameFunFluids(world: TheGameWorld): (Int) -> Int {
            // word size: 4
            return { oldTileNumRaw ->
                val oldFluidNum = oldTileNumRaw and 0x0000FFFF
                val oldFluidFill = oldTileNumRaw and 0xFFFF0000.toInt()
                val oldFluidName = world.oldTileNumberToNameMap[oldFluidNum.toLong()]

                world.tileNameToNumberMap[oldFluidName]!! or oldFluidFill
            }
        }

        private val chunkOffsetsNearPlayer = listOf(
            Point2i(-1,-2), Point2i(0,-2),Point2i(1,-2),
            Point2i(-2,-1),Point2i(-1,-1),Point2i(0,-1),Point2i(1,-1),Point2i(2,-1),
            Point2i(-2,0),Point2i(-1,0),Point2i(1,0),Point2i(2,0),
            Point2i(-2,1),Point2i(-1,1),Point2i(0,1),Point2i(1,1),Point2i(2,1),
            Point2i(-1,2),Point2i(0,2),Point2i(1,2)
        )
    }
}