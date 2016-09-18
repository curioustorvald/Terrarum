package net.torvald.terrarum.virtualcomputer.luaapi

import li.cil.repack.org.luaj.vm2.*
import li.cil.repack.org.luaj.vm2.lib.OneArgFunction
import li.cil.repack.org.luaj.vm2.lib.TwoArgFunction
import li.cil.repack.org.luaj.vm2.lib.ZeroArgFunction
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import net.torvald.terrarum.virtualcomputer.luaapi.Term.Companion.checkIBM437
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * computer directory:
 * .../computers/
 *      media/hda/  ->  .../computers/<uuid for the hda>/
 * 
 * Created by minjaesong on 16-09-17.
 */
internal class Filesystem(globals: Globals, computer: BaseTerrarumComputer) {

    init {
        // load things. WARNING: THIS IS MANUAL!
        globals["fs"] = LuaValue.tableOf()
        globals["fs"]["list"] = ListFiles(computer)
        globals["fs"]["exists"] = FileExists(computer)
        globals["fs"]["isDir"] = IsDirectory(computer)
        globals["fs"]["isFile"] = IsFile(computer)
        globals["fs"]["isReadOnly"] = IsReadOnly(computer)
        globals["fs"]["getSize"] = GetSize(computer)
        globals["fs"]["listFiles"] = ListFiles(computer)
        globals["fs"]["mkdir"] = Mkdir(computer)
        globals["fs"]["mv"] = Mv(computer)
        globals["fs"]["cp"] = Cp(computer)
        globals["fs"]["rm"] = Rm(computer)
        globals["fs"]["concat"] = ConcatPath(computer)
        globals["fs"]["open"] = OpenFile(computer)
        globals["fs"]["parent"] = GetParentDir(computer)
        globals["__haltsystemexplicit__"] = HaltComputer(computer)
        // fs.run defined in ROMLIB
    }

    companion object {
        val isCaseInsensitive: Boolean
            get() {
                // TODO add: force case insensitive in config
                try {
                    val uuid = UUID.randomUUID().toString()
                    val lowerCase = File(Terrarum.currentSaveDir, uuid + "oc_rox")
                    val upperCase = File(Terrarum.currentSaveDir, uuid + "OC_ROX")
                    // This should NEVER happen but could also lead to VERY weird bugs, so we
                    // make sure the files don't exist.
                    lowerCase.exists() && lowerCase.delete()
                    upperCase.exists() && upperCase.delete()
                    lowerCase.createNewFile()
                    val insensitive = upperCase.exists()
                    lowerCase.delete()
                    return insensitive
                }
                catch (e: IOException) {
                    println("[Filesystem] Couldn't determine if file system is case sensitive, falling back to insensitive.")
                    return true
                }
            }

        // Worst-case: we're on Windows or using a FAT32 partition mounted in *nix.
        // Note: we allow / as the path separator and expect all \s to be converted
        // accordingly before the path is passed to the file system.
        private val invalidChars = Regex("""[\\:*?"<>|]""") // original OC uses Set(); we use regex

        fun isValidFilename(name: String) = !name.contains(invalidChars)

        fun validatePath(path: String): String {
            if (!isValidFilename(path)) {
                throw IOException("path contains invalid characters")
            }
            return path
        }

        /** actual directory: <appdata>/Saves/<savename>/computers/<drivename>/
         * directs media/ directory to /<uuid> directory
         */
        fun BaseTerrarumComputer.getRealPath(luapath: LuaValue): String {
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
            var path = luapath.checkjstring()
            if (path.startsWith('/')) path = path.substring(1)
            // replace '\' with '/'
            path.replace('\\', '/')
            
            if (path.startsWith("media/")) {
                val device = path.substring(6, 9)
                val subPath = path.substring(9)
                return computerDir + this.computerValue.getAsString("device") + subPath
            }
            else {
                return computerDir + this.computerValue.getAsString("boot") + "/" + path
            }
        }

        fun combinePath(base: String, local: String): String {
            return "$base$local".replace("//", "/").replace("\\\\", "\\")
        }
    }

