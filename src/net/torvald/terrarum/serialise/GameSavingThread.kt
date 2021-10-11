package net.torvald.terrarum.serialise

import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.ccG
import net.torvald.terrarum.ccW
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.toInt
import net.torvald.terrarum.tvda.*
import net.torvald.terrarum.utils.PlayerLastStatus
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * Will happily overwrite existing entry
 */
private fun addFile(disk: VirtualDisk, file: DiskEntry) {
    disk.entries[file.entryID] = file
    file.parentEntryID = 0
    val dir = VDUtil.getAsDirectory(disk, 0)
    if (!dir.contains(file.entryID)) dir.add(file.entryID)
}

abstract class SavingThread(private val ingame: TerrarumIngame) : Runnable {
    abstract fun save()

    override fun run() {
        try {
            save()
        }
        catch (e: Throwable) {
            e.printStackTrace()
            ingame.uiAutosaveNotifier.setAsError()
        }
    }
}

/**
 * Created by minjaesong on 2021-09-14.
 */
class WorldSavingThread(val time_t: Long, val disk: VirtualDisk, val outFile: File, val ingame: TerrarumIngame, val hasThumbnail: Boolean, val isAuto: Boolean, val callback: () -> Unit) : SavingThread(ingame) {

    override fun save() {

        disk.saveMode = 2 * isAuto.toInt() // no quick

        if (hasThumbnail) {
            while (!IngameRenderer.fboRGBexportedLatch) {
                Thread.sleep(1L)
            }
        }

        val allTheActors = ingame.actorContainerActive.cloneToList() + ingame.actorContainerInactive.cloneToList()

        val playersList: List<IngamePlayer> = allTheActors.filter{ it is IngamePlayer } as List<IngamePlayer>
        val actorsList = allTheActors.filter { WriteWorld.actorAcceptable(it) }
        val layers = intArrayOf(0,1).map { ingame.world.getLayer(it) }
        val cw = ingame.world.width / LandUtil.CHUNK_W
        val ch = ingame.world.height / LandUtil.CHUNK_H

        WriteSavegame.saveProgress = 0f
        WriteSavegame.saveProgressMax = 2f + (cw * ch * layers.size) + actorsList.size


        val tgaout = ByteArray64GrowableOutputStream()
        val gzout = GZIPOutputStream(tgaout)

        Echo("Writing metadata...")

        val creation_t = ingame.world.creationTime


        if (hasThumbnail) {
            PixmapIO2._writeTGA(gzout, IngameRenderer.fboRGBexport, true, true)
            IngameRenderer.fboRGBexport.dispose()

            val thumbContent = EntryFile(tgaout.toByteArray64())
            val thumb = DiskEntry(-2, 0, creation_t, time_t, thumbContent)
            addFile(disk, thumb)
        }

        WriteSavegame.saveProgress += 1f

        // Write World //
        // record all player's last position
        playersList.forEach {
            ingame.world.playersLastStatus[it.uuid] = PlayerLastStatus(it, ingame.isMultiplayer)
        }
        val worldMeta = EntryFile(WriteWorld.encodeToByteArray64(ingame, time_t))
        val world = DiskEntry(-1L, 0, creation_t, time_t, worldMeta)
        addFile(disk, world)

        WriteSavegame.saveProgress += 1f


        for (layer in layers.indices) {
            for (cx in 0 until cw) {
                for (cy in 0 until ch) {
                    val chunkNumber = LandUtil.chunkXYtoChunkNum(ingame.world, cx, cy).toLong()

                    Echo("Writing chunks... ${(cw*ch*layer) + chunkNumber + 1}/${cw*ch*layers.size}")

                    val chunkBytes = WriteWorld.encodeChunk(layers[layer]!!, cx, cy)
                    val entryID = 0x1_0000_0000L or layer.toLong().shl(24) or chunkNumber

                    val entryContent = EntryFile(chunkBytes)
                    val entry = DiskEntry(entryID, 0, creation_t, time_t, entryContent)
                    // "W1L0-92,15"
                    addFile(disk, entry)

                    WriteSavegame.saveProgress += 1
                }
            }
        }


        // Write Actors //
        actorsList.forEachIndexed { count, it ->
            Echo("Writing actors... ${count+1}/${actorsList.size}")

            val actorContent = EntryFile(WriteActor.encodeToByteArray64(it))
            val actor = DiskEntry(it.referenceID.toLong(), 0, creation_t, time_t, actorContent)
            addFile(disk, actor)

            WriteSavegame.saveProgress += 1
        }


        Echo("Writing file to disk...")

        disk.entries[0]!!.modificationDate = time_t
        // entry zero MUST NOT be used to get lastPlayDate, but we'll update it anyway
        // use entry -1 for that purpose!
        disk.capacity = 0
        VDUtil.dumpToRealMachine(disk, outFile)



        Echo ("${ccW}Game saved with size of $ccG${outFile.length()}$ccW bytes")


        if (hasThumbnail) IngameRenderer.fboRGBexportedLatch = false
        WriteSavegame.savingStatus = 255


        callback()
    }
}

/**
 * This function called means the "Avatar" was not externally created and thus has no sprite-bodypart-name-to-entry-number-map
 *
 * Created by minjaesong on 2021-10-08
 */
class PlayerSavingThread(val time_t: Long, val disk: VirtualDisk, val outFile: File, val ingame: TerrarumIngame, val hasThumbnail: Boolean, val isAuto: Boolean, val callback: () -> Unit) : SavingThread(ingame) {

    override fun save() {
        disk.saveMode = 2 * isAuto.toInt() // no quick
        disk.capacity = 0L

        Echo("Writing The Player...")
        WritePlayer(ingame.actorGamer, disk, ingame, time_t)
        VDUtil.dumpToRealMachine(disk, outFile)

        callback()
    }
}
