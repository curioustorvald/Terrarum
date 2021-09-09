package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.console.CommandDict
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.ai.ActorAI
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.UIRemoCon
import net.torvald.terrarum.modulebasegame.ui.UITitleRemoConYaml
import net.torvald.terrarum.serialise.ReadWorld
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.WorldCamera
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by minjaesong on 2017-09-02.
 */
class TitleScreen(batch: SpriteBatch) : IngameInstance(batch) {

    // todo register titlescreen as the ingame, similar in a way that the buildingmaker did

    var camera = OrthographicCamera(App.scr.wf, App.scr.hf)


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

    private lateinit var demoWorld: GameWorld
    private lateinit var cameraNodes: FloatArray // camera Y-pos
    private val cameraAI = object : ActorAI {

        private var firstTime = true
        private val lookaheadDist = 100.0

        private fun getPointAt(px: Double): Float {
            val ww = TILE_SIZEF * demoWorld.width
            val x = px % ww

            val indexThis = ((x / ww * cameraNodes.size).floorInt()) fmod cameraNodes.size
            val xwstart: Float = indexThis.toFloat() / cameraNodes.size * ww
            val xwend: Float = ((indexThis + 1).toFloat() / cameraNodes.size) * ww
            val xw: Float = xwend - xwstart
            val xperc: Double = (x - xwstart) / xw

            return FastMath.interpolateLinear(xperc.toFloat(), cameraNodes[indexThis], cameraNodes[(indexThis + 1) % cameraNodes.size])
        }

        override fun update(actor: Actor, delta: Float) {
            val ww = TILE_SIZEF * demoWorld.width
            val actor = actor as CameraPlayer

            val px: Double = actor.hitbox.canonicalX + actor.actorValue.getAsDouble(AVKey.SPEED)!!
            val pxP = px - lookaheadDist * cos(actor.targetBearing)
            val pxN = px + lookaheadDist * cos(actor.targetBearing)

            val yP = getPointAt(pxP)
            val yN = getPointAt(pxN)

            val y = (yP + yN) / 2f

            if (firstTime) {
                firstTime = false
                actor.hitbox.setPositionY(y - 8.0)
            }
            else {
                //actor.moveTo(px, y - 8.0)
                //actor.hitbox.setPosition(px, y - 8.0)
                actor.moveTo(atan2((yN - yP).toDouble(), pxN - pxP))
            }

            if (actor.hitbox.canonicalX > ww) {
                actor.hitbox.translatePosX(-ww.toDouble())
            }
        }
    }
    private lateinit var cameraPlayer: ActorWithBody

    private val gradWhiteTop = Color(0xf8f8f8ff.toInt())
    private val gradWhiteBottom = Color(0xd8d8d8ff.toInt())


    val uiContainer = UIContainer()
    private lateinit var uiMenu: UICanvas
    internal lateinit var uiFakeBlurOverlay: UICanvas

    private lateinit var worldFBO: FrameBuffer

    private fun loadThingsWhileIntroIsVisible() {
        printdbg(this, "Intro pre-load")


        try {
            val reader = java.io.FileReader(ModMgr.getFile("basegame", "demoworld"))
            //ReadWorld.readWorldAndSetNewWorld(Terrarum.ingame!! as TerrarumIngame, reader)
            val world = ReadWorld.invoke(reader)
            demoWorld = world
            printdbg(this, "Demo world loaded")
        }
        catch (e: IOException) {
            demoWorld = GameWorld(1, 64, 64, 0L, 0L, 0)
            printdbg(this, "Demo world not found, using empty world")
        }


        // set time to summer
        demoWorld.worldTime.addTime(WorldTime.DAY_LENGTH * 32)

        // construct camera nodes
        val nodeCount = demoWorld.width / 10
        cameraNodes = kotlin.FloatArray(nodeCount) {
            val tileXPos = (demoWorld.width.toFloat() * it / nodeCount).floorInt()
            var travelDownCounter = 0
            while (travelDownCounter < demoWorld.height &&
                   !BlockCodex[demoWorld.getTileFromTerrain(tileXPos, travelDownCounter)].isSolid
//                   !BlockCodex[demoWorld.getTileFromWall(tileXPos, travelDownCounter)].isSolid
            ) {
                travelDownCounter += 2
            }
            travelDownCounter * TILE_SIZEF
        }
        // apply gaussian blur to the camera nodes
        for (i in cameraNodes.indices) {
            val offM2 = cameraNodes[(i-2) fmod cameraNodes.size] * 0.05f
            val offM1 = cameraNodes[(i-1) fmod cameraNodes.size] * 0.2f
            val off0 = cameraNodes[i] * 0.5f
            val off1 = cameraNodes[(i+1) fmod cameraNodes.size] * 0.2f
            val off2 = cameraNodes[(i+2) fmod cameraNodes.size] * 0.05f

            cameraNodes[i] = offM2 + offM1 + off0 + off1 + off2
        }



        cameraPlayer = CameraPlayer(demoWorld, cameraAI)

        demoWorld.worldTime.timeDelta = 0// 100


        IngameRenderer.setRenderedWorld(demoWorld)


        // load a half-gradient texture that would be used throughout the titlescreen and its sub UIs
        CommonResourcePool.addToLoadingList("title_halfgrad") { Texture(Gdx.files.internal("./assets/graphics/halfgrad.png")) }
        CommonResourcePool.loadAll()


        // fake UI for gradient overlay
        val uiFakeGradOverlay = UIFakeGradOverlay()
        uiFakeGradOverlay.setPosition(0, 0)
        uiContainer.add(uiFakeGradOverlay)


        // fake UI for blur
        uiFakeBlurOverlay = UIFakeBlurOverlay()
        uiFakeBlurOverlay.setPosition(0,0)
        uiContainer.add(uiFakeBlurOverlay)


        uiMenu = UIRemoCon(this, UITitleRemoConYaml())//UITitleRemoConRoot()
        uiMenu.setPosition(0, 0)
        uiMenu.setAsOpen()


        uiContainer.add(uiMenu)

        CommandDict // invoke
        // TODO add console here


        //loadDone = true
    }


