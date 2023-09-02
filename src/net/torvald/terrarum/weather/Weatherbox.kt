package net.torvald.terrarum.weather

import com.jme3.math.FastMath
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.floorToInt
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import java.util.*
import kotlin.math.max
import kotlin.math.pow

data class WeatherSchedule(val weather: BaseModularWeather = WeatherMixer.DEFAULT_WEATHER, val duration: Long = 3600)

class Weatherbox {

    companion object {
        private val WIND_DIR_TIME_UNIT = 3600f * 6 // every 6hr
        private val WIND_SPEED_TIME_UNIT = 3600f * 1 // every 2hr

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

    val weatherSchedule: MutableList<WeatherSchedule> = mutableListOf<WeatherSchedule>()
    val currentWeather: BaseModularWeather
        get() = weatherSchedule[0].weather
    val currentWeatherDuration: Long
        get() = weatherSchedule[0].duration

    fun initWith(initWeather: BaseModularWeather, duration: Long) {
        weatherSchedule.add(WeatherSchedule(initWeather, duration))
    }

    var updateAkku = 0L; private set

    fun update(world: GameWorld) {
        updateWind(world)

        if (updateAkku >= currentWeatherDuration) {
            // TODO add more random weathers
            if (weatherSchedule.size == 1) {
                val newName = if (currentWeather.identifier == "generic01") "overcast01" else "generic01"
                val newDuration = 7200L
                weatherSchedule.add(WeatherSchedule(WeatherMixer.weatherDict[newName]!!, newDuration))

//                printdbg(this, "Queueing next weather '$newName' that will last $newDuration seconds")
            }

            // subtract akku by old currentWeatherDuration
            updateAkku -= weatherSchedule.removeAt(0).duration
        }

        updateAkku += world.worldTime.timeDelta
    }

    private fun updateWind(world: GameWorld) {
        windSpeed.update(world.worldTime.timeDelta / WIND_SPEED_TIME_UNIT) {
            currentWeather.getRandomWindSpeed(takeUniformRand(-1f..1f))
        }
        windDir.update( world.worldTime.timeDelta / WIND_DIR_TIME_UNIT) { RNG.nextFloat() * 4f }
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

    protected lateinit var polynomial: PolynomialSplineFunction
    protected val interpolator = AkimaSplineInterpolator()

    open val value: Float; get() = valueAt(x)
    open fun valueAt(x: Float) = polynomial.value(x + 1.0).toFloat()

    open protected fun updatePolynomials() {
        polynomial = interpolator.interpolate(
            doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(pM2.toDouble(), pM1.toDouble(), p0.toDouble(), p1.toDouble(), p2.toDouble(), p3.toDouble(), p3.toDouble())
        )
    }

    open fun update(xdelta: Float, next: () -> Float) {
        if (!::polynomial.isInitialized) updatePolynomials()

        synchronized(WeatherMixer.RNG) {
            x += xdelta
            while (x >= 1.0) {
                x -= 1.0f
                pM2 = pM1
                pM1 = p0
                p0 = p1
                p1 = p2
                p2 = p3
                p3 = next()
            }
            updatePolynomials()
        }
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
    override fun valueAt(x: Float) = polynomial.value(x + 1.0).plus(2.0).fmod(4.0).minus(2.0).toFloat()

    override fun updatePolynomials() {
        var pM2 = pM2
        var pM1 = pM1
        var p0 = p0
        var p1 = p1
        var p2 = p2
        var p3 = p3


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

        polynomial = interpolator.interpolate(
            doubleArrayOf(-2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0),
            doubleArrayOf(pM2.toDouble(), pM1.toDouble(), p0.toDouble(), p1.toDouble(), p2.toDouble(), p3.toDouble(), p3.toDouble())
        )
    }
}