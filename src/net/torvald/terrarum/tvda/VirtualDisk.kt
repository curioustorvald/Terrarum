package net.torvald.terrarum.tvda

import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import java.util.zip.CRC32
import kotlin.experimental.and
import kotlin.experimental.or


/*
# Terran Virtual Disk Image Format Specification

current specversion number: 254

## Changes

Version 254 is a customised version of TEVD tailored to be used as a savegame format for Terrarum.

### 254
- Removed compressed file; instead we're providing compression tool
- Footer moved upto the header (thus freeing the entry id 0xFEFEFEFE)
- Entry IDs are extended to 8 bytes
- Removed file name field

### 0x03
- Option to compress file entry

### 0x02
- 48-Bit filesize and timestamp (Max 256 TiB / 8.9 million years)
- 8 Reserved footer

### 0x01
**Note: this version were never released in public**
- Doubly Linked List instead of Singly


## Specs

* File structure


    Header
    <entry>
    <entry>
    <entry>
    ...


* Order of the indices does not matter. Actual sorting is a job of the application.
* Endianness: Big


##  Header
    Uint8[4]    Magic: TEVd
    Int48       Disk size in bytes (max 256 TiB)
    Uint8[32]   Disk name
    Int32       CRC-32
                1. create list of arrays that contains CRC
                2. put all the CRCs of entries
                3. sort the list (here's the catch -- you will treat CRCs as SIGNED integer)
                4. for elems on list: update crc with the elem (crc = calculateCRC(crc, elem))
    Int8        Version
    Int8        0xFE
    Int8        Disk properties flag 1
                0th bit: readonly
    Int8[15]    Extra info bytes
    Unit8[236]  Rest of the long disk name (268 bytes total)

    (Header size: 300 bytes)



##  IndexNumber and Contents
    <Entry Header>
    <Actual Entry>

NOTES:
- entries are not guaranteed to be sorted, even though the disk cracker will make it sorted.
- Root entry (ID=0) however, must be the first entry that comes right after the header.
- Parent node of the root is undefined; do not make an assumption that root node's parent is 0.

###  Entry Header
    Int64       EntryID (random Long). This act as "jump" position for directory listing.
                NOTE: Index 0 must be a root "Directory"
    Int64       EntryID of parent directory
    UInt8       Flag for file or directory or symlink
                0b d000 00tt, where:
                tt - 0x01: Normal file, 0x02: Directory list, 0x03: Symlink
                d - discard the entry if the bit is set
    UInt8[3]    <Reserved>
    Int48       Creation date in real-life UNIX timestamp
    Int48       Last modification date in real-life UNIX timestamp
    Int32       CRC-32 of Actual Entry (entrysize and the actual bytes concatenated)

    (Header size: 36 bytes)

###  Entry of File (Uncompressed)
    Int48       File size in bytes (max 256 TiB)
    <Bytes>     Actual Contents

    (Header size: 6 bytes)

###  Entry of Directory
    Uint32      Number of entries (normal files, other directories, symlinks)
    <Int64s>    Entry listing, contains IndexNumber

    (Header size: 4 bytes)

 */

/**
 * Created by minjaesong on 2021-09-10.
 */

typealias EntryID = Long

val specversion = 254.toByte()

