package net.torvald.terrarum.modulebasegame.serialise

import net.torvald.terrarum.*
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameworld.BlockLayerI16
import net.torvald.terrarum.gameworld.BlockLayerI16F16
import net.torvald.terrarum.gameworld.BlockLayerOresI16I8
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.FancyWorldReadLoadScreen
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.serialise.Common
import java.io.Reader
import java.util.logging.Level
import kotlin.experimental.or

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
    fun getFileReader(disk: SimpleFileSystem, id: Long): Reader =
        ByteArray64Reader(getFileBytes(disk, id), Common.CHARSET)

    operator fun invoke(diskPair: DiskPair) = invoke(diskPair.player, diskPair.world)

    private val getGenver = Regex("""(?<="genver" ?: ?)[0-9]+""")

    /**
     * @param playerDisk DiskSkimmer representing the Player.
     * @param worldDisk0 DiskSkimmer representing the World to be loaded.
     *     If unset, last played world for the Player will be loaded.
     */
    operator fun invoke(playerDisk: DiskSkimmer, worldDisk0: DiskSkimmer? = null) {
        val newIngame = TerrarumIngame(App.batch)
        playerDisk.rebuild()

        val playerDiskSavegameInfo =
            ByteArray64Reader(playerDisk.getFile(VDFileID.SAVEGAMEINFO)!!.bytes, Common.CHARSET)

        val player = ReadActor.invoke(playerDisk, playerDiskSavegameInfo) as IngamePlayer

        App.printdbg(this, "Player localhash: ${player.localHashStr}, hasSprite: ${player.sprite != null}")

        val currentWorldId = player.worldCurrentlyPlaying
        val worldDisk = worldDisk0 ?: App.savegameWorlds[currentWorldId]!!.loadable()
        worldDisk.rebuild()
        val worldDiskSavegameInfo = ByteArray64Reader(worldDisk.getFile(VDFileID.SAVEGAMEINFO)!!.bytes, Common.CHARSET)
        val world = ReadWorld(worldDiskSavegameInfo, worldDisk.diskFile)


        world.layerTerrain = BlockLayerI16(world.width, world.height)
        world.layerWall = BlockLayerI16(world.width, world.height)
        world.layerOres = BlockLayerOresI16I8(world.width, world.height)
        world.layerFluids = BlockLayerI16F16(world.width, world.height)
        world.chunkFlags = Array(world.height / LandUtil.CHUNK_H) { ByteArray(world.width / LandUtil.CHUNK_W) }

        newIngame.world = world // must be set before the loadscreen, otherwise the loadscreen will try to read from the NullWorld which is already destroyed
        newIngame.worldDisk = VDUtil.readDiskArchive(worldDisk.diskFile, Level.INFO)
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
            val worldParam =
                TerrarumIngame.Codices(newIngame.worldDisk, world, actors, player, worldGenver, playerGenver) {}


            newIngame.gameLoadInfoPayload = worldParam
            newIngame.gameLoadMode = TerrarumIngame.GameLoadMode.LOAD_FROM

            App.printdbg(
                this,
                "World dim: ${world.width}x${world.height}, ${world.width / LandUtil.CHUNK_W}x${world.height / LandUtil.CHUNK_H}"
            )

            // load all the world blocklayer chunks
            val cw = LandUtil.CHUNK_W
            val ch = LandUtil.CHUNK_H
            val chunkCount = world.width.toLong() * world.height / (cw * ch)
            val worldLayer = intArrayOf(GameWorld.TERRAIN, GameWorld.WALL, GameWorld.ORES, GameWorld.FLUID).map { world.getLayer(it) }
            for (chunk in 0L until chunkCount) {
                for (layer in worldLayer.indices) {
                    loadscreen.addMessage(Lang["MENU_IO_LOADING"])

                    newIngame.worldDisk.getFile(Common.layerAndChunkNumToEntryID(layer, chunk))?.let { chunkFile ->
                        val (cx, cy) = LandUtil.chunkNumToChunkXY(world, chunk)

                        ReadWorld.decodeChunkToLayer(chunkFile.getContent(), worldLayer[layer]!!, cx, cy)
                        world.chunkFlags[cy][cx] = world.chunkFlags[cy][cx] or GameWorld.CHUNK_LOADED
                    }
                }
                loadscreen.progress.getAndAdd(1)
            }

            loadscreen.addMessage(Lang["MENU_IO_LOAD_UPDATING_BLOCK_MAPPINGS"])
            world.renumberTilesAfterLoad()


            Echo("${ccW}World loaded: $ccY${newIngame.worldDisk.getDiskName(Common.CHARSET)}")
            App.printdbg(this, "World loaded: ${newIngame.worldDisk.getDiskName(Common.CHARSET)}")
        }

        val loadScreen = FancyWorldReadLoadScreen(newIngame, world.width, world.height, loadJob)
        Terrarum.setCurrentIngameInstance(newIngame)
        App.setLoadScreen(loadScreen)
    }

}