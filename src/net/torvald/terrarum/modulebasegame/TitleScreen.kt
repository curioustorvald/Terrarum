package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.Float16FrameBuffer
import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.clut.Skybox
import net.torvald.terrarum.console.CommandDict
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.ai.ActorAI
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.gameparticles.ParticleBase
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.UILoadGovernor
import net.torvald.terrarum.modulebasegame.ui.UIRemoCon
import net.torvald.terrarum.modulebasegame.ui.UITitleRemoConYaml
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.ReadSimpleWorld
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.utils.OpenURL
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.util.CircularArray
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by minjaesong on 2017-09-02.
 */
class TitleScreen(batch: FlippingSpriteBatch) : IngameInstance(batch) {

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
    private val cameraNodeWidth = 15
    private val lookaheadDist = cameraNodeWidth * TILE_SIZED
    private fun getPointAt(px: Double): Double {
        val ww = TILE_SIZEF * demoWorld.width
        val x = px % ww

        val indexThis = ((x / ww * cameraNodes.size).floorToInt())
        val xwstart: Double = indexThis.toDouble() / cameraNodes.size * ww
        val xwend: Double = ((indexThis + 1).toDouble() / cameraNodes.size) * ww
        val xw: Double = xwend - xwstart
        val xperc: Double = (x - xwstart) / xw

//        return FastMath.interpolateLinear(xperc.toFloat(), cameraNodes[indexThis fmod cameraNodes.size], cameraNodes[(indexThis + 1) fmod cameraNodes.size]).toDouble()
        return FastMath.interpolateCatmullRom(xperc.toFloat(), 1.0f, // somehow T=1 works really well thanks to my other smoothing technique
            cameraNodes[(indexThis - 1) fmod cameraNodes.size],
            cameraNodes[(indexThis - 0) fmod cameraNodes.size],
            cameraNodes[(indexThis + 1) fmod cameraNodes.size],
            cameraNodes[(indexThis + 2) fmod cameraNodes.size]
        ).toDouble()
    }
    private val cameraAI = object : ActorAI {
        private var firstTime = true


        override fun update(actor: Actor, delta: Float) {
            val ww = TILE_SIZEF * demoWorld.width
            val actor = actor as CameraPlayer

            val stride = cos(actor.bearing1A) * actor.actorValue.getAsDouble(AVKey.SPEED)!!

            val x1 = actor.hitbox.startX// + stride
            val y1 = actor.hitbox.startY

            val px1L = x1 - lookaheadDist
            val px1C = x1
            val px1R = x1 + lookaheadDist
            val py1L = getPointAt(px1L)
            val py1C = getPointAt(px1C)
            val py1R = getPointAt(px1R)

            val px2L = (px1L + px1C) / 2.0
            val px2R = (px1C + px1R) / 2.0
            val py2L = (py1L + py1C) / 2.0
            val py2R = (py1C + py1R) / 2.0

            val x2 = (px2L + px2R) / 2
            val y2 = (py2L + py2R) / 2

            val theta = atan2(py2R - py2L, px2R - px2L)

            if (firstTime) {
                firstTime = false
                actor.hitbox.setPosition(x1, getPointAt(x1))
            }
            else {
                actor.bearing1A = atan2(py1C - py1L, px1C - px1L)
                actor.bearing1B = atan2(py1R - py1C, px1R - px1C)
                actor.bearing2A = atan2(py2R - py2L, px2R - px2L)
                actor.moveTo(theta)
//                actor.hitbox.setPosition(x2, y2) // there is no reason it would work -- speed is wildly inconsistent as the angle reaches 90deg
            }

            if (actor.hitbox.startX > ww) {
                actor.hitbox.translatePosX(-ww.toDouble())
            }

        }
    }
    private lateinit var cameraPlayer: ActorWithBody

    private val gradWhiteTop = Color(0xf8f8f8ff.toInt())
    private val gradWhiteBottom = Color(0xd8d8d8ff.toInt())


