package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.EMDASH
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.BlockPropUtil
import net.torvald.terrarum.blockstats.BlockStats
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.console.AVTracker
import net.torvald.terrarum.console.ActorsList
import net.torvald.terrarum.console.Authenticator
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gamecontroller.IngameController
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameitem.inInteractableRange
import net.torvald.terrarum.gameparticles.ParticleBase
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldSimulator
import net.torvald.terrarum.modulebasegame.gameactors.*
import net.torvald.terrarum.modulebasegame.gameactors.physicssolver.CollisionSolver
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore
import net.torvald.terrarum.modulebasegame.gameworld.GameEconomy
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen
import net.torvald.terrarum.modulebasegame.worldgenerator.WorldgenParams
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.LoadSavegame
import net.torvald.terrarum.serialise.ReadActor
import net.torvald.terrarum.serialise.WriteSavegame
import net.torvald.terrarum.tvda.DiskSkimmer
import net.torvald.terrarum.tvda.VDUtil
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UIAutosaveNotifier
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.util.CircularArray
import org.khelekore.prtree.PRTree
import java.util.*
import java.util.concurrent.locks.ReentrantLock


/**
 * Ingame instance for the game Terrarum.
 *
 * Created by minjaesong on 2017-06-16.
 */

open class TerrarumIngame(batch: SpriteBatch) : IngameInstance(batch) {

    var historicalFigureIDBucket: ArrayList<Int> = ArrayList<Int>()


    /**
     * list of Actors that is sorted by Actors' referenceID
     */
    //val ACTORCONTAINER_INITIAL_SIZE = 64
    val PARTICLES_MAX = App.getConfigInt("maxparticles")
    val particlesContainer = CircularArray<ParticleBase>(PARTICLES_MAX, true)
    val uiContainer = UIContainer()

    // these are required because actors always change their position
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
        fun setCameraPosition(batch: SpriteBatch, camera: Camera, newX: Float, newY: Float) {
            camera.position.set((-newX + App.scr.halfw).round(), (-newY + App.scr.halfh).round(), 0f)
            camera.update()
            batch.projectionMatrix = camera.combined
        }

        fun getCanonicalTitle() = App.GAME_NAME +
                                  " $EMDASH F: ${Gdx.graphics.framesPerSecond}" +
                                  if (App.IS_DEVELOPMENT_BUILD)
                                      " (ΔF${Terrarum.updateRateStr})" +
                                      " $EMDASH M: J${Terrarum.memJavaHeap}M / N${Terrarum.memNativeHeap}M / U${Terrarum.memUnsafe}M / X${Terrarum.memXmx}M"
                                  else
                                      ""

        val ACTOR_UPDATE_RANGE = 4096

