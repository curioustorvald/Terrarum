package net.torvald.terrarum.weather

import com.badlogic.gdx.math.Vector2
import com.jme3.math.FastMath
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.floorToInt
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import java.util.*
import kotlin.math.roundToLong

data class WeatherSchedule(val weather: BaseModularWeather = WeatherMixer.DEFAULT_WEATHER, val duration: Long = 3600)

class Weatherbox {

    companion object {
        private val WIND_DIR_TIME_UNIT = 3600f * 6 // every 6hr
        private val WIND_SPEED_TIME_UNIT = 360f * 5 // every 0.5hr

        private val HALF_PIF = 1.5707964f
        private val PIF = 3.1415927f
        private val TWO_PIF = 6.2831855f
        private val THREE_PIF = 9.424778f
    }


    private fun takeUniformRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear(RNG.nextFloat(), range.start, range.endInclusive)
    private fun takeTriangularRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear((RNG.nextFloat() + RNG.nextFloat()) / 2f, range.start, range.endInclusive)
    private fun takeGaussianRand(range: ClosedFloatingPointRange<Float>) =
        FastMath.interpolateLinear((RNG.nextFloat() + RNG.nextFloat() + RNG.nextFloat() + RNG.nextFloat() + RNG.nextFloat() + RNG.nextFloat() + RNG.nextFloat() + RNG.nextFloat()) / 8f, range.start, range.endInclusive)

    val RNG: Random
        get() = WeatherMixer.RNG

    val windDir = WeatherDirBox() // 0 .. 1.0
    val windSpeed = WeatherStateBox() // 0 .. arbitrarily large number

    val weatherSchedule = ArrayList<WeatherSchedule>()
    val oldWeather: BaseModularWeather
        get() = weatherSchedule[0].weather
    val currentWeather: BaseModularWeather
        get() = weatherSchedule[1].weather

    //    val nextWeather: BaseModularWeather
//        get() = weatherSchedule[2].weather
    val oldWeatherDuration: Long
        get() = weatherSchedule[0].duration
    val currentWeatherDuration: Long
        get() = weatherSchedule[1].duration

    @Transient val currentVibrancy = Vector2(1f, 1f)
    val weatherBlend: Float
        get() = updateAkku.toFloat() / currentWeatherDuration

    fun initWith(initWeather: BaseModularWeather, duration: Long) {
        weatherSchedule.clear()
        weatherSchedule.add(WeatherSchedule(initWeather, duration))
        weatherSchedule.add(WeatherSchedule(initWeather, duration))
    }

    var updateAkku = 0L; private set

    private fun pickNextWeather(): WeatherSchedule {
        // temporary setup for the release
        val newName = if (takeUniformRand(0f..1f) < 0.5f) "generic01" else "generic02"
        val newDuration = takeTriangularRand(3600f..10800f).roundToLong()
        return WeatherSchedule(WeatherMixer.weatherDict[newName]!!, newDuration)
    }

    fun update(world: GameWorld) {
        updateShaderParams()
        updateWind(world)

        if (updateAkku >= currentWeatherDuration) {
            // TODO add more random weathers
            while (weatherSchedule.size < 3) {
                weatherSchedule.add(pickNextWeather())

//                printdbg(this, "Queueing next weather '$newName' that will last $newDuration seconds")
            }

            // subtract akku by old currentWeatherDuration
//            printdbg(this, "Dequeueing a weather")

            weatherSchedule.removeAt(0)
            updateAkku -= oldWeatherDuration
        }

        updateAkku += world.worldTime.timeDelta
    }

    private fun updateWind(world: GameWorld) {
        windSpeed.update(world.worldTime.timeDelta / WIND_SPEED_TIME_UNIT) { lastValue ->
            currentWeather.getRandomWindSpeed(lastValue, takeUniformRand(-1f..1f))
        }
        windDir.update( world.worldTime.timeDelta / WIND_DIR_TIME_UNIT) { RNG.nextFloat() * 4f }
    }

    private fun updateShaderParams() {
        val (co, cg) = oldWeather.shaderVibrancy
        val (no, ng) = currentWeather.shaderVibrancy
        currentVibrancy.set(
            FastMath.interpolateLinear(weatherBlend * 2, co, no),
            FastMath.interpolateLinear(weatherBlend * 2, cg, ng),
        )
    }
}


