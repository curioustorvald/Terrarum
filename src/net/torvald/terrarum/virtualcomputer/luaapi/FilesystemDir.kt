package net.torvald.terrarum.virtualcomputer.luaapi

import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
import net.torvald.terrarum.virtualcomputer.luaapi.Term.Companion.checkIBM437
import java.io.*
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * computer directory:
 * .../computers/
 *      media/hda/  ->  .../computers/<uuid for the hda>/
 * 
 * Created by minjaesong on 16-09-17.
 *
 *
 * NOTES:
 *      Don't convert '\' to '/'! Rev-slash is used for escape character in sh, and we're sh-compatible!
 *      Use .absoluteFile whenever possible; there's fuckin oddity! (http://bugs.java.com/bugdatabase/view_bug.do;:YfiG?bug_id=4483097)
 */
@Deprecated("Fuck permission and shit, we go virtual. Use FilesystemTar")
internal class FilesystemDir(globals: Globals, computer: TerrarumComputer) {

    init {
        // load things. WARNING: THIS IS MANUAL!
        globals["fs"] = LuaValue.tableOf()
        globals["fs"]["list"] = ListFiles(computer) // CC compliant
        globals["fs"]["exists"] = FileExists(computer) // CC/OC compliant
        globals["fs"]["isDir"] = IsDirectory(computer) // CC compliant
        globals["fs"]["isFile"] = IsFile(computer)
        globals["fs"]["isReadOnly"] = IsReadOnly(computer) // CC compliant
        globals["fs"]["getSize"] = GetSize(computer) // CC compliant
        globals["fs"]["mkdir"] = Mkdir(computer)
        globals["fs"]["mv"] = Mv(computer)
        globals["fs"]["cp"] = Cp(computer)
        globals["fs"]["rm"] = Rm(computer)
        globals["fs"]["concat"] = ConcatPath(computer) // OC compliant
        globals["fs"]["open"] = OpenFile(computer) //CC compliant
        globals["fs"]["parent"] = GetParentDir(computer)
        // fs.dofile defined in BOOT
        // fs.fetchText defined in ROMLIB
    }

    companion object {
        fun ensurePathSanity(path: LuaValue) {
            if (path.checkIBM437().contains(Regex("""\.\.""")))
                throw LuaError("'..' on path is not supported.")
            if (!isValidFilename(path.checkIBM437()))
                throw IOException("path contains invalid characters")
        }

        var isCaseInsensitive: Boolean

        init {
            try {
                val uuid = UUID.randomUUID().toString()
                val lowerCase = File(Terrarum.currentSaveDir, uuid + "oc_rox")
                val upperCase = File(Terrarum.currentSaveDir, uuid + "OC_ROX")
                // This should NEVER happen but could also lead to VERY weird bugs, so we
                // make sure the files don't exist.
                if (lowerCase.exists()) lowerCase.delete()
                if (upperCase.exists()) upperCase.delete()

                lowerCase.createNewFile()

                val insensitive = upperCase.exists()
                lowerCase.delete()

                isCaseInsensitive = insensitive

                println("[Filesystem] Case insensitivity: $isCaseInsensitive")
            }
            catch (e: IOException) {
                System.err.println("[Filesystem] Couldn't determine if the file system is case sensitive, falling back to insensitive.")
                e.printStackTrace(System.out)
                isCaseInsensitive = true
            }
        }

        // Worst-case: we're on Windows or using a FAT32 partition mounted in *nix.
        // Note: we allow / as the path separator and expect all \s to be converted
        // accordingly before the path is passed to the file system.
        private val invalidChars = Regex("""[<>:"|?*\u0000-\u001F]""") // original OC uses Set(); we use regex

        fun isValidFilename(name: String) = !name.contains(invalidChars)

        fun String.validatePath() : String {
            if (!isValidFilename(this)) {
                throw IOException("path contains invalid characters")
            }
            return this
        }

        /** actual directory: <appdata>/Saves/<savename>/computers/<drivename>/
         * directs media/ directory to /<uuid> directory
         */
        fun TerrarumComputer.getRealPath(luapath: LuaValue) : String {
            // direct mounted paths to real path
            val computerDir = Terrarum.currentSaveDir.absolutePath + "/computers/"
            /* if not begins with "(/?)media/", direct to boot
             * else, to corresponding drives
             * 
             * List of device names (these are auto-mounted. why? primitivism :p):
             * = hda - hdd: hard disks
             * = fd1 - fd4: floppy drives
             * = sda: whatever external drives, usually a CD
             * = boot: current boot device
             */
            
            // remove first '/' in path
            var path = luapath.checkIBM437().validatePath()
            if (path.startsWith('/')) path = path.substring(1)

            val finalPath: String

            if (path.startsWith("media/")) {
                val device = path.substring(6, 9)
                val subPath = path.substring(9)
                finalPath = computerDir + this.computerValue.getAsString("device") + subPath
            }
            else {
                finalPath = computerDir + this.computerValue.getAsString("boot") + "/" + path
            }

            // remove trailing slash
            return if (finalPath.endsWith("\\") || finalPath.endsWith("/"))
                finalPath.substring(0, finalPath.length - 1)
            else
                finalPath
        }

        fun combinePath(base: String, local: String) : String {
            return "$base$local".replace("//", "/")
        }
    }

