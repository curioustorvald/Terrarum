package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.EMDASH
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.BlockPropUtil
import net.torvald.terrarum.blockstats.BlockStats
import net.torvald.terrarum.blockstats.MinimapComposer
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.console.Authenticator
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gamecontroller.IngameController
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.console.AVTracker
import net.torvald.terrarum.modulebasegame.console.ActorsList
import net.torvald.terrarum.modulebasegame.gameactors.*
import net.torvald.terrarum.modulebasegame.gameactors.physicssolver.CollisionSolver
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.gameworld.WorldSimulator
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen
import net.torvald.terrarum.modulebasegame.worldgenerator.WorldgenParams
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.TILE_SIZE
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.util.CircularArray
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.roundToInt


/**
 * Ingame instance for the game Terrarum.
 *
 * Created by minjaesong on 2017-06-16.
 */

open class TerrarumIngame(batch: SpriteBatch) : IngameInstance(batch) {

    private val ACTOR_UPDATE_RANGE = 4096

    var historicalFigureIDBucket: ArrayList<Int> = ArrayList<Int>()


    /**
     * list of Actors that is sorted by Actors' referenceID
     */
    //val ACTORCONTAINER_INITIAL_SIZE = 64
    val PARTICLES_MAX = AppLoader.getConfigInt("maxparticles")
    //val actorContainerActive = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    //val actorContainerInactive = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val particlesContainer = CircularArray<ParticleBase>(PARTICLES_MAX, true)
    val uiContainer = UIContainer()

