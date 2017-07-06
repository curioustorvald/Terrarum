package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.loaders.ShaderProgramLoader
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.CpuSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.jme3.math.FastMath
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.ActorWithPhysics.Companion.TILE_SIZE
import net.torvald.terrarum.gameactors.floor
import net.torvald.terrarum.gamecontroller.GameController
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.JsonWriter
import net.torvald.terrarum.worlddrawer.RGB10
import net.torvald.terrarumsansbitmap.gdx.GameFontBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.lwjgl.input.Controllers
import org.lwjgl.opengl.GL11
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Created by minjaesong on 2017-06-15.
 */

const val GAME_NAME = "Terrarum"

fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.foregroundFPS = TerrarumGDX.RENDER_FPS
    config.backgroundFPS = TerrarumGDX.RENDER_FPS
    //config.vSyncEnabled = true
    config.resizable = true
    config.width = 1072
    config.height = 742
    config.backgroundFPS = 9999
    config.foregroundFPS = 9999
    config.title = GAME_NAME

    LwjglApplication(TerrarumGDX, config)
}



typealias RGBA8888 = Int

object TerrarumGDX : ApplicationAdapter() {

    lateinit var batch: SpriteBatch
    lateinit var shapeRender: ShapeRenderer // DO NOT USE!! for very limited applications e.g. WeatherMixer
    inline fun inShapeRenderer(shapeRendererType: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Filled, action: (ShapeRenderer) -> Unit) {
        shapeRender.begin(shapeRendererType)
        action(shapeRender)
        shapeRender.end()
    }


    lateinit var orthoLineTex2px: Texture
    lateinit var orthoLineTex3px: Texture


    //////////////////////////////
    // GLOBAL IMMUTABLE CONFIGS //
    //////////////////////////////

    val RENDER_FPS = getConfigInt("displayfps")
    val USE_VSYNC = getConfigBoolean("usevsync")
    var VSYNC = USE_VSYNC
    val VSYNC_TRIGGER_THRESHOLD = 56


    inline val WIDTH: Int
        get() = Gdx.graphics.width//if (Gdx.graphics.width % 1 == 1) Gdx.graphics.width + 1 else Gdx.graphics.width
    inline val HEIGHT: Int
        get() = Gdx.graphics.height//if (Gdx.graphics.height % 1 == 1) Gdx.graphics.height + 1 else Gdx.graphics.height

    inline val HALFW: Int
        get() = WIDTH.ushr(1)
    inline val HALFH: Int
        get() = HEIGHT.ushr(1)

    /**
     * To be used with physics simulator
     */
    val TARGET_FPS = 33.333333333333333333333

    /**
     * To be used with render, to achieve smooth frame drawing

     * TARGET_INTERNAL_FPS > TARGET_FPS for smooth frame drawing

     * Must choose a value so that (1000 / VAL) is still integer
     */
    val TARGET_INTERNAL_FPS = 100


    /**
     * For the events depends on rendering frame (e.g. flicker on post-hit invincibility)
     */
    var GLOBAL_RENDER_TIMER = Random().nextInt(1020) + 1






    val sysLang: String
        get() {
            val lan = System.getProperty("user.language")
            val country = System.getProperty("user.country")
            return lan + country
        }

    lateinit var currentScreen: Screen
    var previousScreen: Screen? = null // to be used with temporary states like StateMonitorCheck


    var ingame: StateInGameGDX? = null
    private val gameConfig = GameConfig()

    val OSName = System.getProperty("os.name")
    val OSVersion = System.getProperty("os.version")
    lateinit var OperationSystem: String // all caps "WINDOWS, "OSX", "LINUX", "SOLARIS", "UNKNOWN"
        private set
    lateinit var defaultDir: String
        private set
    lateinit var defaultSaveDir: String
        private set

