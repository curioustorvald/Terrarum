package net.torvald.terrarum.tvda

import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Level
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.naming.OperationNotSupportedException
import kotlin.experimental.and

/**
 * Temporarily disabling on-disk compression; it somehow does not work, compress the files by yourself!
 *
 * Created by minjaesong on 2017-04-01.
 */
object VDUtil {

    fun File.writeBytes64(array: net.torvald.terrarum.tvda.ByteArray64) {
        array.writeToFile(this)
    }

    fun File.readBytes64(): net.torvald.terrarum.tvda.ByteArray64 {
        val inbytes = net.torvald.terrarum.tvda.ByteArray64(this.length())
        val inputStream = BufferedInputStream(FileInputStream(this))
        var readInt = inputStream.read()
        var readInCounter = 0L
        while (readInt != -1) {
            inbytes[readInCounter] = readInt.toByte()
            readInCounter += 1

            readInt = inputStream.read()
        }
        inputStream.close()

        return inbytes
    }

    fun dumpToRealMachine(disk: VirtualDisk, outfile: File) {
        outfile.writeBytes64(disk.serialize().array)
    }

    private const val DEBUG_PRINT_READ = false

    /**
     * Reads serialised binary and returns corresponding VirtualDisk instance.
     *
     * @param crcWarnLevel Level.OFF -- no warning, Level.WARNING -- print out warning, Level.SEVERE -- throw error
     */
    fun readDiskArchive(infile: File, crcWarnLevel: Level = Level.SEVERE, warningFunc: ((String) -> Unit)? = null): VirtualDisk {
        val inbytes = infile.readBytes64()



        if (magicMismatch(VirtualDisk.MAGIC, inbytes.sliceArray64(0L..3L).toByteArray()))
            throw RuntimeException("Invalid Virtual Disk file!")

        val diskSize = inbytes.sliceArray64(4L..9L).toInt48Big()
        val diskName = inbytes.sliceArray(10..10 + 31) + inbytes.sliceArray(10+32+22..10+32+22+235)
        val diskCRC = inbytes.sliceArray64(10L + 32..10L + 32 + 3).toIntBig() // to check with completed vdisk
        val diskSpecVersion = inbytes[10L + 32 + 4]
        val footers = inbytes.sliceArray64(10L+32+6..10L+32+21)

        if (diskSpecVersion != specversion)
            throw RuntimeException("Unsupported disk format version: current internal version is $specversion; the file's version is $diskSpecVersion")

        val vdisk = VirtualDisk(diskSize, diskName)

        vdisk.__internalSetFooter__(footers)

        //println("[VDUtil] currentUnixtime = $currentUnixtime")

        var entryOffset = VirtualDisk.HEADER_SIZE
        // not footer, entries
        while (entryOffset < inbytes.size) {
            //println("[VDUtil] entryOffset = $entryOffset")
            // read and prepare all the shits
            val entryID = inbytes.sliceArray64(entryOffset..entryOffset + 7).toLongBig()
            val entryParentID = inbytes.sliceArray64(entryOffset + 8..entryOffset + 15).toLongBig()
            val entryTypeFlag = inbytes[entryOffset + 16]
            val entryCreationTime = inbytes.sliceArray64(entryOffset + 20..entryOffset + 25).toInt48Big()
            val entryModifyTime = inbytes.sliceArray64(entryOffset + 26..entryOffset + 31).toInt48Big()
            val entryCRC = inbytes.sliceArray64(entryOffset + 32..entryOffset + 35).toIntBig() // to check with completed entry

            val entryData = when (entryTypeFlag and 127) {
                DiskEntry.NORMAL_FILE -> {
                    val filesize = inbytes.sliceArray64(entryOffset + DiskEntry.HEADER_SIZE..entryOffset + DiskEntry.HEADER_SIZE + 5).toInt48Big()
                    //println("[VDUtil] --> is file; filesize = $filesize")
                    inbytes.sliceArray64(entryOffset + DiskEntry.HEADER_SIZE + 6..entryOffset + DiskEntry.HEADER_SIZE + 5 + filesize)
                }
                DiskEntry.DIRECTORY   -> {
                    val entryCount = inbytes.sliceArray64(entryOffset + DiskEntry.HEADER_SIZE..entryOffset + DiskEntry.HEADER_SIZE + 3).toIntBig()
                    //println("[VDUtil] --> is directory; entryCount = $entryCount")
                    inbytes.sliceArray64(entryOffset + DiskEntry.HEADER_SIZE + 4..entryOffset + DiskEntry.HEADER_SIZE + 3 + entryCount * 8)
                }
                DiskEntry.SYMLINK     -> {
                    inbytes.sliceArray64(entryOffset + DiskEntry.HEADER_SIZE..entryOffset + DiskEntry.HEADER_SIZE + 7)
                }
                else -> throw RuntimeException("Unknown entry with type $entryTypeFlag at entryOffset $entryOffset")
            }

            if (DEBUG_PRINT_READ) {
                println("[tvda.VDUtil] == Entry deserialise debugprint for entry ID $entryID (child of $entryParentID)")
                println("Entry type flag: ${entryTypeFlag and 127}${if (entryTypeFlag < 0) "*" else ""}")
                println("Entry raw contents bytes: (len: ${entryData.size})")
                entryData.forEachIndexed { i, it ->
                    if (i > 0 && i % 8 == 0L) print(" ")
                    else if (i > 0 && i % 4 == 0L) print("_")
                    print(it.toInt().toHex().substring(6))
                }; println()
            }


            // update entryOffset so that we can fetch next entry in the binary
            entryOffset += DiskEntry.HEADER_SIZE + entryData.size + when (entryTypeFlag and 127) {
                DiskEntry.NORMAL_FILE -> 6      // PLEASE DO REFER TO Spec.md
                DiskEntry.DIRECTORY   -> 4      // PLEASE DO REFER TO Spec.md
                DiskEntry.SYMLINK     -> 0      // PLEASE DO REFER TO Spec.md
                else -> throw RuntimeException("Unknown entry with type $entryTypeFlag")
            }


            // check for the discard bit
            if (entryTypeFlag > 0) {

                // create entry
                val diskEntry = DiskEntry(
                    entryID = entryID,
                    parentEntryID = entryParentID,
                    creationDate = entryCreationTime,
                    modificationDate = entryModifyTime,
                    contents = if (entryTypeFlag == DiskEntry.NORMAL_FILE) {
                        EntryFile(entryData)
                    } else if (entryTypeFlag == DiskEntry.DIRECTORY) {

                        val entryList = ArrayList<EntryID>()

                        (0 until entryData.size / 8).forEach { cnt ->
                            entryList.add(entryData.sliceArray64(8 * cnt until 8 * (cnt+1)).toLongBig())
                        }

                        entryList.sort()

                        EntryDirectory(entryList)
                    } else if (entryTypeFlag == DiskEntry.SYMLINK) {
                        EntrySymlink(entryData.toLongBig())
                    } else
                        throw RuntimeException("Unknown entry with type $entryTypeFlag")
                )

                // check CRC of entry
                if (crcWarnLevel == Level.SEVERE || crcWarnLevel == Level.WARNING) {

                    // test print
                    if (DEBUG_PRINT_READ) {
                        val testbytez = diskEntry.contents.serialize()
                        val testbytes = testbytez.array
                        (diskEntry.contents as? EntryDirectory)?.forEach {
                            println("entry: ${it.toHex()}")
                        }
                        println("[tvda.VDUtil] bytes to calculate crc against:")
                        testbytes.forEachIndexed { i, it ->
                            if (i % 4 == 0L) print(" ")
                            print(it.toInt().toHex().substring(6))
                        }
                        println("\nCRC: " + testbytez.getCRC32().toHex())
                    }
                    // end of test print

                    val calculatedCRC = diskEntry.contents.serialize().getCRC32()

                    val crcMsg =
                        "CRC failed: stored value is ${entryCRC.toHex()}, but calculated value is ${calculatedCRC.toHex()}\n" +
                                "at file \"${diskIDtoReadableFilename(diskEntry.entryID)}\" (entry ID ${diskEntry.entryID})"

                    if (calculatedCRC != entryCRC) {

                        println("[tvda.VDUtil] CRC failed; entry info:\n$diskEntry")

                        if (crcWarnLevel == Level.SEVERE)
                            throw IOException(crcMsg)
                        else if (warningFunc != null)
                            warningFunc(crcMsg)
                    }
                }

                // add entry to disk
                vdisk.entries[entryID] = diskEntry
            }
            else {
                if (DEBUG_PRINT_READ) {
                    println("[tvda.VDUtil] Discarding entry ${entryID.toHex()} (raw type flag: $entryTypeFlag)")
                }
            }
        }

        // check CRC of disk
        if (crcWarnLevel == Level.SEVERE || crcWarnLevel == Level.WARNING) {
            val calculatedCRC = vdisk.hashCode()

            val crcMsg = "Disk CRC failed: expected ${diskCRC.toHex()}, got ${calculatedCRC.toHex()}"

            if (calculatedCRC != diskCRC) {
                if (crcWarnLevel == Level.SEVERE)
                    throw IOException(crcMsg)
                else if (warningFunc != null)
                    warningFunc(crcMsg)
            }
        }

        return vdisk
    }


