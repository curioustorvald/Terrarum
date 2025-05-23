package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.Terrarum.getPlayerSaveFiledesc
import net.torvald.terrarum.Terrarum.getWorldSaveFiledesc
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.BlockPropUtil
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.blockstats.TileSurvey
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.console.AVTracker
import net.torvald.terrarum.console.ActorsList
import net.torvald.terrarum.console.Authenticator
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gamecontroller.IngameController
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.gameitems.FixtureInteractionBlocked
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.gameparticles.ParticleBase
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.*
import net.torvald.terrarum.modulebasegame.gameactors.physicssolver.CollisionSolver
import net.torvald.terrarum.modulebasegame.gameitems.AxeCore
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore
import net.torvald.terrarum.modulebasegame.gameworld.GameEconomy
import net.torvald.terrarum.modulebasegame.serialise.LoadSavegame
import net.torvald.terrarum.modulebasegame.serialise.ReadActor
import net.torvald.terrarum.modulebasegame.serialise.WriteSavegame
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.gradEndCol
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen
import net.torvald.terrarum.modulebasegame.worldgenerator.WorldgenParams
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.savegame.VDUtil
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.toIntAndFrac
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.Toolkit.hdrawWidth
import net.torvald.terrarum.ui.UIAutosaveNotifier
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.unicode.EMDASH
import net.torvald.util.CircularArray
import org.khelekore.prtree.PRTree
import java.io.File
import java.util.*
import java.util.logging.Level
import kotlin.experimental.and
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt


/**
 * Ingame instance for the game Terrarum.
 *
 * Created by minjaesong on 2017-06-16.
 */

open class TerrarumIngame(batch: FlippingSpriteBatch) : IngameInstance(batch) {

    var historicalFigureIDBucket: ArrayList<Int> = ArrayList<Int>()


    /**
     * list of Actors that is sorted by Actors' referenceID
     */
    //val ACTORCONTAINER_INITIAL_SIZE = 64
    val PARTICLES_MAX = App.getConfigInt("maxparticles")
    val particlesContainer = CircularArray<ParticleBase>(PARTICLES_MAX, true)

    // these are required because actors always change their position
    private var visibleActorsRenderFarBehind: ArrayList<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderBehind: ArrayList<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderMiddle: ArrayList<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderMidTop: ArrayList<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderFront: ArrayList<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderOverlay: ArrayList<ActorWithBody> = ArrayList(1)

    //var screenZoom = 1.0f   // definition moved to IngameInstance
    //val ZOOM_MAXIMUM = 4.0f // definition moved to IngameInstance
    //val ZOOM_MINIMUM = 0.5f // definition moved to IngameInstance

