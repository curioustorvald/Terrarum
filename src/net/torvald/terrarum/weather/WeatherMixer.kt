package net.torvald.terrarum.weather

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.RNGConsumer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ParticleMegaRain
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.worlddrawer.WorldCamera
import java.io.File
import java.io.FileFilter

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

    private val renderng = HQRNG()

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

    override fun loadFromSave(s0: Long, s1: Long) {
        super.loadFromSave(s0, s1)
        internalReset()
    }

    fun internalReset() {
        globalLightOverridden = false
        forceTimeAt = null
    }

    init {
        weatherList = HashMap<String, ArrayList<BaseModularWeather>>()


        // read weather descriptions from assets/weather (modular weather)
        val weatherRawValidList = ArrayList<File>()
        val weatherRawsDir = ModMgr.getFilesFromEveryMod("weathers")
        weatherRawsDir.forEach { (modname, parentdir) ->
            printdbg(this, "Scanning dir $parentdir")
            parentdir.listFiles(FileFilter { !it.isDirectory && it.name.endsWith(".json") })?.forEach {
                weatherRawValidList.add(it)
                printdbg(this, "Registering weather '$it' from module $modname")
            }
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
        try {
            currentWeather = weatherList[WEATHER_GENERIC]!![0]
            nextWeather = getRandomWeather(WEATHER_GENERIC)
        }
        catch (e: NullPointerException) {
            e.printStackTrace()

            val defaultWeather = BaseModularWeather(
                    GdxColorMap(1, 3, Color(0x55aaffff), Color(0xaaffffff.toInt()), Color.WHITE),
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


        // test rain toggled by F2
        if (KeyToggler.isOn(Input.Keys.F2) && Terrarum.ingame is TerrarumIngame) {
            val playerPosX = player.hitbox.centeredX
            val playerPosY = player.hitbox.centeredY
            kotlin.repeat(7) {
                val rainParticle = ParticleMegaRain(
                        playerPosX + HQRNG().nextInt(App.scr.width) - App.scr.halfw,
                        playerPosY - App.scr.height
                )
                (Terrarum.ingame!! as TerrarumIngame).addParticle(rainParticle)
            }
            //globalLightNow.set(getGlobalLightOfTime((INGAME.world).time.todaySeconds).mul(0.3f, 0.3f, 0.3f, 0.58f))
        }


        if (!globalLightOverridden) {
            world.globalLight = WeatherMixer.globalLightNow
        }
    }

    //private val parallaxZeroPos = WorldGenerator.TERRAIN_AVERAGE_HEIGHT * 0.75f // just an arb multiplier (266.66666 -> 200)

    private val skyboxPixmap = Pixmap(2, 2, Pixmap.Format.RGBA8888)
    private var skyboxTexture = Texture(skyboxPixmap)

    /**
     * Sub-portion of IngameRenderer. You are not supposed to directly deal with this.
     */
    internal fun render(camera: Camera, batch: FlippingSpriteBatch, world: GameWorld) {
        val parallaxZeroPos = (world.height / 3f) * 0.8888f
        val parallaxDomainSize = world.height / 4f


        // we will not care for nextSkybox for now
        val timeNow = (forceTimeAt ?: world.worldTime.TIME_T.toInt()) % WorldTime.DAY_LENGTH
        val skyboxColourMap = currentWeather.skyboxGradColourMap

        // calculate global light
        val globalLight = getGradientColour(world, skyboxColourMap, 2, timeNow)
        globalLightNow.set(globalLight)


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
        val parallaxSize = 1f / 3f
        val parallaxMidpt = (parallax + 1f) / 2f
        val parallaxTop = (parallaxMidpt - (parallaxSize / 2f)).coerceIn(0f, 1f)
        val parallaxBottom = (parallaxMidpt + (parallaxSize / 2f)).coerceIn(0f, 1f)

        //println("$parallaxZeroPos, $parallax | $parallaxTop - $parallaxMidpt - $parallaxBottom | $parallaxDomainSize")

        // draw skybox to provided graphics instance
        val colTopmost = getGradientColour(world, skyboxColourMap, 0, timeNow).toGdxColor()
        val colBottommost = getGradientColour(world, skyboxColourMap, 1, timeNow).toGdxColor()
        val colMid = colorMix(colTopmost, colBottommost, 0.5f)

        //println(parallax) // parallax value works as intended.

        val colTop = colorMix(colTopmost, colBottommost, parallaxTop)
        val colBottom = colorMix(colTopmost, colBottommost, parallaxBottom)

        // draw using shaperenderer or whatever

        //Terrarum.textureWhiteSquare.bind(0)
        gdxBlendNormalStraightAlpha()

        // draw to skybox texture
        skyboxPixmap.setColor(colBottom)
        skyboxPixmap.drawPixel(0, 0); skyboxPixmap.drawPixel(1, 0)
        skyboxPixmap.setColor(colTop)
        skyboxPixmap.drawPixel(0, 1); skyboxPixmap.drawPixel(1, 1)
        skyboxTexture.dispose()
        skyboxTexture = Texture(skyboxPixmap); skyboxTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)


        batch.inUse {
            batch.shader = null
            batch.drawFlipped(skyboxTexture, 0f, -App.scr.halfhf, App.scr.wf, App.scr.hf * 2f) // because of how the linear filter works, we extend the image by two
        }

        // don't use shader to just fill the whole screen... frag shader will be called a million times and it's best to not burden it
        /*
        IngameRenderer.shaderSkyboxFill.bind()
        IngameRenderer.shaderSkyboxFill.setUniformMatrix("u_projTrans", camera.combined)
        IngameRenderer.shaderSkyboxFill.setUniformf("topColor", topCol.r, topCol.g, topCol.b)
        IngameRenderer.shaderSkyboxFill.setUniformf("bottomColor", bottomCol.r, bottomCol.g, bottomCol.b)
        IngameRenderer.shaderSkyboxFill.setUniformf("parallax", parallax.coerceIn(-1f, 1f))
        IngameRenderer.shaderSkyboxFill.setUniformf("parallax_size", 1f/3f)
        IngameRenderer.shaderSkyboxFill.setUniformf("zoomInv", 1f / (Terrarum.ingame?.screenZoom ?: 1f))
        AppLoader.fullscreenQuad.render(IngameRenderer.shaderSkyboxFill, GL20.GL_TRIANGLES)
        */


        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

    }

    fun Float.clampOne() = if (this > 1) 1f else this

    private operator fun Color.times(other: Color) = Color(this.r * other.r, this.g * other.g, this.b * other.b, 1f)

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
    fun getGlobalLightOfTime(world: GameWorld, timeInSec: Int): Cvec =
            getGradientColour(world, currentWeather.skyboxGradColourMap, 2, timeInSec)

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

        val skyboxInJson = JSON.getString("skyboxGradColourMap")
        val extraImagesPath = JSON.get("extraImages").asStringArray()

        val skybox = GdxColorMap(ModMgr.getGdxFile("basegame", "$pathToImage/${skyboxInJson}"))



        val extraImages = ArrayList<Texture>()
        for (i in extraImagesPath)
            extraImages.add(Texture(ModMgr.getGdxFile("basegame", "$pathToImage/${i}")))


        val classification = JSON.getString("classification")



        var mixFrom: String?
        try { mixFrom = JSON.getString("mixFrom") }
        catch (e: IllegalArgumentException) { mixFrom = null }



        var mixPercentage: Double?
        try { mixPercentage = JSON.getDouble("mixPercentage") }
        catch (e: IllegalArgumentException) { mixPercentage = null }



        return BaseModularWeather(
                skyboxGradColourMap = skybox,
                classification = classification,
                extraImages = extraImages
        )
    }

    fun dispose() {
        try {
            skyboxTexture.dispose()
        }
        catch (e: Throwable) {}

        skyboxPixmap.dispose()
    }
}