    fun isFile(disk: VirtualDisk, entryID: EntryID) = disk.entries[entryID]?.contents is EntryFile
    fun isDirectory(disk: VirtualDisk, entryID: EntryID) = disk.entries[entryID]?.contents is EntryDirectory
    fun isSymlink(disk: VirtualDisk, entryID: EntryID) = disk.entries[entryID]?.contents is EntrySymlink

    /**
     * Get list of entries of directory.
     */
    fun getDirectoryEntries(disk: VirtualDisk, dirToSearch: DiskEntry): Array<DiskEntry> {
        if (dirToSearch.contents !is EntryDirectory)
            throw IllegalArgumentException("The entry is not directory")

        val entriesList = ArrayList<DiskEntry>()
        dirToSearch.contents.forEach {
            val entry = disk.entries[it]
            if (entry != null) entriesList.add(entry)
        }

        return entriesList.toTypedArray()
    }
    /**
     * Get list of entries of directory.
     */
    fun getDirectoryEntries(disk: VirtualDisk, entryID: EntryID): Array<DiskEntry> {
        val entry = disk.entries[entryID]
        if (entry == null) {
            throw IOException("Entry does not exist")
        }
        else {
            return getDirectoryEntries(disk, entry)
        }
    }

    /**
     * SYNOPSIS  disk.getFile("bin/msh.lua")!!.file.getAsNormalFile(disk)
     *
     * Use VirtualDisk.getAsNormalFile(path)
     */
    private fun DiskEntry.getAsNormalFile(disk: VirtualDisk): EntryFile =
            this.contents as? EntryFile ?:
                    if (this.contents is EntryDirectory)
                        throw RuntimeException("this is directory")
                    else if (this.contents is EntrySymlink)
                        disk.entries[this.contents.target]!!.getAsNormalFile(disk)
                    else
                        throw RuntimeException("Unknown entry type")
    /**
     * SYNOPSIS  disk.getFile("bin/msh.lua")!!.first.getAsNormalFile(disk)
     *
     * Use VirtualDisk.getAsNormalFile(path)
     */
    private fun DiskEntry.getAsDirectory(disk: VirtualDisk): EntryDirectory =
            this.contents as? EntryDirectory ?:
                    if (this.contents is EntrySymlink)
                        disk.entries[this.contents.target]!!.getAsDirectory(disk)
                    else if (this.contents is EntryFile)
                        throw RuntimeException("this is not directory")
                    else
                        throw RuntimeException("Unknown entry type")

