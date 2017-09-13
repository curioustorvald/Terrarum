package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.ai.ActorAI
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.serialise.ReadLayerData
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UITitleRemoConRoot
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.*
import java.io.FileInputStream

/**
 * Created by minjaesong on 2017-09-02.
 */
class FuckingWorldRenderer(val batch: SpriteBatch) : Screen {

    var camera = OrthographicCamera(Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())


    // invert Y
    fun initViewPort(width: Int, height: Int) {
        // Set Y to point downwards
        camera.setToOrtho(true, width.toFloat(), height.toFloat())

        // Update camera matrix
        camera.update()

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height)
    }


    private var loadDone = false

    private lateinit var demoWorld: GameWorld
    private lateinit var cameraNodes: FloatArray // camera Y-pos
    private val cameraAI = object : ActorAI {
        private val axisMax = 1f

        private var firstTime = true

        override fun update(actor: HumanoidNPC, delta: Float) {
            // fuck
            val avSpeed = 1.0 // FIXME camera goes faster when FPS is high
            actor.actorValue[AVKey.SPEED] = avSpeed
            actor.actorValue[AVKey.ACCEL] = avSpeed / 6.0
            // end fuck



            val tileSize = FeaturesDrawer.TILE_SIZE.toFloat()
            val catmullRomTension = 0f

            // pan camera
            actor.moveRight(axisMax)


            val domainSize = demoWorld.width * tileSize
            val codomainSize = cameraNodes.size
            val x = actor.hitbox.canonicalX.toFloat()

            val p1 = (x / (domainSize / codomainSize)).floorInt()
            val p0 = (p1 - 1) fmod codomainSize
            val p2 = (p1 + 1) fmod codomainSize
            val p3 = (p1 + 2) fmod codomainSize
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
    private lateinit var cameraPlayer: HumanoidNPC

    private val gradWhiteTop = Color(0xf8f8f8ff.toInt())
    private val gradWhiteBottom = Color(0xd8d8d8ff.toInt())

    private val lightFBOformat = Pixmap.Format.RGB888
    lateinit var lightmapFboA: FrameBuffer
    lateinit var lightmapFboB: FrameBuffer
    private var lightmapInitialised = false // to avoid nullability of lightmapFBO

    lateinit var logo: TextureRegion

    val uiContainer = ArrayList<UICanvas>()
    private lateinit var uiMenu: UICanvas

    private lateinit var worldFBO: FrameBuffer

    private val TILE_SIZE = FeaturesDrawer.TILE_SIZE
    private val TILE_SIZEF = TILE_SIZE.toFloat()

    private fun loadThingsWhileIntroIsVisible() {
        demoWorld = ReadLayerData(FileInputStream(ModMgr.getFile("basegame", "demoworld")))


        // construct camera nodes
        val nodeCount = 100
        cameraNodes = kotlin.FloatArray(nodeCount, { it ->
            val tileXPos = (demoWorld.width.toFloat() * it / nodeCount).floorInt()
            var travelDownCounter = 0
            while (!BlockCodex[demoWorld.getTileFromTerrain(tileXPos, travelDownCounter)].isSolid) {
                travelDownCounter += 4
            }
            travelDownCounter * FeaturesDrawer.TILE_SIZE.toFloat()
        })


        cameraPlayer = object : HumanoidNPC(demoWorld, cameraAI, GameDate(1, 1), usePhysics = false) {
            init {
                setHitboxDimension(2, 2, 0, 0)
                hitbox.setPosition(
                        HQRNG().nextInt(demoWorld.width) * FeaturesDrawer.TILE_SIZE.toDouble(),
                        0.0 // placeholder; camera AI will take it over
                )
                noClip = true
            }
        }

        demoWorld.time.timeDelta = 150


        LightmapRendererNew.world = demoWorld
        BlocksDrawer.world = demoWorld
        FeaturesDrawer.world = demoWorld


        uiMenu = UITitleRemoConRoot()
        uiMenu.setPosition(0, 0)
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


        worldFBO = FrameBuffer(Pixmap.Format.RGBA8888, Terrarum.WIDTH, Terrarum.HEIGHT, false)
    }


    private val introUncoverTime: Second = 0.3f
    private var introUncoverDeltaCounter = 0f
    private var updateDeltaCounter = 0.0
    protected val updateRate = 1.0 / Terrarum.TARGET_INTERNAL_FPS

    override fun render(delta: Float) {
        if (!loadDone) {
            loadThingsWhileIntroIsVisible()
        }
        else {
            // async update
            updateDeltaCounter += delta
            while (updateDeltaCounter >= updateRate) {
                updateScreen(delta)
                updateDeltaCounter -= updateRate
            }

            // render? just do it anyway
            renderScreen()
        }
    }

    fun updateScreen(delta: Float) {
        Gdx.graphics.setTitle(TerrarumAppLoader.GAME_NAME +
                              " — F: ${Gdx.graphics.framesPerSecond} (${Terrarum.TARGET_INTERNAL_FPS})" +
                              " — M: ${Terrarum.memInUse}M / ${Terrarum.memTotal}M / ${Terrarum.memXmx}M"
        )

        demoWorld.globalLight = WeatherMixer.globalLightNow
        demoWorld.updateWorldTime(delta)
        WeatherMixer.update(delta, cameraPlayer)
        cameraPlayer.update(delta)

        // worldcamera update AFTER cameraplayer in this case; the other way is just an exception for actual ingame SFX
        WorldCamera.update(demoWorld, cameraPlayer)


        // update UIs //
        uiContainer.forEach { it.update(delta) }



        LightmapRendererNew.fireRecalculateEvent() // don't half-frame update; it will jitter!
    }

    fun renderScreen() {

        processBlur(LightmapRendererNew.DRAW_FOR_RGB)
        //camera.setToOrtho(true, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())

        // render world
        Gdx.gl.glClearColor(.64f, .754f, .84f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        batch.inUse {
            setCameraPosition(0f, 0f)
            batch.shader = null

            Gdx.gl.glEnable(GL20.GL_BLEND)
            renderDemoWorld()

            batch.shader = null
            batch.color = Color.WHITE
            renderMenus()
            renderOverlayTexts()
        }
    }

    private fun renderDemoWorld() {
        // draw skybox //

        setCameraPosition(0f, 0f)
        batch.color = Color.WHITE
        blendNormal()
        WeatherMixer.render(camera, demoWorld)


        // draw tiles //
        BlocksDrawer.renderWall(batch)
        BlocksDrawer.renderTerrain(batch)


        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // don't know why it is needed; it really depresses me


        FeaturesDrawer.drawEnvOverlay(batch)


        ///////////////////
        // draw lightmap //
        ///////////////////

        setCameraPosition(0f, 0f)

        batch.shader = Terrarum.shaderBayer
        batch.shader.setUniformf("rcount", 64f)
        batch.shader.setUniformf("gcount", 64f)
        batch.shader.setUniformf("bcount", 64f) // de-banding



        val lightTex = lightmapFboB.colorBufferTexture // A or B? flipped in Y means you chose wrong buffer; use one that works correctly
        lightTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) // blocky feeling for A E S T H E T I C S

        blendMul()
        //blendNormal()

        batch.color = Color.WHITE
        val xrem = -(WorldCamera.x % TILE_SIZEF)
        val yrem = -(WorldCamera.y % TILE_SIZEF)
        batch.draw(lightTex,
                xrem,
                yrem,
                lightTex.width * Ingame.lightmapDownsample, lightTex.height * Ingame.lightmapDownsample
                //lightTex.width.toFloat(), lightTex.height.toFloat() // for debugging
        )



        //////////////////////
        // Draw other shits //
        //////////////////////


        batch.shader = null


        // move camera back to its former position
        // using custom code for camera; this is obscure and tricky
        camera.position.set(WorldCamera.gdxCamX, WorldCamera.gdxCamY, 0f) // make camara work
        camera.update()
        batch.projectionMatrix = camera.combined
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
                TerrarumAppLoader.COPYRIGHT_DATE_NAME,
                Lang["COPYRIGHT_GNU_GPL_3"]
        )

        COPYTING.forEachIndexed { index, s ->
            val textWidth = Terrarum.fontGame.getWidth(s)
            Terrarum.fontGame.draw(batch, s,
                    Terrarum.WIDTH - textWidth - 1f - 0.2f,
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

        BlocksDrawer.resize(Terrarum.WIDTH, Terrarum.HEIGHT)
        LightmapRendererNew.resize(Terrarum.WIDTH, Terrarum.HEIGHT)

        if (loadDone) {
            // resize UI by re-creating it (!!)
            uiMenu.resize(Terrarum.WIDTH, Terrarum.HEIGHT)
            uiMenu.setPosition(0, UITitleRemoConRoot.menubarOffY)
        }

        if (lightmapInitialised) {
            lightmapFboA.dispose()
            lightmapFboB.dispose()
        }
        lightmapFboA = FrameBuffer(
                lightFBOformat,
                LightmapRendererNew.lightBuffer.width * LightmapRendererNew.DRAW_TILE_SIZE.toInt(),
                LightmapRendererNew.lightBuffer.height * LightmapRendererNew.DRAW_TILE_SIZE.toInt(),
                false
        )
        lightmapFboB = FrameBuffer(
                lightFBOformat,
                LightmapRendererNew.lightBuffer.width * LightmapRendererNew.DRAW_TILE_SIZE.toInt(),
                LightmapRendererNew.lightBuffer.height * LightmapRendererNew.DRAW_TILE_SIZE.toInt(),
                false
        )
        lightmapInitialised = true // are you the first time?
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

        var blurWriteBuffer = lightmapFboA
        var blurReadBuffer = lightmapFboB


        lightmapFboA.inAction(null, null) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }
        lightmapFboB.inAction(null, null) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }


        if (mode == LightmapRendererNew.DRAW_FOR_RGB) {
            // initialise readBuffer with untreated lightmap
            blurReadBuffer.inAction(camera, batch) {
                batch.inUse {
                    blendNormal(batch)
                    batch.color = Color.WHITE
                    LightmapRendererNew.draw(batch, LightmapRendererNew.DRAW_FOR_RGB)
                }
            }
        }
        else {
            // initialise readBuffer with untreated lightmap
            blurReadBuffer.inAction(camera, batch) {
                batch.inUse {
                    blendNormal(batch)
                    batch.color = Color.WHITE
                    LightmapRendererNew.draw(batch, LightmapRendererNew.DRAW_FOR_ALPHA)
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


    class TitleScreenController(val screen: FuckingWorldRenderer) : InputAdapter() {
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