    /**
     * @param cname == UUID of the drive
     *
     * actual directory: <appdata>/Saves/<savename>/computers/<drivename>/
     */
    class ListFiles(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            println("ListFiles: got path ${path.checkIBM437()}")

            val table = LuaTable()
            val file = File(computer.getRealPath(path)).absoluteFile
            try {
                file.list().forEachIndexed { i, s -> table.insert(i, LuaValue.valueOf(s)) }
            }
            catch (e: NullPointerException) {}
            return table
        }
    }

    /** Don't use this. Use isFile */
    class FileExists(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            return LuaValue.valueOf(Files.exists(Paths.get(computer.getRealPath(path)).toAbsolutePath()))
        }
    }

    class IsDirectory(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            val isDir = Files.isDirectory(Paths.get(computer.getRealPath(path)).toAbsolutePath())
            val exists = Files.exists(Paths.get(computer.getRealPath(path)).toAbsolutePath())

            return LuaValue.valueOf(isDir || exists)
        }
    }

    class IsFile(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            // check if the path is file by checking:
            // 1. isfile
            // 2. canwrite
            // 3. length
            // Why? Our Java simply wants to fuck you.

            val path = Paths.get(computer.getRealPath(path)).toAbsolutePath()
            var result = false
            result = Files.isRegularFile(path)

            if (!result) result = Files.isWritable(path)

            if (!result)
                try { result = Files.size(path) > 0 }
                catch (e: NoSuchFileException) { result = false }

            return LuaValue.valueOf(result)
        }
    }

    class IsReadOnly(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            return LuaValue.valueOf(!Files.isWritable(Paths.get(computer.getRealPath(path)).toAbsolutePath()))
        }
    }

    /** we have 4GB file size limit */
    class GetSize(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            return LuaValue.valueOf(Files.size(Paths.get(computer.getRealPath(path)).toAbsolutePath()).toInt())
        }
    }

    // TODO class GetFreeSpace

    /**
     * difference with ComputerCraft: it returns boolean, true on successful.
     */
    class Mkdir(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            return LuaValue.valueOf(File(computer.getRealPath(path)).absoluteFile.mkdir())
        }
    }

    /**
     * moves a directory, overwrites the target
     */
    class Mv(val computer: TerrarumComputer) : TwoArgFunction() {
        override fun call(from: LuaValue, to: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(from)
            FilesystemDir.ensurePathSanity(to)

            val fromFile = File(computer.getRealPath(from)).absoluteFile
            var success = fromFile.copyRecursively(
                    File(computer.getRealPath(to)).absoluteFile, overwrite = true
            )
            if (success) success = fromFile.deleteRecursively()
            else return LuaValue.valueOf(false)
            return LuaValue.valueOf(success)
        }
    }

    /**
     * copies a directory, overwrites the target
     * difference with ComputerCraft: it returns boolean, true on successful.
     */
    class Cp(val computer: TerrarumComputer) : TwoArgFunction() {
        override fun call(from: LuaValue, to: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(from)
            FilesystemDir.ensurePathSanity(to)

            return LuaValue.valueOf(
                    File(computer.getRealPath(from)).absoluteFile.copyRecursively(
                            File(computer.getRealPath(to)).absoluteFile, overwrite = true
                    )
            )
        }
    }

    /**
     * difference with ComputerCraft: it returns boolean, true on successful.
     */
    class Rm(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            return LuaValue.valueOf(
                    File(computer.getRealPath(path)).absoluteFile.deleteRecursively()
            )
        }
    }

    class ConcatPath(val computer: TerrarumComputer) : TwoArgFunction() {
        override fun call(base: LuaValue, local: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(base)
            FilesystemDir.ensurePathSanity(local)

            val combinedPath = combinePath(base.checkIBM437().validatePath(), local.checkIBM437().validatePath())
            return LuaValue.valueOf(combinedPath)
        }
    }

    /**
     * @param mode: r, rb, w, wb, a, ab
     *
     * Difference: TEXT MODE assumes CP437 instead of UTF-8!
     *
     * When you have opened a file you must always close the file handle, or else data may not be saved.
     *
     * FILE class in CC:
     * (when you look thru them using file = fs.open("./test", "w")
     *
     * file = {
     *      close = function()
     *      -- write mode
     *      write = function(string)
     *      flush = function() -- write, keep the handle
     *      writeLine = function(string) -- text mode
     *      -- read mode
     *      readLine = function() -- text mode
     *      readAll = function()
     *      -- binary read mode
     *      read = function() -- read single byte. return: number or nil
     *      -- binary write mode
     *      write = function(byte)
     *      writeBytes = function(string as bytearray)
     * }
     */
    class OpenFile(val computer: TerrarumComputer) : TwoArgFunction() {
        override fun call(path: LuaValue, mode: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            

            val mode = mode.checkIBM437().toLowerCase()
            val luaClass = LuaTable()
            val file = File(computer.getRealPath(path)).absoluteFile

            if (mode.contains(Regex("""[aw]""")) && !file.canWrite())
                throw LuaError("Cannot open file for " +
                               "${if (mode.startsWith('w')) "read" else "append"} mode" +
                               ": is readonly.")




            when (mode) {
                "r"  -> {
                    try {
                        val fr = FileReader(file)
                        luaClass["close"] = FileClassClose(fr)
                        luaClass["readLine"] = FileClassReadLine(fr)
                        luaClass["readAll"] = FileClassReadAll(file.toPath())
                    }
                    catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        throw LuaError(
                                if (e.message != null && e.message!!.contains(Regex("""[Aa]ccess (is )?denied""")))
                                    "$path: access denied."
                                else
                                    "$path: no such file."
                        )
                    }
                }
                "rb" -> {
                    try {
                        val fis = FileInputStream(file)
                        luaClass["close"] = FileClassClose(fis)
                        luaClass["read"] = FileClassReadByte(fis)
                        luaClass["readAll"] = FileClassReadAll(file.toPath())
                    }
                    catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        throw LuaError("$path: no such file.")
                    }
                }
                "w", "a"  -> {
                    try {
                        val fw = FileWriter(file, (mode.startsWith('a')))
                        luaClass["close"] = FileClassClose(fw)
                        luaClass["write"] = FileClassPrintText(fw)
                        luaClass["writeLine"] = FileClassPrintlnText(fw)
                        luaClass["flush"] = FileClassFlush(fw)
                    }
                    catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        throw LuaError("$path: is a directory.")
                    }
                }
                "wb", "ab" -> {
                    try {
                        val fos = FileOutputStream(file, (mode.startsWith('a')))
                        luaClass["close"] = FileClassClose(fos)
                        luaClass["write"] = FileClassWriteByte(fos)
                        luaClass["writeBytes"] = FileClassWriteBytes(fos)
                        luaClass["flush"] = FileClassFlush(fos)
                    }
                    catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        throw LuaError("$path: is a directory.")
                    }
                }
            }

            return luaClass
        }
    }

    class GetParentDir(val computer: TerrarumComputer) : OneArgFunction() {
        override fun call(path: LuaValue) : LuaValue {
            FilesystemDir.ensurePathSanity(path)

            var pathSB = StringBuilder(path.checkIBM437())

            // backward travel, drop chars until '/' has encountered
            while (!pathSB.endsWith('/'))
                pathSB.deleteCharAt(pathSB.lastIndex - 1)

            // drop trailing '/'
            if (pathSB.endsWith('/'))
                pathSB.deleteCharAt(pathSB.lastIndex - 1)

            return LuaValue.valueOf(pathSB.toString())
        }
    }


    //////////////////////////////
    // OpenFile implementations //
    //////////////////////////////

    private class FileClassClose(val fo: Any) : ZeroArgFunction() {
        override fun call() : LuaValue {
            if (fo is FileOutputStream)
                fo.close()
            else if (fo is FileWriter)
                fo.close()
            else if (fo is FileReader)
                fo.close()
            else if (fo is FileInputStream)
                fo.close()
            else
                throw IllegalArgumentException("Unacceptable file output: must be either Input/OutputStream or Reader/Writer.")

            return LuaValue.NONE
        }
    }

    private class FileClassWriteByte(val fos: FileOutputStream) : OneArgFunction() {
        override fun call(byte: LuaValue) : LuaValue {
            fos.write(byte.checkint())

            return LuaValue.NONE
        }
    }

    private class FileClassWriteBytes(val fos: FileOutputStream) : OneArgFunction() {
        override fun call(byteString: LuaValue) : LuaValue {
            val byteString = byteString.checkIBM437()
            val bytearr = ByteArray(byteString.length, { byteString[it].toByte() })
            fos.write(bytearr)

            return LuaValue.NONE
        }
    }

    private class FileClassPrintText(val fw: FileWriter) : OneArgFunction() {
        override fun call(string: LuaValue) : LuaValue {
            val text = string.checkIBM437()
            fw.write(text)
            return LuaValue.NONE
        }
    }

    private class FileClassPrintlnText(val fw: FileWriter) : OneArgFunction() {
        override fun call(string: LuaValue) : LuaValue {
            val text = string.checkIBM437() + "\n"
            fw.write(text)
            return LuaValue.NONE
        }
    }

    private class FileClassFlush(val fo: Any) : ZeroArgFunction() {
        override fun call() : LuaValue {
            if (fo is FileOutputStream)
                fo.flush()
            else if (fo is FileWriter)
                fo.flush()
            else
                throw IllegalArgumentException("Unacceptable file output: must be either OutputStream or Writer.")

            return LuaValue.NONE
        }
    }

    private class FileClassReadByte(val fis: FileInputStream) : ZeroArgFunction() {
        override fun call() : LuaValue {
            val readByte = fis.read()
            return if (readByte == -1) LuaValue.NIL else LuaValue.valueOf(readByte)
        }
    }

    private class FileClassReadAllBytes(val path: Path) : ZeroArgFunction() {
        override fun call() : LuaValue {
            val byteArr = Files.readAllBytes(path)
            val s: String = java.lang.String(byteArr, "IBM437").toString()
            return LuaValue.valueOf(s)
        }
    }

    private class FileClassReadAll(val path: Path) : ZeroArgFunction() {
        override fun call() : LuaValue {
            return FileClassReadAllBytes(path).call()
        }
    }

    /** returns NO line separator! */
    private class FileClassReadLine(val fr: FileReader) : ZeroArgFunction() {
        val scanner = Scanner(fr.readText()) // no closing; keep the scanner status persistent

        override fun call() : LuaValue {
            return if (scanner.hasNextLine()) LuaValue.valueOf(scanner.nextLine())
                   else LuaValue.NIL
        }
    }
}