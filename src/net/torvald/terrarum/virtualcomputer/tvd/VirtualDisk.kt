package net.torvald.terrarum.virtualcomputer.tvd

import java.nio.charset.Charset
import java.util.*
import java.util.function.Consumer
import java.util.zip.CRC32

/**
 * Created by SKYHi14 on 2017-03-31.
 */

typealias EntryID = Int

class VirtualDisk(
        /** capacity of 0 makes the disk read-only */
        var capacity: Int,
        var diskName: ByteArray = ByteArray(NAME_LENGTH)
) {
    val entries = HashMap<EntryID, DiskEntry>()
    val isReadOnly: Boolean
        get() = capacity == 0
    fun getDiskNameString(charset: Charset) = String(diskName, charset)
    val root: DiskEntry
        get() = entries[0]!!


    private fun serializeEntriesOnly(): ByteArray {
        val bufferList = ArrayList<Byte>()
        entries.forEach {
            val serialised = it.value.serialize()
            serialised.forEach { bufferList.add(it) }
        }

        return ByteArray(bufferList.size, { bufferList[it] })
    }

    fun serialize(): AppendableByteBuffer {
        val entriesBuffer = serializeEntriesOnly()
        val buffer = AppendableByteBuffer(HEADER_SIZE + entriesBuffer.size + FOOTER_SIZE)
        val crc = hashCode().toBigEndian()

        buffer.put(MAGIC)
        buffer.put(capacity.toBigEndian())
        buffer.put(diskName.forceSize(NAME_LENGTH))
        buffer.put(crc)
        buffer.put(entriesBuffer)
        buffer.put(FOOTER_START_MARK)
        buffer.put(EOF_MARK)

        return buffer
    }

    override fun hashCode(): Int {
        val crcList = IntArray(entries.size)
        var crcListAppendCursor = 0
        entries.forEach { t, u ->
            crcList[crcListAppendCursor] = u.hashCode()
            crcListAppendCursor++
        }
        crcList.sort()
        val crc = CRC32()
        crcList.forEach { crc.update(it) }

        return crc.value.toInt()
    }

    /** Expected size of the virtual disk */
    val usedBytes: Int
        get() = entries.map { it.value.serialisedSize }.sum() + HEADER_SIZE + FOOTER_SIZE

    fun generateUniqueID(): Int {
        var id: Int
        do {
            id = Random().nextInt()
        } while (null != entries[id] || id == FOOTER_MARKER)
        return id
    }

    override fun equals(other: Any?) = if (other == null) false else this.hashCode() == other.hashCode()
    override fun toString() = "VirtualDisk(name: ${getDiskNameString(Charsets.UTF_8)}, capacity: $capacity bytes, crc: ${hashCode().toHex()})"

    companion object {
        val HEADER_SIZE = 44 // according to the spec
        val FOOTER_SIZE = 6  // footer mark + EOF
        val NAME_LENGTH = 32

        val MAGIC = "TEVd".toByteArray()
        val FOOTER_MARKER = 0xFEFEFEFE.toInt()
        val FOOTER_START_MARK = FOOTER_MARKER.toBigEndian()
        val EOF_MARK = byteArrayOf(0xFF.toByte(), 0x19.toByte())
    }
}


