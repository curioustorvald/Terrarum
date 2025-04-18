package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.Float16FrameBuffer
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.ActorWithBody.Companion.METER
import net.torvald.terrarum.gameactors.ActorWithBody.Companion.PHYS_EPSILON_DIST
import net.torvald.terrarum.gameactors.ActorWithBody.Companion.SI_TO_GAME_ACC
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.gameparticles.ParticleBase
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.modulebasegame.gameitems.ItemThrowable
import net.torvald.terrarum.modulebasegame.gameitems.getThrowPosAndVector
import net.torvald.terrarum.ui.BlurMgr
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.util.CircularArray
import org.dyn4j.geometry.Vector2
import kotlin.math.min
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
    lateinit var batch: FlippingSpriteBatch
    private lateinit var camera: OrthographicCamera

    private lateinit var blurWriteQuad: Mesh
    private lateinit var blurWriteQuad2: Mesh
//    private lateinit var blurWriteQuad4: Mesh

    private lateinit var lightmapFbo: Float16FrameBuffer
    private lateinit var fboRGB: Float16FrameBuffer
    private lateinit var fboRGB_lightMixed0: Float16FrameBuffer
    private lateinit var fboRGB_lightMixed: Float16FrameBuffer
    private lateinit var fboA: Float16FrameBuffer
    private lateinit var fboA_lightMixed: Float16FrameBuffer
    private lateinit var fboEmissive: Float16FrameBuffer
    private lateinit var fboMixedOut: Float16FrameBuffer

    private lateinit var fboRGBactorsBehind: Float16FrameBuffer // for small shadow eff; A channel is for glow effects so they don't get shadow effects
    private lateinit var fboRGBactorsMiddle: Float16FrameBuffer // for large shadow eff; A channel is for glow effects so they don't get shadow effects
    private lateinit var fboRGBterrain: Float16FrameBuffer // for large shadow eff; A channel is for glow effects so they don't get shadow effects

    private lateinit var fboRGBactorsBehindShadow: Float16FrameBuffer // for tiny shadow eff; A channel is for glow effects so they don't get shadow effects
    private lateinit var fboRGBactorsBehindShadow2: Float16FrameBuffer // for small shadow eff; A channel is for glow effects so they don't get shadow effects
    private lateinit var fboRGBactorsMiddleShadow: Float16FrameBuffer // for large shadow eff; A channel is for glow effects so they don't get shadow effects
    private lateinit var fboRGBterrainShadow: Float16FrameBuffer // for large shadow eff; A channel is for glow effects so they don't get shadow effects

    private lateinit var fboRGBwall: Float16FrameBuffer // for masking away the shadows

    private lateinit var rgbTex: TextureRegion
    private lateinit var aTex: TextureRegion
    private lateinit var mixedOutTex: TextureRegion
    private lateinit var lightTex: TextureRegion
    private lateinit var blurTex: TextureRegion

    private lateinit var fboBlurHalf: Float16FrameBuffer
//    private lateinit var fboBlurQuarter: Float16FrameBuffer

    // you must have lightMixed FBO; otherwise you'll be reading from unbaked FBO and it freaks out GPU

//    inline fun isDither() = App.getConfigBoolean("fx_dither")

    private val rng = HQRNG()


