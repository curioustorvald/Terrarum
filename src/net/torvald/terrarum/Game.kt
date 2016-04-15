package net.torvald.terrarum

import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.console.Authenticator
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
 * Created by minjaesong on 16-03-19.
 */
class Game @Throws(SlickException::class)
constructor() : BasicGameState() {
    internal var game_mode = 0

    lateinit var map: GameMap

    val actorContainer = LinkedList<Actor>()
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

    @Throws(SlickException::class)
    override fun init(gameContainer: GameContainer, stateBasedGame: StateBasedGame) {
        KeyMap.build()

        shader12BitCol = Shader.makeShader("./res/4096.vrt", "./res/4096.frg")
        shaderBlurH = Shader.makeShader("./res/blurH.vrt", "./res/blur.frg")
        shaderBlurV = Shader.makeShader("./res/blurV.vrt", "./res/blur.frg")


        GRADIENT_IMAGE = Image("res/graphics/sky_colour.png")
        skyBox = Rectangle(0f, 0f, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())

        TilePropCodex()
        // new ItemPropCodex() -- This is kotlin object and already initialised.

        map = GameMap(8192, 2048)
        map.gravitation = 9.8f

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
        update_delta = delta
        setAppTitle()

        // GL at after_sunrise-noon_before_sunset
        map.updateWorldTime(delta)
        map.globalLight = globalLightByTime

        GameController.processInput(gc.input)

        TileStats.update()

        MapDrawer.update(gc, delta)
        MapCamera.update(gc, delta)

        actorContainer.forEach { actor -> actor.update(gc, delta) }
        actorContainer.forEach { actor ->
            if (actor is Visible) {
                actor.updateBodySprite(gc, delta)
            }
            if (actor is Glowing) {
                actor.updateGlowSprite(gc, delta)
            }
        }

        uiContainer.forEach { ui -> ui.update(gc, delta) }
        consoleHandler.update(gc, delta)
        debugWindow.update(gc, delta)


        notifinator.update(gc, delta)

        Terrarum.appgc.setVSync(Terrarum.appgc.fps >= Terrarum.VSYNC_TRIGGER_THRESHOLD)
    }

    private fun setAppTitle() {
        Terrarum.appgc.setTitle(
                "Simple Slick Game — FPS: "
                + Terrarum.appgc.fps + " ("
                + Terrarum.TARGET_INTERNAL_FPS.toString()
                + ") — "
                + memInUse.toString() + "M / "
                + totalVMMem.toString() + "M")
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
        actorContainer.forEach { actor -> if (actor is Visible) actor.drawBody(gc, g) }
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
        actorContainer.forEach { actor -> if (actor is Glowing) actor.drawGlow(gc, g) }
        player.drawGlow(gc, g)

        // draw reference ID if debugWindow is open
        if (debugWindow.isVisible) {
            actorContainer.forEach { actor ->
                if (actor is Visible) {
                    g.color = Color.white
                    g.font = Terrarum.smallNumbers
                    g.drawString(
                            actor.referenceID.toString(),
                            actor.hitbox.posX,
                            actor.hitbox.pointedY + 4
                    )
                    g.font = Terrarum.gameFont
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

    private val globalLightByTime: Int
        get() = getGradientColour(2).getRGB24().rgb24ExpandToRgb30()

    /**
     * extension function for org.newdawn.slick.Color
     */
    fun Color.getRGB24(): Int = (this.redByte shl 16) or (this.greenByte shl 8) or (this.blueByte)
    fun Int.rgb24ExpandToRgb30(): Int = (this and 0xff) or
            (this and 0xff00).ushr(8).shl(10) or
            (this and 0xff0000).ushr(16).shl(20)

    /**
     * actorContainer extensions
     */
    fun hasActor(ID: Int): Boolean {
        for (actor in actorContainer) {
            if (actor.referenceID == ID) return true
        }
        return false
    }

    fun removeActor(ID: Int) {
        for (actor in actorContainer) {
            if (actor.referenceID == ID)
                actorContainer.remove(actor)
        }
    }

    fun addActor(other: Actor): Boolean {
        if (hasActor(other.referenceID)) return false
        actorContainer.add(other)
        return true
    }

    fun getActor(ID: Int): Actor {
        for (actor in actorContainer) {
            if (actor.referenceID == ID)
                return actor
        }
        throw NullPointerException("Actor with ID $ID does not exist.")
    }

    fun addUI(other: UIHandler): Boolean {
        if (uiContainer.contains(other)) return false
        uiContainer.add(other)
        return true
    }
}