    /**
     * Fetch the file and returns a instance of normal file.
     */
    fun getAsNormalFile(disk: VirtualDisk, entryIndex: EntryID) =
            disk.entries[entryIndex]!!.getAsNormalFile(disk)
    /**
     * Fetch the file and returns a instance of directory.
     */
    fun getAsDirectory(disk: VirtualDisk, entryIndex: EntryID) =
            disk.entries[entryIndex]!!.getAsDirectory(disk)

    /**
     * Deletes file on the disk safely.
     */
    fun deleteFile(disk: VirtualDisk, targetID: EntryID) {
        disk.checkReadOnly()

        val file = disk.entries[targetID]

        if (file == null) {
            throw FileNotFoundException("No such file to delete")
        }

        val parentID = file.parentEntryID
        val parentDir = getAsDirectory(disk, parentID)

        fun rollback() {
            if (!disk.entries.contains(targetID)) {
                disk.entries[targetID] = file
            }
            if (!parentDir.contains(targetID)) {
                parentDir.add(targetID)
            }
        }

        // check if directory "parentID" has "targetID" in the first place
        if (!directoryContains(disk, parentID, targetID)) {
            throw FileNotFoundException("No such file to delete")
        }
        else if (targetID == 0L) {
            throw IOException("Cannot delete root file system")
        }
        else {
            try {
                // delete file record
                disk.entries.remove(targetID)
                // unlist file from parent directly
                parentDir.remove(targetID)
            }
            catch (e: Exception) {
                rollback()
                throw InternalError("Unknown error *sigh* It's annoying, I know.")
            }
        }
    }
    /**
     * Changes the name of the entry.
     */
    fun renameFile(disk: VirtualDisk, fileID: EntryID, newID: EntryID, charset: Charset) {
        val file = disk.entries[fileID]

        if (file != null) {
            file.entryID = newID
        }
        else {
            throw FileNotFoundException()
        }
    }
    /**
     * Add file to the specified directory.
     * The file will get new EntryID and its ParentID will be overwritten.
     */
    fun addFile(disk: VirtualDisk, file: DiskEntry) {
        disk.entries[file.entryID] = file
        file.parentEntryID = 0
        val dir = VDUtil.getAsDirectory(disk, 0)
        if (!dir.contains(file.entryID)) dir.add(file.entryID)
    }

