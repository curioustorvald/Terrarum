package net.torvald.terrarum.debuggerapp

import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.savegame.EntryFile
import net.torvald.terrarum.savegame.VDUtil
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.savegame.diskIDtoReadableFilename
import net.torvald.terrarum.serialise.Common
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Level
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

private val ESC = 27.toChar()

/**
 * Created by minjaesong on 2021-09-02.
 */
class SavegameCracker(
        val args: Array<String>,
        val readFun: () -> String = { Scanner(System.`in`).nextLine() },
        val stdout: PrintStream = System.out,
        val stderr: PrintStream = System.out,
        val charset: Charset = Common.CHARSET,
        val colourCodes: List<String> = listOf(
                "$ESC[m",
                "$ESC[31m",
                "$ESC[32m",
                "$ESC[35m",
                "$ESC[36m",
                "$ESC[34m"
        )
) {

    private var file: File? = null
    private var disk: VirtualDisk? = null

    private fun println(vararg o: Any?) = stdout.println(o.map { it.toString() }.joinToString("\t"))
    private fun print(vararg o: Any?) = stdout.print(o.map { it.toString() }.joinToString("\t"))
    private fun printerrln(vararg o: Any?) = stderr.println(colourCodes[1] + o.map { it.toString() }.joinToString("\t") + colourCodes[0])
    private fun printerr(vararg o: Any?) = stderr.print(colourCodes[1] + o.map { it.toString() }.joinToString("\t") + colourCodes[0])

    private val motd = """Terrarum Savegame Cracker Interactive Mode
        |${TerrarumAppConfiguration.COPYRIGHT_DATE_NAME}, ${TerrarumAppConfiguration.COPYRIGHT_LICENSE_ENGLISH}
        |Using charset ${charset.displayName()}
        |
    """.trimMargin()

    private var exit = false

    private val ccConst = colourCodes[5]
    private val ccNoun = colourCodes[2] // emph for primary objects (e.g. disk name, file name)
    private val ccNoun2 = colourCodes[3] // emph for secondary objects
    private val ccVerb = colourCodes[4] // emph for primary verbs (e.g. "deleted", "overwritten")
    private val cc0 = colourCodes[0]

    private val prompt: String
        get() = "$ccConst${disk?.getDiskName(charset) ?: ""}$cc0% "

    private val cmds: HashMap<String, KFunction<*>> = HashMap()
    init {
        SavegameCracker::class.declaredFunctions
            .filter { it.findAnnotation<Command>() != null }
//                .forEach { it.isAccessible = true; cmds[it.name] = it }
            .forEach { cmds[it.name] = it }
    }

    operator fun invoke() {
        println(motd)
        args.getOrNull(1).let {
            if (it != null) {
                load(listOf("load", args[1]))
            }
            else {
                println("Disk not loaded; load the disk by running 'load <path-to-file>'")
            }
        }

        while (!exit) runInterpreter()

        println("${ccNoun}LX4 ${ccVerb}cya!$cc0")
    }

    private fun runInterpreter() {
        print(prompt)
        val line = readFun().trim()
        val args = tokenise(line)

//        println(args.mapIndexed { index, s -> if (index == 0) "$ccNoun$s$cc0" else "$ccVerb$s$cc0" }.joinToString(" "))
        if (args[0].isNotBlank()) {
            cmds[args[0]].let {
                if (it == null)
                    printerrln("${args[0]}: command not found")
                else {
                    try {
                        val annot = it.findAnnotation<Command>()!!
                        // check arguments
                        val synopsis = annot.synopsis.split(' ').filter { it.isNotBlank() }
                        // print out synopsis
                        if (synopsis.size + 1 != args.size) {
                            print("${cc0}Synopsis: $ccNoun${args[0]} ")
                            synopsis.forEach { print("$ccNoun2<$it> ") }
                            println(cc0)
                        }
                        else
                            it.call(this, args)
                    }
                    catch (e: Throwable) {
                        val error = (e.cause ?: e) as java.lang.Throwable
                        printerrln("Error -- ${error}")
                        error.printStackTrace(stderr)
                        printerrln("Error -- ${error}")
                    }
                }
            }
        }
    }

    private fun tokenise(line: String): List<String> {
        val tokens = ArrayList<String>()
        val sb = StringBuilder()
        var mode = 0 // 0: literal, 34: quote (""), 39: quote('')

        fun sendout() {
            tokens.add(sb.toString().trim())
            sb.clear()
        }

        line.forEachIndexed { index, c ->
            if (mode == 0) {
                if (c == '"') {
                    sendout()
                    mode = 34
                }
                else if (c == '\'') {
                    sendout()
                    mode = 39
                }
                else
                    sb.append(c)
            }
            else if (mode == 34) {
                if (c == '"') {
                    sendout()
                    mode = 0
                }
                else
                    sb.append(c)
            }
            else if (mode == 39) {
                if (c == '\'') {
                    sendout()
                    mode = 0
                }
                else
                    sb.append(c)
            }
        }

        if (sb.isNotEmpty()) sendout()

        return tokens
    }

    private fun letdisk(action: (VirtualDisk) -> Any?): Any? {
        if (disk == null) printerrln("Disk not loaded!")
        else return action(disk!!)
        return null
    }

    private fun String.padEnd(len: Int, padfun: (Int) -> Char): String {
        val sb = StringBuilder()
        for (i in 0 until len - this.length)
            sb.append(padfun(i))
        return this + sb.toString()
    }

    @Command("Loads a disk archive", "path-to-file")
    fun load(args: List<String>) {
        file = File(args[1])
        disk = VDUtil.readDiskArchive(file!!, Level.INFO) { printerrln("# Warning: $it") }
        file!!.copyTo(File(file!!.absolutePath + ".bak"), true)
    }

    @Command("Lists contents of the disk")
    fun ls(args: List<String>) {
        letdisk {
            it.entries.toSortedMap().forEach { (i, entry) ->
                if (i != 0L) println(
                    ccNoun + i.toString(10).padStart(11, ' ') + " " +
                    ccNoun2 + (diskIDtoReadableFilename(entry.entryID) + cc0).padEnd(40) { if (it == 0) ' ' else '.' }  +
                    ccConst + " " + entry.contents.getSizePure() + " bytes"
                )
            }
            val entryCount = it.entries.size - 1
            println("${cc0}$entryCount entries, total ${it.usedBytes} bytes")
        }
    }

    @Command("Prints out available commands and their usage")
    fun help(args: List<String>) {
        cmds.forEach { name, it ->
            println("$ccNoun${name.padStart(8)}$cc0 - ${it.findAnnotation<Command>()!!.help}")
        }
    }

    @Command("Exits the program")
    fun exit(args: List<String>) { this.exit = true }
    @Command("Exits the program")
    fun quit(args: List<String>) = exit(args)

    @Command("Exports contents of the entry into a real file", "entry-id output-file")
    fun export(args: List<String>) {
        letdisk {
            val entryID = args[1].toLong(10)
            val outfile = File(args[2])
            VDUtil.exportFile(it.entries[entryID]?.contents as? EntryFile ?: throw NullPointerException("No entry with ID $entryID"), outfile)
        }
    }

    @Command("Changes one entry-ID into another", "change-from change-to")
    fun renum(args: List<String>) {
        letdisk {
            val id0 = args[1].toLong(10)
            val id1 = args[2].toLong(10)

            val entry = it.entries.remove(id0)!!
            entry.entryID = id1
            it.entries[id1] = entry
            VDUtil.getAsDirectory(it, 0).remove(id0)
            VDUtil.getAsDirectory(it, 0).add(id1)
        }
    }

    @Command("Imports a real file onto the savefile", "input-file entry-id")
    fun import(args: List<String>) {
        letdisk {
            val file = File(args[1])
            val id = args[2].toLong(10)
            val entry = VDUtil.importFile(file, id, charset)

            it.entries[id] = entry
            entry.parentEntryID = 0
            VDUtil.getAsDirectory(it, 0).add(id)
        }
    }

    @Command("Removes a file within the savefile", "entry-id")
    fun rm(args: List<String>) {
        letdisk {
            val id = args[1].toLong(10)
            it.entries.remove(id)
            VDUtil.getAsDirectory(it, 0).remove(id)
        }
    }

    @Command("Saves changes onto the savefile")
    fun save(args: List<String>) {
        letdisk {
            VDUtil.dumpToRealMachine(it, file!!)
        }
    }
}

internal annotation class Command(val help: String = "", val synopsis: String = "")

fun main(args: Array<String>) {
    SavegameCracker(args).invoke()
}