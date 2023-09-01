package net.torvald.terrarum.weather

import net.torvald.terrarum.floorToInt
import net.torvald.terrarum.gameworld.fmod
import kotlin.math.absoluteValue


class Weatherbox {

    val windDir = WeatherDirBox() // 0 .. 1.0
    val windSpeed = WeatherStateBox() // 0 .. arbitrarily large number

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

    open fun value() = interpolate(x, p0, p1, p2, p3)
    open fun valueAt(x: Float) = when (x.floorToInt()) {
        -2 -> interpolate(x + 2, pM2,pM1, p0, p1)
        -1 -> interpolate(x + 1, pM1, p0, p1, p2)
         0 -> interpolate(x - 0,  p0, p1, p2, p3)
         1 -> interpolate(x - 1,  p1, p2, p3, p3)
         2 -> interpolate(x - 2,  p2, p3, p3, p3)
         3 -> interpolate(x - 3,  p3, p3, p3, p3)
        else -> throw IllegalArgumentException()
    }

    open fun getAndUpdate(xdelta: Float, next: () -> Float): Float {
        synchronized(WeatherMixer.RNG) {
            val y = value()
            x += xdelta
            while (x >= 1.0) {
                x -= 1.0f
                pM2 = pM1
                pM1 = p0
                p0 = p1
                p1 = p2
                p2 = p3
                p3 = next()


//                p3 = p4
//                p4 = p5
//                p5 = next()
            }
            return y
        }
    }
    protected fun interpolate(u: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
        val c1: Float = p1
        val c2: Float = -0.5f * p0 + 0.5f * p2
        val c3: Float = p0 - 2.5f * p1 + 2.0f * p2 - 0.5f * p3
        val c4: Float = -0.5f * p0 + 1.5f * p1 - 1.5f * p2 + 0.5f * p3
        return (((c4 * u + c3) * u + c2) * u + c1)
    }

    companion object {
        // fixed with T=0.5

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
    override fun value() = valueAt(x)

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
            -2 -> interpolate(x + 2, pM2,pM1, p0, p1)
            -1 -> interpolate(x + 1, pM1, p0, p1, p2)
             0 -> interpolate(x - 0,  p0, p1, p2, p3)
             1 -> interpolate(x - 1,  p1, p2, p3, p3)
             2 -> interpolate(x - 2,  p2, p3, p3, p3)
             3 -> interpolate(x - 3,  p3, p3, p3, p3)
            else -> throw IllegalArgumentException()
        }.plus(2f).fmod(4f).minus(2f)
    }
}