class DiskEntry(
        // header
        var entryID: EntryID,
        var filename: ByteArray = ByteArray(NAME_LENGTH),
        var creationDate: Long,
        var modificationDate: Long,

        // content
        val contents: DiskEntryContent
) {
    fun getFilenameString(charset: Charset) = if (entryID == 0) ROOTNAME else filename.toCanonicalString(charset)

    val serialisedSize: Int
        get() = contents.getSizeEntry() + HEADER_SIZE

    companion object {
        val HEADER_SIZE = 281 // according to the spec
        val ROOTNAME = "(root)"
        val NAME_LENGTH  = 256

        val NORMAL_FILE = 1.toByte()
        val DIRECTORY =   2.toByte()
        val SYMLINK =     3.toByte()
        private fun DiskEntryContent.getTypeFlag() =
                if      (this is EntryFile)      NORMAL_FILE
                else if (this is EntryDirectory) DIRECTORY
                else if (this is EntrySymlink)   SYMLINK
                else 0 // NULL

        fun getTypeString(entry: DiskEntryContent) = when(entry.getTypeFlag()) {
            NORMAL_FILE -> "File"
            DIRECTORY   -> "Directory"
            SYMLINK     -> "Symbolic Link"
            else        -> "(unknown type)"
        }
    }

    fun serialize(): AppendableByteBuffer {
        val serialisedContents = contents.serialize()
        val buffer = AppendableByteBuffer(281 + serialisedContents.size)

        buffer.put(entryID.toBigEndian())
        buffer.put(contents.getTypeFlag())
        buffer.put(filename.forceSize(NAME_LENGTH))
        buffer.put(creationDate.toBigEndian())
        buffer.put(modificationDate.toBigEndian())
        buffer.put(this.hashCode().toBigEndian())
        buffer.put(serialisedContents.array)

        return buffer
    }

    override fun hashCode() = contents.serialize().getCRC32()

    override fun equals(other: Any?) = if (other == null) false else this.hashCode() == other.hashCode()

    override fun toString() = "DiskEntry(name: ${getFilenameString(Charsets.UTF_8)}, index: $entryID, type: ${contents.getTypeFlag()}, crc: ${hashCode().toHex()})"
}


fun ByteArray.forceSize(size: Int): ByteArray {
    return ByteArray(size, { if (it < this.size) this[it] else 0.toByte() })
}
interface DiskEntryContent {
    fun serialize(): AppendableByteBuffer
    fun getSizePure(): Int
    fun getSizeEntry(): Int
}
class EntryFile(var bytes: ByteArray) : DiskEntryContent {

    override fun getSizePure() = bytes.size
    override fun getSizeEntry() = getSizePure() + 4

    /** Create new blank file */
    constructor(size: Int): this(ByteArray(size))

    override fun serialize(): AppendableByteBuffer {
        val buffer = AppendableByteBuffer(getSizeEntry())
        buffer.put(getSizePure().toBigEndian())
        buffer.put(bytes)
        return buffer
    }
}
class EntryDirectory(val entries: ArrayList<EntryID> = ArrayList<EntryID>()) : DiskEntryContent {

    override fun getSizePure() = entries.size * 4
    override fun getSizeEntry() = getSizePure() + 2

    override fun serialize(): AppendableByteBuffer {
        val buffer = AppendableByteBuffer(getSizeEntry())
        buffer.put(entries.size.toShort().toBigEndian())
        entries.forEach { indexNumber -> buffer.put(indexNumber.toBigEndian()) }
        return buffer
    }

    companion object {
        val NEW_ENTRY_SIZE = DiskEntry.HEADER_SIZE + 4
    }
}
class EntrySymlink(val target: EntryID) : DiskEntryContent {

    override fun getSizePure() = 4
    override fun getSizeEntry() = 4

    override fun serialize(): AppendableByteBuffer {
        val buffer = AppendableByteBuffer(4)
        return buffer.put(target.toBigEndian())
    }
}


fun Int.toHex() = this.toLong().and(0xFFFFFFFF).toString(16).padStart(8, '0').toUpperCase()
fun Int.toBigEndian(): ByteArray {
    return ByteArray(4, { this.ushr(24 - (8 * it)).toByte() })
}
fun Long.toBigEndian(): ByteArray {
    return ByteArray(8, { this.ushr(56 - (8 * it)).toByte() })

}
fun Short.toBigEndian(): ByteArray {
    return byteArrayOf(
            this.div(256).toByte(),
            this.toByte()
    )
}
fun AppendableByteBuffer.getCRC32(): Int {
    val crc = CRC32()
    crc.update(this.array)
    return crc.value.toInt()
}
class AppendableByteBuffer(val size: Int) {
    val array = ByteArray(size, { 0.toByte() })
    private var offset = 0

    fun put(byteArray: ByteArray): AppendableByteBuffer {
        System.arraycopy(
                byteArray, // source
                0,         // source pos
                array,     // destination
                offset,    // destination pos
                byteArray.size // length
        )
        offset += byteArray.size
        return this
    }
    fun put(byte: Byte): AppendableByteBuffer {
        array[offset] = byte
        offset += 1
        return this
    }
    fun forEach(consumer: (Byte) -> Unit) = array.forEach(consumer)
}
