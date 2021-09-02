package net.torvald.terrarum.debuggerapp

import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VDUtil
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VirtualDisk
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.toCanonicalString
import net.torvald.terrarum.serialise.Common
import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.*
import java.util.logging.Level
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

private val ESC = 27.toChar()

/**
 * Created by minjaesong on 2021-09-02.
 */
class SavegameCracker(
        val args: Array<String>,
        val stdin: InputStream = System.`in`,
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

    private val scanner = Scanner(stdin)

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
        get() = "$ccConst${disk?.diskName?.toString(charset) ?: ""}$cc0% "

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
                println("Disk not loaded; load the disk by running 'load <path-to-savefile>'")
            }
        }

        while (!exit) runInterpreter()

        println("${ccNoun}LX4 ${ccVerb}cya!$cc0")
    }

    private fun runInterpreter() {
        print(prompt)
        val line = scanner.nextLine()
        val args = tokenise(line)

//        println(args.mapIndexed { index, s -> if (index == 0) "$ccNoun$s$cc0" else "$ccVerb$s$cc0" }.joinToString(" "))
        if (args[0].isNotBlank()) {
            cmds[args[0]].let {
                if (it == null)
                    printerrln("${args[0]}: command not found")
                else {
                    try {
                        it.call(this, args)
                    }
                    catch (e: Throwable) {
                        printerrln("An error occured:")
                        e.printStackTrace(stderr)
                    }
                }
            }
        }
    }

    private fun tokenise(line: String): List<String> {
        return line.split(' ') // this will work for now
    }

    private fun letdisk(action: (VirtualDisk) -> Any?): Any? {
        if (disk == null) printerrln("Disk not loaded!")
        else return action(disk!!)
        return null
    }

    private fun printSynopsis(name: String, vararg params: String) {
        print("${cc0}Synopsis: $ccNoun$name ")
        params.forEach { print("$ccNoun2<$it> ") }
        println(cc0)
    }

    @Command("Loads a disk archive")
    fun load(args: List<String>) {
        args.getOrNull(1).let {
            if (it == null)
                printSynopsis(args[0], "path-to-file")
            else {
                file = File(args[1])
                file!!.copyTo(File(file!!.absolutePath + ".bak"), true)
                disk = VDUtil.readDiskArchive(file!!, Level.SEVERE, { printerrln("# Warning: $it") }, charset)
            }
        }
    }

    @Command("Lists contents of the disk")
    fun ls(args: List<String>) {
        letdisk {
            it.entries.forEach { i, entry ->
                println(ccNoun + i.toString(10).padStart(11, ' '), ccNoun2 + entry.filename.toCanonicalString(charset))
            }
            println("${cc0}Entries: ${it.entries.size}, Size: ${it.usedBytes}/${it.capacity} bytes")
        }
    }

    @Command("Prints out available commands and their usage")
    fun help(args: List<String>) {
        cmds.forEach { name, it ->
            println("$ccNoun${name.padStart(8)}$cc0 - ${it.findAnnotation<Command>()!!.help}")
        }
    }

    @Command("Exits the program")
    fun exit(args: List<String>) {
        this.exit = true
    }
}

internal annotation class Command(val help: String = "")

fun main(args: Array<String>) {
    SavegameCracker(args).invoke()
}