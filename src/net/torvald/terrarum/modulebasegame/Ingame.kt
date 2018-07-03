package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer

import net.torvald.dataclass.CircularArray
import net.torvald.terrarum.blockproperties.BlockPropUtil
import net.torvald.terrarum.blockstats.BlockStats
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.console.*
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.modulebasegame.gameactors.physicssolver.CollisionSolver
import net.torvald.terrarum.gamecontroller.IngameController
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameworld.WorldSimulator
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera

import java.util.ArrayList
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import javax.swing.JOptionPane

import com.badlogic.gdx.graphics.OrthographicCamera
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.console.AVTracker
import net.torvald.terrarum.modulebasegame.console.ActorsList
import net.torvald.terrarum.console.Authenticator
import net.torvald.terrarum.console.SetGlobalLightOverride
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.gameactors.*
import net.torvald.terrarum.modulebasegame.imagefont.Watch7SegMain
import net.torvald.terrarum.modulebasegame.imagefont.WatchDotAlph
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulebasegame.worldgenerator.WorldGenerator
import kotlin.system.measureNanoTime


/**
 * Created by minjaesong on 2017-06-16.
 */

class Ingame(batch: SpriteBatch) : IngameInstance(batch) {


    private val ACTOR_UPDATE_RANGE = 4096

    lateinit var world: GameWorld
    lateinit var historicalFigureIDBucket: ArrayList<Int>

    /**
     * list of Actors that is sorted by Actors' referenceID
     */
    //val ACTORCONTAINER_INITIAL_SIZE = 64
    val PARTICLES_MAX = Terrarum.getConfigInt("maxparticles")
    //val actorContainer = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    //val actorContainerInactive = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val particlesContainer = CircularArray<ParticleBase>(PARTICLES_MAX)
    val uiContainer = ArrayList<UICanvas>()

