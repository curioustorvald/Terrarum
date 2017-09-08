package net.torvald.terrarum

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.GdxRuntimeException
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.torvald.random.HQRNG
import net.torvald.terrarum.Terrarum.RENDER_FPS
import net.torvald.terrarum.TerrarumAppLoader
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gamecontroller.IngameController
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.imagefont.Watch7SegMain
import net.torvald.terrarum.imagefont.WatchDotAlph
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.ui.ItemSlotImageBuilder
import net.torvald.terrarum.ui.MessageWindow
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.JsonWriter
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.GameFontBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.lwjgl.BufferUtils
import org.lwjgl.input.Controllers
import java.io.File
import java.io.IOException
import java.lang.management.ManagementFactory
import java.nio.IntBuffer
import java.util.*

/**
 * Slick2d Version Created by minjaesong on 15-12-30.
 *
 * LibGDX Version Created by minjaesong on 2017-06-15.
 */

/*fun main(args: Array<String>) {
    Terrarum // invoke

    val config = LwjglApplicationConfiguration()
    config.vSyncEnabled = Terrarum.USE_VSYNC
    config.resizable = true
    config.width = 1072
    config.height = 742
    config.backgroundFPS = RENDER_FPS
    config.foregroundFPS = RENDER_FPS
    config.title = GAME_NAME

    Terrarum.screenW = config.width
    Terrarum.screenH = config.height

    println("[TerrarumKt] usevsync = ${Terrarum.USE_VSYNC}")

    // the game must run on same speed regardless of the display FPS;
    // "Terrarum.TARGET_INTERNAL_FPS" denotes "execute as if FPS was set to this value"

    LwjglApplication(Terrarum, config)
}*/



typealias RGBA8888 = Int

object Terrarum : Screen {

    lateinit var appLoader: TerrarumAppLoader
    
    var screenW = 0
    var screenH = 0

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
        get() = if (screenW % 2 == 0) screenW else screenW - 1
    val HEIGHT: Int
        get() = if (screenH % 2 == 0) screenH else screenH - 1

    //val WIDTH_MIN = 800
    //val HEIGHT_MIN = 600

    inline val HALFW: Int
        get() = WIDTH.ushr(1)
    inline val HALFH: Int
        get() = HEIGHT.ushr(1)

    /**
     * To be used with physics simulator
     */
    val TARGET_FPS: Double = 26.6666666666666666666666666 // lower value == faster gravity response (IT WON'T HOTSWAP!!)

    /**
     * To be used with render, to achieve smooth frame drawing
     * TARGET_INTERNAL_FPS > TARGET_FPS for smooth frame drawing
     */
    val TARGET_INTERNAL_FPS: Double = 60.0


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

    var previousScreen: Screen? = null // to be used with temporary states like StateMonitorCheck


    var ingame: Ingame? = null
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

    var environment: RunningEnvironment
        private set

    private val localeSimple = arrayOf("de", "en", "es", "it")
    var gameLocale = "lateinit" // TODO move into AppLoader
        set(value) {
            if (value.isBlank() || value.isEmpty()) {
                field = sysLang
            }
            else {
                try {
                    if (localeSimple.contains(value.substring(0..1)))
                        field = value.substring(0..1)
                    else
                        field = value
                }
                catch (e: StringIndexOutOfBoundsException) {
                    field = value
                }
            }


            fontGame.reload(value)
        }



    val fontGame: GameFontBase = TerrarumAppLoader.fontGame
    lateinit var fontSmallNumbers: TinyAlphNum

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

    const val NAME = TerrarumAppLoader.GAME_NAME


    val systemArch = System.getProperty("os.arch")

    val is32BitJVM = !System.getProperty("sun.arch.data.model").contains("64")


    lateinit var shaderBlur: ShaderProgram
    lateinit var shaderBayer: ShaderProgram
    lateinit var shaderBayerSkyboxFill: ShaderProgram
    lateinit var shaderBlendGlow: ShaderProgram
    lateinit var shaderRGBOnly: ShaderProgram
    lateinit var shaderAtoGrey: ShaderProgram


    lateinit var textureWhiteSquare: Texture


    /** Actually just a mesh of four vertices, two triangles -- not a literal glQuad */
    lateinit var fullscreenQuad: Mesh; private set


    val deltaTime: Float; get() = Gdx.graphics.rawDeltaTime


