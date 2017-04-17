package net.torvald.terrarum

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.torvald.imagefont.GameFontImpl
import net.torvald.JsonFetcher
import net.torvald.JsonWriter
import net.torvald.imagefont.TinyAlphNum
import net.torvald.terrarum.gameworld.toUint
import org.lwjgl.input.Controllers
import org.lwjgl.opengl.*
import org.newdawn.slick.*
import org.newdawn.slick.opengl.Texture
import org.newdawn.slick.state.StateBasedGame
import java.io.File
import java.io.IOException
import java.lang.management.ManagementFactory
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

const val GAME_NAME = "Terrarum"

typealias Millisec = Int

/**
 * Created by minjaesong on 15-12-30.
 */
object Terrarum : StateBasedGame(GAME_NAME) {

    //////////////////////////////
    // GLOBAL IMMUTABLE CONFIGS //
    //////////////////////////////
    var WIDTH = 1072
    var HEIGHT = 742 // IMAX ratio

    var VSYNC = true
    val VSYNC_TRIGGER_THRESHOLD = 56

    val HALFW: Int
        get() = WIDTH.ushr(1)
    val HALFH: Int
        get() = HEIGHT.ushr(1)

    val QUICKSLOT_MAX = 10

    /**
     * To be used with physics simulator
     */
    val TARGET_FPS = 50

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


    lateinit var appgc: AppGameContainer

    var ingame: StateInGame? = null
    private val gameConfig = GameConfig()

    val OSName = System.getProperty("os.name")
    val OSVersion = System.getProperty("os.version")
    lateinit var OperationSystem: String // all caps "WINDOWS, "OSX", "LINUX", "SOLARIS", "UNKNOWN"
        private set
    val isWin81: Boolean
        get() = OperationSystem == "WINDOWS" && OSVersion.toDouble() >= 8.1
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

