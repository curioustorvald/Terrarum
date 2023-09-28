package net.torvald.terrarum.weather

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.UnpackedColourSpriteBatch
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
import net.torvald.terrarum.clut.Skybox.elevCnt
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.forEachSiblings
import net.torvald.terrarum.weather.WeatherObjectCloud.Companion.ALPHA_ROLLOFF_Z
import net.torvald.terrarum.weather.WeatherObjectCloud.Companion.NEWBORN_GROWTH_TIME
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

    val DEFAULT_WEATHER = BaseModularWeather(
        "default",
        JsonValue(JsonValue.ValueType.`object`),
        GdxColorMap(1, 3, Color(0x55aaffff), Color(0xaaffffff.toInt()), Color.WHITE),
        GdxColorMap(2, 2, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE),
        "default",
        0f,
        0f,
        0f,
        0f,
        Vector2(1f, 1f),
        Vector2(0f, 0f),
        listOf(),
        floatArrayOf(1f, 1f)
    )

    override val RNG = HQRNG()

    var globalLightOverridden = false

    val weatherDB: HashMap<String, ArrayList<BaseModularWeather>> // search by classification
    val weatherDict: HashMap<String, BaseModularWeather> // search by identifier

    private var forceWindVec: Vector3? = null

    val globalLightNow = Cvec(0)
