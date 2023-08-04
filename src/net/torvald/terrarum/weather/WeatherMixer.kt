package net.torvald.terrarum.weather

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.gameworld.WorldTime.Companion.DAY_LENGTH
import net.torvald.terrarum.RNGConsumer
import net.torvald.terrarum.clut.Skybox
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.worlddrawer.WorldCamera
import java.io.File
import java.io.FileFilter

/**
 * Currently there is a debate whether this module must be part of the engine or the basegame
 */

/**
 *
 *
 * Current, next are there for cross-fading two weathers
 *
 *
 * Building a CLUT:
 *     Brightest:Darkest must be "around" 10:1
 *     Is RGBA-formatted (32-bit)
 *
 * Created by minjaesong on 2016-07-11.
 */
internal object WeatherMixer : RNGConsumer {

    override val RNG = HQRNG()

    var globalLightOverridden = false

    var weatherList: HashMap<String, ArrayList<BaseModularWeather>>

    var currentWeather: BaseModularWeather
    var nextWeather: BaseModularWeather

    lateinit var mixedWeather: BaseModularWeather

    val globalLightNow = Cvec(0)

    // Weather indices
    const val WEATHER_GENERIC = "generic"
    const val WEATHER_GENERIC_RAIN = "genericrain"
    // TODO add weather classification indices manually

    // TODO to save from GL overhead, store lightmap to array; use GdxColorMap

    var forceTimeAt: Int? = null
    var forceSolarElev: Double? = null
    var forceTurbidity: Double? = null

    // doesn't work if the png is in greyscale/indexed mode
    val starmapTex: TextureRegion = TextureRegion(Texture(Gdx.files.internal("./assets/graphics/astrum.png"))).also {
        it.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        it.texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
    }

    private val shaderBlendMax = App.loadShaderFromClasspath("shaders/blendSkyboxStars.vert", "shaders/blendSkyboxStars.frag")

    private var astrumOffX = 0f
    private var astrumOffY = 0f

    private val moonlightMax = Cvec(0.23f, 0.24f, 0.25f, 0.21f) // actual moonlight is around ~4100K but our mesopic vision makes it appear blueish (wikipedia: Purkinje effect)

    override fun loadFromSave(s0: Long, s1: Long) {
        super.loadFromSave(s0, s1)
        internalReset(s0, s1)
    }

    fun internalReset() = internalReset(RNG.state0, RNG.state1)

    fun internalReset(s0: Long, s1: Long) {
        globalLightOverridden = false
        forceTimeAt = null
        forceSolarElev = null
        forceTurbidity = null

        astrumOffX = s0.and(0xFFFFL).toFloat() / 65535f * starmapTex.regionWidth
        astrumOffY = s1.and(0xFFFFL).toFloat() / 65535f * starmapTex.regionHeight
    }

    init {
        weatherList = HashMap<String, ArrayList<BaseModularWeather>>()


        // read weather descriptions from assets/weather (modular weather)
        val weatherRawValidList = ArrayList<Pair<String, File>>()
        val weatherRawsDir = ModMgr.getFilesFromEveryMod("weathers")
        weatherRawsDir.forEach { (modname, parentdir) ->
            printdbg(this, "Scanning dir $parentdir")
            parentdir.listFiles(FileFilter { !it.isDirectory && it.name.endsWith(".json") })?.forEach {
                weatherRawValidList.add(modname to it)
                printdbg(this, "Registering weather '$it' from module $modname")
            }
        }
        // --> read from directory and store file that looks like RAW
        for ((modname, raw) in weatherRawValidList) {
            val weather = readFromJson(modname, raw)

            // if List for the classification does not exist, make one
            if (!weatherList.containsKey(weather.classification))
                weatherList.put(weather.classification, ArrayList())

            weatherList[weather.classification]!!.add(weather)
        }



        // initialise
        try {
            currentWeather = weatherList[WEATHER_GENERIC]!![0]
            nextWeather = getRandomWeather(WEATHER_GENERIC)
        }
        catch (e: NullPointerException) {
            e.printStackTrace()

            val defaultWeather = BaseModularWeather(
                GdxColorMap(1, 3, Color(0x55aaffff), Color(0xaaffffff.toInt()), Color.WHITE),
                GdxColorMap(2, 2, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE),
                "default",
                ArrayList<Texture>()
            )

            currentWeather = defaultWeather
            nextWeather = defaultWeather
        }
    }

