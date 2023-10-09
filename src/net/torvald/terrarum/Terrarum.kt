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
import com.badlogic.gdx.utils.GdxRuntimeException
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.terrarum.App.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.FluidCodex
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.faction.FactionCodex
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.itemproperties.CraftingCodex
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap
import net.torvald.unsafe.UnsafeHelper
import net.torvald.util.CircularArray
import org.dyn4j.geometry.Vector2
import java.io.File
import java.io.PrintStream
import java.util.*
import kotlin.math.*


typealias RGBA8888 = Int


/**
 * Slick2d Version Created by minjaesong on 2015-12-30.
 *
 * LibGDX Version Created by minjaesong on 2017-06-15.
 *
 * Note: Blocks are NOT automatically added; they must be registered manually on the module's EntryPoint
 */
object Terrarum : Disposable {

    init {
    }

    /**
     * All singleplayer "Player" must have this exact reference ID.
     */
    const val PLAYER_REF_ID: Int = 0x91A7E2

    /*inline fun inShapeRenderer(shapeRendererType: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Filled, action: (ShapeRenderer) -> Unit) {
        shapeRender.begin(shapeRendererType)
        action(shapeRender)
        shapeRender.end()
    }*/


    var blockCodex = BlockCodex(); internal set
    /** The actual contents of the ItemCodex is sum of Player's Codex and the World's Codex */
    var itemCodex = ItemCodex(); internal set
    var wireCodex = WireCodex(); internal set
    var materialCodex = MaterialCodex(); internal set
    var factionCodex = FactionCodex(); internal set
    var craftingCodex = CraftingCodex(); internal set
    var apocryphas = HashMap<String, Any>(); internal set
    var fluidCodex = FluidCodex(); internal set


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
    private val updateRateCircularArray = CircularArray<Double>(64, true)

    val memJavaHeap: Int
        get() {
            javaHeapCircularArray.appendHead((Gdx.app.javaHeap shr 20).toInt())
            return javaHeapCircularArray.maxOrNull() ?: 0
        }
    /*val memNativeHeap: Int // as long as you're sticking to the LWJGL, nativeHeap just returns javaHeap
        get() {
            nativeHeapCircularArray.appendHead((Gdx.app.nativeHeap shr 20).toInt())
            return nativeHeapCircularArray.maxOrNull() ?: 0
        }*/
    val memUnsafe: Int
        get() = (UnsafeHelper.unsafeAllocatedSize shr 20).toInt()
    val memXmx: Int
        get() = (Runtime.getRuntime().maxMemory() shr 20).toInt()
    val updateRateStr: String
        get() {
            updateRateCircularArray.appendHead(updateRate)
            return String.format("%.2f", updateRateCircularArray.average())
        }

    lateinit var testTexture: Texture

    init {
        println("[Terrarum] init called by:")
        printStackTrace(this)

        println("[Terrarum] ${App.GAME_NAME} version ${App.getVERSION_STRING()}")
        println("[Terrarum] LibGDX version ${com.badlogic.gdx.Version.VERSION}")


        println("[Terrarum] os.arch = $systemArch") // debug info

        if (is32BitJVM) {
            printdbgerr(this, "32 Bit JVM detected")
        }


        println("[Terrarum] processor = $processor")
        println("[Terrarum] vendor = $processorVendor")


        App.disposables.add(this)



        println("[Terrarum] init complete")
    }


    @JvmStatic fun initialise() {
        // dummy init method
    }


    val RENDER_FPS = getConfigInt("displayfps")
    val USE_VSYNC = getConfigBoolean("usevsync")
    val VSYNC_TRIGGER_THRESHOLD = 56
    val GL_VERSION: Int
        get() = Gdx.graphics.glVersion.majorVersion * 100 +
                Gdx.graphics.glVersion.minorVersion * 10 +
                Gdx.graphics.glVersion.releaseVersion
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