        fun distToActorSqr(world: GameWorld, a: ActorWithBody, p: ActorWithBody) =
                minOf(// take min of normal position and wrapped (x < 0) position
                        (a.hitbox.centeredX - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                        ((a.hitbox.centeredX + world.width * TILE_SIZE) - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                        ((a.hitbox.centeredX - world.width * TILE_SIZE) - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr()
                )
        fun distToCameraSqr(world: GameWorld, a: ActorWithBody) =
                minOf(
                        (a.hitbox.centeredX - WorldCamera.xCentre).sqr() +
                        (a.hitbox.centeredY - WorldCamera.yCentre).sqr(),
                        ((a.hitbox.centeredX + world.width * TILE_SIZE) - WorldCamera.xCentre).sqr() +
                        (a.hitbox.centeredY - WorldCamera.yCentre).sqr(),
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
    }



    init {
    }


    lateinit var uiBlur: UIFakeBlurOverlay
    lateinit var uiPieMenu: UIQuickslotPie
    lateinit var uiQuickBar: UIQuickslotBar
    lateinit var uiInventoryPlayer: UIInventoryFull
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
            field?.let { it.setAsClose() }
            value?.let { uiFixturesHistory.add(it) }
            field = value
        }

    val getUIFixture = object : Id_UICanvasNullable { // quick workaround for the type erasure (you can't use lambda...)
        override fun get(): UICanvas? {
            return uiFixture
        }
    }

    lateinit var uiVitalPrimary: UICanvas
    lateinit var uiVitalSecondary: UICanvas
    lateinit var uiVitalItem: UICanvas // itemcount/durability of held block or active ammo of held gun. As for the block, max value is 500.

    private val uiFixturesHistory = HashSet<UICanvas>()

    private lateinit var uiBasicInfo: UICanvas
    private lateinit var uiWatchTierOne: UICanvas
    lateinit var uiAutosaveNotifier: UIAutosaveNotifier
    lateinit var uiCheatMotherfuckerNootNoot: UICheatDetected


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



    //////////////
    // GDX code //
    //////////////


    lateinit var gameLoadMode: GameLoadMode
    lateinit var gameLoadInfoPayload: Any


    enum class GameLoadMode {
        CREATE_NEW, LOAD_FROM
    }

    override fun show() {
        //initViewPort(AppLoader.terrarumAppConfig.screenW, AppLoader.terrarumAppConfig.screenH)

        // gameLoadMode and gameLoadInfoPayload must be set beforehand!!

        when (gameLoadMode) {
            GameLoadMode.CREATE_NEW -> enterCreateNewWorld(gameLoadInfoPayload as NewGameParams)
            GameLoadMode.LOAD_FROM  -> enterLoadFromSave(gameLoadInfoPayload as Codices)
        }

        IngameRenderer.setRenderedWorld(world)


        super.show() // this function sets gameInitialised = true
    }

    data class NewGameParams(
            val player: IngamePlayer,
            val newWorldParams: NewWorldParameters
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
            val disk: DiskSkimmer, // WORLD disk
            val world: GameWorld,
//            val meta: WriteMeta.WorldMeta,
//            val block: BlockCodex,
//            val item: ItemCodex,
//            val wire: WireCodex,
//            val material: MaterialCodex,
//            val faction: FactionCodex,
//            val apocryphas: Map<String, Any>,
            val actors: List<ActorID>,
            val player: IngamePlayer
    )



    /**
     * Init instance by loading saved world
     */
    private fun enterLoadFromSave(codices: Codices) {
        if (gameInitialised) {
            printdbg(this, "loaded successfully.")
        }
        else {
            printdbg(this, "Ingame setting things up from the savegame")

            RoguelikeRandomiser.loadFromSave(codices.world.randSeeds[0], codices.world.randSeeds[1])
            WeatherMixer.loadFromSave(codices.world.randSeeds[2], codices.world.randSeeds[3])

//            Terrarum.itemCodex.loadFromSave(codices.item)
//            Terrarum.apocryphas = HashMap(codices.apocryphas)

        }
    }

    /** Load rest of the game with GL context */
    private fun postInitForLoadFromSave(codices: Codices) {
        codices.actors.forEach {
            try {
                val actor = ReadActor(codices.disk, LoadSavegame.getFileReader(codices.disk, it.toLong()))
                if (actor !is IngamePlayer) { // actor list should not contain IngamePlayers (see WriteWorld.preWrite) but just in case...
                    addNewActor(actor)
                }
            }
            catch (e: NullPointerException) {
                System.err.println("Could not read the actor ${it} from the disk")
                e.printStackTrace()
            }
        }

        printdbg(this, "Player localhash: ${codices.player.localHashStr}, hasSprite: ${codices.player.sprite != null}")

        // assign new random referenceID for player
        codices.player.referenceID = Terrarum.generateUniqueReferenceID(Actor.RenderOrder.MIDDLE)
        addNewActor(codices.player)


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
                    codices.player.actorValue = it.actorValue!!
                    codices.player.inventory = it.inventory!!
                }
            }
            // if not, move player to the spawn point
            else {
                printdbg(this, "No mapping found")
                printdbg(this, "Changing XY Position ${codices.player.hitbox.canonVec} -> (${world.spawnX * TILE_SIZED}, ${world.spawnY * TILE_SIZED})")

                codices.player.setPosition(world.spawnX * TILE_SIZED, world.spawnY * TILE_SIZED)
            }
        }


        // by doing this, whatever the "possession" the player had will be broken by the game load
        actorNowPlaying = codices.player
        actorGamer = codices.player