//    private val cloudDrawColour = Color()
    private val moonlightMax = Cvec(
        0.23f,
        0.24f,
        0.25f,
        0.21f
    ) // actual moonlight is around ~4100K but our mesopic vision makes it appear blueish (wikipedia: Purkinje effect)

    // Weather indices
    const val WEATHER_GENERIC = "generic"
    const val WEATHER_OVERCAST = "overcast"
    // TODO add weather classification indices manually

    // TODO to save from GL overhead, store lightmap to array; use GdxColorMap

    var forceTimeAt: Int? = null
    var forceSolarElev: Double? = null
    var forceTurbidity: Double? = null

    // doesn't work if the png is in greyscale/indexed mode
    val starmapTex: TextureRegion = TextureRegion(Texture(Gdx.files.internal("assets/graphics/astrum.png"))).also {
        it.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        it.texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
    }

    private val shaderAstrum =
        App.loadShaderFromClasspath("shaders/blendSkyboxStars.vert", "shaders/blendSkyboxStars.frag")
    private val shaderClouds = App.loadShaderFromClasspath("shaders/default.vert", "shaders/clouds.frag")

    private var astrumOffX = 0f
    private var astrumOffY = 0f

    // Clouds are merely a response to the current Weatherbox status //

    private val clouds = SortedArrayList<WeatherObjectCloud>()

    var cloudsSpawned = 0; private set
    private var windVector = Vector3(-1f, 0f, 0.1f) // this is a direction vector
    val cloudSpawnMax: Int
        get() = 256 shl (App.getConfigInt("maxparticles") / 256)


    private val skyboxavr = GdxColorMap(Gdx.files.internal("assets/clut/skyboxavr.png"))


    override fun loadFromSave(ingame: IngameInstance, s0: Long, s1: Long) {
        super.loadFromSave(ingame, s0, s1)
        internalReset(s0, s1)
        initClouds(ingame.world.weatherbox.currentWeather)
    }

    fun internalReset(ingame: IngameInstance) {
        internalReset(RNG.state0, RNG.state1)
        initClouds(ingame.world.weatherbox.currentWeather)
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
        forceWindVec = null
        windVector = Vector3(-0.98f, 0f, 0.21f)

        oldCamPos.set(WorldCamera.camVector)
    }

    init {
        weatherDB = HashMap<String, ArrayList<BaseModularWeather>>()
        weatherDict = HashMap<String, BaseModularWeather>()


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

            weatherDict[weather.identifier] = weather

            // if List for the classification does not exist, make one
            if (!weatherDB.containsKey(weather.classification))
                weatherDB.put(weather.classification, ArrayList())

            weatherDB[weather.classification]!!.add(weather)
        }

        weatherDict["titlescreen"] = weatherDB[WEATHER_GENERIC]!![0].copy(identifier = "titlescreen", windSpeed = 1f)
    }

    /**
     * Part of Ingame update
     */
    fun update(delta: Float, player: ActorWithBody?, world: GameWorld) {
        if (player == null) return

//        currentWeather = weatherList[WEATHER_GENERIC]!![0] // force set weather

        world.weatherbox.update(world)
        updateWind(world.weatherbox)
        updateClouds(delta, world)


        if (!globalLightOverridden) {
            world.globalLight = WeatherMixer.globalLightNow
        }

    }

    private fun FloatArray.shiftAndPut(f: Float) {
        for (k in 1 until this.size) {
            this[k - 1] = this[k]
        }
        this[this.lastIndex] = f
    }

    private val HALF_PI = 1.5707964f
    private val PI = 3.1415927f
    private val TWO_PI = 6.2831855f
    private val THREE_PI = 9.424778f


    // see: https://stackoverflow.com/questions/2708476/rotation-interpolation/14498790#14498790
    private fun getShortestAngle(start: Float, end: Float) =
        (((((end - if (start < 0f) TWO_PI + start else start) % TWO_PI) + THREE_PI) % TWO_PI) - PI).let {
            if (it > PI) it - TWO_PI else it
        }

    private fun updateWind(weatherbox: Weatherbox) {
        val currentWindSpeed = weatherbox.windSpeed.value
        val currentWindDir = weatherbox.windDir.value * HALF_PI

//        printdbg(this, "Wind speed = $currentWindSpeed")

        if (forceWindVec != null) {
            windVector.set(forceWindVec)
        }
        else {
            windVector.set(
                (cos(currentWindDir) * currentWindSpeed),
                0f,
                (sin(currentWindDir) * currentWindSpeed)
            )
        }
    }

    private val cloudParallaxMultY = -0.035f
    private val cloudParallaxMultX = -0.035f
    private var cloudUpdateAkku = 0f
    private val oldCamPos = Vector2(0f, 0f)
    private val camDelta = Vector2(0f, 0f)

    val oobMarginR = 1.5f * App.scr.wf
    val oobMarginL = -0.5f * App.scr.wf
    private val oobMarginY = -0.5f * App.scr.hf

    private val DEBUG_CAUSE_OF_DESPAWN = false

    private fun updateClouds(delta: Float, world: GameWorld) {
        val camvec = WorldCamera.camVector
        val camvec2 = camvec.cpy()
        val testCamDelta = camvec.cpy().sub(oldCamPos)
        val currentWeather = world.weatherbox.currentWeather

        // adjust camDelta to accomodate ROUNDWORLD
        if (testCamDelta.x.absoluteValue > world.width * TILE_SIZEF / 2f) {
            if (testCamDelta.x >= 0)
                camvec2.x -= world.width * TILE_SIZEF
            else
                camvec2.x += world.width * TILE_SIZEF

            testCamDelta.set(camvec2.sub(oldCamPos))
        }

        camDelta.set(testCamDelta)

        // try to spawn an cloud
        val cloudChanceEveryMin =
            60f / (currentWeather.cloudChance * currentWeather.windSpeed) // if chance = 0, the result will be +inf

        while (cloudUpdateAkku >= cloudChanceEveryMin) {
            cloudUpdateAkku -= cloudChanceEveryMin
            val newCloud = tryToSpawnCloud(currentWeather)
//            printdbg(this, "New cloud: scrX,Y,Scale=${newCloud?.screenCoord};\tworldXYZ=${newCloud?.pos}")
        }


        var immDespawnCount = 0
        val immDespawnCauses = ArrayList<String>()
//        printdbg(this, "Wind vector = $windVector")
        // move the clouds
        clouds.forEach {
            // do parallax scrolling
            it.posX += camDelta.x * cloudParallaxMultX
            it.posY += camDelta.y * cloudParallaxMultY

            it.update(world, windVector)

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


        cloudUpdateAkku += delta * world.worldTime.timeDelta


        oldCamPos.set(camvec)
    }

    private val scrHscaler = App.scr.height / 720f
    private val cloudSizeMult = App.scr.wf / TerrarumScreenSize.defaultW

    fun takeUniformRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear(Math.random().toFloat(), range.start, range.endInclusive)

    fun takeTriangularRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear((Math.random() + Math.random()).div(2f).toFloat(), range.start, range.endInclusive)

    fun takeGaussianRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear(
            (Math.random() + Math.random() + Math.random() + Math.random() + Math.random() + Math.random() + Math.random() + Math.random()).div(
                8f
            ).toFloat(), range.start, range.endInclusive
        )

    /**
     * Returns random point for clouds to spawn from, in the opposite side of the current wind vector
     */
    private fun getCloudSpawningPosition(cloud: CloudProps, halfCloudSize: Float, windVector: Vector3): Vector3 {
        val Z_LIM = ALPHA_ROLLOFF_Z
        val Z_POW_BASE = ALPHA_ROLLOFF_Z / 4f
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
                val z = FastMath.interpolateLinear(rr, 1f, Z_POW_BASE)
                    .pow(1.5f) // clouds are more likely to spawn with low Z-value
                val posXscr = App.scr.width + halfCloudSize
                val x = WeatherObjectCloud.screenXtoWorldX(posXscr, z)
                Vector3(x, y, z)
            }

            -3, 1, 5 -> { // z = inf
                val z = ALPHA_ROLLOFF_Z
                val posXscr = FastMath.interpolateLinear(rr, -halfCloudSize, App.scr.width + halfCloudSize)
                val x = WeatherObjectCloud.screenXtoWorldX(posXscr, Z_LIM)
                Vector3(x, y, z)
            }

            -2, 2, 6 -> { // left side of the screen
                val z = FastMath.interpolateLinear(rr, Z_POW_BASE, 1f)
                    .pow(1.5f) // clouds are more likely to spawn with low Z-value
                val posXscr = -halfCloudSize
                val x = WeatherObjectCloud.screenXtoWorldX(posXscr, z)
                Vector3(x, y, z)
            }

            -1, 3, 7 -> { // z = 0
                val posXscr = FastMath.interpolateLinear(rr, -halfCloudSize, App.scr.width + halfCloudSize)
                val z = WeatherObjectCloud.worldYtoWorldZforScreenYof0(y)
                val x = WeatherObjectCloud.screenXtoWorldX(posXscr, Z_LIM)
                Vector3(x, y, z)
            }

            else -> throw InternalError()
        }

    }

    private fun tryToSpawnCloud(
        currentWeather: BaseModularWeather,
        precalculatedPos: Vector3? = null,
        ageOverride: Int = 0
    ): WeatherObjectCloud? {
//        printdbg(this, "Trying to spawn a cloud... (${cloudsSpawned} / ${cloudSpawnMax})")

        return if (cloudsSpawned < cloudSpawnMax) {
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

                val cloudGamma = currentWeather.getRandomCloudGamma(takeUniformRand(-1f..1f), takeUniformRand(-1f..1f))

                WeatherObjectCloud(
                    cloud.spriteSheet.get(sheetX, sheetY),
                    flip,
                    cloudGamma.x,
                    cloudGamma.y
                ).also {
                    it.scale = cloudScale * cloudSizeMult

                    it.pos.set(precalculatedPos ?: getCloudSpawningPosition(cloud, hCloudSize, windVector))

//                    if (precalculatedPos == null) printdbg(this, "Z=${it.posZ}")

                    // further set the random altitude if required
                    if (precalculatedPos != null) {
                        it.pos.y = takeUniformRand(-cloud.altHigh..-cloud.altLow) * scrHscaler
                    }

                    it.life = ageOverride

                    clouds.add(it)
                    cloudsSpawned += 1


//                    printdbg(this, "... Spawning ${cloud.category}($sheetX, $sheetY) cloud at pos ${it.pos}, scale ${it.scale}, invGamma ${it.darkness}")
                }
            }
        }
        else null
    }

    private fun initClouds(currentWeather: BaseModularWeather) {
        clouds.clear()
        cloudsSpawned = 0
        // multiplier is an empirical value that depends on the 'rZ'
        // it does converge at ~6, but having it as an initial state does not make it stay converged
        repeat((currentWeather.cloudChance * 1.333f).ceilToInt()) {

            val z =
                takeUniformRand(0.1f..ALPHA_ROLLOFF_Z / 4f - 0.1f).pow(1.5f) // clouds are more likely to spawn with low Z-value

            val zz =
                FastMath.interpolateLinear((z / ALPHA_ROLLOFF_Z) * 0.8f + 0.1f, ALPHA_ROLLOFF_Z / 4f, ALPHA_ROLLOFF_Z)

            val x = WeatherObjectCloud.screenXtoWorldX(takeUniformRand(0f..App.scr.wf), zz)


            tryToSpawnCloud(currentWeather, Vector3(x, 0f, z), NEWBORN_GROWTH_TIME.toInt())
        }
    }

    internal fun titleScreenInitWeather(weatherbox: Weatherbox) {
        weatherbox.initWith(weatherDict["titlescreen"]!!, Long.MAX_VALUE)
        forceWindVec = Vector3(
            -0.98f,
            0f,
            -0.21f
        ).scl(1f / 30f) // value taken from TitleScreen.kt; search for 'demoWorld.worldTime.timeDelta = '
        initClouds(weatherbox.currentWeather)
    }

    private fun <T> Array<T?>.addAtFreeSpot(obj: T) {
        var c = 0
        while (true) {
            if (this[c] == null) break
            c += 1
        }
        this[c] = obj
    }


    private var turbidity0 = 1.0
    private var turbidity1 = 1.0

    private var mornNoonBlend = 0f

    /** Interpolated value, controlled by the weatherbox */
    var turbidity = 1.0; private set

    /** Controlled by todo: something that monitors ground tile compisition */
    var albedo = 1.0; private set

    private var gH = 1.8f * App.scr.height