    private val actorsRenderBehind = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderMiddle = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderMidTop = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderFront  = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)

    lateinit var playableActorDelegate: PlayableActorDelegate // player must exist; use dummy player if there is none (required for camera)
        private set
    inline val player: ActorHumanoid // currently POSSESSED actor :)
        get() = playableActorDelegate.actor

    //var screenZoom = 1.0f   // definition moved to IngameInstance
    //val ZOOM_MAXIMUM = 4.0f // definition moved to IngameInstance
    //val ZOOM_MINIMUM = 0.5f // definition moved to IngameInstance

    companion object {
        //val lightmapDownsample = 4f //2f: still has choppy look when the camera moves but unnoticeable when blurred


        /** Sets camera position so that (0,0) would be top-left of the screen, (width, height) be bottom-right. */
        fun setCameraPosition(batch: SpriteBatch, camera: Camera, newX: Float, newY: Float) {
            camera.position.set((-newX + Terrarum.HALFW).round(), (-newY + Terrarum.HALFH).round(), 0f)
            camera.update()
            batch.projectionMatrix = camera.combined
        }



        /**
         * Usage:
         *
         * override var referenceID: Int = generateUniqueReferenceID()
         */
        fun generateUniqueReferenceID(renderOrder: Actor.RenderOrder): ActorID {
            fun hasCollision(value: ActorID) =
                    try {
                        Terrarum.ingame!!.theGameHasActor(value) ||
                        value < ItemCodex.ACTORID_MIN ||
                        value !in when (renderOrder) {
                            Actor.RenderOrder.BEHIND -> Actor.RANGE_BEHIND
                            Actor.RenderOrder.MIDDLE -> Actor.RANGE_MIDDLE
                            Actor.RenderOrder.MIDTOP -> Actor.RANGE_MIDTOP
                            Actor.RenderOrder.FRONT  -> Actor.RANGE_FRONT
                        }
                    }
                    catch (gameNotInitialisedException: KotlinNullPointerException) {
                        false
                    }

            var ret: Int
            do {
                ret = HQRNG().nextInt().and(0x7FFFFFFF) // set new ID
            } while (hasCollision(ret)) // check for collision
            return ret
        }
    }



    init {
    }



    private val useShader: Boolean = false
    private val shaderProgram = 0

    val KEY_LIGHTMAP_RENDER = Input.Keys.F7



    lateinit var debugWindow: UICanvas
    lateinit var notifier: UICanvas

    lateinit var uiPieMenu: UICanvas
    lateinit var uiQuickBar: UICanvas
    lateinit var uiInventoryPlayer: UICanvas
    lateinit var uiInventoryContainer: UICanvas
    lateinit var uiVitalPrimary: UICanvas
    lateinit var uiVitalSecondary: UICanvas
    lateinit var uiVitalItem: UICanvas // itemcount/durability of held block or active ammo of held gun. As for the block, max value is 500.

    private lateinit var uiWatchBasic: UICanvas
    private lateinit var uiWatchTierOne: UICanvas

    private lateinit var uiTooltip: UITooltip

    lateinit var uiCheatMotherfuckerNootNoot: UICheatDetected

    // UI aliases
    lateinit var uiAliases: ArrayList<UICanvas>
        private set
    lateinit var uiAlasesPausing: ArrayList<UICanvas>
        private set

    inline val paused: Boolean
        get() = uiAlasesPausing.map { if (it.isOpened) return true else 0 }.isEmpty() // isEmply is always false, which we want
    /**
     * Set to false if UI is opened; set to true  if UI is closed.
     */
    inline val canPlayerControl: Boolean
        get() = !paused // FIXME temporary behab (block movement if the game is paused or paused by UIs)

    var particlesActive = 0
        private set



    private lateinit var ingameUpdateThread: ThreadIngameUpdate
    private lateinit var updateThreadWrapper: Thread
    //private val ingameDrawThread: ThreadIngameDraw // draw must be on the main thread


    var gameInitialised = false
        private set
    var gameFullyLoaded = false
        private set


    private val TILE_SIZEF = FeaturesDrawer.TILE_SIZE.toFloat()

    //////////////
    // GDX code //
    //////////////


    lateinit var gameLoadMode: GameLoadMode
    lateinit var gameLoadInfoPayload: Any

    enum class GameLoadMode {
        CREATE_NEW, LOAD_FROM
    }

    override fun show() {
        //initViewPort(Terrarum.WIDTH, Terrarum.HEIGHT)

        // gameLoadMode and gameLoadInfoPayload must be set beforehand!!

        when (gameLoadMode) {
            GameLoadMode.CREATE_NEW -> enter(gameLoadInfoPayload as NewWorldParameters)
            GameLoadMode.LOAD_FROM  -> enter(gameLoadInfoPayload as GameSaveData)
        }

        LightmapRenderer.world = this.world
        //BlocksDrawer.world = this.world
        FeaturesDrawer.world = this.world

        gameInitialised = true
    }

    data class GameSaveData(
            val world: GameWorld,
            val historicalFigureIDBucket: ArrayList<Int>,
            val realGamePlayer: ActorHumanoid
    )

    data class NewWorldParameters(
            val width: Int,
            val height: Int,
            val worldGenSeed: Long
            // other worldgen options
    )

    /**
     * Init instance by loading saved world
     */
    private fun enter(gameSaveData: GameSaveData) {
        if (gameInitialised) {
            println("[Ingame] loaded successfully.")
        }
        else {
            LoadScreen.addMessage("Loading world from save")


            world = gameSaveData.world
            historicalFigureIDBucket = gameSaveData.historicalFigureIDBucket
            playableActorDelegate = PlayableActorDelegate(gameSaveData.realGamePlayer)
            addNewActor(player)



            //initGame()
        }
    }

    /**
     * Init instance by creating new world
     */
    private fun enter(worldParams: NewWorldParameters) {
        if (gameInitialised) {
            println("[Ingame] loaded successfully.")
        }
        else {
            LoadScreen.addMessage("${Terrarum.NAME} version ${AppLoader.getVERSION_STRING()}")
            LoadScreen.addMessage("Creating new world")


            // init map as chosen size
            world = GameWorld(worldParams.width, worldParams.height)

            // generate terrain for the map
            WorldGenerator.attachMap(world)
            WorldGenerator.SEED = worldParams.worldGenSeed
            WorldGenerator.generateMap()


            historicalFigureIDBucket = ArrayList<Int>()


            RoguelikeRandomiser.seed = HQRNG().nextLong()


            // add new player and put it to actorContainer
            //playableActorDelegate = PlayableActorDelegate(PlayerBuilderSigrid())
            //playableActorDelegate = PlayableActorDelegate(PlayerBuilderTestSubject1())
            //addNewActor(player)


            // test actor
            //addNewActor(PlayerBuilderCynthia())


            //initGame()
        }
    }

    private val ingameController = IngameController(this)

    /** Load rest of the game with GL context */
    fun postInit() {
        //LightmapRenderer.world = this.world
        BlocksDrawer.world = this.world
        //FeaturesDrawer.world = this.world


        MegaRainGovernor // invoke MegaRain Governor



        Gdx.input.inputProcessor = ingameController



        // init console window
        consoleHandler = ConsoleWindow()
        consoleHandler.setPosition(0, 0)


        // init debug window
        debugWindow = BasicDebugInfoWindow()
        debugWindow.setPosition(0, 0)

        // init notifier
        notifier = Notification()
        notifier.setPosition(
                (Terrarum.WIDTH - notifier.width) / 2, Terrarum.HEIGHT - notifier.height)




        // >- queue up game UIs that should pause the world -<
        // inventory
        /*uiInventoryPlayer = UIInventory(player,
                width = 900,
                height = Terrarum.HEIGHT - 160,
                categoryWidth = 210,
                toggleKeyLiteral = Terrarum.getConfigInt("keyinventory")
        )*/
        /*uiInventoryPlayer.setPosition(
                -uiInventoryPlayer.width,
                70
        )*/
        uiInventoryPlayer = UIInventoryFull(player,
                toggleKeyLiteral = Terrarum.getConfigInt("keyinventory")
        )
        uiInventoryPlayer.setPosition(0, 0)

        // >- lesser UIs -<
        // quick bar
        uiQuickBar = UIQuickBar()
        uiQuickBar.isVisible = true
        uiQuickBar.setPosition((Terrarum.WIDTH - uiQuickBar.width) / 2 + 12, -10)

        // pie menu
        uiPieMenu = UIPieMenu()
        uiPieMenu.setPosition(Terrarum.HALFW, Terrarum.HALFH)

        // vital metre
        // fill in getter functions by
        //      (uiAliases[UI_QUICK_BAR]!!.UI as UIVitalMetre).vitalGetterMax = { some_function }
        //uiVitalPrimary = UIVitalMetre(player, { 80f }, { 100f }, Color.red, 2, customPositioning = true)
        //uiVitalPrimary.setAsAlwaysVisible()
        //uiVitalSecondary = UIVitalMetre(player, { 73f }, { 100f }, Color(0x00dfff), 1) customPositioning = true)
        //uiVitalSecondary.setAsAlwaysVisible()
        //uiVitalItem = UIVitalMetre(player, { null }, { null }, Color(0xffcc00), 0, customPositioning = true)
        //uiVitalItem.setAsAlwaysVisible()

        // basic watch-style notification bar (temperature, new mail)
        uiWatchBasic = UIBasicNotifier(player)
        uiWatchBasic.setAsAlwaysVisible()
        uiWatchBasic.setPosition(Terrarum.WIDTH - uiWatchBasic.width, 0)

        uiWatchTierOne = UITierOneWatch(player)
        uiWatchTierOne.setAsAlwaysVisible()
        uiWatchTierOne.setPosition(Terrarum.WIDTH - uiWatchTierOne.width, uiWatchBasic.height - 2)


        uiTooltip = UITooltip()


        uiCheatMotherfuckerNootNoot = UICheatDetected()


        // batch-process uiAliases
        uiAliases = arrayListOf(
                // drawn first
                //uiVitalPrimary,
                //uiVitalSecondary,
                //uiVitalItem,

                uiPieMenu,
                uiQuickBar,
                uiWatchBasic,
                uiWatchTierOne,
                uiTooltip
                // drawn last
        )
        uiAlasesPausing = arrayListOf(
                uiInventoryPlayer,
                //uiInventoryContainer,
                consoleHandler,
                uiCheatMotherfuckerNootNoot
        )
        uiAlasesPausing.forEach { addUI(it) } // put them all to the UIContainer
        uiAliases.forEach { addUI(it) } // put them all to the UIContainer



        ingameUpdateThread = ThreadIngameUpdate(this)
        updateThreadWrapper = Thread(ingameUpdateThread, "Terrarum UpdateThread")



        // these need to appear on top of any others
        uiContainer.add(notifier)
        uiContainer.add(debugWindow)


        LightmapRenderer.fireRecalculateEvent()





        // some sketchy test code here



    }// END enter


    protected var updateDeltaCounter = 0.0
    protected val updateRate = 1.0 / Terrarum.TARGET_INTERNAL_FPS

    private var firstTimeRun = true

    ///////////////
    // prod code //
    ///////////////
    private class ThreadIngameUpdate(val ingame: Ingame): Runnable {
        override fun run() {
            var updateTries = 0
            while (ingame.updateDeltaCounter >= ingame.updateRate) {
                ingame.updateGame(Terrarum.deltaTime)
                ingame.updateDeltaCounter -= ingame.updateRate
                updateTries++

                if (updateTries >= Terrarum.UPDATE_CATCHUP_MAX_TRIES) {
                    break
                }
            }
        }
    }

    override fun render(delta: Float) {
        // Q&D solution for LoadScreen and Ingame, where while LoadScreen is working, Ingame now no longer has GL Context
        // there's still things to load which needs GL context to be present
        if (!gameFullyLoaded) {

            if (gameLoadMode == GameLoadMode.CREATE_NEW) {
                playableActorDelegate = PlayableActorDelegate(PlayerBuilderSigrid())

                // go to spawn position
                player.setPosition(
                        world.spawnX * FeaturesDrawer.TILE_SIZE.toDouble(),
                        world.spawnY * FeaturesDrawer.TILE_SIZE.toDouble()
                )

                addNewActor(player)
            }

            postInit()


            gameFullyLoaded = true
        }





        Gdx.graphics.setTitle(AppLoader.GAME_NAME +
                              " — F: ${Gdx.graphics.framesPerSecond} (${Terrarum.TARGET_INTERNAL_FPS})" +
                              " — M: ${Terrarum.memInUse}M / ${Terrarum.memTotal}M / ${Terrarum.memXmx}M"
        )

        // ASYNCHRONOUS UPDATE AND RENDER //


        /** UPDATE CODE GOES HERE */
        updateDeltaCounter += delta



        if (false && Terrarum.getConfigBoolean("multithread")) { // NO MULTITHREADING: camera don't like concurrent modification (jittery actor movements)
            if (firstTimeRun || updateThreadWrapper.state == Thread.State.TERMINATED) {
                updateThreadWrapper = Thread(ingameUpdateThread, "Terrarum UpdateThread")
                updateThreadWrapper.start()

                if (firstTimeRun) firstTimeRun = false
            }
            // else, NOP;
        }
        else {
            var updateTries = 0
            while (updateDeltaCounter >= updateRate) {

                //updateGame(delta)
                Terrarum.debugTimers["Ingame.update"] = measureNanoTime { updateGame(delta) }

                updateDeltaCounter -= updateRate
                updateTries++

                if (updateTries >= Terrarum.UPDATE_CATCHUP_MAX_TRIES) {
                    break
                }
            }
        }



        /** RENDER CODE GOES HERE */
        //renderGame(batch)
        Terrarum.debugTimers["Ingame.render"] = measureNanoTime { renderGame() }
    }

    protected fun updateGame(delta: Float) {
        particlesActive = 0


        KeyToggler.update()
        ingameController.update(delta)


        if (!paused) {

            ///////////////////////////
            // world-related updates //
            ///////////////////////////
            BlockPropUtil.dynamicLumFuncTickClock()
            world.updateWorldTime(delta)
            //WorldSimulator(player, delta)
            WeatherMixer.update(delta, player)
            BlockStats.update()
            if (!(CommandDict["setgl"] as SetGlobalLightOverride).lightOverride)
                world.globalLight = WeatherMixer.globalLightNow


            ////////////////////////////
            // camera-related updates //
            ////////////////////////////
            FeaturesDrawer.update(delta)
            WorldCamera.update(world, player)



            ///////////////////////////
            // actor-related updates //
            ///////////////////////////
            repossessActor()

            // determine whether the inactive actor should be activated
            wakeDormantActors()
            // determine whether the actor should keep being activated or be dormant
            KillOrKnockdownActors()
            updateActors(delta)
            particlesContainer.forEach { if (!it.flagDespawn) particlesActive++; it.update(delta) }
            // TODO thread pool(?)
            CollisionSolver.process()
        }


        ////////////////////////
        // ui-related updates //
        ////////////////////////
        uiContainer.forEach { it.update(delta) }
        //debugWindow.update(delta)
        //notifier.update(delta)

        // update debuggers using javax.swing //
        if (Authenticator.b()) {
            AVTracker.update()
            ActorsList.update()
        }
    }


    private fun renderGame() {
        IngameRenderer.invoke(
                world,
                actorsRenderBehind,
                actorsRenderMiddle,
                actorsRenderMidTop,
                actorsRenderFront,
                particlesContainer,
                player,
                uiContainer
        )
    }


    private fun repossessActor() {
        // check if currently pocessed actor is removed from game
        if (!theGameHasActor(player)) {
            // re-possess canonical player
            if (theGameHasActor(Player.PLAYER_REF_ID))
                changePossession(Player.PLAYER_REF_ID)
            else
                changePossession(0x51621D) // FIXME fallback debug mode (FIXME is there for a reminder visible in ya IDE)
        }
    }

    private fun changePossession(newActor: PlayableActorDelegate) {
        if (!theGameHasActor(player)) {
            throw IllegalArgumentException("No such actor in the game: $newActor")
        }

        playableActorDelegate = newActor
        WorldSimulator(player, Terrarum.deltaTime)
    }

    private fun changePossession(refid: Int) {
        // TODO prevent possessing other player on multiplayer

        if (!theGameHasActor(refid)) {
            throw IllegalArgumentException(
                    "No such actor in the game: $refid (elemsActive: ${actorContainer.size}, " +
                    "elemsInactive: ${actorContainerInactive.size})")
        }

        // take care of old delegate
        playableActorDelegate!!.actor.collisionType = HumanoidNPC.DEFAULT_COLLISION_TYPE
        // accept new delegate
        playableActorDelegate = PlayableActorDelegate(getActorByID(refid) as ActorHumanoid)
        playableActorDelegate!!.actor.collisionType = ActorWithPhysics.COLLISION_KINEMATIC
        WorldSimulator(player, Terrarum.deltaTime)
    }

    /** Send message to notifier UI and toggle the UI as opened. */
    fun sendNotification(msg: Array<String>) {
        (notifier as Notification).sendNotification(msg)
    }

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
    fun KillOrKnockdownActors() {
        var actorContainerSize = actorContainer.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainer
            val actor = actorContainer[i]
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
                actorContainer.removeAt(actorIndex)
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
        if (false) { // don't multithread this for now, it's SLOWER //if (Terrarum.MULTITHREAD && actorContainer.size > Terrarum.THREADS) {
            val actors = actorContainer.size.toFloat()
            // set up indices
            for (i in 0..Terrarum.THREADS - 1) {
                ThreadParallel.map(
                        i,
                        ThreadActorUpdate(
                                actors.div(Terrarum.THREADS).times(i).roundInt(),
                                actors.div(Terrarum.THREADS).times(i.plus(1)).roundInt() - 1
                        ),
                        "ActorUpdate"
                )
            }

            ThreadParallel.startAll()

            playableActorDelegate?.update(delta)
        }
        else {
            actorContainer.forEach {
                if (it != playableActorDelegate?.actor) {
                    it.update(delta)

                    if (it is Pocketed) {
                        it.inventory.forEach { inventoryEntry ->
                            inventoryEntry.item.effectWhileInPocket(delta)
                            if (it.equipped(inventoryEntry.item)) {
                                inventoryEntry.item.effectWhenEquipped(delta)
                            }
                        }
                    }
                }
            }
            playableActorDelegate?.update(delta)
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
                    (a.hitbox.centeredX - p.hitbox.centeredX + world.width * FeaturesDrawer.TILE_SIZE).sqr() +
                    (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                    (a.hitbox.centeredX - p.hitbox.centeredX - world.width * FeaturesDrawer.TILE_SIZE).sqr() +
                    (a.hitbox.centeredY - p.hitbox.centeredY).sqr()
            )
    private fun distToCameraSqr(a: ActorWithBody) =
            min(
                    (a.hitbox.startX - WorldCamera.x).sqr() +
                    (a.hitbox.startY - WorldCamera.y).sqr(),
                    (a.hitbox.startX - WorldCamera.x + world.width * FeaturesDrawer.TILE_SIZE).sqr() +
                    (a.hitbox.startY - WorldCamera.y).sqr(),
                    (a.hitbox.startX - WorldCamera.x - world.width * FeaturesDrawer.TILE_SIZE).sqr() +
                    (a.hitbox.startY - WorldCamera.y).sqr()
            )

    /** whether the actor is within screen */
    private fun ActorWithBody.inScreen() =
            distToCameraSqr(this) <=
            (Terrarum.WIDTH.plus(this.hitbox.width.div(2)).
                    times(1 / screenZoom).sqr() +
             Terrarum.HEIGHT.plus(this.hitbox.height.div(2)).
                     times(1 / screenZoom).sqr())


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
    override fun removeActor(actor: Actor) {
        if (actor.referenceID == player.referenceID || actor.referenceID == 0x51621D) // do not delete this magic
            throw RuntimeException("Attempted to remove player.")
        val indexToDelete = actorContainer.binarySearch(actor.referenceID!!)
        if (indexToDelete >= 0) {
            actorContainer.removeAt(indexToDelete)

            // indexToDelete >= 0 means that the actor certainly exists in the game
            // which means we don't need to check if i >= 0 again
            if (actor is ActorWithBody) {
                when (actor.renderOrder) {
                    Actor.RenderOrder.BEHIND -> {
                        val i = actorsRenderBehind.binarySearch(actor.referenceID!!)
                        actorsRenderBehind.removeAt(i)
                    }
                    Actor.RenderOrder.MIDDLE -> {
                        val i = actorsRenderMiddle.binarySearch(actor.referenceID!!)
                        actorsRenderMiddle.removeAt(i)
                    }
                    Actor.RenderOrder.MIDTOP -> {
                        val i = actorsRenderMidTop.binarySearch(actor.referenceID!!)
                        actorsRenderMidTop.removeAt(i)
                    }
                    Actor.RenderOrder.FRONT  -> {
                        val i = actorsRenderFront.binarySearch(actor.referenceID!!)
                        actorsRenderFront.removeAt(i)
                    }
                }
            }
        }
    }

    /**
     * Check for duplicates, append actor and sort the list
     */
    override fun addNewActor(actor: Actor) {
        if (theGameHasActor(actor.referenceID!!)) {
            throw Error("The actor $actor already exists in the game")
        }
        else {
            actorContainer.add(actor)
            insertionSortLastElem(actorContainer) // we can do this as we are only adding single actor

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
                }
            }
        }
    }

    fun activateDormantActor(actor: Actor) {
        if (!isInactive(actor.referenceID!!)) {
            if (isActive(actor.referenceID!!))
                throw Error("The actor $actor is already activated")
            else
                throw Error("The actor $actor already exists in the game")
        }
        else {
            actorContainerInactive.remove(actor)
            actorContainer.add(actor)
            insertionSortLastElem(actorContainer) // we can do this as we are only adding single actor

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
                }
            }
        }
    }

    fun addParticle(particle: ParticleBase) {
        particlesContainer.add(particle)
    }

    fun addUI(ui: UICanvas) {
        // check for exact duplicates
        if (uiContainer.contains(ui)) {
            throw IllegalArgumentException(
                    "Exact copy of the UI already exists: The instance of ${ui.javaClass.simpleName}"
            )
        }

        uiContainer.add(ui)
    }

    private fun insertionSortLastElemAV(arr: ArrayList<ActorWithBody>) { // out-projection doesn't work, duh
        lock(ReentrantLock()) {
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
        dispose()
    }


    /**
     * @param width same as Terrarum.WIDTH
     * @param height same as Terrarum.HEIGHT
     * @see net.torvald.terrarum.Terrarum
     */
    override fun resize(width: Int, height: Int) {

        BlocksDrawer.resize(Terrarum.WIDTH, Terrarum.HEIGHT)
        LightmapRenderer.resize(Terrarum.WIDTH, Terrarum.HEIGHT)
        MegaRainGovernor.resize()


        IngameRenderer.resize(Terrarum.WIDTH, Terrarum.HEIGHT)


        if (gameInitialised) {
            LightmapRenderer.fireRecalculateEvent()
        }


        if (gameFullyLoaded) {
            // resize UIs

            notifier.setPosition(
                    (Terrarum.WIDTH - notifier.width) / 2, Terrarum.HEIGHT - notifier.height)

            // inventory
            /*uiInventoryPlayer =
                    UIInventory(player,
                            width = 840,
                            height = Terrarum.HEIGHT - 160,
                            categoryWidth = 210
                    )*/


            // basic watch-style notification bar (temperature, new mail)
            uiWatchBasic.setPosition(Terrarum.WIDTH - uiWatchBasic.width, 0)
            uiWatchTierOne.setPosition(Terrarum.WIDTH - uiWatchTierOne.width, uiWatchBasic.height - 2)
        }


        println("[Ingame] Resize event")
    }

    override fun dispose() {
        IngameRenderer.dispose()

        actorsRenderBehind.forEach { it.dispose() }
        actorsRenderMiddle.forEach { it.dispose() }
        actorsRenderMidTop.forEach { it.dispose() }
        actorsRenderFront.forEach { it.dispose() }

        uiAliases.forEach { it.dispose() }
        uiAlasesPausing.forEach { it.dispose() }


        WatchDotAlph.dispose()
        Watch7SegMain.dispose()
        WatchDotAlph.dispose()

        ItemSlotImageBuilder.dispose()

        MessageWindow.SEGMENT_BLACK.dispose()
        MessageWindow.SEGMENT_WHITE.dispose()
    }


}