    init {
        println("$NAME version ${TerrarumAppLoader.getVERSION_STRING()}")


        getDefaultDirectory()
        createDirs()


        // read config i guess...?
        val readFromDisk = readConfigJson()
        if (!readFromDisk) readConfigJson() // what's this for?



        println("[Terrarum] os.arch = $systemArch") // debug info

        if (is32BitJVM) {
            System.err.println("[Terrarum] 32 Bit JVM detected")
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
        if (environment != RunningEnvironment.MOBILE) {
            Gdx.gl.glDisable(GL20.GL_DITHER)
        }



        println("[Terrarum] GL_VERSION = $GL_VERSION")
        println("[Terrarum] GL_MAX_TEXTURE_SIZE = $GL_MAX_TEXTURE_SIZE")
        println("[Terrarum] GL info:\n${Gdx.graphics.glVersion.debugVersionString}") // debug info


        if (GL_VERSION < MINIMAL_GL_VERSION || GL_MAX_TEXTURE_SIZE < MINIMAL_GL_MAX_TEXTURE_SIZE) {
            // TODO notify properly
            throw GdxRuntimeException("Graphics device not capable -- device's GL_VERSION: $GL_VERSION, required: $MINIMAL_GL_VERSION; GL_MAX_TEXTURE_SIZE: $GL_MAX_TEXTURE_SIZE, required: $MINIMAL_GL_MAX_TEXTURE_SIZE")
        }



        fullscreenQuad = Mesh(
                true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.ColorUnpacked(),
                VertexAttribute.TexCoords(0)
        )

        fullscreenQuad.setVertices(floatArrayOf(
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
                WIDTH.toFloat(), 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                WIDTH.toFloat(), HEIGHT.toFloat(), 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                0f, HEIGHT.toFloat(), 0f, 1f, 1f, 1f, 1f, 0f, 0f
        ))
        fullscreenQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))





        TextureRegionPack.globalFlipY = true // !! TO MAKE LEGACY CODE RENDER ON ITS POSITION !!
        Gdx.graphics.isContinuousRendering = true

        batch = SpriteBatch()
        shapeRender = ShapeRenderer()


        //fontGame = GameFontBase("assets/graphics/fonts/terrarum-sans-bitmap", flipY = true)
        fontSmallNumbers = TinyAlphNum


