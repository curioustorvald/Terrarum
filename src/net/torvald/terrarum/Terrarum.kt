package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.GdxRuntimeException
import com.jme3.math.FastMath
import net.torvald.dataclass.CircularArray
import net.torvald.random.HQRNG
import net.torvald.terrarum.AppLoader.*
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.GameFontBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.lwjgl.BufferUtils
import java.io.File
import kotlin.math.absoluteValue



typealias RGBA8888 = Int


/**
 * Slick2d Version Created by minjaesong on 2015-12-30.
 *
 * LibGDX Version Created by minjaesong on 2017-06-15.
 */
object Terrarum : Screen {

    /**
     * All singleplayer "Player" must have this exact reference ID.
     */
    const val PLAYER_REF_ID: Int = 0x91A7E2

    lateinit var batch: SpriteBatch
    lateinit var shapeRender: ShapeRenderer // DO NOT USE!! for very limited applications e.g. WeatherMixer
    inline fun inShapeRenderer(shapeRendererType: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Filled, action: (ShapeRenderer) -> Unit) {
        shapeRender.begin(shapeRendererType)
        action(shapeRender)
        shapeRender.end()
    }


    //////////////////////////////
    // GLOBAL IMMUTABLE CONFIGS //
    //////////////////////////////

    val WIDTH: Int
        get() = AppLoader.screenW
    val HEIGHT: Int
        get() = AppLoader.screenH

    //val WIDTH_MIN = 800
    //val HEIGHT_MIN = 600

    inline val HALFW: Int
        get() = WIDTH.ushr(1)
    inline val HALFH: Int
        get() = HEIGHT.ushr(1)

    /**
     * To be used with physics simulator. This is a magic number.
     */
    val PHYS_TIME_FRAME: Double = 26.0 + (2.0 / 3.0)
    // 26.0 + (2.0 / 3.0) // lower value == faster gravity response (IT WON'T HOTSWAP!!)
    // protip: using METER, game unit and SI unit will have same number



    var previousScreen: Screen? = null // to be used with temporary states like StateMonitorCheck


    var ingame: IngameInstance? = null

    private val javaHeapCircularArray = CircularArray<Int>(64)
    private val nativeHeapCircularArray = CircularArray<Int>(64)
    private val updateRateCircularArray = CircularArray<Double>(16)

    val memJavaHeap: Int
        get() {
            javaHeapCircularArray.add((Gdx.app.javaHeap shr 20).toInt())

            var acc = 0
            javaHeapCircularArray.forEach { acc = maxOf(acc, it) }
            return acc
        }
    val memNativeHeap: Int
        get() {
            nativeHeapCircularArray.add((Gdx.app.javaHeap shr 20).toInt())

            var acc = 0
            nativeHeapCircularArray.forEach { acc = maxOf(acc, it) }
            return acc
        }
    val memXmx: Int
        get() = (Runtime.getRuntime().maxMemory() shr 20).toInt()
    val updateRateStr: String
        get() {
            updateRateCircularArray.add(updateRate)

            var acc = 0.0
            updateRateCircularArray.forEach { acc = maxOf(acc, it) }
            return String.format("%.2f", acc)
        }




    val fontGame: GameFontBase = AppLoader.fontGame
    val fontSmallNumbers: TinyAlphNum = AppLoader.fontSmallNumbers

    var gamepadLabelStart: Char = 0xE000.toChar() // lateinit
    var gamepadLableSelect: Char = 0xE000.toChar() // lateinit
    var gamepadLabelNinA: Char = 0xE000.toChar() // lateinit TODO
    var gamepadLabelNinB: Char = 0xE000.toChar() // lateinit TODO
    var gamepadLabelNinX: Char = 0xE000.toChar() // lateinit TODO
    var gamepadLabelNinY: Char = 0xE000.toChar() // lateinit TODO
    var gamepadLabelNinL: Char = 0xE000.toChar() // lateinit TODO
    var gamepadLabelNinR: Char = 0xE000.toChar() // lateinit TODO
    var gamepadLabelNinZL: Char = 0xE000.toChar() // lateinit TODO
    var gamepadLabelNinZR: Char = 0xE000.toChar() // lateinit TODO
    val gamepadLabelLEFT = 0xE068.toChar()
    val gamepadLabelDOWN = 0xE069.toChar()
    val gamepadLabelUP = 0xE06A.toChar()
    val gamepadLabelRIGHT = 0xE06B.toChar()