    companion object {
        /** Sets camera position so that (0,0) would be top-left of the screen, (width, height) be bottom-right. */
        fun setCameraPosition(batch: SpriteBatch, camera: OrthographicCamera, newX: Float, newY: Float) {
            camera.position.set((-newX + App.scr.halfw).roundToFloat(), (-newY + App.scr.halfh).roundToFloat(), 0f)
            camera.update()
            batch.projectionMatrix = camera.combined
        }
        fun setCameraPosition(batch: SpriteBatch, shape: ShapeRenderer, camera: OrthographicCamera, newX: Float, newY: Float) {
            camera.setToOrtho(true, App.scr.wf, App.scr.hf)
            camera.update()
            camera.position.set((-newX + App.scr.halfw).roundToFloat(), (-newY + App.scr.halfh).roundToFloat(), 0f)
            camera.update()
            shape.projectionMatrix = camera.combined

            camera.setToOrtho(true, App.scr.wf, App.scr.hf)
            camera.update()
            camera.position.set((-newX + App.scr.halfw).roundToFloat(), (-newY + App.scr.halfh).roundToFloat(), 0f)
            camera.update()
            batch.projectionMatrix = camera.combined
        }
        fun getCanonicalTitle() = App.GAME_NAME +
                                  " $EMDASH F: ${Gdx.graphics.framesPerSecond}" +
                                  if (App.IS_DEVELOPMENT_BUILD)
                                      " (ΔF${Terrarum.updateRateStr})" +
                                      " $EMDASH M: H${Terrarum.memJavaHeap}M / X${Terrarum.memXmx}M / U${Terrarum.memUnsafe}M"
                                  else
                                      ""

        val ACTOR_UPDATE_RANGE = 4096

        fun distToActorSqr(world: GameWorld, a: ActorWithBody, p: ActorWithBody) =
                min(// take min of normal position and wrapped (x < 0) position
                        min((a.hitbox.centeredX - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                        ((a.hitbox.centeredX + world.width * TILE_SIZE) - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr()),
                        ((a.hitbox.centeredX - world.width * TILE_SIZE) - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr()
                )
        fun distToCameraSqr(world: GameWorld, a: ActorWithBody) =
                min(
                        min((a.hitbox.centeredX - WorldCamera.xCentre).sqr() +
                        (a.hitbox.centeredY - WorldCamera.yCentre).sqr(),
                        ((a.hitbox.centeredX + world.width * TILE_SIZE) - WorldCamera.xCentre).sqr() +
                        (a.hitbox.centeredY - WorldCamera.yCentre).sqr()),
                        ((a.hitbox.centeredX - world.width * TILE_SIZE) - WorldCamera.xCentre).sqr() +
                        (a.hitbox.centeredY - WorldCamera.yCentre).sqr()
                )

        /** whether the actor is within update range */
        fun ActorWithBody.inUpdateRange(world: GameWorld) = distToCameraSqr(world, this) <= ACTOR_UPDATE_RANGE.sqr()

        /** whether the actor is within screen */
        fun ActorWithBody.inScreen(world: GameWorld) =

                // y
                this.hitbox.endY >= WorldCamera.y && this.hitbox.startY <= WorldCamera.yEnd

                &&

                // x: camera is on the right side of the seam
                ((this.hitbox.endX - world.width >= WorldCamera.x && this.hitbox.startX - world.width <= WorldCamera.xEnd) ||
                 // x: camera in on the left side of the seam
                 (this.hitbox.endX + world.width >= WorldCamera.x && this.hitbox.startX + world.width <= WorldCamera.xEnd) ||
                 // x: neither
                 (this.hitbox.endX >= WorldCamera.x && this.hitbox.startX <= WorldCamera.xEnd))

        val SIZE_SMALL = Point2i(CHUNK_W*66, CHUNK_H*60)
        val SIZE_NORMAL = Point2i(CHUNK_W*100, CHUNK_H*60)
        val SIZE_LARGE = Point2i(CHUNK_W*150, CHUNK_H*60)
        val SIZE_HUGE = Point2i(CHUNK_W*250, CHUNK_H*60)
        val SIZE_TEST = Point2i(CHUNK_W*11, CHUNK_H*7)
        val NEW_WORLD_SIZE =
            if (App.IS_DEVELOPMENT_BUILD)
                arrayOf(SIZE_SMALL, SIZE_NORMAL, SIZE_LARGE, SIZE_HUGE, SIZE_TEST)
            else
                arrayOf(SIZE_SMALL, SIZE_NORMAL, SIZE_LARGE, SIZE_HUGE)
        val WORLDPORTAL_NEW_WORLD_SIZE =
            if (App.IS_DEVELOPMENT_BUILD)
                arrayOf(SIZE_SMALL, SIZE_NORMAL, SIZE_LARGE, SIZE_HUGE, SIZE_TEST)
            else
                arrayOf(SIZE_SMALL, SIZE_NORMAL, SIZE_LARGE, SIZE_HUGE)

        val worldgenThreadExecutor = ThreadExecutor()
    }



    init {
        particlesContainer.overwritingPolicy = {
            it.dispose()
        }

        gameUpdateGovernor = ConsistentUpdateRate
    }


    lateinit var uiBlur: UIFakeBlurOverlay
    lateinit var uiPieMenu: UIQuickslotPie
    lateinit var uiQuickBar: UIQuickslotBar
    lateinit var uiInventoryPlayer: UIInventoryFull

    internal val uiInventoryPlayerReady: Boolean get() = ::uiInventoryPlayer.isInitialized // this is some ugly hack but I didn't want to make uiInventoryPlayer nullable so bite me

    /**
     * This is a dedicated property for the fixtures' UI.
     *
     * When it's not null, the UI will be updated and rendered;
     * when the UI is closed, it'll be replaced with a null value.
     *
     * This will not allow multiple fixture UIs from popping up (does not prevent them actually being open)
     * because UI updating and rendering is whitelist-operated
     */
    private var uiFixture: UICanvas? = null
        set(value) {
            printdbg(this, "uiFixture change: $uiFixture -> $value")
            field?.setAsClose()
            value?.let { uiFixturesHistory.add(it) }
            field = value
        }

    var wearableDeviceUI: UICanvas? = null
        set(value) {
            field?.setAsClose()
            value?.setAsOpen()
            value?.setPosition(App.scr.tvSafeGraphicsWidth/2, App.scr.tvSafeActionHeight/2)
            field = value
        }

    val getUIFixture = object : Id_UICanvasNullable { // quick workaround for the type erasure (you can't use lambda...)
        override fun get(): UICanvas? {
            return uiFixture
        }
    }
    val getWearableDeviceUI = object : Id_UICanvasNullable { // quick workaround for the type erasure (you can't use lambda...)
        override fun get(): UICanvas? {
            return wearableDeviceUI
        }
    }

    lateinit var uiVitalPrimary: UICanvas
    lateinit var uiVitalSecondary: UICanvas
    lateinit var uiVitalItem: UICanvas // itemcount/durability of held block or active ammo of held gun. As for the block, max value is 500.

    private val uiFixturesHistory = HashSet<UICanvas>()

    private lateinit var uiBasicInfo: UICanvas
    private lateinit var uiWatchTierOne: UICanvas
    lateinit var uiAutosaveNotifier: UIAutosaveNotifier
    lateinit var uiCheatDetected: UICheatDetected


    var particlesActive = 0
        private set

    var selectedWireRenderClass = ""
    private var oldSelectedWireRenderClass = ""



    private lateinit var ingameUpdateThread: ThreadIngameUpdate
    private lateinit var updateThreadWrapper: Thread
    //private val ingameDrawThread: ThreadIngameDraw // draw must be on the main thread


    override var gameInitialised = false
        internal set
    override var gameFullyLoaded = false
        internal set

    override val musicStreamer = TerrarumMusicAndAmbientStreamer()


    //////////////
    // GDX code //
    //////////////


    lateinit var gameLoadMode: GameLoadMode
    lateinit var gameLoadInfoPayload: Any


    enum class GameLoadMode {
        CREATE_NEW, LOAD_FROM
    }

    private val soundReflectiveMaterials = hashSetOf(
        ""
    )

    override fun show() {
        //initViewPort(AppLoader.terrarumAppConfig.screenW, AppLoader.terrarumAppConfig.screenH)

        // gameLoadMode and gameLoadInfoPayload must be set beforehand!!

        when (gameLoadMode) {
            GameLoadMode.CREATE_NEW -> enterCreateNewWorld(gameLoadInfoPayload as NewGameParams)
            GameLoadMode.LOAD_FROM  -> enterLoadFromSave(gameLoadInfoPayload as Codices)
        }

        IngameRenderer.setRenderedWorld(world)
        blockMarkingActor.isVisible = true

        App.audioMixer.reset()

        super.show() // this function sets gameInitialised = true

        TileSurvey.submitProposal(
            TileSurvey.SurveyProposal(
                "basegame.Ingame.audioReflection", 73, 73, 2, 4
            ) { world, x, y ->
                val tileProp = BlockCodex[world.getTileFromTerrain(x, y)]
                val wallProp = BlockCodex[world.getTileFromWall(x, y)]
                val prop = if (tileProp.isSolid && !tileProp.isActorBlock) tileProp else wallProp
                MaterialCodex[prop.material].sondrefl
            }
        )

        TileSurvey.submitProposal(
            TileSurvey.SurveyProposal(
                "basegame.Ingame.openness", 73, 73, 2, 4
            ) { world, x, y ->
                val tileProp = BlockCodex[world.getTileFromTerrain(x, y)]
                val wallProp = BlockCodex[world.getTileFromWall(x, y)]
                (!tileProp.isSolid && !wallProp.isSolid).toInt().toFloat()
            }
        )

        loadedTime_t = App.getTIME_T()

        MusicService.enterScene("ingame")
    }

    data class NewGameParams(
        val player: IngamePlayer,
        val newWorldParams: NewWorldParameters,
        val callbackAfterLoad: (TerrarumIngame) -> Unit

    )

    data class NewWorldParameters(
        val width: Int,
        val height: Int,
        val worldGenSeed: Long,
        val savegameName: String
        // other worldgen options
    ) {
        init {
            if (width % LandUtil.CHUNK_W != 0 || height % LandUtil.CHUNK_H != 0) {
                throw IllegalArgumentException("World size is not a multiple of chunk size; World size: ($width, $height), Chunk size: (${LandUtil.CHUNK_W}, ${LandUtil.CHUNK_H})")
            }
        }
    }

    data class Codices(
        val disk: VirtualDisk, // WORLD disk
        val world: GameWorld,
//        val meta: WriteMeta.WorldMeta,
//        val block: BlockCodex,
//        val item: ItemCodex,
//        val wire: WireCodex,
//        val material: MaterialCodex,
//        val faction: FactionCodex,
//        val apocryphas: Map<String, Any>,
        val actors: List<ActorID>,
        val player: IngamePlayer,
        val worldGenver: Long,
        val playerGenver: Long,
        val callbackAfterLoad: (TerrarumIngame) -> Unit
    )

    private var loadCallback: ((TerrarumIngame) -> Unit)? = null

    /**
     * Init instance by loading saved world
     */
    private fun enterLoadFromSave(codices: Codices) {
        if (gameInitialised) {
            printdbg(this, "loaded successfully.")
        }
        else {
            printdbg(this, "Ingame setting things up from the savegame")

            RoguelikeRandomiser.loadFromSave(this, codices.world.randSeeds[0], codices.world.randSeeds[1])
            WeatherMixer.loadFromSave(this, codices.world.randSeeds[2], codices.world.randSeeds[3])

//            Terrarum.itemCodex.loadFromSave(codices.item)
//            Terrarum.apocryphas = HashMap(codices.apocryphas)


            // feed info to the worldgen
            Worldgen.attachMap(world, WorldgenParams.getParamsByVersion(codices.worldGenver, world.generatorSeed))
        }

        loadCallback = codices.callbackAfterLoad
        worldGenVer = codices.worldGenver
    }

    /** Load rest of the game with GL context */
    private fun postInitForLoadFromSave(codices: Codices) {
        codices.actors.forEach {
            try {
                val actor = ReadActor(codices.disk, LoadSavegame.getFileReader(codices.disk, it.toLong()))
                if (actor !is IngamePlayer) { // actor list should not contain IngamePlayers (see WriteWorld.preWrite) but just in case...
                    forceAddActor(actor)
                }
            }
            catch (e: NullPointerException) {
                System.err.println("Could not read the actor ${it} from the disk")
                e.printStackTrace()
            }
        }

        printdbg(this, "Player localhash: ${codices.player.localHashStr}, hasSprite: ${codices.player.sprite != null}")

        // assign new random referenceID for player
        codices.player.referenceID = Terrarum.generateUniqueReferenceID()
        forceAddActor(codices.player)


        // overwrite player's props with world's for multiplayer
        // see comments on IngamePlayer.unauthorisedPlayerProps to know why this is necessary.
        codices.player.backupPlayerProps(isMultiplayer) // backup first!
        printdbg(this, "postInitForLoadFromSave")
        printdbg(this, "Player UUID: ${codices.player.uuid}")
        printdbg(this, world.playersLastStatus.keys)
        printdbg(this, world.playersLastStatus[codices.player.uuid])
        world.playersLastStatus[codices.player.uuid].let { // regardless of the null-ness, we still keep the backup, which WriteActor looks for it
            // if the world has some saved values, use them
            if (it != null) {

                printdbg(this, "Found LastStatus mapping for Player ${codices.player.uuid}")
                printdbg(this, "Changing XY Position ${codices.player.hitbox.canonVec} -> ${it.physics.position}")

                codices.player.setPosition(it.physics.position)
                if (isMultiplayer) {
                    printdbg(this, "Using world's ActorValue instead of player's")
                    codices.player.actorValue = it.actorValue!!
                    printdbg(this, "Using world's Inventory instead of player's")
                    codices.player.inventory = it.inventory!!
                }
            }
            // if not, move player to the spawn point
            else {
                printdbg(this, "No mapping found")
                printdbg(this, "Changing XY Position ${codices.player.hitbox.canonVec} -> (${world.spawnX * TILE_SIZED}, ${world.spawnY * TILE_SIZED})")

                codices.player.setPosition((world.portalPoint ?: world.spawnPoint).toVector() * TILE_SIZED)
            }
        }


        // try to unstuck the repositioned player
        codices.player.tryUnstuck()



        // by doing this, whatever the "possession" the player had will be broken by the game load
        actorNowPlaying = codices.player
        actorGamer = codices.player

        SavegameMigrator.invoke(codices.worldGenver, codices.playerGenver, actorContainerActive)

        printdbg(this, "postInitForLoadFromSave exit")
    }

    private val autosaveOnErrorAction = { e: Throwable -> uiAutosaveNotifier.setAsError() }


    private fun postInitForNewGame() {
        worldSavefileName = LoadSavegame.getWorldSavefileName(world)
        playerSavefileName = LoadSavegame.getPlayerSavefileName(actorGamer)

        worldDisk = VDUtil.createNewDisk(
                1L shl 60,
                worldName,
                Common.CHARSET
        )

        playerDisk = VDUtil.readDiskArchive(App.savegamePlayers[actorGamer.uuid]!!.loadable().diskFile, Level.INFO)

        // go to spawn position
        printdbg(this, "World Spawn position: (${world.spawnX}, ${world.spawnY})")
        actorGamer.setPosition(world.spawnPoint.toVector() * TILE_SIZED)
        actorGamer.backupPlayerProps(isMultiplayer)

        // make initial savefile
        // we're not writing multiple files at one go because:
        //  1. lighten the IO burden
        //  2. cannot sync up the "counter" to determine whether both are finished
        uiAutosaveNotifier.setAsOpen()
        val saveTime_t = App.getTIME_T()
        printdbg(this, "Immediate Save")
        WriteSavegame.immediate(saveTime_t, WriteSavegame.SaveMode.PLAYER, playerDisk, getPlayerSaveFiledesc(playerSavefileName), this, true, autosaveOnErrorAction) {
            printdbg(this, "immediate save callback from PLAYER")

            makeSavegameBackupCopy(getPlayerSaveFiledesc(playerSavefileName))

            WriteSavegame.immediate(saveTime_t, WriteSavegame.SaveMode.WORLD, worldDisk, getWorldSaveFiledesc(worldSavefileName), this, true, autosaveOnErrorAction) {
                printdbg(this, "immediate save callback from WORLD")

                makeSavegameBackupCopy(getWorldSaveFiledesc(worldSavefileName)) // don't put it on the postInit() or render(); must be called using callback
                uiAutosaveNotifier.setAsClose()

                App.savegameWorlds[world.worldIndex] = SavegameCollection.collectFromBaseFilename(File(worldsDir), worldSavefileName)
                App.savegameWorldsName[world.worldIndex] = worldName

                loadedTime_t = App.getTIME_T()
            }
        }
    }

    /**
     * Init instance by creating new world
     */
    private fun enterCreateNewWorld(newGameParams: NewGameParams) {

        val player = newGameParams.player
        val worldParams = newGameParams.newWorldParams

        printdbg(this, "Ingame called")
        printStackTrace(this)

        if (gameInitialised) {
            printdbg(this, "loaded successfully.")
        }
        else {
            App.getLoadScreen().addMessage("${App.GAME_NAME} ${App.getVERSION_STRING()}")

            App.getLoadScreen().addMessage("Creating new world")


            // init map as chosen size
            val timeNow = App.getTIME_T()
            world = GameWorld(worldParams.width, worldParams.height, timeNow, timeNow) // new game, so the creation time is right now
            world.generatorSeed = worldParams.worldGenSeed
            //gameworldIndices.add(world.worldIndex)
            world.extraFields["basegame.economy"] = GameEconomy()

            // generate terrain for the map
            //WorldGenerator.attachMap(world)
            //WorldGenerator.SEED = worldParams.worldGenSeed
            //WorldGenerator.generateMap()
            Worldgen.attachMap(world, WorldgenParams.getParamsByVersion(null, worldParams.worldGenSeed))
            Worldgen.generateMap(App.getLoadScreen())


            historicalFigureIDBucket = ArrayList<Int>()

            worldName = worldParams.savegameName


            world.worldCreator = UUID.fromString(player.uuid.toString())

            printdbg(this, "new worldIndex: ${world.worldIndex}")
            printdbg(this, "worldCurrentlyPlaying: ${player.worldCurrentlyPlaying}")

            actorNowPlaying = player
            actorGamer = player
            forceAddActor(player)

            WeatherMixer.internalReset(this)

            UILoadGovernor.worldUUID = world.worldIndex
        }

        KeyToggler.forceSet(Input.Keys.Q, false)

        loadCallback = newGameParams.callbackAfterLoad
        worldGenVer = null
    }

    val ingameController = IngameController(this)

    /** Load rest of the game with GL context */
    fun postInit() {
        actorNowPlaying!! // null check, just in case...


        MegaRainGovernor // invoke MegaRain Governor
        MinimapComposer // invoke MinimapComposer



        // make controls work
        Gdx.input.inputProcessor = ingameController
        if (App.gamepad != null) {
            ingameController.gamepad = App.gamepad
        }

        val drawWidth = Toolkit.drawWidth



        // >- queue up game UIs that should pause the world -<
        uiInventoryPlayer = UIInventoryFull()
        uiInventoryPlayer.setPosition(0, 0)

        // >- lesser UIs -<
        // quick bar
        uiQuickBar = UIQuickslotBar()
        uiQuickBar.isVisible = true
        uiQuickBar.setPosition((drawWidth - uiQuickBar.width) / 2, App.scr.tvSafeGraphicsHeight)

        // pie menu
        uiPieMenu = UIQuickslotPie()
        uiPieMenu.setPosition(hdrawWidth, App.scr.halfh)

        // vital metre
        // fill in getter functions by
        //      (uiAliases[UI_QUICK_BAR]!!.UI as UIVitalMetre).vitalGetterMax = { some_function }
        //uiVitalPrimary = UIVitalMetre(player, { 80f }, { 100f }, Color.red, 2, customPositioning = true)
        //uiVitalPrimary.setAsAlwaysVisible()
        //uiVitalSecondary = UIVitalMetre(player, { 73f }, { 100f }, Color(0x00dfff), 1) customPositioning = true)
        //uiVitalSecondary.setAsAlwaysVisible()
        //uiVitalItem = UIVitalMetre(player, { null }, { null }, Color(0xffcc00), 0, customPositioning = true)
        //uiVitalItem.setAsAlwaysVisible()

        // fake UI for blurring the background
        uiBlur = UIFakeBlurOverlay(1f, true)
        uiBlur.setPosition(0,0)

        uiWatchTierOne = UIWatchLargeAnalogue()
//        uiWatchTierOne = UIWatchLargeDigital()
        uiWatchTierOne.setAsAlwaysVisible()
        uiWatchTierOne.setPosition(
                ((drawWidth - App.scr.tvSafeActionWidth) - (uiQuickBar.posX + uiQuickBar.width) - uiWatchTierOne.width) / 2 + (uiQuickBar.posX + uiQuickBar.width),
                App.scr.tvSafeGraphicsHeight + 8
        )

        // basic watch-style notification bar (temperature, new mail)
        uiBasicInfo = UIBasicInfo()
        uiBasicInfo.setAsAlwaysVisible()
        uiBasicInfo.setPosition((uiQuickBar.posX - uiBasicInfo.width - App.scr.tvSafeActionWidth) / 2 + App.scr.tvSafeActionWidth, uiWatchTierOne.posY)


        uiCheatDetected = UICheatDetected()


        // batch-process uiAliases
        // NOTE: UIs that should pause the game (e.g. Inventory) must have relevant codes ON THEIR SIDE
        uiContainer.add(
            // drawn first
            //uiVitalPrimary,
            //uiVitalSecondary,
            //uiVitalItem,
            noticelet,

            uiBlur,

            uiPieMenu,
            uiQuickBar,
//            uiBasicInfo, // temporarily commenting out: wouldn't make sense for v 0.3 release
            uiWatchTierOne,
            getWearableDeviceUI,
            UIScreenZoom(),
            uiAutosaveNotifier,

            uiInventoryPlayer,
            getUIFixture,
            uiTooltip,
            consoleHandler,
            uiPaused,
            uiCheatDetected,
            // drawn last
        )


        ingameUpdateThread = ThreadIngameUpdate(this)
        updateThreadWrapper = Thread(ingameUpdateThread, "Terrarum UpdateThread")

        // add extra UIs from the other modules
        ModMgr.GameExtraGuiLoader.guis.forEach {
            uiContainer.add(it(this))
        }

        ModMgr.GameWatchdogLoader.watchdogs.forEach {
            registerWatchdog(it.key, it.value)
        }

        // these need to appear on top of any others
        uiContainer.add(notifier)

        App.setDebugTime("Ingame.UpdateCounter", 0)

        // some sketchy test code here

        KeyToggler.forceSet(Input.Keys.F2, false)


        if (loadCallback != null) {
            try {
                loadCallback!!(this)
            }
            catch (e: Throwable) {
                e.printStackTrace()
            }
            loadCallback = null
        }
    }// END enter



    private var worldPrimaryClickLatch = false
    private var worldSecondaryClickLatch = false

    private fun fireFixtureInteractEvent(fixture: FixtureBase, mwx: Double, mwy: Double) {
        if (fixture.mouseUp) {
            fixture.onInteract(mwx, mwy)
        }
    }

    // left click: use held item, attack, pick up fixture if i'm holding a pickaxe or hammer (aka tool), do 'bare hand action' if holding nothing
    override fun worldPrimaryClickStart(actor: ActorWithBody, delta: Float) {
        //println("[Ingame] worldPrimaryClickStart $delta")

        // prepare some variables

        val itemOnGrip = ItemCodex[(actor as Pocketed).inventory.itemEquipped.get(GameItem.EquipPosition.HAND_GRIP)]
        // bring up the UIs of the fixtures (e.g. crafting menu from a crafting table)
        var uiOpened = false
        val fixtureUnderMouse0: List<FixtureBase> = getActorsUnderMouse(Terrarum.mouseX, Terrarum.mouseY).filterIsInstance<FixtureBase>()
        if (fixtureUnderMouse0.size > 1) {
            App.printdbgerr(this, "Multiple fixtures at tile coord ${Terrarum.mouseX / TILE_SIZED}, ${Terrarum.mouseY / TILE_SIZED}: [${fixtureUnderMouse0.map { it.javaClass.simpleName }.joinToString()}]")
        }
        val fixtureUnderMouse = fixtureUnderMouse0.firstOrNull()

        ////////////////////////////////

        mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
            // #1. interact with the fixture if NOT FixtureInteractionBlocked
            // scan for the one with non-null UI.
            // what if there's multiple of such fixtures? whatever, you are supposed to DISALLOW such situation.
            if (fixtureUnderMouse != null && itemOnGrip !is FixtureInteractionBlocked) {
                if (!worldPrimaryClickLatch) {
                    worldPrimaryClickLatch = true
                    fixtureUnderMouse.let { fixture ->
                        fixture.mainUI?.let { ui ->
                            uiOpened = true

                            // property 'uiFixture' is a dedicated property that the TerrarumIngame recognises.
                            // when it's not null, the UI will be updated and rendered
                            // when the UI is closed, it'll be replaced with a null value
                            uiFixture = ui
                            ui.setPosition(
                                (Toolkit.drawWidth - ui.width) / 4,
                                (App.scr.height - ui.height) / 4 // what the fuck?
                            )
                            ui.setAsOpen()
                        }

                        if (!uiOpened) {
                            fireFixtureInteractEvent(fixture, mwx, mwy)
                        }
                    }
                }
            }
            // #2. If no fixture under mouse or FixtureInteractionBlocked, use the item
            else if (itemOnGrip != null) {
                // click filtering (latch stuff) is handled by IngameController (see inventoryCategoryAllowClickAndDrag)
                // To disable click dragging for tool/block/etc., put `override val disallowToolDragging = true` to the item's code
                val consumptionSuccessful = itemOnGrip.startPrimaryUse(actor, delta)
                if (consumptionSuccessful > -1)
                    (actor as Pocketed).inventory.consumeItem(itemOnGrip, consumptionSuccessful)

                worldPrimaryClickLatch = true
            }
            // #3. If not holding any item and can do barehandaction (size big enough that barehandactionminheight check passes), do it
            else {
                performBarehandAction(actor, delta, mwx, mwy, mtx, mty)
            }

            0L
        }
    }

    override fun worldPrimaryClickEnd(actor: ActorWithBody, delta: Float) {
        val canPerformBarehandAction = actor.scale * actor.baseHitboxH >=
                                       (actor.actorValue.getAsDouble(AVKey.BAREHAND_MINHEIGHT) ?: 4294967296.0)

        val itemOnGrip = (actor as Pocketed).inventory.itemEquipped.get(GameItem.EquipPosition.HAND_GRIP)
        ItemCodex[itemOnGrip]?.endPrimaryUse(actor, delta)

        if (canPerformBarehandAction) {
            endPerformBarehandAction(actor)
        }

        worldPrimaryClickLatch = false
    }

    override fun worldSecondaryClickStart(actor: ActorWithBody, delta: Float) {
        val itemOnGrip = ItemCodex[(actor as Pocketed).inventory.itemEquipped.get(GameItem.EquipPosition.HAND_GRIP)]

        if (!worldSecondaryClickLatch) {
            // #1. Perform item's secondaryUse
            val consumptionSuccessful = itemOnGrip?.startSecondaryUse(actor, delta) ?: -1
            if (consumptionSuccessful > -1)
                (actor as Pocketed).inventory.consumeItem(itemOnGrip!!, consumptionSuccessful)
            // #2. If #1 failed, try to pick up the fixture
            else {
                mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
                    pickupFixtureOrDroppedItem(actor, delta, mwx, mwy, mtx, mty)
                    0L
                }
            }

            worldSecondaryClickLatch = true
        }
    }

    override fun worldSecondaryClickEnd(actor: ActorWithBody, delta: Float) {
        val itemOnGrip = (actor as Pocketed).inventory.itemEquipped.get(GameItem.EquipPosition.HAND_GRIP)
        ItemCodex[itemOnGrip]?.endSecondaryUse(actor, delta)

        worldSecondaryClickLatch = false
    }



    private var firstTimeRun = true

    ///////////////
    // prod code //
    ///////////////
    private class ThreadIngameUpdate(val terrarumIngame: TerrarumIngame): Runnable {
        override fun run() {
            TODO()
        }
    }

    internal var autosaveTimer: Second = 0f

    override fun renderImpl(updateRate: Float) {
        // Q&D solution for LoadScreen and Ingame, where while LoadScreen is working, Ingame now no longer has GL Context
        // there's still things to load which needs GL context to be present
        if (!gameFullyLoaded) {

            uiAutosaveNotifier = UIAutosaveNotifier()

            if (gameLoadMode == GameLoadMode.CREATE_NEW) {
                postInitForNewGame()
            }
            else if (gameLoadMode == GameLoadMode.LOAD_FROM) {
                postInitForLoadFromSave(gameLoadInfoPayload as Codices)
            }

            postInit()

            gameUpdateGovernor.reset()

            if (UILoadGovernor.previousSaveWasLoaded) {
                sendNotification(listOf(Lang["GAME_PREV_SAVE_WAS_LOADED1"], Lang["GAME_PREV_SAVE_WAS_LOADED2"]))
            }

            gameFullyLoaded = true
        }
        else {
            IngameRenderer.setRenderedWorld(world) // this doesn't slow down the game and prevents world-changing related bugs
        }

        super.renderImpl(updateRate)

        ingameController.update()


        // ASYNCHRONOUS UPDATE AND RENDER //

        /** UPDATE CODE GOES HERE */
        val dt = Gdx.graphics.deltaTime
        if (!uiAutosaveNotifier.isVisible) {
            autosaveTimer += dt
        }

        gameUpdateGovernor.update(dt, App.UPDATE_RATE, updateGame, renderGame)


        val autosaveInterval = App.getConfigInt("autosaveinterval").coerceAtLeast(60000) / 1000f
        if (autosaveTimer >= autosaveInterval) {
            queueAutosave()
            autosaveTimer -= autosaveInterval
        }

    }

    private var worldWidth: Double = 0.0
    private var oldCamX = 0
    private var oldPlayerX = 0.0

    private var deltaTeeCleared = false

    private val terrarumWorldWatchdogs = TreeMap<String, TerrarumWorldWatchdog>()

    fun registerWatchdog(identifier: String, watchdog: TerrarumWorldWatchdog) {
        terrarumWorldWatchdogs[identifier] = watchdog
    }

    /**
     * Ingame (world) related updates; UI update must go to renderGame()
     */
    private val updateGame =  { delta: Float ->
        val world = this.world
        worldWidth = world.width.toDouble() * TILE_SIZE

        particlesActive = 0


        // synchronised Ingame Input Updater
        // will also queue up the block/wall/wire placed events
        ingameController.update()

        if ((!paused && !App.isScreenshotRequested()) || newWorldLoadedLatch) {

            //hypothetical_input_capturing_function_if_you_finally_decided_to_forgo_gdx_input_processor_and_implement_your_own_to_synchronise_everything()


            ////////////////////////////
            // camera-related updates //
            ////////////////////////////
            FeaturesDrawer.update(delta)

            ///////////////////////////
            // actor-related updates //
            ///////////////////////////
            repossessActor()

            // process actor addition requests
            val addCueCpy = actorAdditionQueue.toList()
            addCueCpy.forEach {
                forceAddActor(it.first, it.second)
                it.third(it.first)
            }
            actorAdditionQueue.removeAll(addCueCpy)
            // determine whether the inactive actor should be activated
            wakeDormantActors()
            // update NOW; allow one last update for the actors flagged to despawn
            updateActors(delta)
            // determine whether the actor should keep being activated or be dormant
            killOrKnockdownActors()
            // process actor removal requests
            val remCueCpy = actorRemovalQueue.toList()
            remCueCpy.forEach {
                forceRemoveActor(it.first, it.second)
                it.third(it.first)
            }
            actorRemovalQueue.removeAll(remCueCpy)
            // update particles
            particlesContainer.toList().forEach { if (!it.flagDespawn) particlesActive++; it.update(delta) }
            // TODO thread pool(?)
            CollisionSolver.process()


            ///////////////////////////
            // world-related updates //
            ///////////////////////////
            actorsRTree = PRTree(actorMBRConverter, 24)
            actorsRTree.load(actorContainerActive.filterIsInstance<ActorWithBody>())

            BlockPropUtil.dynamicLumFuncTickClock()
            world.updateWorldTime(delta)
            measureDebugTime("WorldSimulator.update") {
                WorldSimulator.invoke(actorNowPlaying, delta)
            }
            measureDebugTime("WeatherMixer.update") {
                WeatherMixer.update(delta, actorNowPlaying, world)
            }
            measureDebugTime("BlockStats.update") {
                TileSurvey.update()
            }
            // fill up visibleActorsRenderFront for wires but not on every update
            measureDebugTime("Ingame.FillUpWiresBuffer*") {
                if (WORLD_UPDATE_TIMER % 2 == 1L) {
                    fillUpWiresBuffer()
                }
            }
            measureDebugTime("Ingame.FillUpWirePortsView*") {
                if (WORLD_UPDATE_TIMER % 2 == 0L) {
                    val fixtures = INGAME.actorContainerActive.filterIsInstance<Electric>()
                    fillUpWirePortsView(fixtures)
                }
            }
            terrarumWorldWatchdogs.entries.forEach {
                measureDebugTime("Ingame.Watchdog.${it.key}*") {
                    if (WORLD_UPDATE_TIMER % it.value.runIntervalByTick.toLong() == 0L) {
                        it.value(world)
                    }
                }
            }
            oldCamX = WorldCamera.x
            oldPlayerX = actorNowPlaying?.hitbox?.canonicalX ?: 0.0

            // update audio mixer
            val ratio = (TileSurvey.getRatio("basegame.Ingame.audioReflection") ?: 0.0)
            if (ratio >= 0.0) {
                val ratio1 = ratio.coerceIn(0.0, 1.0)
                App.audioMixer.convolveBusCave.volume = ratio1
                App.audioMixer.convolveBusOpen.volume = 1.0 - ratio1
            }
            else {
                val ratio1 = (ratio / MaterialCodex["AIIR"].sondrefl).absoluteValue.coerceIn(0.0, 1.0)
                App.audioMixer.convolveBusOpen.volume = (1.0 - ratio1).pow(0.75)
                App.audioMixer.convolveBusCave.volume = 0.0
            }
            val openness = (TileSurvey.getRatio("basegame.Ingame.openness") ?: 0.0).times(1.74).coerceIn(0.0, 1.0)
            App.audioMixer.amb1plus2.volume = openness.pow(2.0 / 3.0)



            actorNowPlaying?.let { if (WORLD_UPDATE_TIMER % 4 == 1L) updateWorldGenerator(actorNowPlaying!!) }



            WORLD_UPDATE_TIMER += 1

            // run benchmark if F2 is on
            if (KeyToggler.isOn(Input.Keys.F2)) {
                deltaTeeBenchmarks.appendHead(1f / Gdx.graphics.deltaTime)

                if (deltaTeeCleared) deltaTeeCleared = false
            }
            else if (!deltaTeeCleared) {
                deltaTeeCleared = true
                deltaTeeBenchmarks.clear()
            }
        }


        if ((!paused && !App.isScreenshotRequested()) || newWorldLoadedLatch) {
            // completely consume block change queues because why not
            terrainChangeQueue.clear()
            wallChangeQueue.clear()
            wireChangeQueue.clear()

            oldSelectedWireRenderClass = selectedWireRenderClass
        }

        musicStreamer.update(this, delta)
        MusicService.update(delta)

        ////////////////////////
        // ui-related updates //
        ////////////////////////
        // open/close fake blur UI according to what's opened
        if (uiInventoryPlayer.isVisible ||
            getUIFixture.get()?.isVisible == true || worldTransitionOngoing) {
            uiBlur.setAsOpen()
        }
        else {
            uiBlur.setAsClose()
        }

        // update debuggers using javax.swing //
        if (Authenticator.b()) {
            AVTracker.update()
            ActorsList.update()
        }

        //println("paused = $paused")

        if ((!paused && !App.isScreenshotRequested()) && newWorldLoadedLatch) newWorldLoadedLatch = false


        if (doThingsAfterSave) {
            saveRequested2 = false
            doThingsAfterSave = false
            saveCallback!!()
        }

        if (saveRequested2) {
            saveRequested2 = false
            doForceSave()
        }

        if (worldTransitionPauseRequested > 0) { // let a frame to update before locking (=pausing) entirely
            worldTransitionPauseRequested -= 1
        }
        else if (worldTransitionPauseRequested == 0) {
            paused = true
        }


        if (KeyToggler.isOn(Input.Keys.F10)) {
            batch.inUse {
                batch.color = Color.WHITE
                App.fontSmallNumbers.draw(batch, "$ccY\u00DCupd $ccG${delta.toIntAndFrac(1)}", 2f, App.scr.height - 16f)
                _dbgDeltaUpd = delta
            }
        }
    }

    private var _dbgDeltaUpd = 0f


    private val renderGame = { frameDelta: Float ->
        Gdx.graphics.setTitle(getCanonicalTitle())

        WorldCamera.update(world, actorNowPlaying)

        measureDebugTime("Ingame.FilterVisibleActors") {
            filterVisibleActors()
        }

        if (uiInventoryPlayer == uiFixture) throw IllegalStateException("Do NOT use InventoryPlayer as a UIFixture, the engine cannot handle this situation XwX")

        uiContainer.forEach {

            // suppress inventory opening if fixture inventory is opened
            if (uiFixture?.isClosed == false)
                uiInventoryPlayer.handler.lockToggle()
            else
                uiInventoryPlayer.handler.unlockToggle()


            when (it) {
                is Id_UICanvasNullable -> it.get()?.update(Gdx.graphics.deltaTime)
                is UICanvas -> it.update(Gdx.graphics.deltaTime)
            }
        }
        //uiFixture?.update(Gdx.graphics.deltaTime)
        // deal with the uiFixture being closed
        if (uiFixture?.isClosed == true) { uiFixture = null }

        IngameRenderer.invoke(
            frameDelta,
            paused,
            screenZoom,
            visibleActorsRenderFarBehind,
            visibleActorsRenderBehind,
            visibleActorsRenderMiddle,
            visibleActorsRenderMidTop,
            visibleActorsRenderFront,
            visibleActorsRenderOverlay,
            particlesContainer,
            actorNowPlaying,
            uiContainer// + uiFixture
        )

        // quick and dirty way to show
        if (worldTransitionOngoing) {
            batch.inUse {
                batch.color = gradEndCol
                Toolkit.fillArea(batch, 0, 0, App.scr.width, App.scr.height)


                batch.color = Color.WHITE
                val t = Lang["MENU_IO_SAVING"]
                val circleSheet = CommonResourcePool.getAsTextureRegionPack("loading_circle_64")
                Toolkit.drawTextCentered(batch, App.fontGame, t, Toolkit.drawWidth, 0, ((App.scr.height - circleSheet.tileH) / 2) - 40)

                // -1..63
                val index =
                    ((WriteSavegame.saveProgress / WriteSavegame.saveProgressMax) * circleSheet.horizontalCount * circleSheet.verticalCount).roundToInt() - 1
                if (index >= 0) {
                    val sx = index % circleSheet.horizontalCount
                    val sy = index / circleSheet.horizontalCount
                    // q&d fix for ArrayIndexOutOfBoundsException caused when saving huge world... wut?
                    if (sx in 0 until circleSheet.horizontalCount && sy in 0 until circleSheet.horizontalCount) {
                        batch.draw(
                            circleSheet.get(sx, sy),
                            ((Toolkit.drawWidth - circleSheet.tileW) / 2).toFloat(),
                            ((App.scr.height - circleSheet.tileH) / 2).toFloat()
                        )
                    }
                }
            }
        }


        if (KeyToggler.isOn(Input.Keys.F10)) {
            batch.inUse {
                batch.color = Color.WHITE
                App.fontSmallNumbers.draw(batch, "$ccY\u00DCupd $ccG${_dbgDeltaUpd.toIntAndFrac(1)}", 2f, App.scr.height - 16f)
                App.fontSmallNumbers.draw(batch, "$ccY\u00DCren $ccG${frameDelta.toIntAndFrac(1)}", 2f + 7*12, App.scr.height - 16f)
                App.fontSmallNumbers.draw(batch, "$ccY\u00DCgdx $ccG${Gdx.graphics.deltaTime.toIntAndFrac(1)}", 2f + 7*24, App.scr.height - 16f)
            }
        }
    }

    private fun Point2iMod(x: Int, y: Int) = Point2i(x fmod (world.width / CHUNK_W), y)

    private fun updateWorldGenerator(actor: ActorWithBody) {
        val pcx = (actor.intTilewiseHitbox.canonicalX.toInt() fmod world.width) / CHUNK_W
        val pcy = (actor.intTilewiseHitbox.canonicalY.toInt() fmod world.width) / CHUNK_H
        listOf(
            Point2iMod(pcx - 1, pcy - 2), Point2iMod(pcx, pcy - 2), Point2iMod(pcx + 1, pcy - 2),
            Point2iMod(pcx - 2, pcy - 1), Point2iMod(pcx - 1, pcy - 1), Point2iMod(pcx, pcy - 1), Point2iMod(pcx + 1, pcy - 1),  Point2iMod(pcx + 2, pcy - 1),
            Point2iMod(pcx - 2, pcy), Point2iMod(pcx - 1, pcy), Point2iMod(pcx + 1, pcy),  Point2iMod(pcx + 2, pcy),
            Point2iMod(pcx - 2, pcy + 1), Point2iMod(pcx - 1, pcy + 1), Point2iMod(pcx, pcy + 1), Point2iMod(pcx + 1, pcy + 1),  Point2iMod(pcx + 2, pcy + 1),
            Point2iMod(pcx - 1, pcy + 2), Point2iMod(pcx, pcy + 2), Point2iMod(pcx + 1, pcy + 2),
        ).filter { it.y in 0 until world.height }.filter {  (cx, cy) ->
            if (cy !in 0 until world.height / CHUNK_H) false
            else (world.chunkFlags[cy][cx].and(0x7F) == 0.toByte())
        }.forEach { (cx, cy) ->
            Worldgen.generateChunkIngame(cx, cy) { cx, cy ->
                listOf(0,1,2).forEach { layer ->
                    modified(layer, cx, cy)
                }
            }
        }
    }

    private var worldTransitionOngoing = false
    private var worldTransitionPauseRequested = -1
    private var saveRequested2 = false
    private var saveCallback: (() -> Unit)? = null
    private var doThingsAfterSave = false

    override fun requestForceSave(callback: () -> Unit) {
        saveCallback = callback
        worldTransitionOngoing = true
        saveRequested2 = true
        worldTransitionPauseRequested = 1
        blockMarkingActor.isVisible = false
    }

    internal fun doForceSave() {
        // TODO show appropriate UI
//        uiBlur.setAsOpen()

        saveTheGame({ // onSuccessful
            System.gc()
            autosaveTimer = 0f

            // TODO hide appropriate UI
            uiBlur.setAsClose()
            doThingsAfterSave = true
        }, { // onError
            // TODO show failure message

            // TODO hide appropriate UI
            uiBlur.setAsClose()
        })
    }

    override fun saveTheGame(onSuccessful: () -> Unit, onError: (Throwable) -> Unit) {
        val saveTime_t = App.getTIME_T()
        val playerSavefile = getPlayerSaveFiledesc(INGAME.playerSavefileName)
        val worldSavefile = getWorldSaveFiledesc(INGAME.worldSavefileName)


        INGAME.makeSavegameBackupCopy(playerSavefile)
        WriteSavegame(saveTime_t, WriteSavegame.SaveMode.PLAYER, INGAME.playerDisk, playerSavefile, INGAME as TerrarumIngame, false, onError) {

            INGAME.makeSavegameBackupCopy(worldSavefile)
            WriteSavegame(saveTime_t, WriteSavegame.SaveMode.WORLD, INGAME.worldDisk, worldSavefile, INGAME as TerrarumIngame, false, onError) {
                // callback:
                // rebuild the disk skimmers
                INGAME.actorContainerActive.filterIsInstance<IngamePlayer>().forEach {
                    printdbg(this, "Game Save callback -- rebuilding the disk skimmer for IngamePlayer ${it.actorValue.getAsString(AVKey.NAME)}")
//                                it.rebuildingDiskSkimmer?.rebuild()
                }

                // return to normal state
                onSuccessful()

                loadedTime_t = App.getTIME_T()
            }
        }
    }

    private val maxRenderableWires = ReferencingRanges.ACTORS_WIRES.last - ReferencingRanges.ACTORS_WIRES.first + 1
    private val maxRenderablePorts = ReferencingRanges.ACTORS_WIRE_PORTS.last - ReferencingRanges.ACTORS_WIRE_PORTS.first + 1
    private val wireActorsContainer = Array(maxRenderableWires) { WireActor(ReferencingRanges.ACTORS_WIRES.first + it).let {
        forceAddActor(it)
        /*^let*/ it
    } }
    private val wirePortActorsContainer = Array(maxRenderablePorts) { WirePortActor(ReferencingRanges.ACTORS_WIRE_PORTS.first + it).let {
        forceAddActor(it)
        /*^let*/ it
    } }

    private fun fillUpWiresBuffer() {
        val for_y_start = (WorldCamera.y.toFloat() / TILE_SIZE).floorToInt() - 1
        val for_y_end = for_y_start + BlocksDrawer.tilesInVertical + 2*1

        val for_x_start = (WorldCamera.x.toFloat() / TILE_SIZE).floorToInt() - 1
        val for_x_end = for_x_start + BlocksDrawer.tilesInHorizontal + 2*1

        var wiringCounter = 0
        for (y in for_y_start..for_y_end) {
            for (x in for_x_start..for_x_end) {
                if (wiringCounter >= maxRenderableWires) break

                val (wires, nodes) = world.getAllWiresFrom(x, y)

                wires?.forEach {
                    val wireActor = wireActorsContainer[wiringCounter]

                    wireActor.setWire(it, x, y, nodes!![it]!!.cnx)

                    if (WireCodex[it].renderClass == selectedWireRenderClass || selectedWireRenderClass == "wire_render_all") {
                        wireActor.renderOrder = Actor.RenderOrder.OVERLAY
                    }
                    else {
                        wireActor.renderOrder = Actor.RenderOrder.FAR_BEHIND
                    }

                    wireActor.isUpdate = true
                    wireActor.isVisible = true
                    wireActor.forceDormant = false

                    wiringCounter += 1
                }

            }
        }

        for (i in wiringCounter until maxRenderableWires) {
            val wireActor = wireActorsContainer[i]
            wireActor.isUpdate = false
            wireActor.isVisible = false
            wireActor.forceDormant = true
        }
    }

    private fun fillUpWirePortsView(fixtures: List<Electric>) {
        var portsCounter = 0

        fixtures.forEach {
            (it.wireSinkTypes.toList() + it.wireEmitterTypes.toList()).forEach { (boxIndex, type) ->
                if (portsCounter < wirePortActorsContainer.size) {
                    val wireActor = wirePortActorsContainer[portsCounter]
                    val (wx, wy) = it.worldBlockPos!! + it.blockBoxIndexToPoint2i(boxIndex)

                    wireActor.setPort(type, wx, wy)
                    wireActor.isUpdate = true
                    wireActor.isVisible = true
                    wireActor.forceDormant = false

                    portsCounter += 1
                }
            }
        }



        for (i in portsCounter until maxRenderablePorts) {
            val wireActor = wirePortActorsContainer[i]
            wireActor.isUpdate = false
            wireActor.isVisible = false
            wireActor.forceDormant = true
        }
    }

    private fun filterVisibleActors() {
        visibleActorsRenderFarBehind.clear()
        visibleActorsRenderBehind.clear()
        visibleActorsRenderMiddle.clear()
        visibleActorsRenderMidTop.clear()
        visibleActorsRenderFront.clear()
        visibleActorsRenderOverlay.clear()

        actorContainerActive.forEach {
            if (it is ActorWithBody)
                actorToRenderQueue(it).add(it)
        }
    }

    private fun repossessActor() {
        // check if currently pocessed actor is removed from game
        if (!theGameHasActor(actorNowPlaying)) {
            // re-possess canonical player
            if (theGameHasActor(actorGamer))
                changePossession(actorGamer)
            else
                actorNowPlaying = null
        }
    }

    internal fun changePossession(newActor: ActorHumanoid) {
        if (!theGameHasActor(actorNowPlaying)) { throw NoSuchActorWithRefException(newActor) }

        actorNowPlaying = newActor
    }

    internal fun changePossession(refid: Int) {
        val actorToChange = getActorByID(refid)

        if (actorToChange !is ActorHumanoid) {
            throw Error("Unpossessable actor $refid: type expected ActorHumanoid, got ${actorToChange.javaClass.canonicalName}")
        }

        changePossession(getActorByID(refid) as ActorHumanoid)
    }

    fun wakeDormantActors() {
        var actorContainerSize = actorContainerInactive.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainerInactive
            val actor = actorContainerInactive[i]
            if (actor is ActorWithBody && (actor.chunkAnchoring || actor.inUpdateRange(world)) && !actor.forceDormant) {
                activateDormantActor(actor) // duplicates are checked here
                actorContainerSize -= 1
                i-- // array removed 1 elem, so we also decrement counter by 1
            }
            i++
        }
    }