        printdbg("ListSavegames", "Accepting new ingame instance '${ingame.javaClass.canonicalName}', called by:")
        printStackTrace(this)
    }

    /** Don't call this! Call AppLoader.dispose() */
    override fun dispose() {
        //dispose any other resources used in this level
        ingame?.dispose()
    }


    /** For the actual resize, call AppLoader.resize() */
    /*fun resize(width: Int, height: Int) {
        ingame?.resize(width, height)

        printdbg("ListSavegames", "newsize: ${Gdx.graphics.width}x${Gdx.graphics.height} | internal: ${width}x$height")
    }*/



    val currentSaveDir: File
        get() {
            val file = File(saveDir + "/test")

            // failsafe?
            if (!file.exists()) file.mkdir()

            return file // TODO TEST CODE
        }

    /** Position of the cursor in the world, rounded */
    val mouseX: Double
        get() = (WorldCamera.zoomedX + Gdx.input.x / (ingame?.screenZoom ?: 1f).times(scr.magn.toDouble())).fmod(WorldCamera.worldWidth.toDouble())
    /** Position of the cursor in the world */
    val mouseY: Double
        get() = (WorldCamera.zoomedY + Gdx.input.y / (ingame?.screenZoom ?: 1f).times(scr.magn.toDouble()))
    /** Position of the cursor in the world, rounded */
    val oldMouseX: Double
        get() = (WorldCamera.zoomedX + (Gdx.input.x - Gdx.input.deltaX) / (ingame?.screenZoom ?: 1f).times(scr.magn.toDouble())).fmod(WorldCamera.worldWidth.toDouble())
    /** Position of the cursor in the world */
    val oldMouseY: Double
        get() = WorldCamera.zoomedY + (Gdx.input.y - Gdx.input.deltaY) / (ingame?.screenZoom ?: 1f).times(scr.magn.toDouble())
    /** Position of the cursor in the world, rounded */
    @JvmStatic val mouseTileX: Int
        get() = (mouseX / TILE_SIZE).floorToInt()
    /** Position of the cursor in the world */
    @JvmStatic val mouseTileY: Int
        get() = (mouseY / TILE_SIZE).floorToInt()
    /** Position of the cursor in the world, rounded */
    @JvmStatic val oldMouseTileX: Int
        get() = (oldMouseX / TILE_SIZE).floorToInt()
    /** Position of the cursor in the world */
    @JvmStatic val oldMouseTileY: Int
        get() = (oldMouseY / TILE_SIZE).floorToInt()
    inline val mouseScreenX: Int
        get() = Gdx.input.x.div(scr.magn).roundToInt()
    inline val mouseScreenY: Int
        get() = Gdx.input.y.div(scr.magn).roundToInt()
    inline val mouseDeltaX: Int
        get() = Gdx.input.deltaX.div(scr.magn).roundToInt()
    inline val mouseDeltaY: Int
        get() = Gdx.input.deltaY.div(scr.magn).roundToInt()
    /** Delta converted as it it was a FPS */
    inline val updateRate: Double
        get() = 1.0 / Gdx.graphics.deltaTime
    val mouseDown: Boolean
        get() = Gdx.input.isButtonPressed(App.getConfigInt("config_mouseprimary"))


    /**
     * Subtile and Vector indices:
     * ```
     * 4px  8px
     * +-+-----+-+
     * | |  4  | |
     * +-+-----+-+
     * |3|  0  |1|
     * +-+-----+-+
     * | |  2  | |
     * +-+-----+-+
     * ```
     */

    enum class SubtileVector {
        INVALID, CENTRE, LEFT, BOTTOM, RIGHT, TOP
    }

    fun SubtileVector.toInt() = when (this) {
        SubtileVector.INVALID -> throw IllegalArgumentException()
        SubtileVector.RIGHT -> 1
        SubtileVector.BOTTOM -> 2
        SubtileVector.LEFT -> 4
        SubtileVector.TOP    -> 8
        SubtileVector.CENTRE -> 0
    }

    data class MouseSubtile4(val x: Int, val y: Int, val vector: SubtileVector) {

        val nx = when (vector) {
            SubtileVector.CENTRE -> x
            SubtileVector.RIGHT  -> x + 1
            SubtileVector.BOTTOM -> x
            SubtileVector.LEFT   -> x - 1
            SubtileVector.TOP    -> x
            else -> x
        }

        val ny = when (vector) {
            SubtileVector.CENTRE -> y
            SubtileVector.RIGHT  -> y
            SubtileVector.BOTTOM -> y + 1
            SubtileVector.LEFT   -> y
            SubtileVector.TOP    -> y - 1
            else -> y
        }

        val currentTileCoord = x to y
        val nextTileCoord = nx to ny
    }

    fun getMouseSubtile4(): MouseSubtile4 {
        val SMALLGAP = 4.0
        val LARGEGAP = 8.0 // 2*SMALLGAP + LARGEGAP must be equal to TILE_SIZE
        assert(2 * SMALLGAP + LARGEGAP == TILE_SIZED)

        val mx = mouseX
        val my = mouseY
        val mtx = (mouseX / TILE_SIZE).floorToInt()
        val mty = (mouseY / TILE_SIZE).floorToInt()
        val msx = mx fmod TILE_SIZED
        val msy = my fmod TILE_SIZED
        val vector = if (msx < SMALLGAP) { // X to the left
            if (msy < SMALLGAP) SubtileVector.INVALID // TOP LEFT
            else if (msy < SMALLGAP + LARGEGAP) SubtileVector.LEFT // LEFT
            else SubtileVector.INVALID // BOTTOM LEFT
        }
        else if (msx < SMALLGAP + LARGEGAP) { // X to the centre
            if (msy < SMALLGAP) SubtileVector.TOP // TOP
            else if (msy < SMALLGAP + LARGEGAP) SubtileVector.CENTRE // CENTRE
            else SubtileVector.BOTTOM // BOTTOM
        }
        else { // X to the right
            if (msy < SMALLGAP) SubtileVector.INVALID // TOP RIGHT
            else if (msy < SMALLGAP + LARGEGAP) SubtileVector.RIGHT // RIGHT
            else SubtileVector.INVALID // BOTTOM RIGHT
        }

        return MouseSubtile4(mtx, mty, vector)
    }

    /**
     * Usage:
     *
     * override var referenceID: Int = generateUniqueReferenceID()
     */
    fun generateUniqueReferenceID(renderOrder: Actor.RenderOrder): ActorID {
        // render orders can be changed arbitrarily so the whole "renderorder to actor id" is only there for an initial sorting
        fun hasCollision(value: ActorID) =
                try {
                    Terrarum.ingame?.theGameHasActor(value) == true
                }
                catch (gameNotInitialisedException: KotlinNullPointerException) {
                    false
                }

        var ret: Int
        do {
            val range = ReferencingRanges.ACTORS
            val size = range.last - range.first + 1
            ret = (HQRNG().nextInt().rem(size) + range.first) and 0x7FFF_FF00 // make room for sub-actors
        } while (hasCollision(ret)) // check for collision
        return ret
    }



    fun getWorldSaveFiledesc(filename: String) = File(App.worldsDir, filename)
    fun getPlayerSaveFiledesc(filename: String) = File(App.playersDir, filename)
    fun getSharedSaveFiledesc(filename: String) = File(App.saveSharedDir, filename)

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

    val oldCamPos = camera?.position?.cpy()

    camera?.setToOrtho(true, this.width.toFloat(), this.height.toFloat())
    camera?.position?.set((this.width / 2f).roundToFloat(), (this.height / 2f).roundToFloat(), 0f) // TODO floor? ceil? round?
    camera?.update()
    batch?.projectionMatrix = camera?.combined

    action(this)

    //this.end()
    FrameBufferManager.end()

    camera?.setToOrtho(true, App.scr.wf, App.scr.hf)
    camera?.position?.set(oldCamPos)
    camera?.update()
    batch?.projectionMatrix = camera?.combined
}