    fun randomBase62(length: Int): String {
        val glyphs = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val sb = StringBuilder()

        kotlin.repeat(length) {
            sb.append(glyphs[(Math.random() * glyphs.length).toInt()])
        }

        return sb.toString()
    }

    /**
     * Add fully qualified DiskEntry to the disk, using file's own and its parent entryID.
     *
     * It's your job to ensure no ID collision.
     */
    fun registerFile(disk: VirtualDisk, file: DiskEntry) {
        disk.checkReadOnly()
        disk.checkCapacity(file.serialisedSize)

        VDUtil.getAsDirectory(disk, file.parentEntryID).add(file.entryID)
        disk.entries[file.entryID] = file
    }

    /**
     * Add file to the specified directory. ParentID of the file will be overwritten.
     */
    fun addFile(disk: VirtualDisk, directoryID: EntryID, file: DiskEntry) {//}, compressTheFile: Boolean = false) {
        disk.checkReadOnly()
        disk.checkCapacity(file.serialisedSize)

        try {
            // generate new ID for the file
            file.entryID = disk.generateUniqueID()
            // add record to the directory
            getAsDirectory(disk, directoryID).add(file.entryID)

            // Gzip fat boy if marked as
            /*if (compressTheFile && file.contents is EntryFile) {
                val bo = ByteArray64GrowableOutputStream()
                val zo = GZIPOutputStream(bo)

                // zip
                file.contents.bytes.forEach {
                    zo.write(it.toInt())
                }
                zo.flush(); zo.close()

                val newContent = EntryFileCompressed(file.contents.bytes.size, bo.toByteArray64())
                val newEntry = DiskEntry(
                        file.entryID, file.parentEntryID, file.filename, file.creationDate, file.modificationDate,
                        newContent
                )

                disk.entries[file.entryID] = newEntry
            }
            // just the add the boy to the house
            else*/
                disk.entries[file.entryID] = file

            // make this boy recognise his new parent
            file.parentEntryID = directoryID
        }
        catch (e: KotlinNullPointerException) {
            throw FileNotFoundException("No such directory")
        }
    }