    /**
     * Part of Ingame update
     */
    fun update(delta: Float, player: ActorWithBody?, world: GameWorld) {
        if (player == null) return

//        currentWeather = weatherList[WEATHER_GENERIC]!![0] // force set weather


        if (!globalLightOverridden) {
            world.globalLight = WeatherMixer.globalLightNow
        }

    }

    var turbidity = 4.0; private set
    private var gH = 1.4f * App.scr.height
//    private var gH = 0.8f * App.scr.height

    internal var parallaxPos = 0f; private set

    private val HALF_DAY = DAY_LENGTH / 2
    /**
     * Sub-portion of IngameRenderer. You are not supposed to directly deal with this.
     */
    internal fun render(camera: Camera, batch: FlippingSpriteBatch, world: GameWorld) {
        drawSkybox(camera, batch, world)
    }

    private fun drawSkybox(camera: Camera, batch: FlippingSpriteBatch, world: GameWorld) {
        val parallaxZeroPos = (world.height / 3f)
        val parallaxDomainSize = 300f

        // we will not care for nextSkybox for now
        val timeNow = (forceTimeAt ?: world.worldTime.TIME_T.toInt()) % WorldTime.DAY_LENGTH
        val solarElev = if (forceSolarElev != null)
            forceSolarElev!!
        else if (forceTimeAt != null)
            world.worldTime.getSolarElevationAt(world.worldTime.ordinalDay, forceTimeAt!!)
        else
            world.worldTime.solarElevationDeg
        val daylightClut = currentWeather.daylightClut
        // calculate global light
        val moonSize = (-(2.0 * world.worldTime.moonPhase - 1.0).abs() + 1.0).toFloat()
        val globalLightBySun: Cvec = getGradientColour2(daylightClut, solarElev, timeNow)
        val globalLightByMoon: Cvec = moonlightMax * moonSize
        globalLightNow.set(globalLightBySun max globalLightByMoon)

        /* (copied from the shader source)
         UV mapping coord.y

         -+ <- 1.0  =
         D|         = // parallax of -1
         i|  =      =
         s|  = // parallax of 0
         p|  =      =
         .|         = // parallax of +1
         -+ <- 0.0  =
         */
        val parallax = ((parallaxZeroPos - WorldCamera.gdxCamY.div(TILE_SIZEF)) / parallaxDomainSize).times(-1f).coerceIn(-1f, 1f)
        parallaxPos = parallax
//        println(parallax) // parallax value works as intended.

        gdxBlendNormalStraightAlpha()

        val thisTurbidity = forceTurbidity ?: turbidity

        // TODO trilinear with (deg, turb, alb)
        val gradY = -(gH - App.scr.height) * ((parallax + 1f) / 2f)
        val (tex, uvs) = Skybox.getUV(solarElev, thisTurbidity, 0.3)


        starmapTex.texture.bind(1)
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

        val astrumX = world.worldTime.axialTiltDeg.toFloat() * starmapTex.regionWidth / 150f
        val astrumY = ((world.worldTime.TIME_T / WorldTime.DIURNAL_MOTION_LENGTH) % 1f) * starmapTex.regionHeight

        batch.inUse {
            batch.shader = shaderBlendMax
            shaderBlendMax.setUniformi("tex1", 1)
            shaderBlendMax.setUniformf("drawOffset", 0f, gradY)
            shaderBlendMax.setUniformf("drawOffsetSize", App.scr.wf, gH)
            shaderBlendMax.setUniform2fv("skyboxUV1", uvs, 0, 2)
            shaderBlendMax.setUniform2fv("skyboxUV2", uvs, 2, 2)
            shaderBlendMax.setUniformf("astrumScroll", astrumOffX + astrumX, astrumOffY + astrumY)
            shaderBlendMax.setUniformf("randomNumber",
//                (world.worldTime.TIME_T.plus(31L) xor 1453L + 31L).and(1023).toFloat(),
//                (world.worldTime.TIME_T.plus(37L) xor  862L + 31L).and(1023).toFloat(),
//                (world.worldTime.TIME_T.plus(23L) xor 1639L + 29L).and(1023).toFloat(),
//                (world.worldTime.TIME_T.plus(29L) xor 2971L + 41L).and(1023).toFloat(),
                world.worldTime.TIME_T.div(+12.1f).plus(31L),
                world.worldTime.TIME_T.div(-11.8f).plus(37L),
                world.worldTime.TIME_T.div(+11.9f).plus(23L),
                world.worldTime.TIME_T.div(-12.3f).plus(29L),
            )

            batch.color = Color.WHITE
            batch.draw(tex, 0f, gradY, App.scr.wf, gH, 0f, 0f, 1f, 1f)

            batch.color = Color.WHITE
        }

    }

