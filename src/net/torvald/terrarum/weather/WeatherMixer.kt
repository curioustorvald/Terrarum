package net.torvald.terrarum.weather

import com.jme3.math.FastMath
import net.torvald.JsonFetcher
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gamemap.WorldTime
import net.torvald.terrarum.mapdrawer.Light10B
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.fills.GradientFill
import org.newdawn.slick.geom.Rectangle
import java.util.*

/**
 * Current, next are there for cross-fading two weathers
 *
 * Created by minjaesong on 16-07-11.
 */
object WeatherMixer {
    lateinit var weatherList: ArrayList<BaseModularWeather>

    lateinit var currentWeather: BaseModularWeather
    lateinit var nextWeather: BaseModularWeather

    private var skyBoxCurrent = Rectangle(0f, 0f, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())
    private var skyBoxNext = Rectangle(0f, 0f, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())
    val globalLightNow = Light10B(0)

    // Weather indices
    const val WEATHER_GENERIC = 0
    // TODO add weather indices manually

    const val RAW_DIR = "./res/raw/weathers"

    init {
        weatherList = ArrayList()

        // TODO read weather descriptions from res/weather (modular weather)
        // test: read only one
        weatherList.add(readFromJson("$RAW_DIR/WeatherGeneric.json"))

        // initialise
        currentWeather = weatherList[WEATHER_GENERIC]
        // nextWeather = %&)(@$*%&$(*%
    }

    fun update(gc: GameContainer, delta: Int) {
        currentWeather = weatherList[WEATHER_GENERIC]

    }

    fun render(g: Graphics) {

        // we will not care for nextSkybox for now
        val timeNow = Terrarum.ingame.world.time.elapsedSeconds()
        val skyboxColourMap = currentWeather.skyboxGradColourMap
        val lightColourMap = currentWeather.globalLightColourMap

        // draw skybox to provided (should be main) graphics instance
        val skyColourFill = GradientFill(
                0f, 0f,
                getGradientColour(skyboxColourMap, 0, timeNow),
                0f, Terrarum.HEIGHT.toFloat(),
                getGradientColour(skyboxColourMap, 1, timeNow)
        )
        g.fill(skyBoxCurrent, skyColourFill)

        // calculate global light
        globalLightNow.fromSlickColor(getGradientColour(lightColourMap, 0, timeNow))
    }

    fun getGlobalLightOfTime(timeInSec: Int): Light10B =
            Light10B(getGradientColour(currentWeather.globalLightColourMap, 0, timeInSec))

    fun getGradientColour(image: Image, row: Int, timeInSec: Int): Color {
        val gradMapWidth = image.width
        val phaseThis = Math.round(
                timeInSec.toFloat() / WorldTime.DAY_LENGTH.toFloat() * gradMapWidth
        )
        val phaseNext = (phaseThis + 1) % WorldTime.DAY_LENGTH
        val dataPointDistance = WorldTime.DAY_LENGTH / image.width

        val colourThis = image.getColor(phaseThis, row)
        val colourNext = image.getColor(phaseNext, row)

        // interpolate R, G and B
        val scale = (timeInSec % dataPointDistance).toFloat() / dataPointDistance
        val retColour = Color(0)
        retColour.r = FastMath.interpolateLinear(scale, colourThis.r, colourNext.r)
        retColour.g = FastMath.interpolateLinear(scale, colourThis.g, colourNext.g)
        retColour.b = FastMath.interpolateLinear(scale, colourThis.b, colourNext.b)

        return retColour
    }

    fun readFromJson(path: String): BaseModularWeather {
        /* JSON structure:
{
  "globalLight": "colourmap/sky_colour.png", // integer for static, string (path to image) for dynamic
  "skyboxGradColourMap": "colourmap/sky_colour.png", // integer for static, string (path to image) for dynamic
  "extraImages": [
      // if any, it will be like:
      sun01.png,
      clouds01.png,
      clouds02.png,
      auroraBlueViolet.png
  ]
}
         */
        val pathToImage = "./res/graphics/weathers"

        val JSON = JsonFetcher.readJson(path)

        val globalLightInJson = JSON.get("globalLight").asJsonPrimitive
        val skyboxInJson = JSON.get("skyboxGradColourMap").asJsonPrimitive
        val extraImagesPath = JSON.getAsJsonArray("extraImages")

        val globalLight: Image
        val skybox: Image
        val extraImages = ArrayList<Image>()
        val classification = JSON.get("classification").asJsonPrimitive.asString

        // parse globalLight
        if (globalLightInJson.isString)
            globalLight = Image("$pathToImage/${globalLightInJson.asString}")
        else if (globalLightInJson.isNumber) {
            // make 1x1 image with specified colour
            globalLight = Image(1, 1)
            globalLight.graphics.color = Color(globalLightInJson.asNumber.toInt())
            globalLight.graphics.fillRect(0f, 0f, 1f, 1f)
        }
        else
            throw IllegalStateException("In weather descriptor $path -- globalLight seems malformed.")

        // parse skyboxGradColourMap
        if (skyboxInJson.isString)
            skybox = Image("$pathToImage/${skyboxInJson.asString}")
        else if (globalLightInJson.isNumber) {
            // make 1x2 image with specified colour
            skybox = Image(1, 2)
            skybox.graphics.color = Color(skyboxInJson.asNumber.toInt())
            skybox.graphics.fillRect(0f, 0f, 1f, 2f)
        }
        else
            throw IllegalStateException("In weather descriptor $path -- skyboxGradColourMap seems malformed.")

        // get extra images
        for (i in extraImagesPath)
            extraImages.add(Image("$pathToImage/$i"))

        return BaseModularWeather(globalLight, skybox, classification, extraImages)
    }
}
