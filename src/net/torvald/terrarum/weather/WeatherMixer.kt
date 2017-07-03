package net.torvald.terrarum.weather

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.colourutil.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ParticleTestRain
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.WorldTime
import java.io.File
import java.util.*

/**
 * Current, next are there for cross-fading two weathers
 *
 *
 * Building a CLUT:
 *     Brightest:Darkest must be "around" 10:1
 *     Is RGBA-formatted (32-bit)
 *
 * Created by minjaesong on 16-07-11.
 */
object WeatherMixer {
    var weatherList: HashMap<String, ArrayList<BaseModularWeather>>

    var currentWeather: BaseModularWeather
    var nextWeather: BaseModularWeather

    lateinit var mixedWeather: BaseModularWeather

    val globalLightNow = Color(0)
    private val world = TerrarumGDX.ingame!!.world

    // Weather indices
    const val WEATHER_GENERIC = "generic"
    const val WEATHER_GENERIC_RAIN = "genericrain"
    // TODO add weather classification indices manually

    // TODO to save from GL overhead, store lightmap to array; use GdxColorMap

    init {
        weatherList = HashMap<String, ArrayList<BaseModularWeather>>()

        // read weather descriptions from assets/weather (modular weather)
        val weatherRawValidList = ArrayList<File>()
        val weatherRaws = ModMgr.getFiles("basegame", "weathers")
        weatherRaws.forEach {
            if (!it.isDirectory && it.name.endsWith(".json"))
                weatherRawValidList.add(it)
        }
        // --> read from directory and store file that looks like RAW
        for (raw in weatherRawValidList) {
            val weather = readFromJson(raw)

            // if List for the classification does not exist, make one
            if (!weatherList.containsKey(weather.classification))
                weatherList.put(weather.classification, ArrayList())

            weatherList[weather.classification]!!.add(weather)
        }



        // initialise
        currentWeather = weatherList[WEATHER_GENERIC]!![0]
        nextWeather = getRandomWeather(WEATHER_GENERIC)
    }

    fun update(delta: Float) {
        currentWeather = weatherList[WEATHER_GENERIC]!![0]


        if (TerrarumGDX.ingame!!.player != null) {
            // test rain toggled by F2
            if (KeyToggler.isOn(Input.Keys.F2)) {
                val playerPos = TerrarumGDX.ingame!!.player!!.centrePosPoint
                kotlin.repeat(4) {
                    // 4 seems good
                    val rainParticle = ParticleTestRain(
                            playerPos.x + HQRNG().nextInt(Gdx.graphics.width) - TerrarumGDX.HALFW,
                            playerPos.y - TerrarumGDX.HALFH
                    )
                    TerrarumGDX.ingame!!.addParticle(rainParticle)
                }
                globalLightNow.set(getGlobalLightOfTime(world.time.todaySeconds).mul(0.3f, 0.3f, 0.3f, 1f))
            }
        }
    }

    private fun Color.set(other: Color) {
        this.r = other.r
        this.g = other.g
        this.b = other.b
        this.a = other.a
    }

    /**
     * Warning! Ends and begins SpriteBatch
     */
    fun render(batch: SpriteBatch) {

        // we will not care for nextSkybox for now
        val timeNow = TerrarumGDX.ingame!!.world.time.todaySeconds
        val skyboxColourMap = currentWeather.skyboxGradColourMap
        val lightColourMap = currentWeather.globalLightColourMap

        // calculate global light
        val globalLight = getGradientColour(skyboxColourMap, 2, timeNow)
        globalLightNow.r = globalLight.r
        globalLightNow.g = globalLight.g
        globalLightNow.b = globalLight.b

        // draw skybox to provided graphics instance
        batch.end()
        TerrarumGDX.inShapeRenderer {
            it.rect(
                    0f, 0f,
                    Gdx.graphics.width.toFloat(),
                    Gdx.graphics.height.toFloat(),
                    getGradientColour(skyboxColourMap, 1, timeNow),
                    getGradientColour(skyboxColourMap, 1, timeNow),
                    getGradientColour(skyboxColourMap, 0, timeNow),
                    getGradientColour(skyboxColourMap, 0, timeNow)
            )
        }
        batch.begin()
    }

