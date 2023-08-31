package net.torvald.terrarum.weather


class Weatherbox {

    val windDir = WeatherStateBox() // 0 .. 1.0
    val windSpeed = WeatherStateBox() // 0 .. arbitrarily large number

}


data class WeatherStateBox(var x: Float = 0f, var p0: Float = 0f, var p1: Float = 0f, var p2: Float = 0f, var p3: Float = 0f) {

    fun get() = interpolateCatmullRom(x, p0, p1, p2, p3)

    fun getAndUpdate(xdelta: Float, next: () -> Float): Float {
        synchronized(WeatherMixer.RNG) {
            val y = get()
            x += xdelta
            while (x >= 1.0) {
                x -= 1.0f
                p0 = p1
                p1 = p2
                p2 = p3
                p3 = next()
            }
            return y
        }
    }

    companion object {
        // fixed with T=0.5
        fun interpolateCatmullRom(u: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
            val c1: Float = p1
            val c2: Float = -0.5f * p0 + 0.5f * p2
            val c3: Float = p0 - 2.5f * p1 + 2.0f * p2 - 0.5f * p3
            val c4: Float = -0.5f * p0 + 1.5f * p1 - 1.5f * p2 + 0.5f * p3
            return (((c4 * u + c3) * u + c2) * u + c1)
        }

    }
}