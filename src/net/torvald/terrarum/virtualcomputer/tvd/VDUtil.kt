package net.torvald.terrarum.virtualcomputer.tvd

import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Level
import javax.naming.OperationNotSupportedException
import kotlin.collections.ArrayList

/**
 * Created by SKYHi14 on 2017-04-01.
 */
object VDUtil {
    class VDPath() {
        /**
         * input: (root)->etc->boot in Constructor
         * output: ByteArrayListOf(
         *      e t c \0 \0 \0 \0 \0 ... ,
         *      b o o t \0 \0 \0 \0 ...
         * )
         *
         * input: "/"
         * interpretation: (root)
         * output: ByteArrayListOf(
         * )
         */
        var hierarchy = ArrayList<ByteArray>()

        val lastIndex: Int
            get() = hierarchy.lastIndex
        fun last(): ByteArray = hierarchy.last()

        constructor(strPath: String, charset: Charset) : this() {
            val unsanitisedHierarchy = ArrayList<String>()
            strPath.sanitisePath().split('/').forEach { unsanitisedHierarchy.add(it) }

            // deal with bad slashes (will drop '' and tail '')
            // "/bin/boot/drivers/" -> "bin/boot/drivers"
            //  removes head slash
            if (unsanitisedHierarchy[0].isEmpty())
                unsanitisedHierarchy.removeAt(0)
            //  removes tail slash
            if (unsanitisedHierarchy.size > 0 &&
                    unsanitisedHierarchy[unsanitisedHierarchy.lastIndex].isEmpty())
                unsanitisedHierarchy.removeAt(unsanitisedHierarchy.lastIndex)

            unsanitisedHierarchy.forEach {
                hierarchy.add(it.toEntryName(DiskEntry.NAME_LENGTH, charset))
            }
        }

        private constructor(newHierarchy: ArrayList<ByteArray>) : this() {
            hierarchy = newHierarchy
        }

        override fun toString(): String {
            val sb = StringBuilder()
            if (hierarchy.size > 0) {
                sb.append(hierarchy[0].toCanonicalString(Charsets.UTF_8))
            }
            if (hierarchy.size > 1) {
                (1..hierarchy.lastIndex).forEach {
                    sb.append('/')
                    sb.append(hierarchy[it].toCanonicalString(Charsets.UTF_8))
                }
            }

            return sb.toString()
        }

        operator fun get(i: Int) = hierarchy[i]
        fun forEach(action: (ByteArray) -> Unit) = hierarchy.forEach(action)
        fun forEachIndexed(action: (Int, ByteArray) -> Unit) = hierarchy.forEachIndexed(action)

        fun getParent(ancestorCount: Int = 1): VDPath {
            val newPath = ArrayList<ByteArray>()
            hierarchy.forEach { newPath.add(it) }

            repeat(ancestorCount) { newPath.removeAt(newPath.lastIndex) }
            return VDPath(newPath)
        }
    }

    fun File.writeBytes64(array: ByteArray64) {
        array.writeToFile(this)
    }

    fun File.readBytes64(): ByteArray64 {
        val inbytes = ByteArray64(this.length())
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
        if (!outfile.exists()) outfile.createNewFile()
        outfile.writeBytes64(disk.serialize().array)
    }

