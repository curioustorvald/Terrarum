package net.torvald.terrarum

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.torvald.imagefont.GameFontWhite
import net.torvald.JsonFetcher
import net.torvald.JsonWriter
import net.torvald.imagefont.TinyAlphNum
import org.lwjgl.input.Controllers
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.newdawn.slick.*
import org.newdawn.slick.state.StateBasedGame
import java.io.File
import java.io.IOException
import java.lang.management.ManagementFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

/**
 * Created by minjaesong on 15-12-30.
 */
class Terrarum @Throws(SlickException::class)
constructor(gamename: String) : StateBasedGame(gamename) {

    // these properties goes into the GameContainer

    var previousState: Int? = null // to be used with temporary states like StateMonitorCheck

    init {

        gameConfig = GameConfig()

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

        // get locale from config
        val gameLocaleFromConfig = gameConfig.getAsString("language") ?: sysLang

        // if bad game locale were set, use system locale
        if (gameLocaleFromConfig.length < 2)
            gameLocale = sysLang
        else
            gameLocale = gameLocaleFromConfig

        println("[terrarum] Locale: " + gameLocale)

        try {
            Controllers.getController(0)
            environment = if (getConfigString("pcgamepadenv") == "console")
                RunningEnvironment.CONSOLE
            else
                RunningEnvironment.PC
        }
        catch (e: IndexOutOfBoundsException) {
            environment = RunningEnvironment.PC
        }
    }

    @Throws(SlickException::class)
    override fun initStatesList(gc: GameContainer) {
        gc.input.enableKeyRepeat()

        fontGame = GameFontWhite()
        fontSmallNumbers = TinyAlphNum()

        try {
            hasController = gc.input.controllerCount > 0

            if (hasController) {
                // check if the first controller is actually available
                Controllers.getController(0).getAxisValue(0)
            }
        }
        catch (e: ArrayIndexOutOfBoundsException) {
            hasController = false
        }

        if (hasController) {
            for (c in 0..Controllers.getController(0).axisCount - 1) {
                Controllers.getController(0).setDeadZone(c, CONTROLLER_DEADZONE)
            }
        }

        gc.graphics.clear() // clean up any 'dust' in the buffer

        //addState(StateVTTest())
        //addState(StateTestingSandbox())
        //addState(StateSplash())
        //addState(StateMonitorCheck())
        //addState(StateFontTester())
        addState(StateNoiseTexGen())

        //ingame = StateInGame()
        //addState(ingame)
    }

    companion object {

        val sysLang: String
            get() {
                val lan = System.getProperty("user.language")
                val country = System.getProperty("user.country")
                return lan + country
            }

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

        lateinit var appgc: AppGameContainer

        var WIDTH =  1072
        var HEIGHT = 742 // IMAX ratiso
        var VSYNC = true
        val VSYNC_TRIGGER_THRESHOLD = 56

        var gameStarted = false

        lateinit var ingame: StateInGame
        lateinit var gameConfig: GameConfig

        lateinit var OSName: String // System.getProperty("os.name")
            private set
        lateinit var OSVersion: String // System.getProperty("os.version")
            private set
        lateinit var OperationSystem: String // all caps "WINDOWS, "OSX", "LINUX", "SOLARIS", "UNKNOWN"
            private set
        val isWin81: Boolean
            get() = OperationSystem == "WINDOWS" && OSVersion.toDouble() >= 8.1
        lateinit var defaultDir: String
            private set
        lateinit var defaultSaveDir: String
            private set

        val memInUse: Long
            get() = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
        val totalVMMem: Long
            get() = Runtime.getRuntime().maxMemory() shr 20

        lateinit var environment: RunningEnvironment

        private val localeSimple = arrayOf("de", "en", "es", "it")
        var gameLocale = "####" // lateinit placeholder
            set(value) {
                if (localeSimple.contains(value.substring(0..1)))
                    field = value.substring(0..1)
                else
                    field = value
            }

        lateinit var fontGame: Font
            private set
        lateinit var fontSmallNumbers: Font
            private set

        var joypadLabelStart: Char = 0x00.toChar() // lateinit
        var joypadLableSelect:Char = 0x00.toChar() // lateinit
        var joypadLabelNinA:  Char = 0x00.toChar() // lateinit TODO
        var joypadLabelNinB:  Char = 0x00.toChar() // lateinit TODO
        var joypadLabelNinX:  Char = 0x00.toChar() // lateinit TODO
        var joypadLabelNinY:  Char = 0x00.toChar() // lateinit TODO
        var joypadLabelNinL:  Char = 0x00.toChar() // lateinit TODO
        var joypadLabelNinR:  Char = 0x00.toChar() // lateinit TODO
        var joypadLabelNinZL: Char = 0x00.toChar() // lateinit TODO
        var joypadLabelNinZR: Char = 0x00.toChar() // lateinit TODO
        val joypadLabelLEFT  = 0xE068.toChar()
        val joypadLabelDOWN  = 0xE069.toChar()
        val joypadLabelUP    = 0xE06A.toChar()
        val joypadLabelRIGHT = 0xE06B.toChar()

        // 0x0 - 0xF: Game-related
        // 0x10 - 0x1F: Config
        // 0x100 and onward: unit tests for dev
        val STATE_ID_SPLASH = 0x0
        val STATE_ID_HOME = 0x1
        val STATE_ID_GAME = 0x3
        val STATE_ID_CONFIG_CALIBRATE = 0x11

        val STATE_ID_TEST_FONT = 0x100
        val STATE_ID_TEST_SHIT = 0x101
        val STATE_ID_TEST_TTY = 0x102

        val STATE_ID_TOOL_NOISEGEN = 0x200

        var hasController = false
        val CONTROLLER_DEADZONE = 0.1f

        /** Available CPU cores */
        val CORES = Runtime.getRuntime().availableProcessors();

        /**
         * If the game is multithreading.
         * True if:
         *
         *     CORES >= 2 and config "multithread" is true
         */
        val MULTITHREAD: Boolean
            get() = CORES >= 2 && getConfigBoolean("multithread")

        private lateinit var configDir: String

        /**
         * 0xAA_BB_XXXX
         * AA: Major version
         * BB: Minor version
         * XXXX: Revision (Repository commits)
         *
         * e.g. 0x02010034 can be translated as 2.1.52
         */
        const val VERSION_RAW = 0x000200E1
        const val VERSION_STRING: String =
                "${VERSION_RAW.ushr(24)}.${VERSION_RAW.and(0xFF0000).ushr(16)}.${VERSION_RAW.and(0xFFFF)}"
        const val NAME = "Terrarum"

        fun main(args: Array<String>) {
            try {
                appgc = AppGameContainer(Terrarum(NAME))
                appgc.setDisplayMode(WIDTH, HEIGHT, false)

                appgc.setTargetFrameRate(TARGET_INTERNAL_FPS)
                appgc.setVSync(VSYNC)
                appgc.setMaximumLogicUpdateInterval(1000 / TARGET_INTERNAL_FPS) // 10 ms
                appgc.setMinimumLogicUpdateInterval(1000 / TARGET_INTERNAL_FPS - 1) // 9 ms
                appgc.setMultiSample(4)

                appgc.setShowFPS(false)

                // game will run normally even if it is not focused
                appgc.setUpdateOnlyWhenVisible(false)
                appgc.alwaysRender = true

                appgc.start()
            }
            catch (ex: SlickException) {
                val logger = Logger.getLogger(Terrarum::class.java.name)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                val calendar = Calendar.getInstance()
                val filepath = "$defaultDir/crashlog-${dateFormat.format(calendar.time)}.txt"
                val fileHandler = FileHandler(filepath)
                logger.addHandler(fileHandler)

                val formatter = SimpleFormatter()
                fileHandler.formatter = formatter

                //logger.info()
                println("The game has been crashed!")
                println("Crash log were saved to $filepath.")
                println("================================================================================")
                logger.log(Level.SEVERE, null, ex)
            }

        }

        private fun getDefaultDirectory() {
            OSName = System.getProperty("os.name")
            OSVersion = System.getProperty("os.version")

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

            println("os.name: '$OSName'")
            println("os.version: '$OSVersion'")
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
            try { cfg = gameConfig[key.toLowerCase()]!! }
            catch (e: NullPointerException) {
                try { cfg = DefaultConfig.fetch()[key.toLowerCase()] }
                catch (e1: NullPointerException) { e.printStackTrace() }
            }
            return cfg!!
        }

        val currentSaveDir: File
            get() = File(defaultSaveDir + "/test") // TODO TEST CODE
    }
}

fun main(args: Array<String>) {
    Terrarum.main(args)
}

fun blendMul() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glColorMask(true, true, true, true)
    GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA)
}

fun blendNormal() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glColorMask(true, true, true, true)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
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

enum class RunningEnvironment {
    PC, CONSOLE, MOBILE
}