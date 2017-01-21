package net.torvald.terrarum

import net.torvald.imagefont.GameFontBase
import net.torvald.random.HQRNG
import net.torvald.terrarum.audio.AudioResourceLibrary
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.console.*
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.physicssolver.CollisionSolver
import net.torvald.terrarum.gamecontroller.GameController
import net.torvald.terrarum.gamecontroller.Key
import net.torvald.terrarum.gamecontroller.KeyMap
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldSimulator
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.mapdrawer.LightmapRenderer
import net.torvald.terrarum.mapdrawer.LightmapRenderer.constructRGBFromInt
import net.torvald.terrarum.mapdrawer.TilesDrawer
import net.torvald.terrarum.mapdrawer.FeaturesDrawer
import net.torvald.terrarum.mapdrawer.FeaturesDrawer.TILE_SIZE
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.mapgenerator.WorldGenerator
import net.torvald.terrarum.mapgenerator.RoguelikeRandomiser
import net.torvald.terrarum.tileproperties.TileCodex
import net.torvald.terrarum.tileproperties.TilePropUtil
import net.torvald.terrarum.tilestats.TileStats
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.weather.WeatherMixer
import org.lwjgl.opengl.GL11
import org.newdawn.slick.*
import org.newdawn.slick.fills.GradientFill
import org.newdawn.slick.geom.Rectangle
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import shader.Shader
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by minjaesong on 15-12-30.
 */
