package net.torvald.terrarum.modulebasegame.console

import net.torvald.ELLIPSIS
import net.torvald.terrarum.App
import net.torvald.terrarum.ccC
import net.torvald.terrarum.ccG
import net.torvald.terrarum.ccR
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VDUtil
import net.torvald.terrarum.serialise.*
import java.io.File
import java.io.IOException

/**
 * Created by minjaesong on 2021-08-30.
 */
object Load : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                Echo("${ccC}Changing context, ${ccR}do not touch the controller$ccC and ${ccG}wait$ccC$ELLIPSIS")

                val charset = Common.CHARSET
                val file = File(App.defaultDir + "/Exports/${args[1]}")
                val disk = VDUtil.readDiskArchive(file, charset = charset)

                LoadSavegame(disk)
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