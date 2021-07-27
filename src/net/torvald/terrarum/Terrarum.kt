package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.UnsafeHelper
import net.torvald.random.HQRNG
import net.torvald.terrarum.AppLoader.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.GameFontBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.util.CircularArray
import java.io.File
import java.io.PrintStream
import kotlin.math.absoluteValue
import kotlin.math.round



typealias RGBA8888 = Int


/**
 * Slick2d Version Created by minjaesong on 2015-12-30.
 *
 * LibGDX Version Created by minjaesong on 2017-06-15.
 */
object Terrarum : Disposable {

    /**
     * All singleplayer "Player" must have this exact reference ID.
     */
    const val PLAYER_REF_ID: Int = 0x91A7E2

    inline fun inShapeRenderer(shapeRendererType: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Filled, action: (ShapeRenderer) -> Unit) {
        shapeRender.begin(shapeRendererType)
        action(shapeRender)
        shapeRender.end()
    }


    //////////////////////////////
    // GLOBAL IMMUTABLE CONFIGS //
    //////////////////////////////

    /**
     * To be used with physics simulator. This is a magic number.
     */
    const val PHYS_TIME_FRAME: Double = 26.0 + (2.0 / 3.0)
    // 26.0 + (2.0 / 3.0) // lower value == faster gravity response (IT WON'T HOTSWAP!!)
    // protip: using METER, game unit and SI unit will have same number



    var previousScreen: Screen? = null // to be used with temporary states like StateMonitorCheck


    /** Current ingame instance the game is holding.
     *
     *  The ingame instance this variable is subject to change.
     *
     *  Don't do:
     *  ```
     *  private val ingame = Terrarum.ingame
     *  ```
     *
     *  Do instead:
     *  ```
     *  private val ingame: IngameInstance
     *          get() = Terrarum.ingame
     *  ```
     */
    var ingame: IngameInstance? = null
        private set

    private val javaHeapCircularArray = CircularArray<Int>(64, true)
    private val nativeHeapCircularArray = CircularArray<Int>(64, true)
    private val updateRateCircularArray = CircularArray<Double>(16, true)

    val memJavaHeap: Int
        get() {
            javaHeapCircularArray.appendHead((Gdx.app.javaHeap shr 20).toInt())

            var acc = 0
            javaHeapCircularArray.forEach { acc = maxOf(acc, it) }
            return acc
        }
    val memNativeHeap: Int
        get() {
            nativeHeapCircularArray.appendHead((Gdx.app.nativeHeap shr 20).toInt())

            var acc = 0
            nativeHeapCircularArray.forEach { acc = maxOf(acc, it) }
            return acc
        }
    val memUnsafe: Int
        get() {
            return (UnsafeHelper.unsafeAllocatedSize shr 20).toInt()
        }
    val memXmx: Int
        get() = (Runtime.getRuntime().maxMemory() shr 20).toInt()
    val updateRateStr: String
        get() {
            updateRateCircularArray.appendHead(updateRate)

            var acc = 0.0
            updateRateCircularArray.forEach { acc = maxOf(acc, it) }
            return String.format("%.2f", acc)
        }

    lateinit var testTexture: Texture

    init {
        println("[Terrarum] init called by:")
        printStackTrace(this)

        println("[Terrarum] ${AppLoader.GAME_NAME} version ${AppLoader.getVERSION_STRING()}")
        println("[Terrarum] LibGDX version ${com.badlogic.gdx.Version.VERSION}")


        println("[Terrarum] os.arch = $systemArch") // debug info

        if (is32BitJVM) {
            printdbgerr(this, "32 Bit JVM detected")
        }


        println("[Terrarum] processor = $processor")
        println("[Terrarum] vendor = $processorVendor")


        AppLoader.disposableSingletonsPool.add(this)



        println("[Terrarum] init complete")
    }