//    val shaderBlurDither: ShaderProgram
//    val shaderRGBOnlyDither: ShaderProgram
//    val shaderAtoGreyDither: ShaderProgram
    val shaderBlur: ShaderProgram
    val shaderRGBOnly: ShaderProgram
    val shaderAtoGrey: ShaderProgram

    val shaderKawaseDown: ShaderProgram
    val shaderKawaseUp: ShaderProgram

    val shaderBlendGlow: ShaderProgram
    val shaderBlendGlowTex1Flip: ShaderProgram
    val shaderForActors: ShaderProgram
    val shaderShadowShallow: ShaderProgram
    val shaderShadowDeep: ShaderProgram
    val shaderDemultiply: ShaderProgram

    val shaderBayerAlpha: ShaderProgram

    val shaderVibrancy: ShaderProgram

    private val WIDTH = App.scr.width
    private val HEIGHT = App.scr.height
    private val WIDTHF = WIDTH.toFloat()
    private val HEIGHTF = HEIGHT.toFloat()

    private var initDone = false

    private var player: ActorWithBody? = null

    /** lower value = greater lozenge artefact from linear intp */
    const val lightmapDownsample = 2f // still has choppy look when the camera moves but unnoticeable when blurred

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
        shaderBlur = ShaderMgr["irBlur"]
        shaderRGBOnly = ShaderMgr["irRGBOnly"]
        shaderAtoGrey = ShaderMgr["irAtoGrey"]


        shaderForActors = ShaderMgr["irForActors"]
        shaderShadowShallow = ShaderMgr["irShadowShallow"]
        shaderShadowDeep = ShaderMgr["irShadowDeep"]
        shaderBlendGlow = ShaderMgr["irBlendGlow"]
        shaderBlendGlowTex1Flip = ShaderMgr["irBlendGlowTex1Flip"]
        shaderDemultiply = ShaderMgr["irDemultiply"]

        shaderBayerAlpha = ShaderMgr["irBayerAlpha"]

        shaderKawaseDown = ShaderMgr["irKawaseDown"]
        shaderKawaseUp = ShaderMgr["irKawaseUp"]

        shaderVibrancy = ShaderMgr["irVibrancy"]

        if (!shaderBlendGlow.isCompiled) {
            Gdx.app.log("shaderBlendGlow", shaderBlendGlow.log)
            exitProcess(1)
        }

        if (!shaderKawaseDown.isCompiled) {
            Gdx.app.log("shaderKawaseDown", shaderKawaseDown.log)
            exitProcess(1)
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
//        printdbg(this, "Set new RenderedWorld (UUID=${world.worldIndex}) at time ${System.currentTimeMillis()} (disposed: ${world.disposed}), called by:")
//        printStackTrace(this)

        var successful = false

        try {

            // change worlds from internal methods
            this.world = world
            LightmapRenderer.internalSetWorld(world)
            BlocksDrawer.world = world
            FeaturesDrawer.world = world

            if (this.world != world) {
//                    printdbg(this, "World change detected -- " +
//                                   "old world: ${this.world.hashCode()}, " +
//                                   "new world: ${world.hashCode()}")
                newWorldLoadedLatch = true
            }

            successful = true
        }
        catch (e: Throwable) {
            e.printStackTrace()
            // new init, do nothing
        }
        finally {
            if (successful)
                TerrarumPostProcessor.debugUI.world = world
            else
                TerrarumPostProcessor.debugUI.world = null
        }
    }

    private var oldCamX = 0

    private fun FlippingSpriteBatch.drawFramebufferWithZoom(buf: TextureRegion, zoom: Float) {
        val t = (if (App.getConfigBoolean("fx_streamerslayout")) App.scr.chatWidth / 2 else 0).toFloat()

        this.draw(buf,
            -0.5f * buf.regionWidth * zoom + 0.5f * buf.regionWidth + t * (zoom - 1f),
            -0.5f * buf.regionHeight * zoom + 0.5f * buf.regionHeight,
            buf.regionWidth * zoom,
            buf.regionHeight * zoom
        )
    }

    operator fun invoke(
        frameDelta: Float,
        gamePaused: Boolean,
        zoom: Float = 1f,
        actorsRenderFarBehind : List<ActorWithBody>,
        actorsRenderBehind : List<ActorWithBody>,
        actorsRenderMiddle : List<ActorWithBody>,
        actorsRenderMidTop : List<ActorWithBody>,
        actorsRenderFront  : List<ActorWithBody>,
        actorsRenderOverlay: List<ActorWithBody>,
        particlesContainer : CircularArray<ParticleBase>,
        player: ActorWithBody? = null,
        uiContainer: UIContainer? = null,
    ) {
        renderingActorsCount =
            (actorsRenderFarBehind.size) +
            (actorsRenderBehind.size) +
            (actorsRenderMiddle.size) +
            (actorsRenderMidTop.size) +
            (actorsRenderFront.size) +
            (actorsRenderOverlay.size)
        renderingUIsCount = uiContainer?.countVisible() ?: 0

        invokeInit()

        batch.color = Color.WHITE


        this.player = player


        if ((!gamePaused && !App.isScreenshotRequested()) || newWorldLoadedLatch) {
            measureDebugTime("Renderer.LightRun*") {
                // recalculate for every three frame, or if the sign of the cam-x changed
                if (App.GLOBAL_RENDER_TIMER % 3 == 0L || Math.abs(WorldCamera.x - oldCamX) >= world.width * 0.85f * TILE_SIZEF || newWorldLoadedLatch) {
                    LightmapRenderer.recalculate(actorsRenderFarBehind + actorsRenderBehind + actorsRenderFront + actorsRenderMidTop + actorsRenderMiddle + actorsRenderOverlay)
                }
                oldCamX = WorldCamera.x
            }

            prepLightmapRGBA()
            BlocksDrawer.renderData()
            drawToRGB(frameDelta, actorsRenderFarBehind, actorsRenderBehind, actorsRenderMiddle, actorsRenderMidTop, actorsRenderFront, actorsRenderOverlay, particlesContainer)
            drawToA(frameDelta, actorsRenderFarBehind, actorsRenderBehind, actorsRenderMiddle, actorsRenderMidTop, actorsRenderFront, actorsRenderOverlay, particlesContainer)
            drawOverlayActors(frameDelta, actorsRenderOverlay)

            if (player != null && player is Pocketed) drawAimGuide(frameDelta, player)
        }

        batch.color = Color.WHITE

        // clear main or whatever super-FBO being used
        //clearBuffer()
        gdxClearAndEnableBlend(.64f, .754f, .84f, 0f)

        ///////////////////////////////////////////////////////////////////////

        // use shader to mix RGB and A
        setCameraPosition(0f, 0f)

        rgbTex.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        aTex.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        fboMixedOut.inAction(camera, batch) {
            gdxClearAndEnableBlend(0f, 0f, 0f, 0f)

            // draw sky
            measureDebugTime("WeatherMixer.render") {
                WeatherMixer.render(frameDelta, camera, batch, world)
            }


            // normal behaviour
            if (!KeyToggler.isOn(Input.Keys.F6) &&
                !KeyToggler.isOn(Input.Keys.F7)
            ) {
                debugMode = 0

                aTex.texture.bind(1)
                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it


                batch.inUse {
                    blendNormalStraightAlpha(batch)
                    batch.shader = shaderBlendGlow
                    shaderBlendGlow.setUniformi("tex1", 1)
                    batch.drawFramebufferWithZoom(rgbTex, zoom)
                }

            }
            // something about RGB
            else if (KeyToggler.isOn(Input.Keys.F6) &&
                     !KeyToggler.isOn(Input.Keys.F7)
            ) {
                debugMode = 1
                batch.inUse {
                    blendNormalStraightAlpha(batch)
                    batch.shader = null
                    batch.drawFramebufferWithZoom(rgbTex, zoom)

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
                    blendNormalStraightAlpha(batch)
                    batch.shader = null
                    batch.drawFramebufferWithZoom(aTex, zoom)

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

        blendNormalStraightAlpha(batch)

        val (vo, vg) = world.weatherbox.let {
            if (it.currentWeather.identifier == "titlescreen")
                1f to 1f
            else
                it.currentVibrancy.x to it.currentVibrancy.y
        }

        mixedOutTex.texture.bind(0)
        shaderVibrancy.bind()
        shaderVibrancy.setUniformMatrix("u_projTrans", camera.combined)
        shaderVibrancy.setUniformi("u_texture", 0)
        shaderVibrancy.setUniformf("vibrancy", 1f, vo, vg, 1f)
        fullscreenQuad.render(shaderVibrancy, GL20.GL_TRIANGLE_FAN)


        ///////////////////////////////////////////////////////////////////////

        if (screencapRequested) {
            printdbg(this, "Screencap was requested, processing...")
            var hasError = false
            try {
                screencapExportCallback(fboMixedOut)
            }
            catch (e: Throwable) {
                printdbgerr(this, "An error occured while taking screencap:")
                e.printStackTrace()
                hasError = true
            }
            printdbg(this, "Screencap ${if (hasError) "failed" else "successful"}")
            screencapBusy = false
            screencapRequested = false
        }

        ///////////////////////////////////////////////////////////////////////

        // draw UI
        setCameraPosition(0f, 0f)

        batch.inUse {
            batch.shader = null
            batch.color = Color.WHITE

            if (!KeyToggler.isOn(Input.Keys.F4)) {
                uiContainer?.forEach {
                    it?.render(frameDelta, batch, camera)
                }
            }
        }

        // works but some UI elements have wrong transparency -> should be fixed with Terrarum.gdxCleanAndSetBlend -- Torvald 2019-01-12
        blendNormalStraightAlpha(batch)
        batch.color = Color.WHITE


        if (newWorldLoadedLatch) newWorldLoadedLatch = false
    }


    private fun prepLightmapRGBA() {
        lightmapFbo.inAction(null, null) {
            clearBuffer()
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }

        if (KeyToggler.isOn(Input.Keys.F5))
            processNoBlur(lightmapFbo)
        else
            processKawaseBlur(lightmapFbo)


        blendNormalStraightAlpha(batch)
    }

    private val shadowBlack2 = Color(0f, 0f, 0f, 0.55f)

    /**
     * This "screencap" will capture the game WITHOUT gui and postprocessors!
     * To capture the entire game, use [App.requestScreenshot]
     */
    @Volatile private var screencapRequested = false
    @Volatile internal var screencapBusy = false; private set
    @Volatile internal var screencapExportCallback: (FrameBuffer) -> Unit = {}
    @Volatile internal lateinit var fboRGBexport: Pixmap

    fun requestScreencap() {
        screencapRequested = true
        screencapBusy = true
        printdbg(this, "requestScreencap called from:")
        printStackTrace(this)
    }

    private fun drawToRGB(
        frameDelta: Float,
        actorsRenderFarBehind: List<ActorWithBody>?,
        actorsRenderBehind: List<ActorWithBody>?,
        actorsRenderMiddle: List<ActorWithBody>?,
        actorsRenderMidTop: List<ActorWithBody>?,
        actorsRenderFront : List<ActorWithBody>?,
        actorsOverlay : List<ActorWithBody>?,
        particlesContainer: CircularArray<ParticleBase>?
    ) {
        fboRGB.inAction(null, null) { clearBuffer() }
        fboEmissive.inAction(null, null) { clearBuffer() }
        fboRGB_lightMixed0.inAction(null, null) { clearBuffer() }
        fboRGB_lightMixed.inAction(null, null) { clearBuffer() }

        fboRGBactorsBehind.inAction(camera, batch) {
            clearBuffer()
            setCameraPosition(0f, 0f)
            BlocksDrawer.drawTerrainWallSticker(batch.projectionMatrix, false)

            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE
                moveCameraToWorldCoord()
                actorsRenderFarBehind?.forEach { it.drawBody1(frameDelta, batch) }
                actorsRenderBehind?.forEach { it.drawBody1(frameDelta, batch) }
            }
        }
        BlurMgr.makeBlurSmall(fboRGBactorsBehind, fboRGBactorsBehindShadow, 1f)
        BlurMgr.makeBlur(fboRGBactorsBehind, fboRGBactorsBehindShadow2, 0.5f)

        fboRGBactorsMiddle.inAction(camera, batch) {
            clearBuffer()
            setCameraPosition(0f, 0f)

            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE
                moveCameraToWorldCoord()
                actorsRenderMiddle?.forEach { it.drawBody1(frameDelta, batch) }
            }
        }
        BlurMgr.makeBlur(fboRGBactorsMiddle, fboRGBactorsMiddleShadow, 2.5f)

        fboRGBwall.inAction(camera, batch) {
            clearBuffer()
            setCameraPosition(0f, 0f)
            BlocksDrawer.drawWall(batch.projectionMatrix, false)
        }

        fboRGBterrain.inAction(camera, batch) {
            clearBuffer()
            setCameraPosition(0f, 0f)
            BlocksDrawer.drawTerrain(batch.projectionMatrix, false)
        }
        BlurMgr.makeBlur(fboRGBterrain, fboRGBterrainShadow, 2.5f)

        /////////////////////////////////////////////////////////////////////////////////////////////////////

        fboRGB.inAction(camera, batch) {
            setCameraPosition(0f, 0f)

            batch.inUse {
                batch.shader = null
                batch.color = Color.WHITE
                batch.drawFlipped(fboRGBwall.colorBufferTexture, 0f, 0f)
            }

            // draw actor shadow BEFORE the terrain draw
            fboRGBwall.colorBufferTexture.bind(1)
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

            batch.inUse {
                batch.shader = shaderShadowShallow
                batch.color = Color.BLACK
                shaderShadowShallow.setUniformi("u_wall", 1)
                setCameraPosition(0f, 0f)
                batch.drawFlipped(fboRGBactorsBehindShadow.colorBufferTexture, 0f, 0f)
            }
            batch.inUse {
                batch.shader = shaderShadowDeep
                batch.color = shadowBlack2
                shaderShadowShallow.setUniformi("u_wall", 1)
                setCameraPosition(0f, 0f)
                batch.drawFlipped(fboRGBactorsBehindShadow2.colorBufferTexture, 0f, 0f)
            }

            batch.inUse {
                batch.shader = shaderShadowDeep
                batch.color = Color.BLACK
                shaderShadowDeep.setUniformi("u_wall", 1)
                setCameraPosition(0f, 0f)
                batch.drawFlipped(fboRGBterrainShadow.colorBufferTexture, 0f, 0f)
                batch.drawFlipped(fboRGBactorsMiddleShadow.colorBufferTexture, 0f, 0f)
            }

            // draw behind actors and particles
            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE

                setCameraPosition(0f, 0f)
                batch.color = Color.WHITE
                batch.drawFlipped(fboRGBactorsBehind.colorBufferTexture, 0f, 0f)

                moveCameraToWorldCoord()
                particlesContainer?.forEach { it.drawBody(frameDelta, batch) }
            }

            // draw just the terrain
            batch.inUse {
                batch.shader = null
                setCameraPosition(0f, 0f)
                batch.color = Color.WHITE
                batch.drawFlipped(fboRGBterrain.colorBufferTexture, 0f, 0f)
            }

            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE
                /////////////////
                // draw actors //
                /////////////////
                setCameraPosition(0f, 0f)
                batch.color = Color.WHITE
                batch.drawFlipped(fboRGBactorsMiddle.colorBufferTexture, 0f, 0f)

                moveCameraToWorldCoord()
                actorsRenderMidTop?.forEach { it.drawBody1(frameDelta, batch) }
                player?.drawBody1(frameDelta, batch)
                actorsRenderFront?.forEach { it.drawBody1(frameDelta, batch) }
                // --> Change of blend mode <-- introduced by children of ActorWithBody //
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawFront(batch.projectionMatrix) // blue coloured filter of water, etc.

            batch.shader = null
            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE
                FeaturesDrawer.drawEnvOverlay(batch)
            }
        }

        fboEmissive.inAction(camera, batch) {
            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE
                moveCameraToWorldCoord()
                actorsRenderFarBehind?.forEach { it.drawEmissive(frameDelta, batch) }
                actorsRenderBehind?.forEach { it.drawEmissive(frameDelta, batch) }
                particlesContainer?.forEach { it.drawEmissive(frameDelta, batch) }
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawTerrain(batch.projectionMatrix, false, true)

            batch.shader = shaderForActors
            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE
                /////////////////
                // draw actors //
                /////////////////
                moveCameraToWorldCoord()
                actorsRenderMiddle?.forEach { it.drawEmissive(frameDelta, batch) }
                actorsRenderMidTop?.forEach { it.drawEmissive(frameDelta, batch) }
                player?.drawEmissive(frameDelta, batch)
                actorsRenderFront?.forEach { it.drawEmissive(frameDelta, batch) }
                // --> Change of blend mode <-- introduced by children of ActorWithBody //
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawFront(batch.projectionMatrix, true) // blue coloured filter of water, etc.
        }

        fboRGB_lightMixed0.inAction(camera, batch) {

            setCameraPosition(0f, 0f)
            val (xrem, yrem) = worldCamToRenderPos()

            gdxEnableBlend()

//            App.getCurrentDitherTex().bind(1)
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

            batch.inUse {

                batch.color = Color.WHITE
                blendNormalStraightAlpha(batch)

                // draw world
                batch.shader = shaderDemultiply
                batch.draw(fboRGB.colorBufferTexture, 0f, 0f)
                batch.flush()

                // multiply light on top of it
                lightTex.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

                if (KeyToggler.isOn(Input.Keys.F8))
                    blendNormalStraightAlpha(batch)
                else
                    blendMul(batch)

//                if (!KeyToggler.isOn(Input.Keys.F9)) {
                    batch.shader = shaderRGBOnly
                    batch.shader.setUniformi("rnd", rng.nextInt(8192), rng.nextInt(8192))
                    batch.shader.setUniformi("u_pattern", 1)
                    batch.draw(
                        lightTex,
                        xrem, yrem - TILE_SIZEF * 0.5f,
                        lightTex.regionWidth * lightmapDownsample,
                        lightTex.regionHeight * lightmapDownsample
                    )
//                }

            }



            // NOTE TO SELF: this works.
        }


        fboRGB_lightMixed.inActionF(camera, batch) {

            setCameraPosition(0f, 0f)
            val (xrem, yrem) = worldCamToRenderPos()

            gdxEnableBlend()


            fboEmissive.colorBufferTexture.bind(1)
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it


            // draw emissive
            batch.inUse {
                batch.color = Color.WHITE
                blendNormalStraightAlpha(batch)
                batch.shader = shaderBlendGlowTex1Flip
                shaderBlendGlowTex1Flip.setUniformi("tex1", 1)
                shaderBlendGlowTex1Flip.setUniformi("tex1flip", 1)

                batch.color = Color.WHITE
                batch.draw(fboRGB_lightMixed0.colorBufferTexture, 0f, 0f)
                batch.flush()
            }
        }

        blendNormalStraightAlpha(batch)
    }

    private fun drawToA(
        frameDelta: Float,
        actorsRenderFarBehind: List<ActorWithBody>?,
        actorsRenderBehind: List<ActorWithBody>?,
        actorsRenderMiddle: List<ActorWithBody>?,
        actorsRenderMidTop: List<ActorWithBody>?,
        actorsRenderFront : List<ActorWithBody>?,
        actorsOverlay : List<ActorWithBody>?,
        particlesContainer: CircularArray<ParticleBase>?
    ) {
        fboA.inAction(null, null) {
            clearBuffer()
            // paint black
            gdxClearAndEnableBlend(0f,0f,0f,1f) // solid black: so that unused area will be also black
        }
        fboA_lightMixed.inAction(null, null) { clearBuffer() }

        fboA.inAction(camera, batch) {

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawWall(batch.projectionMatrix, true)

            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE

                moveCameraToWorldCoord()
                actorsRenderFarBehind?.forEach { it.drawGlow(frameDelta, batch) }
                actorsRenderBehind?.forEach { it.drawGlow(frameDelta, batch) }
                particlesContainer?.forEach { it.drawGlow(frameDelta, batch) }
            }

            setCameraPosition(0f, 0f)
            BlocksDrawer.drawTerrain(batch.projectionMatrix, true)

            batch.inUse {
                /////////////////
                // draw actors //
                /////////////////
                moveCameraToWorldCoord()
                actorsRenderMiddle?.forEach { it.drawGlow(frameDelta, batch) }
                actorsRenderMidTop?.forEach { it.drawGlow(frameDelta, batch) }
                player?.drawGlow(frameDelta, batch)
                actorsRenderFront?.forEach { it.drawGlow(frameDelta, batch) }
                // --> Change of blend mode <-- introduced by children of ActorWithBody //
            }
        }


        fboA_lightMixed.inAction(camera, batch) {

            setCameraPosition(0f, 0f)
            val (xrem, yrem) = worldCamToRenderPos()

//            App.getCurrentDitherTex().bind(1)
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

            batch.inUse {
                batch.shader = null
                batch.color = Color.WHITE

                // draw world
                batch.draw(fboA.colorBufferTexture, 0f, 0f)
                batch.flush()

                // multiply light on top of it
                lightTex.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

                if (KeyToggler.isOn(Input.Keys.F8))
                    blendNormalStraightAlpha(batch)
                else
                    blendMul(batch)

                batch.shader = shaderAtoGrey
                batch.shader.setUniformi("rnd", rng.nextInt(8192), rng.nextInt(8192))
                batch.shader.setUniformi("u_pattern", 1)
                batch.draw(lightTex,
                    xrem, yrem - TILE_SIZEF * 0.5f,
                        lightTex.regionWidth * lightmapDownsample,
                        lightTex.regionHeight * lightmapDownsample
                )


            }


            // NOTE TO SELF: this works.
        }


        blendNormalStraightAlpha(batch)
    }

    private fun drawOverlayActors(frameDelta: Float, actors: List<ActorWithBody>?) {
        fboRGB_lightMixed.inActionF(camera, batch) {

            setCameraPosition(0f, 0f)
            // BlocksDrawer.renderWhateverGlow_WALL

            batch.inUse {
                batch.shader = shaderForActors
                batch.color = Color.WHITE

                moveCameraToWorldCoord()
                actors?.forEach {
                    it.drawBody1(frameDelta, batch)
                }
            }

            setCameraPosition(0f, 0f)
            // BlocksDrawer.renderWhateverGlow_TERRAIN
        }
    }

    private fun drawAimGuide(frameDelta: Float, player: ActorWithBody) {
        fboRGB_lightMixed.inActionF(camera, batch) { fb ->

            setCameraPosition(0f, 0f)
            // BlocksDrawer.renderWhateverGlow_WALL

            batch.inUse {
                batch.shader = null
                batch.color = Color.WHITE

                moveCameraToWorldCoord()

                (player as Pocketed).inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]?.let { itemID ->
                    val heldItem = ItemCodex[itemID] // will be null for blocks
                    // this is a case-by-case affair
                    when (heldItem) {
                        is ItemThrowable -> drawTrajectoryForThrowable(fb, batch, frameDelta, player, world, heldItem)
                    }
                }
            }

            setCameraPosition(0f, 0f)
            // BlocksDrawer.renderWhateverGlow_TERRAIN
        }
    }
    private val cubeSize = 7.0
    private val hcubeSize = cubeSize / 2
    private val externalV = Vector2()
    private val maxStep = 56
    private val trajectoryFlow = 30

    private fun getSubmergedHeight(gravitation: Vector2, hitbox: Hitbox): Double {
        val straightGravity = (gravitation.y > 0)
        // TODO reverse gravity
        if (!straightGravity) TODO()

        val itsY = (hitbox.startY / TILE_SIZED).toInt()
        val iteY = (hitbox.endY / TILE_SIZED).toInt()
        val txL = (hitbox.startX / TILE_SIZED).floorToInt()
        val txR = (hitbox.endX / TILE_SIZED).floorToInt()

        var hL = 0.0
        var hR = 0.0

        val rec = java.util.ArrayList<Double>()

        for (ty in itsY..iteY) {
            val fL = world.getFluid(txL, ty).amount.coerceAtMost(1f) * TILE_SIZED // 0-16
            val fR = world.getFluid(txR, ty).amount.coerceAtMost(1f) * TILE_SIZED // 0-16

            // if head
            if (ty == itsY) {
                val actorHs = hitbox.startY % TILE_SIZED // 0-16
                val yp = TILE_SIZED - actorHs // 0-16

                hL += min(yp, fL)
                hR += min(yp, fR)

                rec.add(min(yp, fL))
            }
            // if tail
            else if (ty == iteY) {
                val actorHe = hitbox.endY % TILE_SIZED // 0-16

                hL += (actorHe - TILE_SIZED + fL).coerceAtLeast(0.0)
                hR += (actorHe - TILE_SIZED + fR).coerceAtLeast(0.0)

                rec.add((actorHe - TILE_SIZED + fL).coerceAtLeast(0.0))
            }
            else {
                hL += fL
                hR += fR

                rec.add(fL)
            }
        }

        // returns average of two sides
        return (hL + hR) / 2.0
    }


    private fun forEachOccupyingFluid(hitbox: Hitbox, consumer: (GameWorld.FluidInfo?) -> Unit) {
        val hIntTilewiseHitbox = Hitbox(0.0, 0.0, 1.0, 1.0).setFromTwoPoints(
            hitbox.startX.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
            hitbox.startY.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
            hitbox.endX.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5,
            hitbox.endY.plus(PHYS_EPSILON_DIST).div(TILE_SIZE).floorToDouble() + 0.5
        )
        val tileProps = java.util.ArrayList<GameWorld.FluidInfo?>()
        for (y in hIntTilewiseHitbox.startY.toInt()..hIntTilewiseHitbox.endY.toInt()) {
            for (x in hIntTilewiseHitbox.startX.toInt()..hIntTilewiseHitbox.endX.toInt()) {
                tileProps.add(world.getFluid(x, y))
            }
        }

        return tileProps.forEach(consumer)
    }


    private fun drawTrajectoryForThrowable(frameBuffer: FrameBuffer, batch: SpriteBatch, frameDelta: Float, player: ActorWithBody, world: GameWorld, item: ItemThrowable) {
        val ww = world.width * TILE_SIZEF


        mouseInInteractableRange(player) { mx, my, mtx, mty ->
            val (throwPos, throwVector) = getThrowPosAndVector(player)
            val grav = world.gravitation
            val toff = (App.GLOBAL_RENDER_TIMER % trajectoryFlow) / trajectoryFlow.toFloat()
            externalV.set(throwVector)

            val points = ArrayList<Pair<Float, Float>>()

            var c = 0
            while (c < maxStep) {

                // plot a dot
                points.add(throwPos.x.toFloat() to throwPos.y.toFloat())

                // simulate physics
                applyGravitation(grav, cubeSize) // TODO use actual value instead of `cubeSize`
                val hb = Hitbox(throwPos.x  - hcubeSize, throwPos.y - hcubeSize, cubeSize, cubeSize)
                var tileDensityFluid = 0
                forEachOccupyingFluid(hb) {
                    // get max density for each tile
                    if (it?.isFluid() == true && it.getProp().density > tileDensityFluid) {
                        tileDensityFluid = it.getProp().density
                    }
                }
                val submergedHeight = getSubmergedHeight(grav, hb)
                val submergedRatio = submergedHeight / cubeSize
                applyBuoyancy(grav, item.mass, item.material.density.toDouble(), tileDensityFluid, submergedRatio)
                // move the point
                throwPos += externalV
                // more physics
                setHorizontalFriction()
                setVerticalFriction()


                // break if colliding with a tile
                val hitSolid = listOf(
                    throwPos + Vector2(-hcubeSize, -hcubeSize),
                    throwPos + Vector2(-hcubeSize, +hcubeSize),
                    throwPos + Vector2(+hcubeSize, +hcubeSize),
                    throwPos + Vector2(+hcubeSize, -hcubeSize),
                ).any {
                    val wx = (it.x / TILE_SIZED).toInt()
                    val wy = (it.y / TILE_SIZED).toInt()
                    val tile = world.getTileFromTerrain(wx, wy)
                    BlockCodex[tile].isSolid
                }

                if (hitSolid) {
                    points.add(throwPos.x.toFloat() to throwPos.y.toFloat())
                    break
                }

                c++
            }


            if (points.size > 4) {
                var v0 = points[0]
                var v1 = points[0]
                var v2 = points[1]
                var v3 = points[2]
                for (i in 3 until points.size) {
                    // shift vars
                    v0 = v1; v1 = v2; v2 = v3; v3 = points[i]

                    val xp = FastMath.interpolateCatmullRom(toff, v0.first, v1.first, v2.first, v3.first)
                    val yp = FastMath.interpolateCatmullRom(toff, v0.second, v1.second, v2.second, v3.second)

                    batch.color = Color(0.9f, 0.9f, 0.9f, 0.9f * (1f - ((i-3+toff) / maxStep).sqr()))
                    Toolkit.fillArea(batch, xp, yp, 2f, 2f)
                    Toolkit.fillArea(batch, xp + ww, yp, 2f, 2f)
                    Toolkit.fillArea(batch, xp - ww, yp, 2f, 2f)
                }
            }


            1L
        }



    }

    private val bodyFriction = BlockCodex[Block.AIR].friction.frictionToMult()


    private fun applyGravitation(gravitation: Vector2, hitboxWidth: Double) {
        applyForce(getDrag(externalV, gravitation, hitboxWidth))
    }

    private fun applyBuoyancy(grav: Vector2, mass: Double, density: Double, tileDensityFluid: Int, submergedRatio: Double) {


        val rho = tileDensityFluid // kg / m^3
        val V_full = mass / density * 2.0 // density = mass / volume, simply rearrange this. Multiplier of 2.0 is a hack!
        val V = V_full * submergedRatio
        val F_k = grav * mass // F = ma where a is g; TODO add jump-accel into 'a' to allow better jumping under water
        val F_bo = grav * (rho * V) // Newtons

        // mh'' = mg - rho*gv
        // h'' = (mg - rho*gv) / m

        // if tileDensity = actorDensity, F_k = F_bo (this will be the case if there was no hack)
//        printdbg(this, "F_k=$F_k [N] \t F_bo=${F_bo} [N] \t density=$density")

        val F = F_k - F_bo

        val acc = F / mass // (kg * m / s^2) / kg = m / s^2
        val acc_game = acc.let { Vector2(it.x, it.y.coerceAtMost(0.0)) } * SI_TO_GAME_ACC

        applyForce(acc_game)
    }

    private fun Int.frictionToMult(): Double = this / 16.0
    private fun Int.viscosityToMult(): Double = 16.0 / (16.0 + this)

    private fun applyForce(acc: Vector2) {
        externalV += acc
    }

    private fun getDrag(externalForce: Vector2, gravitation: Vector2, hitboxWidth: Double): Vector2 {
        val dragCoefficient = 1.2

        /**
         * weight; gravitational force in action
         * W = mass * G (9.8 [m/s^2])
         */
        val W: Vector2 = gravitation * Terrarum.PHYS_TIME_FRAME
        /**
         * Area
         */
        val A: Double = (hitboxWidth / METER).sqr() // this is not physically accurate but it's needed to make large playable characters more controllable
        /**
         * Drag of atmosphere
         * D = Cd (drag coefficient) * 0.5 * rho (density) * V^2 (velocity sqr) * A (area)
         */
        val D: Vector2 = Vector2(externalForce.x.magnSqr(), externalForce.y.magnSqr()) * dragCoefficient * 0.5 * A// * tileDensityFluid.toDouble()

        val V: Vector2 = (W - D) / Terrarum.PHYS_TIME_FRAME * SI_TO_GAME_ACC

        return V

        // FIXME v * const, where const = 1.0 for FPS=60, sqrt(2.0) for FPS=30, etc.
        //       this is "close enough" solution and not perfect.
   }

    /** about stopping
     * for about get moving, see updateMovementControl */
    private fun setHorizontalFriction() {
        val friction =  0.3 * bodyFriction

        if (externalV.x < 0) {
            externalV.x += friction
            if (externalV.x > 0) externalV.x = 0.0 // compensate overshoot
        }
        else if (externalV.x > 0) {
            externalV.x -= friction
            if (externalV.x < 0) externalV.x = 0.0 // compensate overshoot
        }
    }

    private fun setVerticalFriction() {
        val friction =  0.3 * bodyFriction

        if (externalV.y < 0) {
            externalV.y += friction
            if (externalV.y > 0) externalV.y = 0.0 // compensate overshoot
        }
        else if (externalV.y > 0) {
            externalV.y -= friction
            if (externalV.y < 0) externalV.y = 0.0 // compensate overshoot
        }
    }




    private fun invokeInit() {
        if (!initDone) {
            batch = FlippingSpriteBatch()
            camera = OrthographicCamera(WIDTHF, HEIGHTF)

            camera.setToOrtho(true, WIDTHF, HEIGHTF)
            camera.update()

            resize(WIDTH, HEIGHT)

            initDone = true
        }
    }

    private fun clearBuffer() {
        gdxClearAndEnableBlend(0f,0f,0f,0f)
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
        camera.position.set((-newX + App.scr.halfw).roundToFloat(), (-newY + App.scr.halfh).roundToFloat(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }


    private var blurtex0 = Texture(16, 16, Pixmap.Format.RGBA8888)
    private lateinit var blurtex1: Texture
    private lateinit var blurtex2: Texture
    private lateinit var blurtex3: Texture
    private lateinit var blurtex4: Texture

    private const val KAWASE_POWER = 1.5f

    fun processNoBlur(outFbo: Float16FrameBuffer) {

        blurtex0.dispose()


        outFbo.inAction(camera, batch) {
            blurtex0 = LightmapRenderer.draw()
            blurtex0.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            blurtex0.bind(0)
            App.shaderPassthruRGBA.bind()
            App.shaderPassthruRGBA.setUniformMatrix("u_projTrans", camera.combined)
            App.shaderPassthruRGBA.setUniformi("u_texture", 0)
            blurWriteQuad.render(App.shaderPassthruRGBA, GL20.GL_TRIANGLE_FAN)
        }
    }

    fun processKawaseBlur(outFbo: Float16FrameBuffer) {

        blurtex0.dispose()


        // initialise readBuffer with untreated lightmap
        outFbo.inAction(camera, batch) {
            blurtex0 = LightmapRenderer.draw()
            blurtex0.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex0.bind(0)
            App.shaderPassthruRGBA.bind()
            App.shaderPassthruRGBA.setUniformMatrix("u_projTrans", camera.combined)
            App.shaderPassthruRGBA.setUniformi("u_texture", 0)
            blurWriteQuad.render(App.shaderPassthruRGBA, GL20.GL_TRIANGLE_FAN)
        }

        fboBlurHalf.inAction(camera, batch) {
            blurtex1 = outFbo.colorBufferTexture
            blurtex1.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex1.bind(0)
            shaderKawaseDown.bind()
            shaderKawaseDown.setUniformMatrix("u_projTrans", camera.combined)
            shaderKawaseDown.setUniformi("u_texture", 0)
            shaderKawaseDown.setUniformf("halfpixel", KAWASE_POWER / fboBlurHalf.width, KAWASE_POWER / fboBlurHalf.height)
            blurWriteQuad2.render(shaderKawaseDown, GL20.GL_TRIANGLE_FAN)
        }

        /*fboBlurQuarter.inAction(camera, batch) {
            blurtex2 = fboBlurHalf.colorBufferTexture
            blurtex2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex2.bind(0)
            shaderKawaseDown.bind()
            shaderKawaseDown.setUniformMatrix("u_projTrans", camera.combined)
            shaderKawaseDown.setUniformi("u_texture", 0)
            shaderKawaseDown.setUniformf("halfpixel", KAWASE_POWER / fboBlurQuarter.width, KAWASE_POWER / fboBlurQuarter.height)
            blurWriteQuad4.render(shaderKawaseDown, GL20.GL_TRIANGLE_FAN)
        }

        fboBlurHalf.inAction(camera, batch) {
            blurtex3 = fboBlurQuarter.colorBufferTexture
            blurtex3.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex3.bind(0)
            shaderKawaseUp.bind()
            shaderKawaseUp.setUniformMatrix("u_projTrans", camera.combined)
            shaderKawaseUp.setUniformi("u_texture", 0)
            shaderKawaseUp.setUniformf("halfpixel", KAWASE_POWER / fboBlurQuarter.width, KAWASE_POWER / fboBlurQuarter.height)
            blurWriteQuad2.render(shaderKawaseUp, GL20.GL_TRIANGLE_FAN)
        }*/

        outFbo.inAction(camera, batch) {
            blurtex4 = fboBlurHalf.colorBufferTexture
            blurtex4.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            blurtex4.bind(0)
            shaderKawaseUp.bind()
            shaderKawaseUp.setUniformMatrix("u_projTrans", camera.combined)
            shaderKawaseUp.setUniformi("u_texture", 0)
            shaderKawaseUp.setUniformf("halfpixel", KAWASE_POWER / fboBlurHalf.width, KAWASE_POWER / fboBlurHalf.height)
            blurWriteQuad.render(shaderKawaseUp, GL20.GL_TRIANGLE_FAN)
        }

    }

    private var init = false

    fun resize(width: Int, height: Int) {
        if (!init) {
            blurWriteQuad = Mesh(
                    true, 4, 4,
                    VertexAttribute.Position(),
                    VertexAttribute.ColorUnpacked(),
                    VertexAttribute.TexCoords(0)
            )
            blurWriteQuad2 = Mesh(
                    true, 4, 4,
                    VertexAttribute.Position(),
                    VertexAttribute.ColorUnpacked(),
                    VertexAttribute.TexCoords(0)
            )
            /*blurWriteQuad4 = Mesh(
                    true, 4, 4,
                    VertexAttribute.Position(),
                    VertexAttribute.ColorUnpacked(),
                    VertexAttribute.TexCoords(0)
            )*/
            init = true
        }
        else {
            fboRGB.dispose()
            fboRGB_lightMixed0.dispose()
            fboRGB_lightMixed.dispose()
            fboA.dispose()
            fboA_lightMixed.dispose()
            fboEmissive.dispose()
            lightmapFbo.dispose()

            fboBlurHalf.dispose()
            //fboBlurQuarter.dispose()
        }

        fboRGB = Float16FrameBuffer(width, height, false)
        fboRGB_lightMixed0 = Float16FrameBuffer(width, height, false)
        fboRGB_lightMixed = Float16FrameBuffer(width, height, false)
        fboA = Float16FrameBuffer(width, height, false)
        fboA_lightMixed = Float16FrameBuffer(width, height, false)
        fboEmissive = Float16FrameBuffer(width, height, false)
        fboMixedOut = Float16FrameBuffer(width, height, false)
        fboRGBactorsBehind = Float16FrameBuffer(width, height, false)
        fboRGBactorsMiddle = Float16FrameBuffer(width, height, false)
        fboRGBterrain = Float16FrameBuffer(width, height, false)
        fboRGBactorsBehindShadow = Float16FrameBuffer(width, height, false)
        fboRGBactorsBehindShadow2 = Float16FrameBuffer(width, height, false)
        fboRGBactorsMiddleShadow = Float16FrameBuffer(width, height, false)
        fboRGBterrainShadow = Float16FrameBuffer(width, height, false)
        fboRGBwall = Float16FrameBuffer(width, height, false)
        lightmapFbo = Float16FrameBuffer(
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                false
        )
        rgbTex = TextureRegion(fboRGB_lightMixed.colorBufferTexture)
        aTex = TextureRegion(fboA_lightMixed.colorBufferTexture)
        lightTex = TextureRegion(lightmapFbo.colorBufferTexture)
        blurTex = TextureRegion()
        mixedOutTex = TextureRegion(fboMixedOut.colorBufferTexture)

        fboBlurHalf = Float16FrameBuffer(
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt() / 2,
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt() / 2,
                false
        )

        /*fboBlurQuarter = Float16FrameBuffer(
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt() / 4,
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt() / 4,
                false
        )*/

        BlocksDrawer.resize(width, height)
        LightmapRenderer.resize(width, height)


        blurWriteQuad.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                lightmapFbo.width.toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                lightmapFbo.width.toFloat(),lightmapFbo.height.toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f,lightmapFbo.height.toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
        blurWriteQuad.setIndices(shortArrayOf(0, 1, 2, 3))

        blurWriteQuad2.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                lightmapFbo.width.div(2).toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                lightmapFbo.width.div(2).toFloat(),lightmapFbo.height.div(2).toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f,lightmapFbo.height.div(2).toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
        blurWriteQuad2.setIndices(shortArrayOf(0, 1, 2, 3))

        /*blurWriteQuad4.setVertices(floatArrayOf(
                0f,0f,0f, 1f,1f,1f,1f, 0f,1f,
                lightmapFbo.width.div(4).toFloat(),0f,0f, 1f,1f,1f,1f, 1f,1f,
                lightmapFbo.width.div(4).toFloat(),lightmapFbo.height.div(4).toFloat(),0f, 1f,1f,1f,1f, 1f,0f,
                0f,lightmapFbo.height.div(4).toFloat(),0f, 1f,1f,1f,1f, 0f,0f))
        blurWriteQuad4.setIndices(shortArrayOf(0, 1, 2, 3))*/
    }

    override fun dispose() {
        if (::blurWriteQuad.isInitialized) blurWriteQuad.tryDispose()
        if (::blurWriteQuad2.isInitialized) blurWriteQuad2.tryDispose()
        //if (::blurWriteQuad4.isInitialized) blurWriteQuad4.tryDispose()

        if (::fboRGB.isInitialized) fboRGB.tryDispose()
        if (::fboA.isInitialized) fboA.tryDispose()
        if (::fboRGB_lightMixed0.isInitialized) fboRGB_lightMixed0.tryDispose()
        if (::fboRGB_lightMixed.isInitialized) fboRGB_lightMixed.tryDispose()
        if (::fboA_lightMixed.isInitialized) fboA_lightMixed.tryDispose()
        if (::fboEmissive.isInitialized) fboEmissive.tryDispose()
        if (::fboMixedOut.isInitialized) fboMixedOut.tryDispose()
        if (::lightmapFbo.isInitialized) lightmapFbo.tryDispose()
        if (::fboRGBactorsBehind.isInitialized) fboRGBactorsBehind.tryDispose()
        if (::fboRGBactorsMiddle.isInitialized) fboRGBactorsMiddle.tryDispose()
        if (::fboRGBterrain.isInitialized) fboRGBterrain.tryDispose()
        if (::fboRGBactorsBehindShadow.isInitialized) fboRGBactorsBehindShadow.tryDispose()
        if (::fboRGBactorsBehindShadow2.isInitialized) fboRGBactorsBehindShadow2.tryDispose()
        if (::fboRGBactorsMiddleShadow.isInitialized) fboRGBactorsMiddleShadow.tryDispose()
        if (::fboRGBterrainShadow.isInitialized) fboRGBterrainShadow.tryDispose()
        if (::fboRGBwall.isInitialized) fboRGBwall.tryDispose()

        blurtex0.tryDispose()

        if (::fboBlurHalf.isInitialized) fboBlurHalf.tryDispose()
        //if (::fboBlurQuarter.isInitialized) fboBlurQuarter.tryDispose()

        LightmapRenderer.dispose()
        BlocksDrawer.dispose()
        WeatherMixer.dispose()

        if (::batch.isInitialized) batch.tryDispose()
        if (::fboRGBexport.isInitialized) fboRGBexport.tryDispose()
    }

    private fun worldCamToRenderPos(): Pair<Float, Float> {
        // for some reason it does not like integer. No, really; it breaks (jitter when you move) when you try to "fix" that.
        val xoff = (WorldCamera.x / TILE_SIZE) - LightmapRenderer.camX
        val yoff = (WorldCamera.y / TILE_SIZE) - LightmapRenderer.camY - 1
        val xrem = -(WorldCamera.x.toFloat() fmod TILE_SIZEF) - (xoff * TILE_SIZEF)
        val yrem = +(WorldCamera.y.toFloat() fmod TILE_SIZEF) + (yoff * TILE_SIZEF)

        return (xrem - LightmapRenderer.LIGHTMAP_OVERRENDER * TILE_SIZEF) to (yrem - LightmapRenderer.LIGHTMAP_OVERRENDER * TILE_SIZEF)
    }
}