    /**
     * Imports external file and returns corresponding DiskEntry.
     */
    fun importFile(file: File, newID: EntryID, charset: Charset): DiskEntry {
        if (file.isDirectory) {
            throw IOException("The file is a directory")
        }

        return DiskEntry(
                entryID = newID,
                parentEntryID = 0, // placeholder
                creationDate = currentUnixtime,
                modificationDate = currentUnixtime,
                contents = EntryFile(file.readBytes64())
        )
    }

    /**
     * Export file on the virtual disk into real disk.
     */
    fun exportFile(entryFile: EntryFile, outfile: File) {
        outfile.createNewFile()

        /*if (entryFile is EntryFileCompressed) {
            entryFile.bytes.forEachBanks {
                val fos = FileOutputStream(outfile)
                val inflater = InflaterOutputStream(fos)

                inflater.write(it)
                inflater.flush()
                inflater.close()
            }
        }
        else*/
            outfile.writeBytes64(entryFile.bytes)
    }


    /**
     * Creates new disk with given name and capacity
     */
    fun createNewDisk(diskSize: Long, diskName: String, charset: Charset): VirtualDisk {
        val newdisk = VirtualDisk(diskSize, diskName.toEntryName(VirtualDisk.NAME_LENGTH, charset))
        val rootDir = DiskEntry(
                entryID = 0,
                parentEntryID = 0,
                creationDate = currentUnixtime,
                modificationDate = currentUnixtime,
                contents = EntryDirectory()
        )

        newdisk.entries[0] = rootDir

        return newdisk
    }


    /**
     * Throws an exception if the disk is read-only
     */
    fun VirtualDisk.checkReadOnly() {
        if (this.isReadOnly)
            throw IOException("Disk is read-only")
    }
    /**
     * Throws an exception if specified size cannot fit into the disk
     */
    fun VirtualDisk.checkCapacity(newSize: Long) {
        if (this.usedBytes + newSize > this.capacity)
            throw IOException("Not enough space on the disk")
    }
    fun ByteArray64.toIntBig(): Int {
        if (this.size != 4L)
            throw OperationNotSupportedException("ByteArray is not Int")

        var i = 0
        var c = 0
        this.forEach { byte -> i = i or byte.toUint().shl(24 - c * 8); c += 1 }
        return i
    }
    fun ByteArray64.toLongBig(): Long {
        if (this.size != 8L)
            throw OperationNotSupportedException("ByteArray is not Long")

        var i = 0L
        var c = 0
        this.forEach { byte -> i = i or byte.toUlong().shl(56 - c * 8); c += 1 }
        return i
    }
    fun ByteArray64.toInt48Big(): Long {
        if (this.size != 6L)
            throw OperationNotSupportedException("ByteArray is not Long")

        var i = 0L
        var c = 0
        this.forEach { byte -> i = i or byte.toUlong().shl(40 - c * 8); c += 1 }
        return i
    }
    fun ByteArray64.toShortBig(): Short {
        if (this.size != 2L)
            throw OperationNotSupportedException("ByteArray is not Short")

        return (this[0].toUint().shl(256) + this[1].toUint()).toShort()
    }
    fun String.sanitisePath(): String {
        val invalidChars = Regex("""[<>:"|?*\u0000-\u001F]""")
        if (this.contains(invalidChars))
            throw IOException("path contains invalid characters")

        val path1 = this.replace('\\', '/')
        return path1
    }

