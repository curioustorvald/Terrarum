package net.torvald.terrarum.weather

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.colourutil.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.ParticleTestRain
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.GameWorld
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

    fun update(delta: Float, player: ActorWithBody) {
        currentWeather = weatherList[WEATHER_GENERIC]!![0]


        // test rain toggled by F2
        /*if (KeyToggler.isOn(Input.Keys.F2)) {
            val playerPos = player.centrePosPoint
            kotlin.repeat(4) {
                // 4 seems good
                val rainParticle = ParticleTestRain(
                        playerPos.x + HQRNG().nextInt(Terrarum.WIDTH) - Terrarum.HALFW,
                        playerPos.y - Terrarum.HALFH
                )
                Terrarum.ingame!!.addParticle(rainParticle)
            }
            globalLightNow.set(getGlobalLightOfTime(world.time.todaySeconds).mul(0.3f, 0.3f, 0.3f, 0.58f))
        }*/

    }

    fun render(camera: Camera, world: GameWorld) {

        // we will not care for nextSkybox for now
        val timeNow = world.time.todaySeconds
        val skyboxColourMap = currentWeather.skyboxGradColourMap

        // calculate global light
        val globalLight = getGradientColour(skyboxColourMap, 2, timeNow)
        globalLightNow.set(globalLight)


        // draw skybox to provided graphics instance
        val topCol = getGradientColour(skyboxColourMap, 0, timeNow)
        val bottomCol = getGradientColour(skyboxColourMap, 1, timeNow)

        //Terrarum.textureWhiteSquare.bind(0)

        Terrarum.shaderBayerSkyboxFill.begin()
        Terrarum.shaderBayerSkyboxFill.setUniformMatrix("u_projTrans", camera.combined)
        Terrarum.shaderBayerSkyboxFill.setUniformf("topColor", topCol.r, topCol.g, topCol.b)
        Terrarum.shaderBayerSkyboxFill.setUniformf("bottomColor", bottomCol.r, bottomCol.g, bottomCol.b)
        Terrarum.fullscreenQuad.render(Terrarum.shaderBayerSkyboxFill, GL20.GL_TRIANGLES)
        Terrarum.shaderBayerSkyboxFill.end()
    }

    fun Float.clampOne() = if (this > 1) 1f else this

    private operator fun Color.times(other: Color) = Color(this.r * other.r, this.g * other.g, this.b * other.b, 1f)

    /**
     * Get a GL of specific time
     */
    fun getGlobalLightOfTime(timeInSec: Int): Color =
            getGradientColour(currentWeather.skyboxGradColourMap, 2, timeInSec)

    fun getGradientColour(colorMap: GdxColorMap, row: Int, timeInSec: Int): Color {
        val dataPointDistance = WorldTime.DAY_LENGTH / colorMap.width

        val phaseThis: Int = timeInSec / dataPointDistance // x-coord in gradmap
        val phaseNext: Int = (phaseThis + 1) % colorMap.width

        val colourThis = colorMap.get(phaseThis, row)
        val colourNext = colorMap.get(phaseNext, row)

        // interpolate R, G, B and A
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

    fun getWeatherList(classification: String) = weatherList[classification]!!
    fun getRandomWeather(classification: String) =
            getWeatherList(classification)[HQRNG().nextInt(getWeatherList(classification).size)]

    fun readFromJson(file: File): BaseModularWeather = readFromJson(file.path)

    fun readFromJson(path: String): BaseModularWeather {
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

        val skyboxInJson = JSON.get("skyboxGradColourMap").asJsonPrimitive
        val extraImagesPath = JSON.getAsJsonArray("extraImages")



        val skybox = if (skyboxInJson.isString)
            GdxColorMap(ModMgr.getGdxFile("basegame", "$pathToImage/${skyboxInJson.asString}"))
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
                skyboxGradColourMap = skybox,
                classification = classification,
                extraImages = extraImages
        )
    }
}
