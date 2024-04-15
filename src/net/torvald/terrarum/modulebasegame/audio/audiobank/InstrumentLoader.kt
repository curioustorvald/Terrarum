package net.torvald.terrarum.modulebasegame.audio.audiobank

import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.audio.AudioProcessBuf
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.floorToInt
import org.dyn4j.Epsilon
import java.lang.Math.pow
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Creates 61-note (C1 to C5) pack of samples from a single file. Tuning system is 12-note Equal Temperament.
 *
 * Created by minjaesong on 2024-04-14.
 */
object InstrumentLoader {

    // 0 is C0
    private fun getRate(noteNum: Int) = pow(2.0, noteNum / 12.0)


    /**
     * Will read the sample and create 61 copies of them. Rendered samples will be stored on the CommonResourcePool
     * with the naming rule of `"${idBase}_${noteNumber}"`, with format of Pair<FloatArray, FloatArray>
     *
     * The sample must be in two channels, 48 kHz sampling rate.
     *
     * If `isDualMono` option is set, two values of a pair will point to the same FloatArray.
     *
     * @param idBase Base ID string
     * @param module Which module the path must refer to
     * @param path path to the audio
     * @param initialNote Initial note of the given sample. Ranged from 0 to 60. C1 is 0, F3 is 29
     * @param isDualMono if the input sample is in dual mono
     */
    fun load(idBase: String, module: String, path: String, initialNote: Int, isDualMono: Boolean = true): (Int) -> Pair<FloatArray, FloatArray> {
        if (initialNote !in 0..60) throw IllegalArgumentException("Initial note too low or high ($initialNote not in range of 0..60)")

        val baseResourceName = "inst$$idBase"
        if (CommonResourcePool.resourceExists("${baseResourceName}_$initialNote")) return { it ->
            CommonResourcePool.getAs<Pair<FloatArray, FloatArray>>("${baseResourceName}_$it")
        }

        val masterFile = MusicContainer("${idBase}_${initialNote}", ModMgr.getFile(module, path))
        val masterSamplesL = FloatArray(masterFile.totalSizeInSamples.toInt())
        val masterSamplesR = FloatArray(masterFile.totalSizeInSamples.toInt())
        masterFile.readSamples(masterSamplesL, masterSamplesR)

        val renderedSamples = Array<Pair<FloatArray?, FloatArray?>>(61) { null to null }

        for (j in 0 until 61) {
            val i = j - initialNote
            val rate = getRate(i)
            val sampleCount = (masterFile.totalSizeInSamples * (1.0 / rate)).roundToInt()

            val samplesL = FloatArray(sampleCount)
            val samplesR = if (isDualMono) samplesL else FloatArray(sampleCount)

            renderedSamples[j] = samplesL to samplesR

            // do resampling
            resample(masterSamplesL, samplesL, rate)
            if (!isDualMono)
                resample(masterSamplesR, samplesR, rate)

            CommonResourcePool.addToLoadingList("${baseResourceName}_$j") {
                samplesL to samplesR
            }
        }

        CommonResourcePool.loadAll()

        masterFile.dispose()

        return { it ->
            CommonResourcePool.getAs<Pair<FloatArray, FloatArray>>("${baseResourceName}_$it")
        }
    }

    private val TAPS = 8
    private val RESAMPLE_RATE = 1.0// 122880.0 / 48000.0

    private fun resample(input: FloatArray, output: FloatArray, rate: Double) {
        for (sampleIdx in 0 until output.size) {
            val t = sampleIdx.toDouble() * rate * RESAMPLE_RATE
            val leftBound = maxOf(0, (t - TAPS + 1).floorToInt())
            val rightBound = minOf(input.size - 1, (t + TAPS).ceilToInt())


            var akkuL = 0.0
            var weightedSum = 0.0

            for (j in leftBound..rightBound) {
                val w = L(t - j.toDouble())
                akkuL += input[j] * w
                weightedSum += w
            }

            output[sampleIdx] = (akkuL / weightedSum).toFloat()
        }
    }


    fun L(x: Double): Double {
        return if (x.absoluteValue < Epsilon.E)
                1.0
            else if (-TAPS <= x && x < TAPS)
                (TAPS * sin(PI * x) * sin(PI * x / TAPS)) / (PI * PI * x * x)
            else
                0.0
    }

}