    // 0x0 - 0xF: Game-related
    // 0x10 - 0x1F: Config
    // 0x100 and onward: unit tests for dev
    val STATE_ID_SPLASH = 0x0
    val STATE_ID_HOME = 0x1
    val STATE_ID_GAME = 0x3
    val STATE_ID_CONFIG_CALIBRATE = 0x11

    val STATE_ID_TEST_FONT = 0x100
    val STATE_ID_TEST_GFX = 0x101
    val STATE_ID_TEST_TTY = 0x102
    val STATE_ID_TEST_BLUR = 0x103
    val STATE_ID_TEST_SHADER = 0x104
    val STATE_ID_TEST_REFRESHRATE = 0x105
    val STATE_ID_TEST_INPUT = 0x106

    val STATE_ID_TEST_UI1 = 0x110

    val STATE_ID_TOOL_NOISEGEN = 0x200
    val STATE_ID_TOOL_RUMBLE_DIAGNOSIS = 0x201

    /** Available CPU threads */
    val THREADS = Runtime.getRuntime().availableProcessors() + 1

    /**
     * If the game is multithreading.
     * True if:
     *
     *     THREADS >= 2 and config "multithread" is true
     */
    val MULTITHREAD: Boolean
        get() = THREADS >= 3 && getConfigBoolean("multithread")

    const val NAME = AppLoader.GAME_NAME


    lateinit var shaderBlur: ShaderProgram
    lateinit var shaderBayer: ShaderProgram
    lateinit var shaderSkyboxFill: ShaderProgram
    lateinit var shaderBlendGlow: ShaderProgram
    lateinit var shaderRGBOnly: ShaderProgram
    lateinit var shaderAtoGrey: ShaderProgram

    lateinit var testTexture: Texture


    /** Actually just a mesh of four vertices, two triangles -- not a literal glQuad */
    val fullscreenQuad = AppLoader.fullscreenQuad



    lateinit var assetManager: AssetManager // TODO


    init {
        println("$NAME version ${AppLoader.getVERSION_STRING()}")
        println("Java Runtime version ${System.getProperty("java.version")}")
        println("LibGDX version ${com.badlogic.gdx.Version.VERSION}")


        println("os.arch = $systemArch") // debug info

        if (is32BitJVM) {
            printdbgerr(this, "32 Bit JVM detected")
        }


        println("processor = $processor")
        println("vendor = $processorVendor")


        gamepadLabelStart = when (getConfigString("gamepadlabelstyle")) {
            "nwii"     -> 0xE04B.toChar() // + mark
            "logitech" -> 0xE05A.toChar() // number 10
            else       -> 0xE042.toChar() // |> mark (sonyps, msxb360, generic)
        }
        gamepadLableSelect = when (getConfigString("gamepadlabelstyle")) {
            "nwii"     -> 0xE04D.toChar() // - mark
            "logitech" -> 0xE059.toChar() // number 9
            "sonyps"   -> 0xE043.toChar() // solid rectangle
            "msxb360"  -> 0xE041.toChar() // <| mark
            else       -> 0xE043.toChar() // solid rectangle
        }



    }


    val RENDER_FPS = getConfigInt("displayfps")
    val USE_VSYNC = getConfigBoolean("usevsync")
    var VSYNC = USE_VSYNC
    val VSYNC_TRIGGER_THRESHOLD = 56
    val GL_VERSION: Int
        get() = Gdx.graphics.glVersion.majorVersion * 100 +
                Gdx.graphics.glVersion.minorVersion * 10 +
                Gdx.graphics.glVersion.releaseVersion
    val MINIMAL_GL_VERSION = 210
    val GL_MAX_TEXTURE_SIZE: Int
        get() {
            val intBuffer = BufferUtils.createIntBuffer(16) // size must be at least 16, or else LWJGL complains
            Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, intBuffer)

            intBuffer.rewind()

