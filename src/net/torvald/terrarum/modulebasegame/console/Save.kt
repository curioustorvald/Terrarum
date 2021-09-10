package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.tvda.DiskEntry
import net.torvald.terrarum.tvda.VDUtil
import net.torvald.terrarum.tvda.VirtualDisk
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.WriteSavegame
import java.io.File
import java.io.IOException

/**
 * Created by minjaesong on 2021-08-29.
 */
object Save : ConsoleCommand {

    private fun acceptable(actor: Actor): Boolean {
        return actor.referenceID !in ReferencingRanges.ACTORS_WIRES &&
               actor.referenceID !in ReferencingRanges.ACTORS_WIRES_HELPER &&
               actor != (CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor)
    }

    private fun addFile(disk: VirtualDisk, file: DiskEntry) {
        VDUtil.getAsDirectory(disk, 0).add(file.entryID)
        disk.entries[file.entryID] = file
        file.parentEntryID = 0
    }

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val ingame = Terrarum.ingame!! as TerrarumIngame
                val savename = args[1].trim()
                val disk = VDUtil.createNewDisk(1L shl 60, savename, Common.CHARSET)
                val file = File(App.defaultDir + "/Exports/${args[1]}")

                WriteSavegame(disk, file, ingame)

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
        Echo("Usage: save <filename>")
    }

}