        makeSavegameBackupCopy() // don't put it on the postInit() or render(); postInitForNewGame calls this function on the savegamewriter's callback
    }

    private fun postInitForNewGame() {
        worldSavefileName = LoadSavegame.getWorldSavefileName(savegameNickname, world)
        playerSavefileName = LoadSavegame.getPlayerSavefileName(actorGamer)

        worldDisk = VDUtil.createNewDisk(
                1L shl 60,
                savegameNickname,
                Common.CHARSET
        )

        playerDisk = VDUtil.createNewDisk(
                1L shl 60,
                actorGamer.actorValue.getAsString(AVKey.NAME) ?: "",
                Common.CHARSET
        )

        // go to spawn position
        printdbg(this, "World Spawn position: (${world.spawnX}, ${world.spawnY})")
        actorGamer.setPosition(
                world.spawnX * TILE_SIZED,
                world.spawnY * TILE_SIZED
        )
        actorGamer.backupPlayerProps(isMultiplayer)

        val onError = { e: Throwable -> uiAutosaveNotifier.setAsError() }

        // make initial savefile
        // we're not writing multiple files at one go because:
        //  1. lighten the IO burden
        //  2. cannot sync up the "counter" to determine whether both are finished
        uiAutosaveNotifier.setAsOpen()
        val saveTime_t = App.getTIME_T()
        WriteSavegame.immediate(saveTime_t, WriteSavegame.SaveMode.PLAYER, playerDisk, getPlayerSaveFiledesc(playerSavefileName), this, true, onError) {
            makeSavegameBackupCopy(getPlayerSaveFiledesc(playerSavefileName))

            WriteSavegame.immediate(saveTime_t, WriteSavegame.SaveMode.WORLD, worldDisk, getWorldSaveFiledesc(worldSavefileName), this, true, onError) {
                makeSavegameBackupCopy(getWorldSaveFiledesc(worldSavefileName)) // don't put it on the postInit() or render(); must be called using callback
                uiAutosaveNotifier.setAsClose()
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
            App.getLoadScreen().addMessage("${App.GAME_NAME} version ${App.getVERSION_STRING()}")

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
            Worldgen.attachMap(world, WorldgenParams(worldParams.worldGenSeed))
            Worldgen.generateMap()


            historicalFigureIDBucket = ArrayList<Int>()

            savegameNickname = worldParams.savegameName


            world.worldCreator = UUID.fromString(player.uuid.toString())

            printdbg(this, "new woridIndex: ${world.worldIndex}")
            printdbg(this, "worldCurrentlyPlaying: ${player.worldCurrentlyPlaying}")

            actorNowPlaying = player
            actorGamer = player
            addNewActor(player)

        }

        KeyToggler.forceSet(Input.Keys.Q, false)
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

        // init console window
        // TODO test put it on the IngameInstance.(init)
        //consoleHandler = ConsoleWindow()
        //consoleHandler.setPosition(0, 0)

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
        uiPieMenu.setPosition(drawWidth / 2, App.scr.halfh)

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

        uiWatchTierOne = UITierOneWatch()
        uiWatchTierOne.setAsAlwaysVisible()
        uiWatchTierOne.setPosition(
                ((drawWidth - App.scr.tvSafeActionWidth) - (uiQuickBar.posX + uiQuickBar.width) - uiWatchTierOne.width) / 2 + (uiQuickBar.posX + uiQuickBar.width),
                App.scr.tvSafeGraphicsHeight + 8
        )

        // basic watch-style notification bar (temperature, new mail)
        uiBasicInfo = UIBasicInfo()
        uiBasicInfo.setAsAlwaysVisible()
        uiBasicInfo.setPosition((uiQuickBar.posX - uiBasicInfo.width - App.scr.tvSafeActionWidth) / 2 + App.scr.tvSafeActionWidth, uiWatchTierOne.posY)


        uiCheatMotherfuckerNootNoot = UICheatDetected()


        // batch-process uiAliases
        // NOTE: UIs that should pause the game (e.g. Inventory) must have relevant codes ON THEIR SIDE
        uiContainer.add(
                // drawn first
                //uiVitalPrimary,
                //uiVitalSecondary,
                //uiVitalItem,

                uiBlur,

                uiPieMenu,
                uiQuickBar,
//                uiBasicInfo, // temporarily commenting out: wouldn't make sense for v 0.3 release
                uiWatchTierOne,
                UIScreenZoom(),
                uiAutosaveNotifier,
                uiInventoryPlayer,
                getUIFixture,
                uiTooltip,
                consoleHandler,
                uiCheatMotherfuckerNootNoot
                // drawn last
        )


        ingameUpdateThread = ThreadIngameUpdate(this)
        updateThreadWrapper = Thread(ingameUpdateThread, "Terrarum UpdateThread")



        // these need to appear on top of any others
        uiContainer.add(notifier)

        App.setDebugTime("Ingame.UpdateCounter", 0)

        // some sketchy test code here



    }// END enter

    override fun worldPrimaryClickStart(actor: ActorWithBody, delta: Float) {
        //println("[Ingame] worldPrimaryClickStart $delta")

        // prepare some variables

        val itemOnGrip = ItemCodex[(actor as Pocketed).inventory.itemEquipped.get(GameItem.EquipPosition.HAND_GRIP)]
        // bring up the UIs of the fixtures (e.g. crafting menu from a crafting table)
        var uiOpened = false

        val canPerformBarehandAction = actor.scale * actor.baseHitboxH >= actor.actorValue.getAsDouble(AVKey.BAREHAND_MINHEIGHT) ?: 4294967296.0

        // TODO actorsUnderMouse: support ROUNDWORLD
        val actorsUnderMouse: List<FixtureBase> = getActorsAt(Terrarum.mouseX, Terrarum.mouseY).filterIsInstance<FixtureBase>()
        if (actorsUnderMouse.size > 1) {
            App.printdbgerr(this, "Multiple fixtures at world coord ${Terrarum.mouseX}, ${Terrarum.mouseY}")
        }

        ////////////////////////////////


        // #1. Try to open a UI under the cursor
        // scan for the one with non-null UI.
        // what if there's multiple of such fixtures? whatever, you are supposed to DISALLOW such situation.
        if (itemOnGrip?.inventoryCategory != GameItem.Category.TOOL) { // don't open the UI when player's holding a tool
            for (kk in actorsUnderMouse.indices) {
                actorsUnderMouse[kk].mainUI?.let {
                    uiOpened = true

                    // property 'uiFixture' is a dedicated property that the TerrarumIngame recognises.
                    // when it's not null, the UI will be updated and rendered
                    // when the UI is closed, it'll be replaced with a null value
                    uiFixture = it

                    it.setPosition(0, 0)
                    it.setAsOpen()
                }
                break
            }
        }

        // #2. If there is no UI under and if I'm holding an item, use it
        // don't want to open the UI and use the item at the same time, would ya?
        if (!uiOpened && itemOnGrip != null) {
            val consumptionSuccessful = itemOnGrip.startPrimaryUse(actor, delta)
            if (consumptionSuccessful)
                (actor as Pocketed).inventory.consumeItem(itemOnGrip)
        }
        // #3. If I'm not holding any item and I can do barehandaction (size big enough that barehandactionminheight check passes), perform it
        else if (itemOnGrip == null && canPerformBarehandAction) {
            inInteractableRange(actor) {
                performBarehandAction(actor, delta)
                true
            }
        }
    }

    override fun worldPrimaryClickEnd(actor: ActorWithBody, delta: Float) {
        val canPerformBarehandAction = actor.scale * actor.baseHitboxH >= actor.actorValue.getAsDouble(AVKey.BAREHAND_MINHEIGHT) ?: 4294967296.0
        val itemOnGrip = (actor as Pocketed).inventory.itemEquipped.get(GameItem.EquipPosition.HAND_GRIP)
        ItemCodex[itemOnGrip]?.endPrimaryUse(actor, delta)

        if (canPerformBarehandAction) {
            actor.actorValue.set(AVKey.__ACTION_TIMER, 0.0)
        }
    }

    // I have decided that left and right clicks must do the same thing, so no secondary use from now on. --Torvald on 2019-05-26
    /*override fun worldSecondaryClickStart(delta: Float) {
        val itemOnGrip = actorNowPlaying?.inventory?.itemEquipped?.get(GameItem.EquipPosition.HAND_GRIP)
        val consumptionSuccessful = ItemCodex[itemOnGrip]?.startSecondaryUse(delta) ?: false
        if (consumptionSuccessful)
            actorNowPlaying?.inventory?.consumeItem(ItemCodex[itemOnGrip]!!)
    }

    override fun worldSecondaryClickEnd(delta: Float) {
        val itemOnGrip = actorNowPlaying?.inventory?.itemEquipped?.get(GameItem.EquipPosition.HAND_GRIP)
        ItemCodex[itemOnGrip]?.endSecondaryUse(delta)
    }*/



    private var firstTimeRun = true

    ///////////////
    // prod code //
    ///////////////
    private class ThreadIngameUpdate(val terrarumIngame: TerrarumIngame): Runnable {
        override fun run() {
            TODO()
        }
    }

    private var updateAkku = 0f
    private var autosaveTimer = 0f

    override fun render(`_`: Float) {
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

            gameFullyLoaded = true
        }


        ingameController.update()


        // define custom update rate
        val updateRate = if (KeyToggler.isOn(Input.Keys.APOSTROPHE)) 1f / 8f else App.UPDATE_RATE

        // ASYNCHRONOUS UPDATE AND RENDER //

        /** UPDATE CODE GOES HERE */
        val dt = Gdx.graphics.deltaTime
        updateAkku += dt
        autosaveTimer += dt

        var i = 0L
        while (updateAkku >= updateRate) {
            measureDebugTime("Ingame.Update") { updateGame(updateRate) }
            updateAkku -= updateRate
            i += 1
        }
        setDebugTime("Ingame.UpdateCounter", i)



        /** RENDER CODE GOES HERE */
        measureDebugTime("Ingame.Render") { renderGame() }

        val autosaveInterval = App.getConfigInt("autosaveinterval") / 1000f
        if (autosaveTimer >= autosaveInterval) {
            queueAutosave()
            autosaveTimer -= autosaveInterval
        }

    }

    private var worldWidth: Double = 0.0
    private var oldCamX = 0

    /**
     * Ingame (world) related updates; UI update must go to renderGame()
     */
    protected fun updateGame(delta: Float) {
        val world = this.world
        worldWidth = world.width.toDouble() * TILE_SIZE

        particlesActive = 0


        // synchronised Ingame Input Updater
        // will also queue up the block/wall/wire placed events
        ingameController.update()

        if (!paused || newWorldLoadedLatch) {

            //hypothetical_input_capturing_function_if_you_finally_decided_to_forgo_gdx_input_processor_and_implement_your_own_to_synchronise_everything()

            WorldSimulator.resetForThisFrame()


            ////////////////////////////
            // camera-related updates //
            ////////////////////////////
            FeaturesDrawer.update(delta)


            ///////////////////////////
            // actor-related updates //
            ///////////////////////////
            repossessActor()

            // determine whether the inactive actor should be activated
            wakeDormantActors()
            // determine whether the actor should keep being activated or be dormant
            killOrKnockdownActors()
            updateActors(delta)
            particlesContainer.forEach { if (!it.flagDespawn) particlesActive++; it.update(delta) }
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
                BlockStats.update()
            }
            // fill up visibleActorsRenderFront for wires, if:
            // 0. Camera wrapped
            // 1. new world has been loaded
            // 2. something is cued on the wire change queue
            // 3. wire renderclass changed
            if (Math.abs(WorldCamera.x - oldCamX) >= worldWidth * 0.85 ||
                newWorldLoadedLatch || wireChangeQueue.isNotEmpty() || selectedWireRenderClass != oldSelectedWireRenderClass) {
                measureDebugTime("Ingame.FillUpWiresBuffer") {
                    fillUpWiresBuffer()
                }
            }
            oldCamX = WorldCamera.x

        }

        if (!paused || newWorldLoadedLatch) {
            // completely consume block change queues because why not
            terrainChangeQueue.clear()
            wallChangeQueue.clear()
            wireChangeQueue.clear()

            oldSelectedWireRenderClass = selectedWireRenderClass
        }


        ////////////////////////
        // ui-related updates //
        ////////////////////////
        //uiContainer.forEach { it.update(delta) }
        //debugWindow.update(delta)
        //notifier.update(delta)
        // open/close fake blur UI according to what's opened
        if (uiInventoryPlayer.isVisible ||
            getUIFixture.get()?.isVisible == true) {
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

        if (!paused && newWorldLoadedLatch) newWorldLoadedLatch = false
    }


    private fun renderGame() {
        Gdx.graphics.setTitle(getCanonicalTitle())

        WorldCamera.update(world, actorNowPlaying)

        measureDebugTime("Ingame.FilterVisibleActors") {
            filterVisibleActors()
        }

        uiContainer.forEach {
            when (it) {
                is UICanvas -> it.update(Gdx.graphics.deltaTime)
                is Id_UICanvasNullable -> it.get()?.update(Gdx.graphics.deltaTime)
            }
        }
        //uiFixture?.update(Gdx.graphics.deltaTime)
        // deal with the uiFixture being closed
        if (uiFixture?.isClosed == true) { uiFixture = null }

        IngameRenderer.invoke(
                paused,
                screenZoom,
                visibleActorsRenderBehind,
                visibleActorsRenderMiddle,
                visibleActorsRenderMidTop,
                visibleActorsRenderFront,
                visibleActorsRenderOverlay,
                particlesContainer,
                actorNowPlaying,
                uiContainer// + uiFixture
        )
    }

    private val maxRenderableWires = ReferencingRanges.ACTORS_WIRES.endInclusive - ReferencingRanges.ACTORS_WIRES.first + 1
    private val wireActorsContainer = Array(maxRenderableWires) { WireActor(ReferencingRanges.ACTORS_WIRES.first + it).let {
        addNewActor(it)
        /*^let*/ it
    } }

    private fun fillUpWiresBuffer() {
        val for_y_start = (WorldCamera.y.toFloat() / TILE_SIZE).floorInt()
        val for_y_end = for_y_start + BlocksDrawer.tilesInVertical - 1

        val for_x_start = (WorldCamera.x.toFloat() / TILE_SIZE).floorInt()
        val for_x_end = for_x_start + BlocksDrawer.tilesInHorizontal - 1

        var wiringCounter = 0
        for (y in for_y_start..for_y_end) {
            for (x in for_x_start..for_x_end) {
                if (wiringCounter >= maxRenderableWires) break
                world.getAllWiresFrom(x, y)?.forEach {
                    val wireActor = wireActorsContainer[wiringCounter]

                    wireActor.setWire(it, x, y)

                    if (WireCodex[it].renderClass == selectedWireRenderClass || selectedWireRenderClass == "wire_render_all") {
                        wireActor.renderOrder = Actor.RenderOrder.OVERLAY
                    }
                    else {
                        wireActor.renderOrder = Actor.RenderOrder.BEHIND
                    }

                    wireActor.isUpdate = true
                    wireActor.isVisible = true
                    wireActor.forceDormant = false

                    wiringCounter += 1
                }

            }
        }

        for (i in wiringCounter until maxRenderableWires) {
            wireActorsContainer[i].isUpdate = false
            wireActorsContainer[i].isVisible = false
            wireActorsContainer[i].forceDormant = true
        }
    }

    private fun filterVisibleActors() {
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
        if (!theGameHasActor(actorNowPlaying)) {
            throw NoSuchActorWithRefException(newActor)
        }

        actorNowPlaying = newActor
        //WorldSimulator(actorNowPlaying, AppLoader.getSmoothDelta().toFloat())
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
            if (actor is ActorWithBody && actor.inUpdateRange(world) && !actor.forceDormant) {
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
            if (actor.flagDespawn) {
                removeActor(actor)
                actorContainerSize -= 1
                i-- // array removed 1 elem, so we also decrement counter by 1
            }
            // inactivate distant actors
            else if (actor is ActorWithBody && (!actor.inUpdateRange(world) || actor.forceDormant)) {
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
                                ItemCodex[inventoryEntry.itm]!!.effectWhenEquipped(it as ActorWithBody, delta)
                            }
                        }
                    }

                    if (it is CuedByTerrainChange) {
                        terrainChangeQueue.forEach { cue ->
                            printdbg(this, "Ingame actors terrainChangeCue: ${cue}")
                            it.updateForTerrainChange(cue)
                        }
                    }

                    if (it is CuedByWallChange) {
                        wallChangeQueue.forEach { cue ->
                            printdbg(this, "Ingame actors wallChangeCue: ${cue}")
                            it.updateForWallChange(cue)
                        }
                    }

                    if (it is CuedByWireChange) {
                        wireChangeQueue.forEach { cue ->
                            printdbg(this, "Ingame actors wireChangeCue: ${cue}")
                            it.updateForWireChange(cue)
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

        /*uiAutosaveNotifier.setAsOpen()
        makeSavegameBackupCopy()
        WriteSavegame.quick(savegameArchive, getSaveFileMain(), this, true) {
            uiAutosaveNotifier.setAsClose()

            debugTimers.put("Last Autosave Duration", System.nanoTime() - start)
        }*/
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
            Actor.RenderOrder.BEHIND -> visibleActorsRenderBehind
            Actor.RenderOrder.MIDDLE -> visibleActorsRenderMiddle
            Actor.RenderOrder.MIDTOP -> visibleActorsRenderMidTop
            Actor.RenderOrder.FRONT  -> visibleActorsRenderFront
            Actor.RenderOrder.OVERLAY-> visibleActorsRenderOverlay
        }
    }

    override fun removeActor(ID: Int) = removeActor(getActorByID(ID))
    /**
     * get index of the actor and delete by the index.
     * we can do this as the list is guaranteed to be sorted
     * and only contains unique values.
     *
     * Any values behind the index will be automatically pushed to front.
     * This is how remove function of [java.util.ArrayList] is defined.
     */
    override fun removeActor(actor: Actor?) {
        if (actor == null) return

//        if (actor.referenceID == actorGamer.referenceID || actor.referenceID == 0x51621D) // do not delete this magic
//            throw ProtectedActorRemovalException("Player")

        forceRemoveActor(actor)
    }

    override fun forceRemoveActor(actor: Actor) {
        arrayOf(actorContainerActive, actorContainerInactive).forEach { actorContainer ->
            val indexToDelete = actorContainer.searchForIndex(actor.referenceID) { it.referenceID }
            if (indexToDelete != null) {
                printdbg(this, "Removing actor $actor")
                printStackTrace(this)

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
    override fun addNewActor(actor: Actor?) {
        if (actor == null) return

        if (App.IS_DEVELOPMENT_BUILD && theGameHasActor(actor.referenceID)) {
            throw ReferencedActorAlreadyExistsException(actor)
        }
        else {
            if (actor.referenceID !in ReferencingRanges.ACTORS_WIRES && actor.referenceID !in ReferencingRanges.ACTORS_WIRES_HELPER) {
                printdbg(this, "Adding actor $actor")
                printStackTrace(this)
            }

            actorContainerActive.add(actor)
            if (actor is ActorWithBody) actorToRenderQueue(actor).add(actor)
        }
    }

    fun activateDormantActor(actor: Actor) {
        if (App.IS_DEVELOPMENT_BUILD && !isInactive(actor.referenceID)) {
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

    private fun insertionSortLastElemAV(arr: ArrayList<ActorWithBody>) { // out-projection doesn't work, duh
        ReentrantLock().lock {
            var j = arr.lastIndex - 1
            val x = arr.last()
            while (j >= 0 && arr[j] > x) {
                arr[j + 1] = arr[j]
                j -= 1
            }
            arr[j + 1] = x
        }
    }

    fun performBarehandAction(actor: ActorWithBody, delta: Float) {
//        println("whack!")

        fun getActorsAtVicinity(worldX: Double, worldY: Double, radius: Double): List<ActorWithBody> {
            val outList = java.util.ArrayList<ActorWithBody>()
            try {
                actorsRTree.find(worldX - radius, worldY - radius, worldX + radius, worldY + radius, outList)
            }
            catch (e: NullPointerException) {
            }
            return outList
        }


        val punchSize = actor.scale * actor.actorValue.getAsDouble(AVKey.BAREHAND_BASE_DIGSIZE)!!

        // if there are attackable actor (todo) on the "actor punch hitbox (todo)", attack them (todo)
        val actorsUnderMouse: List<ActorWithBody> = getActorsAtVicinity(Terrarum.mouseX, Terrarum.mouseY, punchSize / 2.0).filter { true }

        // else, punch a block
        val punchBlockSize = punchSize.div(TILE_SIZED).floorInt()
        if (punchBlockSize > 0) {
            PickaxeCore.startPrimaryUse(actor, delta, null, Terrarum.mouseTileX, Terrarum.mouseTileY, 1.0 / punchBlockSize, punchBlockSize, punchBlockSize)
        }
    }

    override fun pause() {
        paused = true
    }

    override fun resume() {
        paused = false
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

        if (gameInitialised) {
            //LightmapRenderer.fireRecalculateEvent()
        }


        if (gameFullyLoaded) {
            // resize UIs

            notifier.setPosition(
                    (drawWidth - notifier.width) / 2, App.scr.height - notifier.height)
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


        println("[Ingame] Resize event")
    }

    override fun dispose() {
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

        super.dispose()
    }
}
