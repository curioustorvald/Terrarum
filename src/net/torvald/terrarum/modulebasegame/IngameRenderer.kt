package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.App.measureDebugTime
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameparticles.ParticleBase
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.util.CircularArray
import kotlin.system.exitProcess

/**
 * This will be rendered to a postprocessor FBO.
 *
 * For the entire render path, see AppLoader.
 *
 * NOTE: config "fx_dither" only controls the skybox (which is capable of having more than 256 colours
 * thanks to the hardware linear intp.) because this dithering shader is somewhat heavy.
 *
 * Semitransparency is rendered using dithering, so it is good idea to avoid them.
 * If you must add semitransparency to the tile, they must have alpha NOT premultiplied.
 * Actors' transparency (and not an UI) still uses its own lightweight ditherrer
 */
object IngameRenderer : Disposable {
    /** for non-private use, use with care! */
    lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera

    private lateinit var blurWriteQuad: Mesh

    private lateinit var lightmapFboA: FrameBuffer
    private lateinit var lightmapFboB: FrameBuffer
    private lateinit var fboRGB: FrameBuffer
    private lateinit var fboRGB_lightMixed: FrameBuffer
    private lateinit var fboA: FrameBuffer
    private lateinit var fboA_lightMixed: FrameBuffer
    private lateinit var fboMixedOut: FrameBuffer
    private lateinit var rgbTex: TextureRegion
    private lateinit var aTex: TextureRegion
    private lateinit var mixedOutTex: TextureRegion
    private lateinit var lightTex: TextureRegion
    private lateinit var blurTex: TextureRegion

    // you must have lightMixed FBO; otherwise you'll be reading from unbaked FBO and it freaks out GPU

    inline fun isDither() = App.getConfigBoolean("fx_dither")

    private val rng = HQRNG()

    val shaderBlur: ShaderProgram
        get() = if (isDither()) shaderBlurDither else shaderBlurRaw
    val shaderRGBOnly: ShaderProgram
        get() = if (isDither()) shaderRGBOnlyDither else shaderRGBOnlyRaw
    val shaderAtoGrey: ShaderProgram
        get() = if (isDither()) shaderAtoGreyDither else shaderAtoGreyRaw

    val shaderBlurDither: ShaderProgram
    val shaderBlurRaw: ShaderProgram
    val shaderRGBOnlyDither: ShaderProgram
    val shaderRGBOnlyRaw: ShaderProgram
    val shaderAtoGreyDither: ShaderProgram
    val shaderAtoGreyRaw: ShaderProgram

    val shaderBayer: ShaderProgram
    val shaderPassthru = SpriteBatch.createDefaultShader()

    val shaderBlendGlow: ShaderProgram
    val shaderAlphaDither: ShaderProgram

    private val WIDTH = App.scr.width
    private val HEIGHT = App.scr.height
    private val WIDTHF = WIDTH.toFloat()
    private val HEIGHTF = HEIGHT.toFloat()

    private var initDone = false

    private var player: ActorWithBody? = null

    const val lightmapDownsample = 1f//4f //2f: still has choppy look when the camera moves but unnoticeable when blurred

    private var debugMode = 0

    var renderingActorsCount = 0
        private set
    var renderingUIsCount = 0
        private set
    //var renderingParticleCount = 0
    //    private set

    var world: GameWorld = GameWorld.makeNullWorld()
        private set // the grammar "IngameRenderer.world = gameWorld" seemes mundane and this function needs special care!

    private var newWorldLoadedLatch = false


    // these codes will run regardless of the invocation of the "initialise()" function
    // the "initialise()" function will also be called
    init {
        shaderBlurDither = App.loadShaderFromFile("assets/shaders/blur.vert", "assets/shaders/blur_dither.frag")
        shaderRGBOnlyDither = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/4096_bayer_rgb1.frag")
        shaderAtoGreyDither = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/4096_bayer_aaa1.frag")

        shaderBlurRaw = App.loadShaderFromFile("assets/shaders/blur.vert", "assets/shaders/blur.frag")
        shaderRGBOnlyRaw = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/rgbonly.frag")
        shaderAtoGreyRaw = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/aonly.frag")


        shaderBayer = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/4096_bayer.frag") // always load the shader regardless of config because the config may cange
        shaderAlphaDither = App.loadShaderFromFile("assets/shaders/4096.vert", "assets/shaders/alphadither.frag")
        shaderBlendGlow = App.loadShaderFromFile("assets/shaders/blendGlow.vert", "assets/shaders/blendGlow.frag")


        if (!shaderBlendGlow.isCompiled) {
            Gdx.app.log("shaderBlendGlow", shaderBlendGlow.log)
            exitProcess(1)
        }


        if (isDither()) {
            if (!shaderBayer.isCompiled) {
                Gdx.app.log("shaderBayer", shaderBayer.log)
                exitProcess(1)
            }
        }

        initialise()
    }

