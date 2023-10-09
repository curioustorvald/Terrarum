package net.torvald.terrarum.modulebasegame.serialise

import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.savegame.VDFileID.ROOT
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.savegame.VDFileID.WORLD_SCREENSHOT
import net.torvald.terrarum.toInt
import net.torvald.terrarum.utils.PlayerLastStatus
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-09-29.
 */
class QuickSingleplayerWorldSavingThread(
        val time_t: Long,
        val disk: VirtualDisk,
        val outFile: File,
        val ingame: TerrarumIngame,
        val isAuto: Boolean,
        val callback: () -> Unit,
        val errorHandler: (Throwable) -> Unit
) : SavingThread(errorHandler) {

    /**
     * Will happily overwrite existing entry
     */
    private fun addFile(disk: VirtualDisk, file: DiskEntry) {
        disk.entries[file.entryID] = file
        file.parentEntryID = 0
        val dir = VDUtil.getAsDirectory(disk, 0)
        if (!dir.contains(file.entryID)) dir.add(file.entryID)
    }


    private val chunkProgressMultiplier = 1f
    private val actorProgressMultiplier = 1f


    override fun save() {
        printdbg(this, "outFile: ${outFile.path}")

        val skimmer = DiskSkimmer(outFile)

        // wait for screencap
        var emergencyStopCnt = 0
        while (IngameRenderer.screencapBusy) {
//            printdbg(this, "spinning for screencap to be taken")
            Thread.sleep(4L)
            emergencyStopCnt += 1
            if (emergencyStopCnt >= SCREENCAP_WAIT_TRY_MAX) throw InterruptedException("Waiting screencap to be taken for too long")
        }

        val allTheActors = ingame.actorContainerActive.cloneToList() + ingame.actorContainerInactive.cloneToList()

        val playersList: List<IngamePlayer> = allTheActors.filter{ it is IngamePlayer } as List<IngamePlayer>
        val actorsList = allTheActors.filter { WriteWorld.actorAcceptable(it) }
        val chunks = ingame.modifiedChunks

        val chunkCount = chunks.map { it.size }.sum()

        WriteSavegame.saveProgress = 0f
        WriteSavegame.saveProgressMax = 2f +
                                        (chunkCount) * chunkProgressMultiplier +
                                        actorsList.size * actorProgressMultiplier


        val tgaout = ByteArray64GrowableOutputStream()
        val gzout = GZIPOutputStream(tgaout)

        printdbg(this, "Writing metadata...")

        val creation_t = ingame.world.creationTime


        PixmapIO2._writeTGA(gzout, IngameRenderer.fboRGBexport, true, true)
        IngameRenderer.fboRGBexport.dispose()

        val thumbContent = EntryFile(tgaout.toByteArray64())
        val thumb = DiskEntry(WORLD_SCREENSHOT, ROOT, creation_t, time_t, thumbContent)
        addFile(disk, thumb)



        WriteSavegame.saveProgress += 1f

        // Write World //
        // record all player's last position
        playersList.forEach {
            ingame.world.playersLastStatus[it.uuid] = PlayerLastStatus(it, ingame.isMultiplayer)
        }
        val worldMeta = EntryFile(WriteWorld.encodeToByteArray64(ingame, time_t, actorsList, playersList))
        val world = DiskEntry(SAVEGAMEINFO, ROOT, creation_t, time_t, worldMeta)
        addFile(disk, world); skimmer.appendEntry(world)

        WriteSavegame.saveProgress += 1f

        var chunksWrote = 1
        chunks.forEachIndexed { layerNum, chunks ->

            if (chunks.size != 0) {
                ingame.world.getLayer(layerNum)?.let { layer ->
                    chunks.forEach { chunkNumber ->

                        printdbg(this, "Writing chunks... $chunksWrote/$chunkCount (chunk# $chunkNumber at layer# $layerNum)")

                        val chunkXY = LandUtil.chunkNumToChunkXY(ingame.world, chunkNumber)

//                    println("Chunk xy from number $chunkNumber -> (${chunkXY.x}, ${chunkXY.y})")

                        val chunkBytes = WriteWorld.encodeChunk(layer, chunkXY.x, chunkXY.y)
                        val entryID = 0x1_0000_0000L or layerNum.toLong().shl(24) or chunkNumber.toLong()

                        val entryContent = EntryFile(chunkBytes)
                        val entry = DiskEntry(entryID, ROOT, creation_t, time_t, entryContent)
                        // "W1L0-92,15"
                        addFile(disk, entry); skimmer.appendEntry(entry)

                        WriteSavegame.saveProgress += chunkProgressMultiplier
                        chunksWrote += 1

                    }
                }
            }
        }


        // Write Actors //
        actorsList.forEachIndexed { count, it ->
            printdbg(this, "Writing actors... ${count+1}/${actorsList.size} (${it.javaClass.canonicalName})")

            val actorContent = EntryFile(WriteActor.encodeToByteArray64(it))
            val actor = DiskEntry(it.referenceID.toLong(), ROOT, creation_t, time_t, actorContent)
            addFile(disk, actor)
            skimmer.appendEntry(actor)

            WriteSavegame.saveProgress += actorProgressMultiplier
        }


        disk.saveKind = VDSaveKind.WORLD_DATA

//        skimmer.rewriteDirectories()
        skimmer.injectDiskCRC(disk.hashCode())
        skimmer.setSaveMode(1 + 2 * isAuto.toInt())
        skimmer.setSaveKind(VDSaveKind.WORLD_DATA)
        skimmer.setSaveSnapshotVersion(TerrarumAppConfiguration.VERSION_SNAPSHOT)
        skimmer.setSaveOrigin(skimmer.getSaveOrigin() and 15) // remove flag "imported" if applicable
        skimmer.setLastModifiedTime(time_t)
        skimmer.setCreationTime(creation_t)

        printdbg(this, "Game saved with size of ${outFile.length()} bytes")


//        IngameRenderer.screencapBusy = false
        WriteSavegame.savingStatus = 255
        ingame.clearModifiedChunks()

        callback()
    }

}