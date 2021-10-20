package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.savegame.VDUtil
import java.io.File
import java.io.IOException

/**
 * Created by minjaesong on 2021-08-29.
 */
object Save : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val ingame = Terrarum.ingame!! as TerrarumIngame
                val savename = args[1].trim()
                val disk = VDUtil.createNewDisk(1L shl 60, savename, Common.CHARSET)
                val file = File(App.saveDir + "/${args[1]}")

//                WriteSavegame(disk, file, ingame, false)

            }
            catch (e: IOException) {
                Echo("Save: IOException raised.")
                e.printStackTrace()
            }
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: save <new-savegame-name>")
    }

}

object Quicksave : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val ingame = Terrarum.ingame!! as TerrarumIngame

//        WriteSavegame.quick(ingame.savegameArchive, ingame.getSaveFileMain(), ingame, false) {}
    }

    override fun printUsage() {
        Echo("Usage: quicksave")
    }
}