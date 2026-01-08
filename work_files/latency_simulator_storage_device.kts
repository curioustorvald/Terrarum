import kotlin.math.ceil


object Random {
    fun uniformRand(low: Int, high: Int) = (Math.random() * (high + 1)).toInt()

    fun triangularRand(low: Float, high: Float): Float {
        val a = (Math.random() + Math.random()) / 2.0
    	return ((high - low) * a + low).toFloat()
    }
    fun gaussianRand(avg: Float, stddev: Float): Float {
        // Box-Muller transform to generate random numbers with standard normal distribution
        // This implementation uses the polar form for better efficiency

        // We need two uniform random values between 0 and 1
        val random = kotlin.random.Random

        // Using the polar form of the Box-Muller transformation
        var u: Double
        var v: Double
        var s: Double

        do {
            // Generate two uniform random numbers between -1 and 1
            u = Math.random() * 2 - 1
            v = Math.random() * 2 - 1

            // Calculate sum of squares
            s = u * u + v * v
        } while (s >= 1 || s == 0.0)

        // Calculate polar transformation
        val multiplier = kotlin.math.sqrt(-2.0 * kotlin.math.ln(s) / s)

        // Transform to the desired mean and standard deviation
        // We only use one of the two generated values here
        return (avg + stddev * u * multiplier).toFloat()
    }
}

sealed class SeekSimulator {

    abstract fun computeSeekTime(currentSector: Int, targetSector: Int): Float

    class Tape(
        val totalSectors: Int,
        val tapeLengthMeters: Float = 200f,
        val baseSeekTime: Float = 0.5f,  // seconds base inertia
        val tapeSpeedMetersPerSec: Float = 2.0f,  // normal speed
    ) : SeekSimulator() {
        override fun computeSeekTime(currentSector: Int, targetSector: Int): Float {
            val posCurrent = (currentSector.toFloat() / totalSectors) * tapeLengthMeters
            val posTarget = (targetSector.toFloat() / totalSectors) * tapeLengthMeters
            val distance = kotlin.math.abs(posTarget - posCurrent)

            // Inject random tape jitter
            val effectiveSpeed = tapeSpeedMetersPerSec * Random.triangularRand(0.9f, 1.1f)

            return baseSeekTime + (distance / effectiveSpeed)
        }
    }

    class Disc(
        val totalTracks: Int,
        val armSeekBaseTime: Float = 0.005f,  // fast seek, seconds
        val armSeekMultiplier: Float = 0.002f,  // slower for bigger jumps
        val rotationLatencyAvg: Float = 0.008f,  // seconds (half-rotation average)
    ) : SeekSimulator() {
        override fun computeSeekTime(currentSector: Int, targetSector: Int): Float {
            val cylCurrent = sectorToTrack(currentSector)
            val cylTarget = sectorToTrack(targetSector)
            val deltaTracks = kotlin.math.abs(cylTarget - cylCurrent)

            val armSeek = armSeekBaseTime + (armSeekMultiplier * kotlin.math.sqrt(deltaTracks.toFloat()))
            val rotationLatency = Random.gaussianRand(rotationLatencyAvg, rotationLatencyAvg * 0.2f)

            return armSeek + rotationLatency
        }

        private fun sectorToTrack(sector: Int): Int {
            // Simplistic assumption: sector layout maps 1:1 to track at this level
            return sector % totalTracks
        }
    }

    class Drum(
        val rpm: Float = 3000f
    ) : SeekSimulator() {
        override fun computeSeekTime(currentSector: Int, targetSector: Int): Float {
            val degreesPerSector = 360.0f / 10000.0f  // Assume 10k sectors per drum circumference
            val angleCurrent = currentSector * degreesPerSector
            val angleTarget = targetSector * degreesPerSector
            val deltaAngle = kotlin.math.abs(angleTarget - angleCurrent) % 360f

            val rotationLatencySeconds = (deltaAngle / 360f) * (60f / rpm)

            // Add a little mechanical jitter
            val jitteredLatency = rotationLatencySeconds * Random.triangularRand(0.95f, 1.05f)

            return jitteredLatency
        }
    }
}


class SeekLatencySampler(
    val simulator: SeekSimulator,
    val totalSectors: Int,
    val sampleCount: Int = 10000
) {
    data class Sample(val fromSector: Int, val toSector: Int, val latency: Float)

    val samples = mutableListOf<Sample>()

    fun runSampling() {
        samples.clear()
        var lastSector = Random.uniformRand(0, totalSectors - 1)

        repeat(sampleCount) {
            val nextSector = Random.uniformRand(0, totalSectors - 1)
            val latency = simulator.computeSeekTime(lastSector, nextSector)
            samples.add(Sample(lastSector, nextSector, latency))
            lastSector = nextSector
        }
    }

    fun analyzeAndPrint() {
        if (samples.isEmpty()) {
            println("No samples generated. Run runSampling() first.")
            return
        }

        val latencies = samples.map { it.latency }
        val minLatency = latencies.minOrNull() ?: 0f
        val maxLatency = latencies.maxOrNull() ?: 0f
        val avgLatency = latencies.average().toFloat()
        val stddevLatency = kotlin.math.sqrt(latencies.map { (it - avgLatency).let { diff -> diff * diff } }.average()).toFloat()

        println("=== Seek Latency Stats ===")
        println("Samples: $sampleCount")
        println("Min: ${"%.4f".format(minLatency)} s")
        println("Max: ${"%.4f".format(maxLatency)} s")
        println("Avg: ${"%.4f".format(avgLatency)} s")
        println("Stddev: ${"%.4f".format(stddevLatency)} s")

        printSimpleHistogram(latencies)
    }

    private fun printSimpleHistogram(latencies: List<Float>, bins: Int = 30) {
        val min = latencies.minOrNull() ?: return
        val max = latencies.maxOrNull() ?: return
        val binSize = (max - min) / bins

        val histogram = IntArray(bins) { 0 }

        latencies.forEach { latency ->
            val bin = kotlin.math.min(((latency - min) / binSize).toInt(), bins - 1)
            histogram[bin]++
        }

        println("--- Latency Distribution ---")
        histogram.forEachIndexed { index, count ->
            val lower = min + binSize * index
            val upper = lower + binSize
            val bar = "#".repeat(count / (sampleCount / 200))  // Scale bar length
            println("${"%.4f".format(lower)} - ${"%.4f".format(upper)} s: $bar")
        }
    }
}

fun main() {
    val tapeSimulator = SeekSimulator.Tape(
        totalSectors = 100000,
        tapeLengthMeters = 200f,
        baseSeekTime = 0.2f,
        tapeSpeedMetersPerSec = 5.0f
    )

    val discSimulator = SeekSimulator.Disc(
        totalTracks = 3810,
        armSeekBaseTime = 0.005f,
        armSeekMultiplier = 0.002f,
        rotationLatencyAvg = 0.008f
    )

    val drumSimulator = SeekSimulator.Drum(
        rpm = 3000f
    )

    listOf(tapeSimulator, discSimulator, drumSimulator).forEach { sim ->
    	SeekLatencySampler(
            simulator = sim,
            totalSectors = 100000,
            sampleCount = 5000
        ).also {
            it.runSampling()
            it.analyzeAndPrint()
        }
    }
}
