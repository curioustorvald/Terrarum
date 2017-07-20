package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.serialise.ReadLayerData
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIHandler
import net.torvald.terrarum.ui.UIStartMenu
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera
import java.io.FileInputStream

class TitleScreen(val batch: SpriteBatch) : Screen {

    var camera = OrthographicCamera(Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())

    // invert Y
    fun initViewPort(width: Int, height: Int) {
        //val width = if (width % 1 == 1) width + 1 else width
        //val height = if (height % 1 == 1) height + 1 else width

        // Set Y to point downwards
        camera.setToOrtho(true, width.toFloat(), height.toFloat())

        // Update camera matrix
        camera.update()

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height)
    }


    private var loadDone = false

    private lateinit var demoWorld: GameWorld
    private val cameraPlayer = object : ActorWithBody(RenderOrder.BEHIND) {
        override fun drawBody(batch: SpriteBatch) { }
        override fun drawGlow(batch: SpriteBatch) { }
        override fun dispose() { }
        override fun onActorValueChange(key: String, value: Any?) { }
        override fun run() { }

        override fun update(delta: Float) {
            // camera walk?
        }
    }

    private val gradWhiteTop = Color(0xf8f8f8ff.toInt())
    private val gradWhiteBottom = Color(0xd8d8d8ff.toInt())

    private val lightFBOformat = Pixmap.Format.RGB888
    var lightmapFboA = FrameBuffer(lightFBOformat, Terrarum.WIDTH.div(Ingame.lightmapDownsample.toInt()), Terrarum.HEIGHT.div(Ingame.lightmapDownsample.toInt()), false)
    var lightmapFboB = FrameBuffer(lightFBOformat, Terrarum.WIDTH.div(Ingame.lightmapDownsample.toInt()), Terrarum.HEIGHT.div(Ingame.lightmapDownsample.toInt()), false)

    lateinit var logo: TextureRegion

    val uiContainer = ArrayList<UIHandler>()
    private lateinit var uiMenu: UIHandler

    private fun loadThingsWhileIntroIsVisible() {
        demoWorld = ReadLayerData(FileInputStream(ModMgr.getFile("basegame", "demoworld")))

        cameraPlayer.hitbox.setPosition(
                demoWorld.spawnX * FeaturesDrawer.TILE_SIZE.toDouble(),
                demoWorld.spawnY * FeaturesDrawer.TILE_SIZE.toDouble()
        )
        cameraPlayer.hitbox.setDimension(2.0, 2.0)

        demoWorld.time.timeDelta = 60


        LightmapRenderer.world = demoWorld
        BlocksDrawer.world = demoWorld
        FeaturesDrawer.world = demoWorld


        uiMenu = UIHandler(UIStartMenu())
        uiMenu.setPosition(0, UIStartMenu.menubarOffY)
        uiMenu.setAsOpen()


        uiContainer.add(uiMenu)

        loadDone = true
    }


    override fun hide() {
    }

    override fun show() {
        initViewPort(Terrarum.WIDTH, Terrarum.HEIGHT)

        logo = TextureRegion(Texture(Gdx.files.internal("assets/graphics/logo_placeholder.tga")))
        logo.flip(false, true)


        Gdx.input.inputProcessor = TitleScreenController(this)
    }

    private var blurWriteBuffer = lightmapFboA
    private var blurReadBuffer = lightmapFboB

    private val minimumIntroTime = 1.0f
    private var deltaCounter = 0f

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (!loadDone || deltaCounter < minimumIntroTime) {
            // draw load screen
            Terrarum.shaderBayerSkyboxFill.begin()
            Terrarum.shaderBayerSkyboxFill.setUniformMatrix("u_projTrans", camera.combined)
            Terrarum.shaderBayerSkyboxFill.setUniformf("topColor", gradWhiteTop.r, gradWhiteTop.g, gradWhiteTop.b)
            Terrarum.shaderBayerSkyboxFill.setUniformf("bottomColor", gradWhiteBottom.r, gradWhiteBottom.g, gradWhiteBottom.b)
            Terrarum.fullscreenQuad.render(Terrarum.shaderBayerSkyboxFill, GL20.GL_TRIANGLES)
            Terrarum.shaderBayerSkyboxFill.end()

            batch.inUse {
                batch.color = Color.WHITE
                blendNormal()
                batch.shader = null


                setCameraPosition(0f, 0f)
                batch.draw(logo, (Terrarum.WIDTH - logo.regionWidth) / 2f, (Terrarum.HEIGHT - logo.regionHeight) / 2f)
            }

            if (!loadDone) {
                loadThingsWhileIntroIsVisible()
            }
        }
        else {
            //if (Terrarum.GLOBAL_RENDER_TIMER % 2 == 1) {
                LightmapRenderer.fireRecalculateEvent()
            //}


            cameraPlayer.hitbox.setPosition(1024 * 16.0, 340 * 16.0)


            demoWorld.updateWorldTime(delta)
            WeatherMixer.update(delta, cameraPlayer)
            cameraPlayer.update(delta)
            // worldcamera update AFTER cameraplayer in this case; the other way is just an exception for actual ingame SFX
            WorldCamera.update(demoWorld, cameraPlayer)


            // update UIs //
            uiContainer.forEach { it.update(delta) }


            // render and blur lightmap
            //processBlur(LightmapRenderer.DRAW_FOR_RGB)

            // render world
            batch.inUse {
                batch.shader = null
                camera.position.set(WorldCamera.gdxCamX, WorldCamera.gdxCamY, 0f) // make camara work
                camera.update()
                batch.projectionMatrix = camera.combined
                batch.color = Color.WHITE
                blendNormal()



                renderDemoWorld()

                renderMenus()

                renderOverlayTexts()
            }

        }



        deltaCounter += delta
    }

    private fun renderDemoWorld() {
        // draw skybox //

        setCameraPosition(0f, 0f)
        batch.color = Color.WHITE
        blendNormal()
        WeatherMixer.render(camera, demoWorld)


        // draw tiles //

        // using custom code for camera; this is obscure and tricky
        camera.position.set(WorldCamera.gdxCamX, WorldCamera.gdxCamY, 0f) // make camara work
        camera.update()
        batch.projectionMatrix = camera.combined
        batch.shader = null

        blendNormal()
        BlocksDrawer.renderWall(batch)
        BlocksDrawer.renderTerrain(batch)
        BlocksDrawer.renderFront(batch, false)
        FeaturesDrawer.drawEnvOverlay(batch)
    }

    private fun renderMenus() {
        setCameraPosition(0f, 0f)
        blendNormal()
        batch.shader = null


        uiContainer.forEach { it.render(batch, camera) }
    }

    private fun renderOverlayTexts() {
        setCameraPosition(0f, 0f)
        blendNormal()
        batch.shader = null

        batch.color = Color.LIGHT_GRAY

        val COPYTING = arrayOf(
                COPYRIGHT_DATE_NAME,
                Lang["COPYRIGHT_GNU_GPL_3"]
        )

        COPYTING.forEachIndexed { index, s ->
            val textWidth = Terrarum.fontGame.getWidth(s)
            Terrarum.fontGame.draw(batch, s,
                    Terrarum.WIDTH - textWidth - 1f - 0.667f,
                    Terrarum.HEIGHT - Terrarum.fontGame.lineHeight * (COPYTING.size - index) - 1f
            )
        }
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
        // Set up viewport when window is resized
        initViewPort(Terrarum.WIDTH, Terrarum.HEIGHT)


        if (loadDone) {
            // resize UI by re-creating it (!!)
            uiMenu.UI.resize(Terrarum.WIDTH, Terrarum.HEIGHT)
            uiMenu.setPosition(0, UIStartMenu.menubarOffY)
        }
    }

    override fun dispose() {
        logo.texture.dispose()
        lightmapFboA.dispose()
        lightmapFboB.dispose()

        uiMenu.dispose()
    }



    fun setCameraPosition(newX: Float, newY: Float) {
        Ingame.setCameraPosition(batch, camera, newX, newY)
    }


    fun processBlur(mode: Int) {
        val blurIterations = 5 // ideally, 4 * radius; must be even/odd number -- odd/even number will flip the image
        val blurRadius = 4f / Ingame.lightmapDownsample // (5, 4f); using low numbers for pixel-y aesthetics

        blurWriteBuffer = lightmapFboA
        blurReadBuffer = lightmapFboB


        lightmapFboA.inAction(null, null) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }
        lightmapFboB.inAction(null, null) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }


        if (mode == LightmapRenderer.DRAW_FOR_RGB) {
            // initialise readBuffer with untreated lightmap
            blurReadBuffer.inAction(camera, batch) {
                batch.inUse {
                    // using custom code for camera; this is obscure and tricky
                    camera.position.set(
                            (WorldCamera.gdxCamX / Ingame.lightmapDownsample).round(),
                            (WorldCamera.gdxCamY / Ingame.lightmapDownsample).round(),
                            0f
                    ) // make camara work
                    camera.update()
                    batch.projectionMatrix = camera.combined


                    blendNormal()
                    batch.color = Color.WHITE
                    LightmapRenderer.draw(batch, LightmapRenderer.DRAW_FOR_RGB)
                }
            }
        }
        else {
            // initialise readBuffer with untreated lightmap
            blurReadBuffer.inAction(camera, batch) {
                batch.inUse {
                    // using custom code for camera; this is obscure and tricky
                    camera.position.set(
                            (WorldCamera.gdxCamX / Ingame.lightmapDownsample).round(),
                            (WorldCamera.gdxCamY / Ingame.lightmapDownsample).round(),
                            0f
                    ) // make camara work
                    camera.update()
                    batch.projectionMatrix = camera.combined


                    blendNormal()
                    batch.color = Color.WHITE
                    LightmapRenderer.draw(batch, LightmapRenderer.DRAW_FOR_ALPHA)
                }
            }
        }



        for (i in 0 until blurIterations) {
            blurWriteBuffer.inAction(camera, batch) {

                batch.inUse {
                    val texture = blurReadBuffer.colorBufferTexture

                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)


                    batch.shader = Terrarum.shaderBlur
                    batch.shader.setUniformf("iResolution",
                            blurWriteBuffer.width.toFloat(), blurWriteBuffer.height.toFloat())
                    batch.shader.setUniformf("flip", 1f)
                    if (i % 2 == 0)
                        batch.shader.setUniformf("direction", blurRadius, 0f)
                    else
                        batch.shader.setUniformf("direction", 0f, blurRadius)


                    batch.color = Color.WHITE
                    batch.draw(texture, 0f, 0f)


                    // swap
                    val t = blurWriteBuffer
                    blurWriteBuffer = blurReadBuffer
                    blurReadBuffer = t
                }
            }
        }

    }


    class TitleScreenController(val screen: TitleScreen) : InputAdapter() {
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            screen.uiContainer.forEach { it.touchUp(screenX, screenY, pointer, button) }
            return true
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            screen.uiContainer.forEach { it.mouseMoved(screenX, screenY) }
            return true
        }

        override fun keyTyped(character: Char): Boolean {
            screen.uiContainer.forEach { it.keyTyped(character) }
            return true
        }

        override fun scrolled(amount: Int): Boolean {
            screen.uiContainer.forEach { it.scrolled(amount) }
            return true
        }

        override fun keyUp(keycode: Int): Boolean {
            screen.uiContainer.forEach { it.keyUp(keycode) }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            screen.uiContainer.forEach { it.touchDragged(screenX, screenY, pointer) }
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            screen.uiContainer.forEach { it.keyDown(keycode) }
            return true
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            screen.uiContainer.forEach { it.touchDown(screenX, screenY, pointer, button) }
            return true
        }
    }
}