            return intBuffer.get()
        }
    val MINIMAL_GL_MAX_TEXTURE_SIZE = 4096

    override fun show() {
        assetManager = AssetManager()


        testTexture = Texture(Gdx.files.internal("./assets/test_texture.tga"))


        val glInfo = Gdx.graphics.glVersion.debugVersionString

        println("GL_VERSION = $GL_VERSION")
        println("GL_MAX_TEXTURE_SIZE = $GL_MAX_TEXTURE_SIZE")
        println("GL info:\n$glInfo") // debug info


        if (GL_VERSION < MINIMAL_GL_VERSION || GL_MAX_TEXTURE_SIZE < MINIMAL_GL_MAX_TEXTURE_SIZE) {
            // TODO notify properly
            throw GdxRuntimeException("Graphics device not capable -- device's GL_VERSION: $GL_VERSION, required: $MINIMAL_GL_VERSION; GL_MAX_TEXTURE_SIZE: $GL_MAX_TEXTURE_SIZE, required: $MINIMAL_GL_MAX_TEXTURE_SIZE")
        }

        // resize fullscreen quad?


        TextureRegionPack.globalFlipY = true // !! TO MAKE LEGACY CODE RENDER ON ITS POSITION !!
        Gdx.graphics.isContinuousRendering = true

        batch = SpriteBatch()
        shapeRender = ShapeRenderer()



        shaderBlur = AppLoader.loadShader("assets/blur.vert", "assets/blur.frag")


        if (getConfigBoolean("fxdither")) {
            shaderBayer = AppLoader.loadShader("assets/4096.vert", "assets/4096_bayer.frag")
            shaderBayer.begin()
            shaderBayer.setUniformf("rcount", 64f)
            shaderBayer.setUniformf("gcount", 64f)
            shaderBayer.setUniformf("bcount", 64f)
            shaderBayer.end()

            shaderSkyboxFill = AppLoader.loadShader("assets/4096.vert", "assets/4096_bayer_skyboxfill.frag")
            shaderSkyboxFill.begin()
            shaderSkyboxFill.setUniformf("rcount", 64f)
            shaderSkyboxFill.setUniformf("gcount", 64f)
            shaderSkyboxFill.setUniformf("bcount", 64f)
            shaderSkyboxFill.end()
        }
        else {
            shaderBayer = AppLoader.loadShader("assets/4096.vert", "assets/passthru.frag")
            shaderSkyboxFill = AppLoader.loadShader("assets/4096.vert", "assets/skyboxfill.frag")
        }


        shaderBlendGlow = AppLoader.loadShader("assets/blendGlow.vert", "assets/blendGlow.frag")

        shaderRGBOnly = AppLoader.loadShader("assets/4096.vert", "assets/rgbonly.frag")
        shaderAtoGrey = AppLoader.loadShader("assets/4096.vert", "assets/aonly.frag")


        if (!shaderBlendGlow.isCompiled) {
            Gdx.app.log("shaderBlendGlow", shaderBlendGlow.log)
            System.exit(1)
        }


        if (getConfigBoolean("fxdither")) {
            if (!shaderBayer.isCompiled) {
                Gdx.app.log("shaderBayer", shaderBayer.log)
                System.exit(1)
            }

            if (!shaderSkyboxFill.isCompiled) {
                Gdx.app.log("shaderSkyboxFill", shaderSkyboxFill.log)
                System.exit(1)
            }
        }





        AppLoader.GAME_LOCALE = getConfigString("language")
        printdbg(this, "locale = ${AppLoader.GAME_LOCALE}")



        ModMgr // invoke Module Manager



        printdbg(this, "all modules loaded successfully")



        // jump straight into the ingame
        /*val ingame = Ingame(batch)
        ingame.gameLoadInfoPayload = Ingame.NewWorldParameters(2400, 800, HQRNG().nextLong())
        ingame.gameLoadMode = Ingame.GameLoadMode.CREATE_NEW
        LoadScreen.screenToLoad = ingame
        this.ingame = ingame
        setScreen(LoadScreen)*/



        // title screen
        AppLoader.getINSTANCE().setScreen(TitleScreen(batch))
    }

    fun setScreen(screen: Screen) {
        AppLoader.getINSTANCE().setScreen(screen)
    }

    override fun render(delta: Float) {
        AppLoader.setDebugTime("GDX.rawDelta", Gdx.graphics.rawDeltaTime.times(1000_000_000f).toLong())
        AppLoader.setDebugTime("GDX.smtDelta", Gdx.graphics.deltaTime.times(1000_000_000f).toLong())
        AppLoader.getINSTANCE().screen.render(delta)
    }

    override fun pause() {
        AppLoader.getINSTANCE().screen.pause()
    }

    override fun resume() {
        AppLoader.getINSTANCE().screen.resume()
    }

    /** Don't call this! Call AppLoader.dispose() */
    override fun dispose() {
        //dispose any other resources used in this level
        shaderBayer.dispose()
        shaderSkyboxFill.dispose()
        shaderBlur.dispose()
        shaderBlendGlow.dispose()

        ingame?.dispose()
    }

    override fun hide() {
        AppLoader.getINSTANCE().screen.hide()
    }

    /** For the actual resize, call AppLoader.resize() */
    override fun resize(width: Int, height: Int) {
        ingame?.resize(width, height)

        printdbg(this, "newsize: ${Gdx.graphics.width}x${Gdx.graphics.height} | internal: ${width}x$height")
    }



    val currentSaveDir: File
        get() {
            val file = File(defaultSaveDir + "/test")

            // failsafe?
            if (!file.exists()) file.mkdir()

            return file // TODO TEST CODE
        }

    /** Position of the cursor in the world */
    val mouseX: Double
        get() = WorldCamera.x + Gdx.input.x / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    val mouseY: Double
        get() = WorldCamera.y + Gdx.input.y / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    val oldMouseX: Double
        get() = WorldCamera.x + (Gdx.input.x - Gdx.input.deltaX) / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    val oldMouseY: Double
        get() = WorldCamera.y + (Gdx.input.y - Gdx.input.deltaY) / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    @JvmStatic val mouseTileX: Int
        get() = (mouseX / FeaturesDrawer.TILE_SIZE).floorInt()
    /** Position of the cursor in the world */
    @JvmStatic val mouseTileY: Int
        get() = (mouseY / FeaturesDrawer.TILE_SIZE).floorInt()
    /** Position of the cursor in the world */
    @JvmStatic val oldMouseTileX: Int
        get() = (oldMouseX / FeaturesDrawer.TILE_SIZE).floorInt()
    /** Position of the cursor in the world */
    @JvmStatic val oldMouseTileY: Int
        get() = (oldMouseY / FeaturesDrawer.TILE_SIZE).floorInt()
    inline val mouseScreenX: Int
        get() = Gdx.input.x
    inline val mouseScreenY: Int
        get() = Gdx.input.y
    /** Delta converted as it it was a FPS */
    inline val updateRate: Double
        get() = 1.0 / Gdx.graphics.deltaTime
    /**
     * Usage:
     *
     * override var referenceID: Int = generateUniqueReferenceID()
     */
    fun generateUniqueReferenceID(renderOrder: Actor.RenderOrder): ActorID {
        fun hasCollision(value: ActorID) =
                try {
                    Terrarum.ingame!!.theGameHasActor(value) ||
                    value < ItemCodex.ACTORID_MIN ||
                    value !in when (renderOrder) {
                        Actor.RenderOrder.BEHIND -> Actor.RANGE_BEHIND
                        Actor.RenderOrder.MIDDLE -> Actor.RANGE_MIDDLE
                        Actor.RenderOrder.MIDTOP -> Actor.RANGE_MIDTOP
                        Actor.RenderOrder.FRONT  -> Actor.RANGE_FRONT
                        Actor.RenderOrder.OVERLAY-> Actor.RANDE_OVERLAY
                    }
                }
                catch (gameNotInitialisedException: KotlinNullPointerException) {
                    false
                }

        var ret: Int
        do {
            ret = HQRNG().nextInt().and(0x7FFFFFFF) // set new ID
        } while (hasCollision(ret)) // check for collision
        return ret
    }
}