    fun resolveIfSymlink(disk: VirtualDisk, indexNumber: EntryID, recurse: Boolean = false): DiskEntry {
        var entry: DiskEntry? = disk.entries[indexNumber]
        if (entry == null) throw IOException("File does not exist")
        if (entry.contents !is EntrySymlink) return entry
        if (recurse) {
            while (entry!!.contents is EntrySymlink) {
                entry = disk.entries[(entry.contents as EntrySymlink).target]
                if (entry == null) break
            }
        }
        else {
            entry = disk.entries[(entry.contents as EntrySymlink).target]
        }
        if (entry == null) throw IOException("Pointing file does not exist")
        return entry
    }

    val currentUnixtime: Long
        get() = System.currentTimeMillis() / 1000

    fun directoryContains(disk: VirtualDisk, dirID: EntryID, targetID: EntryID): Boolean {
        val dir = resolveIfSymlink(disk, dirID)

        if (dir.contents !is EntryDirectory) {
            throw FileNotFoundException("Not a directory")
        }
        else {
            return dir.contents.contains(targetID)
        }
    }

    /**
     * Searches for disconnected nodes using its parent pointer.
     * If the parent node is invalid, the node is considered orphan, and will be added
     * to the list this function returns.
     *
     * @return List of orphan entries
     */
    fun gcSearchOrphan(disk: VirtualDisk): List<EntryID> {
        return disk.entries.filter { disk.entries[it.value.parentEntryID] == null }.keys.toList()
    }

    /**
     * Searches for null-pointing entries (phantoms) within every directory.
     *
     * @return List of search results, which is Pair(directory that contains null pointer, null pointer)
     */
    fun gcSearchPhantomBaby(disk: VirtualDisk): List<Pair<EntryID, EntryID>> {
        // Pair<DirectoryID, ID of phantom in the directory>
        val phantoms = ArrayList<Pair<EntryID, EntryID>>()
        disk.entries.filter { it.value.contents is EntryDirectory }.values.forEach { directory ->
            (directory.contents as EntryDirectory).forEach { dirEntryID ->
                if (disk.entries[dirEntryID] == null) {
                    phantoms.add(Pair(directory.entryID, dirEntryID))
                }
            }
        }
        return phantoms
    }

