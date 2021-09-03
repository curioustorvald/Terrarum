package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64Reader
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.EntryFile
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VDUtil
import net.torvald.terrarum.serialise.*
import java.io.File
import java.io.IOException
import java.io.StringReader
import kotlin.reflect.full.declaredMemberProperties

/**
 * Created by minjaesong on 2021-08-30.
 */
object Load : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val charset = Common.CHARSET
                val file = File(AppLoader.defaultDir + "/Exports/${args[1]}")
                val disk = VDUtil.readDiskArchive(file, charset = charset)
                val meta = ReadMeta(disk)

                meta.blocks.forEach { s, str -> println("Module $s\n"); println(str.doc) }
                println(meta.loadorder.joinToString())
            }
            catch (e: IOException) {
                Echo("Load: IOException raised.")
                e.printStackTrace()
            }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: load <filename>")
    }

}