    internal lateinit var uiRemoCon: UIRemoCon
    internal lateinit var uiFakeBlurOverlay: UICanvas

    private lateinit var worldFBO: Float16FrameBuffer

    private val warning32bitJavaIcon = TextureRegion(Texture(Gdx.files.internal("assets/graphics/gui/32_bit_warning.tga")))
    private val warningAppleRosettaIcon = TextureRegion(Texture(Gdx.files.internal("assets/graphics/gui/apple_rosetta_warning.tga")))

    init {
        gameUpdateGovernor = ConsistentUpdateRate.also { it.reset() }
    }

    private fun loadThingsWhileIntroIsVisible() {
        printdbg(this, "Intro pre-load")


        try {
            val file = ModMgr.getFile("basegame", "demoworld")
            val reader = java.io.FileReader(file)
            //ReadWorld.readWorldAndSetNewWorld(Terrarum.ingame!! as TerrarumIngame, reader)
            val world = ReadSimpleWorld(reader, file)
            demoWorld = world
            demoWorld.worldTime.timeDelta = 30
            printdbg(this, "Demo world loaded")
        }
        catch (e: IOException) {
            demoWorld = GameWorld(LandUtil.CHUNK_W, LandUtil.CHUNK_H, 0L, 0L)
            demoWorld.worldTime.timeDelta = 30
            printdbg(this, "Demo world not found, using empty world")
        }

        demoWorld.renumberTilesAfterLoad()
        this.world = demoWorld

        // set initial time to summer
        demoWorld.worldTime.addTime(WorldTime.DAY_LENGTH * 32)

        // construct camera nodes
        val nodeCount = demoWorld.width / cameraNodeWidth
        cameraNodes = kotlin.FloatArray(nodeCount) {
            val tileXPos = (demoWorld.width.toFloat() * it / nodeCount).floorToInt()
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
//            val offM2 = cameraNodes[(i-2) fmod cameraNodes.size] * 1f
            val offM1 = cameraNodes[(i-1) fmod cameraNodes.size] * 1f
            val off0 = cameraNodes[i] * 2f
            val off1 = cameraNodes[(i+1) fmod cameraNodes.size] * 1f
//            val off2 = cameraNodes[(i+2) fmod cameraNodes.size] * 1f

//            cameraNodes[i] = (offM2 + offM1 + off0 + off1 + off2) / 16f
            cameraNodes[i] = (offM1 + off0 + off1) / 4f
        }



        cameraPlayer = CameraPlayer(demoWorld, cameraAI)


        IngameRenderer.setRenderedWorld(demoWorld)
        WeatherMixer.internalReset(this)
        WeatherMixer.titleScreenInitWeather(demoWorld.weatherbox)


        // load a half-gradient texture that would be used throughout the titlescreen and its sub UIs
        CommonResourcePool.addToLoadingList("title_halfgrad") {
            Texture(Gdx.files.internal("./assets/graphics/halfgrad.tga")).also {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
        CommonResourcePool.loadAll()


        // fake UI for gradient overlay
        val uiFakeGradOverlay = UIFakeGradOverlay()
        uiFakeGradOverlay.setPosition(0, 0)
        uiContainer.add(uiFakeGradOverlay)


        // fake UI for blur
        uiFakeBlurOverlay = UIFakeBlurOverlay(1f, false)
        uiFakeBlurOverlay.setPosition(0,0)
        uiContainer.add(uiFakeBlurOverlay)


        uiRemoCon = UIRemoCon(this, UITitleRemoConYaml(App.savegamePlayers.isNotEmpty()))
        uiRemoCon.setPosition(0, 0)
        uiRemoCon.setAsOpen()


        uiContainer.add(uiRemoCon)

        CommandDict // invoke
        Skybox.loadlut() // invoke
//        Skybox.initiate() // invoke the lengthy calculation
        // TODO add console here


        //loadDone = true

        // measure bogoflops here
        val st = System.nanoTime()
        var sc = st
        var bogoflopf = Math.random()
        var bogoflops = 0L
        while (sc - st < 100000000L) {
            bogoflopf *= Math.random()
            bogoflops++
            sc = System.nanoTime()
        }
        bogoflops = Math.round(bogoflops.toDouble() * (1000000000.0 / (sc - st)))
        printdbg(this, "Bogoflops old: ${App.bogoflops} new: $bogoflops")
        App.bogoflops = maxOf(App.bogoflops, bogoflops)


        App.audioMixer.ambientTracks.forEach {
            it.stop()
            it.currentTrack = null
            it.nextTrack = null
        }
        App.audioMixer.reset()

    }


    override fun hide() {
    }

    override fun show() {
        printdbg(this, "show() called")

        for (k in Input.Keys.F1..Input.Keys.F12) {
            KeyToggler.forceSet(k, false)
        }

        initViewPort(App.scr.width, App.scr.height)


        Gdx.input.inputProcessor = TitleScreenController(this)


        worldFBO = Float16FrameBuffer(App.scr.width, App.scr.height, false)

        // load list of savegames
        printdbg(this, "update list of savegames")
        // to show "Continue" and "Load" on the titlescreen, uncomment this line
        App.updateListOfSavegames()
        UILoadGovernor.reset()

        loadThingsWhileIntroIsVisible()
        printdbg(this, "show() exit")
    }


    private val introUncoverTime: Second = 0.3f
    private var introUncoverDeltaCounter = 0f

    override fun render(updateRate: Float) {
        IngameRenderer.setRenderedWorld(demoWorld)

        super.render(updateRate)
        // async update and render
        gameUpdateGovernor.update(Gdx.graphics.deltaTime, App.UPDATE_RATE, updateScreen, renderScreen)
    }

    private val updateScreen = { delta: Float ->

        demoWorld.globalLight = WeatherMixer.globalLightNow
        demoWorld.updateWorldTime(delta)
        WeatherMixer.update(delta, cameraPlayer, demoWorld)
        cameraPlayer.update(delta)

        // worldcamera update AFTER cameraplayer in this case; the other way is just an exception for actual ingame SFX
        WorldCamera.update(demoWorld, cameraPlayer)

        // update UIs //
        uiContainer.forEach { it?.update(delta) }
    }

    private val particles = CircularArray<ParticleBase>(16, true)

    private fun drawLineOnWorld(x1: Float, y1: Float, x2: Float, y2: Float) {
        val w = 2.0f
        App.shapeRender.rectLine(
            x1 - WorldCamera.x, y1 - WorldCamera.y,
            x2 - WorldCamera.x, y2 - WorldCamera.y,
            w
        )
    }
    private fun drawLineOnWorld(x1: Double, y1: Double, x2: Double, y2: Double) {
        val ww = demoWorld.width * TILE_SIZE
        drawLineOnWorld(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())
        drawLineOnWorld(x1.toFloat() + ww, y1.toFloat(), x2.toFloat() + ww, y2.toFloat())
    }

    private val baseSlopeCol = Color(0f, 0.9f, 0.85f, 1f)
    private val firstOrderSlopeCol = Color(0f, 0.4f, 0f, 1f)
    private val secondOrderSlopeCol = Color(1f, 0.3f, 0.6f, 1f)

    private val renderScreen = { delta: Float ->
        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())


        //camera.setToOrtho(true, AppLoader.terrarumAppConfig.screenWf, AppLoader.terrarumAppConfig.screenHf)

        // render world
        gdxClearAndEnableBlend(.64f, .754f, .84f, 1f)


        if (!demoWorld.layerTerrain.ptr.destroyed) { // FIXME q&d hack to circumvent the dangling pointer issue #26
            IngameRenderer.invoke(
                delta,
                false,
                1f,
                listOf(),
                listOf(),
                listOf(),
                listOf(),
                listOf(),
                particles,
                uiContainer = uiContainer
            )

            if (KeyToggler.isOn(Input.Keys.F10)) {
                App.shapeRender.inUse {

                    val actor = cameraPlayer as CameraPlayer

                    val x1 = actor.hitbox.startX
                    val y1 = actor.hitbox.startY

                    val px1L = x1 - lookaheadDist// * cos(actor.bearing1A)
                    val px1C = x1
                    val px1R = x1 + lookaheadDist// * cos(actor.bearing1B)
                    val py1L = getPointAt(px1L)
                    val py1C = getPointAt(px1C)
                    val py1R = getPointAt(px1R)

                    val px2L = (px1L + px1C) / 2.0
                    val px2R = (px1C + px1R) / 2.0
                    val py2L = (py1L + py1C) / 2.0
                    val py2R = (py1C + py1R) / 2.0

                    it.color = firstOrderSlopeCol
                    drawLineOnWorld(px1L, py1L, px1C, py1C)
                    drawLineOnWorld(px1C, py1C, px1R, py1R)

                    /*(1..cameraNodes.lastIndex + 16).forEach { index0 ->
                        val x1 = (index0 - 1) * cameraNodeWidth * TILE_SIZEF; val x2 = (index0 - 0) * cameraNodeWidth * TILE_SIZEF
                        val y1 = cameraNodes[(index0 - 1) fmod cameraNodes.size]; val y2 = cameraNodes[index0 fmod cameraNodes.size]
                        drawLineOnWorld(x1, y1, x2, y2)
                    }*/
                    it.color = baseSlopeCol
                    val points = (0..App.scr.width).map { x ->
                        val worldX = (WorldCamera.x + x).toDouble()
                        worldX to getPointAt(worldX)
                    }
                    points.forEachIndexed { index, (x, y) ->
                        if (index > 0) {
                            drawLineOnWorld(points[index - 1].first, points[index - 1].second, x, y)
                        }
                    }

                    it.color = secondOrderSlopeCol
                    drawLineOnWorld(px2L, py2L, px2R, py2R)

                }
            }
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
        blendNormalStraightAlpha(batch)
        batch.shader = null

        batch.color = Color.LIGHT_GRAY

        val COPYTING = arrayOf(
                TerrarumAppConfiguration.COPYRIGHT_DATE_NAME,
                TerrarumAppConfiguration.COPYRIGHT_LICENSE
        )
        val drawWidth = Toolkit.drawWidth

        COPYTING.forEachIndexed { index, s ->
            val textWidth = App.fontGame.getWidth(s)
            App.fontGame.draw(batch, s,
                    (drawWidth - textWidth - 1f).toInt().toFloat(),
                    (App.scr.height - App.fontGame.lineHeight * (COPYTING.size - index) - 1f).toInt().toFloat()
            )
        }

        batch.color = if (App.IS_DEVELOPMENT_BUILD) Toolkit.Theme.COL_MOUSE_UP else Color.LIGHT_GRAY
        App.fontGame.draw(batch, TerrarumPostProcessor.thisIsDebugStr, 5f, App.scr.height - 24f)



        batch.color = Color.WHITE

        val linegap = 4
        val imgTxtGap = 10
        val yoff = App.scr.height - App.scr.tvSafeGraphicsHeight - 64 - (3*(20+linegap)) - imgTxtGap - 9
        if (uiRemoCon.currentRemoConContents.parent == null) {
            // warn: 32-bit
            var texts = emptyList<String>()
            var textcols = emptyList<Color>()
            if (App.is32BitJVM) {
                Toolkit.drawCentered(batch, warning32bitJavaIcon, yoff)
                texts = (1..3).map { Lang.get("GAME_32BIT_WARNING$it", (it != 3)) }
                textcols = (1..3).map { if (it == 3) Toolkit.Theme.COL_SELECTED else Color.WHITE }
            }
            // warn: rosetta on Apple M-chips
            else if (App.getUndesirableConditions() == "apple_execution_through_rosetta") {
                Toolkit.drawCentered(batch, warningAppleRosettaIcon, yoff)
                texts = (1..2).map { Lang.get("GAME_APPLE_ROSETTA_WARNING$it") }
                textcols = texts.map { Color.WHITE }
            }


            texts.forEachIndexed { i, text ->
                batch.color = textcols[i]
                App.fontGame.draw(
                    batch,
                    text,
                    ((drawWidth - App.fontGame.getWidth(text)) / 2).toFloat(),
                    yoff + imgTxtGap + 64f + linegap + i * (20 + linegap)
                )
            }


            // update available!
            if (App.hasUpdate) {

                batch.color = if (System.currentTimeMillis() % 1500 < 750L)
                    Toolkit.Theme.COL_MOUSE_UP
                else
                    Toolkit.Theme.COL_SELECTED

                val tx = UIRemoCon.menubarOffX + UIRemoCon.UIRemoConElement.paddingLeft / 2 + uiRemoCon.posX
                val ty1 = UIRemoCon.menubarOffY - uiRemoCon.height + uiRemoCon.posY - 60
                val ty2 = ty1 + 28
                App.fontGame.draw(
                    batch,
                    Lang["MENU_UPDATE_UPDATE_AVAILABLE"],
                    tx, ty1
                )


                val tw = App.fontGame.getWidth("<${TerrarumAppConfiguration.FIXED_LATEST_DOWNLOAD_LINK}>")
                if (Terrarum.mouseScreenX in tx - 32 until tx + tw + 32 &&
                    Terrarum.mouseScreenY in ty2 - 16 until ty2 + App.fontGame.lineHeight.toInt() + 16) {

                    if (Gdx.input.isButtonJustPressed(App.getConfigInt("config_mouseprimary"))) {
                        OpenURL(TerrarumAppConfiguration.FIXED_LATEST_DOWNLOAD_LINK)
                    }
                    batch.color = Toolkit.Theme.COL_SELECTED
                }
                else
                    batch.color = Toolkit.Theme.COL_MOUSE_UP

                App.fontGame.draw(
                    batch,
                    "<${TerrarumAppConfiguration.FIXED_LATEST_DOWNLOAD_LINK}>",
                    tx, ty2
                )

            }
        }

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
        uiRemoCon.resize(App.scr.width, App.scr.height)
        // TODO I forgot what the fuck kind of hack I was talking about
        //uiMenu.setPosition(0, UITitleRemoConRoot.menubarOffY)
        uiRemoCon.setPosition(0, 0) // shitty hack. Could be:
        // 1: Init code and resize code are different
        // 2: The UI is coded shit


        IngameRenderer.resize(App.scr.width, App.scr.height)

        printdbg(this, "resize() exit")
    }

    override fun dispose() {
        uiRemoCon.dispose()
        demoWorld.dispose()
        warning32bitJavaIcon.texture.dispose()
        warningAppleRosettaIcon.texture.dispose()
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        uiContainer.forEach { it?.inputStrobed(e) }
    }



    fun setCameraPosition(newX: Float, newY: Float) {
        TerrarumIngame.setCameraPosition(batch, App.shapeRender, camera, newX, newY)
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

    private class CameraPlayer(val demoWorld: GameWorld, override var ai: ActorAI) : ActorWithBody(RenderOrder.FRONT, physProp = PhysProperties.MOBILE_OBJECT()), AIControlled {

        override val hitbox = Hitbox(0.0, 0.0, 2.0, 2.0)

        init {
            actorValue[AVKey.SPEED] = 1.666 * (if (Math.random() < 1.0 / 65536.0) -1 else 1) // some easter egg
            hitbox.setPosition(
                    HQRNG().nextInt(demoWorld.width) * TILE_SIZED,
                    0.0 // Y pos: placeholder; camera AI will take it over
            )
        }

        override fun drawBody(frameDelta: Float, batch: SpriteBatch) { }
        override fun drawGlow(frameDelta: Float, batch: SpriteBatch) { }

        override fun updateImpl(delta: Float) {
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
        var currentBearing = 0.0

        var bearing1A = 0.0
        var bearing1B = 0.0
        var bearing1C = 0.0
        var bearing2A = 0.0
        var bearing2B = 0.0
        var bearing3A = 0.0

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