//    private var gH = 0.8f * App.scr.height

    internal var parallaxPos = 0f; private set
    private var solarElev = 0.0
    private val HALF_DAY = DAY_LENGTH / 2

    /**
     * Sub-portion of IngameRenderer. You are not supposed to directly deal with this.
     */
    internal fun render(camera: OrthographicCamera, batch: FlippingSpriteBatch, world: GameWorld) {
        solarElev = if (forceSolarElev != null)
            forceSolarElev!!
        else if (forceTimeAt != null)
            world.worldTime.getSolarElevationAt(world.worldTime.ordinalDay, forceTimeAt!!)
        else
            world.worldTime.solarElevationDeg

        drawSkybox(camera, batch, world)
        drawClouds(batch, world)
        batch.color = Color.WHITE
    }

    private val RECIPROCAL_OF_APPARENT_SOLAR_Y_AT_45DEG = 0.00000785
    private val APPARENT_SOLAR_Y_AT_45DEG = 1.0 / RECIPROCAL_OF_APPARENT_SOLAR_Y_AT_45DEG

    /**
     * Mathematical model: https://www.desmos.com/calculator/cf6wqwltqq
     */
    private fun cloudYtoSolarAlt(cloudY: Double, currentsolarDeg: Double): Double {
        fun a(x: Double) = APPARENT_SOLAR_Y_AT_45DEG * tan(Math.toRadians(x))
        fun g(x: Double) = Math.toDegrees(atan(RECIPROCAL_OF_APPARENT_SOLAR_Y_AT_45DEG * x))
        val phi = currentsolarDeg + CLOUD_SOLARDEG_OFFSET
        val x = cloudY
        return g(x + a(phi)).bipolarClamp(Skybox.elevMax)
    }

    /**
     * Dependent on the `drawSkybox(camera, batch, world)` for the `cloudDrawColour`
     *
     */
    private fun drawClouds(batch: SpriteBatch, world: GameWorld) {
        val currentWeather = world.weatherbox.currentWeather
        val timeNow = (forceTimeAt ?: world.worldTime.TIME_T.toInt()) % WorldTime.DAY_LENGTH

        batch.inUse { _ ->
            batch.shader = shaderClouds
            val shadeLum = (globalLightNow.r * 3f + globalLightNow.g * 4f + globalLightNow.b * 1f) / 8f * 0.5f
            batch.shader.setUniformf("shadeCol", shadeLum * 1.05f, shadeLum, shadeLum / 1.05f, 1f)

            clouds.forEach {
                val altOfSolarRay = cloudYtoSolarAlt(it.posY*-1.0, solarElev)

                val cloudCol1 = getGradientCloud(skyboxavr, solarElev, mornNoonBlend.toDouble(), turbidity, albedo)
                val cloudCol2 = getGradientColour2(currentWeather.daylightClut, altOfSolarRay, timeNow, 4)
                val cloudDrawColour = lerp(0.75, cloudCol1, cloudCol2) // no srgblerp for performance

                val shadiness = (1.0 / cosh(altOfSolarRay * 0.5)).toFloat().coerceAtLeast(if (altOfSolarRay < 0) 0.6666f else 0f)

//                printdbg(this, "cloudY=${-it.posY}\tsolarElev=$solarElev\trayAlt=$altOfSolarRay\tshady=$shadiness")
                it.render(batch as UnpackedColourSpriteBatch, cloudDrawColour.toGdxColor(), shadiness)
            }
        }
    }

    private val parallaxDomainSize = 550f
    private val turbidityDomainSize = parallaxDomainSize * 1.3333334f

    private val CLOUD_SOLARDEG_OFFSET = -0.0f

    private val globalLightBySun = Cvec()
    private val globalLightByMoon = Cvec()

    private fun drawSkybox(camera: OrthographicCamera, batch: FlippingSpriteBatch, world: GameWorld) {
        val weatherbox = world.weatherbox
        val currentWeather = world.weatherbox.currentWeather
        val parallaxZeroPos = (world.height * 0.4f)

        // we will not care for nextSkybox for now
        val timeNow = (forceTimeAt ?: world.worldTime.TIME_T.toInt()) % WorldTime.DAY_LENGTH
        val daylightClut = currentWeather.daylightClut
        // calculate global light
        val moonSize = (-(2.0 * world.worldTime.moonPhase - 1.0).abs() + 1.0).toFloat()
        globalLightBySun.set(getGradientColour2(daylightClut, solarElev, timeNow))
        globalLightByMoon.set(moonlightMax * moonSize)
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
        val parallax =
            ((parallaxZeroPos - WorldCamera.gdxCamY.div(TILE_SIZEF)) / parallaxDomainSize).times(-1f).coerceIn(-1f, 1f)
        val turbidityCoeff =
            ((parallaxZeroPos - WorldCamera.gdxCamY.div(TILE_SIZEF)) / turbidityDomainSize).times(-1f).coerceIn(-1f, 1f)
        parallaxPos = parallax
//        println(parallax) // parallax value works as intended.

        gdxBlendNormalStraightAlpha()

        val oldNewBlend = weatherbox.weatherBlend.times(2f).coerceAtMost(1f)
        mornNoonBlend =
            (1f / 4000f * (timeNow - 43200) + 0.5f).coerceIn(0f, 1f) // 0.0 at T41200; 0.5 at T43200; 1.0 at T45200;

        turbidity0 =
            (world.weatherbox.oldWeather.json.getDouble("atmoTurbidity") + turbidityCoeff * 2.5).coerceIn(1.0, 10.0)
        turbidity1 = (currentWeather.json.getDouble("atmoTurbidity") + turbidityCoeff * 2.5).coerceIn(1.0, 10.0)
        turbidity = FastMath.interpolateLinear(oldNewBlend.toDouble(), turbidity0, turbidity1)
        val oldTurbidity = forceTurbidity ?: turbidity0
        val thisTurbidity = forceTurbidity ?: turbidity1


        albedo = 0.3 // TODO() depends on the ground tile composition
        val oldAlbedo = forceTurbidity ?: turbidity0
        val thisAlbedo = forceTurbidity ?: turbidity1




        /*cloudCol1.set(getGradientCloud(skyboxavr, solarElev, mornNoonBlend.toDouble(), turbidity, albedo))
        cloudCol2.set(
            getGradientColour2(
                daylightClut,
                solarElev + CLOUD_SOLARDEG_OFFSET,
                timeNow
            ) max globalLightByMoon
        )
        cloudDrawColour.set(srgblerp(0.7, cloudCol1, cloudCol2))*/


        val gradY = -(gH - App.scr.height) * ((parallax + 1f) / 2f)

        val (tex, uvs, turbTihsBlend, albThisBlend, turbOldBlend, albOldBlend) = Skybox.getUV(
            solarElev,
            oldTurbidity,
            oldAlbedo,
            thisTurbidity,
            thisAlbedo
        )

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
            shaderAstrum.setUniformf("texBlend1", turbTihsBlend, albThisBlend, turbOldBlend, albOldBlend)
            shaderAstrum.setUniformf("texBlend2", oldNewBlend, mornNoonBlend, 0f, 0f)
            shaderAstrum.setUniformf("astrumScroll", astrumOffX + astrumX, astrumOffY + astrumY)
            shaderAstrum.setUniformf(
                "randomNumber",
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
    fun getGlobalLightOfTimeOfNoon(currentWeather: BaseModularWeather): Cvec {
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

    fun getGradientColour2(colorMap: GdxColorMap, solarAngleInDeg: Double, timeOfDay: Int, offY: Int = 0): Cvec {
        val pNowRaw = (solarAngleInDeg + 75.0) / 150.0 * colorMap.width

        val pStartRaw = pNowRaw.floorToInt()
        val pNextRaw = pStartRaw + 1

        val pSx: Int;
        val pSy: Int;
        val pNx: Int;
        val pNy: Int
        if (timeOfDay < HALF_DAY) {
            pSx = pStartRaw.coerceIn(0 until colorMap.width)
            pSy = 0
            if (pSx == colorMap.width - 1) {
                pNx = pSx; pNy = 1
            }
            else {
                pNx = pSx + 1; pNy = 0
            }
        }
        else {
            pSx = (pStartRaw + 1).coerceIn(0 until colorMap.width)
            pSy = 1
            if (pSx == 0) {
                pNx = 0; pNy = 0
            }
            else {
                pNx = pSx - 1; pNy = 1
            }
        }
        // interpolate R, G, B and A
        var scale = (pNowRaw - pStartRaw).toFloat()
        if (timeOfDay >= HALF_DAY) scale = 1f - scale

        val colourThisRGB = colorMap.get(pSx, pSy + offY)
        val colourNextRGB = colorMap.get(pNx, pNy + offY)
        val colourThisUV = colorMap.get(pSx, pSy + 2)
        val colourNextUV = colorMap.get(pNx, pNy + 2)

        val newColRGB =
            colourThisRGB.cpy().lerp(colourNextRGB, scale)//CIELuvUtil.getGradient(scale, colourThis, colourNext)
        val newColUV =
            colourThisUV.cpy().lerp(colourNextUV, scale)//CIELuvUtil.getGradient(scale, colourThis, colourNext)

        return Cvec(newColRGB, newColUV.r)
    }

    fun getGradientCloud(
        colorMap: GdxColorMap,
        solarAngleInDeg0: Double,
        mornNoonBlend: Double,
        turbidity: Double,
        albedo: Double
    ): Cvec {
        val solarAngleInDeg = solarAngleInDeg0 + CLOUD_SOLARDEG_OFFSET // add a small offset
        val solarAngleInDegInt = solarAngleInDeg.floorToInt()

        // fine-grained
        val angleX1 = (solarAngleInDegInt + 75).coerceAtMost(150)
        val angleX2 = (angleX1 + 1).coerceAtMost(150)
        val ax = solarAngleInDeg - solarAngleInDegInt
        // fine-grained
        val turbY = turbidity.coerceIn(Skybox.turbiditiesD.first(), Skybox.turbiditiesD.last()).minus(1.0)
            .times(Skybox.turbDivisor)
        val turbY1 = turbY.floorToInt()
        val turbY2 = (turbY1).coerceAtMost(Skybox.turbCnt - 1)
        val tx = turbY - turbY1
        // coarse-grained
        val albX =
            albedo.coerceIn(Skybox.albedos.first(), Skybox.albedos.last()).times(5.0) * Skybox.elevCnt // 0*151..5*151
        val albX1 = albX.floorToInt()
        val albX2 = (albX1 + 1).coerceAtMost(5 * Skybox.elevCnt)
        val bx = albX - albX1

        val a1t1b1A = colorMap.getCvec(albX1 * elevCnt + angleX1, turbY1)
        val a2t1b1A = colorMap.getCvec(albX1 * elevCnt + angleX2, turbY1)
        val a1t2b1A = colorMap.getCvec(albX1 * elevCnt + angleX1, turbY2)
        val a2t2b1A = colorMap.getCvec(albX1 * elevCnt + angleX2, turbY2)
        val a1t1b2A = colorMap.getCvec(albX2 * elevCnt + angleX1, turbY1)
        val a2t1b2A = colorMap.getCvec(albX2 * elevCnt + angleX2, turbY1)
        val a1t2b2A = colorMap.getCvec(albX2 * elevCnt + angleX1, turbY2)
        val a2t2b2A = colorMap.getCvec(albX2 * elevCnt + angleX2, turbY2)
        val a1t1b1B = colorMap.getCvec(albX1 * elevCnt + angleX1 + Skybox.albedoCnt * elevCnt, turbY1)
        val a2t1b1B = colorMap.getCvec(albX1 * elevCnt + angleX2 + Skybox.albedoCnt * elevCnt, turbY1)
        val a1t2b1B = colorMap.getCvec(albX1 * elevCnt + angleX1 + Skybox.albedoCnt * elevCnt, turbY2)
        val a2t2b1B = colorMap.getCvec(albX1 * elevCnt + angleX2 + Skybox.albedoCnt * elevCnt, turbY2)
        val a1t1b2B = colorMap.getCvec(albX2 * elevCnt + angleX1 + Skybox.albedoCnt * elevCnt, turbY1)
        val a2t1b2B = colorMap.getCvec(albX2 * elevCnt + angleX2 + Skybox.albedoCnt * elevCnt, turbY1)
        val a1t2b2B = colorMap.getCvec(albX2 * elevCnt + angleX1 + Skybox.albedoCnt * elevCnt, turbY2)
        val a2t2b2B = colorMap.getCvec(albX2 * elevCnt + angleX2 + Skybox.albedoCnt * elevCnt, turbY2)

        // no srgblerp here to match the skybox shader's behaviour

        val t1b1A = lerp(ax, a1t1b1A, a2t1b1A)
        val t2b1A = lerp(ax, a1t2b1A, a2t2b1A)
        val t1b2A = lerp(ax, a1t1b2A, a2t1b2A)
        val t2b2A = lerp(ax, a1t2b2A, a2t2b2A)
        val t1b1B = lerp(ax, a1t1b1B, a2t1b1B)
        val t2b1B = lerp(ax, a1t2b1B, a2t2b1B)
        val t1b2B = lerp(ax, a1t1b2B, a2t1b2B)
        val t2b2B = lerp(ax, a1t2b2B, a2t2b2B)

        val b1A = lerp(tx, t1b1A, t2b1A)
        val b2A = lerp(tx, t1b2A, t2b2A)
        val b1B = lerp(tx, t1b1B, t2b1B)
        val b2B = lerp(tx, t1b2B, t2b2B)

        val A = lerp(bx, b1A, b2A)
        val B = lerp(bx, b1B, b2B)

        return lerp(mornNoonBlend, A, B)
    }

    private fun lerp(x: Double, c1: Cvec, c2: Cvec): Cvec {
        // yes I'm well aware that I must do gamma correction before lerping but it's just tooooo slowwww
        val r = (((1.0 - x) * c1.r) + (x * c2.r)).toFloat()
        val g = (((1.0 - x) * c1.g) + (x * c2.g)).toFloat()
        val b = (((1.0 - x) * c1.b) + (x * c2.b)).toFloat()
        val a = (((1.0 - x) * c1.a) + (x * c2.a)).toFloat()
        return Cvec(r, g, b, a)
    }

    private fun srgblerp(x: Double, c1: Cvec, c2: Cvec): Cvec {
        return lerp(x, c1.linearise(), c2.linearise()).unlinearise()
    }

    fun getWeatherList(classification: String) = weatherDB[classification]!!
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



        return BaseModularWeather(
            identifier = JSON.getString("identifier"),
            json = JSON,
            skyboxGradColourMap = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${skyboxInJson}")),
            daylightClut = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${lightbox}")),
            classification = JSON.getString("classification"),
            cloudChance = JSON.getFloat("cloudChance"),
            windSpeed = JSON.getFloat("windSpeed"),
            windSpeedVariance = JSON.getFloat("windSpeedVariance"),
            windSpeedDamping = JSON.getFloat("windSpeedDamping"),
            cloudGamma = JSON["cloudGamma"].asFloatArray().let { Vector2(it[0], it[1]) },
            cloudGammaVariance = JSON["cloudGammaVariance"].asFloatArray().let { Vector2(it[0], it[1]) },
            clouds = cloudsMap,
            shaderVibrancy = JSON["shaderVibrancy"].asFloatArray()
        )
    }

    fun dispose() {
        weatherDB.values.forEach { list ->
            list.forEach { weather ->
                weather.clouds.forEach { it.spriteSheet.dispose() }
            }
        }
        starmapTex.texture.dispose()
        shaderAstrum.dispose()
        shaderClouds.dispose()
    }

    private fun Cvec.linearise(): Cvec {
        val newR = if (r > 0.04045f)
            ((r + 0.055f) / 1.055f).pow(2.4f)
        else r / 12.92f
        val newG = if (g > 0.04045f)
            ((g + 0.055f) / 1.055f).pow(2.4f)
        else g / 12.92f
        val newB = if (b > 0.04045f)
            ((b + 0.055f) / 1.055f).pow(2.4f)
        else b / 12.92f

        return Cvec(newR, newG, newB, a)
    }

    private fun Cvec.unlinearise(): Cvec {
        val newR = if (r > 0.0031308f)
            1.055f * r.pow(1f / 2.4f) - 0.055f
        else
            r * 12.92f
        val newG = if (g > 0.0031308f)
            1.055f * g.pow(1f / 2.4f) - 0.055f
        else
            g * 12.92f
        val newB = if (b > 0.0031308f)
            1.055f * b.pow(1f / 2.4f) - 0.055f
        else
            b * 12.92f

        return Cvec(newR, newG, newB, a)
    }
}

enum class GradientColourMode {
    DAYLIGHT, CLOUD_COLOUR
}

private fun Color.set(cvec: Cvec) {
    this.r = cvec.r
    this.g = cvec.g
    this.b = cvec.b
    this.a = cvec.a
}