open class WeatherStateBox(
    var x:  Float = 0f,
    var pM2:Float = 0f,
    var pM1:Float = 0f,
    var p0: Float = 0f,
    var p1: Float = 0f,
    var p2: Float = 0f,
    var p3: Float = 0f,
//    var p4: Float = 0f,
//    var p5: Float = 0f,
    // pM1 and p4 only exists for the sake of better weather forecasting
    // - removing p4 and beyond: for faster response to the changing weather schedule and make the forecasting less accurate like irl
) {
//    protected lateinit var polynomial: UnivariateFunction
//    protected val interpolator = SplineInterpolator()

    open val value: Float; get() = valueAt(x)
    open fun valueAt(x: Float) = when (x.floorToInt()) {
        -2 -> interpolate(x + 2, pM2,pM1, p0, p1)
        -1 -> interpolate(x + 1, pM1, p0, p1, p2)
        0 -> interpolate(x - 0,  p0, p1, p2, p3)
        1 -> interpolate(x - 1,  p1, p2, p3, p3)
        2 -> interpolate(x - 2,  p2, p3, p3, p3)
        3 -> interpolate(x - 3,  p3, p3, p3, p3)
        else -> throw IllegalArgumentException()
    }

    open protected fun extrapolate(): Float {
        val d1 = p2 - p1
        val d2 = p3 - p2

        // if two slopes are monotonic
        return if (d1 * d2 >= 0f)
            (d2 + d1) / 2f + p3
        else
            (d2 + d1) / 2f + p3
    }

    open protected fun updatePolynomials() {

    }

    open fun update(xdelta: Float, next: (Float) -> Float) {
//        if (!::polynomial.isInitialized) updatePolynomials()

        synchronized(WeatherMixer.RNG) {
            x += xdelta
            while (x >= 1.0) {
                x -= 1.0f
                pM2 = pM1
                pM1 = p0
                p0 = p1
                p1 = p2
                p2 = p3
                p3 = next(p2)
            }
//            updatePolynomials()
        }
    }

    protected fun interpolate(u: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
        val T = FastMath.interpolateLinear(u, p1, p2).div(maxOf(p0, p3).coerceAtLeast(1f)).toDouble().coerceIn(0.0, 0.5)
//        if (u == x) printdbg(this, "u=$u, p1=$p1, p2=$p2; T=$T")

        val c1 = p1.toDouble()
        val c2 = -1.0 * T * p0 + T * p2
        val c3 = 2 * T * p0 + (T - 3) * p1 + (3 - 2 * T) * p2 + -T * p3
        val c4 = -T * p0 + (2 - T) * p1 + (T - 2) * p2 + T * p3

        return (((c4 * u + c3) * u + c2) * u + c1).toFloat()
    }
}

/**
 * WeatherStateBox with rotational range of -2..2
 */
class WeatherDirBox(
    x:  Float = 0f,
    pM2:Float = 0f,
    pM1:Float = 0f,
    p0: Float = 0f,
    p1: Float = 0f,
    p2: Float = 0f,
    p3: Float = 0f,
) : WeatherStateBox(x, pM2, pM1, p0, p1, p2, p3) {

    override fun valueAt(x: Float): Float {
        var pM2 = pM2
        var pM1 = pM1
        var p0 = p0
        var p1 = p1
        var p2 = p2
        var p3 = p3

        if (x < -2f) {
            if (pM1 - pM2 > 2f) {
                pM2 -= 4f
                pM1 -= 4f
                p0 -= 4f
                p1 -= 4f
                p2 -= 4f
                p3 -= 4f
            }
            else if (pM1 - pM2 < -2f) {
                pM2 += 4f
                pM1 += 4f
                p0 += 4f
                p1 += 4f
                p2 += 4f
                p3 += 4f
            }
        }

        if (x < -1f) {
            if (pM1 - pM2 > 2f) {
                pM1 -= 4f
                p0 -= 4f
                p1 -= 4f
                p2 -= 4f
                p3 -= 4f
            }
            else if (pM1 - pM2 < -2f) {
                pM1 += 4f
                p0 += 4f
                p1 += 4f
                p2 += 4f
                p3 += 4f
            }
        }

        if (x < 0f) {
            if (p0 - pM1 > 2f) {
                p0 -= 4f
                p1 -= 4f
                p2 -= 4f
                p3 -= 4f
            }
            else if (p0 - pM1 < -2f) {
                p0 += 4f
                p1 += 4f
                p2 += 4f
                p3 += 4f
            }
        }

        if (p1 - p0 > 2f) {
            p1 -= 4f
            p2 -= 4f
            p3 -= 4f
        }
        else if (p1 - p0 < -2f) {
            p1 += 4f
            p2 += 4f
            p3 += 4f
        }

        if (p2 - p1 > 2f) {
            p2 -= 4f
            p3 -= 4f
        }
        else if (p2 - p1 < -2f) {
            p2 += 4f
            p3 += 4f
        }

        if (p3 - p2 > 2f) {
            p3 -= 4f
        }
        else if (p3 - p2 < -2f) {
            p3 += 4f
        }

        return when (x.floorToInt()) {
            -2 -> interpolate2(x + 2, pM2,pM1, p0, p1)
            -1 -> interpolate2(x + 1, pM1, p0, p1, p2)
            0 -> interpolate2(x - 0,  p0, p1, p2, p3)
            1 -> interpolate2(x - 1,  p1, p2, p3, p3)
            2 -> interpolate2(x - 2,  p2, p3, p3, p3)
            3 -> interpolate2(x - 3,  p3, p3, p3, p3)
            else -> throw IllegalArgumentException()
        }.plus(2f).fmod(4f).minus(2f)
    }

    private fun interpolate2(u: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
        val T = 0.5

        val c1 = p1.toDouble()
        val c2 = -1.0 * T * p0 + T * p2
        val c3 = 2 * T * p0 + (T - 3) * p1 + (3 - 2 * T) * p2 + -T * p3
        val c4 = -T * p0 + (2 - T) * p1 + (T - 2) * p2 + T * p3

        return (((c4 * u + c3) * u + c2) * u + c1).toFloat()
    }
}