package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.GdxRuntimeException
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.jme3.math.FastMath
import net.torvald.dataclass.ArrayListMap
import net.torvald.dataclass.CircularArray
import net.torvald.random.HQRNG
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.AppLoader.printdbgerr
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.itemproperties.ItemCodex
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

    val debugTimers = ArrayListMap<String, Long>()

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
    val TARGET_FPS: Double = 26.0 + (2.0 / 3.0)
    // 26.0 + (2.0 / 3.0) // lower value == faster gravity response (IT WON'T HOTSWAP!!)
    // protip: using METER, game unit and SI unit will have same number

    /**
     * To be used with render, to achieve smooth frame drawing
     * TARGET_INTERNAL_FPS > TARGET_FPS for smooth frame drawing
     */
    val TARGET_INTERNAL_FPS: Double = 60.0

    internal val UPDATE_CATCHUP_MAX_TRIES = 10






    var previousScreen: Screen? = null // to be used with temporary states like StateMonitorCheck


    var ingame: IngameInstance? = null
    private val gameConfig = GameConfig()

    val OSName = System.getProperty("os.name")
    val OSVersion = System.getProperty("os.version")
    lateinit var OperationSystem: String // all caps "WINDOWS, "OSX", "LINUX", "SOLARIS", "UNKNOWN"
        private set
    lateinit var defaultDir: String
        private set
    lateinit var defaultSaveDir: String
        private set



    private val javaHeapCircularArray = CircularArray<Int>(128)
    private val nativeHeapCircularArray = CircularArray<Int>(128)

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

    var environment: RunningEnvironment
        private set




    val fontGame: GameFontBase = AppLoader.fontGame
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

    const val NAME = AppLoader.GAME_NAME


    val systemArch = System.getProperty("os.arch")

    val is32BitJVM = !System.getProperty("sun.arch.data.model").contains("64")


    lateinit var shaderBlur: ShaderProgram
    lateinit var shaderBayer: ShaderProgram
    lateinit var shaderSkyboxFill: ShaderProgram
    lateinit var shaderBlendGlow: ShaderProgram
    lateinit var shaderRGBOnly: ShaderProgram
    lateinit var shaderAtoGrey: ShaderProgram


    lateinit var textureWhiteSquare: Texture


    lateinit var testTexture: Texture


    /** Actually just a mesh of four vertices, two triangles -- not a literal glQuad */
    val fullscreenQuad = AppLoader.fullscreenQuad


    val deltaTime: Float; get() = Gdx.graphics.rawDeltaTime


    lateinit var assetManager: AssetManager // TODO


    init {
        println("$NAME version ${AppLoader.getVERSION_STRING()}")
        println("Java Runtime version ${System.getProperty("java.version")}")
        println("LibGDX version ${com.badlogic.gdx.Version.VERSION}")


        getDefaultDirectory()
        createDirs()


        // read config i guess...?
        val readFromDisk = readConfigJson()
        if (!readFromDisk) readConfigJson() // what's this for?



        printdbg(this, "os.arch = $systemArch") // debug info

        if (is32BitJVM) {
            printdbgerr(this, "32 Bit JVM detected")
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


        assetManager = AssetManager()


        testTexture = Texture(Gdx.files.internal("./assets/test_texture.tga"))


        printdbg(this, "GL_VERSION = $GL_VERSION")
        printdbg(this, "GL_MAX_TEXTURE_SIZE = $GL_MAX_TEXTURE_SIZE")
        printdbg(this, "GL info:\n${Gdx.graphics.glVersion.debugVersionString}") // debug info


        if (GL_VERSION < MINIMAL_GL_VERSION || GL_MAX_TEXTURE_SIZE < MINIMAL_GL_MAX_TEXTURE_SIZE) {
            // TODO notify properly
            throw GdxRuntimeException("Graphics device not capable -- device's GL_VERSION: $GL_VERSION, required: $MINIMAL_GL_VERSION; GL_MAX_TEXTURE_SIZE: $GL_MAX_TEXTURE_SIZE, required: $MINIMAL_GL_MAX_TEXTURE_SIZE")
        }

        // resize fullscreen quad?


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


        if (getConfigBoolean("fxdither")) {
            shaderBayer = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096_bayer.frag"))
            shaderBayer.begin()
            shaderBayer.setUniformf("rcount", 64f)
            shaderBayer.setUniformf("gcount", 64f)
            shaderBayer.setUniformf("bcount", 64f)
            shaderBayer.end()

            shaderSkyboxFill = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/4096_bayer_skyboxfill.frag"))
            shaderSkyboxFill.begin()
            shaderSkyboxFill.setUniformf("rcount", 64f)
            shaderSkyboxFill.setUniformf("gcount", 64f)
            shaderSkyboxFill.setUniformf("bcount", 64f)
            shaderSkyboxFill.end()
        }
        else {
            shaderBayer = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/passthru.frag"))
            shaderSkyboxFill = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/skyboxfill.frag"))
        }


        shaderBlendGlow = ShaderProgram(Gdx.files.internal("assets/blendGlow.vert"), Gdx.files.internal("assets/blendGlow.frag"))

        shaderRGBOnly = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/rgbonly.frag"))
        shaderAtoGrey = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/aonly.frag"))


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



        // jump right into the ingame
        /*ingame = Ingame(batch)
        ingame!!.gameLoadInfoPayload = Ingame.NewWorldParameters(2400, 800, HQRNG().nextLong())
        ingame!!.gameLoadMode = Ingame.GameLoadMode.CREATE_NEW
        LoadScreen.screenToLoad = ingame!!
        super.setScreen(LoadScreen)*/



        // title screen
        AppLoader.getINSTANCE().setScreen(TitleScreen(batch))
        //appLoader.setScreen(FuckingWorldRenderer(batch))
    }

    fun setScreen(screen: Screen) {
        AppLoader.getINSTANCE().setScreen(screen)
    }

    override fun render(delta: Float) {
        Terrarum.debugTimers["GDX.delta"] = delta.times(1000_000_000f).toLong()
        AppLoader.getINSTANCE().screen.render(deltaTime)
        //GLOBAL_RENDER_TIMER += 1
        // moved to AppLoader; global event must be place at the apploader to prevent ACCIDENTAL forgot-to-update type of bug.
    }

    override fun pause() {
        AppLoader.getINSTANCE().screen.pause()
    }

    override fun resume() {
        AppLoader.getINSTANCE().screen.resume()
    }

    override fun dispose() {
        AppLoader.getINSTANCE().screen.dispose()

        fontGame.dispose()
        fontSmallNumbers.dispose()


        //dispose any other resources used in this level


        shaderBayer.dispose()
        shaderSkyboxFill.dispose()
        shaderBlur.dispose()
        shaderBlendGlow.dispose()


        shapeRender.dispose()
        batch.dispose()
    }

    override fun hide() {
        AppLoader.getINSTANCE().screen.hide()
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


        try {
            AppLoader.getINSTANCE().screen.resize(screenW, screenH)
        }
        catch (e: NullPointerException) { }

        // re-calculate fullscreen quad
        //updateFullscreenQuad(screenW, screenH)

        //appLoader.resize(width, height)
        //Gdx.graphics.setWindowedMode(width, height)

        printdbg(this, "newsize: ${Gdx.graphics.width}x${Gdx.graphics.height} | internal: ${width}x$height")
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

        printdbg(this, "os.name = $OSName (with identifier $OperationSystem)")
        printdbg(this, "os.version = $OSVersion")
        printdbg(this, "default directory: $defaultDir")
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

    /** Position of the cursor in the world */
    inline val mouseX: Double
        get() = WorldCamera.x + Gdx.input.x / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    inline val mouseY: Double
        get() = WorldCamera.y + Gdx.input.y / (ingame?.screenZoom ?: 1f).toDouble()
    /** Position of the cursor in the world */
    @JvmStatic inline val mouseTileX: Int
        get() = (mouseX / FeaturesDrawer.TILE_SIZE).floorInt()
    /** Position of the cursor in the world */
    @JvmStatic inline val mouseTileY: Int
        get() = (mouseY / FeaturesDrawer.TILE_SIZE).floorInt()
    inline val mouseScreenX: Int
        get() = Gdx.input.x
    inline val mouseScreenY: Int
        get() = Gdx.input.y
    /** Bigger than 1.0 */
    inline val updateRate: Double
        get() = 1.0 / Gdx.graphics.deltaTime
    val updateRateStr: String
        get() = String.format("%.2f", updateRate)
    /** Smaller than 1.0 */
    val renderRate = 1.0 / TARGET_INTERNAL_FPS
    val renderRateStr = TARGET_INTERNAL_FPS.toString()

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
    this.draw(Terrarum.textureWhiteSquare, x, y, w, h)
}
inline fun SpriteBatch.drawStraightLine(x: Float, y: Float, otherEnd: Float, thickness: Float, isVertical: Boolean) {
    if (!isVertical)
        this.fillRect(x, y, otherEnd - x, thickness)
    else
        this.fillRect(x, y, thickness, otherEnd - y)
}



infix fun Color.mul(other: Color): Color = this.cpy().mul(other)



/*inline fun Color.toRGB10(): RGB10 {
    val bits = this.toIntBits() // ABGR
    // 0bxxRRRRRRRRRRGGGGGGGGGGBBBBBBBBBB
    // 0bAAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
    return bits.and(0x0000FF).shl(20) or bits.and(0x00FF00).shl(2) or bits.and(0xFF0000).ushr(16)
}*/



fun blendMul(batch: SpriteBatch? = null) {
    (batch ?: Terrarum.batch).enableBlending()
    (batch ?: Terrarum.batch).setBlendFunction(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

fun blendNormal(batch: SpriteBatch? = null) {
    (batch ?: Terrarum.batch).enableBlending()
    (batch ?: Terrarum.batch).setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

fun blendScreen(batch: SpriteBatch? = null) {
    (batch ?: Terrarum.batch).enableBlending()
    (batch ?: Terrarum.batch).setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR)
    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD) // batch.flush does not touch blend equation
}

fun blendDisable(batch: SpriteBatch? = null) {
    (batch ?: Terrarum.batch).disableBlending()
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

inline fun Int.sqr(): Int = this * this
inline fun Double.floorInt() = Math.floor(this).toInt()
inline fun Float.floorInt() = FastMath.floor(this)
inline fun Float.floor() = FastMath.floor(this).toFloat()
inline fun Double.ceilInt() = Math.ceil(this).toInt()
inline fun Float.ceil(): Float = FastMath.ceil(this).toFloat()
inline fun Float.ceilInt() = FastMath.ceil(this)
inline fun Double.round() = Math.round(this).toDouble()
inline fun Double.floor() = Math.floor(this)
inline fun Double.ceil() = this.floor() + 1.0
inline fun Double.roundInt(): Int = Math.round(this).toInt()
inline fun Float.roundInt(): Int = Math.round(this)
inline fun Double.abs() = Math.abs(this)
inline fun Double.sqr() = this * this
inline fun Double.sqrt() = Math.sqrt(this)
inline fun Float.sqrt() = FastMath.sqrt(this)
inline fun Int.abs() = if (this < 0) -this else this
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