/**
 * Vertically flipped version of [FrameBuffer.inAction]
 */
inline fun FrameBuffer.inActionF(camera: OrthographicCamera?, batch: SpriteBatch?, action: (FrameBuffer) -> Unit) {
    //this.begin()
    FrameBufferManager.begin(this)

    val oldCamPos = camera?.position?.cpy()

    camera?.setToOrtho(false, this.width.toFloat(), this.height.toFloat())
    camera?.position?.set((this.width / 2f).roundToFloat(), (this.height / 2f).roundToFloat(), 0f) // TODO floor? ceil? round?
    camera?.update()
    batch?.projectionMatrix = camera?.combined

    action(this)

    //this.end()
    FrameBufferManager.end()

    camera?.setToOrtho(true, App.scr.wf, App.scr.hf)
    camera?.position?.set(oldCamPos)
    camera?.update()
    batch?.projectionMatrix = camera?.combined
}


private val rgbMultLUT = Array(256) { y -> IntArray(256) { x ->
    val i = x / 255f
    val j = y / 255f
    (i * j).times(255f).roundToInt()
} }

infix fun Int.rgbamul(other: Int): Int {
    val r = rgbMultLUT[this.ushr(24).and(255)][other.ushr(24).and(255)]
    val g = rgbMultLUT[this.ushr(16).and(255)][other.ushr(16).and(255)]
    val b = rgbMultLUT[this.ushr(8).and(255)][other.ushr(8).and(255)]
    val a = rgbMultLUT[this.ushr(0).and(255)][other.ushr(0).and(255)]
    return r.shl(24) or g.shl(16) or b.shl(8) or a
}

