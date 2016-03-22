package com.Torvald.Terrarum

import com.Torvald.ColourUtil.Col40
import com.Torvald.Terrarum.Actors.*
import com.Torvald.Terrarum.ConsoleCommand.Authenticator
import com.Torvald.Terrarum.ConsoleCommand.CommandDict
import com.Torvald.Terrarum.GameControl.GameController
import com.Torvald.Terrarum.GameControl.Key
import com.Torvald.Terrarum.GameControl.KeyMap
import com.Torvald.Terrarum.GameControl.KeyToggler
import com.Torvald.Terrarum.GameMap.GameMap
import com.Torvald.Terrarum.GameMap.WorldTime
import com.Torvald.Terrarum.MapDrawer.LightmapRenderer
import com.Torvald.Terrarum.MapDrawer.MapCamera
import com.Torvald.Terrarum.MapDrawer.MapDrawer
import com.Torvald.Terrarum.MapGenerator.MapGenerator
import com.Torvald.Terrarum.MapGenerator.RoguelikeRandomiser
import com.Torvald.Terrarum.TileProperties.TilePropCodex
import com.Torvald.Terrarum.TileStat.TileStat
import com.Torvald.Terrarum.UserInterface.BasicDebugInfoWindow
import com.Torvald.Terrarum.UserInterface.ConsoleWindow
import com.Torvald.Terrarum.UserInterface.Notification
import com.Torvald.Terrarum.UserInterface.UIHandler
import com.jme3.math.FastMath
import org.lwjgl.opengl.ARBShaderObjects
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

    var actorContainer = HashSet<Actor>()
    var uiContainer = HashSet<UIHandler>()

    lateinit var consoleHandler: UIHandler
    lateinit var debugWindow: UIHandler
    lateinit var notifinator: UIHandler

    lateinit internal var player: Player

    private var GRADIENT_IMAGE: Image? = null
    private var skyBox: Rectangle? = null

    var screenZoom = 1.0f
    val ZOOM_MAX = 2.0f
    val ZOOM_MIN = 0.25f

    private var shader12BitCol: Shader? = null
    private var shaderBlurH: Shader? = null
    private var shaderBlurV: Shader? = null


    private val useShader: Boolean = false
    private val shaderProgram = 0


    private val ENV_COLTEMP_SUNRISE = 2500
    private val ENV_SUNLIGHT_DELTA = MapDrawer.ENV_COLTEMP_NOON - ENV_COLTEMP_SUNRISE


    var memInUse: Long = 0
        get() = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
    var totalVMMem: Long = 0
        get() = Runtime.getRuntime().maxMemory() shr 20

    var auth = Authenticator()

    private var update_delta: Int = 0

    private val KEY_LIGHTMAP_RENDER = Key.F7
    private val KEY_LIGHTMAP_SMOOTH = Key.F8

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
        MapGenerator.setSeed(0x51621D2)
        //MapGenerator.setSeed(new HQRNG().nextLong());
        MapGenerator.generateMap()

        RoguelikeRandomiser.setSeed(0x540198)
        //RoguelikeRandomiser.setSeed(new HQRNG().nextLong());


        // add new player and put it to actorContainer
        //player = new Player();
        player = PFSigrid.build()
        //player.setNoClip(true);
        actorContainer.add(player)

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
        map.setGlobalLight(globalLightByTime);

        GameController.processInput(gc.input)

        TileStat.update()

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
        Terrarum.gameConfig["smoothlighting"] = KeyToggler.isOn(KEY_LIGHTMAP_SMOOTH)

        if (!g.isAntiAlias) g.isAntiAlias = true

        drawSkybox(g)

        // compensate for zoom. UIs have to be treated specially! (see UIHandler)
        g.translate(
                -MapCamera.getCameraX() * screenZoom, -MapCamera.getCameraY() * screenZoom)

        MapCamera.renderBehind(gc, g)

        actorContainer.forEach { actor -> if (actor is Visible) actor.drawBody(gc, g) }
        actorContainer.forEach { actor -> if (actor is Glowing) actor.drawGlow(gc, g) }

        LightmapRenderer.renderLightMap()

        MapCamera.renderFront(gc, g)
        MapDrawer.render(gc, g)


        setBlendModeMul()

        MapDrawer.drawEnvOverlay(g)

        if (!KeyToggler.isOn(KEY_LIGHTMAP_RENDER)) setBlendModeMul()
        else setBlendModeNormal()

        LightmapRenderer.draw(g)

        setBlendModeNormal()

        uiContainer.forEach { ui -> ui.render(gc, g) }
        debugWindow.render(gc, g)
        consoleHandler.render(gc, g)
        notifinator.render(gc, g)
    }

    fun addActor(e: Actor): Boolean {
        return actorContainer.add(e)
    }

    fun removeActor(e: Actor): Boolean {
        return actorContainer.remove(e)
    }

    private fun getGradientColour(): Array<Color> {
        val gradMapWidth = GRADIENT_IMAGE!!.width
        val phase = Math.round(
                map.worldTime.elapsedSeconds().toFloat() / WorldTime.DAY_LENGTH.toFloat() * gradMapWidth
        )

        //update in every INTERNAL_FRAME frames
        return arrayOf(
                GRADIENT_IMAGE!!.getColor(phase, 0),
                GRADIENT_IMAGE!!.getColor(phase, GRADIENT_IMAGE!!.height - 1)
        )
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
        val colourTable = getGradientColour()
        val skyColourFill = GradientFill(
                0f, 0f, colourTable[0],
                0f, Terrarum.HEIGHT.toFloat(), colourTable[1]
        )
        g.fill(skyBox, skyColourFill)
    }

    private fun setBlendModeMul() {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun setBlendModeNormal() {
        GL11.glDisable(GL11.GL_BLEND)
        Terrarum.appgc.graphics.setDrawMode(Graphics.MODE_NORMAL)
    }

    fun sendNotification(msg: Array<String>) {
        (notifinator.UI as Notification).sendNotification(Terrarum.appgc, update_delta, msg)
        notifinator.setAsOpening()
    }

    private val globalLightByTime: Char
        get() {
            /**
             * y = -DELTA(x-1)^RAYLEIGH + MAX
             * See documentation 'sky colour'
             */
            val INTENSITY_MIN = 9
            val INTENSITY_MAX = 39

            val COLTEMP_MIN = 2500
            val COLTEMP_MAX = MapDrawer.ENV_COLTEMP_NOON
            val COLTEMP_DELTA = COLTEMP_MAX - COLTEMP_MIN
            val RAYLEIGH_INDEX = 3.3f

            /**
             * get colour temperature
             */
            val dusk_len_colouring = 0.5f
            val daytime_len = 10
            var secs_offset: Int = Math.round(WorldTime.HOUR_SEC * dusk_len_colouring) // 1h as Seconds
            var time_domain_x_in_sec = (daytime_len + 2*dusk_len_colouring) * WorldTime.HOUR_SEC // 11h as Seconds

            var today_secs: Float = map.worldTime.elapsedSeconds().toFloat() + secs_offset
            if (today_secs > time_domain_x_in_sec - secs_offset) today_secs - WorldTime.DAY_LENGTH // 79000 -> -200

            var func_x: Float = (today_secs / time_domain_x_in_sec) * 2f // 0-46800 -> 0-2.0
            if (func_x < 1) func_x = 2f - func_x // mirror graph
            if (func_x > 2) func_x = 2f // clamp

            // println("x: $func_x")

            val sunAltColouring: Int = FastMath.ceil(
                    -COLTEMP_DELTA * FastMath.pow(func_x - 1, RAYLEIGH_INDEX) + COLTEMP_MAX
            )
            val sunColour: Col40 = Col40(MapDrawer.getColourFromMap(sunAltColouring))

            /**
             * get intensity
             */
            val dusk_len = 1.5f
            val intensity: Int = 39
            secs_offset = Math.round(WorldTime.HOUR_SEC * dusk_len) // 1h30 as Seconds
            time_domain_x_in_sec = (daytime_len + 2*dusk_len) * WorldTime.HOUR_SEC // 13h as Seconds

            today_secs = map.worldTime.elapsedSeconds().toFloat() + secs_offset
            if (today_secs > time_domain_x_in_sec - secs_offset) today_secs - WorldTime.DAY_LENGTH // 79000 -> -200




            return LightmapRenderer.darkenUniformInt(sunColour.raw, INTENSITY_MAX - intensity)
        }
}