            (fontGame as GameFontImpl).reload()
        }

    private val nullFont = object : Font {
        override fun getHeight(str: String?) = 0
        override fun drawString(x: Float, y: Float, text: String?) {}
        override fun drawString(x: Float, y: Float, text: String?, col: Color?) {}
        override fun drawString(x: Float, y: Float, text: String?, col: Color?, startIndex: Int, endIndex: Int) {}
        override fun getWidth(str: String?) = 0
        override fun getLineHeight() = 0
    }

    var fontGame: Font = nullFont
        private set
    var fontSmallNumbers: Font = nullFont
        private set

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
    const val VERSION_RAW = 0x0002018E
    const val VERSION_STRING: String =
            "${VERSION_RAW.ushr(24)}.${VERSION_RAW.and(0xFF0000).ushr(16)}.${VERSION_RAW.and(0xFFFF)}"
    const val NAME = "Terrarum"

    var UPDATE_DELTA: Int = 0

    // these properties goes into the GameContainer

    var previousState: Int? = null // to be used with temporary states like StateMonitorCheck

    val systemArch = System.getProperty("os.arch")

    private val thirtyTwoBitArchs = arrayOf("i386", "i686", "ppc", "x86", "x86_32") // I know I should Write Once, Run Everywhere; but just in case :)
    val is32Bit = thirtyTwoBitArchs.contains(systemArch)

    lateinit var textureWhite: Image
    lateinit var textureBlack: Image

    init {

        // just in case
        println("[Terrarum] os.arch = $systemArch")

        if (is32Bit) {
            println("Java is running in 32 Bit")
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
            Controllers.getController(0)
            if (getConfigString("pcgamepadenv") == "console")
                RunningEnvironment.CONSOLE
            else
                RunningEnvironment.PC
        }
        catch (e: IndexOutOfBoundsException) {
            RunningEnvironment.PC
        }
    }

    @Throws(SlickException::class)
    override fun initStatesList(gc: GameContainer) {
        textureWhite = Image("./assets/graphics/background_white.png")
        textureBlack = Image("./assets/graphics/background_black.png")


        fontGame = GameFontImpl()
        fontSmallNumbers = TinyAlphNum()


        gc.input.enableKeyRepeat()


        // get locale from config
        val gameLocaleFromConfig = gameConfig.getAsString("language") ?: sysLang

        // if bad game locale were set, use system locale
        if (gameLocaleFromConfig.length < 2)
            gameLocale = sysLang
        else
            gameLocale = gameLocaleFromConfig

        println("[Terrarum] Locale: " + gameLocale)



        // search for real controller
        // exclude controllers with name "Mouse", "keyboard"
        val notControllerRegex = Regex("mouse|keyboard")
        try {
            // gc.input.controllerCount is unreliable
            for (i in 0..255) {
                val controllerInQuo = Controllers.getController(i)

                println("Controller $i: ${controllerInQuo.name}")

                // check the name
                if (!controllerInQuo.name.toLowerCase().contains(notControllerRegex)) {
                    controller = controllerInQuo
                    println("Controller $i selected: ${controller!!.name}")
                    break
                }
            }


            // test acquired controller
            controller!!.getAxisValue(0)
        }
        catch (controllerDoesNotHaveAnyAxesException: java.lang.ArrayIndexOutOfBoundsException) {
            controller = null
        }

        if (controller != null) {
            for (c in 0..controller!!.axisCount - 1) {
                controller!!.setDeadZone(c, CONTROLLER_DEADZONE)
            }
        }


        // load modules
        ModuleManager


        gc.graphics.clear() // clean up any 'dust' in the buffer

        //addState(StateVTTest())
        //addState(StateGraphicComputerTest())
        //addState(StateTestingLightning())
        //addState(StateSplash())
        //addState(StateMonitorCheck())
        //addState(StateFontTester())
        //addState(StateNoiseTexGen())
        //addState(StateBlurTest())
        //addState(StateShaderTest())
        //addState(StateNoiseTester())
        //addState(StateUITest())
        //addState(StateControllerRumbleTest())
        //addState(StateMidiInputTest())
        //addState(StateNewRunesTest())

        ingame = StateInGame(); addState(ingame)


        // foolproof
        if (stateCount < 1) {
            throw Error("Please add or un-comment addState statements")
        }
    }

    private fun getDefaultDirectory() {
        val OS = System.getProperty("os.name").toUpperCase()
        if (OS.contains("WIN")) {
            OperationSystem = "WINDOWS"
            defaultDir = System.getenv("APPDATA") + "/terrarum"
        }
        else if (OS.contains("OS X")) {
            OperationSystem = "OSX"
            defaultDir = System.getProperty("user.home") + "/Library/Application Support/terrarum"
        }
        else if (OS.contains("NUX") || OS.contains("NIX")) {
            OperationSystem = "LINUX"
            defaultDir = System.getProperty("user.home") + "/.terrarum"
        }
        else if (OS.contains("SUNOS")) {
            OperationSystem = "SOLARIS"
            defaultDir = System.getProperty("user.home") + "/.terrarum"
        }
        else {
            OperationSystem = "UNKNOWN"
            defaultDir = System.getProperty("user.home") + "/.terrarum"
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

    @Throws(IOException::class)
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
}

fun main(args: Array<String>) {
    System.setProperty("java.library.path", "lib")
    System.setProperty("org.lwjgl.librarypath", File("lib").absolutePath)

    try {
        Terrarum.appgc = AppGameContainer(Terrarum)
        Terrarum.appgc.setDisplayMode(Terrarum.WIDTH, Terrarum.HEIGHT, false)

        Terrarum.appgc.setTargetFrameRate(Terrarum.TARGET_INTERNAL_FPS)
        Terrarum.appgc.setVSync(Terrarum.VSYNC)
        Terrarum.appgc.setMaximumLogicUpdateInterval(1000 / Terrarum.TARGET_INTERNAL_FPS) // 10 ms
        Terrarum.appgc.setMinimumLogicUpdateInterval(1000 / Terrarum.TARGET_INTERNAL_FPS - 1) // 9 ms

        Terrarum.appgc.setMultiSample(0)

        Terrarum.appgc.setShowFPS(false)

        // game will run normally even if it is not focused
        Terrarum.appgc.setUpdateOnlyWhenVisible(false)
        Terrarum.appgc.alwaysRender = true

        Terrarum.appgc.start()
    }
    catch (ex: Exception) {
        val logger = Logger.getLogger(Terrarum::class.java.name)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss")
        val calendar = Calendar.getInstance()
        val filepath = "${Terrarum.defaultDir}/crashlog-${dateFormat.format(calendar.time)}.txt"
        val fileHandler = FileHandler(filepath)
        logger.addHandler(fileHandler)

        val formatter = SimpleFormatter()
        fileHandler.formatter = formatter

        //logger.info()
        println("The game has crashed!")
        println("Crash log were saved to $filepath.")
        println("================================================================================")
        logger.log(Level.SEVERE, null, ex)
    }
}

///////////////////////////////////
// customised blending functions //
///////////////////////////////////

fun blendMul() {
    // I must say: What the fuck is wrong with you, Slick2D? Your built-it blending is just fucking wrong.
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glColorMask(true, true, true, true)
    GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA)
}