    private val actorsRenderBehind = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderMiddle = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderMidTop = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderFront  = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderOverlay= ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)

    private var visibleActorsRenderBehind: List<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderMiddle: List<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderMidTop: List<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderFront: List<ActorWithBody> = ArrayList(1)
    private var visibleActorsRenderOverlay: List<ActorWithBody> = ArrayList(1)

    //var screenZoom = 1.0f   // definition moved to IngameInstance
    //val ZOOM_MAXIMUM = 4.0f // definition moved to IngameInstance
    //val ZOOM_MINIMUM = 0.5f // definition moved to IngameInstance

    companion object {
        /** Sets camera position so that (0,0) would be top-left of the screen, (width, height) be bottom-right. */
        fun setCameraPosition(batch: SpriteBatch, camera: Camera, newX: Float, newY: Float) {
            camera.position.set((-newX + AppLoader.halfScreenW).round(), (-newY + AppLoader.halfScreenH).round(), 0f)
            camera.update()
            batch.projectionMatrix = camera.combined
        }

        fun getCanonicalTitle() = AppLoader.GAME_NAME +
                                  " $EMDASH F: ${Gdx.graphics.framesPerSecond}" +
                                  if (AppLoader.IS_DEVELOPMENT_BUILD)
                                      " (ΔF${Terrarum.updateRateStr})" +
                                      " $EMDASH M: J${Terrarum.memJavaHeap}M / N${Terrarum.memNativeHeap}M / U${Terrarum.memUnsafe}M / X${Terrarum.memXmx}M"
                                  else
                                      ""
    }



    init {
    }



    lateinit var notifier: UICanvas

    lateinit var uiPieMenu: UICanvas
    lateinit var uiQuickBar: UICanvas
    lateinit var uiInventoryPlayer: UICanvas
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

    lateinit var uiInventoryContainer: UICanvas
    lateinit var uiVitalPrimary: UICanvas
    lateinit var uiVitalSecondary: UICanvas
    lateinit var uiVitalItem: UICanvas // itemcount/durability of held block or active ammo of held gun. As for the block, max value is 500.

    private val uiFixturesHistory = HashSet<UICanvas>()

    private lateinit var uiBasicInfo: UICanvas
    private lateinit var uiWatchTierOne: UICanvas

    /** For in-world text overlays? e.g. cursor on the ore block and tooltip will say "Malachite" or something */
    private lateinit var uiTooltip: UITooltip

    lateinit var uiCheatMotherfuckerNootNoot: UICheatDetected


    var particlesActive = 0
        private set



    private lateinit var ingameUpdateThread: ThreadIngameUpdate
    private lateinit var updateThreadWrapper: Thread
    //private val ingameDrawThread: ThreadIngameDraw // draw must be on the main thread


    override var gameInitialised = false
        internal set
    override var gameFullyLoaded = false
        internal set


    private val TILE_SIZEF = CreateTileAtlas.TILE_SIZE.toFloat()

    //////////////
    // GDX code //
    //////////////


    lateinit var gameLoadMode: GameLoadMode
    lateinit var gameLoadInfoPayload: Any
    lateinit var gameworld: GameWorldExtension
    lateinit var theRealGamer: IngamePlayer
        // get() = actorGamer as IngamePlayer



    enum class GameLoadMode {
        CREATE_NEW, LOAD_FROM
    }

    override fun show() {
        //initViewPort(AppLoader.screenW, AppLoader.screenH)

        // gameLoadMode and gameLoadInfoPayload must be set beforehand!!

        when (gameLoadMode) {
            GameLoadMode.CREATE_NEW -> enterCreateNewWorld(gameLoadInfoPayload as NewWorldParameters)
            GameLoadMode.LOAD_FROM  -> enterLoadFromSave(gameLoadInfoPayload as GameSaveData)
        }

        IngameRenderer.setRenderedWorld(gameworld)


        super.show() // gameInitialised = true
    }

    data class GameSaveData(
            val world: GameWorldExtension,
            val historicalFigureIDBucket: ArrayList<Int>,
            val realGamePlayer: IngamePlayer,
            val rogueS0: Long,
            val rogueS1: Long,
            val weatherS0: Long,
            val weatherS1: Long
    )

    data class NewWorldParameters(
            val width: Int,
            val height: Int,
            val worldGenSeed: Long
            // other worldgen options
    )

    private fun setTheRealGamerFirstTime(actor: IngamePlayer) {
        if (actor.referenceID != Terrarum.PLAYER_REF_ID) {
            throw Error()
        }

        actorNowPlaying = actor
        theRealGamer = actor
        addNewActor(actorNowPlaying)
    }

    /**
     * Init instance by loading saved world
     */
    private fun enterLoadFromSave(gameSaveData: GameSaveData) {
        if (gameInitialised) {
            printdbg(this, "loaded successfully.")
        }
        else {
            AppLoader.getLoadScreen().addMessage("Loading world from save")


            gameworld = gameSaveData.world
            world = gameworld
            historicalFigureIDBucket = gameSaveData.historicalFigureIDBucket
            setTheRealGamerFirstTime(gameSaveData.realGamePlayer)


            // set the randomisers right
            RoguelikeRandomiser.loadFromSave(gameSaveData.rogueS0, gameSaveData.rogueS1)
            WeatherMixer.loadFromSave(gameSaveData.weatherS0, gameSaveData.weatherS1)
        }
    }

    /**
     * Init instance by creating new world
     */
    private fun enterCreateNewWorld(worldParams: NewWorldParameters) {
        printdbg(this, "Ingame called")
        printStackTrace(this)

        if (gameInitialised) {
            printdbg(this, "loaded successfully.")
        }
        else {
            AppLoader.getLoadScreen().addMessage("${AppLoader.GAME_NAME} version ${AppLoader.getVERSION_STRING()}")
            AppLoader.getLoadScreen().addMessage("Creating new world")


            // init map as chosen size
            val timeNow = System.currentTimeMillis() / 1000
            gameworld = GameWorldExtension(1, worldParams.width, worldParams.height, timeNow, timeNow, 0) // new game, so the creation time is right now
            gameworldCount++
            world = gameworld

            // generate terrain for the map
            //WorldGenerator.attachMap(world)
            //WorldGenerator.SEED = worldParams.worldGenSeed
            //WorldGenerator.generateMap()
            Worldgen.attachMap(world, WorldgenParams(worldParams.worldGenSeed))
            Worldgen.generateMap()


            historicalFigureIDBucket = ArrayList<Int>()
        }

        KeyToggler.forceSet(Input.Keys.Q, false)
    }

    val ingameController = IngameController(this)

    /** Load rest of the game with GL context */
    fun postInit() {
        //setTheRealGamerFirstTime(PlayerBuilderSigrid())
        setTheRealGamerFirstTime(PlayerBuilderTestSubject1())



        MegaRainGovernor // invoke MegaRain Governor
        MinimapComposer // invoke MinimapComposer



        // make controls work
        Gdx.input.inputProcessor = ingameController
        if (AppLoader.gamepad != null) {
            ingameController.gamepad = AppLoader.gamepad
        }

        // init console window
        // TODO test put it on the IngameInstance.(init)
        //consoleHandler = ConsoleWindow()
        //consoleHandler.setPosition(0, 0)


        // init notifier
        notifier = Notification()
        notifier.setPosition(
                (AppLoader.screenW - notifier.width) / 2,
                AppLoader.screenH - notifier.height - AppLoader.getTvSafeGraphicsHeight()
        )




        // >- queue up game UIs that should pause the world -<
        uiInventoryPlayer = UIInventoryFull(actorNowPlaying!!,
                toggleKeyLiteral = AppLoader.getConfigInt("config_keyinventory"),
                toggleButtonLiteral = AppLoader.getConfigInt("config_gamepadstart")
        )
        uiInventoryPlayer.setPosition(0, 0)

        // >- lesser UIs -<
        // quick bar
        uiQuickBar = UIQuickslotBar()
        uiQuickBar.isVisible = true
        uiQuickBar.setPosition((AppLoader.screenW - uiQuickBar.width) / 2, AppLoader.getTvSafeGraphicsHeight())

        // pie menu
        uiPieMenu = UIQuickslotPie()
        uiPieMenu.setPosition(AppLoader.halfScreenW, AppLoader.halfScreenH)

        // vital metre
        // fill in getter functions by
        //      (uiAliases[UI_QUICK_BAR]!!.UI as UIVitalMetre).vitalGetterMax = { some_function }
        //uiVitalPrimary = UIVitalMetre(player, { 80f }, { 100f }, Color.red, 2, customPositioning = true)
        //uiVitalPrimary.setAsAlwaysVisible()
        //uiVitalSecondary = UIVitalMetre(player, { 73f }, { 100f }, Color(0x00dfff), 1) customPositioning = true)
        //uiVitalSecondary.setAsAlwaysVisible()
        //uiVitalItem = UIVitalMetre(player, { null }, { null }, Color(0xffcc00), 0, customPositioning = true)
        //uiVitalItem.setAsAlwaysVisible()

        uiWatchTierOne = UITierOneWatch(actorNowPlaying)
        uiWatchTierOne.setAsAlwaysVisible()
        uiWatchTierOne.setPosition(
                ((AppLoader.screenW - AppLoader.getTvSafeActionWidth()) - (uiQuickBar.posX + uiQuickBar.width) - uiWatchTierOne.width) / 2 + (uiQuickBar.posX + uiQuickBar.width),
                AppLoader.getTvSafeGraphicsHeight() + 8
        )

        // basic watch-style notification bar (temperature, new mail)
        uiBasicInfo = UIBasicInfo(actorNowPlaying)
        uiBasicInfo.setAsAlwaysVisible()
        uiBasicInfo.setPosition((uiQuickBar.posX - uiBasicInfo.width - AppLoader.getTvSafeActionWidth()) / 2 + AppLoader.getTvSafeActionWidth(), uiWatchTierOne.posY)


        uiTooltip = UITooltip()


        uiCheatMotherfuckerNootNoot = UICheatDetected()


        // batch-process uiAliases
        // NOTE: UIs that should pause the game (e.g. Inventory) must have relevant codes ON THEIR SIDE
        uiContainer.add(
                // drawn first
                //uiVitalPrimary,
                //uiVitalSecondary,
                //uiVitalItem,

                uiPieMenu,
                uiQuickBar,
                uiBasicInfo,
                uiWatchTierOne,
                UIScreenZoom(),
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

        AppLoader.setDebugTime("Ingame.updateCounter", 0)

        // some sketchy test code here



    }// END enter

    override fun worldPrimaryClickStart(delta: Float) {
        //println("[Ingame] worldPrimaryClickStart $delta")

        // bring up the UIs of the fixtures (e.g. crafting menu from a crafting table)
        var uiOpened = false

        // TODO actorsUnderMouse: support ROUNDWORLD
        val actorsUnderMouse: List<FixtureBase> = WorldSimulator.getActorsAt(Terrarum.mouseX, Terrarum.mouseY).filterIsInstance<FixtureBase>()
        if (actorsUnderMouse.size > 1) {
            AppLoader.printdbgerr(this, "Multiple fixtures at world coord ${Terrarum.mouseX}, ${Terrarum.mouseY}")
        }
        // scan for the one with non-null UI.
        // what if there's multiple of such fixtures? whatever, you are supposed to DISALLOW such situation.
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


        // don't want to open the UI and use the item at the same time, would ya?
        if (!uiOpened) {
            val itemOnGrip = actorNowPlaying?.inventory?.itemEquipped?.get(GameItem.EquipPosition.HAND_GRIP)
            val consumptionSuccessful = ItemCodex[itemOnGrip]?.startPrimaryUse(delta) ?: false
            if (consumptionSuccessful)
                actorNowPlaying?.inventory?.consumeItem(ItemCodex[itemOnGrip]!!)
        }
    }

    override fun worldPrimaryClickEnd(delta: Float) {
        val itemOnGrip = actorNowPlaying?.inventory?.itemEquipped?.get(GameItem.EquipPosition.HAND_GRIP)
        ItemCodex[itemOnGrip]?.endPrimaryUse(delta)
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

    private var updateAkku = 0.0

    override fun render(`_`: Float) {
        // Q&D solution for LoadScreen and Ingame, where while LoadScreen is working, Ingame now no longer has GL Context
        // there's still things to load which needs GL context to be present
        if (!gameFullyLoaded) {

            if (gameLoadMode == GameLoadMode.CREATE_NEW) {
                // go to spawn position
                actorNowPlaying?.setPosition(
                        world.spawnX * CreateTileAtlas.TILE_SIZE.toDouble(),
                        world.spawnY * CreateTileAtlas.TILE_SIZE.toDouble()
                )
            }

            postInit()


            gameFullyLoaded = true
        }

        // define custom update rate
        val updateRate = if (KeyToggler.isOn(Input.Keys.APOSTROPHE)) 1f / 8f else AppLoader.UPDATE_RATE

        // ASYNCHRONOUS UPDATE AND RENDER //

        /** UPDATE CODE GOES HERE */
        val dt = Gdx.graphics.rawDeltaTime
        updateAkku += dt

        var i = 0L
        while (updateAkku >= updateRate) {
            AppLoader.measureDebugTime("Ingame.update") { updateGame(updateRate) }
            updateAkku -= updateRate
            i += 1
        }
        AppLoader.setDebugTime("Ingame.updateCounter", i)



        /** RENDER CODE GOES HERE */
        AppLoader.measureDebugTime("Ingame.render") { renderGame() }
        AppLoader.setDebugTime("Ingame.render-Light",
                (AppLoader.debugTimers["Ingame.render"] as Long) - ((AppLoader.debugTimers["Renderer.LightTotal"] as? Long) ?: 0)
        )

    }

    private var worldWidth: Double = 0.0

    /**
     * Ingame (world) related updates; UI update must go to renderGame()
     */
    protected fun updateGame(delta: Float) {
        val world = this.world as GameWorldExtension
        worldWidth = world.width.toDouble() * TILE_SIZE

        particlesActive = 0


        if (!paused) {

            WorldSimulator.resetForThisFrame()

            ///////////////////////////
            // world-related updates //
            ///////////////////////////
            BlockPropUtil.dynamicLumFuncTickClock()
            world.updateWorldTime(delta)
            AppLoader.measureDebugTime("WorldSimulator.update") {
                WorldSimulator.invoke(actorNowPlaying, delta)
            }
            AppLoader.measureDebugTime("WeatherMixer.update") {
                WeatherMixer.update(delta, actorNowPlaying, world)
            }
            AppLoader.measureDebugTime("BlockStats.update") {
                BlockStats.update()
            }



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

            WorldCamera.update(gameworld, actorNowPlaying)

        }

        // world click events (e.g. opening the UI that a fixture has) must go here
        ingameController.update(delta)

        if (!paused) {

            // completely consume block change queues because why not
            terrainChangeQueue.clear()
            wallChangeQueue.clear()
            wireChangeQueue.clear()
        }


        ////////////////////////
        // ui-related updates //
        ////////////////////////
        //uiContainer.forEach { it.update(delta) }
        //debugWindow.update(delta)
        //notifier.update(delta)

        // update debuggers using javax.swing //
        if (Authenticator.b()) {
            AVTracker.update()
            ActorsList.update()
        }

        //println("paused = $paused")
    }


    private fun renderGame() {
        Gdx.graphics.setTitle(getCanonicalTitle())

        filterVisibleActors()
        uiContainer.forEach {
            when (it) {
                is UICanvas -> it.update(Gdx.graphics.rawDeltaTime)
                is Id_UICanvasNullable -> it.get()?.update(Gdx.graphics.rawDeltaTime)
            }
        }
        //uiFixture?.update(Gdx.graphics.rawDeltaTime)
        // deal with the uiFixture being closed
        if (uiFixture?.isClosed == true) { uiFixture = null }

        IngameRenderer.invoke(
                paused,
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

    private fun filterVisibleActors() {
        visibleActorsRenderBehind = actorsRenderBehind.filter { it.inScreen() }
        visibleActorsRenderMiddle = actorsRenderMiddle.filter { it.inScreen() }
        visibleActorsRenderMidTop = actorsRenderMidTop.filter { it.inScreen() }
        visibleActorsRenderFront  =  actorsRenderFront.filter { it.inScreen() }
        visibleActorsRenderOverlay=actorsRenderOverlay.filter { it.inScreen() }
    }

    private fun repossessActor() {
        // check if currently pocessed actor is removed from game
        if (!theGameHasActor(actorNowPlaying)) {
            // re-possess canonical player
            if (theGameHasActor(Terrarum.PLAYER_REF_ID))
                changePossession(Terrarum.PLAYER_REF_ID)
            else
                changePossession(0x51621D) // FIXME fallback debug mode (FIXME is there for a reminder visible in ya IDE)
        }
    }

    private fun changePossession(newActor: ActorHumanoid) {
        if (!theGameHasActor(actorNowPlaying)) {
            throw IllegalArgumentException("No such actor in the game: $newActor")
        }

        actorNowPlaying = newActor
        //WorldSimulator(actorNowPlaying, AppLoader.getSmoothDelta().toFloat())
    }

    private fun changePossession(refid: Int) {
        val actorToChange = getActorByID(refid)

        if (actorToChange !is ActorHumanoid) {
            throw Error("Unpossessable actor $refid: type expected ActorHumanoid, got ${actorToChange.javaClass.canonicalName}")
        }

        changePossession(getActorByID(refid) as ActorHumanoid)
    }

    /** Send message to notifier UI and toggle the UI as opened. */
    override fun sendNotification(messages: Array<String>) {
        (notifier as Notification).sendNotification(messages.toList())
    }

    override fun sendNotification(messages: List<String>) {
        (notifier as Notification).sendNotification(messages)
    }

    override fun sendNotification(singleMessage: String) = sendNotification(listOf(singleMessage))

    fun wakeDormantActors() {
        var actorContainerSize = actorContainerInactive.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainerInactive
            val actor = actorContainerInactive[i]
            if (actor is ActorWithBody && actor.inUpdateRange()) {
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
            else if (actor is ActorWithBody && !actor.inUpdateRange()) {
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
            ThreadExecutor.renew()

            val actors = actorContainerActive.size.toFloat()
            // set up indices
            for (i in 0..AppLoader.THREAD_COUNT - 1) {
                ThreadExecutor.submit(
                        ThreadActorUpdate(
                                actors.div(AppLoader.THREAD_COUNT).times(i).roundToInt(),
                                actors.div(AppLoader.THREAD_COUNT).times(i + 1).roundToInt() - 1
                        )
                )
            }

            ThreadExecutor.join()

            actorNowPlaying?.update(delta)
        }
        else {
            actorContainerActive.forEach {
                if (it != actorNowPlaying) {
                    it.update(delta)

                    if (it is Pocketed) {
                        it.inventory.forEach { inventoryEntry ->
                            ItemCodex[inventoryEntry.item]!!.effectWhileInPocket(delta)
                            if (it.equipped(inventoryEntry.item)) {
                                ItemCodex[inventoryEntry.item]!!.effectWhenEquipped(delta)
                            }
                        }
                    }

                    if (it is CuedByTerrainChange) {
                        terrainChangeQueue.forEach { cue ->
                            it.updateForWorldChange(cue)
                        }
                    }
                }
            }
            actorNowPlaying?.update(delta)
            //AmmoMeterProxy(player, uiVitalItem.UI as UIVitalMetre)
        }
    }

    fun Double.sqr() = this * this
    fun Int.sqr() = this * this
    fun min(vararg d: Double): Double {
        var ret = Double.MAX_VALUE
        d.forEach { if (it < ret) ret = it }
        return ret
    }
    private fun distToActorSqr(a: ActorWithBody, p: ActorWithBody) =
            min(// take min of normal position and wrapped (x < 0) position
                    (a.hitbox.centeredX - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                    ((a.hitbox.centeredX + world.width * CreateTileAtlas.TILE_SIZE) - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                    ((a.hitbox.centeredX - world.width * CreateTileAtlas.TILE_SIZE) - p.hitbox.centeredX).sqr() +
                        (a.hitbox.centeredY - p.hitbox.centeredY).sqr()
            )
    private fun distToCameraSqr(a: ActorWithBody) =
            min(
                    (a.hitbox.centeredX - WorldCamera.xCentre).sqr() +
                        (a.hitbox.centeredY - WorldCamera.yCentre).sqr(),
                    ((a.hitbox.centeredX + world.width * CreateTileAtlas.TILE_SIZE) - WorldCamera.xCentre).sqr() +
                        (a.hitbox.centeredY - WorldCamera.yCentre).sqr(),
                    ((a.hitbox.centeredX - world.width * CreateTileAtlas.TILE_SIZE) - WorldCamera.xCentre).sqr() +
                        (a.hitbox.centeredY - WorldCamera.yCentre).sqr()
            )

    /** whether the actor is within screen */
    private fun ActorWithBody.inScreen() =

            // y
            this.hitbox.endY >= WorldCamera.y && this.hitbox.startY <= WorldCamera.yEnd

            &&

            // x: camera is on the right side of the seam
            ((this.hitbox.endX - worldWidth >= WorldCamera.x && this.hitbox.startX - worldWidth <= WorldCamera.xEnd) ||
            // x: camera in on the left side of the seam
            (this.hitbox.endX + worldWidth >= WorldCamera.x && this.hitbox.startX + worldWidth <= WorldCamera.xEnd) ||
            // x: neither
            (this.hitbox.endX >= WorldCamera.x && this.hitbox.startX <= WorldCamera.xEnd))


    private val cameraWindowX = WorldCamera.x.toDouble()..WorldCamera.xEnd.toDouble()
    private val cameraWindowY = WorldCamera.y.toDouble()..WorldCamera.yEnd.toDouble()

    /** whether the actor is within update range */
    private fun ActorWithBody.inUpdateRange() = distToCameraSqr(this) <= ACTOR_UPDATE_RANGE.sqr()

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

        if (actor.referenceID == theRealGamer.referenceID || actor.referenceID == 0x51621D) // do not delete this magic
            throw RuntimeException("Attempted to remove player.")
        val indexToDelete = actorContainerActive.searchForIndex(actor.referenceID) { it.referenceID!! }
        if (indexToDelete != null) {
            printdbg(this, "Removing actor $actor")
            printStackTrace(this)

            actorContainerActive.removeAt(indexToDelete)

            // indexToDelete >= 0 means that the actor certainly exists in the game
            // which means we don't need to check if i >= 0 again
            if (actor is ActorWithBody) {
                when (actor.renderOrder) {
                    Actor.RenderOrder.BEHIND -> {
                        val i = actorsRenderBehind.binarySearch(actor.referenceID)
                        actorsRenderBehind.removeAt(i)
                    }
                    Actor.RenderOrder.MIDDLE -> {
                        val i = actorsRenderMiddle.binarySearch(actor.referenceID)
                        actorsRenderMiddle.removeAt(i)
                    }
                    Actor.RenderOrder.MIDTOP -> {
                        val i = actorsRenderMidTop.binarySearch(actor.referenceID)
                        actorsRenderMidTop.removeAt(i)
                    }
                    Actor.RenderOrder.FRONT  -> {
                        val i = actorsRenderFront.binarySearch(actor.referenceID)
                        actorsRenderFront.removeAt(i)
                    }
                    Actor.RenderOrder.OVERLAY-> {
                        val i = actorsRenderOverlay.binarySearch(actor.referenceID)
                        actorsRenderFront.removeAt(i)
                    }
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

        if (AppLoader.IS_DEVELOPMENT_BUILD && theGameHasActor(actor.referenceID)) {
            throw Error("The actor $actor already exists in the game")
        }
        else {
            printdbg(this, "Adding actor $actor")
            printStackTrace(this)

            actorContainerActive.add(actor)

            if (actor is ActorWithBody) {
                when (actor.renderOrder) {
                    Actor.RenderOrder.BEHIND -> {
                        actorsRenderBehind.add(actor); insertionSortLastElemAV(actorsRenderBehind)
                    }
                    Actor.RenderOrder.MIDDLE -> {
                        actorsRenderMiddle.add(actor); insertionSortLastElemAV(actorsRenderMiddle)
                    }
                    Actor.RenderOrder.MIDTOP -> {
                        actorsRenderMidTop.add(actor); insertionSortLastElemAV(actorsRenderMidTop)
                    }
                    Actor.RenderOrder.FRONT  -> {
                        actorsRenderFront.add(actor); insertionSortLastElemAV(actorsRenderFront)
                    }
                    Actor.RenderOrder.OVERLAY-> {
                        actorsRenderOverlay.add(actor); insertionSortLastElemAV(actorsRenderOverlay)
                    }
                }
            }
        }
    }

    fun activateDormantActor(actor: Actor) {
        if (AppLoader.IS_DEVELOPMENT_BUILD && !isInactive(actor.referenceID)) {
            if (isActive(actor.referenceID))
                throw Error("The actor $actor is already activated")
            else
                throw Error("The actor $actor already exists in the game")
        }
        else {
            actorContainerInactive.remove(actor)
            actorContainerActive.add(actor)

            if (actor is ActorWithBody) {
                when (actor.renderOrder) {
                    Actor.RenderOrder.BEHIND -> {
                        actorsRenderBehind.add(actor); insertionSortLastElemAV(actorsRenderBehind)
                    }
                    Actor.RenderOrder.MIDDLE -> {
                        actorsRenderMiddle.add(actor); insertionSortLastElemAV(actorsRenderMiddle)
                    }
                    Actor.RenderOrder.MIDTOP -> {
                        actorsRenderMidTop.add(actor); insertionSortLastElemAV(actorsRenderMidTop)
                    }
                    Actor.RenderOrder.FRONT  -> {
                        actorsRenderFront.add(actor); insertionSortLastElemAV(actorsRenderFront)
                    }
                    Actor.RenderOrder.OVERLAY-> {
                        actorsRenderOverlay.add(actor); insertionSortLastElemAV(actorsRenderOverlay)
                    }
                }
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

    fun setTooltipMessage(message: String?) {
        if (message == null) {
            uiTooltip.setAsClose()
        }
        else {
            if (uiTooltip.isClosed || uiTooltip.isClosing) {
                uiTooltip.setAsOpen()
            }
            uiTooltip.message = message
        }
    }

    override fun pause() {
        // TODO no pause when off-focus on desktop
    }

    override fun resume() {
    }

    override fun hide() {
        uiContainer.forEach { it?.handler?.dispose() }
    }


    /**
     * @param width same as AppLoader.screenW
     * @param height same as AppLoader.screenH
     * @see net.torvald.terrarum.Terrarum
     */
    override fun resize(width: Int, height: Int) {

        // FIXME debugger is pointing at this thing, not sure it actually caused memleak
        //MegaRainGovernor.resize()


        IngameRenderer.resize(AppLoader.screenW, AppLoader.screenH)


        if (gameInitialised) {
            //LightmapRenderer.fireRecalculateEvent()
        }


        if (gameFullyLoaded) {
            // resize UIs

            notifier.setPosition(
                    (AppLoader.screenW - notifier.width) / 2, AppLoader.screenH - notifier.height)
            uiQuickBar.setPosition((AppLoader.screenW - uiQuickBar.width) / 2, AppLoader.getTvSafeGraphicsHeight())

            // inventory
            /*uiInventoryPlayer =
                    UIInventory(player,
                            width = 840,
                            height = AppLoader.screenH - 160,
                            categoryWidth = 210
                    )*/


            // basic watch-style notification bar (temperature, new mail)
            uiBasicInfo.setPosition(AppLoader.screenW - uiBasicInfo.width, 0)
            uiWatchTierOne.setPosition(
                    ((AppLoader.screenW - AppLoader.getTvSafeGraphicsWidth()) - (uiQuickBar.posX + uiQuickBar.width) - uiWatchTierOne.width) / 2 + (uiQuickBar.posX + uiQuickBar.width),
                    AppLoader.getTvSafeGraphicsHeight() + 8
            )

        }


        println("[Ingame] Resize event")
    }

    override fun dispose() {
        actorsRenderBehind.forEach { it.dispose() }
        actorsRenderMiddle.forEach { it.dispose() }
        actorsRenderMidTop.forEach { it.dispose() }
        actorsRenderFront.forEach { it.dispose() }
        actorsRenderOverlay.forEach { it.dispose() }

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
