package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.dataclass.CircularArray
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.gameactors.ParticleBase
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera

/**
 * This will be rendered to a postprocessor FBO
 */
object IngameRenderer {

    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera

    private lateinit var lightmapFboA: FrameBuffer
    private lateinit var lightmapFboB: FrameBuffer
    private lateinit var fboRGB: FrameBuffer
    private lateinit var fboRGB_lightMixed: FrameBuffer
    private lateinit var fboA: FrameBuffer
    private lateinit var fboA_lightMixed: FrameBuffer

    // you must have lightMixed FBO; otherwise you'll be reading from unbaked FBO and it freaks out GPU

    private val shaderBlur = Terrarum.shaderBlur
    private val shaderSkyboxFill = Terrarum.shaderSkyboxFill
    private val shaderBlendGlow = Terrarum.shaderBlendGlow
    private val shaderRGBOnly = Terrarum.shaderRGBOnly
    private val shaderAtoGrey = Terrarum.shaderAtoGrey

    private val width = Terrarum.WIDTH
    private val height = Terrarum.HEIGHT
    private val widthf = width.toFloat()
    private val heightf = height.toFloat()

    private var initDone = false

    private var player: ActorWithBody? = null

    var uiListToDraw = ArrayList<UICanvas>()

    const val lightmapDownsample = 4f //2f: still has choppy look when the camera moves but unnoticeable when blurred

    private var debugMode = 0

