package net.torvald.terrarum.modulebasegame.serialise

import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.savegame.VDFileID.ROOT
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.savegame.VDFileID.THUMBNAIL
import net.torvald.terrarum.serialise.Common
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
        val hasThumbnail: Boolean,
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
        val skimmer = DiskSkimmer(outFile, Common.CHARSET)

        if (hasThumbnail) {
            while (!IngameRenderer.fboRGBexportedLatch) {
                Thread.sleep(1L)
            }
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


        if (hasThumbnail) {
            PixmapIO2._writeTGA(gzout, IngameRenderer.fboRGBexport, true, true)
            IngameRenderer.fboRGBexport.dispose()

            val thumbContent = EntryFile(tgaout.toByteArray64())
            val thumb = DiskEntry(THUMBNAIL, ROOT, creation_t, time_t, thumbContent)
            addFile(disk, thumb)
        }

        WriteSavegame.saveProgress += 1f

        // Write World //
        // record all player's last position
        playersList.forEach {
            ingame.world.playersLastStatus[it.uuid] = PlayerLastStatus(it, ingame.isMultiplayer)
        }
        val worldMeta = EntryFile(WriteWorld.encodeToByteArray64(ingame, time_t, actorsList, playersList))
        val world = DiskEntry(SAVEGAMEINFO, ROOT, creation_t, time_t, worldMeta)
        addFile(disk, world); skimmer.appendEntryOnly(world)

        WriteSavegame.saveProgress += 1f

        var chunksWrote = 1
        chunks.forEachIndexed { layerNum, chunks ->

            if (chunks.size != 0) {
                ingame.world.getLayer(layerNum)?.let { layer ->
                    chunks.forEach { chunkNumber ->

//                        Echo("Writing chunks... $chunksWrote/$chunkCount")

                        val chunkXY = LandUtil.chunkNumToChunkXY(ingame.world, chunkNumber)

//                    println("Chunk xy from number $chunkNumber -> (${chunkXY.x}, ${chunkXY.y})")

                        val chunkBytes = WriteWorld.encodeChunk(layer, chunkXY.x, chunkXY.y)
                        val entryID = 0x1_0000_0000L or layerNum.toLong().shl(24) or chunkNumber.toLong()

                        val entryContent = EntryFile(chunkBytes)
                        val entry = DiskEntry(entryID, ROOT, creation_t, time_t, entryContent)
                        // "W1L0-92,15"
                        addFile(disk, entry); skimmer.appendEntryOnly(entry)

                        WriteSavegame.saveProgress += chunkProgressMultiplier
                        chunksWrote += 1

                    }
                }
            }
        }


        // Write Actors //
        actorsList.forEachIndexed { count, it ->
            printdbg(this, "Writing actors... ${count+1}/${actorsList.size}")

            val actorContent = EntryFile(WriteActor.encodeToByteArray64(it))
            val actor = DiskEntry(it.referenceID.toLong(), ROOT, creation_t, time_t, actorContent)
            addFile(disk, actor); skimmer.appendEntryOnly(actor)

            WriteSavegame.saveProgress += actorProgressMultiplier
        }


        skimmer.rewriteDirectories()
        skimmer.injectDiskCRC(disk.hashCode())
        skimmer.setSaveMode(1 + 2 * isAuto.toInt())

        printdbg(this, "Game saved with size of ${outFile.length()} bytes")


        if (hasThumbnail) IngameRenderer.fboRGBexportedLatch = false
        WriteSavegame.savingStatus = 255
        ingame.clearModifiedChunks()

        callback()
    }

}