package net.torvald.terrarum.serialise

import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.App
import net.torvald.terrarum.ccG
import net.torvald.terrarum.ccW
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.toInt
import net.torvald.terrarum.tvda.*
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-09-29.
 */
class QuickSaveThread(val disk: VirtualDisk, val file: File, val ingame: TerrarumIngame, val hasThumbnail: Boolean, val isAuto: Boolean, val callback: () -> Unit) : Runnable {

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


    override fun run() {
        val skimmer = DiskSkimmer(file, Common.CHARSET)

        if (hasThumbnail) {
            while (!IngameRenderer.fboRGBexportedLatch) {
                Thread.sleep(1L)
            }
        }

        val actorsList = listOf(ingame.actorContainerActive).flatMap { it.filter { WriteWorld.actorAcceptable(it) } }
        val chunks = ingame.modifiedChunks

        val chunkCount = chunks.map { it.size }.sum()

        WriteSavegame.saveProgress = 0f
        WriteSavegame.saveProgressMax = 2f +
                                        (chunkCount) * chunkProgressMultiplier +
                                        actorsList.size * actorProgressMultiplier


        val tgaout = ByteArray64GrowableOutputStream()
        val gzout = GZIPOutputStream(tgaout)

        Echo("Writing metadata...")

        val creation_t = ingame.creationTime
        val time_t = App.getTIME_T()
        

        // Write Meta //
        val metaContent = EntryFile(WriteMeta.encodeToByteArray64(ingame, time_t))
        val meta = DiskEntry(-1, 0, creation_t, time_t, metaContent)
        addFile(disk, meta); skimmer.appendEntryOnly(meta)

        if (hasThumbnail) {
            PixmapIO2._writeTGA(gzout, IngameRenderer.fboRGBexport, true, true)
            IngameRenderer.fboRGBexport.dispose()

            val thumbContent = EntryFile(tgaout.toByteArray64())
            val thumb = DiskEntry(-2, 0, creation_t, time_t, thumbContent)
            addFile(disk, thumb); skimmer.appendEntryOnly(thumb)
        }

        WriteSavegame.saveProgress += 1f


        // Write World //
        val worldNum = ingame.world.worldIndex
        val worldMeta = EntryFile(WriteWorld.encodeToByteArray64(ingame, time_t))
        val world = DiskEntry(worldNum.toLong(), 0, creation_t, time_t, worldMeta)
        addFile(disk, world); skimmer.appendEntryOnly(world)

        WriteSavegame.saveProgress += 1f

        var chunksWrote = 1
        chunks.forEachIndexed { layerNum, chunks ->

            if (chunks.size != 0) {
                val layer = ingame.world.getLayer(layerNum)

                chunks.forEach { chunkNumber ->

                    Echo("Writing chunks... $chunksWrote/$chunkCount")

                    val chunkXY = LandUtil.chunkNumToChunkXY(ingame.world, chunkNumber)

//                    println("Chunk xy from number $chunkNumber -> (${chunkXY.x}, ${chunkXY.y})")

                    val chunkBytes = WriteWorld.encodeChunk(layer, chunkXY.x, chunkXY.y)
                    val entryID = worldNum.toLong().shl(32) or layerNum.toLong().shl(24) or chunkNumber.toLong()

                    val entryContent = EntryFile(chunkBytes)
                    val entry = DiskEntry(entryID, 0, creation_t, time_t, entryContent)
                    // "W1L0-92,15"
                    addFile(disk, entry); skimmer.appendEntryOnly(entry)

                    WriteSavegame.saveProgress += chunkProgressMultiplier
                    chunksWrote += 1

                }
            }
        }


        // Write Actors //
        actorsList.forEachIndexed { count, it ->
            Echo("Writing actors... ${count+1}/${actorsList.size}")

            val actorContent = EntryFile(WriteActor.encodeToByteArray64(it))
            val actor = DiskEntry(it.referenceID.toLong(), 0, creation_t, time_t, actorContent)
            addFile(disk, actor); skimmer.appendEntryOnly(actor)

            WriteSavegame.saveProgress += actorProgressMultiplier
        }


        skimmer.rewriteDirectories()
        skimmer.injectDiskCRC(disk.hashCode())
        skimmer.setSaveMode(1 + 2 * isAuto.toInt())

        Echo ("${ccW}Game saved with size of $ccG${file.length()}$ccW bytes")


        if (hasThumbnail) IngameRenderer.fboRGBexportedLatch = false
        WriteSavegame.savingStatus = 255
        ingame.clearModifiedChunks()

        callback()
    }

}