    private operator fun Cvec.times(other: Float) = Cvec(this.r * other, this.g * other, this.b * other, this.a * other)
    private infix fun Cvec.max(other: Cvec) = Cvec(
        if (this.r > other.r) this.r else other.r,
        if (this.g > other.g) this.g else other.g,
        if (this.b > other.b) this.b else other.b,
        if (this.a > other.a) this.a else other.a
    )

    fun colorMix(one: Color, two: Color, scale: Float): Color {
        return Color(
                FastMath.interpolateLinear(scale, one.r, two.r),
                FastMath.interpolateLinear(scale, one.g, two.g),
                FastMath.interpolateLinear(scale, one.b, two.b),
                FastMath.interpolateLinear(scale, one.a, two.a)
        )
    }

    /**
     * Get a GL of specific time
     */
    fun getGlobalLightOfTimeOfNoon(): Cvec {
        currentWeather.daylightClut.let { it.get(it.width - 1, 0) }.let {
            return Cvec(it.r, it.g, it.b, it.a)
        }
    }

    fun getGradientColour(world: GameWorld, colorMap: GdxColorMap, row: Int, timeInSec: Int): Cvec {
        val dataPointDistance = WorldTime.DAY_LENGTH / colorMap.width

        val phaseThis: Int = timeInSec / dataPointDistance // x-coord in gradmap
        val phaseNext: Int = (phaseThis + 1) % colorMap.width

        val colourThis = colorMap.get(phaseThis % colorMap.width, row)
        val colourNext = colorMap.get(phaseNext % colorMap.width, row)

        // interpolate R, G, B and A
        val scale = (timeInSec % dataPointDistance).toFloat() / dataPointDistance // [0.0, 1.0]

        val newCol = colourThis.cpy().lerp(colourNext, scale)//CIELuvUtil.getGradient(scale, colourThis, colourNext)

        /* // very nice monitor code
        // 65 -> 66 | 300 | 19623 | RGB8(255, 0, 255) -[41%]-> RGB8(193, 97, 23) | * `230`40`160`
        // ^ step   |width| time  | colour from        scale   colour to         | output
        if (dataPointDistance == 300)
            println("$phaseThis -> $phaseNext | $dataPointDistance | $timeInSec" +
                    " | ${colourThis.toStringRGB()} -[${scale.times(100).toInt()}%]-> ${colourNext.toStringRGB()}" +
                    " | * `$r`$g`$b`")*/

        return Cvec(newCol)
    }