    /**
     * @param cname == UUID of the drive
     *
     * actual directory: <appdata>/Saves/<savename>/computers/<drivename>/
     */
    class ListFiles(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            val table = LuaTable()
            val file = File(computer.getRealPath(path))
            file.list().forEachIndexed { i, s -> table.insert(i, LuaValue.valueOf(s)) }
            return table
        }
    }

    class FileExists(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            return LuaValue.valueOf(File(computer.getRealPath(path)).exists())
        }
    }

    class IsDirectory(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            return LuaValue.valueOf(File(computer.getRealPath(path)).isDirectory)
        }
    }

    class IsFile(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            return LuaValue.valueOf(File(computer.getRealPath(path)).isFile)
        }
    }

    class IsReadOnly(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            return LuaValue.valueOf(!File(computer.getRealPath(path)).canWrite())
        }
    }

    /** we have 4GB file size limit */
    class GetSize(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            return LuaValue.valueOf(File(computer.getRealPath(path)).length().toInt())
        }
    }

    // TODO class GetFreeSpace

    /**
     * difference with ComputerCraft: it returns boolean, true on successful.
     */
    class Mkdir(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            return LuaValue.valueOf(File(computer.getRealPath(path)).mkdir())
        }
    }

    /**
     * moves a directory, overwrites the target
     */
    class Mv(val computer: BaseTerrarumComputer): TwoArgFunction() {
        override fun call(from: LuaValue, to: LuaValue): LuaValue {
            val fromFile = File(computer.getRealPath(from))
            fromFile.copyRecursively(
                    File(computer.getRealPath(to)), overwrite = true
            )
            fromFile.deleteRecursively()
            return LuaValue.NONE
        }
    }

    /**
     * copies a directory, overwrites the target
     * difference with ComputerCraft: it returns boolean, true on successful.
     */
    class Cp(val computer: BaseTerrarumComputer): TwoArgFunction() {
        override fun call(from: LuaValue, to: LuaValue): LuaValue {
            return LuaValue.valueOf(
                    File(computer.getRealPath(from)).copyRecursively(
                            File(computer.getRealPath(to)), overwrite = true
                    )
            )
        }
    }

    /**
     * difference with ComputerCraft: it returns boolean, true on successful.
     */
    class Rm(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            return LuaValue.valueOf(
                    File(computer.getRealPath(path)).deleteRecursively()
            )
        }
    }

    class ConcatPath(val computer: BaseTerrarumComputer): TwoArgFunction() {
        override fun call(base: LuaValue, local: LuaValue): LuaValue {
            val combinedPath = combinePath(base.checkjstring(), local.checkjstring())
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
    class OpenFile(val computer: BaseTerrarumComputer): TwoArgFunction() {
        override fun call(path: LuaValue, mode: LuaValue): LuaValue {
            val mode = mode.checkjstring().toLowerCase()
            val luaClass = LuaTable()
            val file = File(computer.getRealPath(path))

            if (mode.contains("[aw]") && !file.canWrite())
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
                        throw LuaError("$path: No such file.")
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
                        throw LuaError("$path: No such file.")
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
                        throw LuaError("$path: Is a directory.")
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
                        throw LuaError("$path: Is a directory.")
                    }
                }
            }

            return luaClass
        }
    }

    class GetParentDir(val computer: BaseTerrarumComputer): OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            var pathSB = StringBuilder(path.checkjstring())

            // backward travel, drop chars until '/' has encountered
            while (!pathSB.endsWith('/') && !pathSB.endsWith('\\'))
                pathSB.deleteCharAt(pathSB.lastIndex - 1)

            // drop trailing '/'
            if (pathSB.endsWith('/') || pathSB.endsWith('\\'))
                pathSB.deleteCharAt(pathSB.lastIndex - 1)

            return LuaValue.valueOf(pathSB.toString())
        }
    }

    class HaltComputer(val computer: BaseTerrarumComputer): ZeroArgFunction() {
        override fun call(): LuaValue {
            computer.isHalted = true
            computer.luaJ_globals.load("""print("system halted.")""", "=").call()
            return LuaValue.NONE
        }
    }



    //////////////////////////////
    // OpenFile implementations //
    //////////////////////////////

    private class FileClassClose(val fo: Any): ZeroArgFunction() {
        override fun call(): LuaValue {
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

    private class FileClassWriteByte(val fos: FileOutputStream): OneArgFunction() {
        override fun call(byte: LuaValue): LuaValue {
            fos.write(byte.checkint())

            return LuaValue.NONE
        }
    }

    private class FileClassWriteBytes(val fos: FileOutputStream): OneArgFunction() {
        override fun call(byteString: LuaValue): LuaValue {
            val byteString = byteString.checkIBM437()
            val bytearr = ByteArray(byteString.length, { byteString[it].toByte() })
            fos.write(bytearr)

            return LuaValue.NONE
        }
    }

    private class FileClassPrintText(val fw: FileWriter): OneArgFunction() {
        override fun call(string: LuaValue): LuaValue {
            val text = string.checkIBM437()
            fw.write(text)
            return LuaValue.NONE
        }
    }

    private class FileClassPrintlnText(val fw: FileWriter): OneArgFunction() {
        override fun call(string: LuaValue): LuaValue {
            val text = string.checkIBM437() + "\n"
            fw.write(text)
            return LuaValue.NONE
        }
    }

    private class FileClassFlush(val fo: Any): ZeroArgFunction() {
        override fun call(): LuaValue {
            if (fo is FileOutputStream)
                fo.flush()
            else if (fo is FileWriter)
                fo.flush()
            else
                throw IllegalArgumentException("Unacceptable file output: must be either OutputStream or Writer.")

            return LuaValue.NONE
        }
    }

    private class FileClassReadByte(val fis: FileInputStream): ZeroArgFunction() {
        override fun call(): LuaValue {
            val readByte = fis.read()
            return if (readByte == -1) LuaValue.NIL else LuaValue.valueOf(readByte)
        }
    }

    private class FileClassReadAllBytes(val path: Path): ZeroArgFunction() {
        override fun call(): LuaValue {
            val byteArr = Files.readAllBytes(path)
            val s: String = java.lang.String(byteArr, "IBM437").toString()
            return LuaValue.valueOf(s)
        }
    }

    private class FileClassReadAll(val path: Path): ZeroArgFunction() {
        override fun call(): LuaValue {
            return FileClassReadAllBytes(path).call()
        }
    }

    private class FileClassReadLine(val fr: FileReader): ZeroArgFunction() {
        val scanner = Scanner(fr.readText()) // no closing; keep the scanner status persistent

        override fun call(): LuaValue {
            return if (scanner.hasNextLine()) LuaValue.valueOf(scanner.nextLine())
                   else LuaValue.NIL
        }
    }
}