infix fun Color.mul(other: Color): Color = this.cpy().mul(other)
infix fun Color.mulAndAssign(other: Color): Color {
    this.r *= other.r
    this.g *= other.g
    this.b *= other.b
    this.a *= other.a

    return this
}

/**
 * Use demultiplier shader on GL Source (foreground) if source has semitransparency
 */
fun blendMul(batch: SpriteBatch) {
    batch.enableBlending()
    batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA)
}

/**
 * Use demultiplier shader on GL Source (foreground) if source has semitransparency
 */
fun blendScreen(batch: SpriteBatch) {
    // will break if the colour image contains semitransparency
    batch.enableBlending()
    batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR)
}

fun blendDisable(batch: SpriteBatch) {
    batch.disableBlending()
}

fun blendNormalStraightAlpha(batch: SpriteBatch) {
    batch.enableBlending()
    batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
    // helpful links:
    // - https://stackoverflow.com/questions/19674740/opengl-es2-premultiplied-vs-straight-alpha-blending#37869033
    // - https://gamedev.stackexchange.com/questions/82741/normal-blend-mode-with-opengl-trouble
    // - https://www.andersriggelsen.dk/glblendfunc.php
    // - https://stackoverflow.com/questions/45781683/how-to-get-correct-sourceover-alpha-compositing-in-sdl-with-opengl
}
fun blendNormalPremultAlpha(batch: SpriteBatch) {
    batch.enableBlending()
    batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
    // helpful links:
    // - https://stackoverflow.com/questions/19674740/opengl-es2-premultiplied-vs-straight-alpha-blending#37869033
    // - https://gamedev.stackexchange.com/questions/82741/normal-blend-mode-with-opengl-trouble
    // - https://www.andersriggelsen.dk/glblendfunc.php
    // - https://stackoverflow.com/questions/45781683/how-to-get-correct-sourceover-alpha-compositing-in-sdl-with-opengl
}

fun gdxClearAndEnableBlend(color: Color) {
    gdxClearAndEnableBlend(color.r, color.g, color.b, color.a)
}

fun gdxClearAndEnableBlend(r: Float, g: Float, b: Float, a: Float) {
    Gdx.gl.glClearColor(r,g,b,a)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    gdxEnableBlend()
}

fun gdxEnableBlend() {
    Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
    Gdx.gl.glEnable(GL20.GL_BLEND)
}

fun gdxBlendNormalStraightAlpha() {
    gdxEnableBlend()
    Gdx.gl.glBlendFuncSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
    // helpful links:
    // - https://stackoverflow.com/questions/19674740/opengl-es2-premultiplied-vs-straight-alpha-blending#37869033
    // - https://gamedev.stackexchange.com/questions/82741/normal-blend-mode-with-opengl-trouble
    // - https://www.andersriggelsen.dk/glblendfunc.php
    // - https://stackoverflow.com/questions/45781683/how-to-get-correct-sourceover-alpha-compositing-in-sdl-with-opengl
}