    /**
     * Reads serialised binary and returns corresponding VirtualDisk instance.
     *
     * @param crcWarnLevel Level.OFF -- no warning, Level.WARNING -- print out warning, Level.SEVERE -- throw error
     */
    fun readDiskArchive(infile: File, crcWarnLevel: Level = Level.SEVERE, warningFunc: ((String) -> Unit)? = null, charset: Charset): VirtualDisk {
        val inbytes = infile.readBytes64()



        if (magicMismatch(VirtualDisk.MAGIC, inbytes.sliceArray(0L..3L).toByteArray()))
            throw RuntimeException("Invalid Virtual Disk file!")

        val diskSize = inbytes.sliceArray(4L..9L).toInt48Big()
        val diskName = inbytes.sliceArray(10L..10L + 31)
        val diskCRC = inbytes.sliceArray(10L + 32..10L + 32 + 3).toIntBig() // to check with completed vdisk
        val diskSpecVersion = inbytes[10L + 32 + 4]


        if (diskSpecVersion != specversion)
            throw RuntimeException("Unsupported disk format version: current internal version is $specversion; the file's version is $diskSpecVersion")

        val vdisk = VirtualDisk(diskSize, diskName.toByteArray())

        //println("[VDUtil] currentUnixtime = $currentUnixtime")

        var entryOffset = VirtualDisk.HEADER_SIZE
        while (!Arrays.equals(inbytes.sliceArray(entryOffset..entryOffset + 3).toByteArray(), VirtualDisk.FOOTER_START_MARK)) {
            //println("[VDUtil] entryOffset = $entryOffset")
            // read and prepare all the shits
            val entryID = inbytes.sliceArray(entryOffset..entryOffset + 3).toIntBig()
            val entryParentID = inbytes.sliceArray(entryOffset + 4..entryOffset + 7).toIntBig()
            val entryTypeFlag = inbytes[entryOffset + 8]
            val entryFileName = inbytes.sliceArray(entryOffset + 9..entryOffset + 9 + 255).toByteArray()
            val entryCreationTime = inbytes.sliceArray(entryOffset + 265..entryOffset + 270).toInt48Big()
            val entryModifyTime = inbytes.sliceArray(entryOffset + 271..entryOffset + 276).toInt48Big()
            val entryCRC = inbytes.sliceArray(entryOffset + 277..entryOffset + 280).toIntBig() // to check with completed entry

            val entryData = when (entryTypeFlag) {
                DiskEntry.NORMAL_FILE -> {
                    val filesize = inbytes.sliceArray(entryOffset + DiskEntry.HEADER_SIZE..entryOffset + DiskEntry.HEADER_SIZE + 5).toInt48Big()
                    //println("[VDUtil] --> is file; filesize = $filesize")
                    inbytes.sliceArray(entryOffset + DiskEntry.HEADER_SIZE + 6..entryOffset + DiskEntry.HEADER_SIZE + 5 + filesize)
                }
                DiskEntry.DIRECTORY   -> {
                    val entryCount = inbytes.sliceArray(entryOffset + DiskEntry.HEADER_SIZE..entryOffset + DiskEntry.HEADER_SIZE + 1).toShortBig()
                    //println("[VDUtil] --> is directory; entryCount = $entryCount")
                    inbytes.sliceArray(entryOffset + DiskEntry.HEADER_SIZE + 2..entryOffset + DiskEntry.HEADER_SIZE + 1 + entryCount * 4)
                }
                DiskEntry.SYMLINK     -> {
                    inbytes.sliceArray(entryOffset + DiskEntry.HEADER_SIZE..entryOffset + DiskEntry.HEADER_SIZE + 3)
                }
                else -> throw RuntimeException("Unknown entry with type $entryTypeFlag at entryOffset $entryOffset")
            }



            // update entryOffset so that we can fetch next entry in the binary
            entryOffset += DiskEntry.HEADER_SIZE + entryData.size + when (entryTypeFlag) {
                DiskEntry.NORMAL_FILE -> 6
                DiskEntry.DIRECTORY   -> 2
                DiskEntry.SYMLINK     -> 0
                else -> throw RuntimeException("Unknown entry with type $entryTypeFlag")
            }


            // create entry
            val diskEntry = DiskEntry(
                    entryID = entryID,
                    parentEntryID = entryParentID,
                    filename = entryFileName,
                    creationDate = entryCreationTime,
                    modificationDate = entryModifyTime,
                    contents = if (entryTypeFlag == DiskEntry.NORMAL_FILE) {
                        EntryFile(entryData)
                    }
                    else if (entryTypeFlag == DiskEntry.DIRECTORY) {
                        val entryList = ArrayList<EntryID>()
                        (0..entryData.size / 4 - 1).forEach {
                            entryList.add(entryData.sliceArray(4 * it..4 * it + 3).toIntBig())
                        }

                        EntryDirectory(entryList)
                    }
                    else if (entryTypeFlag == DiskEntry.SYMLINK) {
                        EntrySymlink(entryData.toIntBig())
                    }
                    else
                        throw RuntimeException("Unknown entry with type $entryTypeFlag")
            )

            // check CRC of entry
            if (crcWarnLevel == Level.SEVERE || crcWarnLevel == Level.WARNING) {
                val calculatedCRC = diskEntry.hashCode()

                val crcMsg = "CRC failed: expected ${entryCRC.toHex()}, got ${calculatedCRC.toHex()}\n" +
                        "at file \"${diskEntry.getFilenameString(charset)}\" (entry ID ${diskEntry.entryID})"

                if (calculatedCRC != entryCRC) {
                    if (crcWarnLevel == Level.SEVERE)
                        throw IOException(crcMsg)
                    else if (warningFunc != null)
                        warningFunc(crcMsg)
                }
            }

            // add entry to disk
            vdisk.entries[entryID] = diskEntry
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


    /**
     * Get list of entries of directory.
     */
    fun getDirectoryEntries(disk: VirtualDisk, dirToSearch: DiskEntry): Array<DiskEntry> {
        if (dirToSearch.contents !is EntryDirectory)
            throw IllegalArgumentException("The entry is not directory")

        val entriesList = ArrayList<DiskEntry>()
        dirToSearch.contents.entries.forEach {
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
     * Search a entry using path
     * @return Pair of <The file, Parent file>, or null if not found
     */
    fun getFile(disk: VirtualDisk, path: VDPath): EntrySearchResult? {
        val searchHierarchy = ArrayList<DiskEntry>()
        fun getCurrentEntry(): DiskEntry = searchHierarchy.last()
        //var currentDirectory = disk.root

        searchHierarchy.add(disk.entries[0]!!)

        // path of root
        if (path.hierarchy.size == 0) {
            return EntrySearchResult(
                    disk.entries[0]!!,
                    disk.entries[0]!!
            )
        }

        try {
            // search for the file
            path.forEachIndexed { i, nameToSearch ->
                // if we hit the last elem, we won't search more
                if (i <= path.lastIndex) {
                    val currentDirEntries = getDirectoryEntries(disk, getCurrentEntry())

                    var fileFound: DiskEntry? = null
                    for (entry in currentDirEntries) {
                        if (Arrays.equals(entry.filename, nameToSearch)) {
                            fileFound = entry
                            break
                        }
                    }
                    if (fileFound == null) { // file not found
                        throw KotlinNullPointerException()
                    }
                    else { // file found
                        searchHierarchy.add(fileFound)
                    }
                }
            }
        }
        catch (e1: KotlinNullPointerException) {
            return null
        }

        // file found
        return EntrySearchResult(
                searchHierarchy[searchHierarchy.lastIndex],
                searchHierarchy[searchHierarchy.lastIndex - 1]
        )
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
     * Search for the file and returns a instance of normal file.
     */
    fun getAsNormalFile(disk: VirtualDisk, path: VDPath) =
            getFile(disk, path)!!.file.getAsNormalFile(disk)
    /**
     * Fetch the file and returns a instance of normal file.
     */
    fun getAsNormalFile(disk: VirtualDisk, entryIndex: EntryID) =
            disk.entries[entryIndex]!!.getAsNormalFile(disk)
    /**
     * Search for the file and returns a instance of directory.
     */
    fun getAsDirectory(disk: VirtualDisk, path: VDPath) =
            getFile(disk, path)!!.file.getAsDirectory(disk)
    /**
     * Fetch the file and returns a instance of directory.
     */
    fun getAsDirectory(disk: VirtualDisk, entryIndex: EntryID) =
            disk.entries[entryIndex]!!.getAsDirectory(disk)
    /**
     * Deletes file on the disk safely.
     */
    fun deleteFile(disk: VirtualDisk, path: VDPath) {
        disk.checkReadOnly()

        val fileSearchResult = getFile(disk, path)!!

        return deleteFile(disk, fileSearchResult.file.entryID)
    }
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
        val parentDir = disk.entries[parentID]

        fun rollback() {
            if (!disk.entries.contains(targetID)) {
                disk.entries[targetID] = file
            }
            if (!(parentDir!!.contents as EntryDirectory).entries.contains(targetID)) {
                (parentDir.contents as EntryDirectory).entries.add(targetID)
            }
        }

        if (parentDir == null || parentDir.contents !is EntryDirectory) {
            throw FileNotFoundException("No such parent directory")
        }
        // check if directory "parentID" has "targetID" in the first place
        else if (!directoryContains(disk, parentID, targetID)) {
            throw FileNotFoundException("No such file to delete")
        }
        else if (targetID == 0) {
            throw IOException("Cannot delete root file system")
        }
        else if (file.contents is EntryDirectory && file.contents.entries.size > 0) {
            //throw IOException("Cannot delete directory that contains something")
            deleteDirRecurse(disk, targetID)
        }
        else {
            try {
                // delete file record
                disk.entries.remove(targetID)
                // unlist file from parent directly
                (disk.entries[parentID]!!.contents as EntryDirectory).entries.remove(targetID)
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
    fun renameFile(disk: VirtualDisk, path: VDPath, newName: String, charset: Charset) {
        val file = getFile(disk, path)?.file

        if (file != null) {
            file.filename = newName.sanitisePath().toEntryName(DiskEntry.NAME_LENGTH, charset)
        }
        else {
            throw FileNotFoundException()
        }
    }
    /**
     * Changes the name of the entry.
     */
    fun renameFile(disk: VirtualDisk, fileID: EntryID, newName: String, charset: Charset) {
        val file = disk.entries[fileID]

        if (file != null) {
            file.filename = newName.sanitisePath().toEntryName(DiskEntry.NAME_LENGTH, charset)
        }
        else {
            throw FileNotFoundException()
        }
    }
    /**
     * Add file to the specified directory. ParentID of the file will be overwritten.
     */
    fun addFile(disk: VirtualDisk, parentPath: VDPath, file: DiskEntry) {
        disk.checkReadOnly()
        disk.checkCapacity(file.serialisedSize)

        try {
            val parentID = getFile(disk, parentPath)!!.file.entryID
            // add record to the directory
            getAsDirectory(disk, parentPath).entries.add(file.entryID)
            // add entry on the disk
            disk.entries[file.entryID] = file
            file.parentEntryID = parentID
        }
        catch (e: KotlinNullPointerException) {
            throw FileNotFoundException("No such directory")
        }
    }
    /**
     * Add file to the specified directory. ParentID of the file will be overwritten.
     */
    fun addFile(disk: VirtualDisk, directoryID: EntryID, file: DiskEntry) {
        disk.checkReadOnly()
        disk.checkCapacity(file.serialisedSize)

        try {
            // add record to the directory
            getAsDirectory(disk, directoryID).entries.add(file.entryID)
            // add entry on the disk
            disk.entries[file.entryID] = file
            file.parentEntryID = directoryID
        }
        catch (e: KotlinNullPointerException) {
            throw FileNotFoundException("No such directory")
        }
    }
    /**
     * Add subdirectory to the specified directory.
     */
    fun addDir(disk: VirtualDisk, parentPath: VDPath, name: ByteArray) {
        disk.checkReadOnly()
        disk.checkCapacity(EntryDirectory.NEW_ENTRY_SIZE)

        val newID = disk.generateUniqueID()

        try {
            val parentID = getFile(disk, parentPath)!!.file.entryID
            // add record to the directory
            getAsDirectory(disk, parentPath).entries.add(newID)
            // add entry on the disk
            disk.entries[newID] = DiskEntry(
                    newID,
                    parentID,
                    name,
                    currentUnixtime,
                    currentUnixtime,
                    EntryDirectory()
            )
        }
        catch (e: KotlinNullPointerException) {
            throw FileNotFoundException("No such directory")
        }
    }
    /**
     * Add file to the specified directory.
     */
    fun addDir(disk: VirtualDisk, directoryID: EntryID, name: ByteArray) {
        disk.checkReadOnly()
        disk.checkCapacity(EntryDirectory.NEW_ENTRY_SIZE)

        val newID = disk.generateUniqueID()

        try {
            // add record to the directory
            getAsDirectory(disk, directoryID).entries.add(newID)
            // add entry on the disk
            disk.entries[newID] = DiskEntry(
                    newID,
                    directoryID,
                    name,
                    currentUnixtime,
                    currentUnixtime,
                    EntryDirectory()
            )
        }
        catch (e: KotlinNullPointerException) {
            throw FileNotFoundException("No such directory")
        }
    }

    fun deleteDirRecurse(disk: VirtualDisk, directoryID: EntryID) {
        val entriesToDelete = ArrayList<EntryID>()

        fun recurse1(entry: DiskEntry?) {
            // return conditions
            if (entry == null) return
            if (entry.contents !is EntryDirectory) {
                entriesToDelete.add(entry.entryID)
                return
            }
            // recurse
            else {
                entry.contents.entries.forEach {
                    entriesToDelete.add(entry.entryID)
                    recurse1(disk.entries[it])
                }
            }
        }

        val entry = disk.entries[directoryID]
        if (entry != null && entry.contents is EntryDirectory) {
            entry.contents.entries.forEach {
                entriesToDelete.add(directoryID)
                recurse1(disk.entries[it])
            }

            // delete entries
            entriesToDelete.forEach { disk.entries.remove(it) }
            // GC
            gcDumpAll(disk)
            System.gc()
        }
        else if (entry == null) {
            throw FileNotFoundException("No such directory")
        }
        else {
            throw IOException("The file is not a directory")
        }
    }

    /**
     * Imports external file and returns corresponding DiskEntry.
     */
    fun importFile(file: File, id: EntryID): DiskEntry {
        if (file.isDirectory) {
            throw IOException("The file is a directory")
        }

        return DiskEntry(
                entryID = id,
                parentEntryID = 0, // placeholder
                filename = file.name.toByteArray(),
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
        outfile.writeBytes64(entryFile.bytes)
    }

    /**
     * Check for name collision in specified directory.
     */
    fun nameExists(disk: VirtualDisk, name: String, directoryID: EntryID, charset: Charset): Boolean {
        return nameExists(disk, name.toEntryName(256, charset), directoryID)
    }
    /**
     * Check for name collision in specified directory.
     */
    fun nameExists(disk: VirtualDisk, name: ByteArray, directoryID: EntryID): Boolean {
        val directoryContents = getDirectoryEntries(disk, directoryID)
        directoryContents.forEach {
            if (Arrays.equals(name, it.filename))
                return true
        }
        return false
    }

    /**
     * Move file from to there, overwrite
     */
    fun moveFile(disk1: VirtualDisk, fromPath: VDPath, disk2: VirtualDisk, toPath: VDPath) {
        val file = getFile(disk1, fromPath)

        if (file != null) {
            if (file.file.contents is EntryDirectory) {
                throw IOException("Cannot move directory")
            }

            disk2.checkCapacity(file.file.contents.getSizeEntry())

            try {
                deleteFile(disk2, toPath)
            }
            catch (e: KotlinNullPointerException) { /* Nothing to delete beforehand */ }

            deleteFile(disk1, fromPath) // any uncaught no_from_file will be caught here
            try {
                addFile(disk2, toPath.getParent(), file.file)
            }
            catch (e: FileNotFoundException) {
                // roll back delete on disk1
                addFile(disk1, file.parent.entryID, file.file)
                throw FileNotFoundException("No such destination")
            }
        }
        else {
            throw FileNotFoundException("No such file to move")
        }
    }


    /**
     * Creates new disk with given name and capacity
     */
    fun createNewDisk(diskSize: Long, diskName: String, charset: Charset): VirtualDisk {
        val newdisk = VirtualDisk(diskSize, diskName.toEntryName(VirtualDisk.NAME_LENGTH, charset))
        val rootDir = DiskEntry(
                entryID = 0,
                parentEntryID = 0,
                filename = DiskEntry.ROOTNAME.toByteArray(charset),
                creationDate = currentUnixtime,
                modificationDate = currentUnixtime,
                contents = EntryDirectory()
        )

        newdisk.entries[0] = rootDir

        return newdisk
    }
    /**
     * Creates new zero-filled file with given name and size
     */
    fun createNewBlankFile(disk: VirtualDisk, directoryID: EntryID, fileSize: Long, filename: String, charset: Charset) {
        disk.checkReadOnly()
        disk.checkCapacity(fileSize + DiskEntry.HEADER_SIZE + 4)

        addFile(disk, directoryID, DiskEntry(
                disk.generateUniqueID(),
                directoryID,
                filename.toEntryName(DiskEntry.NAME_LENGTH, charset = charset),
                currentUnixtime,
                currentUnixtime,
                EntryFile(fileSize)
        ))
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
        this.forEach { byte -> i += byte.toUint().shl(24 - c * 8); c += 1 }
        return i
    }
    fun ByteArray64.toInt48Big(): Long {
        if (this.size != 6L)
            throw OperationNotSupportedException("ByteArray is not Long")

        var i = 0L
        var c = 0
        this.forEach { byte -> i += byte.toUint().shl(40 - c * 8); c += 1 }
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
    data class EntrySearchResult(val file: DiskEntry, val parent: DiskEntry)

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
            return dir.contents.entries.contains(targetID)
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

    fun gcSearchPhantomBaby(disk: VirtualDisk): List<Pair<EntryID, EntryID>> {
        // Pair<DirectoryID, ID of phantom in the directory>
        val phantoms = ArrayList<Pair<EntryID, EntryID>>()
        disk.entries.filter { it.value.contents is EntryDirectory }.values.forEach { directory ->
            (directory.contents as EntryDirectory).entries.forEach { dirEntryID ->
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
            throw InternalError("Aw, snap! Here's what it says:\n$e")
        }
    }

    fun gcDumpAll(disk: VirtualDisk) {
        try {
            gcSearchPhantomBaby(disk).forEach {
                getAsDirectory(disk, it.first).entries.remove(it.second)
            }
            gcSearchOrphan(disk).forEach {
                disk.entries.remove(it)
            }
        }
        catch (e: Exception) {
            throw InternalError("Aw, snap! Here's what it says:\n$e")
        }
    }
}

fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
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

fun ArrayList<Byte>.toByteArray64(): ByteArray64 {
    val array = ByteArray64(this.size.toLong())
    this.forEachIndexed { index, byte ->
        array[index.toLong()] = byte
    }
    return array
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