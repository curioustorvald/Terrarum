package net.torvald.terrarum.tvda

import java.io.*
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

/**
 * Skimming allows modifying the Virtual Disk without loading entire disk onto the memory.
 *
 * Skimmer will just scan through the raw bytes of the Virtual Disk to get the file requested with its Entry ID;
 * modifying/removing files will edit the Virtual Disk in "dirty" way, where old entries are simply marked as deletion
 * and leaves the actual contents untouched, then will simply append modified files at the end.
 *
 * To obtain "clean" version of the modified Virtual Disk, simply run [sync] function.
 *
 * Created by minjaesong on 2017-11-17.
 */
class DiskSkimmer(private val diskFile: File, val charset: Charset = Charset.defaultCharset()) {

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

    /**
     * EntryID to Offset.
     *
     * Offset is where the header begins, so first 4 bytes are exactly the same as the EntryID.
     */
    private var entryToOffsetTable = HashMap<EntryID, Long>()


    /** temporary storage to store tree edges */
//    private var directoryStruct = ArrayList<DirectoryEdge>()

    /** root node of the directory tree */
//    private var directory = DirectoryNode(0, null, DiskEntry.DIRECTORY, "")

//    private data class DirectoryEdge(val nodeParent: EntryID, val node: EntryID, val type: Byte, val name: String)
//    private data class DirectoryNode(var nodeThis: EntryID, val nodeParent: EntryID?, var type: Byte, var name: String)

    private val dirDelim = Regex("""[\\/]""")
    private val DIR = "/"

    val fa = RandomAccessFile(diskFile, "rw")

    init {
        val fis = FileInputStream(diskFile)

        println("[DiskSkimmer] loading the diskfile ${diskFile.canonicalPath}")

        var currentPosition = fis.skip(64) // skip disk header


        fun skipRead(bytes: Long) {
            currentPosition += fis.skip(bytes)
        }
        /**
         * Reads a byte and adds up the position var
         */
        fun readByte(): Byte {
            currentPosition++
            val read = fis.read()

            if (read < 0) throw InternalError("Unexpectedly reached EOF")
            return read.toByte()
        }

        /**
         * Reads specific bytes to the buffer and adds up the position var
         */
        fun readBytes(buffer: ByteArray): Int {
            val readStatus = fis.read(buffer)
            currentPosition += readStatus
            return readStatus
        }
        fun readUshortBig(): Int {
            val buffer = ByteArray(2)
            val readStatus = readBytes(buffer)
            if (readStatus != 2) throw InternalError("Unexpected error -- EOF reached? (expected 4, got $readStatus)")
            return buffer.toShortBig()
        }
        fun readIntBig(): Int {
            val buffer = ByteArray(4)
            val readStatus = readBytes(buffer)
            if (readStatus != 4) throw InternalError("Unexpected error -- EOF reached? (expected 4, got $readStatus)")
            return buffer.toIntBig()
        }
        fun readInt48(): Long {
            val buffer = ByteArray(6)
            val readStatus = readBytes(buffer)
            if (readStatus != 6) throw InternalError("Unexpected error -- EOF reached? (expected 6, got $readStatus)")
            return buffer.toInt48()
        }
        fun readLongBig(): Long {
            val buffer = ByteArray(8)
            val readStatus = readBytes(buffer)
            if (readStatus != 8) throw InternalError("Unexpected error -- EOF reached? (expected 8, got $readStatus)")
            return buffer.toLongBig()
        }

        val currentLength = diskFile.length()
        while (currentPosition < currentLength) {

            val entryID = readLongBig() // at this point, cursor is 4 bytes past to the entry head

            // fill up the offset table
            val offset = currentPosition

            skipRead(8)
            val typeFlag = readByte()
            skipRead(3)
            skipRead(16) // skip rest of the header

            val entrySize = when (typeFlag and 127) {
                DiskEntry.NORMAL_FILE -> readInt48()
                DiskEntry.DIRECTORY -> readIntBig().toLong()
                else -> 0
            }

            skipRead(entrySize) // skips rest of the entry's actual contents

            if (typeFlag > 0) {
                entryToOffsetTable[entryID] = offset
                println("[DiskSkimmer] successfully read the entry $entryID at offset $offset (name: ${diskIDtoReadableFilename(entryID)})")
            }
            else {
                println("[DiskSkimmer] discarding entry $entryID at offset $offset (name: ${diskIDtoReadableFilename(entryID)})")
            }
        }

    }


    //////////////////////////////////////////////////
    // THESE ARE METHODS TO SUPPORT ON-LINE READING //
    //////////////////////////////////////////////////