fun gdxBlendNormalPremultAlpha() {
    gdxEnableBlend()
    Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
    // helpful links:
    // - https://stackoverflow.com/questions/19674740/opengl-es2-premultiplied-vs-straight-alpha-blending#37869033
    // - https://gamedev.stackexchange.com/questions/82741/normal-blend-mode-with-opengl-trouble
    // - https://www.andersriggelsen.dk/glblendfunc.php
    // - https://stackoverflow.com/questions/45781683/how-to-get-correct-sourceover-alpha-compositing-in-sdl-with-opengl
}

fun gdxBlendMul() {
    gdxEnableBlend()
    Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA)
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
            NORMAL   -> blendNormalStraightAlpha(batch)
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


val ccW = TerrarumSansBitmap.toColorCode(0xFFFF)
val ccY = TerrarumSansBitmap.toColorCode(0xFFE8)
val ccO = TerrarumSansBitmap.toColorCode(0xFFB2)
val ccR = TerrarumSansBitmap.toColorCode(0xFF88)
val ccF = TerrarumSansBitmap.toColorCode(0xFFAE)
val ccM = TerrarumSansBitmap.toColorCode(0xFEAF)
val ccB = TerrarumSansBitmap.toColorCode(0xF6CF)
val ccC = TerrarumSansBitmap.toColorCode(0xF8FF)
val ccG = TerrarumSansBitmap.toColorCode(0xF8F8)
val ccV = TerrarumSansBitmap.toColorCode(0xF080)
val ccX = TerrarumSansBitmap.toColorCode(0xF853)
val ccK = TerrarumSansBitmap.toColorCode(0xF888)
val ccE = TerrarumSansBitmap.toColorCode(0xFBBB)

// Zelda-esque text colour emphasis
val emphRed = TerrarumSansBitmap.toColorCode(0xFF88)
val emphObj = TerrarumSansBitmap.toColorCode(0xF0FF)
val emphVerb = TerrarumSansBitmap.toColorCode(0xFFF6)


typealias Second = Float

inline fun Double.floorToInt() = floor(this).toInt()
inline fun Float.floorToInt() = FastMath.floor(this)
inline fun Double.ceilToInt() = Math.ceil(this).toInt()
inline fun Float.ceilToFloat(): Float = FastMath.ceil(this).toFloat()
inline fun Float.ceilToInt() = FastMath.ceil(this)
inline fun Float.floorToFloat() = FastMath.floor(this).toFloat()
inline fun Float.roundToFloat(): Float = round(this)
//inline fun Double.round() = Math.round(this).toDouble()
inline fun Double.floorToDouble() = floor(this)
inline fun Double.ceilToDouble() = ceil(this)
inline fun Int.sqr(): Int = this * this
inline fun Double.sqr() = this * this
inline fun Float.sqr() = this * this
inline fun Double.sqrt() = Math.sqrt(this)
inline fun Float.sqrt() = FastMath.sqrt(this)
inline fun Int.abs() = this.absoluteValue
inline fun Double.abs() = this.absoluteValue
inline fun Double.bipolarClamp(limit: Double) = this.coerceIn(-limit, limit)
inline fun Boolean.toInt(shift: Int = 0) = if (this) 1.shl(shift) else 0
inline fun Boolean.toLong(shift: Int = 0) = if (this) 1L.shl(shift) else 0L
inline fun Int.bitCount() = java.lang.Integer.bitCount(this)
inline fun Long.bitCount() = java.lang.Long.bitCount(this)


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
    if (App.IS_DEVELOPMENT_BUILD) {
        val indentation = " ".repeat(obj.javaClass.simpleName.length + 4)
        Thread.currentThread().stackTrace.forEachIndexed { index, it ->
            if (index == 1)
                out.println("[${obj.javaClass.simpleName}]> $it")
            else if (index > 1)
                out.println("$indentation$it")
        }
    }
}

