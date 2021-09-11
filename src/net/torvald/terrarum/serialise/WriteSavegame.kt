package net.torvald.terrarum.serialise

import com.badlogic.gdx.graphics.Pixmap
import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.Common.getUnzipInputStream
import net.torvald.terrarum.serialise.Common.zip
import net.torvald.terrarum.serialise.WriteWorld.actorAcceptable
import net.torvald.terrarum.tvda.*
import java.io.File
import java.io.Reader
import java.util.zip.GZIPOutputStream

/**
 * It's your responsibility to create a new VirtualDisk if your save is new, and create a backup for modifying existing save.
 *
 * Created by minjaesong on 2021-09-03.
 */
object WriteSavegame {

    /**
     * Will happily overwrite existing entry
     */
    private fun addFile(disk: VirtualDisk, file: DiskEntry) {
        disk.entries[file.entryID] = file
        file.parentEntryID = 0
        val dir = VDUtil.getAsDirectory(disk, 0)
        if (!dir.contains(file.entryID)) dir.add(file.entryID)
    }

    operator fun invoke(disk: VirtualDisk, outFile: File, ingame: TerrarumIngame) {
        val tgaout = ByteArray64GrowableOutputStream()
        val gzout = GZIPOutputStream(tgaout)
        IngameRenderer.fboRGBexportCallback = {
            val w = 960
            val h = 640
            val p = Pixmap.createFromFrameBuffer((it.width - w).ushr(1), (it.height - h).ushr(1), w, h)
            PixmapIO2._writeTGA(gzout, p, true, true)
            p.dispose()
            

            val creation_t = ingame.world.creationTime
            val time_t = App.getTIME_T()
            val currentPlayTime_t = time_t - ingame.loadedTime_t


            // Write Meta //
            val metaContent = EntryFile(WriteMeta.encodeToByteArray64(ingame, currentPlayTime_t))
            val meta = DiskEntry(-1, 0, creation_t, time_t, metaContent)
            addFile(disk, meta)
            
            
            val thumbContent = EntryFile(tgaout.toByteArray64())
            val thumb = DiskEntry(-2, 0, creation_t, time_t, thumbContent)
            addFile(disk, thumb)
            

            // Write BlockCodex//
//        val blockCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(BlockCodex).toByteArray(Common.CHARSET))))
//        val blocks = DiskEntry(-16, 0, "blocks".toByteArray(Common.CHARSET), creation_t, time_t, blockCodexContent)
//        addFile(disk, blocks)
            // Commented out; nothing to write

            // Write ItemCodex//
            val itemCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(ItemCodex).toByteArray(Common.CHARSET))))
            val items = DiskEntry(-17, 0, creation_t, time_t, itemCodexContent)
            addFile(disk, items)
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
            val apocryphasContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(Apocryphas).toByteArray(Common.CHARSET))))
            val apocryphas = DiskEntry(-1024, 0, creation_t, time_t, apocryphasContent)
            addFile(disk, apocryphas)

            // Write World //
            val worldNum = ingame.world.worldIndex
            val worldMeta = EntryFile(WriteWorld.encodeToByteArray64(ingame))
            val world = DiskEntry(worldNum.toLong(), 0, creation_t, time_t, worldMeta)
            addFile(disk, world)

            val layers = intArrayOf(0,1).map { ingame.world.getLayer(it) }
            val cw = ingame.world.width / LandUtil.CHUNK_W
            val ch = ingame.world.height / LandUtil.CHUNK_H
            for (layer in layers.indices) {
                for (cy in 0 until ch) {
                    for (cx in 0 until cw) {
                        val chunkNumber = (cy * cw + cx).toLong()
                        val chunkBytes = WriteWorld.encodeChunk(layers[layer], cx, cy)
                        val entryID = worldNum.toLong().shl(32) or layer.toLong().shl(24) or chunkNumber

                        val entryContent = EntryFile(chunkBytes)
                        val entry = DiskEntry(entryID, 0, creation_t, time_t, entryContent)
                        // "W1L0-92,15"
                        addFile(disk, entry)
                    }
                }
            }


            // Write Actors //
            listOf(ingame.actorContainerActive, ingame.actorContainerInactive).forEach { actors ->
                actors.forEach {
                    if (actorAcceptable(it)) {
                        val actorContent = EntryFile(WriteActor.encodeToByteArray64(it))
                        val actor = DiskEntry(it.referenceID.toLong(), 0, creation_t, time_t, actorContent)
                        addFile(disk, actor)
                    }
                }
            }

            disk.capacity = 0
            VDUtil.dumpToRealMachine(disk, outFile)



            Echo ("${ccW}Game saved with size of $ccG${outFile.length()}$ccW bytes")
        }
        IngameRenderer.fboRGBexportRequested = true
    }
}



