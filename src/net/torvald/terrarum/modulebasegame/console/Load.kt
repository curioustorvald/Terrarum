package net.torvald.terrarum.modulebasegame.console

import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.DiskEntry
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.EntryFile
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
                val charset = Common.CHARSET
                val file = File(AppLoader.defaultDir + "Exports/${args[1]}")
                val disk = VDUtil.readDiskArchive(file, charset = charset)

                val metaFile = VDUtil.getFile(disk, VDUtil.VDPath("savegame", charset))!!
                val metaReader = ByteArray64Reader(metaFile.contents.serialize().array, charset)
                val meta = Common.jsoner.fromJson(JsonValue::class.java, metaReader)

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