package net.torvald.terrarum

import net.torvald.imagefont.GameFontBase
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
class Game @Throws(SlickException::class)
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
    lateinit var notifinator: UIHandler

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


    val memInUse: Long
        get() = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
    val totalVMMem: Long
        get() = Runtime.getRuntime().maxMemory() shr 20

    val auth = Authenticator()

    private var update_delta: Int = 0

    val KEY_LIGHTMAP_RENDER = Key.F7
    val KEY_LIGHTMAP_SMOOTH = Key.F8

    var DELTA_T: Int = 0

    @Throws(SlickException::class)
    override fun init(gameContainer: GameContainer, stateBasedGame: StateBasedGame) {
        KeyMap.build()

        shader12BitCol = Shader.makeShader("./res/4096.vrt", "./res/4096.frg")
        shaderBlurH = Shader.makeShader("./res/blurH.vrt", "./res/blur.frg")
        shaderBlurV = Shader.makeShader("./res/blurV.vrt", "./res/blur.frg")


        GRADIENT_IMAGE = Image("res/graphics/colourmap/sky_colour.png")
        skyBox = Rectangle(0f, 0f, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())

        TilePropCodex()
        // new ItemPropCodex() -- This is kotlin object and already initialised.

        map = GameMap(8192, 2048)

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

        consoleHandler = UIHandler(ConsoleWindow())
        consoleHandler.setPosition(0, 0)

        debugWindow = UIHandler(BasicDebugInfoWindow())
        debugWindow.setPosition(0, 0)

        notifinator = UIHandler(Notification())
        notifinator.setPosition(
                (Terrarum.WIDTH - notifinator.UI.width) / 2, Terrarum.HEIGHT - notifinator.UI.height)
        notifinator.setVisibility(true)

        if (Terrarum.gameConfig.getAsBoolean("smoothlighting") == true)
            KeyToggler.forceSet(KEY_LIGHTMAP_SMOOTH, true)
        else
            KeyToggler.forceSet(KEY_LIGHTMAP_SMOOTH, false)
    }

    override fun update(gc: GameContainer, sbg: StateBasedGame, delta: Int) {
        DELTA_T = delta

        update_delta = delta
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
        // also updates active actors
        updateAndInactivateDistantActors(gc, delta)

        CollisionSolver.process()

        uiContainer.forEach { ui -> ui.update(gc, delta) }
        consoleHandler.update(gc, delta)
        debugWindow.update(gc, delta)


        notifinator.update(gc, delta)

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
        actorContainer.forEach { actor ->
            if (actor is Visible && actor.inScreen()) { // if visible and within screen
                actor.drawBody(gc, g)
            }
        }
        player.drawBody(gc, g)

        LightmapRenderer.renderLightMap()

        MapCamera.renderFront(gc, g)
        MapDrawer.render(gc, g)

        setBlendMul()

            MapDrawer.drawEnvOverlay(g)

            if (!KeyToggler.isOn(KEY_LIGHTMAP_RENDER)) setBlendMul() else setBlendNormal()
            LightmapRenderer.draw(g)

        setBlendNormal()

        // draw actor glows
        actorContainer.forEach { actor ->
            if (actor is Visible && actor.inScreen()) { // if visible and within screen
                actor.drawGlow(gc, g)
            }
        }
        player.drawGlow(gc, g)

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
        uiContainer.forEach { ui -> ui.render(gc, g) }
        debugWindow.render(gc, g)
        consoleHandler.render(gc, g)
        notifinator.render(gc, g)
    }

    private fun getGradientColour(row: Int): Color {
        val gradMapWidth = GRADIENT_IMAGE!!.width
        val phase = Math.round(
                map.worldTime.elapsedSeconds().toFloat() / WorldTime.DAY_LENGTH.toFloat() * gradMapWidth
        )

        //update in every INTERNAL_FRAME frames
        return GRADIENT_IMAGE!!.getColor(phase, row)
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

    override fun getID(): Int {
        return Terrarum.SCENE_ID_GAME
    }

    private fun drawSkybox(g: Graphics) {
        val skyColourFill = GradientFill(
                0f, 0f, getGradientColour(0),
                0f, Terrarum.HEIGHT.toFloat(), getGradientColour(1)
        )
        g.fill(skyBox, skyColourFill)
    }

    fun sendNotification(msg: Array<String>) {
        (notifinator.UI as Notification).sendNotification(Terrarum.appgc, update_delta, msg)
        notifinator.setAsOpening()
    }

    fun wakeDormantActors() {
        // determine whether the inactive actor should be re-active
        var actorContainerSize = actorContainerInactive.size
        var i = 0
        while (i < actorContainerSize) { // loop thru actorContainerInactive
            val actor = actorContainerInactive[i]
            val actorIndex = i
            if (actor is Visible && actor.inUpdateRange()) {
                addActor(actor)
                actorContainerInactive.removeAt(actorIndex)
                actorContainerSize -= 1
                i-- // array removed 1 elem, so also decrement counter by 1
            }
            i++
        }
    }

    fun updateAndInactivateDistantActors(gc: GameContainer, delta: Int) {
        var actorContainerSize = actorContainer.size
        var i = 0
        // determine whether the actor should be active or dormant by its distance from the player
        // will put dormant actors to list specifically for them
        // if the actor is not to be dormant, update it
        while (i < actorContainerSize) { // loop thru actorContainer
            val actor = actorContainer[i]
            val actorIndex = i
            if (actor is Visible && !actor.inUpdateRange()) {
                actorContainerInactive.add(actor) // duplicates are checked when the actor is re-activated
                actorContainer.removeAt(actorIndex)
                actorContainerSize -= 1
                i-- // array removed 1 elem, so also decrement counter by 1
            }
            else {
                actorContainer[i].update(gc, delta)
            }
            i++
        }
    }

    private val globalLightByTime: Int
        get() = getGradientColour(2).getRGB24().rgb24ExpandToRgb30()

    fun Color.getRGB24(): Int = (this.redByte shl 16) or (this.greenByte shl 8) or (this.blueByte)
    fun Int.rgb24ExpandToRgb30(): Int = (this and 0xff) or
            (this and 0xff00).ushr(8).shl(10) or
            (this and 0xff0000).ushr(16).shl(20)

    fun Double.sqr() = this * this
    fun Int.sqr() = this * this
    private fun distToActorSqr(a: Visible, p: Player): Float =
            (a.hitbox.centeredX - p.hitbox.centeredX).sqr().toFloat() + (a.hitbox.centeredY - p.hitbox.centeredY).sqr().toFloat()
    private fun Visible.inScreen() = distToActorSqr(this, player) <=
                                     (Terrarum.WIDTH.plus(this.hitbox.width.div(2)).times(1 / Terrarum.game.screenZoom).sqr() +
                                      Terrarum.HEIGHT.plus(this.hitbox.height.div(2)).times(1 / Terrarum.game.screenZoom).sqr())
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
    fun removeActor(ID: Int) {
        if (ID == player.referenceID) throw RuntimeException("Attempted to remove player.")
        // get index of the actor and delete by the index.
        // we can do this as the list is guaranteed to be sorted
        // and only contains unique values
        val indexToDelete = actorContainer.binarySearch(ID)
        if (indexToDelete >= 0) actorContainer.removeAt(indexToDelete)
    }

    /**
     * Check for duplicates, append actor and sort the list
     */
    fun addActor(actor: Actor): Boolean {
        if (hasActor(actor.referenceID))
            throw RuntimeException("Actor with ID ${actor.referenceID} already exists.")
        actorContainer.add(actor)
        insertionSortLastElem(actorContainer) // we can do this as we are only adding single actor
        return true
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
        // 'insertion sort' last element
        var x: Actor
        var j: Int
        var index: Int = arr.size - 1
        x = arr[index]
        j = index - 1
        while (j > 0 && arr[j].referenceID > x.referenceID) {
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