/**
 * Load and setup the game for the first load.
 *
 * To load additional actors/worlds, use ReadActor/ReadWorld.
 *
 * Created by minjaesong on 2021-09-03.
 */
object LoadSavegame {

    private fun getFileBytes(disk: VirtualDisk, id: Long): ByteArray64 = VDUtil.getAsNormalFile(disk, id).getContent()
    private fun getFileReader(disk: VirtualDisk, id: Long): Reader = ByteArray64Reader(getFileBytes(disk, id), Common.CHARSET)

    operator fun invoke(disk: VirtualDisk) {
        val newIngame = TerrarumIngame(App.batch)

        // NOTE: do NOT set ingame.actorNowPlaying as one read directly from the disk;
        // you'll inevitably read the player actor twice, and they're separate instances of the player!
        val meta = ReadMeta(disk)
        val currentWorld = (ReadActor(getFileReader(disk, Terrarum.PLAYER_REF_ID.toLong())) as IngamePlayer).worldCurrentlyPlaying
        val world = ReadWorld(getFileReader(disk, currentWorld.toLong()))
        val actors = world.actors.distinct().map { ReadActor(getFileReader(disk, it.toLong())) }
//        val block = Common.jsoner.fromJson(BlockCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -16)))
        val item = Common.jsoner.fromJson(ItemCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -17)))
//        val wire = Common.jsoner.fromJson(WireCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -18)))
//        val material = Common.jsoner.fromJson(MaterialCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -19)))
//        val faction = Common.jsoner.fromJson(FactionCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -20)))
        val apocryphas = Common.jsoner.fromJson(Apocryphas.javaClass, getUnzipInputStream(getFileBytes(disk, -1024)))

        // set lateinit vars on the gameworld
        world.layerTerrain = BlockLayer(world.width, world.height)
        world.layerWall = BlockLayer(world.width, world.height)

        val worldParam = TerrarumIngame.Codices(meta, item, apocryphas)
        newIngame.world = world
        newIngame.gameLoadInfoPayload = worldParam
        newIngame.gameLoadMode = TerrarumIngame.GameLoadMode.LOAD_FROM
        newIngame.savegameArchive = disk

        // load all the world blocklayer chunks
        val worldnum = world.worldIndex.toLong()
        val cw = LandUtil.CHUNK_W; val ch = LandUtil.CHUNK_H
        val chunksX = world.width / cw
        for (layer in 0L..1L) {
            val worldLayer = world.getLayer(layer.toInt())

            for (chunk in 0L until (world.width * world.height) / (cw * ch)) {
                val chunkFile = VDUtil.getAsNormalFile(disk, worldnum.shl(32) or layer.shl(24) or chunk)
                val cx = chunk % chunksX
                val cy = chunk / chunksX
                ReadWorld.decodeChunkToLayer(chunkFile.getContent(), worldLayer, cx.toInt(), cy.toInt())
            }
        }

        actors.forEach { newIngame.addNewActor(it) }

        // by doing this, whatever the "possession" the player had will be broken by the game load
        newIngame.actorNowPlaying = newIngame.getActorByID(Terrarum.PLAYER_REF_ID) as IngamePlayer


//        ModMgr.reloadModules()

        Terrarum.setCurrentIngameInstance(newIngame)
        App.setScreen(newIngame)

        Echo("${ccW}Savegame loaded from $ccY${disk.getDiskNameString(Common.CHARSET)}")
        printdbg(this, "Savegame loaded from ${disk.getDiskNameString(Common.CHARSET)}")

//        Terrarum.ingame!!.consoleHandler.setAsOpen()
    }

}