        textureWhiteSquare = Texture(Gdx.files.internal("assets/graphics/ortho_line_tex_2px.tga"))
        textureWhiteSquare.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)


        ShaderProgram.pedantic = false
        shaderBlur = ShaderProgram(Gdx.files.internal("assets/blur.vert"), Gdx.files.internal("assets/blur.frag"))

        shaderBayer = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096_bayer.frag"))
        shaderBayer.begin()
        shaderBayer.setUniformf("rcount", 16f)
        shaderBayer.setUniformf("gcount", 16f)
        shaderBayer.setUniformf("bcount", 16f)
        shaderBayer.end()

        shaderBayerSkyboxFill = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096_bayer_skyboxfill.frag"))

        shaderBlendGlow = ShaderProgram(Gdx.files.internal("assets/blendGlow.vert"), Gdx.files.internal("assets/blendGlow.frag"))

        shaderRGBOnly = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/rgbonly.frag"))
        shaderAtoGrey = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/aonly.frag"))


        if (!shaderBlendGlow.isCompiled) {
            Gdx.app.log("shaderBlendGlow", shaderBlendGlow.log)
            System.exit(1)
        }


        if (!shaderBayer.isCompiled) {
            Gdx.app.log("shaderBayer", shaderBayer.log)
            System.exit(1)
        }

        if (!shaderBayerSkyboxFill.isCompiled) {
            Gdx.app.log("shaderBayerSkyboxFill", shaderBayerSkyboxFill.log)
            System.exit(1)
        }





        gameLocale = getConfigString("language")
        println("[Terrarum] locale = $gameLocale")



        ModMgr // invoke Module Manager, will also invoke BlockCodex
        ItemCodex // invoke Item Codex




        // jump right into the ingame
        /*ingame = Ingame(batch)
        ingame!!.gameLoadInfoPayload = Ingame.NewWorldParameters(2400, 800, HQRNG().nextLong())
        ingame!!.gameLoadMode = Ingame.GameLoadMode.CREATE_NEW
        LoadScreen.screenToLoad = ingame!!
        super.setScreen(LoadScreen)*/



        // title screen
        //appLoader.setScreen(TitleScreen(batch))
        appLoader.setScreen(FuckingWorldRenderer(batch))
    }

    internal fun setScreen(screen: Screen) {
        appLoader.setScreen(screen)
    }

    override fun render(delta: Float) {
        //appLoader.screen.render(deltaTime)
        GLOBAL_RENDER_TIMER += 1
    }

    override fun pause() {
        //appLoader.screen.pause()
    }

    override fun resume() {
        //appLoader.screen.resume()
    }

    override fun dispose() {
        //appLoader.screen.dispose()
        fontGame.dispose()
        fontSmallNumbers.dispose()


        ItemSlotImageBuilder.dispose()
        WatchDotAlph.dispose()
        Watch7SegMain.dispose()
        WatchDotAlph.dispose()

        MessageWindow.SEGMENT_BLACK.dispose()
        MessageWindow.SEGMENT_WHITE.dispose()
        //dispose any other resources used in this level


        shaderBayer.dispose()
        shaderBayerSkyboxFill.dispose()
        shaderBlur.dispose()
        shaderBlendGlow.dispose()
    }

    override fun hide() {

    }

    override fun resize(width: Int, height: Int) {
        //var width = maxOf(width, WIDTH_MIN)
        //var height = maxOf(height, HEIGHT_MIN)

        var width = width
        var height = height

        if (width % 2 == 1) width -= 1
        if (height % 2 == 1) height -= 1

        screenW = width
        screenH = height


        // re-calculate fullscreen quad
        fullscreenQuad.setVertices(floatArrayOf(
                0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
                Terrarum.WIDTH.toFloat(), 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat(), 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                0f, Terrarum.HEIGHT.toFloat(), 0f, 1f, 1f, 1f, 1f, 0f, 0f
        ))
        fullscreenQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))


        //appLoader.resize(width, height)
        //Gdx.graphics.setWindowedMode(width, height)

        println("newsize: ${Gdx.graphics.width}x${Gdx.graphics.height}")
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
        else if (OS.contains("NUX") || OS.contains("NIX") || OS.contains("BSD")) {
            OperationSystem = "LINUX"
            defaultDir = System.getProperty("user.home") + "/.Terrarum"
        }
        else if (OS.contains("SUNOS")) {
            OperationSystem = "SOLARIS"
            defaultDir = System.getProperty("user.home") + "/.Terrarum"
        }
        else if (System.getProperty("java.runtime.name").toUpperCase().contains("ANDROID")) {
            OperationSystem = "ANDROID"
            defaultDir = System.getProperty("user.home") + "/.Terrarum"
            environment = RunningEnvironment.MOBILE
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

    /**
     * Get config from config file. If the entry does not exist, get from defaults; if the entry is not in the default, NullPointerException will be thrown
     */
    private val defaultConfig = DefaultConfig.fetch()

    private fun getConfigMaster(key: String): Any {
        val key = key.toLowerCase()

        val config = try {
            gameConfig[key]
        }
        catch (e: NullPointerException) {
            null
        }

        val defaults = try {
            defaultConfig.get(key)
        }
        catch (e: NullPointerException) {
            null
        }

        if (config == null) {
            if (defaults == null) {
                throw NullPointerException("key not found: '$key'")
            }
            else {
                return defaults
            }
        }
        else {
            return config
        }
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
        get() = WorldCamera.x + Gdx.input.x / (ingame?.screenZoom ?: 1f).toDouble()
    inline val mouseY: Double
        get() = WorldCamera.y + Gdx.input.y / (ingame?.screenZoom ?: 1f).toDouble()
    @JvmStatic inline val mouseTileX: Int
        get() = (mouseX / FeaturesDrawer.TILE_SIZE).floorInt()
    @JvmStatic inline val mouseTileY: Int
        get() = (mouseY / FeaturesDrawer.TILE_SIZE).floorInt()
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
    camera?.setToOrtho(true, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())
    camera?.update()
    batch?.projectionMatrix = camera?.combined
}

fun Float.round(): Float {
    return Math.round(this).toFloat()
}


// ShapeRenderer alternative for rects
fun SpriteBatch.fillRect(x: Float, y: Float, w: Float, h: Float) {
    this.draw(Terrarum.textureWhiteSquare, x, y, w, h)
}
inline fun SpriteBatch.drawStraightLine(x: Float, y: Float, p2: Float, thickness: Float, isVertical: Boolean) {
    if (!isVertical)
        this.fillRect(x, y, p2 - x, thickness)
    else
        this.fillRect(x, y, thickness, p2 - y)
}



infix fun Color.mul(other: Color): Color = this.cpy().mul(other)



/*inline fun Color.toRGB10(): RGB10 {
    val bits = this.toIntBits() // ABGR
    // 0bxxRRRRRRRRRRGGGGGGGGGGBBBBBBBBBB
    // 0bAAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
    return bits.and(0x0000FF).shl(20) or bits.and(0x00FF00).shl(2) or bits.and(0xFF0000).ushr(16)
}*/



fun blendMul() {
    Terrarum.batch.enableBlending()
    Terrarum.batch.setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

fun blendNormal() {
    Terrarum.batch.enableBlending()
    Terrarum.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

fun blendScreen() {
    Terrarum.batch.enableBlending()
    Terrarum.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

object BlendMode {
    const val SCREEN   = "screen"
    const val MULTIPLY = "multiply"
    const val NORMAL   = "normal"
    //const val MAX      = "GL_MAX" // not supported by GLES -- use shader

    fun resolve(mode: String) {
        when (mode) {
            SCREEN   -> blendScreen()
            MULTIPLY -> blendMul()
            NORMAL   -> blendNormal()
            //MAX      -> blendLightenOnly() // not supported by GLES -- use shader
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