    /** Whether or not "initialise()" method had been called */
    private var initialisedExternally = false

    /** To make it more convenient to be initialised by the Java code, and for the times when the order of the call
     * actually matter */
    @JvmStatic fun initialise() {
        if (!initialisedExternally) {
            App.disposables.add(this)

            // also initialise these sinigletons
            BlocksDrawer
            LightmapRenderer


            initialisedExternally = true
        }
    }

    /**
     * Your game/a scene that renders the world must call this method at least once!
     *
     * For example:
     * - When the main scene that renders the world is first created
     * - When the game make transition to the new world (advancing to the next level/entering or exiting the room)
     */
    fun setRenderedWorld(world: GameWorld) {
            try {
                if (this.world != world) {
//                    printdbg(this, "World change detected -- " +
//                                   "old world: ${this.world.hashCode()}, " +
//                                   "new world: ${world.hashCode()}")

                    // change worlds from internal methods
                    this.world = world
                    LightmapRenderer.internalSetWorld(world)
                    BlocksDrawer.world = world
                    FeaturesDrawer.world = world

                    newWorldLoadedLatch = true
                }
            }
            catch (e: UninitializedPropertyAccessException) {
                // new init, do nothing
                this.world = world
            }
        }

    private var oldCamX = 0

    operator fun invoke(
            gamePaused: Boolean,
            zoom: Float = 1f,
            actorsRenderBehind : List<ActorWithBody>? = null,
            actorsRenderMiddle : List<ActorWithBody>? = null,
            actorsRenderMidTop : List<ActorWithBody>? = null,
            actorsRenderFront  : List<ActorWithBody>? = null,
            actorsRenderOverlay: List<ActorWithBody>? = null,
            particlesContainer : CircularArray<ParticleBase>? = null,
            player: ActorWithBody? = null,
            uiContainer: UIContainer? = null
    ) {
        renderingActorsCount = (actorsRenderBehind?.size ?: 0) +
                               (actorsRenderMiddle?.size ?: 0) +
                               (actorsRenderMidTop?.size ?: 0) +
                               (actorsRenderFront?.size ?: 0) +
                               (actorsRenderOverlay?.size ?: 0)
        //renderingParticleCount = particlesContainer?.size ?: 0
        //renderingParticleCount = (particlesContainer?.buffer?.map { (!it.flagDespawn).toInt() } ?: listOf(0)).sum()
        renderingUIsCount = uiContainer?.countVisible() ?: 0

        invokeInit()

        batch.color = Color.WHITE


        this.player = player


        if (!gamePaused || newWorldLoadedLatch) {
            measureDebugTime("Renderer.ApparentLightRun") {
                // recalculate for even frames, or if the sign of the cam-x changed
                if (App.GLOBAL_RENDER_TIMER % 3 == 0 || Math.abs(WorldCamera.x - oldCamX) >= world.width * 0.85f * TILE_SIZEF || newWorldLoadedLatch) {
                    LightmapRenderer.fireRecalculateEvent(actorsRenderBehind, actorsRenderFront, actorsRenderMidTop, actorsRenderMiddle, actorsRenderOverlay)
                }
                oldCamX = WorldCamera.x
            }

            prepLightmapRGBA()
            BlocksDrawer.renderData()
            drawToRGB(actorsRenderBehind, actorsRenderMiddle, actorsRenderMidTop, actorsRenderFront, particlesContainer)
            drawToA(actorsRenderBehind, actorsRenderMiddle, actorsRenderMidTop, actorsRenderFront, particlesContainer)
            drawOverlayActors(actorsRenderOverlay)
        }
        // clear main or whatever super-FBO being used
        //clearBuffer()
        gdxClearAndSetBlend(.64f, .754f, .84f, 0f)

        ///////////////////////////////////////////////////////////////////////

        // use shader to mix RGB and A
        setCameraPosition(0f, 0f)

        rgbTex.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        aTex.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        fboMixedOut.inAction(camera, batch) {
            gdxClearAndSetBlend(0f, 0f, 0f, 0f)

            // draw sky
            WeatherMixer.render(camera, batch, world)


            // normal behaviour
            if (!KeyToggler.isOn(Input.Keys.F6) &&
                !KeyToggler.isOn(Input.Keys.F7)
            ) {
                debugMode = 0

                aTex.texture.bind(1)
                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it


                batch.inUse {
                    blendNormal(batch)
                    batch.shader = shaderBlendGlow
                    shaderBlendGlow.setUniformi("tex1", 1)
                    batch.draw(rgbTex,
                            -0.5f * rgbTex.regionWidth * zoom + 0.5f * rgbTex.regionWidth,
                            -0.5f * rgbTex.regionHeight * zoom + 0.5f * rgbTex.regionHeight,
                            rgbTex.regionWidth * zoom,
                            rgbTex.regionHeight * zoom
                    )
                }

            }
            // something about RGB
            else if (KeyToggler.isOn(Input.Keys.F6) &&
                     !KeyToggler.isOn(Input.Keys.F7)
            ) {
                debugMode = 1
                batch.inUse {
                    blendNormal(batch)
                    batch.shader = null
                    batch.draw(rgbTex,
                            -0.5f * rgbTex.regionWidth * zoom + 0.5f * rgbTex.regionWidth,
                            -0.5f * rgbTex.regionHeight * zoom + 0.5f * rgbTex.regionHeight,
                            rgbTex.regionWidth * zoom,
                            rgbTex.regionHeight * zoom
                    )

                    // indicator
                    batch.color = Color.RED
                    Toolkit.fillArea(batch, 0, 0, 6, 10)
                    batch.color = Color.LIME
                    Toolkit.fillArea(batch, 6, 0, 6, 10)
                    batch.color = Color.ROYAL
                    Toolkit.fillArea(batch, 12, 0, 6, 10)
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
                    batch.draw(aTex,
                            -0.5f * aTex.regionWidth * zoom + 0.5f * aTex.regionWidth,
                            -0.5f * aTex.regionHeight * zoom + 0.5f * aTex.regionHeight,
                            aTex.regionWidth * zoom,
                            aTex.regionHeight * zoom
                    )

                    // indicator
                    batch.color = Color.WHITE
                    Toolkit.fillArea(batch, 18, 0, 18, 10)
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
        }

        blendNormal(batch)

        batch.inUse {
            // it's no use applying dithering here: colours are no longer "floats" once they're written to the FBO
            // proof: disable dithering on skybox and enable dither here -- banding is still visible
            // it would work if GDX supported HDR, or GL_RGBA32F as a texture format, but alas.
            // but mixedOutTex is still needed for the screen capturing

            //batch.shader = if (App.getConfigBoolean("fx_dither")) IngameRenderer.shaderBayer else null
            batch.shader = null
            batch.draw(mixedOutTex, 0f, 0f)
        }


        ///////////////////////////////////////////////////////////////////////

        if (screencapRequested) {
            screencapRequested = false
            try {
                screencapExportCallback(fboMixedOut)
            }
            catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        ///////////////////////////////////////////////////////////////////////

        // draw UI
        setCameraPosition(0f, 0f)

        batch.inUse {
            batch.shader = null
            batch.color = Color.WHITE

            uiContainer?.forEach {
                it?.render(batch, camera)
            }
        }

        // works but some UI elements have wrong transparency -> should be fixed with Terrarum.gdxCleanAndSetBlend -- Torvald 2019-01-12
        blendNormal(batch)
        batch.color = Color.WHITE


        if (newWorldLoadedLatch) newWorldLoadedLatch = false
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

    @Volatile internal var screencapRequested = false
    @Volatile internal var fboRGBexportedLatch = false
    @Volatile internal var screencapExportCallback: (FrameBuffer) -> Unit = {}
    @Volatile internal lateinit var fboRGBexport: Pixmap

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
                batch.shader = shaderAlphaDither
                batch.color = Color.WHITE
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawWall(batch.projectionMatrix, false)

            batch.inUse {
                moveCameraToWorldCoord()
                actorsRenderBehind?.forEach { it.drawBody(batch) }
            }
            batch.shader = shaderAlphaDither
            batch.inUse {
                particlesContainer?.forEach { it.drawBody(batch) }
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawTerrain(batch.projectionMatrix, false)

            batch.shader = shaderAlphaDither
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
            BlocksDrawer.drawFront(batch.projectionMatrix) // blue coloured filter of water, etc.

            batch.inUse {
                FeaturesDrawer.drawEnvOverlay(batch)
            }
        }

        fboRGB_lightMixed.inAction(camera, batch) {

            setCameraPosition(0f, 0f)
            val (xrem, yrem) = worldCamToRenderPos()

            gdxSetBlend()

            App.getCurrentDitherTex().bind(1)
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

            batch.inUse {

                blendNormal(batch)

                // draw world
                batch.draw(fboRGB.colorBufferTexture, 0f, 0f)
                batch.flush()

                // multiply light on top of it
                lightTex.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

                if (KeyToggler.isOn(Input.Keys.F8))
                    blendNormal(batch)
                else
                    blendMul(batch)

                batch.shader = shaderRGBOnly
                batch.shader.setUniformi("rnd", rng.nextInt(8192), rng.nextInt(8192))
                batch.shader.setUniformi("u_pattern", 1)
                batch.draw(lightTex,
                        xrem, yrem,
                        lightTex.regionWidth * lightmapDownsample,
                        lightTex.regionHeight * lightmapDownsample
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
            gdxClearAndSetBlend(0f,0f,0f,1f) // solid black: so that unused area will be also black
        }
        fboA_lightMixed.inAction(null, null) { clearBuffer() }

        fboA.inAction(camera, batch) {

            batch.inUse {
                batch.shader = shaderAlphaDither
                batch.color = Color.WHITE
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawWall(batch.projectionMatrix, true)

            batch.inUse {
                moveCameraToWorldCoord()
                actorsRenderBehind?.forEach { it.drawGlow(batch) }
                particlesContainer?.forEach { it.drawGlow(batch) }
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawTerrain(batch.projectionMatrix, true)

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
            val (xrem, yrem) = worldCamToRenderPos()

            App.getCurrentDitherTex().bind(1)
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

            batch.inUse {
                // draw world
                batch.draw(fboA.colorBufferTexture, 0f, 0f)
                batch.flush()

                // multiply light on top of it
                lightTex.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

                if (KeyToggler.isOn(Input.Keys.F8))
                    blendNormal(batch)
                else
                    blendMul(batch)

                batch.shader = shaderAtoGrey
                batch.shader.setUniformi("rnd", rng.nextInt(8192), rng.nextInt(8192))
                batch.shader.setUniformi("u_pattern", 1)
                batch.draw(lightTex,
                        xrem, yrem,
                        lightTex.regionWidth * lightmapDownsample,
                        lightTex.regionHeight * lightmapDownsample
                )
            }


            // NOTE TO SELF: this works.
        }


        blendNormal(batch)
    }

    private fun drawOverlayActors(actors: List<ActorWithBody>?) {
        fboRGB_lightMixed.inAction(camera, batch) {

            batch.inUse {
                batch.shader = shaderAlphaDither
                batch.color = Color.WHITE
            }

            setCameraPosition(0f, 0f)
            // BlocksDrawer.renderWhateverGlow_WALL

            batch.inUse {
                moveCameraToWorldCoord()
                actors?.forEach { it.drawBody(batch) }
            }

            setCameraPosition(0f, 0f)
            // BlocksDrawer.renderWhateverGlow_TERRAIN
        }
    }


    private fun invokeInit() {
        if (!initDone) {
            batch = SpriteBatch()
            camera = OrthographicCamera(WIDTHF, HEIGHTF)

            camera.setToOrtho(true, WIDTHF, HEIGHTF)
            camera.update()
            Gdx.gl20.glViewport(0, 0, WIDTH, HEIGHT)

            resize(WIDTH, HEIGHT)

            initDone = true
        }
    }

    private fun clearBuffer() {
        gdxClearAndSetBlend(0f,0f,0f,0f)
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
        camera.position.set((-newX + App.scr.halfw).round(), (-newY + App.scr.halfh).round(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

    fun processBlur(lightmapFboA: FrameBuffer, lightmapFboB: FrameBuffer) {
        var blurWriteBuffer = lightmapFboA
        var blurReadBuffer = lightmapFboB


        // buffers must be cleared beforehand


        // initialise readBuffer with untreated lightmap
        blurReadBuffer.inAction(camera, batch) {
            val texture = LightmapRenderer.draw()
            texture.bind(0)

            shaderPassthru.bind()
            shaderPassthru.setUniformMatrix("u_projTrans", camera.combined)
            shaderPassthru.setUniformi("u_texture", 0)
            blurWriteQuad.render(shaderPassthru, GL20.GL_TRIANGLES)
        }

        // do blurring
        for (i in 0 until 4) {
            val blurRadius = (4f / lightmapDownsample) * (1 shl i.ushr(1))

            blurWriteBuffer.inAction(camera, batch) {


                blurTex.texture = blurReadBuffer.colorBufferTexture
                blurTex.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                blurTex.texture.bind(0)
                App.getCurrentDitherTex().bind(1) // order is important!

                shaderBlur.bind()
                shaderBlur.setUniformMatrix("u_projTrans", camera.combined)
                shaderBlur.setUniformi("rnd", rng.nextInt(8192), rng.nextInt(8192))
                shaderBlur.setUniformi("u_pattern", 1)
                shaderBlur.setUniformi("u_texture", 0)
                shaderBlur.setUniformf("iResolution",
                        blurWriteBuffer.width.toFloat(), blurWriteBuffer.height.toFloat())
                shaderBlur.setUniformf("flip", 1f)
                if (i % 2 == 0)
                    shaderBlur.setUniformf("direction", blurRadius, 0f)
                else
                    shaderBlur.setUniformf("direction", 0f, blurRadius)
                blurWriteQuad.render(shaderBlur, GL20.GL_TRIANGLES)


                // swap
                val t = blurWriteBuffer
                blurWriteBuffer = blurReadBuffer
                blurReadBuffer = t
            }
        }



        blendNormal(batch)
    }

    private var init = false

    fun resize(width: Int, height: Int) {
        if (!init) {
            blurWriteQuad = Mesh(
                    true, 4, 6,
                    VertexAttribute.Position(),
                    VertexAttribute.ColorUnpacked(),
                    VertexAttribute.TexCoords(0)
            )

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

        fboRGB = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
        fboRGB_lightMixed = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
        fboA = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
        fboA_lightMixed = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
        fboMixedOut = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
        lightmapFboA = FrameBuffer(
                Pixmap.Format.RGBA8888,
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                true
        )
        lightmapFboB = FrameBuffer(
                Pixmap.Format.RGBA8888,
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                true
        )
        rgbTex = TextureRegion(fboRGB_lightMixed.colorBufferTexture)
        aTex = TextureRegion(fboA_lightMixed.colorBufferTexture)
        lightTex = TextureRegion(lightmapFboB.colorBufferTexture)
        blurTex = TextureRegion()
        mixedOutTex = TextureRegion(fboMixedOut.colorBufferTexture)

        BlocksDrawer.resize(width, height)
        LightmapRenderer.resize(width, height)


        blurWriteQuad.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                lightmapFboA.width.toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                lightmapFboA.width.toFloat(),lightmapFboA.height.toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f,lightmapFboA.height.toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
        blurWriteQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))

    }

    override fun dispose() {
        fboRGB.dispose()
        fboA.dispose()
        fboRGB_lightMixed.dispose()
        fboA_lightMixed.dispose()
        fboMixedOut.dispose()
        lightmapFboA.dispose()
        lightmapFboB.dispose()

        LightmapRenderer.dispose()
        BlocksDrawer.dispose()
        WeatherMixer.dispose()

        batch.dispose()


        shaderBlurDither.dispose()
        shaderBlurRaw.dispose()
        shaderRGBOnlyDither.dispose()
        shaderRGBOnlyRaw.dispose()
        shaderAtoGreyDither.dispose()
        shaderAtoGreyRaw.dispose()

        shaderBayer.dispose()
        shaderBlendGlow.dispose()
        shaderPassthru.dispose()
        shaderAlphaDither.dispose()

        try {
            fboRGBexport.dispose()
        }
        catch (e: UninitializedPropertyAccessException) {}
        catch (e: Throwable) { e.printStackTrace(System.out) }
    }

    private fun worldCamToRenderPos(): Pair<Float, Float> {
        // for some reason it does not like integer. No, really; it breaks (jitter when you move) when you try to "fix" that.
        val xoff = (WorldCamera.x / TILE_SIZE) - LightmapRenderer.camX
        val yoff = (WorldCamera.y / TILE_SIZE) - LightmapRenderer.camY
        val xrem = -(WorldCamera.x.toFloat() fmod TILE_SIZEF) - (xoff * TILE_SIZEF)
        val yrem = -(WorldCamera.y.toFloat() fmod TILE_SIZEF) - (yoff * TILE_SIZEF)

        return (xrem - LightmapRenderer.LIGHTMAP_OVERRENDER * TILE_SIZEF) to (yrem - LightmapRenderer.LIGHTMAP_OVERRENDER * TILE_SIZEF)
    }
}