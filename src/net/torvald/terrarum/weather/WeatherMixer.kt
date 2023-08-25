package net.torvald.terrarum.weather

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.JsonValue
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
import net.torvald.terrarum.utils.forEachSiblings
import net.torvald.terrarum.weather.WeatherObjectCloud.Companion.ALPHA_ROLLOFF_Z
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.util.SortedArrayList
import java.io.File
import java.io.FileFilter
import java.lang.Double.doubleToLongBits
import java.lang.Math.toDegrees
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.*

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
    private val moonlightMax = Cvec(0.23f, 0.24f, 0.25f, 0.21f) // actual moonlight is around ~4100K but our mesopic vision makes it appear blueish (wikipedia: Purkinje effect)

    // Weather indices
    const val WEATHER_GENERIC = "generic"
    const val WEATHER_OVERCAST = "overcast"
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

    private val shaderAstrum = App.loadShaderFromClasspath("shaders/blendSkyboxStars.vert", "shaders/blendSkyboxStars.frag")
    private val shaderClouds = App.loadShaderFromClasspath("shaders/default.vert", "shaders/clouds.frag")

    private var astrumOffX = 0f
    private var astrumOffY = 0f

    private val clouds = SortedArrayList<WeatherObjectCloud>()
    var cloudsSpawned = 0; private set
    private var windVector = Vector3(-1f, 0f, 0.1f) // this is a direction vector
    val cloudSpawnMax: Int
        get() = 256 shl (App.getConfigInt("maxparticles") / 256)

    override fun loadFromSave(s0: Long, s1: Long) {
        super.loadFromSave(s0, s1)
        currentWeather = weatherList[WEATHER_GENERIC]!![0]
        internalReset(s0, s1)
        initClouds()
    }

    fun internalReset() {
        currentWeather = weatherList[WEATHER_GENERIC]!![0]
        internalReset(RNG.state0, RNG.state1)
        initClouds()
    }

    fun internalReset(s0: Long, s1: Long) {
        globalLightOverridden = false
        forceTimeAt = null
        forceSolarElev = null
        forceTurbidity = null

        astrumOffX = s0.and(0xFFFFL).toFloat() / 65535f * starmapTex.regionWidth
        astrumOffY = s1.and(0xFFFFL).toFloat() / 65535f * starmapTex.regionHeight

        clouds.clear()
        cloudsSpawned = 0
        windVector = Vector3(-0.98f, 0f, 0.21f)

        windDirWindow = null
        windSpeedWindow = null

        oldCamPos.set(WorldCamera.camVector)
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
            weatherList["titlescreen"] = arrayListOf(weatherList[WEATHER_GENERIC]!![0].copy(windSpeed = 1f))
            currentWeather = weatherList[WEATHER_GENERIC]!![0]
            nextWeather = getRandomWeather(WEATHER_GENERIC)
        }
        catch (e: NullPointerException) {
            e.printStackTrace()

            val defaultWeather = BaseModularWeather(
                JsonValue(JsonValue.ValueType.`object`),
                GdxColorMap(1, 3, Color(0x55aaffff), Color(0xaaffffff.toInt()), Color.WHITE),
                GdxColorMap(2, 2, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE),
                "default",
                0f,
                0f,
                0f,
                Vector2(1f, 1f),
                Vector2(0f, 0f),
                listOf()
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

        updateWind(delta, world)
        updateClouds(delta, world)


        if (!globalLightOverridden) {
            world.globalLight = WeatherMixer.globalLightNow
        }

    }

    private fun FloatArray.shiftAndPut(f: Float) {
        for (k in 1 until this.size) {
            this[k-1] = this[k]
        }
        this[this.lastIndex] = f
    }

    private val PI = 3.1415927f
    private val TWO_PI = 6.2831855f
    private val THREE_PI = 9.424778f

    private var windDirWindow: FloatArray? = null
    private var windSpeedWindow: FloatArray? = null
    private val WIND_DIR_TIME_UNIT = 14400 // every 4hr
    private val WIND_SPEED_TIME_UNIT = 3600 // every 1hr
    private var windDirAkku = 0 // only needed if timeDelta is not divisible by WIND_TIME_UNIT
    private var windSpeedAkku = 0

    // see: https://stackoverflow.com/questions/2708476/rotation-interpolation/14498790#14498790
    private fun getShortestAngle(start: Float, end: Float) =
        (((((end - if (start < 0f) TWO_PI + start else start) % TWO_PI) + THREE_PI) % TWO_PI) - PI).let {
            if (it > PI) it - TWO_PI else it
        }

    private fun updateWind(delta: Float, world: GameWorld) {
        if (windDirWindow == null) {
            windDirWindow = FloatArray(4) { takeUniformRand(-PI..PI) } // completely random regardless of the seed
        }
        if (windSpeedWindow == null) {
            windSpeedWindow = FloatArray(4) { currentWeather.getRandomWindSpeed(takeTriangularRand(-1f..1f)) } // completely random regardless of the seed
        }

        val windDirStep = windDirAkku / WIND_DIR_TIME_UNIT.toFloat()
        val windSpeedStep = windSpeedAkku / WIND_SPEED_TIME_UNIT.toFloat()

//        val angle0 = windDirWindow[0]
//        val angle1 = getShortestAngle(angle0, windDirWindow[1])
//        val angle2 = getShortestAngle(angle1, windDirWindow[2])
//        val angle3 = getShortestAngle(angle2, windDirWindow[3])
//        val fixedAngles = floatArrayOf(angle0, angle1, angle2, angle3)

        val currentWindDir = FastMath.interpolateCatmullRom(windDirStep, windDirWindow)
        val currentWindSpeed = FastMath.interpolateCatmullRom(windSpeedStep, windSpeedWindow)
        /*
        printdbg(this,
            "dir ${Math.toDegrees(currentWindDir.toDouble()).roundToInt()}\t" +
                    "spd ${currentWindSpeed.times(10f).roundToInt().div(10f)}\t   " +
                    "dirs ${windDirWindow!!.map { Math.toDegrees(it.toDouble()).roundToInt() }} ${windDirStep.times(100).roundToInt()}\t" +
                    "spds ${windSpeedWindow!!.map { it.times(10f).roundToInt().div(10f) }} ${windSpeedStep.times(100).roundToInt()}"
        )*/

        if (currentWeather.forceWindVec != null) {
            windVector.set(currentWeather.forceWindVec)
        }
        else {
            windVector.set(
                cos(currentWindDir) * currentWindSpeed,
                0f,
                sin(currentWindDir) * currentWindSpeed
            )
        }

        while (windDirAkku >= WIND_DIR_TIME_UNIT) {
            windDirAkku -= WIND_DIR_TIME_UNIT
            windDirWindow!!.shiftAndPut(takeUniformRand(-PI..PI))
        }
        while (windSpeedAkku >= WIND_SPEED_TIME_UNIT) {
            windSpeedAkku -= WIND_SPEED_TIME_UNIT
            windSpeedWindow!!.shiftAndPut(currentWeather.getRandomWindSpeed(takeTriangularRand(-1f..1f)))
        }

        windDirAkku += world.worldTime.timeDelta
        windSpeedAkku += world.worldTime.timeDelta
    }

    private val cloudParallaxMultY = -0.035f
    private val cloudParallaxMultX = -0.035f
    private var cloudUpdateAkku = 0f
    private val oldCamPos = Vector2(0f, 0f)
    private val camDelta = Vector2(0f, 0f)

    val oobMarginR =  1.5f * App.scr.wf
    val oobMarginL = -0.5f * App.scr.wf
    private val oobMarginY = -0.5f * App.scr.hf

    private val DEBUG_CAUSE_OF_DESPAWN = false

    private fun updateClouds(delta: Float, world: GameWorld) {
        val camvec = WorldCamera.camVector
        val camvec2 = camvec.cpy()
        val testCamDelta = camvec.cpy().sub(oldCamPos)

        if (testCamDelta.x.absoluteValue > world.width * TILE_SIZEF / 2f) {
            if (testCamDelta.x >= 0)
                camvec2.x -= world.width * TILE_SIZEF
            else
                camvec2.x += world.width * TILE_SIZEF

            testCamDelta.set(camvec2.sub(oldCamPos))
        }

        camDelta.set(testCamDelta)


        val cloudChanceEveryMin = 60f / (currentWeather.cloudChance * currentWeather.windSpeed) // if chance = 0, the result will be +inf

        while (cloudUpdateAkku >= cloudChanceEveryMin) {
            cloudUpdateAkku -= cloudChanceEveryMin
            tryToSpawnCloud(currentWeather)
        }


        var immDespawnCount = 0
        val immDespawnCauses = ArrayList<String>()
        clouds.forEach {
            // do parallax scrolling
            it.posX += camDelta.x * cloudParallaxMultX
            it.posY += camDelta.y * cloudParallaxMultY


            it.update(windVector)

            if (DEBUG_CAUSE_OF_DESPAWN && it.life == 0) {
                immDespawnCount += 1
                immDespawnCauses.add(it.despawnCode)
            }
        }

//        printdbg(this, "Newborn cloud death rate: $immDespawnCount/$cloudsSpawned")


        if (DEBUG_CAUSE_OF_DESPAWN && App.IS_DEVELOPMENT_BUILD && immDespawnCount > cloudsSpawned / 4) {
            val despawnCauseStats = HashMap<String, Int>()
            immDespawnCauses.forEach {
                if (despawnCauseStats[it] != null) {
                    despawnCauseStats[it] = despawnCauseStats[it]!! + 1
                }
                else {
                    despawnCauseStats[it] = 1
                }
            }
            despawnCauseStats.forEach { s, i ->
                printdbg(this, "Cause of death -- $s: $i")
            }
            System.exit(0)
        }


        // remove clouds that are marked to be despawn
        var i = 0
        while (true) {
            if (i >= clouds.size) break

            if (clouds[i].flagToDespawn) {
                clouds.removeAt(i)
                i -= 1
                cloudsSpawned -= 1
            }

            i += 1
        }


        cloudUpdateAkku += delta


        oldCamPos.set(camvec)
    }

    private val scrHscaler = App.scr.height / 720f
    private val cloudSizeMult = App.scr.wf / TerrarumScreenSize.defaultW

    private fun takeUniformRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear(Math.random().toFloat(), range.start, range.endInclusive)
    private fun takeTriangularRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear((Math.random() + Math.random()).div(2f).toFloat(), range.start, range.endInclusive)
    private fun takeGaussianRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear((Math.random() + Math.random() + Math.random() + Math.random() + Math.random() + Math.random() + Math.random() + Math.random()).div(8f).toFloat(), range.start, range.endInclusive)

    /**
     * Returns random point for clouds to spawn from, in the opposite side of the current wind vector
     */
    private fun getCloudSpawningPosition(cloud: CloudProps, halfCloudSize: Float, windVector: Vector3): Vector3 {
        val Z_LIM = ALPHA_ROLLOFF_Z/2f
        val y = takeUniformRand(-cloud.altHigh..-cloud.altLow) * scrHscaler

        var windVectorDir = toDegrees(atan2(windVector.z.toDouble(), windVector.x.toDouble())).toFloat() + 180f
        if (windVectorDir < 0f) windVectorDir += 360f
        windVectorDir /= 90f // full circle: 4

        // an "edge" is a line of length 1 drawn into the edge of the square of size 1 (its total edge length will be 4)
        // when the windVectorDir is not an integer, the "edge" will take the shape similar to this: ¬
        // 'rr' is a point on the "edge", where 0.5 is a middle point in its length
//        val rl = (windVectorDir % 1f).let { if (it < 0.5f) -it else it - 1f }.toInt()
//        val rh = 1 + (windVectorDir % 1f).let { if (it < 0.5f) it else 1f - it }.toInt()

        // choose between rl and rh using (windVectorDir % 1f) as a pivot
        // if pivot = 0.3, rL is 70%, and rR is 30% likely
        // plug the vote result into the when()
        val selectedQuadrant = takeUniformRand(windVectorDir..windVectorDir + 1f)

//        printdbg(this, "Dir: $windVectorDir, Rand(${windVectorDir}..${windVectorDir + 1f}) = ${selectedQuadrant.floorToInt()}($selectedQuadrant)")

        val rr = takeUniformRand(0f..1f)

        return when (selectedQuadrant.floorToInt()) {
            -4, 0, 4 -> { // right side of the screen
                val z = FastMath.interpolateLinear(rr, 1f, ALPHA_ROLLOFF_Z).pow(1.5f) // clouds are more likely to spawn with low Z-value
                val posXscr = App.scr.width + halfCloudSize
                val x = WeatherObjectCloud.screenXtoWorldX(posXscr, z)
                Vector3(x, y, z)
            }
            -3, 1, 5 -> { // z = inf
                val z = ALPHA_ROLLOFF_Z
                val posXscr = FastMath.interpolateLinear(rr, App.scr.width + halfCloudSize, -halfCloudSize)
                val x = WeatherObjectCloud.screenXtoWorldX(posXscr, Z_LIM)
                Vector3(x, y, z)
            }
            -2, 2, 6 -> { // left side of the screen
                val z = FastMath.interpolateLinear(rr, ALPHA_ROLLOFF_Z, 1f).pow(1.5f) // clouds are more likely to spawn with low Z-value
                val posXscr = -halfCloudSize
                val x = WeatherObjectCloud.screenXtoWorldX(posXscr, z)
                Vector3(x, y, z)
            }
            -1, 3, 7 -> { // z = 0
                val z = 0.1f
                val posXscr = FastMath.interpolateLinear(rr, -halfCloudSize, App.scr.width + halfCloudSize)
                val x = WeatherObjectCloud.screenXtoWorldX(posXscr, Z_LIM)
                Vector3(x, y, z)
            }
            else -> throw InternalError()
        }

    }

    private fun tryToSpawnCloud(currentWeather: BaseModularWeather, precalculatedPos: Vector3? = null) {
//        printdbg(this, "Trying to spawn a cloud... (${cloudsSpawned} / ${cloudSpawnMax})")

        if (cloudsSpawned < cloudSpawnMax) {
            val flip = Math.random() < 0.5
            val rC = takeUniformRand(0f..1f)
//            val rZ = takeUniformRand(1f..ALPHA_ROLLOFF_Z/4f).pow(1.5f) // clouds are more likely to spawn with low Z-value
//            val rY = takeUniformRand(-1f..1f)
            val rT1 = takeTriangularRand(-1f..1f)
            val (rA, rB) = doubleToLongBits(Math.random()).let {
                it.ushr(20).and(0xFFFF).toInt() to it.ushr(36).and(0xFFFF).toInt()
            }

            var cloudsToSpawn: CloudProps? = null
            var c = 0
            while (c < currentWeather.clouds.size) {
                if (rC < currentWeather.clouds[c].probability) {
                    cloudsToSpawn = currentWeather.clouds[c]
                    break
                }
                c += 1
            }

            cloudsToSpawn?.let { cloud ->
                val cloudScale = cloud.getCloudScaleVariance(rT1)
                val hCloudSize = (cloud.spriteSheet.tileW * cloudScale) / 2f + 1f

//                val posXscr = initX ?: if (cloudDriftVector.x < 0) (App.scr.width + hCloudSize) else -hCloudSize
//                val posX = WeatherObjectCloud.screenXtoWorldX(posXscr, rZ)
//                val posY = randomPosWithin(-cloud.altHigh..-cloud.altLow, rY) * scrHscaler

                val sheetX = rA % cloud.spriteSheet.horizontalCount
                val sheetY = rB % cloud.spriteSheet.verticalCount
                WeatherObjectCloud(cloud.spriteSheet.get(sheetX, sheetY), flip).also {
                    it.scale = cloudScale * cloudSizeMult

                    it.pos.set(precalculatedPos ?: getCloudSpawningPosition(cloud, hCloudSize, windVector))

                    // further set the random altitude if required
                    if (precalculatedPos != null) {
                        it.pos.y = takeUniformRand(-cloud.altHigh..-cloud.altLow) * scrHscaler
                    }

                    clouds.add(it)
                    cloudsSpawned += 1


//                    printdbg(this, "... Spawning ${cloud.category}($sheetX, $sheetY) cloud at pos ${it.pos}, scale ${it.scale}, invGamma ${it.darkness}")
                }

            }
        }
    }

    private fun initClouds() {
        val hCloudSize = 1024f
        // multiplier is an empirical value that depends on the 'rZ'
        // it does converge at ~6, but having it as an initial state does not make it stay converged
        repeat((currentWeather.cloudChance * 1.333f).ceilToInt()) {

            val z = takeUniformRand(0.1f..ALPHA_ROLLOFF_Z / 4f - 0.1f).pow(1.5f) // clouds are more likely to spawn with low Z-value

            val zz = FastMath.interpolateLinear((z / ALPHA_ROLLOFF_Z) * 0.8f + 0.1f, ALPHA_ROLLOFF_Z / 4f, ALPHA_ROLLOFF_Z)

            val x = WeatherObjectCloud.screenXtoWorldX(takeUniformRand(0f..App.scr.wf), zz)


            tryToSpawnCloud(currentWeather, Vector3(x, 0f, z))
        }
    }

    internal fun titleScreenInitWeather() {
        currentWeather = weatherList["titlescreen"]!![0]
        currentWeather.forceWindVec = Vector3(-0.98f, 0f, -0.21f)
        initClouds()
    }

    private fun <T> Array<T?>.addAtFreeSpot(obj: T) {
        var c = 0
        while (true) {
            if (this[c] == null) break
            c += 1
        }
        this[c] = obj
    }


    var turbidity = 1.0; private set
    private var gH = 1.8f * App.scr.height
//    private var gH = 0.8f * App.scr.height

    internal var parallaxPos = 0f; private set

    private val HALF_DAY = DAY_LENGTH / 2
    /**
     * Sub-portion of IngameRenderer. You are not supposed to directly deal with this.
     */
    internal fun render(camera: Camera, batch: FlippingSpriteBatch, world: GameWorld) {
        drawSkybox(camera, batch, world)
        drawClouds(batch)
        batch.color = Color.WHITE
    }

    private fun drawClouds(batch: SpriteBatch) {
        batch.inUse { _ ->
            batch.shader = shaderClouds
            batch.shader.setUniformf("gamma", currentWeather.cloudGamma)
            batch.shader.setUniformf("shadeCol", 0.06f, 0.07f, 0.08f, 1f) // TODO temporary value

            clouds.forEach {
                batch.color = Color(globalLightNow.r, globalLightNow.g, globalLightNow.b, it.alpha)
                it.render(batch, 0f, 0f)
            }
        }
    }

    private val parallaxDomainSize = 400f
    private val turbidityDomainSize = 533.3333f

    private fun drawSkybox(camera: Camera, batch: FlippingSpriteBatch, world: GameWorld) {
        val parallaxZeroPos = (world.height / 3f)

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
        val turbidityCoeff = ((parallaxZeroPos - WorldCamera.gdxCamY.div(TILE_SIZEF)) / turbidityDomainSize).times(-1f).coerceIn(-1f, 1f)
        parallaxPos = parallax
//        println(parallax) // parallax value works as intended.

        gdxBlendNormalStraightAlpha()

        turbidity = (3.5 + turbidityCoeff * 2.5).coerceIn(1.0, 6.0)
        val thisTurbidity = forceTurbidity ?: turbidity

        val gradY = -(gH - App.scr.height) * ((parallax + 1f) / 2f)

        val (tex, uvs, turbX, albX) = Skybox.getUV(solarElev, thisTurbidity, 0.3)

        val mornNoonBlend = (1f/4000f * (timeNow - 43200) + 0.5f).coerceIn(0f, 1f) // 0.0 at T41200; 0.5 at T43200; 1.0 at T45200;

        starmapTex.texture.bind(1)
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it

        val astrumX = world.worldTime.axialTiltDeg.toFloat() * starmapTex.regionWidth / 150f
        val astrumY = ((world.worldTime.TIME_T / WorldTime.DIURNAL_MOTION_LENGTH) % 1f) * starmapTex.regionHeight

        batch.inUse {
            batch.shader = shaderAstrum
            shaderAstrum.setUniformi("tex1", 1)
            shaderAstrum.setUniformf("drawOffsetSize", 0f, gradY, App.scr.wf, gH)
            shaderAstrum.setUniform4fv("uvA", uvs, 0, 4)
            shaderAstrum.setUniform4fv("uvB", uvs, 4, 4)
            shaderAstrum.setUniform4fv("uvC", uvs, 8, 4)
            shaderAstrum.setUniform4fv("uvD", uvs, 12, 4)
            shaderAstrum.setUniform4fv("uvE", uvs, 16, 4)
            shaderAstrum.setUniform4fv("uvF", uvs, 20, 4)
            shaderAstrum.setUniform4fv("uvG", uvs, 24, 4)
            shaderAstrum.setUniform4fv("uvH", uvs, 28, 4)
            shaderAstrum.setUniformf("texBlend", mornNoonBlend, turbX, albX, 0f)
            shaderAstrum.setUniformf("astrumScroll", astrumOffX + astrumX, astrumOffY + astrumY)
            shaderAstrum.setUniformf("randomNumber",
//                (world.worldTime.TIME_T.plus(31L) xor 1453L + 31L).and(1023).toFloat(),
//                (world.worldTime.TIME_T.plus(37L) xor  862L + 31L).and(1023).toFloat(),
//                (world.worldTime.TIME_T.plus(23L) xor 1639L + 29L).and(1023).toFloat(),
//                (world.worldTime.TIME_T.plus(29L) xor 2971L + 41L).and(1023).toFloat(),
                world.worldTime.TIME_T.div(+14.1f).plus(31L),
                world.worldTime.TIME_T.div(-13.8f).plus(37L),
                world.worldTime.TIME_T.div(+13.9f).plus(23L),
                world.worldTime.TIME_T.div(-14.3f).plus(29L),
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

        val skybox = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${skyboxInJson}"))
        val daylight = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${lightbox}"))


        val classification = JSON.getString("classification")


        val cloudsMap = ArrayList<CloudProps>()
        val clouds = JSON["clouds"]
        clouds.forEachSiblings { name, json ->
            cloudsMap.add(CloudProps(
                name,
                TextureRegionPack(ModMgr.getGdxFile(modname, "$pathToImage/${json.getString("filename")}"), json.getInt("tw"), json.getInt("th")),
                json.getFloat("probability"),
                json.getFloat("baseScale"),
                json.getFloat("scaleVariance"),
                json.getFloat("altLow"),
                json.getFloat("altHigh"),
            ))
        }
        cloudsMap.sortBy { it.probability }









        var mixFrom: String?
        try { mixFrom = JSON.getString("mixFrom") }
        catch (e: IllegalArgumentException) { mixFrom = null }



        var mixPercentage: Double?
        try { mixPercentage = JSON.getDouble("mixPercentage") }
        catch (e: IllegalArgumentException) { mixPercentage = null }



        return BaseModularWeather(
            json = JSON,
            skyboxGradColourMap = skybox,
            daylightClut = daylight,
            classification = classification,
            cloudChance = JSON.getFloat("cloudChance"),
            windSpeed = JSON.getFloat("windSpeed"),
            windSpeedVariance = JSON.getFloat("windSpeedVariance"),
            cloudGamma = JSON["cloudGamma"].asFloatArray().let { Vector2(it[0], it[1]) },
            cloudGammaVariance = JSON["cloudGammaVariance"].asFloatArray().let { Vector2(it[0], it[1]) },
            clouds = cloudsMap,
        )
    }

    fun dispose() {
        weatherList.values.forEach { list ->
            list.forEach { weather ->
                weather.clouds.forEach { it.spriteSheet.dispose() }
            }
        }
        starmapTex.texture.dispose()
        shaderAstrum.dispose()
        shaderClouds.dispose()
    }
}