    @JvmStatic fun initialise() {
        // dummy init method
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
    /*val GL_MAX_TEXTURE_SIZE: Int
        get() {
            val intBuffer = BufferUtils.createIntBuffer(16) // size must be at least 16, or else LWJGL complains
            Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, intBuffer)

            intBuffer.rewind()

            return intBuffer.get()
        }
    val MINIMAL_GL_MAX_TEXTURE_SIZE = 4096*/

    fun setCurrentIngameInstance(ingame: IngameInstance) {
        this.ingame = ingame

        printdbg(this, "Accepting new ingame instance '${ingame.javaClass.canonicalName}', called by:")
        printStackTrace(this)
    }

    private fun showxxx() {

        testTexture = Texture(Gdx.files.internal("./assets/test_texture.tga"))


        // resize fullscreen quad?


        TextureRegionPack.globalFlipY = true // !! TO MAKE LEGACY CODE RENDER ON ITS POSITION !!
        Gdx.graphics.isContinuousRendering = true

        //batch = SpriteBatch()
        //shapeRender = ShapeRenderer()


        AppLoader.GAME_LOCALE = getConfigString("language")
        printdbg(this, "locale = ${AppLoader.GAME_LOCALE}")




        // jump straight into the ingame
        /*val ingame = Ingame(batch)
        ingame.gameLoadInfoPayload = Ingame.NewWorldParameters(2400, 800, HQRNG().nextLong())
        ingame.gameLoadMode = Ingame.GameLoadMode.CREATE_NEW
        LoadScreen.screenToLoad = ingame
        this.ingame = ingame
        setScreen(LoadScreen)*/



        // title screen
        AppLoader.setScreen(TitleScreen(batch))
    }

    /** Don't call this! Call AppLoader.dispose() */
    override fun dispose() {
        //dispose any other resources used in this level
        ingame?.dispose()
    }


    /** For the actual resize, call AppLoader.resize() */
    /*fun resize(width: Int, height: Int) {
        ingame?.resize(width, height)

        printdbg(this, "newsize: ${Gdx.graphics.width}x${Gdx.graphics.height} | internal: ${width}x$height")
    }*/



    val currentSaveDir: File
        get() {
            val file = File(defaultSaveDir + "/test")

            // failsafe?
            if (!file.exists()) file.mkdir()

            return file // TODO TEST CODE
        }

    /** Position of the cursor in the world */
    val mouseX: Double
        get() = WorldCamera.zoomedX + Gdx.input.x / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    val mouseY: Double
        get() = WorldCamera.zoomedY + Gdx.input.y / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    val oldMouseX: Double
        get() = WorldCamera.zoomedX + (Gdx.input.x - Gdx.input.deltaX) / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    val oldMouseY: Double
        get() = WorldCamera.zoomedY + (Gdx.input.y - Gdx.input.deltaY) / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    @JvmStatic val mouseTileX: Int
        get() = (mouseX / TILE_SIZE).floorInt()
    /** Position of the cursor in the world */
    @JvmStatic val mouseTileY: Int
        get() = (mouseY / TILE_SIZE).floorInt()
    /** Position of the cursor in the world */
    @JvmStatic val oldMouseTileX: Int
        get() = (oldMouseX / TILE_SIZE).floorInt()
    /** Position of the cursor in the world */
    @JvmStatic val oldMouseTileY: Int
        get() = (oldMouseY / TILE_SIZE).floorInt()
    inline val mouseScreenX: Int
        get() = Gdx.input.x
    inline val mouseScreenY: Int
        get() = Gdx.input.y
    inline val mouseDeltaX: Int
        get() = Gdx.input.deltaX
    inline val mouseDeltaY: Int
        get() = Gdx.input.deltaY
    /** Delta converted as it it was a FPS */
    inline val updateRate: Double
        get() = 1.0 / Gdx.graphics.rawDeltaTime
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
            ret = HQRNG().nextInt().and(0x7FFFFF00) // set new ID
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

    camera?.setToOrtho(true, AppLoader.screenSize.screenWf, AppLoader.screenSize.screenHf)
    camera?.update()
    batch?.projectionMatrix = camera?.combined
}


// ShapeRenderer alternative for rects
fun SpriteBatch.fillRect(x: Float, y: Float, w: Float, h: Float) {
    this.draw(AppLoader.textureWhiteSquare, x, y, w, h)
}
fun SpriteBatch.fillCircle(x: Float, y: Float, w: Float, h: Float) {
    this.draw(AppLoader.textureWhiteCircle, x, y, w, h)
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

fun gdxClearAndSetBlend(color: Color) {
    gdxClearAndSetBlend(color.r, color.g, color.b, color.a)
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
fun Float.round(): Float = round(this)
fun Double.round() = Math.round(this).toDouble()
fun Double.floor() = Math.floor(this)
fun Double.ceil() = this.floor() + 1.0
fun Double.abs() = Math.abs(this)
fun Double.sqr() = this * this
fun Float.sqr() = this * this
fun Double.sqrt() = Math.sqrt(this)
fun Float.sqrt() = FastMath.sqrt(this)
fun Int.abs() = this.absoluteValue
fun Double.bipolarClamp(limit: Double) =
        this.coerceIn(-limit, limit)
fun Boolean.toInt() = if (this) 1 else 0


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

inline fun printStackTrace(obj: Any) = printStackTrace(obj, System.out) // because of Java

fun printStackTrace(obj: Any, out: PrintStream = System.out) {
    if (AppLoader.IS_DEVELOPMENT_BUILD) {
        Thread.currentThread().stackTrace.forEach {
            out.println("[${obj.javaClass.simpleName}] ... $it")
        }
    }
}

class UIContainer {
    private val data = ArrayList<Any>()
    fun add(vararg things: Any) {
        things.forEach {
            if (it is UICanvas || it is Id_UICanvasNullable)
                data.add(it)
            else throw IllegalArgumentException(it.javaClass.name) }
    }

    fun iterator() = object : Iterator<UICanvas?> {
        private var cursor = 0

        override fun hasNext() = cursor < data.size

        override fun next(): UICanvas? {
            val it = data[cursor]
            // whatever the fucking reason when() does not work
            if (it is UICanvas) {
                cursor += 1
                return it
            }
            else if (it is Id_UICanvasNullable) {
                cursor += 1
                return it.get()
            }
            else throw IllegalArgumentException("Unacceptable type ${it.javaClass.name}, instance of ${it.javaClass.superclass.name}")
        }
    }

    fun forEach(operation: (UICanvas?) -> Unit) = iterator().forEach(operation)
    fun countVisible(): Int {
        var c = 0
        forEach { if (it?.isVisible == true) c += 1 }
        return c
    }

    fun contains(element: Any) = data.contains(element)

    fun <T> map(transformation: (UICanvas?) -> T) = iterator().asSequence().map(transformation)
}

interface Id_UICanvasNullable {
    fun get(): UICanvas?
}