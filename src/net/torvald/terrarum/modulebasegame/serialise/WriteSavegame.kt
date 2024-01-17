package net.torvald.terrarum.modulebasegame.serialise

import com.badlogic.gdx.graphics.Pixmap
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameworld.BlockLayerI16
import net.torvald.terrarum.gameworld.BlockLayerI16F16
import net.torvald.terrarum.gameworld.BlockLayerI16I8
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.GameWorld.Companion.CHUNK_LOADED
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.FancyWorldReadLoadScreen
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
import kotlin.experimental.or

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

    private fun getSaveThread(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) = when (mode) {
        SaveMode.WORLD -> WorldSavingThread(time_t, disk, outFile, ingame, isAuto, callback, errorHandler)
        SaveMode.PLAYER -> PlayerSavingThread(time_t, disk, outFile, ingame, isAuto, callback, errorHandler)
        SaveMode.QUICK_WORLD -> QuickSingleplayerWorldSavingThread(time_t, disk, outFile, ingame, isAuto, callback, errorHandler)
        else -> throw IllegalArgumentException("$mode")
    }

    private fun installScreencap() {
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

            printdbg(this, "Done thumbnail generation")
        }
    }

    operator fun invoke(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) {
        savingStatus = 0
        printdbg(this, "Save queued")

        installScreencap()
        try { printdbg(this, "ScreencapExport installed: ${IngameRenderer.screencapExportCallback}") }
        catch (e: UninitializedPropertyAccessException) { printdbg(this, "ScreencapExport installed: no") }
        IngameRenderer.requestScreencap()

        val savingThread = Thread(getSaveThread(time_t, mode, disk, outFile, ingame, isAuto, errorHandler, callback), "TerrarumBasegameGameSaveThread")
        savingThread.start()

        // it is caller's job to keep the game paused or keep a "save in progress" ui up
        // use callback to fire the after-the-saving-progress job
    }


    fun immediate(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) {
        savingStatus = 0
        printdbg(this, "Immediate save fired")

        installScreencap()
        try { printdbg(this, "ScreencapExport installed: ${IngameRenderer.screencapExportCallback}") }
        catch (e: UninitializedPropertyAccessException) { printdbg(this, "ScreencapExport installed: no") }
        IngameRenderer.requestScreencap()

        val savingThread = Thread(getSaveThread(time_t, mode, disk, outFile, ingame, isAuto, errorHandler, callback), "TerrarumBasegameGameSaveThread")
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

    fun getWorldName(worldDisk: SimpleFileSystem) = worldDisk.getDiskName(Common.CHARSET)
    fun getWorldSavefileName(world: GameWorld) = "${world.worldIndex}"
    fun getPlayerSavefileName(player: IngamePlayer) = "${player.uuid}"

    fun getFileBytes(disk: SimpleFileSystem, id: Long): ByteArray64 = disk.getFile(id)!!.bytes
    fun getFileReader(disk: SimpleFileSystem, id: Long): Reader = ByteArray64Reader(getFileBytes(disk, id), Common.CHARSET)

    operator fun invoke(diskPair: DiskPair) = invoke(diskPair.player, diskPair.world)

    private val getGenver = Regex("""(?<="genver":)[0-9]+""")

    /**
     * @param playerDisk DiskSkimmer representing the Player.
     * @param worldDisk0 DiskSkimmer representing the World to be loaded.
     *     If unset, last played world for the Player will be loaded.
     */
    operator fun invoke(playerDisk: DiskSkimmer, worldDisk0: DiskSkimmer? = null) {
        val newIngame = TerrarumIngame(App.batch)
        playerDisk.rebuild()

        val playerDiskSavegameInfo = ByteArray64Reader(playerDisk.getFile(SAVEGAMEINFO)!!.bytes, Common.CHARSET)

        val player = ReadActor.invoke(playerDisk, playerDiskSavegameInfo) as IngamePlayer

        printdbg(this, "Player localhash: ${player.localHashStr}, hasSprite: ${player.sprite != null}")

        val currentWorldId = player.worldCurrentlyPlaying
        val worldDisk = worldDisk0 ?: App.savegameWorlds[currentWorldId]!!.loadable()
        worldDisk.rebuild()
        val worldDiskSavegameInfo = ByteArray64Reader(worldDisk.getFile(SAVEGAMEINFO)!!.bytes, Common.CHARSET)
        val world = ReadWorld(worldDiskSavegameInfo, worldDisk.diskFile)


        world.layerTerrain = BlockLayerI16(world.width, world.height)
        world.layerWall = BlockLayerI16(world.width, world.height)
        world.layerOres = BlockLayerI16I8(world.width, world.height)
        world.layerFluids = BlockLayerI16F16(world.width, world.height)
        world.chunkFlags = Array(world.height / LandUtil.CHUNK_H) { ByteArray(world.width / LandUtil.CHUNK_W) }

        newIngame.world = world // must be set before the loadscreen, otherwise the loadscreen will try to read from the NullWorld which is already destroyed
        newIngame.worldDisk =  VDUtil.readDiskArchive(worldDisk.diskFile, Level.INFO)
        newIngame.playerDisk = VDUtil.readDiskArchive(playerDisk.diskFile, Level.INFO)
        newIngame.worldName = getWorldName(worldDisk)
        newIngame.worldSavefileName = getWorldSavefileName(world)
        newIngame.playerSavefileName = getPlayerSavefileName(player)

//        worldDisk.dispose()
//        playerDisk.dispose()


        val loadJob = { it: LoadScreenBase ->
            val loadscreen = it as FancyWorldReadLoadScreen
            loadscreen.addMessage(Lang["MENU_IO_LOADING"])


            val worldGenver = CharArray(128).let {
                worldDiskSavegameInfo.read(it, 0, 128)
                getGenver.find(String(it))?.value?.toLong()!!
            }
            val playerGenver = CharArray(128).let {
                playerDiskSavegameInfo.read(it, 0, 128)
                getGenver.find(String(it))?.value?.toLong()!!
            }


            val actors = world.actors.distinct()
            val worldParam = TerrarumIngame.Codices(newIngame.worldDisk, world, actors, player, worldGenver, playerGenver)


            newIngame.gameLoadInfoPayload = worldParam
            newIngame.gameLoadMode = TerrarumIngame.GameLoadMode.LOAD_FROM

            printdbg(this, "World dim: ${world.width}x${world.height}, ${world.width / LandUtil.CHUNK_W}x${world.height / LandUtil.CHUNK_H}")

            // load all the world blocklayer chunks
            val cw = LandUtil.CHUNK_W
            val ch = LandUtil.CHUNK_H
            val chunkCount = world.width * world.height / (cw * ch)
            val hasOreLayer = (newIngame.worldDisk.getFile(0x1_0000_0000L or 2L.shl(24)) != null)
            val worldLayer = (if (hasOreLayer) intArrayOf(0,1,2) else intArrayOf(0,1)).map { world.getLayer(it) }
            val layerCount = worldLayer.size
            for (chunk in 0L until (world.width * world.height) / (cw * ch)) {
                for (layer in worldLayer.indices) {
                    loadscreen.addMessage(Lang["MENU_IO_LOADING"])

                    val chunkFile = newIngame.worldDisk.getFile(0x1_0000_0000L or layer.toLong().shl(24) or chunk)!!
                    val (cx, cy) = LandUtil.chunkNumToChunkXY(world, chunk.toInt())

                    ReadWorld.decodeChunkToLayer(chunkFile.getContent(), worldLayer[layer]!!, cx, cy)

                    world.chunkFlags[cy][cx] = world.chunkFlags[cy][cx] or CHUNK_LOADED
                }
                loadscreen.progress.getAndAdd(1)
            }

            loadscreen.addMessage(Lang["MENU_IO_LOAD_UPDATING_BLOCK_MAPPINGS"])
            world.renumberTilesAfterLoad()


            Echo("${ccW}World loaded: $ccY${newIngame.worldDisk.getDiskName(Common.CHARSET)}")
            printdbg(this, "World loaded: ${newIngame.worldDisk.getDiskName(Common.CHARSET)}")
        }

        val loadScreen = FancyWorldReadLoadScreen(newIngame, world.width, world.height, loadJob)
        Terrarum.setCurrentIngameInstance(newIngame)
        App.setLoadScreen(loadScreen)
    }

}