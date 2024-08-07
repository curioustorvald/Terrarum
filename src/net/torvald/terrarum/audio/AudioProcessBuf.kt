package net.torvald.terrarum.audio

import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATEF
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.floorToInt
import net.torvald.terrarum.serialise.toUint
import org.dyn4j.Epsilon
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sin

private data class Frac(var nom: Int, val denom: Int) {
    fun toDouble() = nom.toDouble() / denom.toDouble()
    fun toFloat() = this.toDouble().toFloat()
    private val denomStrLen = denom.toString().length
    override fun toString() = "${nom.toString().padStart(denomStrLen)} / $denom"
}

/**
 * Audio is assumed to be 2 channels, 16 bits
 *
 * Created by minjaesong on 2023-11-17.
 */
class AudioProcessBuf(val inputSamplingRate: Float, val audioReadFun: (FloatArray, FloatArray) -> Int?, val onAudioFinished: () -> Unit) {

    var pitch: Float = 1f
    var playbackSpeed = 1f
    var jitterMode = 0 // 0: none, 1: phono, 2: tape
    var jitterIntensity = 0f


    private fun _jitterPhonoEccentricity(t: Float): Float {
        val a = FastMath.TWO_PI * t * RPM
        val b = 60f * SAMPLING_RATE
        return sin(a / b).toRate()
    }

    private fun jitterMode1(t: Float): Float {
        return _jitterPhonoEccentricity(t)
    }

    private fun Float.toRate(): Float {
        return if (this >= 0f) 1f + this
        else 1f / (1f - this)
    }

    /**
     * PlayRate is varying value for simulation of Doppler Shift, etc., if all you want is to just change
     * the pitch of the entire audio, override the sampling rate of the [MusicContainer].
     */
    internal val playRate: Float
        get() = (playbackSpeed * pitch).coerceIn(0.5f, 2f)/*(playbackSpeed * when (jitterMode) {  // disabled until arraycopy negative length bug is resolved
            1 -> jitterMode1(totalSamplesPlayed.toFloat())
            else -> 0f
        } * jitterIntensity).coerceIn(0.5f, 2f)*/

    private val internalSamplingRate
        get() = inputSamplingRate * playRate

    private val doResample
        get() = !(inputSamplingRate == SAMPLING_RATEF && (playRate - 1f).absoluteValue < (1f / 1024f))

    companion object {
        private val RPM = 45f

        private val epsilon: Double = Epsilon.E

        private val TAPS = 4 // 2*a tap lanczos intp. Lower = greater artefacts

//        private val Lcache = HashMap<Long, Double>(1048576)
        fun L(x: Double): Double {
            /*return Lcache.getOrPut(x.toBits()) { // converting double to longbits allows faster cache lookup?!
                if (x.absoluteValue < epsilon)
                    1.0
                else if (-TAPS <= x && x < TAPS)
                    (TAPS * sin(PI * x) * sin(PI * x / TAPS)) / (PI * PI * x * x)
                else
                    0.0
            }*/
            return if (x.absoluteValue < epsilon)
                    1.0
                else if (-TAPS <= x && x < TAPS)
                    (TAPS * sin(PI * x) * sin(PI * x / TAPS)) / (PI * PI * x * x)
                else
                    0.0
        }

        const val MP3_CHUNK_SIZE = 1152 // 1152 for 32k-48k, 576 for 16k-24k, 384 for 8k-12k


        private val bufLut = HashMap<Pair<Int, Int>, Int>()

        private val bufferRates = arrayOf(48000,44100,32768,32000,24000,22050,16384,16000,12000,11025,8192,8000)

        init {
            val bl = arrayOf(
                1152,1380,1814,1792,2304,2634,3502,3456,4608,5141,6874,6912,
                1280,1508,1942,1920,2304,2762,3630,3584,4608,5267,7004,6912,
                1536,1764,2198,2176,2560,3018,3886,3840,4608,5519,7260,7168,
                2048,2276,2710,2688,3072,3530,4398,4352,5120,6023,7772,7680,
                4096,4554,5421,5376,6144,7056,8796,8704,10240,12078,15544,15360
            )

            bufferRates.forEachIndexed { ri, r ->
                arrayOf(128,256,512,1024,2048).forEachIndexed { bi, b ->
                    bufLut[b to r] = (bl[bi * 12 + ri] * 1.05).ceilToInt() * 2
                }
            }
        }

        private fun getOptimalBufferSize(rate: Float): Int {
            val validRate = bufferRates.map { (rate - it) to it }.first { it.first >= 0 }.second
            return bufLut[App.audioBufferSize to validRate]!!
        }
    }

    private val q
    get() = internalSamplingRate.toDouble() / SAMPLING_RATE // <= 1.0

    private val fetchSize = (App.audioBufferSize.toFloat() / MP3_CHUNK_SIZE).ceilToInt() * MP3_CHUNK_SIZE // fetchSize is always multiple of MP3_CHUNK_SIZE, even if the audio is NOT MP3
    private val internalBufferSize = getOptimalBufferSize(inputSamplingRate)// fetchSize * 3

    private val PADSIZE = TAPS + 1