inline fun SpriteBatch.inUse(action: (SpriteBatch) -> Unit) {
    this.begin()
    action(this)
    this.end()
}

inline fun ShapeRenderer.inUse(shapeRendererType: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Filled, action: (ShapeRenderer) -> Unit) {
    this.begin(shapeRendererType)
    action(this)
    this.end()
}

/** Use Batch inside of it! */
inline fun FrameBuffer.inAction(camera: OrthographicCamera?, batch: SpriteBatch?, action: (FrameBuffer) -> Unit) {
    //this.begin()
    FrameBufferManager.begin(this)

    camera?.setToOrtho(true, this.width.toFloat(), this.height.toFloat())
    camera?.position?.set((this.width / 2f).round(), (this.height / 2f).round(), 0f) // TODO floor? ceil? round?
    camera?.update()
    batch?.projectionMatrix = camera?.combined

    action(this)

    //this.end()
    FrameBufferManager.end()

    camera?.setToOrtho(true, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())
    camera?.update()
    batch?.projectionMatrix = camera?.combined
}

fun Float.round(): Float {
    return Math.round(this).toFloat()
}


// ShapeRenderer alternative for rects
fun SpriteBatch.fillRect(x: Float, y: Float, w: Float, h: Float) {
    this.draw(AppLoader.textureWhiteSquare, x, y, w, h)
}
fun SpriteBatch.drawStraightLine(x: Float, y: Float, otherEnd: Float, thickness: Float, isVertical: Boolean) {
    if (!isVertical)
        this.fillRect(x, y, otherEnd - x, thickness)
    else
        this.fillRect(x, y, thickness, otherEnd - y)
}



