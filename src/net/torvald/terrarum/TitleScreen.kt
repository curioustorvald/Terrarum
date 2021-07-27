package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.AppLoader.printdbgerr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.ai.ActorAI
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.HumanoidNPC
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.ui.UIRemoCon
import net.torvald.terrarum.modulebasegame.ui.UITitleRemoConYaml
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.WorldCamera

/**
 * Created by minjaesong on 2017-09-02.
 */
class TitleScreen(batch: SpriteBatch) : IngameInstance(batch) {

    // todo register titlescreen as the ingame, similar in a way that the buildingmaker did

    var camera = OrthographicCamera(AppLoader.screenSize.screenWf, AppLoader.screenSize.screenHf)


    // invert Y
    fun initViewPort(width: Int, height: Int) {
        // Set Y to point downwards
        camera.setToOrtho(true, width.toFloat(), height.toFloat())

        // Update camera matrix
        camera.update()

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height)
    }


    //private var loadDone = false // not required; draw-while-loading is implemented in the AppLoader

    private lateinit var demoWorld: GameWorldExtension
    private lateinit var cameraNodes: FloatArray // camera Y-pos
    private val cameraAI = object : ActorAI {
        private val axisMax = 1f

        private var firstTime = true

        override fun update(actor: Actor, delta: Float) {
            val actor = actor as HumanoidNPC

            // fuck
            val avSpeed = 1.0 // FIXME camera goes faster when FPS is high
            actor.actorValue[AVKey.SPEED] = avSpeed
            actor.actorValue[AVKey.ACCEL] = avSpeed / 6.0
            // end fuck



            val tileSize = TILE_SIZEF
            val catmullRomTension = 0f

            // pan camera
            actor.moveRight(axisMax)


            val domainSize = demoWorld.width * tileSize
            val codomainSize = cameraNodes.size
            val x = actor.hitbox.canonicalX.toFloat()

            val p1 = (x / (domainSize / codomainSize)).floorInt() fmod cameraNodes.size
            val p0 = ((p1 - 1) fmod codomainSize) fmod cameraNodes.size
            val p2 = ((p1 + 1) fmod codomainSize) fmod cameraNodes.size
            val p3 = ((p1 + 2) fmod codomainSize) fmod cameraNodes.size
            val u: Float = 1f - (p2 - (x / (domainSize / codomainSize))) / (p2 - p1)

            //val targetYPos = FastMath.interpolateCatmullRom(u, catmullRomTension, cameraNodes[p0], cameraNodes[p1], cameraNodes[p2], cameraNodes[p3])
            val targetYPos = FastMath.interpolateLinear(u, cameraNodes[p1], cameraNodes[p2])
            val yDiff = targetYPos - actor.hitbox.canonicalY

            /*if (!firstTime) {
                actor.moveDown(yDiff.bipolarClamp(axisMax.toDouble()).toFloat())
            }
            else {
                actor.hitbox.setPosition(actor.hitbox.canonicalX, targetYPos.toDouble())
                firstTime = false
            }*/
            actor.hitbox.setPosition(actor.hitbox.canonicalX, targetYPos.toDouble()) // just move the cameraY to interpolated path


            //println("${actor.hitbox.canonicalX}, ${actor.hitbox.canonicalY}")
        }
    }
    private lateinit var cameraPlayer: ActorWithBody

    private val gradWhiteTop = Color(0xf8f8f8ff.toInt())
    private val gradWhiteBottom = Color(0xd8d8d8ff.toInt())


    val uiContainer = UIContainer()
    private lateinit var uiMenu: UICanvas

    private lateinit var worldFBO: FrameBuffer

    private fun loadThingsWhileIntroIsVisible() {
        printdbg(this, "Intro pre-load")


        demoWorld = GameWorldExtension(1, 64, 64, 0L, 0L, 0)

        printdbg(this, "Demo world gen complete")

        // set time to summer
        demoWorld.worldTime.addTime(WorldTime.DAY_LENGTH * 32)

        // construct camera nodes
        val nodeCount = 100
        cameraNodes = kotlin.FloatArray(nodeCount) { it ->
            val tileXPos = (demoWorld.width.toFloat() * it / nodeCount).floorInt()
            var travelDownCounter = 0
            while (travelDownCounter < demoWorld.height && !BlockCodex[demoWorld.getTileFromTerrain(tileXPos, travelDownCounter)].isSolid) {
                travelDownCounter += 4
            }
            travelDownCounter * TILE_SIZEF
        }


        cameraPlayer = CameraPlayer(demoWorld, cameraAI)

        demoWorld.worldTime.timeDelta = 150


        IngameRenderer.setRenderedWorld(demoWorld)


        uiMenu = UIRemoCon(UITitleRemoConYaml())//UITitleRemoConRoot()
        uiMenu.setPosition(0, 0)
        uiMenu.setAsOpen()


        uiContainer.add(uiMenu)

        //loadDone = true
    }


    override fun hide() {
    }

    override fun show() {
        printdbg(this, "show() called")

        initViewPort(AppLoader.screenSize.screenW, AppLoader.screenSize.screenH)


        Gdx.input.inputProcessor = TitleScreenController(this)


        worldFBO = FrameBuffer(Pixmap.Format.RGBA8888, AppLoader.screenSize.screenW, AppLoader.screenSize.screenH, false)

        loadThingsWhileIntroIsVisible()

        printdbg(this, "show() exit")
    }


    private val introUncoverTime: Second = 0.3f
    private var introUncoverDeltaCounter = 0f
    private var updateAkku = 0.0

    private var fucklatch = false

    override fun render(updateRate: Float) {
        if (!fucklatch) {
            printdbg(this, "render start")
            fucklatch = true
        }

        // async update and render

        val dt = Gdx.graphics.rawDeltaTime
        updateAkku += dt

        var i = 0L
        while (updateAkku >= updateRate) {
            AppLoader.measureDebugTime("Ingame.update") { updateScreen(updateRate) }
            updateAkku -= updateRate
            i += 1
        }
        AppLoader.setDebugTime("Ingame.updateCounter", i)


        // render? just do it anyway
        AppLoader.measureDebugTime("Ingame.render") { renderScreen() }
        AppLoader.setDebugTime("Ingame.render-Light",
                ((AppLoader.debugTimers["Ingame.render"] as? Long) ?: 0) - ((AppLoader.debugTimers["Renderer.LightTotal"] as? Long) ?: 0)
        )
    }

    fun updateScreen(delta: Float) {
        demoWorld.globalLight = WeatherMixer.globalLightNow
        demoWorld.updateWorldTime(delta)
        WeatherMixer.update(delta, cameraPlayer, demoWorld)
        cameraPlayer.update(delta)

        // worldcamera update AFTER cameraplayer in this case; the other way is just an exception for actual ingame SFX
        WorldCamera.update(demoWorld, cameraPlayer)


        // update UIs //
        uiContainer.forEach { it?.update(delta) }
    }

    fun renderScreen() {
        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())


        //camera.setToOrtho(true, AppLoader.terrarumAppConfig.screenWf, AppLoader.terrarumAppConfig.screenHf)

        // render world
        gdxClearAndSetBlend(.64f, .754f, .84f, 1f)


        if (!demoWorld.layerTerrain.ptr.destroyed) { // FIXME q&d hack to circumvent the dangling pointer issue #26
            IngameRenderer.invoke(gamePaused = false, uiContainer = uiContainer)
        }
        else {
            printdbgerr(this, "Demoworld is already been destroyed")
        }


        batch.inUse {
            setCameraPosition(0f, 0f)
            batch.shader = null
            batch.color = Color.WHITE
            renderOverlayTexts()
        }
    }

    private fun renderOverlayTexts() {
        setCameraPosition(0f, 0f)
        blendNormal(batch)
        batch.shader = null

        batch.color = Color.LIGHT_GRAY

        val COPYTING = arrayOf(
                AppLoader.COPYRIGHT_DATE_NAME,
                Lang["COPYRIGHT_GNU_GPL_3"]
        )

        COPYTING.forEachIndexed { index, s ->
            val textWidth = AppLoader.fontGame.getWidth(s)
            AppLoader.fontGame.draw(batch, s,
                    (AppLoader.screenSize.screenW - textWidth - 1f).toInt().toFloat(),
                    (AppLoader.screenSize.screenH - AppLoader.fontGame.lineHeight * (COPYTING.size - index) - 1f).toInt().toFloat()
            )
        }

        AppLoader.fontGame.draw(batch, "${AppLoader.GAME_NAME} ${AppLoader.getVERSION_STRING()}",
                1f.toInt().toFloat(),
                (AppLoader.screenSize.screenH - AppLoader.fontGame.lineHeight - 1f).toInt().toFloat()
        )

    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
        printdbg(this, "resize() called")
        printdbg(this, "called by:")
        printStackTrace(this)

        // Set up viewport when window is resized
        initViewPort(AppLoader.screenSize.screenW, AppLoader.screenSize.screenH)


        // resize UI by re-creating it (!!)
        uiMenu.resize(AppLoader.screenSize.screenW, AppLoader.screenSize.screenH)
        // TODO I forgot what the fuck kind of hack I was talking about
        //uiMenu.setPosition(0, UITitleRemoConRoot.menubarOffY)
        uiMenu.setPosition(0, 0) // shitty hack. Could be:
        // 1: Init code and resize code are different
        // 2: The UI is coded shit


        IngameRenderer.resize(AppLoader.screenSize.screenW, AppLoader.screenSize.screenH)

        printdbg(this, "resize() exit")
    }

    override fun dispose() {
        uiMenu.dispose()
        demoWorld.dispose()
    }



    fun setCameraPosition(newX: Float, newY: Float) {
        TerrarumIngame.setCameraPosition(batch, camera, newX, newY)
    }



    class TitleScreenController(val screen: TitleScreen) : InputAdapter() {
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            screen.uiContainer.forEach { it?.touchUp(screenX, screenY, pointer, button) }
            return true
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            screen.uiContainer.forEach { it?.mouseMoved(screenX, screenY) }
            return true
        }

        override fun keyTyped(character: Char): Boolean {
            screen.uiContainer.forEach { it?.keyTyped(character) }
            return true
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            screen.uiContainer.forEach { it?.scrolled(amountX, amountY) }
            return true
        }

        override fun keyUp(keycode: Int): Boolean {
            screen.uiContainer.forEach { it?.keyUp(keycode) }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            screen.uiContainer.forEach { it?.touchDragged(screenX, screenY, pointer) }
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            screen.uiContainer.forEach { it?.keyDown(keycode) }
            return true
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            screen.uiContainer.forEach { it?.touchDown(screenX, screenY, pointer, button) }
            return true
        }
    }

    private class CameraPlayer(val demoWorld: GameWorld, override val ai: ActorAI) : ActorWithBody(RenderOrder.FRONT, physProp = PhysProperties.MOBILE_OBJECT), AIControlled {

        override val hitbox = Hitbox(0.0, 0.0, 2.0, 2.0)

        init {
            hitbox.setPosition(
                    HQRNG().nextInt(demoWorld.width) * TILE_SIZED,
                    0.0 // Y pos: placeholder; camera AI will take it over
            )
        }

        override fun drawBody(batch: SpriteBatch) { }
        override fun drawGlow(batch: SpriteBatch) { }

        override fun update(delta: Float) {

        }

        override fun onActorValueChange(key: String, value: Any?) { }
        override fun dispose() { }

        override fun run() { TODO("not implemented") }

        override fun moveLeft(amount: Float) {
            TODO("not implemented")
        }

        override fun moveRight(amount: Float) {
            TODO("not implemented")
        }

        override fun moveUp(amount: Float) {
            TODO("not implemented")
        }

        override fun moveDown(amount: Float) {
            TODO("not implemented")
        }

        override fun moveJump(amount: Float) {
            TODO("not implemented")
        }

        override fun moveTo(bearing: Double) {
            TODO("not implemented")
        }

        override fun moveTo(toX: Double, toY: Double) {
            TODO("not implemented")
        }
    }

}