class UIContainer {
    private val data = ArrayList<Any>()
    fun add(vararg things: Any) {
        things.forEach {
            if (it is UICanvas || it is Id_UICanvasNullable)
                if (!data.contains(it)) data.add(it)
            else throw IllegalArgumentException(it.javaClass.name)
        }
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

    fun filter(predicate: (Any) -> Boolean) = data.filter(predicate)
}

interface Id_UICanvasNullable {
    fun get(): UICanvas?
}

// haskell-inspired array selectors
// head and last use first() and last()
fun <T> Array<T>.tail() = this.sliceArray(1 until this.size)
fun <T> Array<T>.init() = this.sliceArray(0 until this.lastIndex)
fun <T> List<T>.tail() = this.subList(1, this.size)
fun <T> List<T>.init() = this.subList(0, this.lastIndex)

fun <T> Collection<T>.notEmptyOrNull() = this.ifEmpty { null }

val BlockCodex: BlockCodex
    get() = Terrarum.blockCodex
val ItemCodex: ItemCodex
    get() = Terrarum.itemCodex
val WireCodex: WireCodex
    get() = Terrarum.wireCodex
val MaterialCodex: MaterialCodex
    get() = Terrarum.materialCodex
val FactionCodex: FactionCodex
    get() = Terrarum.factionCodex
val CraftingRecipeCodex: CraftingCodex
    get() = Terrarum.craftingCodex
val Apocryphas: HashMap<String, Any>
    get() = Terrarum.apocryphas
val FluidCodex: FluidCodex
    get() = Terrarum.fluidCodex

class Codex : KVHashMap() {

