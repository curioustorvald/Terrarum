package com.torvald.terrarum

import com.torvald.imagefont.GameFontWhite
import com.torvald.JsonFetcher
import com.torvald.JsonWriter
import com.torvald.terrarum.langpack.Lang
import org.lwjgl.input.Controllers
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.Font
import org.newdawn.slick.GameContainer
import org.newdawn.slick.SlickException
import org.newdawn.slick.state.StateBasedGame
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by minjaesong on 15-12-30.
 * Kotlin code: Created by minjaesong on 16-03-19.
 */
class Terrarum @Throws(SlickException::class)
constructor(gamename: String) : StateBasedGame(gamename) {

    init {

        gameConfig = GameConfig()

        getDefaultDirectory()
        createDirs()

        val readFromDisk = readConfigJson()
        if (!readFromDisk) readConfigJson()

        // get locale from config
        gameLocale = gameConfig.getAsString("language") ?: sysLang

        // if bad game locale were set, use system locale
        if (gameLocale.length < 4)
            gameLocale = sysLang

        println("[terrarum] Locale: " + gameLocale)
    }

    @Throws(SlickException::class)
    override fun initStatesList(gc: GameContainer) {
        gameFontWhite = GameFontWhite()

        hasController = gc.input.controllerCount > 0
        if (hasController) {
            for (c in 0..Controllers.getController(0).axisCount - 1) {
                Controllers.getController(0).setDeadZone(c, CONTROLLER_DEADZONE)
            }
        }

        appgc.input.enableKeyRepeat()

        game = Game()
        addState(game)
    }

    companion object {

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

        val WIDTH = 1060
        val HEIGHT = 742 // IMAX ratio
        var VSYNC = true
        val VSYNC_TRIGGER_THRESHOLD = 56

        lateinit var game: Game
        lateinit var gameConfig: GameConfig

        lateinit var OSName: String
        lateinit var OSVersion: String
        lateinit var OperationSystem: String
        lateinit var defaultDir: String
        lateinit var defaultSaveDir: String

        var gameLocale = "" // locale override

        lateinit var gameFontWhite: Font

        val SCENE_ID_HOME = 1
        val SCENE_ID_GAME = 3

        var hasController = false
        val CONTROLLER_DEADZONE = 0.1f

        private lateinit var configDir: String

        fun main(args: Array<String>) {
            try {
                appgc = AppGameContainer(Terrarum("Terrarum"))
                appgc.setDisplayMode(WIDTH, HEIGHT, false)

                appgc.setTargetFrameRate(TARGET_INTERNAL_FPS)
                appgc.setVSync(VSYNC)
                appgc.setMaximumLogicUpdateInterval(1000 / TARGET_INTERNAL_FPS)
                appgc.setMinimumLogicUpdateInterval(1000 / TARGET_INTERNAL_FPS - 1)

                appgc.setShowFPS(false)
                appgc.setUpdateOnlyWhenVisible(false)

                appgc.start()
            }
            catch (ex: SlickException) {
                Logger.getLogger(Terrarum::class.java.name).log(Level.SEVERE, null, ex)
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
        }

        private fun createDirs() {
            val dirs = arrayOf(File(defaultSaveDir))

            for (d in dirs) {
                if (!d.exists()) {
                    d.mkdirs()
                }
            }
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
                val jsonObject = JsonFetcher.readJson(configDir)

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

        // exception handling
        val sysLang: String
            get() {
                val lan = System.getProperty("user.language")
                var country = System.getProperty("user.country")
                if (lan == "en")
                    country = "US"
                else if (lan == "fr")
                    country = "FR"
                else if (lan == "de")
                    country = "DE"
                else if (lan == "ko") country = "KR"

                return lan + country
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
            var cfg: Int = 0
            try {
                cfg = gameConfig.getAsInt(key)!!
            }
            catch (e: NullPointerException) {
                // if the config set does not have the key, try for the default config
                try {
                    cfg = DefaultConfig.fetch().get(key).asInt
                }
                catch (e1: NullPointerException) {
                    e.printStackTrace()
                }
            }
            catch (e: TypeCastException) {
                // if the config set does not have the key, try for the default config
                try {
                    cfg = DefaultConfig.fetch().get(key).asInt
                }
                catch (e1: kotlin.TypeCastException) {
                    e.printStackTrace()
                }
            }

            return cfg
        }

        /**
         * Return config from config set. If the config does not exist, default value will be returned.
         * @param key
         * *
         * @return Config from config set or default config if it does not exist.
         * *
         * @throws NullPointerException if the specified config simply does not exist.
         */
        fun getConfigFloat(key: String): Float {
            var cfg = 0f
            try {
                cfg = gameConfig.getAsFloat(key)!!
            }
            catch (e: NullPointerException) {
                try {
                    cfg = DefaultConfig.fetch().get(key).asFloat
                }
                catch (e1: NullPointerException) {
                    e.printStackTrace()
                }

            }

            return cfg
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
            var cfg = ""
            try {
                cfg = gameConfig.getAsString(key)!!
            }
            catch (e: NullPointerException) {
                try {
                    cfg = DefaultConfig.fetch().get(key).asString
                }
                catch (e1: NullPointerException) {
                    e.printStackTrace()
                }

            }

            return cfg
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
            var cfg = false
            try {
                cfg = gameConfig.getAsBoolean(key)!!
            }
            catch (e: NullPointerException) {
                try {
                    cfg = DefaultConfig.fetch().get(key).asBoolean
                }
                catch (e1: NullPointerException) {
                    e.printStackTrace()
                }

            }

            return cfg
        }
    }
}

fun main(args: Array<String>) = Terrarum.main(args)