fun blendNormal() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glColorMask(true, true, true, true)
    //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

    // semitransparent textures working as intended with this,
    // but needs further investigation in the case of:
    // TODO test blend in the situation of semitransparent over semitransparent
    GL14.glBlendFuncSeparate(
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, // blend func for RGB channels
            GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA // blend func for alpha channels
    )
}

fun blendLightenOnly() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glColorMask(true, true, true, false)
    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE)
    GL14.glBlendEquation(GL14.GL_MAX)
}

fun blendAlphaMap() {
    GL11.glDisable(GL11.GL_BLEND)
    GL11.glColorMask(false, false, false, true)
}

fun blendScreen() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glColorMask(true, true, true, true)
    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR)
}

fun blendDisable() {
    GL11.glDisable(GL11.GL_BLEND)
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

/** @return Intarray(R, G, B, A) */
fun Texture.getPixel(x: Int, y: Int): IntArray {
    val textureWidth = this.textureWidth
    val hasAlpha = this.hasAlpha()

    val offset = (if (hasAlpha) 4 else 3) * (textureWidth * y + x) // 4: # of channels (RGBA)

    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
        return intArrayOf(
                this.textureData[offset].toUint(),
                this.textureData[offset + 1].toUint(),
                this.textureData[offset + 2].toUint(),
                if (hasAlpha)
                    this.textureData[offset + 3].toUint()
                else 255
        )
    }
    else {
        return intArrayOf(
                this.textureData[offset + 2].toUint(),
                this.textureData[offset + 1].toUint(),
                this.textureData[offset].toUint(),
                if (hasAlpha)
                    this.textureData[offset + 3].toUint()
                else 255
        )
    }
}

/** @return Intarray(R, G, B, A) */
fun Image.getPixel(x: Int, y: Int) = this.texture.getPixel(x, y)

fun Color.toInt() = redByte.shl(16) or greenByte.shl(8) or blueByte
fun Color.to10bit() = redByte.shl(20) or greenByte.shl(10) or blueByte

infix fun Color.screen(other: Color) = Color(
        1f - (1f - this.r) * (1f - other.r),
        1f - (1f - this.g) * (1f - other.g),
        1f - (1f - this.b) * (1f - other.b),
        1f - (1f - this.a) * (1f - other.a)
)
infix fun Color.mul(other: Color) = Color( // don't turn into an operator!
        this.r * other.r,
        this.g * other.g,
        this.b * other.b,
        this.a * other.a
)
infix fun Color.minus(other: Color) = Color( // don't turn into an operator!
    this.r - other.r,
    this.g - other.g,
    this.b - other.b,
    this.a - other.a
)

fun Int.toHex() = this.toLong().and(0xFFFFFFFF).toString(16).padStart(8, '0').toUpperCase()
fun Long.toHex() = {
    val sb = StringBuilder()
    (0..16).forEach {

    }
}


