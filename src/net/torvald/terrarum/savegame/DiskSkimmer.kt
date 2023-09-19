package net.torvald.terrarum.savegame

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.serialise.toUint
import net.torvald.terrarum.serialise.toUlong
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Level
import kotlin.experimental.and

/**
 * Skimmer allows modifications of the Virtual Disk without building a DOM (disk object model).
 *
 * Skimmer will scan through the raw bytes of the Virtual Disk to get the file requested with its Entry ID;
 * modifying/removing files will edit the Virtual Disk in "dirty" way, where old entries are simply marked as deletion
 * and leaves the actual contents untouched, then will simply append modified files at the end.
 *
 * To obtain "clean" version of the modified Virtual Disk, simply run [sync] function.
 *
 * Created by minjaesong on 2017-11-17.
 */
class DiskSkimmer(
        val diskFile: File,
        noInit: Boolean = false
): SimpleFileSystem {

    override fun getBackingFile() = diskFile

    /*

init:

1. get the startingpoint of the entries (after the 8 byte ID space ofc)

addfile/editfile:

10. mark old parentdir as invalidated
11. mark old entryfile as invalidated
20. append new file
30. append modified parentdir
40. update startingpoint table

removefile:

10. mark old parentdir as invalidated
20. append modified parentdir
30. update startingpoint table

     */

    fun checkFileSanity() {
        if (!diskFile.exists()) throw NoSuchFileException(diskFile.absoluteFile)
        if (diskFile.length() < 310L) throw RuntimeException("Invalid Virtual Disk file: ${diskFile.path}")

        // check magic
        val fis = FileInputStream(diskFile)

        val magic = ByteArray(4).let { fis.read(it); it }
        if (!magic.contentEquals(VirtualDisk.MAGIC)) throw RuntimeException("Invalid Virtual Disk file: ${diskFile.path}")

        fis.close()
    }

    init {
        checkFileSanity()
    }

    /**
     * EntryID to Offset.
     *
     * Offset is where the header begins, so first 4 bytes are exactly the same as the EntryID.
     */
    private var entryToOffsetTable = HashMap<EntryID, Long>()


    private fun debugPrintln(s: Any) {
        if (false) println(s.toString())
    }

    var initialised = false
        private set

    init {
        if (!noInit) {
            rebuild()
        }
    }

    private fun FileInputStream.readBytes(buffer: ByteArray): Int {
        val readStatus = this.read(buffer)
        return readStatus
    }
    private fun FileInputStream.readInt16(): Int {
        val buffer = ByteArray(2)
        val readStatus = readBytes(buffer)
        if (readStatus != 2) throw InternalError("Unexpected error -- EOF reached? (expected 2, got $readStatus)")
        return buffer.toInt16()
    }
    private fun FileInputStream.readInt32(): Int {
        val buffer = ByteArray(4)
        val readStatus = readBytes(buffer)
        if (readStatus != 4) throw InternalError("Unexpected error -- EOF reached? (expected 4, got $readStatus)")
        return buffer.toInt32()
    }
    private fun FileInputStream.readInt48(): Long {
        val buffer = ByteArray(6)
        val readStatus = readBytes(buffer)
        if (readStatus != 6) throw InternalError("Unexpected error -- EOF reached? (expected 6, got $readStatus)")
        return buffer.toInt48()
    }
    private fun FileInputStream.readInt24(): Int {
        val buffer = ByteArray(3)
        val readStatus = readBytes(buffer)
        if (readStatus != 3) throw InternalError("Unexpected error -- EOF reached? (expected 3, got $readStatus)")
        return buffer.toInt24()
    }
    private fun FileInputStream.readInt64(): Long {
        val buffer = ByteArray(8)
        val readStatus = readBytes(buffer)
        if (readStatus != 8) throw InternalError("Unexpected error -- EOF reached? (expected 8, got $readStatus)")
        return buffer.toInt64()
    }
    private fun RandomAccessFile.readBytes(buffer: ByteArray): Int {
        val readStatus = this.read(buffer)
        return readStatus
    }
    private fun RandomAccessFile.readInt16(): Int {
        val buffer = ByteArray(2)
        val readStatus = readBytes(buffer)
        if (readStatus != 2) throw InternalError("Unexpected error -- EOF reached? (expected 2, got $readStatus)")
        return buffer.toInt16()
    }
    private fun RandomAccessFile.readInt32(): Int {
        val buffer = ByteArray(4)
        val readStatus = readBytes(buffer)
        if (readStatus != 4) throw InternalError("Unexpected error -- EOF reached? (expected 4, got $readStatus)")
        return buffer.toInt32()
    }
    private fun RandomAccessFile.readInt48(): Long {
        val buffer = ByteArray(6)
        val readStatus = readBytes(buffer)
        if (readStatus != 6) throw InternalError("Unexpected error -- EOF reached? (expected 6, got $readStatus)")
        return buffer.toInt48()
    }
    private fun RandomAccessFile.readInt24(): Int {
        val buffer = ByteArray(3)
        val readStatus = readBytes(buffer)
        if (readStatus != 3) throw InternalError("Unexpected error -- EOF reached? (expected 3, got $readStatus)")
        return buffer.toInt24()
    }
    private fun RandomAccessFile.readInt64(): Long {
        val buffer = ByteArray(8)
        val readStatus = readBytes(buffer)
        if (readStatus != 8) throw InternalError("Unexpected error -- EOF reached? (expected 8, got $readStatus)")
        return buffer.toInt64()
    }
    private fun ByteArray.toInt16(offset: Int = 0): Int {
        return  this[0 + offset].toUint().shl(8) or
                this[1 + offset].toUint()
    }
    private fun ByteArray.toInt24(offset: Int = 0): Int {
        return  this[0 + offset].toUint().shl(16) or
                this[1 + offset].toUint().shl(8) or
                this[2 + offset].toUint()
    }
    private fun ByteArray.toInt32(offset: Int = 0): Int {
        return  this[0 + offset].toUint().shl(24) or
                this[1 + offset].toUint().shl(16) or
                this[2 + offset].toUint().shl(8) or
                this[3 + offset].toUint()
    }
    private fun ByteArray.toInt48(offset: Int = 0): Long {
        return  this[0 + offset].toUlong().shl(40) or
                this[1 + offset].toUlong().shl(32) or
                this[2 + offset].toUlong().shl(24) or
                this[3 + offset].toUlong().shl(16) or
                this[4 + offset].toUlong().shl(8) or
                this[5 + offset].toUlong()
    }
    private fun ByteArray.toInt64(offset: Int = 0): Long {
        return  this[0 + offset].toUlong().shl(56) or
                this[1 + offset].toUlong().shl(48) or
                this[2 + offset].toUlong().shl(40) or
                this[3 + offset].toUlong().shl(32) or
                this[4 + offset].toUlong().shl(24) or
                this[5 + offset].toUlong().shl(16) or
                this[6 + offset].toUlong().shl(8) or
                this[7 + offset].toUlong()
    }

    fun rebuild() {
        checkFileSanity() // state of the file may have been changed (e.g. file deleted) so we check again

        entryToOffsetTable.clear()

//        fa = RandomAccessFile(diskFile, "rw")

        val fa = RandomAccessFile(diskFile, "rwd")
        var currentPosition = VirtualDisk.HEADER_SIZE
        fa.seek(VirtualDisk.HEADER_SIZE) // skip disk header

//        println("[DiskSkimmer] rebuild ${diskFile.canonicalPath}")


        val currentLength = diskFile.length()
        var ccc = 0
        while (currentPosition < currentLength) {

            val entryID = fa.readInt64() // at this point, cursor is 8 bytes past to the entry head
            currentPosition += 8

            // fill up the offset table/
            val offset = currentPosition

//            printdbg(this, "Offset $offset, entryID $entryID")

            fa.readInt64() // parentID
            val typeFlag = fa.read().toByte()
            fa.readInt24()
            fa.read(16)

            currentPosition += 8+4+16

//            printdbg(this, "    $currentPosition")

            val entrySize = when (typeFlag and 127) {
                DiskEntry.NORMAL_FILE -> {
                    currentPosition += 6
                    fa.readInt48()
                }
                DiskEntry.DIRECTORY -> {
                    currentPosition += 4
                    fa.readInt32().toLong() * 8L
                }
                else -> 0
            }

//            printdbg(this, "    type $typeFlag entrySize = $entrySize")

            currentPosition += entrySize // skips rest of the entry's actual contents
            fa.seek(currentPosition)

            if (typeFlag > 0) {
                if (entryToOffsetTable[entryID] != null)
                    debugPrintln("[DiskSkimmer] ... overwriting the entry $entryID previously found at offset:${entryToOffsetTable[entryID]} with offset:$offset (name: ${diskIDtoReadableFilename(entryID, getSaveKind())})")
                else
                    debugPrintln("[DiskSkimmer] ... successfully read the entry $entryID at offset:$offset (name: ${diskIDtoReadableFilename(entryID, getSaveKind())})")

                entryToOffsetTable[entryID] = offset
            }
            else {
                debugPrintln("[DiskSkimmer] ... discarding entry $entryID at offset:$offset (name: ${diskIDtoReadableFilename(entryID, getSaveKind())})")
            }

//            printdbg(this, "   currentPosition = $currentPosition / $currentLength")

            ccc++
//            if (ccc == 13) System.exit(1)

        }

        fa.close()
        initialised = true
    }


    fun hasEntry(entryID: EntryID) = entryToOffsetTable.containsKey(entryID)

    //////////////////////////////////////////////////
    // THESE ARE METHODS TO SUPPORT ON-LINE READING //
    //////////////////////////////////////////////////

    /**
     * Using entryToOffsetTable, composes DiskEntry on the fly upon request.
     * @return DiskEntry if the entry exists on the disk, `null` otherwise.
     */
    fun requestFile(entryID: EntryID): DiskEntry? {
        if (entryID == 0L) throw UnsupportedOperationException("Peeking at the root entry is an undefined behaviour for the Terrarum Savegame format.")

        if (!initialised) throw IllegalStateException("File entries not built! Initialise the Skimmer by executing rebuild()")

//        rebuild()

        entryToOffsetTable[entryID].let { offset ->
            if (offset == null) {
                debugPrintln("[DiskSkimmer.requestFile] entry $entryID does not exist on the table")
                return null
            }
            else {
                val fis = FileInputStream(diskFile)
                fis.skipNBytes(offset)
                val parent = fis.readInt64()
                val fileFlag = fis.read().toByte()
                fis.readInt24()
                val creationTime = fis.readInt48()
                val modifyTime = fis.readInt48()
                fis.readInt32()


                // get entry size     // TODO future me, is this kind of comment helpful or redundant?
                val entrySize = when (fileFlag) {
                    DiskEntry.NORMAL_FILE -> {
                        fis.readInt48()
                    }
                    DiskEntry.DIRECTORY -> {
                        fis.readInt32().toLong() and 0xFFFFFFFFL
                    }
                    DiskEntry.SYMLINK -> 8L
                    else -> throw UnsupportedOperationException("Unsupported entry type: $fileFlag for entryID $entryID at offset ${offset+8}") // FIXME no support for compressed file
                }

                val entryContent = when (fileFlag) {
                    DiskEntry.NORMAL_FILE -> {
                        val byteArray = ByteArray64(entrySize)
                        // read one byte at a time
                        for (c in 0L until entrySize) {
                            byteArray[c] = fis.read().toByte()
                        }

                        EntryFile(byteArray)
                    }
                    DiskEntry.DIRECTORY -> {
                        val dirContents = ArrayList<EntryID>()
                        // read 8 bytes at a time
                        val bytesBuffer8 = ByteArray(8)
                        for (c in 0L until entrySize) {
                            fis.read(bytesBuffer8)
                            dirContents.add(bytesBuffer8.toLongBig())
                        }

                        EntryDirectory(dirContents)
                    }
                    DiskEntry.SYMLINK -> {
                        val target = fis.readInt64()

                        EntrySymlink(target)
                    }
                    else -> throw UnsupportedOperationException("Unsupported entry type: $fileFlag for entryID $entryID at offset ${offset+8}") // FIXME no support for compressed file
                }

                fis.close()
                return DiskEntry(entryID, parent, creationTime, modifyTime, entryContent)
            }
        }
    }

    override fun getEntry(id: EntryID) = requestFile(id)
    override fun getFile(id: EntryID) = requestFile(id)?.contents as? EntryFile

    fun invalidateEntry(id: EntryID) {
        val fa = RandomAccessFile(diskFile, "rwd")
        entryToOffsetTable[id]?.let {
            fa.seek(it + 8)
            val type = fa.read()
            fa.seek(it + 8)
            fa.write(type or 128)
            entryToOffsetTable.remove(id)
        }
        fa.close()
    }


    fun injectDiskCRC(crc: Int) {
        val fa = RandomAccessFile(diskFile, "rwd")
        fa.seek(42L)
        fa.write(crc.toBigEndian())
        fa.close()
    }

    fun setSaveMode(bits: Int) {
        val fa = RandomAccessFile(diskFile, "rwd")
        fa.seek(49L)
        fa.writeByte(bits)
        fa.close()
    }

    fun setSaveKind(bits: Int) {
        val fa = RandomAccessFile(diskFile, "rwd")
        fa.seek(50L)
        fa.writeByte(bits)
        fa.close()
    }

    fun setSaveOrigin(bits: Int) {
        val fa = RandomAccessFile(diskFile, "rwd")
        fa.seek(51L)
        fa.writeByte(bits)
        fa.close()
    }

    /**
     * @return Save type (0b 0000 00ab)
     *                   b: unset - full save; set - quicksave (only applicable to worlds -- quicksave just means the disk is in dirty state)
     *                   a: set - generated by autosave
     */
    fun getSaveMode(): Int {
        val fa = RandomAccessFile(diskFile, "rwd")
        fa.seek(49L)
        return fa.read().also { fa.close() }
    }

    /**
     * @return 1 if the savegame is player data, 2 if the savegame is world data, 0 otherwise
     */
    fun getSaveKind(): Int {
        val fa = RandomAccessFile(diskFile, "rwd")
        fa.seek(50L)
        return fa.read().also { fa.close() }
    }

    /**
     * @return 16 if the savegame was imported, 0 if the savegame was generated in-game
     */
    fun getSaveOrigin(): Int {
        val fa = RandomAccessFile(diskFile, "rwd")
        fa.seek(51L)
        return fa.read().also { fa.close() }
    }



    override fun getDiskName(charset: Charset): String {
        val fa = RandomAccessFile(diskFile, "rwd")
        val bytes = ByteArray(268)
        fa.seek(10)
        fa.read(bytes, 0, 32)
        fa.seek(60L)
        fa.read(bytes, 32, 236)
        fa.close()
        return bytes.toCanonicalString(charset)
    }

    /**
     * @return creation time of the root directory
     */
    fun getCreationTime(): Long {
        val fa = RandomAccessFile(diskFile, "rwd")
        val bytes = ByteArray(6)
        fa.seek(320L)
        fa.read(bytes)
        fa.close()
        return bytes.toInt48()
    }

    /**
     * @return last modified time of the root directory
     */
    fun getLastModifiedTime(): Long {
        val fa = RandomAccessFile(diskFile, "rwd")
        val bytes = ByteArray(6)
        fa.seek(326L)
        fa.read(bytes)
        fa.close()
        return bytes.toInt48()
    }

    /**
     * redefines creation time of the root directory
     */
    fun setCreationTime(time_t: Long) {
        val fa = RandomAccessFile(diskFile, "rwd")
        val bytes = ByteArray(6)
        fa.seek(320L)
        fa.write(time_t.toInt48())
        fa.close()
    }

    /**
     * redefines last modified time of the root directory
     */
    fun setLastModifiedTime(time_t: Long) {
        val fa = RandomAccessFile(diskFile, "rwd")
        val bytes = ByteArray(6)
        fa.seek(326L)
        fa.write(time_t.toInt48())
        fa.close()
    }

    ///////////////////////////////////////////////////////
    // THESE ARE METHODS TO SUPPORT ON-LINE MODIFICATION //
    ///////////////////////////////////////////////////////

    /*fun appendEntryOnly(entry: DiskEntry) {
        val parentDir = requestFile(entry.parentEntryID)!!
        val id = entry.entryID

        // add the entry to its parent directory if there was none
        val dirContent = (parentDir.contents as EntryDirectory)
        if (!dirContent.contains(id)) dirContent.add(id)
        modifiedDirectories.add(parentDir)

        invalidateEntry(id)

        val appendAt = fa.length()
        fa.seek(appendAt)

        // append new file
        entryToOffsetTable[id] = appendAt + 8
        entry.serialize().forEach { fa.writeByte(it.toInt()) }
    }*/

    fun appendEntry(entry: DiskEntry) {
        val fa = RandomAccessFile(diskFile, "rwd")

        val id = entry.entryID

        val appendAt = fa.length()
        fa.seek(appendAt)

        // append new file
        entryToOffsetTable[id] = appendAt + 8
        entry.serialize().let { ba ->
            ba.forEachUsedBanks { count, bytes ->
                fa.write(bytes, 0, count)
            }
        }

        fa.close()
    }

    fun deleteEntry(id: EntryID) {
        invalidateEntry(id)
    }

    fun appendEntries(entries: List<DiskEntry>) = entries.forEach { appendEntry(it) }
    fun deleteEntries(entries: List<EntryID>) = entries.forEach { deleteEntry(it) }

    /**
     * Writes new clean file
     */
    fun sync(): VirtualDisk {
        // rebuild VirtualDisk out of this and use it to write out
        val disk = VDUtil.readDiskArchive(diskFile, Level.INFO)
        VDUtil.dumpToRealMachine(disk, diskFile)
        entryToOffsetTable.clear()
        rebuild()
        return disk
    }


//    fun dispose() {
//        fa.close()
//    }


    companion object {
        fun InputStream.read(size: Int): ByteArray {
            val ba = ByteArray(size)
            this.read(ba)
            return ba
        }
        fun RandomAccessFile.read(size: Int): ByteArray {
            val ba = ByteArray(size)
            this.read(ba)
            return ba
        }
    }

    /**
     * total size of the entry block. This size includes that of the header
     */
    private fun getEntryBlockSize(id: EntryID): Long? {
        val offset = entryToOffsetTable[id] ?: return null

        val HEADER_SIZE = DiskEntry.HEADER_SIZE

        debugPrintln("[DiskSkimmer.getEntryBlockSize] offset for entry $id = $offset")

        val fis = FileInputStream(diskFile)
        fis.skipNBytes(offset + 8)
        val type = fis.read().toByte()
        fis.skipNBytes(272) // skip name, timestamp and CRC


        val ret: Long
        when (type) {
            DiskEntry.NORMAL_FILE -> {
                ret = fis.read(6).toInt48() + HEADER_SIZE + 6
            }
            DiskEntry.DIRECTORY -> {
                ret = fis.read(2).toShortBig() * 4 + HEADER_SIZE + 2
            }
            DiskEntry.SYMLINK -> { ret = 4 }
            else -> throw UnsupportedOperationException("Unknown type $type for entry $id")
        }

        fis.close()

        return ret
    }

    private fun byteByByteCopy(size: Long, `in`: InputStream, out: OutputStream) {
        for (i in 0L until size) {
            out.write(`in`.read())
        }
    }

    private fun ByteArray.toShortBig(): Int {
        return  this[0].toUint().shl(8) or
                this[1].toUint()
    }

    private fun ByteArray.toIntBig(): Int {
        return  this[0].toUint().shl(24) or
                this[1].toUint().shl(16) or
                this[2].toUint().shl(8) or
                this[3].toUint()
    }

    private fun ByteArray.toInt48(): Long {
        return  this[0].toUlong().shl(40) or
                this[1].toUlong().shl(32) or
                this[2].toUlong().shl(24) or
                this[3].toUlong().shl(16) or
                this[4].toUlong().shl(8) or
                this[5].toUlong()
    }

    private fun ByteArray.toLongBig(): Long {
        return  this[0].toUlong().shl(56) or
                this[1].toUlong().shl(48) or
                this[2].toUlong().shl(40) or
                this[3].toUlong().shl(32) or
                this[4].toUlong().shl(24) or
                this[5].toUlong().shl(16) or
                this[6].toUlong().shl(8) or
                this[7].toUlong()
    }

    fun setDiskName(name: String, charset: Charset) {
        val fa = RandomAccessFile(diskFile, "rwd")
        val bytes = name.toEntryName(268, charset)
        fa.seek(10L)
        fa.write(bytes, 0, 32)
        fa.seek(60L)
        fa.write(bytes, 32, 236)
        fa.close()
    }
}