    private fun resampleBlock(innL: FloatArray, innR: FloatArray, outL: FloatArray, outR: FloatArray, outSampleCount: Int) {
        for (sampleIdx in 0 until outSampleCount) {
            val t = sampleIdx.toDouble() * q
            val leftBound = maxOf(0, (t - TAPS + 1).floorToInt())
            val rightBound = minOf(innL.size - 1, (t + TAPS).ceilToInt())


            var akkuL = 0.0
            var akkuR = 0.0
            var weightedSum = 0.0

            for (j in leftBound..rightBound) {
                val w = L(t - j.toDouble())
                akkuL += innL[j] * w
                akkuR += innR[j] * w
                weightedSum += w
            }

            outL[sampleIdx] = (akkuL / weightedSum).toFloat()
            outR[sampleIdx] = (akkuR / weightedSum).toFloat()
        }
    }

    var validSamplesInBuf = 0; private set

    private var totalSamplesPlayed = 0L

    private val finL = FloatArray(fetchSize + 2 * PADSIZE)
    private val finR = FloatArray(fetchSize + 2 * PADSIZE)
    private val fmidL = FloatArray(fetchSize * 4)
    private val fmidR = FloatArray(fetchSize * 4)
    private val foutL = FloatArray(internalBufferSize) // 640 for (44100, 48000), 512 for (48000, 48000) with BUFFER_SIZE = 512 * 4
    private val foutR = FloatArray(internalBufferSize) // 640 for (44100, 48000), 512 for (48000, 48000) with BUFFER_SIZE = 512 * 4
    private val readBufL = FloatArray(fetchSize)
    private val readBufR = FloatArray(fetchSize)

    init {
//        printdbg(this, "App.audioMixerBufferSize=${App.audioBufferSize}")
    }

    private fun shift(array: FloatArray, size: Int) {
        System.arraycopy(array, size, array, 0, array.size - size)
        for (i in array.size - size until array.size) { array[i] = 0f }
    }

    fun fetchBytes() {
        val samplesInBuf = validSamplesInBuf
        val readCount = if (samplesInBuf < App.audioBufferSize) fetchSize else 0
        val writeCount = (readCount / q).roundToInt()

        fun getFromReadBufL(index: Int, samplesRead: Int) = if (index < samplesRead) readBufL[index] else 0f
        fun getFromReadBufR(index: Int, samplesRead: Int) = if (index < samplesRead) readBufR[index] else 0f

        if (readCount > 0) {
            try {
                shift(finL, readCount)
                shift(finR, readCount)

                val samplesRead = audioReadFun(readBufL, readBufR)
//                printdbg(this, "Reading audio $readCount samples, got ${bytesRead?.div(4)} samples")

                if (samplesRead == null || samplesRead <= 0) {
//                    printdbg(this, "Music finished; bytesRead = $bytesRead")

                    onAudioFinished()
                }
                else {
                    for (c in 0 until readCount) {
                        val fl = getFromReadBufL(c, samplesRead)
                        val fr = getFromReadBufR(c, samplesRead)

                        finL[2 * PADSIZE + c] = fl
                        finR[2 * PADSIZE + c] = fr
                    }
                }
            }
            catch (e: Throwable) {
                e.printStackTrace()
            }
            finally {
                if (doResample) {
                    // perform resampling
                    resampleBlock(finL, finR, fmidL, fmidR, writeCount)

                    // fill in the output buffers
                    System.arraycopy(fmidL, 0, foutL, samplesInBuf, writeCount)
                    System.arraycopy(fmidR, 0, foutR, samplesInBuf, writeCount)
                }
                else {
                    // fill in the output buffers
                    System.arraycopy(finL, 0, foutL, samplesInBuf, writeCount)
                    System.arraycopy(finR, 0, foutR, samplesInBuf, writeCount)
                }

                validSamplesInBuf = samplesInBuf + writeCount
                totalSamplesPlayed += writeCount
            }
        }
        else {
//            printdbg(this, "Reading audio zero samples; Buffer: $validSamplesInBuf / $internalBufferSize samples")
        }

//        printdbg(this, "phase = $fPhaseL")
    }

    // return the copy of the foutL/R
    fun getLR(): Pair<FloatArray, FloatArray> {
        val samplesInBuf = validSamplesInBuf

        // copy into the out
        val outL = FloatArray(App.audioBufferSize)
        val outR = FloatArray(App.audioBufferSize)
        System.arraycopy(foutL, 0, outL, 0, App.audioBufferSize)
        System.arraycopy(foutR, 0, outR, 0, App.audioBufferSize)
        // shift bytes in the fout
        System.arraycopy(foutL, App.audioBufferSize, foutL, 0, samplesInBuf - App.audioBufferSize)
        System.arraycopy(foutR, App.audioBufferSize, foutR, 0, samplesInBuf - App.audioBufferSize)
        for (i in samplesInBuf until App.audioBufferSize) {
            foutL[i] = 0f
            foutR[i] = 0f
        }
        // decrement necessary variables
        validSamplesInBuf = samplesInBuf - App.audioBufferSize

        return outL to outR
    }
}