    /**
     * determine whether the actor should be active or dormant by its distance from the player.
     * If the actor must be dormant, the target actor will be put to the list specifically for them.
     * if the actor is not to be dormant, it will be just ignored.
     */
    fun killOrKnockdownActors() {
        var actorContainerSize = actorContainerActive.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainerActive
            val actor = actorContainerActive[i]
            val actorIndex = i
            // kill actors flagged to despawn
            if (actor.despawned) {
                queueActorRemoval(actor)
                actorContainerSize -= 1
                i-- // array removed 1 elem, so we also decrement counter by 1
            }
            // inactivate distant actors
            else if (actor is ActorWithBody && (!actor.chunkAnchoring && !actor.inUpdateRange(world) || actor.forceDormant)) {
                if (actor !is Projectile) { // if it's a projectile, don't inactivate it; just kill it.
                    actorContainerInactive.add(actor) // naïve add; duplicates are checked when the actor is re-activated
                }
                actorContainerActive.removeAt(actorIndex)
                actorContainerSize -= 1
                i-- // array removed 1 elem, so we also decrement counter by 1
            }
            i++
        }
    }

    /**
     * Update actors concurrently.
     *
     * NOTE: concurrency for actor updating is currently disabled because of it's poor performance
     */
    fun updateActors(delta: Float) {
        if (false) { // don't multithread this for now, it's SLOWER //if (Terrarum.MULTITHREAD && actorContainerActive.size > Terrarum.THREADS) {
            /*ThreadExecutor.renew()

            val actors = actorContainerActive.size.toFloat()
            // set up indices
            for (i in 0..App.THREAD_COUNT - 1) {
                ThreadExecutor.submit(
                        ThreadActorUpdate(
                                actors.div(App.THREAD_COUNT).times(i).roundToInt(),
                                actors.div(App.THREAD_COUNT).times(i + 1).roundToInt() - 1
                        )
                )
            }

            ThreadExecutor.join()

            actorNowPlaying?.update(delta)*/
        }
        else {
            actorContainerActive.forEach {
                if (it != actorNowPlaying) {
                    it.update(delta)

                    if (it is Pocketed) {
                        it.inventory.forEach { inventoryEntry ->
                            ItemCodex[inventoryEntry.itm]!!.effectWhileInPocket(it as ActorWithBody, delta) // kind of an error checking because all Pocketed must be ActorWithBody
                            if (it.equipped(inventoryEntry.itm)) {
                                ItemCodex[inventoryEntry.itm]!!.effectWhileEquipped(it as ActorWithBody, delta)
                            }
                        }
                    }

                    if (it is CuedByTerrainChange) {
                        terrainChangeQueue.toList().forEach { cue ->
//                            printdbg(this, "Ingame actors terrainChangeCue: ${cue}")
                            if (cue != null) it.updateForTerrainChange(cue)
                        }
                    }

                    if (it is CuedByWallChange) {
                        wallChangeQueue.toList().forEach { cue ->
//                            printdbg(this, "Ingame actors wallChangeCue: ${cue}")
                            if (cue != null) it.updateForWallChange(cue)
                        }
                    }

                    if (it is CuedByWireChange) {
                        wireChangeQueue.toList().forEach { cue ->
//                            printdbg(this, "Ingame actors wireChangeCue: ${cue}")
                            if (cue != null) it.updateForWireChange(cue)
                        }
                    }
                }
            }
            actorNowPlaying?.update(delta)
            //AmmoMeterProxy(player, uiVitalItem.UI as UIVitalMetre)
        }
    }

    fun queueAutosave() {
        val start = System.nanoTime()

        uiAutosaveNotifier.setAsOpen()

        val saveTime_t = App.getTIME_T()
        val playerSavefile0 = getPlayerSaveFiledesc(INGAME.playerSavefileName)
        val worldSavefile0 = getWorldSaveFiledesc(INGAME.worldSavefileName)

        val playerSavefile = INGAME.makeSavegameBackupCopyAuto(playerSavefile0)
        WriteSavegame(saveTime_t, WriteSavegame.SaveMode.PLAYER, INGAME.playerDisk, playerSavefile, INGAME as TerrarumIngame, true, autosaveOnErrorAction) {

            val worldSavefile = INGAME.makeSavegameBackupCopyAuto(worldSavefile0)
            WriteSavegame(saveTime_t, WriteSavegame.SaveMode.QUICK_WORLD, INGAME.worldDisk, worldSavefile, INGAME as TerrarumIngame, true, autosaveOnErrorAction) {
                // callback:
                // rebuild the disk skimmers
                INGAME.actorContainerActive.filterIsInstance<IngamePlayer>().forEach {
                    printdbg(this, "Game Save callback -- rebuilding the disk skimmer for IngamePlayer ${it.actorValue.getAsString(AVKey.NAME)}")
//                    it.rebuildingDiskSkimmer?.rebuild()
                }

                // return to normal state
                uiAutosaveNotifier.setAsClose()
                autosaveTimer = 0f

                val timeDiff = System.nanoTime() - start
                debugTimers.put("Last Autosave Duration", timeDiff)
                printdbg(this, "Last Autosave Duration: ${(timeDiff) / 1000000000} s")


                loadedTime_t = App.getTIME_T()
            }
        }

    }


    fun Double.sqr() = this * this
    fun Int.sqr() = this * this
    fun min(vararg d: Double): Double {
        var ret = Double.MAX_VALUE
        d.forEach { if (it < ret) ret = it }
        return ret
    }


    private val cameraWindowX = WorldCamera.x.toDouble()..WorldCamera.xEnd.toDouble()
    private val cameraWindowY = WorldCamera.y.toDouble()..WorldCamera.yEnd.toDouble()

    private fun actorToRenderQueue(actor: ActorWithBody): ArrayList<ActorWithBody> {
        return when (actor.renderOrder) {
            Actor.RenderOrder.FAR_BEHIND -> visibleActorsRenderFarBehind
            Actor.RenderOrder.BEHIND -> visibleActorsRenderBehind
            Actor.RenderOrder.MIDDLE -> visibleActorsRenderMiddle
            Actor.RenderOrder.MIDTOP -> visibleActorsRenderMidTop
            Actor.RenderOrder.FRONT  -> visibleActorsRenderFront
            Actor.RenderOrder.OVERLAY-> visibleActorsRenderOverlay
        }
    }


    override fun forceRemoveActor(actor: Actor, caller: Throwable) {
        arrayOf(actorContainerActive, actorContainerInactive).forEach { actorContainer ->
            val indexToDelete = actorContainer.searchForIndex(actor.referenceID) { it.referenceID }
            if (indexToDelete != null) {
//                printdbg(this, "Removing actor $actor")
//                printStackTrace(this)

                actor.dispose()
                actorContainer.removeAt(indexToDelete)

                // indexToDelete >= 0 means that the actor certainly exists in the game
                // which means we don't need to check if i >= 0 again
                if (actor is ActorWithBody) {
                    actorToRenderQueue(actor).remove(actor)
                }
            }
        }
    }

    private fun ArrayList<*>.binarySearch(actor: Actor) = this.binarySearch(actor.referenceID)

    private fun ArrayList<*>.binarySearch(ID: Int): Int {
        // code from collections/Collections.kt
        var low = 0
        var high = this.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows

            val midVal = get(mid)!!

            if (ID > midVal.hashCode())
                low = mid + 1
            else if (ID < midVal.hashCode())
                high = mid - 1
            else
                return mid // key found
        }
        return -(low + 1)  // key not found
    }

    /**
     * Check for duplicates, append actor and sort the list
     */
    override fun forceAddActor(actor: Actor?, caller: Throwable) {
        if (actor == null) return

        if (theGameHasActor(actor.referenceID)) {
            throw ReferencedActorAlreadyExistsException(actor, caller)
        }
        else {
            actorContainerActive.add(actor)
            if (actor is ActorWithBody) actorToRenderQueue(actor).add(actor)
        }
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        uiContainer.forEach { it?.inputStrobed(e) }
    }

    fun activateDormantActor(actor: Actor) {
        if (!isInactive(actor.referenceID)) {
            /*if (isActive(actor.referenceID))
                throw Error("The actor $actor is already activated")
            else
                throw Error("The actor $actor already exists in the game")*/
            return
        }
        else {
            actorContainerInactive.remove(actor)
            actorContainerActive.add(actor)

            if (actor is ActorWithBody) {
                actorToRenderQueue(actor).add(actor)
            }
        }
    }

    fun addParticle(particle: ParticleBase) {
        particlesContainer.appendHead(particle)
    }

    private var barehandAxeInUse = false
    private var barehandPickInUse = false

    fun endPerformBarehandAction(actor: ActorWithBody) {
        if (barehandAxeInUse) {
            barehandAxeInUse = false
            AxeCore.endPrimaryUse(actor, null)
        }
        if (barehandPickInUse) {
            barehandPickInUse = false
            PickaxeCore.endPrimaryUse(actor, null)
        }
    }

    private fun getActorsAtVicinity(worldX: Double, worldY: Double, radius: Double): List<ActorWithBody> {
        val outList = java.util.ArrayList<ActorWithBody>()
        try {
            actorsRTree.find(worldX - radius, worldY - radius, worldX + radius, worldY + radius, outList)
        }
        catch (e: NullPointerException) {
        }
        return outList
    }

    private fun getPunchSize(actor: ActorWithBody) = actor.scale * actor.actorValue.getAsDouble(AVKey.BAREHAND_BASE_DIGSIZE)!!


    fun performBarehandAction(actor: ActorWithBody, delta: Float, mwx: Double, mwy: Double, mtx: Int, mty: Int) {
        // for giant actors punching every structure pickaxe can dig out
        val canAttackOrDig =
            actor.scale * actor.baseHitboxH >= (actor.actorValue.getAsDouble(AVKey.BAREHAND_MINHEIGHT) ?: 4294967296.0)

        // for players punching dirts or weaker blocks
        val canDigSoftTileOnly =
            actor is ActorHumanoid && (actor.baseHitboxH * actor.scale) >= 32f


        val punchSize = getPunchSize(actor)
        val punchBlockSize = punchSize.div(TILE_SIZED).floorToInt()

        val mouseUnderPunchableTree = BlockCodex[world.getTileFromTerrain(mtx, mty)].hasAnyTagsOf("LEAVES", "TREESMALL")


        // punch a small tree/shrub
        if (mouseUnderPunchableTree) {
            barehandAxeInUse = true
            AxeCore.startPrimaryUse(actor, delta, null, mtx, mty, punchBlockSize.coerceAtLeast(1), punchBlockSize.coerceAtLeast(1), listOf("TREESMALL"))
        }
        // TODO attack a mob
//        else if (mobsUnderHand.size > 0 && canAttackOrDig) {
//        }
        // else, punch a block
        else if (canAttackOrDig) {
            if (punchBlockSize > 0) {
                barehandPickInUse = true
                PickaxeCore.startPrimaryUse(actor, delta, null, mtx, mty, 1.0 / punchBlockSize, punchBlockSize, punchBlockSize)
            }
        }
        else if (canDigSoftTileOnly) {
            barehandPickInUse = true
            val tileUnderCursor = INGAME.world.getTileFromTerrain(mtx, mty)
            val tileprop = BlockCodex[tileUnderCursor]
            if (tileprop.strength <= 12)
                PickaxeCore.startPrimaryUse(actor, delta, null, mtx, mty, 1.0, 1, 1)
        }
    }

    fun getActorsUnderMouse(mwx: Double, mwy: Double): List<ActorWithBody> {
        val actorsUnderMouse: List<ActorWithBody> = getActorsAt(mwx, mwy).filter { it !is InternalActor }.sortedBy {
            (mwx - it.hitbox.centeredX).sqr() + (mwy - it.hitbox.centeredY).sqr()
        } // sorted by the distance from the mouse
        return actorsUnderMouse
    }

    fun pickupFixtureOrDroppedItem(actor: ActorWithBody, delta: Float, mwx: Double, mwy: Double, mtx: Int, mty: Int, assignToQuickslot: Boolean = true) {
        val actorsUnderMouse = getActorsUnderMouse(mwx, mwy)

        val fixture = actorsUnderMouse.firstOrNull {
            it is FixtureBase && it.canBeDespawned &&
                    System.nanoTime() - it.spawnRequestedTime > 500000000 // don't pick up the fixture if it was recently placed (0.5 seconds)
        } as? FixtureBase
        val droppedItemNoAutoPickup = actorsUnderMouse.firstOrNull {
            it is DroppedItem && it.noAutoPickup
        } as? DroppedItem
        val mob = actorsUnderMouse.firstOrNull {
            it !is FixtureBase && it.physProp.usePhysics && !it.physProp.immobileBody
        } as? ActorWithBody

        // pickup a fixture
        if (fixture != null) {
            val fixtureItem = fixture.itemise()
            printdbg(this, "Fixture pickup at F${WORLD_UPDATE_TIMER}: ${fixture.javaClass.canonicalName} -> $fixtureItem")
            // 0. hide tooltips
            TooltipManager.tooltipShowing.clear()
            setTooltipMessage(null)
            if (!fixture.flagDespawn) {
                // 1. put the fixture to the inventory
                fixture.flagDespawn()
                // 2. register this item(fixture) to the quickslot so that the player sprite would be actually lifting the fixture
                if (actor is Pocketed) {
                    actor.inventory.add(fixtureItem)

                    if (assignToQuickslot) {
                        actor.equipItem(fixtureItem)
                        actor.inventory.setQuickslotItemAtSelected(fixtureItem)
                        // 2-1. unregister if other slot has the same item
                        for (k in 0..9) {
                            if (actor.inventory.getQuickslotItem(k)?.itm == fixtureItem && k != actor.actorValue.getAsInt(
                                    AVKey.__PLAYER_QUICKSLOTSEL
                                )
                            ) {
                                actor.inventory.setQuickslotItem(k, null)
                            }
                        }
                    }
                }
            }
        }
        // pickup a dropped item (no auto-pickup variants only)
        else if (droppedItemNoAutoPickup != null) {
            val droppedItemID = droppedItemNoAutoPickup.itemID
            val droppedItemCount = droppedItemNoAutoPickup.itemCount
            // 0. hide tooltips
            TooltipManager.tooltipShowing.clear()
            setTooltipMessage(null)
            if (!droppedItemNoAutoPickup.flagDespawn) {
                // 1. put the fixture to the inventory
                droppedItemNoAutoPickup.flagDespawn()
                // 2. register this item(fixture) to the quickslot so that the player sprite would be actually lifting the fixture
                // BUUUUUUUT, only when the slot is already empty this time
                if (actor is Pocketed) {
                    actor.inventory.add(droppedItemID, droppedItemCount)

                    if (assignToQuickslot && actor.inventory.getQuickslotItem(actor.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)) == null) {
                        actor.equipItem(droppedItemID)
                        actor.inventory.setQuickslotItemAtSelected(droppedItemID)
                        // 2-1. unregister if other slot has the same item
                        for (k in 0..9) {
                            if (actor.inventory.getQuickslotItem(k)?.itm == droppedItemID && k != actor.actorValue.getAsInt(
                                    AVKey.__PLAYER_QUICKSLOTSEL
                                )
                            ) {
                                actor.inventory.setQuickslotItem(k, null)
                            }
                        }
                    }
                }
            }
        }
        else if (mob != null) {
            // TODO pickup a mob
        }
    }

    override fun hide() {
        uiContainer.forEach { it?.handler?.dispose() }
    }

    /**
     * @param width same as AppLoader.terrarumAppConfig.screenW
     * @param height same as AppLoader.terrarumAppConfig.screenH
     * @see net.torvald.terrarum.Terrarum
     */
    override fun resize(width: Int, height: Int) {

        // FIXME debugger is pointing at this thing, not sure it actually caused memleak
        //MegaRainGovernor.resize()


        IngameRenderer.resize(App.scr.width, App.scr.height)
        val drawWidth = Toolkit.drawWidth


        if (gameFullyLoaded) {
            // resize UIs

            notifier.setPosition(
                (Toolkit.drawWidth - notifier.width) / 2,
                App.scr.height - notifier.height - App.scr.tvSafeGraphicsHeight
            )
            noticelet.setPosition(0, 0)
            uiQuickBar.setPosition((drawWidth - uiQuickBar.width) / 2, App.scr.tvSafeGraphicsHeight)

            // inventory
            /*uiInventoryPlayer =
                    UIInventory(player,
                            width = 840,
                            height = AppLoader.terrarumAppConfig.screenH - 160,
                            categoryWidth = 210
                    )*/


            // basic watch-style notification bar (temperature, new mail)
            uiBasicInfo.setPosition(drawWidth - uiBasicInfo.width, 0)
            uiWatchTierOne.setPosition(
                    ((drawWidth - App.scr.tvSafeGraphicsWidth) - (uiQuickBar.posX + uiQuickBar.width) - uiWatchTierOne.width) / 2 + (uiQuickBar.posX + uiQuickBar.width),
                    App.scr.tvSafeGraphicsHeight + 8
            )
        }


        printdbg(this, "Resize event")
    }

    override fun dispose() {
        visibleActorsRenderFarBehind.forEach { it.dispose() }
        visibleActorsRenderBehind.forEach { it.dispose() }
        visibleActorsRenderMiddle.forEach { it.dispose() }
        visibleActorsRenderMidTop.forEach { it.dispose() }
        visibleActorsRenderFront.forEach { it.dispose() }
        visibleActorsRenderOverlay.forEach { it.dispose() }

        uiContainer.forEach {
            it?.handler?.dispose()
            it?.dispose()
        }
        uiFixturesHistory.forEach {
            try {
                it.handler.dispose()
                it.dispose()
            }
            catch (e: IllegalArgumentException) {}
        }

        musicStreamer.dispose()
        super.dispose()
    }
}