class StateInGame @Throws(SlickException::class)
constructor() : BasicGameState() {
    private val ACTOR_UPDATE_RANGE = 4096

    internal var game_mode = 0

    lateinit var world: GameWorld

    /**
     * list of Actors that is sorted by Actors' referenceID
     */
    val ACTORCONTAINER_INITIAL_SIZE = 128
    val PARTICLES_MAX = 768
    val actorContainer = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val actorContainerInactive = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val particlesContainer = CircularArray<ParticleBase>(PARTICLES_MAX)
    val uiContainer = ArrayList<UIHandler>()

    private val actorsRenderBehind = ArrayList<ActorVisible>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderMiddle = ArrayList<ActorVisible>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderMidTop = ArrayList<ActorVisible>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderFront  = ArrayList<ActorVisible>(ACTORCONTAINER_INITIAL_SIZE)

    lateinit var consoleHandler: UIHandler
    lateinit var debugWindow: UIHandler
    lateinit var notifier: UIHandler

    internal var playableActorDelegate: PlayableActorDelegate? = null
    internal val player: ActorHumanoid // currently POSSESSED actor :)
        get() = playableActorDelegate!!.actor

    var screenZoom = 1.0f
    val ZOOM_MAX = 2.0f
    val ZOOM_MIN = 0.5f

    val worldDrawFrameBuffer = Image(Terrarum.WIDTH.div(ZOOM_MIN).ceilInt(), Terrarum.HEIGHT.div(ZOOM_MIN).ceilInt())
    val worldG = worldDrawFrameBuffer.graphics
    val uisDrawFrameBuffer = Image(Terrarum.WIDTH, Terrarum.HEIGHT)
    val uiG = uisDrawFrameBuffer.graphics

    //private lateinit var shader12BitCol: Shader // grab LibGDX if you want some shader
    //private lateinit var shaderBlur: Shader

    private val useShader: Boolean = false
    private val shaderProgram = 0

    val KEY_LIGHTMAP_RENDER = Key.F7
    val KEY_LIGHTMAP_SMOOTH = Key.F8

    var UPDATE_DELTA: Int = 0

    // UI aliases
    val uiAliases = HashMap<String, UIHandler>()
    private val UI_PIE_MENU = "uiPieMenu"
    private val UI_QUICK_BAR = "uiQuickBar"
    private val UI_INVENTORY_PLAYER = "uiInventoryPlayer"
    private val UI_INVENTORY_ANON = "uiInventoryAnon"

    var paused: Boolean = false
        get() = consoleHandler.isOpened

    @Throws(SlickException::class)
    override fun init(gameContainer: GameContainer, stateBasedGame: StateBasedGame) {
        // state init code. Executed before the game goes into any "state" in states in StateBasedGame.java

        Terrarum.gameStarted = true
    }

    override fun enter(gc: GameContainer, sbg: StateBasedGame) {
        // load things when the game entered this "state"
        // load necessary shaders
        //shader12BitCol = Shader.makeShader("./assets/4096.vert", "./assets/4096.frag")
        //shaderBlur = Shader.makeShader("./assets/blur.vert", "./assets/blur.frag")

        // init map as chosen size
        world = GameWorld(8192, 2048)

        // generate terrain for the map
        WorldGenerator.attachMap(world)
        //WorldGenerator.SEED = 0x51621D2
        WorldGenerator.SEED = HQRNG().nextLong()
        WorldGenerator.generateMap()


        RoguelikeRandomiser.seed = HQRNG().nextLong()


        // add new player and put it to actorContainer
        playableActorDelegate = PlayableActorDelegate(PlayerBuilderSigrid())
        addActor(player)


        // test actor
        addActor(PlayerBuilderCynthia())



        // init console window
        consoleHandler = UIHandler(ConsoleWindow())
        consoleHandler.setPosition(0, 0)

        // init debug window
        debugWindow = UIHandler(BasicDebugInfoWindow())
        debugWindow.setPosition(0, 0)

        // init notifier
        notifier = UIHandler(Notification())
        notifier.UI.handler = notifier
        notifier.setPosition(
                (Terrarum.WIDTH - notifier.UI.width) / 2, Terrarum.HEIGHT - notifier.UI.height)

        // set smooth lighting as in config
        KeyToggler.forceSet(KEY_LIGHTMAP_SMOOTH, Terrarum.getConfigBoolean("smoothlighting"))

        // queue up game UIs
        //  lesser UIs
        // quick bar
        uiAliases[UI_QUICK_BAR] = UIHandler(UIQuickBar())
        uiAliases[UI_QUICK_BAR]!!.isVisible = true
        uiAliases[UI_QUICK_BAR]!!.setPosition(0, 0)
        uiAliases[UI_QUICK_BAR]!!.UI.handler = uiAliases[UI_QUICK_BAR]
        uiContainer.add(uiAliases[UI_QUICK_BAR]!!)
        // pie menu
        uiAliases[UI_PIE_MENU] = UIHandler(UIPieMenu())
        uiAliases[UI_PIE_MENU]!!.setPosition(
                (Terrarum.WIDTH - uiAliases[UI_PIE_MENU]!!.UI.width) / 2,
                (Terrarum.HEIGHT - uiAliases[UI_PIE_MENU]!!.UI.height) / 2
        )
        uiAliases[UI_PIE_MENU]!!.UI.handler = uiAliases[UI_PIE_MENU]
        uiContainer.add(uiAliases[UI_PIE_MENU]!!)

        // audio test
        //AudioResourceLibrary.ambientsWoods[0].play()
    }

    override fun update(gc: GameContainer, sbg: StateBasedGame, delta: Int) {
        UPDATE_DELTA = delta
        setAppTitle()


        if (!paused) {

            ///////////////////////////
            // world-related updates //
            ///////////////////////////
            TilePropUtil.dynamicLumFuncTickClock()
            world.updateWorldTime(delta)
            WorldSimulator(player, delta)
            WeatherMixer.update(gc, delta)
            TileStats.update()
            if (!(CommandDict["setgl"] as SetGlobalLightOverride).lightOverride)
                world.globalLight = constructRGBFromInt(
                        WeatherMixer.globalLightNow.redByte,
                        WeatherMixer.globalLightNow.greenByte,
                        WeatherMixer.globalLightNow.blueByte
                )


            ///////////////////////////
            // input-related updates //
            ///////////////////////////
            GameController.processInput(gc, delta, gc.input)
            uiContainer.forEach { it.processInput(gc, delta, gc.input) }


            ////////////////////////////
            // camera-related updates //
            ////////////////////////////
            FeaturesDrawer.update(gc, delta)
            MapCamera.update()
            TilesDrawer.update()


            ///////////////////////////
            // actor-related updates //
            ///////////////////////////
            repossessActor()

            // determine whether the inactive actor should be activated
            wakeDormantActors()
            // determine whether the actor should keep being activated or be dormant
            KillOrKnockdownActors()
            updateActors(gc, delta)
            particlesContainer.forEach { it.update(gc, delta) }
            // TODO thread pool(?)
            CollisionSolver.process()
        }


        ////////////////////////
        // ui-related updates //
        ////////////////////////
        uiContainer.forEach { it.update(gc, delta) }
        consoleHandler.update(gc, delta)
        debugWindow.update(gc, delta)
        notifier.update(gc, delta)

        // update debuggers using javax.swing //
        if (Authenticator.b()) {
            AVTracker.update()
            ActorsList.update()
        }


        /////////////////////////
        // app-related updates //
        /////////////////////////
        if (!Terrarum.isWin81) {
            Terrarum.appgc.setVSync(Terrarum.appgc.fps >= Terrarum.VSYNC_TRIGGER_THRESHOLD) // windows 10 has some trouble with this...
        }

        // determine if lightmap blending should be done
        Terrarum.gameConfig["smoothlighting"] = KeyToggler.isOn(KEY_LIGHTMAP_SMOOTH)
    }

    private fun repossessActor() {
        // check if currently pocessed actor is removed from game
        if (!hasActor(player)) {
            // re-possess canonical player
            if (hasActor(Player.PLAYER_REF_ID))
                changePossession(Player.PLAYER_REF_ID)
            else
                changePossession(0x51621D) // FIXME fallback debug mode (FIXME is there for a reminder visible in ya IDE)
        }
    }

    private fun changePossession(newActor: PlayableActorDelegate) {
        if (!hasActor(player)) {
            throw IllegalArgumentException("No such actor in actorContainer: $newActor")
        }

        playableActorDelegate = newActor
        WorldSimulator(player, UPDATE_DELTA)
    }

    private fun changePossession(refid: Int) {
        // TODO prevent possessing other player on multiplayer

        if (!hasActor(refid)) {
            throw IllegalArgumentException("No such actor in actorContainer: $refid")
        }

        // take care of old delegate
        playableActorDelegate!!.actor.collisionType = HumanoidNPC.DEFAULT_COLLISION_TYPE
        // accept new delegate
        playableActorDelegate = PlayableActorDelegate(getActorByID(refid) as ActorHumanoid)
        playableActorDelegate!!.actor.collisionType = ActorWithSprite.COLLISION_KINEMATIC
        WorldSimulator(player, UPDATE_DELTA)
    }

    private fun setAppTitle() {
        Terrarum.appgc.setTitle(
                Terrarum.NAME +
                " — F: ${Terrarum.appgc.fps} (${Terrarum.TARGET_INTERNAL_FPS})" +
                " — M: ${Terrarum.memInUse}M / ${Terrarum.totalVMMem}M")
    }

    override fun render(gc: GameContainer, sbg: StateBasedGame, gwin: Graphics) {
        // clean the shit beforehand
        worldG.clear()
        uiG.clear()

        blendNormal()


        drawSkybox(gwin) // drawing to gwin so that any lights from lamp wont "leak" to the skybox
                         // e.g. Bright blue light on sunset


        // make camara work //
        // compensate for zoom. UIs must be treated specially! (see UIHandler)
        worldG.translate(-MapCamera.x.toFloat(), -MapCamera.y.toFloat())


        blendNormal()

        /////////////////////////////
        // draw map related stuffs //
        /////////////////////////////
        TilesDrawer.renderWall(worldG)
        actorsRenderBehind.forEach { it.drawBody(worldG) }
        actorsRenderBehind.forEach { it.drawGlow(worldG) }
        particlesContainer.forEach { it.drawBody(worldG) }
        particlesContainer.forEach { it.drawGlow(worldG) }
        TilesDrawer.renderTerrain(worldG)

        /////////////////
        // draw actors //
        /////////////////
        actorsRenderMiddle.forEach { it.drawBody(worldG) }
        actorsRenderMidTop.forEach { it.drawBody(worldG) }
        player.drawBody(worldG)
        actorsRenderFront.forEach { it.drawBody(worldG) }
        // --> Change of blend mode <-- introduced by ActorVisible //


        /////////////////////////////
        // draw map related stuffs //
        /////////////////////////////
        LightmapRenderer.renderLightMap()

        TilesDrawer.renderFront(worldG, false)
        // --> blendNormal() <-- by TilesDrawer.renderFront
        FeaturesDrawer.render(gc, worldG)


        FeaturesDrawer.drawEnvOverlay(worldG)

        if (!KeyToggler.isOn(KEY_LIGHTMAP_RENDER)) blendMul()
        else blendNormal()
        LightmapRenderer.draw(worldG)


        //////////////////////
        // draw actor glows //
        //////////////////////
        actorsRenderMiddle.forEach { it.drawGlow(worldG) }
        actorsRenderMidTop.forEach { it.drawGlow(worldG) }
        player.drawGlow(worldG)
        actorsRenderFront.forEach { it.drawGlow(worldG) }
        // --> blendLightenOnly() <-- introduced by ActorVisible //


        ////////////////////////
        // debug informations //
        ////////////////////////
        blendNormal()
        // draw reference ID if debugWindow is open
        if (debugWindow.isVisible) {
            actorContainer.forEachIndexed { i, actor ->
                if (actor is ActorVisible) {
                    worldG.color = Color.white
                    worldG.font = Terrarum.fontSmallNumbers
                    worldG.drawString(
                            actor.referenceID.toString(),
                            actor.hitbox.posX.toFloat(),
                            actor.hitbox.pointedY.toFloat() + 4
                    )

                    if (DEBUG_ARRAY) {
                        worldG.color = GameFontBase.codeToCol["g"]
                        worldG.drawString(
                                i.toString(),
                                actor.hitbox.posX.toFloat(),
                                actor.hitbox.pointedY.toFloat() + 4 + 10
                        )
                    }
                }
            }
        }
        // fluidmap debug
        if (KeyToggler.isOn(Key.F4))
            WorldSimulator.drawFluidMapDebug(worldG)


        //////////////
        // draw UIs //
        //////////////
        uiContainer.forEach { it.render(gc, sbg, uiG) }
        debugWindow.render(gc, sbg, uiG)
        consoleHandler.render(gc, sbg, uiG)
        notifier.render(gc, sbg, uiG)


        /////////////////
        // draw layers //
        /////////////////
        gwin.drawImage(worldDrawFrameBuffer.getScaledCopy(screenZoom), 0f, 0f)
        gwin.drawImage(uisDrawFrameBuffer, 0f, 0f)


        // centre marker
        /*gwin.color = Color(0x00FFFF)
        gwin.lineWidth = 1f
        gwin.drawLine(Terrarum.WIDTH / 2f, 0f, Terrarum.WIDTH / 2f, Terrarum.HEIGHT.toFloat())
        gwin.drawLine(0f, Terrarum.HEIGHT / 2f, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT / 2f)*/
    }

    override fun keyPressed(key: Int, c: Char) {
        GameController.keyPressed(key, c)

        if (Terrarum.getConfigIntArray("keyquickselalt").contains(key)
            || key == Terrarum.getConfigInt("keyquicksel")) {
            uiAliases[UI_PIE_MENU]!!.setAsOpen()
            uiAliases[UI_QUICK_BAR]!!.setAsClose()
        }

        uiContainer.forEach { it.keyPressed(key, c) } // for KeyboardControlled UIcanvases
    }

    override fun keyReleased(key: Int, c: Char) {
        GameController.keyReleased(key, c)

        if (Terrarum.getConfigIntArray("keyquickselalt").contains(key)
            || key == Terrarum.getConfigInt("keyquicksel")) {
            uiAliases[UI_PIE_MENU]!!.setAsClose()
            uiAliases[UI_QUICK_BAR]!!.setAsOpen()
        }

        uiContainer.forEach { it.keyReleased(key, c) } // for KeyboardControlled UIcanvases
    }

    override fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        GameController.mouseMoved(oldx, oldy, newx, newy)

        uiContainer.forEach { it.mouseMoved(oldx, oldy, newx, newy) } // for MouseControlled UIcanvases
    }

    override fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        GameController.mouseDragged(oldx, oldy, newx, newy)

        uiContainer.forEach { it.mouseDragged(oldx, oldy, newx, newy) } // for MouseControlled UIcanvases
    }

    override fun mousePressed(button: Int, x: Int, y: Int) {
        GameController.mousePressed(button, x, y)

        uiContainer.forEach { it.mousePressed(button, x, y) } // for MouseControlled UIcanvases
    }

    override fun mouseReleased(button: Int, x: Int, y: Int) {
        GameController.mouseReleased(button, x, y)

        uiContainer.forEach { it.mouseReleased(button, x, y) } // for MouseControlled UIcanvases
    }

    override fun mouseWheelMoved(change: Int) {
        GameController.mouseWheelMoved(change)

        uiContainer.forEach { it.mouseWheelMoved(change) } // for MouseControlled UIcanvases
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
        GameController.controllerButtonPressed(controller, button)

        uiContainer.forEach { it.controllerButtonPressed(controller, button) } // for GamepadControlled UIcanvases
    }

    override fun controllerButtonReleased(controller: Int, button: Int) {
        GameController.controllerButtonReleased(controller, button)

        uiContainer.forEach { it.controllerButtonReleased(controller, button) } // for GamepadControlled UIcanvases
    }

    override fun getID(): Int = Terrarum.STATE_ID_GAME

    private fun drawSkybox(g: Graphics) = WeatherMixer.render(g)

    /** Send message to notifier UI and toggle the UI as opened. */
    fun sendNotification(msg: Array<String>) {
        (notifier.UI as Notification).sendNotification(msg)
    }

    fun wakeDormantActors() {
        var actorContainerSize = actorContainerInactive.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainerInactive
            val actor = actorContainerInactive[i]
            val actorIndex = i
            if (actor is ActorVisible && actor.inUpdateRange()) {
                addActor(actor) // duplicates are checked here
                actorContainerInactive.removeAt(actorIndex)
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
            else if (actor is ActorVisible && !actor.inUpdateRange()) {
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
    fun updateActors(gc: GameContainer, delta: Int) {
        if (false) { // don't multithread this for now, it's SLOWER //if (Terrarum.MULTITHREAD && actorContainer.size > Terrarum.THREADS) {
            val actors = actorContainer.size.toFloat()
            // set up indices
            for (i in 0..Terrarum.THREADS - 1) {
                ThreadParallel.map(
                        i,
                        ThreadActorUpdate(
                                actors.div(Terrarum.THREADS).times(i).roundInt(),
                                actors.div(Terrarum.THREADS).times(i.plus(1)).roundInt() - 1,
                                gc, delta
                        ),
                        "ActorUpdate"
                )
            }

            ThreadParallel.startAll()
        }
        else {
            actorContainer.forEach { it.update(gc, delta) }
        }
    }

    fun Double.sqr() = this * this
    fun Int.sqr() = this * this
    fun min(vararg d: Double): Double {
        var ret = Double.MAX_VALUE
        d.forEach { if (it < ret) ret = it }
        return ret
    }
    private fun distToActorSqr(a: ActorVisible, p: ActorVisible) =
            min(// take min of normal position and wrapped (x < 0) position
                    (a.hitbox.centeredX - p.hitbox.centeredX).sqr() +
                    (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                    (a.hitbox.centeredX - p.hitbox.centeredX + world.width * TILE_SIZE).sqr() +
                    (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                    (a.hitbox.centeredX - p.hitbox.centeredX - world.width * TILE_SIZE).sqr() +
                    (a.hitbox.centeredY - p.hitbox.centeredY).sqr()
            )
    private fun distToCameraSqr(a: ActorVisible) =
            min(
                    (a.hitbox.posX - MapCamera.x).sqr() +
                    (a.hitbox.posY - MapCamera.y).sqr(),
                    (a.hitbox.posX - MapCamera.x + world.width * TILE_SIZE).sqr() +
                    (a.hitbox.posY - MapCamera.y).sqr(),
                    (a.hitbox.posX - MapCamera.x - world.width * TILE_SIZE).sqr() +
                    (a.hitbox.posY - MapCamera.y).sqr()
            )

    /** whether the actor is within screen */
    private fun ActorVisible.inScreen() =
            distToCameraSqr(this) <=
            (Terrarum.WIDTH.plus(this.hitbox.width.div(2)).times(1 / Terrarum.ingame.screenZoom).sqr() +
             Terrarum.HEIGHT.plus(this.hitbox.height.div(2)).times(1 / Terrarum.ingame.screenZoom).sqr())


    /** whether the actor is within update range */
    private fun ActorVisible.inUpdateRange() = distToCameraSqr(this) <= ACTOR_UPDATE_RANGE.sqr()

    /**
     * actorContainer extensions
     */
    fun hasActor(actor: Actor) = hasActor(actor.referenceID)

    fun hasActor(ID: Int): Boolean =
            if (actorContainer.size == 0)
                false
            else {
                // TODO cherche for inactive
                val binsearch = actorContainer.binarySearch(ID)
                if (binsearch < 0) {
                    if (actorContainerInactive.size == 0) false
                    else {
                        val binsearch2 = actorContainerInactive.binarySearch(ID)

                        binsearch2 >= 0
                    }
                }
                else {
                    true
                }
            }

    fun removeActor(ID: Int) = removeActor(getActorByID(ID))
    /**
     * get index of the actor and delete by the index.
     * we can do this as the list is guaranteed to be sorted
     * and only contains unique values.
     *
     * Any values behind the index will be automatically pushed to front.
     * This is how remove function of [java.util.ArrayList] is defined.
     */
    fun removeActor(actor: Actor) {
        if (actor.referenceID == player.referenceID) throw RuntimeException("Attempted to remove player.")
        val indexToDelete = actorContainer.binarySearch(actor.referenceID)
        if (indexToDelete >= 0) {
            actorContainer.removeAt(indexToDelete)

            // indexToDelete >= 0 means that the actor certainly exists in the game
            // which means we don't need to check if i >= 0 again
            if (actor is ActorVisible) {
                when (actor.renderOrder) {
                    ActorOrder.BEHIND -> {
                        val i = actorsRenderBehind.binarySearch(actor.referenceID)
                        actorsRenderBehind.removeAt(i)
                    }
                    ActorOrder.MIDDLE -> {
                        val i = actorsRenderMiddle.binarySearch(actor.referenceID)
                        actorsRenderMiddle.removeAt(i)
                    }
                    ActorOrder.MIDTOP -> {
                        val i = actorsRenderMidTop.binarySearch(actor.referenceID)
                        actorsRenderMidTop.removeAt(i)
                    }
                    ActorOrder.FRONT  -> {
                        val i = actorsRenderFront.binarySearch(actor.referenceID)
                        actorsRenderFront.removeAt(i)
                    }
                }
            }
        }
    }

    /**
     * Check for duplicates, append actor and sort the list
     */
    fun addActor(actor: Actor) {
        if (hasActor(actor.referenceID))
            throw RuntimeException("Actor with ID ${actor.referenceID} already exists.")
        actorContainer.add(actor)
        insertionSortLastElem(actorContainer) // we can do this as we are only adding single actor

        if (actor is ActorVisible) {
            when (actor.renderOrder) {
                ActorOrder.BEHIND -> { actorsRenderBehind.add(actor); insertionSortLastElemAV(actorsRenderBehind) }
                ActorOrder.MIDDLE -> { actorsRenderMiddle.add(actor); insertionSortLastElemAV(actorsRenderMiddle) }
                ActorOrder.MIDTOP -> { actorsRenderMidTop.add(actor); insertionSortLastElemAV(actorsRenderMidTop) }
                ActorOrder.FRONT  -> { actorsRenderFront .add(actor); insertionSortLastElemAV(actorsRenderFront ) }
            }
        }
    }

    fun addParticle(particle: ParticleBase) {
        particlesContainer.add(particle)
    }

    /**
     * Whether the game should display actorContainer elem number when F3 is on
     */
    val DEBUG_ARRAY = false

    fun getActorByID(ID: Int): Actor {
        if (actorContainer.size == 0 && actorContainerInactive.size == 0)
            throw IllegalArgumentException("Actor with ID $ID does not exist.")

        var index = actorContainer.binarySearch(ID)
        if (index < 0) {
            index = actorContainerInactive.binarySearch(ID)

            if (index < 0)
                throw IllegalArgumentException("Actor with ID $ID does not exist.")
            else
                return actorContainerInactive[index]
        }
        else
            return actorContainer[index]
    }

    private fun insertionSortLastElem(arr: ArrayList<Actor>) {
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
    private fun insertionSortLastElemAV(arr: ArrayList<ActorVisible>) { // out-projection doesn't work, duh
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

    private fun ArrayList<out Actor>.binarySearch(actor: Actor) = this.binarySearch(actor.referenceID)

    private fun ArrayList<out Actor>.binarySearch(ID: Int): Int {
        // code from collections/Collections.kt
        var low = 0
        var high = actorContainer.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows
            val midVal = get(mid)

            if (ID > midVal.referenceID)
                low = mid + 1
            else if (ID < midVal.referenceID)
                high = mid - 1
            else
                return mid // key found
        }
        return -(low + 1)  // key not found
    }

    inline fun lock(lock: Lock, body: () -> Unit) {
        lock.lock()
        try {
            body()
        }
        finally {
            lock.unlock()
        }
    }
}

fun Color.toInt() = redByte.shl(16) or greenByte.shl(8) or blueByte
fun Color.to10bit() = redByte.shl(20) or greenByte.shl(10) or blueByte