class VirtualDisk(
        /** capacity of 0 makes the disk read-only */
        var capacity: Long,
        var diskName: ByteArray = ByteArray(NAME_LENGTH)
) {
    var extraInfoBytes = ByteArray(16)
    val entries = HashMap<EntryID, DiskEntry>()
    var isReadOnly: Boolean
        set(value) { extraInfoBytes[0] = (extraInfoBytes[0] and 0xFE.toByte()) or value.toBit() }
        get() = capacity == 0L || (extraInfoBytes.size > 0 && extraInfoBytes[0].and(1) == 1.toByte())
    fun getDiskNameString(charset: Charset) = diskName.toCanonicalString(charset)
    val root: DiskEntry
        get() = entries[0]!!

    private fun Boolean.toBit() = if (this) 1.toByte() else 0.toByte()

    internal fun __internalSetFooter__(footer: ByteArray64) {
        extraInfoBytes = footer.toByteArray()
    }

    private fun serializeEntriesOnly(): ByteArray64 {
        val buffer = ByteArray64()
        entries.forEach {
            val serialised = it.value.serialize()
            serialised.forEach { buffer.add(it) }
        }

        return buffer
    }

    fun serialize(): AppendableByteBuffer {
        val entriesBuffer = serializeEntriesOnly()
        val buffer = AppendableByteBuffer(HEADER_SIZE + entriesBuffer.size)
        val crc = hashCode().toBigEndian()

        val diskName0 = diskName.forceSize(NAME_LENGTH)
        val diskName1 = diskName0.sliceArray(0..31).forceSize(32)
        val diskName2 = diskName0.sliceArray(32 until NAME_LENGTH).forceSize(NAME_LENGTH - 32)

        buffer.put(MAGIC)

        buffer.put(capacity.toInt48())
        buffer.put(diskName1)
        buffer.put(crc)
        buffer.put(specversion)
        buffer.put(0xFE.toByte())
        buffer.put(extraInfoBytes)
        buffer.put(diskName2)

        buffer.put(entriesBuffer)

        return buffer
    }

    override fun hashCode(): Int {
        val crcList = IntArray(entries.size)
        var crcListAppendCursor = 0
        entries.forEach { _, u ->
            crcList[crcListAppendCursor] = u.hashCode()
            crcListAppendCursor++
        }
        crcList.sort()
        val crc = CRC32()
        crcList.forEach { crc.update(it) }

        return crc.value.toInt()
    }

    /** Expected size of the virtual disk */
    val usedBytes: Long
        get() = entries.map { it.value.serialisedSize }.sum() + HEADER_SIZE

    fun generateUniqueID(): Long {
        var id: Long
        do {
            id = Random().nextLong()
        } while (null != entries[id])
        return id
    }

    override fun equals(other: Any?) = if (other == null) false else this.hashCode() == other.hashCode()
    override fun toString() = "VirtualDisk(name: ${getDiskNameString(Charsets.UTF_8)}, capacity: $capacity bytes, crc: ${hashCode().toHex()})"

    companion object {
        val HEADER_SIZE = 300L // according to the spec
        val NAME_LENGTH = 268

        val MAGIC = "TEVd".toByteArray()
    }
}

fun diskIDtoReadableFilename(id: EntryID): String = when (id) {
    0L -> "root"
    -1L -> "savegameinfo.json"
    -2L -> "thumbnail.tga.gz"
    -16L -> "blockcodex.json.gz"
    -17L -> "itemcodex.json.gz"
    -18L -> "wirecodex.json.gz"
    -19L -> "materialcodex.json.gz"
    -20L -> "factioncodex.json.gz"
    -1024L -> "apocryphas.json.gz"
    in 1..65535 -> "worldinfo-$id.json"
    in 1048576..2147483647 -> "actor-$id.json"
    in 0x0000_0001_0000_0000L..0x0000_FFFF_FFFF_FFFFL ->
        "World${id.ushr(32)}-L${id.and(0xFF00_0000).ushr(24)}-C${id.and(0xFFFFFF)}.gz"
    else -> "file-$id"
}

class DiskEntry(
        // header
        var entryID: EntryID,
        var parentEntryID: EntryID,
        var creationDate: Long,
        var modificationDate: Long,

        // content
        val contents: DiskEntryContent
): Comparable<DiskEntry> {

    override fun compareTo(other: DiskEntry) = entryID.compareTo(other.entryID)

    val serialisedSize: Long
        get() = contents.getSizeEntry() + HEADER_SIZE

    companion object {
        val HEADER_SIZE = 36L // according to the spec

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
        val buffer = AppendableByteBuffer(HEADER_SIZE + serialisedContents.size)

        buffer.put(entryID.toBigEndian())
        buffer.put(parentEntryID.toBigEndian())
        buffer.put(contents.getTypeFlag())
        buffer.put(0); buffer.put(0); buffer.put(0)
        buffer.put(creationDate.toInt48())
        buffer.put(modificationDate.toInt48())
        buffer.put(this.hashCode().toBigEndian())

        buffer.put(serialisedContents.array)

        return buffer
    }

    override fun hashCode() = contents.serialize().getCRC32()

    override fun equals(other: Any?) = if (other == null) false else this.hashCode() == other.hashCode()

    override fun toString() = "DiskEntry(name: ${diskIDtoReadableFilename(entryID)}, ID: $entryID, parent: $parentEntryID, type: ${contents.getTypeFlag()}, contents size: ${contents.getSizeEntry()}, crc: ${hashCode().toHex()})"
}