    override fun hide() {
    }

    override fun show() {
        printdbg(this, "show() called")

        initViewPort(App.scr.width, App.scr.height)


        Gdx.input.inputProcessor = TitleScreenController(this)


        worldFBO = FrameBuffer(Pixmap.Format.RGBA8888, App.scr.width, App.scr.height, false)

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

        val dt = Gdx.graphics.deltaTime
        updateAkku += dt

        var i = 0L
        while (updateAkku >= updateRate) {
            App.measureDebugTime("Ingame.Update") { updateScreen(updateRate) }
            updateAkku -= updateRate
            i += 1
        }
        App.setDebugTime("Ingame.UpdateCounter", i)


        // render? just do it anyway
        App.measureDebugTime("Ingame.Render") { renderScreen() }
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
                TerrarumAppConfiguration.COPYRIGHT_DATE_NAME,
                TerrarumAppConfiguration.COPYRIGHT_LICENSE
        )

        COPYTING.forEachIndexed { index, s ->
            val textWidth = App.fontGame.getWidth(s)
            App.fontGame.draw(batch, s,
                    (App.scr.width - textWidth - 1f).toInt().toFloat(),
                    (App.scr.height - App.fontGame.lineHeight * (COPYTING.size - index) - 1f).toInt().toFloat()
            )
        }

        App.fontGame.draw(batch, PostProcessor.thisIsDebugStr, 5f, App.scr.height - 24f)

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
        initViewPort(App.scr.width, App.scr.height)


        // resize UI by re-creating it (!!)
        uiMenu.resize(App.scr.width, App.scr.height)
        // TODO I forgot what the fuck kind of hack I was talking about
        //uiMenu.setPosition(0, UITitleRemoConRoot.menubarOffY)
        uiMenu.setPosition(0, 0) // shitty hack. Could be:
        // 1: Init code and resize code are different
        // 2: The UI is coded shit


        IngameRenderer.resize(App.scr.width, App.scr.height)

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

    private class CameraPlayer(val demoWorld: GameWorld, override var ai: ActorAI) : ActorWithBody(RenderOrder.FRONT, physProp = PhysProperties.MOBILE_OBJECT), AIControlled {

        override val hitbox = Hitbox(0.0, 0.0, 2.0, 2.0)

        init {
            actorValue[AVKey.SPEED] = 1.666
            hitbox.setPosition(
                    HQRNG().nextInt(demoWorld.width) * TILE_SIZED,
                    0.0 // Y pos: placeholder; camera AI will take it over
            )
        }

        override fun drawBody(batch: SpriteBatch) { }
        override fun drawGlow(batch: SpriteBatch) { }

        override fun update(delta: Float) {
            ai.update(this, delta)
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

        var targetBearing = 0.0
        var currentBearing = Double.NaN

        override fun moveTo(bearing: Double) {
            targetBearing = bearing
            if (currentBearing.isNaN()) currentBearing = bearing
            val v = actorValue.getAsDouble(AVKey.SPEED)!!

            //currentBearing = interpolateLinear(1.0 / 22.0, currentBearing, targetBearing)
            currentBearing = targetBearing
            hitbox.translate(v * cos(currentBearing), v * sin(currentBearing))
        }

        override fun moveTo(toX: Double, toY: Double) {
            val ww = TILE_SIZED * demoWorld.width
            // select appropriate toX because ROUNDWORLD
            val xdiff1 = toX - hitbox.canonicalX
            val xdiff2 = (toX + ww) - hitbox.canonicalX

            val xdiff = if (xdiff1 < 0) xdiff2 else xdiff1
            val ydiff = toY - hitbox.canonicalY
            moveTo(atan2(ydiff, xdiff))
            hitbox.setPositionX(hitbox.canonicalX % ww)
        }
    }

}