    fun getGradientColour2(colorMap: GdxColorMap, solarAngleInDeg: Double, timeOfDay: Int): Cvec {
        val pNowRaw = (solarAngleInDeg + 75.0) / 150.0 * colorMap.width

        val pStartRaw = pNowRaw.floorToInt()
        val pNextRaw = pStartRaw + 1

        val pSx: Int; val pSy: Int; val pNx: Int; val pNy: Int
        if (timeOfDay < HALF_DAY) {
            pSx = pStartRaw.coerceIn(0 until colorMap.width)
            pSy = 0
            if (pSx == colorMap.width-1) { pNx = pSx; pNy = 1 }
            else                         { pNx = pSx + 1; pNy = 0 }
        }
        else {
            pSx = (pStartRaw + 1).coerceIn(0 until colorMap.width)
            pSy = 1
            if (pSx == 0) { pNx = 0; pNy = 0 }
            else          { pNx = pSx - 1; pNy = 1 }
        }

        val colourThisRGB = colorMap.get(pSx, pSy)
        val colourNextRGB = colorMap.get(pNx, pNy)
        val colourThisUV = colorMap.get(pSx, pSy + 2)
        val colourNextUV = colorMap.get(pNx, pNy + 2)

        // interpolate R, G, B and A
        var scale = (pNowRaw - pStartRaw).toFloat()
        if (timeOfDay >= HALF_DAY) scale = 1f - scale

        val newColRGB = colourThisRGB.cpy().lerp(colourNextRGB, scale)//CIELuvUtil.getGradient(scale, colourThis, colourNext)
        val newColUV = colourThisUV.cpy().lerp(colourNextUV, scale)//CIELuvUtil.getGradient(scale, colourThis, colourNext)

        return Cvec(newColRGB, newColUV.r)
    }

    fun getWeatherList(classification: String) = weatherList[classification]!!
    fun getRandomWeather(classification: String) =
            getWeatherList(classification)[RNG.nextInt(getWeatherList(classification).size)]

    fun readFromJson(modname: String, file: File): BaseModularWeather = readFromJson(modname, file.path)

    fun readFromJson(modname: String, path: String): BaseModularWeather {
        /* JSON structure:
{
  "skyboxGradColourMap": "colourmap/sky_colour.tga", // string (path to image) for dynamic. Image must be RGBA8888 or RGB888
  "extraImages": [
      // if any, it will be like:
      sun01.tga,
      clouds01.tga,
      clouds02.tga,
      auroraBlueViolet.tga
  ]
}
         */
        val pathToImage = "weathers"

        val JSON = JsonFetcher(path)

        val skyboxInJson = JSON.getString("skyboxGradColourMap")
        val lightbox = JSON.getString("daylightClut")
        val extraImagesPath = JSON.get("extraImages").asStringArray()

        val skybox = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${skyboxInJson}"))
        val daylight = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${lightbox}"))


        val extraImages = ArrayList<Texture>()
        for (i in extraImagesPath)
            extraImages.add(Texture(ModMgr.getGdxFile(modname, "$pathToImage/${i}")))


        val classification = JSON.getString("classification")



        var mixFrom: String?
        try { mixFrom = JSON.getString("mixFrom") }
        catch (e: IllegalArgumentException) { mixFrom = null }



        var mixPercentage: Double?
        try { mixPercentage = JSON.getDouble("mixPercentage") }
        catch (e: IllegalArgumentException) { mixPercentage = null }



        return BaseModularWeather(
            skyboxGradColourMap = skybox,
            daylightClut = daylight,
            classification = classification,
            extraImages = extraImages
        )
    }

    fun dispose() {
        weatherList.values.forEach { list ->
            list.forEach { weather ->
                weather.extraImages.forEach { it.tryDispose() }
            }
        }
        starmapTex.texture.dispose()
        shaderBlendMax.dispose()
    }
}