fun ByteArray.forceSize(size: Int): ByteArray {
    return ByteArray(size) { if (it < this.size) this[it] else 0.toByte() }
}
interface DiskEntryContent {
    fun serialize(): AppendableByteBuffer
    fun getSizePure(): Long
    fun getSizeEntry(): Long
    fun getContent(): Any
}

/**
 * Do not retrieve bytes directly from this! Use VDUtil.retrieveFile(DiskEntry)
 * And besides, the bytes could be compressed.
 */
open class EntryFile(internal var bytes: ByteArray64) : DiskEntryContent {

    override fun getSizePure() = bytes.size
    override fun getSizeEntry() = getSizePure() + 6

    /** Create new blank file */
    constructor(size: Long): this(ByteArray64(size))

    override fun serialize(): AppendableByteBuffer {
        val buffer = AppendableByteBuffer(getSizeEntry())
        buffer.put(getSizePure().toInt48())
        buffer.put(bytes)
        return buffer
    }

    override fun getContent() = bytes
}
class EntryDirectory(private val entries: ArrayList<EntryID> = ArrayList<EntryID>()) : DiskEntryContent {

    override fun getSizePure() = entries.size * 8L
    override fun getSizeEntry() = getSizePure() + 4
    private fun checkCapacity(toAdd: Long = 1L) {
        if (entries.size + toAdd > 4294967295L)
            throw IOException("Directory entries limit exceeded.")
    }

    fun add(entryID: EntryID) {
        checkCapacity()
        entries.add(entryID)
    }

    fun remove(entryID: EntryID) {
        entries.remove(entryID)
    }

    fun contains(entryID: EntryID) = entries.contains(entryID)

    fun forEach(consumer: (EntryID) -> Unit) = entries.forEach(consumer)

    val entryCount: Int
        get() = entries.size

    override fun serialize(): AppendableByteBuffer {
        val buffer = AppendableByteBuffer(getSizeEntry())
        buffer.put(entries.size.toBigEndian())
        entries.sorted().forEach { indexNumber -> buffer.put(indexNumber.toBigEndian()) }
        return buffer
    }

    override fun getContent() = entries.toLongArray()

    companion object {
        val NEW_ENTRY_SIZE = DiskEntry.HEADER_SIZE + 12L
    }
}
class EntrySymlink(val target: EntryID) : DiskEntryContent {

    override fun getSizePure() = 8L
    override fun getSizeEntry() = 8L

    override fun serialize(): AppendableByteBuffer {
        val buffer = AppendableByteBuffer(getSizeEntry())
        return buffer.put(target.toBigEndian())
    }

    override fun getContent() = target
}


fun Int.toHex() = this.toLong().and(0xFFFFFFFF).toString(16).padStart(8, '0').toUpperCase()
fun Long.toHex() = this.ushr(32).toInt().toHex() + "_" + this.toInt().toHex()
fun Int.toBigEndian(): ByteArray {
    return ByteArray(4) { this.ushr(24 - (8 * it)).toByte() }
}
fun Long.toBigEndian(): ByteArray {
    return ByteArray(8) { this.ushr(56 - (8 * it)).toByte() }
}
fun Long.toInt48(): ByteArray {
    return ByteArray(6) { this.ushr(40 - (8 * it)).toByte() }
}
fun Short.toBigEndian(): ByteArray {
    return byteArrayOf(
            this.div(256).toByte(),
            this.toByte()
    )
}

fun AppendableByteBuffer.getCRC32(): Int {
    val crc = CRC32()
    this.array.forEach { crc.update(it.toInt()) }
    return crc.value.toInt()
}
class AppendableByteBuffer(val size: Long) {
    val array = ByteArray64(size)
    private var offset = 0L

    fun put(byteArray64: ByteArray64): AppendableByteBuffer {
        // it's slow but works
        // can't do system.arrayCopy directly
        byteArray64.forEach { put(it) }
        return this
    }
    fun put(byteArray: ByteArray): AppendableByteBuffer {
        byteArray.forEach { put(it) }
        return this
    }
    fun put(byte: Byte): AppendableByteBuffer {
        array[offset] = byte
        offset += 1
        return this
    }
    fun forEach(consumer: (Byte) -> Unit) = array.forEach(consumer)
}