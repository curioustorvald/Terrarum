package net.torvald.terrarum.serialise

import com.badlogic.gdx.graphics.Pixmap
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ChunkLoadingLoadScreen
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.Common.getUnzipInputStream
import net.torvald.terrarum.tvda.ByteArray64
import net.torvald.terrarum.tvda.ByteArray64Reader
import net.torvald.terrarum.tvda.VDUtil
import net.torvald.terrarum.tvda.VirtualDisk
import java.io.File
import java.io.Reader

/**
 * It's your responsibility to create a new VirtualDisk if your save is new, and create a backup for modifying existing save.
 *
 * Created by minjaesong on 2021-09-03.
 */
object WriteSavegame {

    enum class SaveMode {
        META, PLAYER, WORLD, SHARED
    }

    @Volatile var savingStatus = -1 // -1: not started, 0: saving in progress, 255: saving finished
    @Volatile var saveProgress = 0f
    @Volatile var saveProgressMax = 1f

    private fun getSaveThread(mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, hasThumbnail: Boolean, isAuto: Boolean, callback: () -> Unit = {}) = when (mode) {
        SaveMode.WORLD -> WorldSavingThread(disk, outFile, ingame, hasThumbnail, isAuto, callback)
        SaveMode.PLAYER -> PlayerSavingThread(disk, outFile, ingame, hasThumbnail, isAuto, callback)
        else -> throw IllegalArgumentException("$mode")
    }

    operator fun invoke(mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, callback: () -> Unit = {}) {
        savingStatus = 0

        Echo("Save queued")

        IngameRenderer.screencapExportCallback = {
            Echo("Generating thumbnail...")

            val w = 960
            val h = 640
            val p = Pixmap.createFromFrameBuffer((it.width - w).ushr(1), (it.height - h).ushr(1), w, h)
            IngameRenderer.fboRGBexport = p
            //PixmapIO2._writeTGA(gzout, p, true, true)
            //p.dispose()
            IngameRenderer.fboRGBexportedLatch = true

            Echo("Done thumbnail generation")
        }
        IngameRenderer.screencapRequested = true

        val savingThread = Thread(getSaveThread(mode, disk, outFile, ingame, true, isAuto, callback), "TerrarumBasegameGameSaveThread")
        savingThread.start()

        // it is caller's job to keep the game paused or keep a "save in progress" ui up
        // use field 'savingStatus' to know when the saving is done
    }


    fun immediate(mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, hasThumbnail: Boolean, isAuto: Boolean, callback: () -> Unit = {}) {

        savingStatus = 0

        Echo("Immediate save fired")

        val savingThread = Thread(getSaveThread(mode, disk, outFile, ingame, false, isAuto, callback), "TerrarumBasegameGameSaveThread")
        savingThread.start()

        // it is caller's job to keep the game paused or keep a "save in progress" ui up
        // use field 'savingStatus' to know when the saving is done
    }