    /**
     * Using entryToOffsetTable, composes DiskEntry on the fly upon request.
     * @return DiskEntry if the entry exists on the disk, `null` otherwise.
     */
    fun requestFile(entryID: EntryID): DiskEntry? {
        entryToOffsetTable[entryID].let { offset ->
            if (offset == null) {
                println("[DiskSkimmer.requestFile] entry $entryID does not exist on the table")
                return null
            }
            else {
                fa.seek(offset)
                val parent = fa.read(8).toLongBig()
                val fileFlag = fa.read(4)[0]
                val creationTime = fa.read(6).toInt48()
                val modifyTime = fa.read(6).toInt48()
                val skip_crc = fa.read(4)

                // get entry size     // TODO future me, is this kind of comment helpful or redundant?
                val entrySize = when (fileFlag) {
                    DiskEntry.NORMAL_FILE -> {
                        fa.read(6).toInt48()
                    }
                    DiskEntry.DIRECTORY -> {
                        fa.read(4).toIntBig().toLong()
                    }
                    DiskEntry.SYMLINK -> 8L
                    else -> throw UnsupportedOperationException("Unsupported entry type: $fileFlag") // FIXME no support for compressed file
                }


                val entryContent = when (fileFlag) {
                    DiskEntry.NORMAL_FILE -> {
                        val byteArray = ByteArray64(entrySize)
                        // read one byte at a time
                        for (c in 0L until entrySize) {
                            byteArray[c] = fa.read().toByte()
                        }

                        EntryFile(byteArray)
                    }
                    DiskEntry.DIRECTORY -> {
                        val dirContents = ArrayList<EntryID>()
                        // read 8 bytes at a time
                        val bytesBuffer8 = ByteArray(8)
                        for (c in 0L until entrySize) {
                            fa.read(bytesBuffer8)
                            dirContents.add(bytesBuffer8.toLongBig())
                        }

                        EntryDirectory(dirContents)
                    }
                    DiskEntry.SYMLINK -> {
                        val target = fa.read(8).toLongBig()

                        EntrySymlink(target)
                    }
                    else -> throw UnsupportedOperationException("Unsupported entry type: $fileFlag") // FIXME no support for compressed file
                }

                return DiskEntry(entryID, parent, creationTime, modifyTime, entryContent)
            }
        }
    }

    /**
     * Try to find a file with given path (which uses '/' as a separator). Is search is failed for whatever reason,
     * `null` is returned.
     *
     * @param path A path to the file from the root, directory separated with '/' (and not '\')
     * @return DiskEntry if the search was successful, `null` otherwise
     */
    /*fun requestFile(path: String): DiskEntry? {
        // fixme pretty much untested

        val path = path.split(dirDelim)
        //println(path)

        // bunch-of-io-access approach (for reading)
        var traversedDir = 0L // entry ID
        var dirFile: DiskEntry? = null
        path.forEachIndexed { index, dirName ->
            println("[DiskSkimmer.requestFile] $index\t$dirName, traversedDir = $traversedDir")

            dirFile = requestFile(traversedDir)
            if (dirFile == null) {
                println("[DiskSkimmer.requestFile] requestFile($traversedDir) came up null")
                return null
            } // outright null
            if (dirFile!!.contents !is EntryDirectory && index < path.lastIndex) { // unexpectedly encountered non-directory
                return null // because other than the last path, everything should be directory (think about it!)
            }
            //if (index == path.lastIndex) return dirFile // reached the end of the search strings

            // still got more paths behind to traverse
            var dirGotcha = false
            // loop for current dir contents
            (dirFile!!.contents as EntryDirectory).forEach {
                if (!dirGotcha) { // alternative impl of 'break' as it's not allowed
                    // get name of the file
                    val childDirFile = requestFile(it)!!
                    if (childDirFile.filename.toCanonicalString(charset) == dirName) {
                        //println("[DiskSkimmer] found, $traversedDir -> $it")
                        dirGotcha = true
                        traversedDir = it
                    }
                }
            }

            if (!dirGotcha) return null // got null || directory empty ||
        }

        return requestFile(traversedDir)
    }*/

    fun invalidateEntry(id: EntryID) {
        fa.seek(entryToOffsetTable[id]!! + 8)
        val type = fa.read()
        fa.seek(entryToOffsetTable[id]!! + 8)
        fa.write(type or 128)
        entryToOffsetTable.remove(id)
    }

    ///////////////////////////////////////////////////////
    // THESE ARE METHODS TO SUPPORT ON-LINE MODIFICATION //
    ///////////////////////////////////////////////////////

    fun appendEntry(entry: DiskEntry) {
        val parentDir = requestFile(entry.parentEntryID)!!
        val id = entry.entryID
        val parent = entry.parentEntryID

        // add the entry to its parent directory if there was none
        val dirContent = (parentDir.contents as EntryDirectory)
        if (!dirContent.contains(id)) dirContent.add(id)

        invalidateEntry(parent)
        invalidateEntry(id)

        val appendAt = fa.length()
        fa.seek(appendAt)

        // append new file
        entryToOffsetTable[id] = appendAt + 8
        entry.serialize().forEach { fa.writeByte(it.toInt()) }
        // append modified directory
        entryToOffsetTable[parent] = fa.filePointer + 8
        parentDir.serialize().forEach { fa.writeByte(it.toInt()) }
    }

    fun deleteEntry(id: EntryID) {
        val entry = requestFile(id)!!
        val parentDir = requestFile(entry.parentEntryID)!!
        val parent = entry.parentEntryID

        invalidateEntry(parent)

        // remove the entry
        val dirContent = (parentDir.contents as EntryDirectory)
        dirContent.remove(id)

        val appendAt = fa.length()
        fa.seek(appendAt)

        // append modified directory
        entryToOffsetTable[id] = appendAt + 8
        parentDir.serialize().forEach { fa.writeByte(it.toInt()) }
    }

    fun appendEntries(entries: List<DiskEntry>) = entries.forEach { appendEntry(it) }
    fun deleteEntries(entries: List<EntryID>) = entries.forEach { deleteEntry(it) }

    /**
     * Writes new clean file
     */
    fun sync(): VirtualDisk {
        // rebuild VirtualDisk out of this and use it to write out
        return VDUtil.readDiskArchive(diskFile, charset = charset)
    }


    fun dispose() {
        fa.close()
    }


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

        println("[DiskSkimmer.getEntryBlockSize] offset for entry $id = $offset")

        val fis = FileInputStream(diskFile)
        fis.skip(offset + 8)
        val type = fis.read().toByte()
        fis.skip(272) // skip name, timestamp and CRC


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
}