    fun gcDumpOrphans(disk: VirtualDisk) {
        try {
            gcSearchOrphan(disk).forEach {
                disk.entries.remove(it)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            throw InternalError("Aw, snap!")
        }
    }

    fun gcDumpAll(disk: VirtualDisk) {
        try {
            gcSearchPhantomBaby(disk).forEach {
                getAsDirectory(disk, it.first).remove(it.second)
            }
            gcSearchOrphan(disk).forEach {
                disk.entries.remove(it)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            throw InternalError("Aw, snap!")
        }
    }

    fun compress(ba: ByteArray64) = compress(ba.iterator())
    fun compress(byteIterator: Iterator<Byte>): ByteArray64 {
        val bo = ByteArray64GrowableOutputStream()
        val zo = GZIPOutputStream(bo)

        // zip
        byteIterator.forEach {
            zo.write(it.toInt())
        }
        zo.flush(); zo.close()
        return bo.toByteArray64()
    }

    fun decompress(bytes: ByteArray64): ByteArray64 {
        val unzipdBytes = ByteArray64()
        val zi = GZIPInputStream(ByteArray64InputStream(bytes))
        while (true) {
            val byte = zi.read()
            if (byte == -1) break
            unzipdBytes.add(byte.toByte())
        }
        zi.close()
        return unzipdBytes
    }
}

fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
fun Byte.toUlong() = java.lang.Byte.toUnsignedLong(this)
fun magicMismatch(magic: ByteArray, array: ByteArray): Boolean {
    return !Arrays.equals(array, magic)
}
fun String.toEntryName(length: Int, charset: Charset): ByteArray {
    val buffer = AppendableByteBuffer(length.toLong())
    val stringByteArray = this.toByteArray(charset)
    buffer.put(stringByteArray.sliceArray(0..minOf(length, stringByteArray.size) - 1))
    return buffer.array.toByteArray()
}
fun ByteArray.toCanonicalString(charset: Charset): String {
    var lastIndexOfRealStr = 0
    for (i in this.lastIndex downTo 0) {
        if (this[i] != 0.toByte()) {
            lastIndexOfRealStr = i
            break
        }
    }
    return String(this.sliceArray(0..lastIndexOfRealStr), charset)
}

fun ByteArray.toByteArray64(): ByteArray64 {
    val array = ByteArray64(this.size.toLong())
    this.forEachIndexed { index, byte ->
        array[index.toLong()] = byte
    }
    return array
}

/**
 * Writes String to the file
 *
 * Note: this FileWriter cannot write more than 2 GiB
 *
 * @param fileEntry must be File, resolve symlink beforehand
 * @param mode "w" or "a"
 */
class VDFileWriter(private val fileEntry: DiskEntry, private val append: Boolean, val charset: Charset) : Writer() {

    private @Volatile var newFileBuffer = ArrayList<Byte>()

    private @Volatile var closed = false

    init {
        if (fileEntry.contents !is EntryFile) {
            throw FileNotFoundException("Not a file")
        }
    }

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        if (!closed) {
            val newByteArray = String(cbuf).toByteArray(charset).toByteArray64()
            newByteArray.forEach { newFileBuffer.add(it) }
        }
        else {
            throw IOException()
        }
    }

    override fun flush() {
        if (!closed) {
            val newByteArray = newFileBuffer.toByteArray()

            if (!append) {
                (fileEntry.contents as EntryFile).bytes = newByteArray.toByteArray64()
            }
            else {
                val oldByteArray = (fileEntry.contents as EntryFile).bytes.toByteArray().copyOf()
                val newFileBuffer = ByteArray(oldByteArray.size + newByteArray.size)

                System.arraycopy(oldByteArray, 0, newFileBuffer, 0, oldByteArray.size)
                System.arraycopy(newByteArray, 0, newFileBuffer, oldByteArray.size, newByteArray.size)

                fileEntry.contents.bytes = newByteArray.toByteArray64()
            }

            newFileBuffer = ArrayList<Byte>()

            fileEntry.modificationDate = VDUtil.currentUnixtime
        }
        else {
            throw IOException()
        }
    }

    override fun close() {
        flush()
        closed = true
    }
}

class VDFileOutputStream(private val fileEntry: DiskEntry, private val append: Boolean, val charset: Charset) : OutputStream() {

    private @Volatile var newFileBuffer = ArrayList<Byte>()

    private @Volatile var closed = false

    override fun write(b: Int) {
        if (!closed) {
            newFileBuffer.add(b.toByte())
        }
        else {
            throw IOException()
        }
    }

    override fun flush() {
        if (!closed) {
            val newByteArray = newFileBuffer.toByteArray()

            if (!append) {
                (fileEntry.contents as EntryFile).bytes = newByteArray.toByteArray64()
            }
            else {
                val oldByteArray = (fileEntry.contents as EntryFile).bytes.toByteArray().copyOf()
                val newFileBuffer = ByteArray(oldByteArray.size + newByteArray.size)

                System.arraycopy(oldByteArray, 0, newFileBuffer, 0, oldByteArray.size)
                System.arraycopy(newByteArray, 0, newFileBuffer, oldByteArray.size, newByteArray.size)

                fileEntry.contents.bytes = newByteArray.toByteArray64()
            }

            newFileBuffer = ArrayList<Byte>()

            fileEntry.modificationDate = VDUtil.currentUnixtime
        }
        else {
            throw IOException()
        }
    }

    override fun close() {
        flush()
        closed = true
    }
}