    val memInUse: Long
        get() = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) shr 20
    val memTotal: Long
        get() = Runtime.getRuntime().totalMemory() shr 20
    val memXmx: Long
        get() = Runtime.getRuntime().maxMemory() shr 20

    val environment: RunningEnvironment

    private val localeSimple = arrayOf("de", "en", "es", "it")
    var gameLocale = "lateinit"
        set(value) {
            if (localeSimple.contains(value.substring(0..1)))
                field = value.substring(0..1)
            else
                field = value

            fontGame.reload(value)
        }



    lateinit var fontGame: GameFontBase
    lateinit var fontSmallNumbers: BitmapFont

    var joypadLabelStart: Char = 0xE000.toChar() // lateinit
    var joypadLableSelect: Char = 0xE000.toChar() // lateinit
    var joypadLabelNinA: Char = 0xE000.toChar() // lateinit TODO
    var joypadLabelNinB: Char = 0xE000.toChar() // lateinit TODO
    var joypadLabelNinX: Char = 0xE000.toChar() // lateinit TODO
    var joypadLabelNinY: Char = 0xE000.toChar() // lateinit TODO
    var joypadLabelNinL: Char = 0xE000.toChar() // lateinit TODO
    var joypadLabelNinR: Char = 0xE000.toChar() // lateinit TODO
    var joypadLabelNinZL: Char = 0xE000.toChar() // lateinit TODO
    var joypadLabelNinZR: Char = 0xE000.toChar() // lateinit TODO
    val joypadLabelLEFT = 0xE068.toChar()
    val joypadLabelDOWN = 0xE069.toChar()
    val joypadLabelUP = 0xE06A.toChar()
    val joypadLabelRIGHT = 0xE06B.toChar()

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

    var controller: org.lwjgl.input.Controller? = null
        private set
    val CONTROLLER_DEADZONE = 0.1f

    /** Available CPU threads */
    val THREADS = Runtime.getRuntime().availableProcessors()

    /**
     * If the game is multithreading.
     * True if:
     *
     *     THREADS >= 2 and config "multithread" is true
     */
    val MULTITHREAD: Boolean
        get() = THREADS >= 2 && getConfigBoolean("multithread")

    private lateinit var configDir: String

    /**
     * 0xAA_BB_XXXX
     * AA: Major version
     * BB: Minor version
     * XXXX: Revision (Repository commits)
     *
     * e.g. 0x02010034 can be translated as 2.1.52
     */
    const val VERSION_RAW = 0x00_02_018E
    const val VERSION_STRING: String =
            "${VERSION_RAW.ushr(24)}.${VERSION_RAW.and(0xFF0000).ushr(16)}.${VERSION_RAW.and(0xFFFF)}"
    const val NAME = "Terrarum"


    val systemArch = System.getProperty("os.arch")

    val is32BitJVM = !System.getProperty("sun.arch.data.model").contains("64")


    lateinit var shaderBlur: ShaderProgram
    lateinit var shader4096: ShaderProgram
    lateinit var shader4096Bayer: ShaderProgram


    init {
        println("[Terrarum] os.arch = $systemArch") // debug info

        if (is32BitJVM) {
            println("32 Bit JVM detected")
        }

        joypadLabelStart = when (getConfigString("joypadlabelstyle")) {
            "nwii"     -> 0xE04B.toChar() // + mark
            "logitech" -> 0xE05A.toChar() // number 10
            else       -> 0xE042.toChar() // |> mark (sonyps, msxb360, generic)
        }
        joypadLableSelect = when (getConfigString("joypadlabelstyle")) {
            "nwii"     -> 0xE04D.toChar() // - mark
            "logitech" -> 0xE059.toChar() // number 9
            "sonyps"   -> 0xE043.toChar() // solid rectangle
            "msxb360"  -> 0xE041.toChar() // <| mark
            else       -> 0xE043.toChar() // solid rectangle
        }



        getDefaultDirectory()
        createDirs()

        val readFromDisk = readConfigJson()
        if (!readFromDisk) readConfigJson()

        environment = try {
            Controllers.getController(0) // test if controller exists
            if (getConfigString("pcgamepadenv") == "console")
                RunningEnvironment.CONSOLE
            else
                RunningEnvironment.PC
        }
        catch (e: IndexOutOfBoundsException) {
            RunningEnvironment.PC
        }
    }

    override fun create() {
        TextureRegionPack.globalFlipY = true // !! TO MAKE LEGACY CODE RENDER ON ITS POSITION !!
        Gdx.graphics.isContinuousRendering = true

        batch = SpriteBatch()
        shapeRender = ShapeRenderer()

        orthoLineTex2px = Texture("assets/graphics/ortho_line_tex_2px.tga")
        orthoLineTex3px = Texture("assets/graphics/ortho_line_tex_3px.tga")


        fontGame = GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap", flipY = true)
        fontSmallNumbers = TinyAlphNum


        ShaderProgram.pedantic = false
        shaderBlur = ShaderProgram(Gdx.files.internal("assets/blur.vert"), Gdx.files.internal("assets/blur.frag"))
        shader4096 = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096.frag"))
        shader4096Bayer = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096_bayer.frag"))

        shader4096Bayer.begin()
        shader4096Bayer.setUniformMatrix("Bayer", Matrix4(floatArrayOf(0f,8f,2f,10f,12f,4f,14f,6f,3f,11f,1f,9f,15f,7f,13f,5f)))
        shader4096Bayer.setUniformf("monitorGamma", 2.2f)
        shader4096Bayer.end()


        ModMgr // invoke Module Manager, will also invoke BlockCodex
        ItemCodex // invoke Item Codex




        ingame = StateInGameGDX(batch)
        currentScreen = ingame as Screen
        ingame!!.enter()

    }

    override fun render() {
        currentScreen.render(Gdx.graphics.deltaTime)
        GLOBAL_RENDER_TIMER += 1
    }

    override fun pause() {
        currentScreen.pause()
    }

    override fun resume() {
        currentScreen.resume()
    }

    override fun dispose() {
        currentScreen.dispose()
        fontGame.dispose()
        fontSmallNumbers.dispose()
        //dispose any other resources used in this level
    }

    override fun resize(width: Int, height: Int) {
        currentScreen.resize(width, height)
    }




    private fun getDefaultDirectory() {
        val OS = System.getProperty("os.name").toUpperCase()
        if (OS.contains("WIN")) {
            OperationSystem = "WINDOWS"
            defaultDir = System.getenv("APPDATA") + "/Terrarum"
        }
        else if (OS.contains("OS X")) {
            OperationSystem = "OSX"
            defaultDir = System.getProperty("user.home") + "/Library/Application Support/Terrarum"
        }
        else if (OS.contains("NUX") || OS.contains("NIX")) {
            OperationSystem = "LINUX"
            defaultDir = System.getProperty("user.home") + "/.Terrarum"
        }
        else if (OS.contains("SUNOS")) {
            OperationSystem = "SOLARIS"
            defaultDir = System.getProperty("user.home") + "/.Terrarum"
        }
        else {
            OperationSystem = "UNKNOWN"
            defaultDir = System.getProperty("user.home") + "/.Terrarum"
        }

        defaultSaveDir = defaultDir + "/Saves"
        configDir = defaultDir + "/config.json"

        println("[Terrarum] os.name = $OSName")
        println("[Terrarum] os.version = $OSVersion")
    }

    private fun createDirs() {
        val dirs = arrayOf(File(defaultSaveDir))
        dirs.forEach { if (!it.exists()) it.mkdirs() }
    }

    private fun createConfigJson() {
        val configFile = File(configDir)

        if (!configFile.exists() || configFile.length() == 0L) {
            JsonWriter.writeToFile(DefaultConfig.fetch(), configDir)
        }
    }

    private fun readConfigJson(): Boolean {
        try {
            // read from disk and build config from it
            val jsonObject = JsonFetcher(configDir)

            // make config
            jsonObject.entrySet().forEach { entry -> gameConfig[entry.key] = entry.value }

            return true
        }
        catch (e: IOException) {
            // write default config to game dir. Call this method again to read config from it.
            try {
                createConfigJson()
            }
            catch (e1: IOException) {
                e.printStackTrace()
            }

            return false
        }

    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    fun getConfigInt(key: String): Int {
        val cfg = getConfigMaster(key)
        if (cfg is JsonPrimitive)
            return cfg.asInt
        else
            return cfg as Int
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    fun getConfigString(key: String): String {
        val cfg = getConfigMaster(key)
        if (cfg is JsonPrimitive)
            return cfg.asString
        else
            return cfg as String
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    fun getConfigBoolean(key: String): Boolean {
        val cfg = getConfigMaster(key)
        if (cfg is JsonPrimitive)
            return cfg.asBoolean
        else
            return cfg as Boolean
    }

    fun getConfigIntArray(key: String): IntArray {
        val cfg = getConfigMaster(key)
        if (cfg is JsonArray) {
            val jsonArray = cfg.asJsonArray
            return IntArray(jsonArray.size(), { i -> jsonArray[i].asInt })
        }
        else
            return cfg as IntArray
    }

    private fun getConfigMaster(key: String): Any {
        var cfg: Any? = null
        try {
            cfg = gameConfig[key.toLowerCase()]!!
        }
        catch (e: NullPointerException) {
            try {
                cfg = DefaultConfig.fetch()[key.toLowerCase()]
            }
            catch (e1: NullPointerException) {
                e.printStackTrace()
            }
        }
        return cfg!!
    }

    fun setConfig(key: String, value: Any) {
        gameConfig[key] = value
    }

    val currentSaveDir: File
        get() {
            val file = File(defaultSaveDir + "/test")

            // failsafe?
            if (!file.exists()) file.mkdir()

            return file // TODO TEST CODE
        }

    inline val mouseX: Double
        get() = GameController.mouseX.toDouble()
    inline val mouseY: Double
        get() = GameController.mouseY.toDouble()
    @JvmStatic inline val mouseTileX: Int
        get() = GameController.mouseTileX
    @JvmStatic inline val mouseTileY: Int
        get() = GameController.mouseTileY
    inline val mouseScreenX: Int
        get() = Gdx.input.x
    inline val mouseScreenY: Int
        get() = Gdx.input.y




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
    this.begin()
    camera?.setToOrtho(true, this.width.toFloat(), this.height.toFloat())
    camera?.position?.set((this.width / 2f).round(), (this.height / 2f).round(), 0f) // TODO floor? ceil? round?
    camera?.update()
    batch?.projectionMatrix = camera?.combined
    action(this)
    this.end()
}

fun Float.round(): Float {
    return Math.round(this).toFloat()
}


// ShapeRenderer alternative for rects
fun SpriteBatch.fillRect(x: Float, y: Float, w: Float, h: Float) {
    this.draw(net.torvald.terrarum.worlddrawer.BlocksDrawer.tilesTerrain.get(1, 0), x, y, w, h)
}
inline fun SpriteBatch.drawStraightLine(x: Float, y: Float, p2: Float, thickness: Float, isVertical: Boolean) {
    if (!isVertical)
        this.fillRect(x, y, p2 - x, thickness)
    else
        this.fillRect(x, y, thickness, p2 - y)
}



infix fun Color.mul(other: Color): Color = this.cpy().mul(other)



inline fun Color.toRGB10(): RGB10 {
    val bits = this.toIntBits() // ABGR
    // 0bxxRRRRRRRRRRGGGGGGGGGGBBBBBBBBBB
    // 0bAAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
    return bits.and(0x0000FF).shl(20) or bits.and(0x00FF00).shl(2) or bits.and(0xFF0000).ushr(16)
}



fun blendMul() {
    // I must say: What the fuck is wrong with you, Slick2D? Your built-it blending is just fucking wrong.
    TerrarumGDX.batch.enableBlending()
    TerrarumGDX.batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

fun blendNormal() {
    TerrarumGDX.batch.enableBlending()
    TerrarumGDX.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

fun blendLightenOnly() {
    TerrarumGDX.batch.enableBlending()
    TerrarumGDX.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE)
    Gdx.gl.glBlendEquation(GL30.GL_MAX) // batch.flush does not touch blend equation
}

fun blendScreen() {
    TerrarumGDX.batch.enableBlending()
    TerrarumGDX.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

object BlendMode {
    const val SCREEN   = "GL_BLEND screen"
    const val MULTIPLY = "GL_BLEND multiply"
    const val NORMAL   = "GL_BLEND normal"
    const val MAX      = "GL_MAX"

    fun resolve(mode: String) {
        when (mode) {
            SCREEN   -> blendScreen()
            MULTIPLY -> blendMul()
            NORMAL   -> blendNormal()
            MAX      -> blendLightenOnly()
            else     -> throw Error("Unknown blend mode: $mode")
        }
    }
}

enum class RunningEnvironment {
    PC, CONSOLE, MOBILE
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


