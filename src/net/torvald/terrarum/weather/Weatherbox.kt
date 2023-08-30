package net.torvald.terrarum.weather

import com.jme3.math.FastMath
import net.torvald.random.HQRNG
import java.util.*

class Weatherbox {

    val RNG: HQRNG
        get() = WeatherMixer.RNG



}


data class WeatherStateBox(var x: Double, var p0: Double, var p1: Double, var p2: Double, var p3: Double) {

    fun get() = interpolateCatmullRom(x, p0, p1, p2, p3)

    fun getAndUpdate(xdelta: Double, RNG: Random): Double {
        synchronized(RNG) {
            val y = get()
            x += xdelta
            while (x >= 1.0) {
                x -= 1.0
                p0 = p1
                p1 = p2
                p2 = p3
                p3 = RNG.nextDouble()
            }
            return y
        }
    }

    companion object {
        // fixed with T=0.5
        fun interpolateCatmullRom(u: Double, p0: Double, p1: Double, p2: Double, p3: Double): Double {
            val c1: Double = p1
            val c2: Double = -0.5 * p0 + 0.5 * p2
            val c3: Double = p0 - 2.5 * p1 + 2.0 * p2 - 0.5 * p3
            val c4: Double = -0.5 * p0 + 1.5 * p1 - 1.5 * p2 + 0.5 * p3
            return (((c4 * u + c3) * u + c2) * u + c1)
        }

    }
}