infix fun Color.mul(other: Color): Color = this.cpy().mul(other)
infix fun Color.mulAndAssign(other: Color): Color {
    this.r *= other.r
    this.g *= other.g
    this.b *= other.b
    this.a *= other.a

    return this
}


fun blendMul(batch: SpriteBatch) {
    // will break if the colour image contains semitransparency
    batch.enableBlending()
    batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA)
}

fun blendScreen(batch: SpriteBatch) {
    // will break if the colour image contains semitransparency
    batch.enableBlending()
    batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR)
}

fun blendDisable(batch: SpriteBatch) {
    batch.disableBlending()
}

fun blendNormal(batch: SpriteBatch) {
    batch.enableBlending()
    batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_SRC_ALPHA, GL20.GL_ONE)

    // ALPHA *MUST BE* PREMULTIPLIED //

    // One way to tell:
    //  1. Check (RGB) and (A) values.
    //  2. If there exist a pixel such that max(R,G,B) > (A), then the image is NOT premultiplied.
    // Easy way:
    //  Base game (mods/basegame/blocks/terrain.tga.gz) has impure window glass. When looking at the RGB channel only:
    //      premultipied     if the glass looks very dark.
    //      not premultipied if the glass looks VERY GREEN.

    // helpful links:
    // - https://gamedev.stackexchange.com/questions/82741/normal-blend-mode-with-opengl-trouble
    // - https://www.andersriggelsen.dk/glblendfunc.php
}

fun gdxClearAndSetBlend(r: Float, g: Float, b: Float, a: Float) {
    Gdx.gl.glClearColor(r,g,b,a)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    gdxSetBlend()
}

fun gdxSetBlend() {
    Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
    Gdx.gl.glEnable(GL20.GL_BLEND)
}

fun gdxSetBlendNormal() {
    gdxSetBlend()
    Gdx.gl.glBlendFuncSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_SRC_ALPHA, GL20.GL_ONE)
    //Gdx.gl.glBlendEquationSeparate(GL20.GL_FUNC_ADD, GL30.GL_MAX) // batch.flush does not touch blend equation

    // ALPHA *MUST BE* PREMULTIPLIED //

    // One way to tell:
    //  1. Check (RGB) and (A) values.
    //  2. If there exist a pixel such that max(R,G,B) > (A), then the image is NOT premultiplied.
    // Easy way:
    //  Base game (mods/basegame/blocks/terrain.tga.gz) has impure window glass. When looking at the RGB channel only:
    //      premultipied     if the glass looks very dark.
    //      not premultipied if the glass looks VERY GREEN.

    // helpful links:
    // - https://gamedev.stackexchange.com/questions/82741/normal-blend-mode-with-opengl-trouble
    // - https://www.andersriggelsen.dk/glblendfunc.php
}

object BlendMode {
    const val SCREEN   = "screen"
    const val MULTIPLY = "multiply"
    const val NORMAL   = "normal"
    //const val MAX      = "GL_MAX" // not supported by GLES -- use shader