    fun quick(disk: VirtualDisk, file: File, ingame: TerrarumIngame, isAuto: Boolean, callback: () -> Unit = {}) {
        return

        // TODO //

        savingStatus = 0

        Echo("Quicksave queued")

        IngameRenderer.screencapExportCallback = {
            Echo("Generating thumbnail...")

            val w = 960
            val h = 640
            val p = Pixmap.createFromFrameBuffer((it.width - w).ushr(1), (it.height - h).ushr(1), w, h)
            IngameRenderer.fboRGBexport = p
            //PixmapIO2._writeTGA(gzout, p, true, true)
            //p.dispose()
            IngameRenderer.fboRGBexportedLatch = true

            Echo("Done thumbnail generation")
        }
        IngameRenderer.screencapRequested = true

        val savingThread = Thread(QuickSaveThread(disk, file, ingame, true, isAuto, callback), "TerrarumBasegameGameSaveThread")
        savingThread.start()

        // it is caller's job to keep the game paused or keep a "save in progress" ui up
        // use field 'savingStatus' to know when the saving is done
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

    fun getFileBytes(disk: VirtualDisk, id: Long): ByteArray64 = VDUtil.getAsNormalFile(disk, id).getContent()
    fun getFileReader(disk: VirtualDisk, id: Long): Reader = ByteArray64Reader(getFileBytes(disk, id), Common.CHARSET)

    operator fun invoke(disk: VirtualDisk) {
        TODO()

        val newIngame = TerrarumIngame(App.batch)

        val meta = ReadMeta(disk)

        // NOTE: do NOT set ingame.actorNowPlaying as one read directly from the disk;
        // you'll inevitably read the player actor twice, and they're separate instances of the player!
        val currentWorld = (ReadActor.readActorBare(getFileReader(disk, Terrarum.PLAYER_REF_ID.toLong())) as IngamePlayer).worldCurrentlyPlaying
        val world = ReadWorld(getFileReader(disk, -1-1-1-1-1-1))

        // set lateinit vars on the gameworld FIRST
        world.layerTerrain = BlockLayer(world.width, world.height)
        world.layerWall = BlockLayer(world.width, world.height)

        newIngame.creationTime = meta.creation_t
        newIngame.lastPlayTime = meta.lastplay_t
        newIngame.totalPlayTime = meta.playtime_t
        newIngame.world = world // must be set before the loadscreen, otherwise the loadscreen will try to read from the NullWorld which is already destroyed



        val loadJob = { it: LoadScreenBase ->
            val loadscreen = it as ChunkLoadingLoadScreen

            loadscreen.addMessage(Lang["MENU_IO_LOADING"])

            val actors = world.actors.distinct()//.map { ReadActor(getFileReader(disk, it.toLong())) }
    //        val block = Common.jsoner.fromJson(BlockCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -16)))
            val item = Common.jsoner.fromJson(ItemCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -17)))
    //        val wire = Common.jsoner.fromJson(WireCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -18)))
    //        val material = Common.jsoner.fromJson(MaterialCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -19)))
    //        val faction = Common.jsoner.fromJson(FactionCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -20)))
            val apocryphas = Common.jsoner.fromJson(Apocryphas.javaClass, getUnzipInputStream(getFileBytes(disk, -1024)))



            val worldParam = TerrarumIngame.Codices(disk, meta, item, apocryphas, actors)
            newIngame.gameLoadInfoPayload = worldParam
            newIngame.gameLoadMode = TerrarumIngame.GameLoadMode.LOAD_FROM


            // load all the world blocklayer chunks
            val cw = LandUtil.CHUNK_W
            val ch = LandUtil.CHUNK_H
            val chunkCount = world.width * world.height / (cw * ch)
            val worldLayer = arrayOf(world.getLayer(0), world.getLayer(1))
            for (chunk in 0L until (world.width * world.height) / (cw * ch)) {
                for (layer in worldLayer.indices) {
                    loadscreen.addMessage("${Lang["MENU_IO_LOADING"]}  ${chunk*worldLayer.size+layer+1}/${chunkCount*2}")

                    val chunkFile = VDUtil.getAsNormalFile(disk, 0x1_0000_0000L or layer.toLong().shl(24) or chunk)
                    val chunkXY = LandUtil.chunkNumToChunkXY(world, chunk.toInt())

                    ReadWorld.decodeChunkToLayer(chunkFile.getContent(), worldLayer[layer]!!, chunkXY.x, chunkXY.y)
                }
            }


            loadscreen.addMessage("Updating Block Mappings...")
            world.renumberTilesAfterLoad()


            Echo("${ccW}Savegame loaded from $ccY${disk.getDiskNameString(Common.CHARSET)}")
            printdbg(this, "Savegame loaded from ${disk.getDiskNameString(Common.CHARSET)}")
        }

        val loadScreen = ChunkLoadingLoadScreen(newIngame, world.width, world.height, loadJob)
        Terrarum.setCurrentIngameInstance(newIngame)
        App.setLoadScreen(loadScreen)


    }

}