    operator fun invoke(
            world: GameWorldExtension,
            actorsRenderBehind: List<ActorWithBody>? = null,
            actorsRenderMiddle: List<ActorWithBody>? = null,
            actorsRenderMidTop: List<ActorWithBody>? = null,
            actorsRenderFront : List<ActorWithBody>? = null,
            particlesContainer: CircularArray<ParticleBase>? = null,
            player: ActorWithBody? = null,
            uisToDraw: ArrayList<UICanvas>? = null
    ) {

        if (uisToDraw != null) {
            uiListToDraw = uisToDraw
        }

        init()

        BlocksDrawer.world = world
        LightmapRenderer.setWorld(world)
        FeaturesDrawer.world = world

        this.player = player


        LightmapRenderer.fireRecalculateEvent()

        prepLightmapRGBA()
        drawToRGB(actorsRenderBehind, actorsRenderMiddle, actorsRenderMidTop, actorsRenderFront, particlesContainer)
        drawToA(actorsRenderBehind, actorsRenderMiddle, actorsRenderMidTop, actorsRenderFront, particlesContainer)

        // clear main or whatever super-FBO being used
        //clearBuffer()
        Gdx.gl.glClearColor(.64f, .754f, .84f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        ///////////////////////////////////////////////////////////////////////

        // draw sky
        WeatherMixer.render(camera, world)

        ///////////////////////////////////////////////////////////////////////

        // use shader to mix RGB and A
        setCameraPosition(0f, 0f)

        val rgbTex = fboRGB_lightMixed.colorBufferTexture
        val aTex = fboA_lightMixed.colorBufferTexture

        // normal behaviour
        if (!KeyToggler.isOn(Input.Keys.F6) &&
            !KeyToggler.isOn(Input.Keys.F7)
        ) {
            debugMode = 0

            aTex.bind(1)
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

            batch.inUse {
                blendNormal(batch)
                batch.shader = shaderBlendGlow
                shaderBlendGlow.setUniformi("tex1", 1)
                batch.draw(rgbTex, 0f, 0f)
            }


            // definitely something is not blended correctly
        }
        // something about RGB
        else if (KeyToggler.isOn(Input.Keys.F6) &&
                 !KeyToggler.isOn(Input.Keys.F7)
        ) {
            debugMode = 1
            batch.inUse {
                blendNormal(batch)
                batch.shader = null
                batch.draw(rgbTex, 0f, 0f)


                // indicator
                batch.color = Color.RED
                batch.fillRect(0f, 0f, 6f, 10f)
                batch.color = Color.LIME
                batch.fillRect(6f, 0f, 6f, 10f)
                batch.color = Color.ROYAL
                batch.fillRect(12f, 0f, 6f, 10f)
                batch.color = Color.WHITE
            }

            // works as intended
        }
        // something about A
        else if (!KeyToggler.isOn(Input.Keys.F6) &&
                 KeyToggler.isOn(Input.Keys.F7)
        ) {
            debugMode = 2
            batch.inUse {
                blendNormal(batch)
                batch.shader = null
                batch.draw(aTex, 0f, 0f)


                // indicator
                batch.color = Color.WHITE
                batch.fillRect(18f, 0f, 18f, 10f)
            }

            // works as intended
        }
        else {
            if (debugMode == 1) {
                KeyToggler.forceSet(Input.Keys.F6, false)
                KeyToggler.forceSet(Input.Keys.F7, true)
            }
            else if (debugMode == 2) {
                KeyToggler.forceSet(Input.Keys.F6, true)
                KeyToggler.forceSet(Input.Keys.F7, false)
            }
            else {
                KeyToggler.forceSet(Input.Keys.F6, false)
                KeyToggler.forceSet(Input.Keys.F7, false)
            }

            // works as intended
        }


        blendNormal(batch)


        ///////////////////////////////////////////////////////////////////////

        // draw UI
        setCameraPosition(0f, 0f)

        batch.inUse {
            batch.shader = null
            batch.color = Color.WHITE

            uiListToDraw.forEach {
                it.render(batch, camera)
            }
        }

        // works but some UI elements have wrong transparency
        blendNormal(batch)
    }


    private fun prepLightmapRGBA() {
        lightmapFboA.inAction(null, null) {
            clearBuffer()
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }
        lightmapFboB.inAction(null, null) {
            clearBuffer()
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }

        processBlur(lightmapFboA, lightmapFboB)
    }

    private fun drawToRGB(
            actorsRenderBehind: List<ActorWithBody>?,
            actorsRenderMiddle: List<ActorWithBody>?,
            actorsRenderMidTop: List<ActorWithBody>?,
            actorsRenderFront : List<ActorWithBody>?,
            particlesContainer: CircularArray<ParticleBase>?
    ) {
        fboRGB.inAction(null, null) { clearBuffer() }
        fboRGB_lightMixed.inAction(null, null) { clearBuffer() }

        fboRGB.inAction(camera, batch) {

            batch.inUse {
                batch.shader = null
                batch.color = Color.WHITE
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.renderWall(batch.projectionMatrix)

            batch.inUse {
                moveCameraToWorldCoord()
                actorsRenderBehind?.forEach { it.drawBody(batch) }
                particlesContainer?.forEach { it.drawBody(batch) }
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.renderTerrain(batch.projectionMatrix)

            batch.inUse {
                /////////////////
                // draw actors //
                /////////////////
                moveCameraToWorldCoord()
                actorsRenderMiddle?.forEach { it.drawBody(batch) }
                actorsRenderMidTop?.forEach { it.drawBody(batch) }
                player?.drawBody(batch)
                actorsRenderFront?.forEach { it.drawBody(batch) }
                // --> Change of blend mode <-- introduced by children of ActorWithBody //
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.renderFront(batch.projectionMatrix, false) // blue coloured filter of water, etc.

            batch.inUse {
                FeaturesDrawer.drawEnvOverlay(batch)
            }
        }


        fboRGB_lightMixed.inAction(camera, batch) {

            setCameraPosition(0f, 0f)
            val xrem = -(WorldCamera.x.toFloat() fmod TILE_SIZEF)
            val yrem = -(WorldCamera.y.toFloat() fmod TILE_SIZEF)

            batch.inUse {
                // draw world
                batch.draw(fboRGB.colorBufferTexture, 0f, 0f)
                batch.flush()

                // multiply light on top of it
                val lightTex = lightmapFboB.colorBufferTexture
                lightTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

                if (KeyToggler.isOn(Input.Keys.F8))
                    blendNormal(batch)
                else
                    blendMul(batch)

                batch.shader = shaderRGBOnly
                batch.draw(lightTex,
                        xrem, yrem,
                        lightTex.width * lightmapDownsample,
                        lightTex.height * lightmapDownsample
                )
            }


            // NOTE TO SELF: this works.
        }


        blendNormal(batch)
    }

    private fun drawToA(
            actorsRenderBehind: List<ActorWithBody>?,
            actorsRenderMiddle: List<ActorWithBody>?,
            actorsRenderMidTop: List<ActorWithBody>?,
            actorsRenderFront : List<ActorWithBody>?,
            particlesContainer: CircularArray<ParticleBase>?
    ) {
        fboA.inAction(null, null) {
            clearBuffer()
            // paint black
            Gdx.gl.glClearColor(0f,0f,0f,1f) // solid black: so that unused area will be also black
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }
        fboA_lightMixed.inAction(null, null) { clearBuffer() }

        fboA.inAction(camera, batch) {

            batch.inUse {
                batch.shader = null
                batch.color = Color.WHITE
            }

            setCameraPosition(0f, 0f)
            // BlocksDrawer.renderWhateverGlow_WALL

            batch.inUse {
                moveCameraToWorldCoord()
                actorsRenderBehind?.forEach { it.drawGlow(batch) }
                particlesContainer?.forEach { it.drawGlow(batch) }
            }

            setCameraPosition(0f, 0f)
            // BlocksDrawer.renderWhateverGlow_TERRAIN

            batch.inUse {
                /////////////////
                // draw actors //
                /////////////////
                moveCameraToWorldCoord()
                actorsRenderMiddle?.forEach { it.drawGlow(batch) }
                actorsRenderMidTop?.forEach { it.drawGlow(batch) }
                player?.drawGlow(batch)
                actorsRenderFront?.forEach { it.drawGlow(batch) }
                // --> Change of blend mode <-- introduced by children of ActorWithBody //
            }
        }


        fboA_lightMixed.inAction(camera, batch) {

            setCameraPosition(0f, 0f)
            val xrem = -(WorldCamera.x.toFloat() fmod TILE_SIZEF)
            val yrem = -(WorldCamera.y.toFloat() fmod TILE_SIZEF)

            batch.inUse {
                // draw world
                batch.draw(fboA.colorBufferTexture, 0f, 0f)
                batch.flush()

                // multiply light on top of it
                val lightTex = lightmapFboB.colorBufferTexture
                lightTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

                if (KeyToggler.isOn(Input.Keys.F8))
                    blendNormal(batch)
                else
                    blendMul(batch)

                batch.shader = shaderAtoGrey
                batch.draw(lightTex,
                        xrem, yrem,
                        lightTex.width * lightmapDownsample,
                        lightTex.height * lightmapDownsample
                )
            }


            // NOTE TO SELF: this works.
        }


        blendNormal(batch)
    }


    private fun init() {
        if (!initDone) {
            batch = SpriteBatch()
            camera = OrthographicCamera(widthf, heightf)

            camera.setToOrtho(true, widthf, heightf)
            camera.update()
            Gdx.gl20.glViewport(0, 0, width, height)

            resize(width, height)

            initDone = true
        }
    }

    private fun clearBuffer() {
        Gdx.gl.glClearColor(0f,0f,0f,0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun moveCameraToWorldCoord() {
        // using custom code for camera; this is obscure and tricky
        camera.position.set(WorldCamera.gdxCamX, WorldCamera.gdxCamY, 0f) // make camara work
        camera.update()
        batch.projectionMatrix = camera.combined
    }

    /**
     * Camera will be moved so that (newX, newY) would be sit on the top-left edge.
     */
    private fun setCameraPosition(newX: Float, newY: Float) {
        camera.position.set((-newX + Terrarum.HALFW).round(), (-newY + Terrarum.HALFH).round(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

    fun processBlur(lightmapFboA: FrameBuffer, lightmapFboB: FrameBuffer) {
        val blurIterations = 5 // ideally, 4 * radius; must be even/odd number -- odd/even number will flip the image
        val blurRadius = 4f / lightmapDownsample // (5, 4f); using low numbers for pixel-y aesthetics

        var blurWriteBuffer = lightmapFboA
        var blurReadBuffer = lightmapFboB


        // buffers must be cleared beforehand


        // initialise readBuffer with untreated lightmap
        blurReadBuffer.inAction(camera, batch) {
            batch.inUse {
                blendDisable(batch)
                batch.color = Color.WHITE
                LightmapRenderer.draw(batch)
            }
        }




        for (i in 0 until blurIterations) {
            blurWriteBuffer.inAction(camera, batch) {

                batch.inUse {
                    val texture = blurReadBuffer.colorBufferTexture

                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)


                    batch.shader = shaderBlur
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



        blendNormal(batch)
    }

    private var init = false

    fun resize(width: Int, height: Int) {
        if (!init) {
            init = true
        }
        else {
            fboRGB.dispose()
            fboRGB_lightMixed.dispose()
            fboA.dispose()
            fboA_lightMixed.dispose()
            lightmapFboA.dispose()
            lightmapFboB.dispose()
        }

        fboRGB = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
        fboRGB_lightMixed = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
        fboA = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
        fboA_lightMixed = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
        lightmapFboA = FrameBuffer(
                Pixmap.Format.RGBA8888,
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                false
        )
        lightmapFboB = FrameBuffer(
                Pixmap.Format.RGBA8888,
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                false
        )

        BlocksDrawer.resize(width, height)
        LightmapRenderer.resize(width, height)


        //LightmapRenderer.fireRecalculateEvent()
    }

    private val TILE_SIZEF = FeaturesDrawer.TILE_SIZE.toFloat()

    fun dispose() {
        fboRGB.dispose()
        fboA.dispose()
        fboRGB_lightMixed.dispose()
        fboA_lightMixed.dispose()
        lightmapFboA.dispose()
        lightmapFboB.dispose()

        LightmapRenderer.dispose()
    }
}