    fun getAsCvec(key: String): Cvec? {
        val value = get(key)

        if (value == null) return null

        return value as Cvec
    }

}

fun AppUpdateListOfSavegames() {
    App.sortedSavegameWorlds.clear()
    App.savegameWorlds.clear()
    App.savegameWorldsName.clear()
    App.sortedPlayers.clear()
    App.savegamePlayers.clear()
    App.savegamePlayersName.clear()


    // create list of worlds
    printdbg("ListSavegames", "Listing saved worlds...")
    val worldsDirLs = File(worldsDir).listFiles().filter { !it.isDirectory && !it.name.contains('.') }.mapNotNull { file ->
        try {
            DiskSkimmer(file, true)
        }
        catch (e: Throwable) {
            System.err.println("Unable to load a world file ${file.absolutePath}")
            e.printStackTrace()
            null
        }
    }.sortedByDescending { it.getLastModifiedTime() }
    val filteringResults = arrayListOf<List<DiskSkimmer>>() // first element of the list is always file with no suffix
    worldsDirLs.forEach {
        val li = arrayListOf(it)
        listOf(".1",".2",".3",".a",".b",".c").forEach { suffix ->
            val file = File(it.diskFile.absolutePath + suffix)
            try {
                val d = DiskSkimmer(file, true)
                li.add(d)
            }
            catch (e: Throwable) {}
        }
        filteringResults.add(li)
    }
    filteringResults.forEachIndexed { index, list ->
        val it = list.first()
        printdbg("ListSavegames", " ${index+1}.\t${it.diskFile.absolutePath}")

        printdbg("ListSavegames", "    collecting...")
        val collection = SavegameCollection.collectFromBaseFilename(list, it.diskFile.name)
        printdbg("ListSavegames", "    disk rebuilding...")
        collection.rebuildLoadable()
        printdbg("ListSavegames", "    get UUID...")
        val worldUUID = collection.getUUID()

        printdbg("ListSavegames", "    registration...")
        // if multiple valid savegames with same UUID exist, only the most recent one is retained
        if (!App.savegameWorlds.contains(worldUUID)) {
            App.savegameWorlds[worldUUID] = collection
            App.sortedSavegameWorlds.add(worldUUID)
            App.savegameWorldsName[worldUUID] = it.getDiskName(Common.CHARSET)
        }
    }


    // create list of players
    printdbg("ListSavegames", "Listing saved players...")
    val playersDirLs = File(playersDir).listFiles().filter { !it.isDirectory && !it.name.contains('.') }.mapNotNull { file ->
        try {
            DiskSkimmer(file, true)
        }
        catch (e: Throwable) {
            System.err.println("Unable to load a world file ${file.absolutePath}")
            e.printStackTrace()
            null
        }
    }.sortedByDescending { it.getLastModifiedTime() }
    val filteringResults2 = arrayListOf<List<DiskSkimmer>>()
    playersDirLs.forEach {
        val li = arrayListOf(it)
        listOf(".1",".2",".3",".a",".b",".c").forEach { suffix ->
            val file = File(it.diskFile.absolutePath + suffix)
            try {
                val d = DiskSkimmer(file, true)
                li.add(d)
            }
            catch (e: Throwable) {}
        }
        filteringResults2.add(li)
    }
    filteringResults2.forEachIndexed { index, list ->
        val it = list.first()
        printdbg("ListSavegames", " ${index+1}.\t${it.diskFile.absolutePath}")

        printdbg("ListSavegames", "    collecting...")
        val collection = SavegameCollection.collectFromBaseFilename(list, it.diskFile.name)
        printdbg("ListSavegames", "    disk rebuilding...")
        collection.rebuildLoadable()
        printdbg("ListSavegames", "    get UUID...")
        val playerUUID = collection.getUUID()

        printdbg("ListSavegames", "    registration...")
        // if multiple valid savegames with same UUID exist, only the most recent one is retained
        if (!App.savegamePlayers.contains(playerUUID)) {
            App.savegamePlayers[playerUUID] = collection
            App.sortedPlayers.add(playerUUID)
            App.savegamePlayersName[playerUUID] = it.getDiskName(Common.CHARSET)
        }
    }

    /*println("SortedPlayers...")
    App.sortedPlayers.forEach {
        println(it)
    }*/

}

/**
 * @param skimmer loaded with the savefile, rebuilt/updated beforehand
 */
fun checkForSavegameDamage(skimmer: DiskSkimmer): Boolean {
    try {
        // # check for meta
        /*val metaFile = skimmer.requestFile(-1) ?: return true
        // # check if The Player is there
        val player = skimmer.requestFile(PLAYER_REF_ID.toLong().and(0xFFFFFFFFL))?.contents ?: return true
        // # check if:
        //      the world The Player is at actually exists
        //      all the actors for the world actually exists
        // TODO SAX parser for JSON -- use JsonReader().parse(<String>) for now...
        val currentWorld = (player as EntryFile).bytes.let {
            (ReadActor.readActorBare(ByteArray64Reader(it, Common.CHARSET)) as? IngamePlayer
             ?: return true).worldCurrentlyPlaying
        }
        if (currentWorld == 0) return true
        val worldData = (skimmer.requestFile(currentWorld.toLong())?.contents as? EntryFile)?.bytes ?: return true
        val world = Common.jsoner.fromJson(GameWorld::class.java, ByteArray64Reader(worldData, Common.CHARSET))

        var hasMissingActor = false
        world.actors.forEach {
            if (!skimmer.hasEntry(it.toLong().and(0xFFFFFFFFL))) {
                System.err.println("Nonexisting actor $it for savegame ${skimmer.diskFile.absolutePath}")
                hasMissingActor = true
            }
        }; if (hasMissingActor) return true*/


        return false
    }
    catch (e: Throwable) {
        e.printStackTrace()
        return true
    }
}

/**
 * No lateinit!
 */
inline fun Disposable.tryDispose() {
    try { this.dispose() }
    catch (_: Throwable) {}
}

fun distBetweenActors(a: ActorWithBody, b: ActorWithBody): Double {
    val ww = INGAME.world.width * TILE_SIZED
    val apos1 = a.centrePosVector
    val apos2 = Vector2(apos1.x + ww, apos1.y)
    val apos3 = Vector2(apos1.x - ww, apos1.y)
    val bpos = b.centrePosVector
    val dist = min(min(bpos.distanceSquared(apos1), bpos.distanceSquared(apos2)), bpos.distanceSquared(apos3))
    return dist.sqrt()
}
fun distBetween(a: ActorWithBody, bpos: Vector2): Double {
    val ww = INGAME.world.width * TILE_SIZED
    val apos1 = a.centrePosVector
    val apos2 = Vector2(apos1.x + ww, apos1.y)
    val apos3 = Vector2(apos1.x - ww, apos1.y)
    val dist = min(min(bpos.distanceSquared(apos1), bpos.distanceSquared(apos2)), bpos.distanceSquared(apos3))
    return dist.sqrt()
}

fun getHashStr(length: Int = 5) = (0 until length).map { "YBNDRFG8EJKMCPQXOTLVWIS2A345H769"[Math.random().times(32).toInt()] }.joinToString("")
