package net.torvald.terrarum

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.concurrent.ThreadPool
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.console.Authenticator
import net.torvald.terrarum.gameactors.physicssolver.CollisionSolver
import net.torvald.terrarum.gamecontroller.GameController
import net.torvald.terrarum.gamecontroller.Key
import net.torvald.terrarum.gamecontroller.KeyMap
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gamemap.GameMap
import net.torvald.terrarum.gamemap.WorldTime
import net.torvald.terrarum.mapdrawer.LightmapRenderer
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.mapgenerator.MapGenerator
import net.torvald.terrarum.mapgenerator.RoguelikeRandomiser
import net.torvald.terrarum.tileproperties.TilePropCodex
import net.torvald.terrarum.tilestats.TileStats
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.ui.ConsoleWindow
import net.torvald.terrarum.ui.Notification
import net.torvald.terrarum.ui.UIHandler
import org.lwjgl.opengl.GL11
import org.newdawn.slick.*
import org.newdawn.slick.fills.GradientFill
import org.newdawn.slick.geom.Rectangle
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import shader.Shader
import java.lang.management.ManagementFactory
import java.util.*

/**
 * Created by minjaesong on 15-12-30.
 */
class StateInGame @Throws(SlickException::class)
constructor() : BasicGameState() {
    private val ACTOR_UPDATE_RANGE = 4096

    internal var game_mode = 0

    lateinit var map: GameMap

    /**
     * list of Actors that is sorted by Actors' referenceID
     */
    val ACTORCONTAINER_INITIAL_SIZE = 128
    val actorContainer = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val actorContainerInactive = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val uiContainer = LinkedList<UIHandler>()

    lateinit var consoleHandler: UIHandler
    lateinit var debugWindow: UIHandler
    lateinit var notifier: UIHandler

    lateinit internal var player: Player

    private var GRADIENT_IMAGE: Image? = null
    private var skyBox: Rectangle? = null

    var screenZoom = 1.0f
    val ZOOM_MAX = 2.0f
    val ZOOM_MIN = 0.5f

    private lateinit var shader12BitCol: Shader
    private lateinit var shaderBlurH: Shader
    private lateinit var shaderBlurV: Shader

    private val useShader: Boolean = false
    private val shaderProgram = 0

    private val CORES = ThreadPool.POOL_SIZE

    val memInUse: Long
        get() = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
    val totalVMMem: Long
        get() = Runtime.getRuntime().maxMemory() shr 20

    val auth = Authenticator()

    val KEY_LIGHTMAP_RENDER = Key.F7
    val KEY_LIGHTMAP_SMOOTH = Key.F8

    var UPDATE_DELTA: Int = 0

    @Throws(SlickException::class)
    override fun init(gameContainer: GameContainer, stateBasedGame: StateBasedGame) {
        // load necessary shaders
        shader12BitCol = Shader.makeShader("./res/4096.vrt", "./res/4096.frg")
        shaderBlurH = Shader.makeShader("./res/blurH.vrt", "./res/blur.frg")
        shaderBlurV = Shader.makeShader("./res/blurV.vrt", "./res/blur.frg")

        // init skybox
        GRADIENT_IMAGE = Image("res/graphics/colourmap/sky_colour.png")
        skyBox = Rectangle(0f, 0f, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())

        // init map as chosen size
        map = GameMap(8192, 2048)

        // generate terrain for the map
        MapGenerator.attachMap(map)
        MapGenerator.SEED = 0x51621D2
        //mapgenerator.setSeed(new HQRNG().nextLong());
        MapGenerator.generateMap()


        RoguelikeRandomiser.seed = 0x540198
        //RoguelikeRandomiser.setSeed(new HQRNG().nextLong());


        // add new player and put it to actorContainer
        player = PBSigrid.create()
        //player = PFCynthia.create()
        //player.setNoClip(true);
        addActor(player)

        // init console window
        consoleHandler = UIHandler(ConsoleWindow())
        consoleHandler.setPosition(0, 0)

        // init debug window
        debugWindow = UIHandler(BasicDebugInfoWindow())
        debugWindow.setPosition(0, 0)

        // init notifier
        notifier = UIHandler(Notification())
        notifier.setPosition(
                (Terrarum.WIDTH - notifier.UI.width) / 2, Terrarum.HEIGHT - notifier.UI.height)
        notifier.visible = true

        // set smooth lighting as in config
        KeyToggler.forceSet(KEY_LIGHTMAP_SMOOTH, Terrarum.getConfigBoolean("smoothlighting"))
    }

    override fun update(gc: GameContainer, sbg: StateBasedGame, delta: Int) {
        UPDATE_DELTA = delta

        setAppTitle()

        map.updateWorldTime(delta)
        map.globalLight = globalLightByTime

        GameController.processInput(gc.input)

        TileStats.update()

        MapDrawer.update(gc, delta)
        MapCamera.update(gc, delta)

        // determine whether the inactive actor should be re-active
        wakeDormantActors()

        // determine whether the actor should be active or dormant
        InactivateDistantActors()

        updateActors(gc, delta)

        // TODO thread pool(?)
        CollisionSolver.process()

        uiContainer.forEach { ui -> ui.update(gc, delta) }
        consoleHandler.update(gc, delta)
        debugWindow.update(gc, delta)


        notifier.update(gc, delta)

        Terrarum.appgc.setVSync(Terrarum.appgc.fps >= Terrarum.VSYNC_TRIGGER_THRESHOLD)
    }

    private fun setAppTitle() {
        Terrarum.appgc.setTitle(
                "Simple Slick Game" +
                " — FPS: ${Terrarum.appgc.fps} (${Terrarum.TARGET_INTERNAL_FPS})" +
                " — ${memInUse}M / ${totalVMMem}M")
    }

    override fun render(gc: GameContainer, sbg: StateBasedGame, g: Graphics) {
        setBlendNormal()

        // determine if lightmap blending should be done
        Terrarum.gameConfig["smoothlighting"] = KeyToggler.isOn(KEY_LIGHTMAP_SMOOTH)

        // set antialias as on
        if (!g.isAntiAlias) g.isAntiAlias = true

        drawSkybox(g)

        // compensate for zoom. UIs have to be treated specially! (see UIHandler)
        g.translate(-MapCamera.cameraX * screenZoom, -MapCamera.cameraY * screenZoom)

        MapCamera.renderBehind(gc, g)

        // draw actors
        run {
            actorContainer.forEach { actor ->
                if (actor is Visible && actor.inScreen() && actor !is Player) { // if visible and within screen
                    actor.drawBody(gc, g)
                }
            }
            player.drawBody(gc, g)
        }

        LightmapRenderer.renderLightMap()

        MapCamera.renderFront(gc, g)
        MapDrawer.render(gc, g)

        setBlendMul()
            MapDrawer.drawEnvOverlay(g)

            if (!KeyToggler.isOn(KEY_LIGHTMAP_RENDER)) setBlendMul() else setBlendNormal()
            LightmapRenderer.draw(g)
        setBlendNormal()

        // draw actor glows
        run {
            actorContainer.forEach { actor ->
                if (actor is Visible && actor.inScreen() && actor !is Player) { // if visible and within screen
                    actor.drawGlow(gc, g)
                }
            }
            player.drawGlow(gc, g)
        }

        // draw reference ID if debugWindow is open
        if (debugWindow.visible) {
            actorContainer.forEachIndexed { i, actor ->
                if (actor is Visible) {
                    g.color = Color.white
                    g.font = Terrarum.smallNumbers
                    g.drawString(
                            actor.referenceID.toString(),
                            actor.hitbox.posX.toFloat(),
                            actor.hitbox.pointedY.toFloat() + 4
                    )

                    if (DEBUG_ARRAY) {
                        g.color = GameFontBase.codeToCol["g"]
                        g.drawString(
                                i.toString(),
                                actor.hitbox.posX.toFloat(),
                                actor.hitbox.pointedY.toFloat() + 4 + 10
                        )
                    }
                }
            }
        }

        // draw UIs
        run {
            uiContainer.forEach { ui -> ui.render(gc, sbg, g) }
            debugWindow.render(gc, sbg, g)
            consoleHandler.render(gc, sbg, g)
            notifier.render(gc, sbg, g)
        }
    }

    private fun getGradientColour(row: Int, phase: Int) = GRADIENT_IMAGE!!.getColor(phase, row)

    private fun getGradientColour(row: Int): Color {
        val gradMapWidth = GRADIENT_IMAGE!!.width
        val phase = Math.round(
                map.worldTime.elapsedSeconds().toFloat() / WorldTime.DAY_LENGTH.toFloat() * gradMapWidth
        )

        //update in every INTERNAL_FRAME frames
        return getGradientColour(row, phase)
    }

    /**
     * @param time in seconds
     */
    private fun getGradientColourByTime(row: Int, time: Int): Color {
        val gradMapWidth = GRADIENT_IMAGE!!.width
        val phase = Math.round(
                time.toFloat() / WorldTime.DAY_LENGTH.toFloat() * gradMapWidth
        )

        return getGradientColour(row, phase)
    }

    override fun keyPressed(key: Int, c: Char) {
        GameController.keyPressed(key, c)
    }

    override fun keyReleased(key: Int, c: Char) {
        GameController.keyReleased(key, c)
    }

    override fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        GameController.mouseMoved(oldx, oldy, newx, newy)
    }

    override fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
        GameController.mouseDragged(oldx, oldy, newx, newy)
    }

    override fun mousePressed(button: Int, x: Int, y: Int) {
        GameController.mousePressed(button, x, y)
    }

    override fun mouseReleased(button: Int, x: Int, y: Int) {
        GameController.mouseReleased(button, x, y)
    }

    override fun mouseWheelMoved(change: Int) {
        GameController.mouseWheelMoved(change)
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
        GameController.controllerButtonPressed(controller, button)
    }

    override fun controllerButtonReleased(controller: Int, button: Int) {
        GameController.controllerButtonReleased(controller, button)
    }

    override fun getID(): Int = Terrarum.SCENE_ID_GAME

    private fun drawSkybox(g: Graphics) {
        val skyColourFill = GradientFill(
                0f, 0f, getGradientColour(0),
                0f, Terrarum.HEIGHT.toFloat(), getGradientColour(1)
        )
        g.fill(skyBox, skyColourFill)
    }

    /** Send message to notifier UI and toggle the UI as opened. */
    fun sendNotification(msg: Array<String>) {
        (notifier.UI as Notification).sendNotification(Terrarum.appgc, UPDATE_DELTA, msg)
        notifier.setAsOpening()
    }

    fun wakeDormantActors() {
        var actorContainerSize = actorContainerInactive.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainerInactive
            val actor = actorContainerInactive[i]
            val actorIndex = i
            if (actor is Visible && actor.inUpdateRange()) {
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
    fun InactivateDistantActors() {
        var actorContainerSize = actorContainer.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainer
            val actor = actorContainer[i]
            val actorIndex = i
            if (actor is Visible && !actor.inUpdateRange()) {
                actorContainerInactive.add(actor) // naïve add; duplicates are checked when the actor is re-activated
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
        if (false) { // don't multithread this for now, it's SLOWER //if (Terrarum.MULTITHREAD) {
            val actors = actorContainer.size.toFloat()
            // set up indices
            for (i in 0..ThreadPool.POOL_SIZE - 1) {
                ThreadPool.map(
                        i,
                        ThreadActorUpdate(
                                ((actors / CORES) * i).toInt(),
                                ((actors / CORES) * i.plus(1)).toInt() - 1,
                                gc, delta
                        ),
                        "ActorUpdate"
                )
            }

            ThreadPool.startAll()
        }
        else {
            actorContainer.forEach { it.update(gc, delta) }
        }
    }

    private val globalLightByTime: Int
        get() = getGradientColour(2).getRGB24().rgb24ExpandToRgb30()
    fun globalLightByTime(t: Int): Int = getGradientColourByTime(2, t).getRGB24().rgb24ExpandToRgb30()

    fun Color.getRGB24(): Int = this.redByte.shl(16) or this.greenByte.shl(8) or this.blueByte
    /** Remap 8-bit value (0.0-1.0) to 10-bit value (0.0-4.0) by prepending two bits of zero for each R, G and B. */
    fun Int.rgb24ExpandToRgb30(): Int = (this and 0xff) or
            (this and 0xff00).ushr(8).shl(10) or
            (this and 0xff0000).ushr(16).shl(20)

    fun Double.sqr() = this * this
    fun Int.sqr() = this * this
    private fun distToActorSqr(a: Visible, p: Player): Double =
            (a.hitbox.centeredX - p.hitbox.centeredX).sqr() + (a.hitbox.centeredY - p.hitbox.centeredY).sqr()
    /** whether the actor is within screen */
    private fun Visible.inScreen() = distToActorSqr(this, player) <=
                                     (Terrarum.WIDTH.plus(this.hitbox.width.div(2)).times(1 / Terrarum.ingame.screenZoom).sqr() +
                                      Terrarum.HEIGHT.plus(this.hitbox.height.div(2)).times(1 / Terrarum.ingame.screenZoom).sqr())
    /** whether the actor is within update range */
    private fun Visible.inUpdateRange() = distToActorSqr(this, player) <= ACTOR_UPDATE_RANGE.sqr()
    /**
     * actorContainer extensions
     */
    fun hasActor(actor: Actor) = hasActor(actor.referenceID)
    fun hasActor(ID: Int): Boolean =
            if (actorContainer.size == 0)
                false
            else
                actorContainer.binarySearch(ID) >= 0

    fun removeActor(actor: Actor) = removeActor(actor.referenceID)
    /**
     * get index of the actor and delete by the index.
     * we can do this as the list is guaranteed to be sorted
     * and only contains unique values.
     *
     * Any values behind the index will be automatically pushed to front.
     * This is how remove function of [java.util.ArrayList] is defined.
     */
    fun removeActor(ID: Int) {
        if (ID == player.referenceID) throw RuntimeException("Attempted to remove player.")
        val indexToDelete = actorContainer.binarySearch(ID)
        if (indexToDelete >= 0) actorContainer.removeAt(indexToDelete)
    }

    /**
     * Check for duplicates, append actor and sort the list
     */
    fun addActor(actor: Actor) {
        if (hasActor(actor.referenceID))
            throw RuntimeException("Actor with ID ${actor.referenceID} already exists.")
        actorContainer.add(actor)
        insertionSortLastElem(actorContainer) // we can do this as we are only adding single actor
    }

    /**
     * Whether the game should display actorContainer elem number when F3 is on
     */
    val DEBUG_ARRAY = false

    fun getActorByID(ID: Int): Actor {
        if (actorContainer.size == 0) throw IllegalArgumentException("Actor with ID $ID does not exist.")

        val index = actorContainer.binarySearch(ID)
        if (index < 0)
            throw IllegalArgumentException("Actor with ID $ID does not exist.")
        else
            return actorContainer[index]
    }

    private fun insertionSortLastElem(arr: ArrayList<Actor>) {
        var j: Int
        val index: Int = arr.size - 1
        val x = arr[index]
        j = index - 1
        while (j > 0 && arr[j] > x) {
            arr[j + 1] = arr[j]
            j -= 1
        }
        arr[j + 1] = x
    }

    private fun ArrayList<Actor>.binarySearch(actor: Actor) = this.binarySearch(actor.referenceID)

    private fun ArrayList<Actor>.binarySearch(ID: Int): Int {
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
}
