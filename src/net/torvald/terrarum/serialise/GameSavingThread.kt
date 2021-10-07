package net.torvald.terrarum.serialise

import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.*
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.tvda.*
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-09-14.
 */
class GameSavingThread(val disk: VirtualDisk, val outFile: File, val ingame: TerrarumIngame, val hasThumbnail: Boolean, val isAuto: Boolean, val callback: () -> Unit) : Runnable {

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
        callback()
        return

        // TODO //

        disk.saveMode = 2 * isAuto.toInt() // no quick

        if (hasThumbnail) {
            while (!IngameRenderer.fboRGBexportedLatch) {
                Thread.sleep(1L)
            }
        }

        val actorsList = listOf(ingame.actorContainerActive, ingame.actorContainerInactive).flatMap { it.filter { WriteWorld.actorAcceptable(it) } }
        val layers = intArrayOf(0,1).map { ingame.world.getLayer(it) }
        val cw = ingame.world.width / LandUtil.CHUNK_W
        val ch = ingame.world.height / LandUtil.CHUNK_H

        WriteSavegame.saveProgress = 0f
        WriteSavegame.saveProgressMax = 2f +
                                        (cw * ch * layers.size) * chunkProgressMultiplier +
                                        actorsList.size * actorProgressMultiplier


        val tgaout = ByteArray64GrowableOutputStream()
        val gzout = GZIPOutputStream(tgaout)

        Echo("Writing metadata...")

        val creation_t = ingame.creationTime
        val time_t = App.getTIME_T()


        // Write Meta //
        val metaContent = EntryFile(WriteMeta.encodeToByteArray64(ingame, time_t))
        val meta = DiskEntry(-1, 0, creation_t, time_t, metaContent)
        addFile(disk, meta)

        if (hasThumbnail) {
            PixmapIO2._writeTGA(gzout, IngameRenderer.fboRGBexport, true, true)
            IngameRenderer.fboRGBexport.dispose()

            val thumbContent = EntryFile(tgaout.toByteArray64())
            val thumb = DiskEntry(-2, 0, creation_t, time_t, thumbContent)
            addFile(disk, thumb)
        }

        WriteSavegame.saveProgress += 1f


        // Write BlockCodex//
//        val blockCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(BlockCodex).toByteArray(Common.CHARSET))))
//        val blocks = DiskEntry(-16, 0, "blocks".toByteArray(Common.CHARSET), creation_t, time_t, blockCodexContent)
//        addFile(disk, blocks)
        // Commented out; nothing to write

        // Write ItemCodex//
//        val itemCodexContent = EntryFile(Common.zip(ByteArray64.fromByteArray(Common.jsoner.toJson(ItemCodex).toByteArray(Common.CHARSET))))
//        val items = DiskEntry(-17, 0, creation_t, time_t, itemCodexContent)
//        addFile(disk, items)
        // Gotta save dynamicIDs

        // Write WireCodex//
//        val wireCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(WireCodex).toByteArray(Common.CHARSET))))
//        val wires = DiskEntry(-18, 0, "wires".toByteArray(Common.CHARSET), creation_t, time_t, wireCodexContent)
//        addFile(disk, wires)
        // Commented out; nothing to write

        // Write MaterialCodex//
//        val materialCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(MaterialCodex).toByteArray(Common.CHARSET))))
//        val materials = DiskEntry(-19, 0, "materials".toByteArray(Common.CHARSET), creation_t, time_t, materialCodexContent)
//        addFile(disk, materials)
        // Commented out; nothing to write

        // Write FactionCodex//
//        val factionCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(FactionCodex).toByteArray(Common.CHARSET))))
//        val factions = DiskEntry(-20, 0, "factions".toByteArray(Common.CHARSET), creation_t, time_t, factionCodexContent)
//        addFile(disk, factions)

        // Write Apocryphas//
//        val apocryphasContent = EntryFile(Common.zip(ByteArray64.fromByteArray(Common.jsoner.toJson(Apocryphas).toByteArray(Common.CHARSET))))
//        val apocryphas = DiskEntry(-1024, 0, creation_t, time_t, apocryphasContent)
//        addFile(disk, apocryphas)

        // Write World //
//        val worldNum = ingame.world.worldIndex
//        val worldMeta = EntryFile(WriteWorld.encodeToByteArray64(ingame, time_t))
//        val world = DiskEntry(worldNum.toLong(), 0, creation_t, time_t, worldMeta)
//        addFile(disk, world)

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

                    WriteSavegame.saveProgress += chunkProgressMultiplier
                }
            }
        }


        // Write Actors //
        actorsList.forEachIndexed { count, it ->
            Echo("Writing actors... ${count+1}/${actorsList.size}")

            val actorContent = EntryFile(WriteActor.encodeToByteArray64(it))
            val actor = DiskEntry(it.referenceID.toLong(), 0, creation_t, time_t, actorContent)
            addFile(disk, actor)

            WriteSavegame.saveProgress += actorProgressMultiplier
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