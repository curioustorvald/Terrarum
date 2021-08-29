package net.torvald.terrarum.modulebasegame.console

import com.badlogic.gdx.Gdx
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.DiskEntry
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.DiskEntryContent
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.EntryFile
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VDUtil
import net.torvald.terrarum.serialise.WriteActor
import net.torvald.terrarum.serialise.WriteMeta
import net.torvald.terrarum.serialise.WriteWorld
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

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            try {
                val ingame = Terrarum.ingame!! as TerrarumIngame
                val savename = args[1].trim()
                val creation_t = VDUtil.currentUnixtime
                val time_t = VDUtil.currentUnixtime

                val disk = VDUtil.createNewDisk(1L shl 60, savename, Charsets.UTF_8)

                val metaContent = EntryFile(WriteMeta(ingame).encodeToByteArray64())
                val meta = DiskEntry(32768, 0, "savegame.json".toByteArray(), creation_t, time_t, metaContent)
                VDUtil.addFile(disk, 0, meta, false)

                val worldContent = EntryFile(WriteWorld(ingame).encodeToByteArray64())
                val world = DiskEntry(ingame.world.worldIndex, 0, "world${ingame.world.worldIndex}.json".toByteArray(), creation_t, time_t, worldContent)
                VDUtil.addFile(disk, 0, world, false)

                listOf(ingame.actorContainerActive, ingame.actorContainerInactive).forEach { actors ->
                    actors.forEach {
                        if (acceptable(it)) {
                            val actorContent = EntryFile(WriteActor.encodeToByteArray64(it))
                            val actor = DiskEntry(it.referenceID, 0, "actor${it.referenceID}.json".toByteArray(), creation_t, time_t, actorContent)
                            VDUtil.addFile(disk, 0, actor, false)
                        }
                    }
                }

                disk.capacity = 0
                VDUtil.dumpToRealMachine(disk, File(AppLoader.defaultDir + "/Exports/${args[1]}"))
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