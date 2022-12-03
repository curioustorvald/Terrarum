package net.torvald.terrarum.modulebasegame.serialise

import com.badlogic.gdx.graphics.Pixmap
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ChunkLoadingLoadScreen
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.worlddrawer.WorldCamera
import java.io.File
import java.io.Reader
import java.util.logging.Level

/**
 * It's your responsibility to create a new VirtualDisk if your save is new, and create a backup for modifying existing save.
 *
 * Created by minjaesong on 2021-09-03.
 */
object WriteSavegame {

    enum class SaveMode {
        META, PLAYER, WORLD, SHARED, QUICK_WORLD
    }

    @Volatile var savingStatus = -1 // -1: not started, 0: saving in progress, 255: saving finished
    @Volatile var saveProgress = 0f
    @Volatile var saveProgressMax = 1f

    private fun getSaveThread(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, hasThumbnail: Boolean, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) = when (mode) {
        SaveMode.WORLD -> WorldSavingThread(time_t, disk, outFile, ingame, hasThumbnail, isAuto, callback, errorHandler)
        SaveMode.PLAYER -> PlayerSavingThread(time_t, disk, outFile, ingame, hasThumbnail, isAuto, callback, errorHandler)
        SaveMode.QUICK_WORLD -> QuickSingleplayerWorldSavingThread(time_t, disk, outFile, ingame, hasThumbnail, isAuto, callback, errorHandler)
        else -> throw IllegalArgumentException("$mode")
    }

    operator fun invoke(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) {
        savingStatus = 0
        val hasThumbnail = (mode == SaveMode.WORLD || mode == SaveMode.QUICK_WORLD)
        printdbg(this, "Save queued")

        if (hasThumbnail) {
            IngameRenderer.screencapExportCallback = { fb ->
                printdbg(this, "Generating thumbnail...")

                val w = 960
                val h = 640

                val cx = /*1-*/(WorldCamera.x % 2)
                val cy = /*1-*/(WorldCamera.y % 2)

                val x = (fb.width - w) / 2 - cx // force the even-numbered position
                val y = (fb.height - h) / 2 - cy // force the even-numbered position

                val p = Pixmap.createFromFrameBuffer(x, y, w, h)
                IngameRenderer.fboRGBexport = p
                //PixmapIO2._writeTGA(gzout, p, true, true)
                //p.dispose()
                IngameRenderer.fboRGBexportedLatch = true

                printdbg(this, "Done thumbnail generation")
            }
            IngameRenderer.screencapRequested = true
        }

        val savingThread = Thread(getSaveThread(time_t, mode, disk, outFile, ingame, hasThumbnail, isAuto, errorHandler, callback), "TerrarumBasegameGameSaveThread")
        savingThread.start()

        // it is caller's job to keep the game paused or keep a "save in progress" ui up
        // use callback to fire the after-the-saving-progress job
    }


    fun immediate(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) {

        savingStatus = 0

        printdbg(this, "Immediate save fired")

        val savingThread = Thread(getSaveThread(time_t, mode, disk, outFile, ingame, false, isAuto, errorHandler, callback), "TerrarumBasegameGameSaveThread")
        savingThread.start()

        // it is caller's job to keep the game paused or keep a "save in progress" ui up
        // use callback to fire the after-the-saving-progress job
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

    fun getSavegameNickname(worldDisk: SimpleFileSystem) = worldDisk.getDiskName(Common.CHARSET)
    fun getWorldSavefileName(nick: String, world: GameWorld) = "$nick-${world.worldIndex}"
    fun getPlayerSavefileName(player: IngamePlayer) = (player.actorValue.getAsString(AVKey.NAME) ?: "Player") + "-${player.uuid}"

    fun getFileBytes(disk: SimpleFileSystem, id: Long): ByteArray64 = disk.getFile(id)!!.bytes
    fun getFileReader(disk: SimpleFileSystem, id: Long): Reader = ByteArray64Reader(getFileBytes(disk, id), Common.CHARSET)

    /**
     * @param playerDisk DiskSkimmer representing the Player.
     * @param worldDisk0 DiskSkimmer representing the World to be loaded.
     *     If unset, last played world for the Player will be loaded.
     */
    operator fun invoke(playerDisk: DiskSkimmer, worldDisk0: DiskSkimmer? = null) {
        val newIngame = TerrarumIngame(App.batch)
        val player = ReadActor.invoke(playerDisk, ByteArray64Reader(playerDisk.getFile(SAVEGAMEINFO)!!.bytes, Common.CHARSET)) as IngamePlayer

        printdbg(this, "Player localhash: ${player.localHashStr}, hasSprite: ${player.sprite != null}")

        val currentWorldId = player.worldCurrentlyPlaying
        val worldDisk = worldDisk0 ?: App.savegameWorlds[currentWorldId]!!
        val world = ReadWorld(ByteArray64Reader(worldDisk.getFile(SAVEGAMEINFO)!!.bytes, Common.CHARSET), worldDisk.diskFile)

        world.layerTerrain = BlockLayer(world.width, world.height)
        world.layerWall = BlockLayer(world.width, world.height)

        newIngame.world = world // must be set before the loadscreen, otherwise the loadscreen will try to read from the NullWorld which is already destroyed
        newIngame.worldDisk =  VDUtil.readDiskArchive(worldDisk.diskFile, Level.INFO)
        newIngame.playerDisk = VDUtil.readDiskArchive(playerDisk.diskFile, Level.INFO)
        newIngame.savegameNickname = getSavegameNickname(worldDisk)
        newIngame.worldSavefileName = getWorldSavefileName(newIngame.savegameNickname, world)
        newIngame.playerSavefileName = getPlayerSavefileName(player)

//        worldDisk.dispose()
//        playerDisk.dispose()

        val loadJob = { it: LoadScreenBase ->
            val loadscreen = it as ChunkLoadingLoadScreen
            loadscreen.addMessage(Lang["MENU_IO_LOADING"])


            val actors = world.actors.distinct()
            val worldParam = TerrarumIngame.Codices(newIngame.worldDisk, world, actors, player)


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

                    val chunkFile = newIngame.worldDisk.getFile(0x1_0000_0000L or layer.toLong().shl(24) or chunk)!!
                    val chunkXY = LandUtil.chunkNumToChunkXY(world, chunk.toInt())

                    ReadWorld.decodeChunkToLayer(chunkFile.getContent(), worldLayer[layer]!!, chunkXY.x, chunkXY.y)
                }
            }

            loadscreen.addMessage("Updating Block Mappings...")
            world.renumberTilesAfterLoad()


            Echo("${ccW}World loaded: $ccY${newIngame.worldDisk.getDiskName(Common.CHARSET)}")
            printdbg(this, "World loaded: ${newIngame.worldDisk.getDiskName(Common.CHARSET)}")
        }

        val loadScreen = ChunkLoadingLoadScreen(newIngame, world.width, world.height, loadJob)
        Terrarum.setCurrentIngameInstance(newIngame)
        App.setLoadScreen(loadScreen)
    }

}