    fun Float.clampOne() = if (this > 1) 1f else this

    private operator fun Color.times(other: Color) = Color(this.r * other.r, this.g * other.g, this.b * other.b, 1f)

    /**
     * Get a GL of specific time
     */
    fun getGlobalLightOfTime(timeInSec: Int): Color =
            getGradientColour(currentWeather.globalLightColourMap, 0, timeInSec)

    // TODO colour gradient load from image, store to array
    fun getGradientColour(colorMap: GdxColorMap, row: Int, timeInSec: Int): Color {
        val dataPointDistance = WorldTime.DAY_LENGTH / colorMap.width

        val phaseThis: Int = timeInSec / dataPointDistance // x-coord in gradmap
        val phaseNext: Int = (phaseThis + 1) % colorMap.width

        val colourThis = colorMap.get(phaseThis, row)
        val colourNext = colorMap.get(phaseNext, row)

        // interpolate R, G and B
        val scale = (timeInSec % dataPointDistance).toFloat() / dataPointDistance // [0.0, 1.0]

        val newCol = CIELabUtil.getGradient(scale, colourThis, colourNext)

        /* // very nice monitor code
        // 65 -> 66 | 300 | 19623 | RGB8(255, 0, 255) -[41%]-> RGB8(193, 97, 23) | * `230`40`160`
        // ^ step   |width| time  | colour from        scale   colour to         | output
        if (dataPointDistance == 300)
            println("$phaseThis -> $phaseNext | $dataPointDistance | $timeInSec" +
                    " | ${colourThis.toStringRGB()} -[${scale.times(100).toInt()}%]-> ${colourNext.toStringRGB()}" +
                    " | * `$r`$g`$b`")*/

        return newCol
    }

    fun Color.toStringRGB() = "RGB8(${this.r}, ${this.g}, ${this.b})"

    fun getWeatherList(classification: String) = weatherList[classification]!!
    fun getRandomWeather(classification: String) =
            getWeatherList(classification)[HQRNG().nextInt(getWeatherList(classification).size)]

    fun readFromJson(file: File): BaseModularWeather = readFromJson(file.path)

    fun readFromJson(path: String): BaseModularWeather {
        /* JSON structure:
{
  "globalLight": "colourmap/sky_colour.tga", // integer for static, string (path to image) for dynamic
  "skyboxGradColourMap": "colourmap/sky_colour.tga", // integer for static, string (path to image) for dynamic
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

        val globalLightInJson = JSON.get("globalLight").asJsonPrimitive
        val skyboxInJson = JSON.get("skyboxGradColourMap").asJsonPrimitive
        val extraImagesPath = JSON.getAsJsonArray("extraImages")



        val globalLight = if (globalLightInJson.isString)
            GdxColorMap(ModMgr.getGdxFile("basegame", "$pathToImage/${globalLightInJson.asString}"))
        else if (globalLightInJson.isNumber)
            GdxColorMap(globalLightInJson.asNumber.toInt())
        else
            throw IllegalStateException("In weather descriptor $path -- globalLight seems malformed.")



        val skybox = if (skyboxInJson.isString)
            GdxColorMap(ModMgr.getGdxFile("basegame", "$pathToImage/${skyboxInJson.asString}"))
        else if (globalLightInJson.isNumber)
            GdxColorMap(skyboxInJson.asNumber.toInt())
        else
            throw IllegalStateException("In weather descriptor $path -- skyboxGradColourMap seems malformed.")



        val extraImages = ArrayList<Texture>()
        for (i in extraImagesPath)
            extraImages.add(Texture(ModMgr.getGdxFile("basegame", "$pathToImage/${i.asString}")))



        val classification = JSON.get("classification").asJsonPrimitive.asString



        var mixFrom: String?
        try { mixFrom = JSON.get("mixFrom").asJsonPrimitive.asString }
        catch (e: NullPointerException) { mixFrom = null }



        var mixPercentage: Double?
        try { mixPercentage = JSON.get("mixPercentage").asJsonPrimitive.asDouble }
        catch (e: NullPointerException) { mixPercentage = null }



        return BaseModularWeather(
                globalLightColourMap = globalLight,
                skyboxGradColourMap = skybox,
                classification = classification,
                extraImages = extraImages
        )
    }
}