    fun resolve(mode: String, batch: SpriteBatch) {
        when (mode) {
            SCREEN   -> blendScreen(batch)
            MULTIPLY -> blendMul(batch)
            NORMAL   -> blendNormal(batch)
            //MAX      -> blendLightenOnly() // not supported by GLES -- use shader
            else     -> throw Error("Unknown blend mode: $mode")
        }
    }
}

enum class RunningEnvironment {
    PC, CONSOLE//, MOBILE
}

infix fun Color.screen(other: Color) = Color(
        1f - (1f - this.r) * (1f - other.r),
        1f - (1f - this.g) * (1f - other.g),
        1f - (1f - this.b) * (1f - other.b),
        1f - (1f - this.a) * (1f - other.a)
)

infix fun Color.minus(other: Color) = Color( // don't turn into an operator!
        this.r - other.r,
        this.g - other.g,
        this.b - other.b,
        this.a - other.a
)

fun Int.toHex() = this.toLong().and(0xFFFFFFFF).toString(16).padStart(8, '0').toUpperCase()

fun MutableList<Any>.shuffle() {
    for (i in this.size - 1 downTo 1) {
        val rndIndex = (Math.random() * (i + 1)).toInt()

        val t = this[rndIndex]
        this[rndIndex] = this[i]
        this[i] = t
    }
}


val ccW = GameFontBase.toColorCode(0xFFFF)
val ccY = GameFontBase.toColorCode(0xFE8F)
val ccO = GameFontBase.toColorCode(0xFB2F)
val ccR = GameFontBase.toColorCode(0xF88F)
val ccF = GameFontBase.toColorCode(0xFAEF)
val ccM = GameFontBase.toColorCode(0xEAFF)
val ccB = GameFontBase.toColorCode(0x88FF)
val ccC = GameFontBase.toColorCode(0x8FFF)
val ccG = GameFontBase.toColorCode(0x8F8F)
val ccV = GameFontBase.toColorCode(0x080F)
val ccX = GameFontBase.toColorCode(0x853F)
val ccK = GameFontBase.toColorCode(0x888F)


typealias Second = Float

fun Int.sqr(): Int = this * this
fun Double.floorInt() = Math.floor(this).toInt()
fun Float.floorInt() = FastMath.floor(this)
fun Float.floor() = FastMath.floor(this).toFloat()
fun Double.ceilInt() = Math.ceil(this).toInt()
fun Float.ceil(): Float = FastMath.ceil(this).toFloat()
fun Float.ceilInt() = FastMath.ceil(this)
fun Double.round() = Math.round(this).toDouble()
fun Double.floor() = Math.floor(this)
fun Double.ceil() = this.floor() + 1.0
fun Double.roundInt(): Int = Math.round(this).toInt()
fun Float.roundInt(): Int = Math.round(this)
fun Double.abs() = Math.abs(this)
fun Double.sqr() = this * this
fun Float.sqr() = this * this
fun Double.sqrt() = Math.sqrt(this)
fun Float.sqrt() = FastMath.sqrt(this)
fun Int.abs() = this.absoluteValue
fun Double.bipolarClamp(limit: Double) =
        this.coerceIn(-limit, limit)

fun absMax(left: Double, right: Double): Double {
    if (left > 0 && right > 0)
        if (left > right) return left
        else return right
    else if (left < 0 && right < 0)
        if (left < right) return left
        else return right
    else {
        val absL = left.abs()
        val absR = right.abs()
        if (absL > absR) return left
        else return right
    }
}

fun Double.magnSqr() = if (this >= 0.0) this.sqr() else -this.sqr()
fun Double.sign() = if (this > 0.0) 1.0 else if (this < 0.0) -1.0 else 0.0
fun interpolateLinear(scale: Double, startValue: Double, endValue: Double): Double {
    if (startValue == endValue) {
        return startValue
    }
    if (scale <= 0.0) {
        return startValue
    }
    if (scale >= 1.0) {
        return endValue
    }
    return (1.0 - scale) * startValue + scale * endValue
}

fun <T> List<T>.linearSearch(selector: (T) -> Boolean): Int? {
    this.forEachIndexed { index, it ->
        if (selector.invoke(it)) return index
    }

    return null
}
fun <T> List<T>.linearSearchBy(selector: (T) -> Boolean): T? {
    this.forEach {
        if (selector.invoke(it)) return it
    }

    return null
}
