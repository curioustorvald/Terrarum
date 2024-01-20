package net.torvald.terrarum.audio

import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
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
class AudioProcessBuf(val inputSamplingRate: Int, val audioReadFun: (ByteArray) -> Int?, val onAudioFinished: () -> Unit) {

    var playbackSpeed = 1f
        set(value) {
            field = value.coerceIn(0.5f, 2f)
        }

    private val internalSamplingRate
        get() = inputSamplingRate * playbackSpeed

    private val doResample
        get() = !(inputSamplingRate == SAMPLING_RATE && (playbackSpeed - 1f).absoluteValue < (1f / 1024f))

    companion object {
        private val epsilon: Double = Epsilon.E

        private val TAPS = 4 // 2*a tap lanczos intp. Lower = greater artefacts

        private val Lcache = HashMap<Long, Double>(1048576)
        fun L(x: Double): Double {
            return Lcache.getOrPut(x.toBits()) { // converting double to longbits allows faster cache lookup?!
                if (x.absoluteValue < epsilon)
                    1.0
                else if (-TAPS <= x && x < TAPS)
                    (TAPS * sin(PI * x) * sin(PI * x / TAPS)) / (PI * PI * x * x)
                else
                    0.0
            }
        }

        private val MP3_CHUNK_SIZE = 1152 // 1152 for 32k-48k, 576 for 16k-24k, 384 for 8k-12k


        private val bufLut = HashMap<Pair<Int, Int>, Int>()

        init {
            val bl = arrayOf(
                1152,1380,1814,1792,2304,2634,3502,3456,4608,5141,6874,6912,
                1280,1508,1942,1920,2304,2762,3630,3584,4608,5267,7004,6912,
                1536,1764,2198,2176,2560,3018,3886,3840,4608,5519,7260,7168,
                2048,2276,2710,2688,3072,3530,4398,4352,5120,6023,7772,7680,
                4096,4554,5421,5376,6144,7056,8796,8704,10240,12078,15544,15360
            )

            arrayOf(48000,44100,32768,32000,24000,22050,16384,16000,12000,11025,8192,8000).forEachIndexed { ri, r ->
                arrayOf(128,256,512,1024,2048).forEachIndexed { bi, b ->
                    bufLut[b to r] = bl[bi * 12 + ri] * 2
                }
            }
        }

        private fun getOptimalBufferSize(rate: Int) = bufLut[App.audioBufferSize to rate]!!
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

    var validSamplesInBuf = 0

    private val finL = FloatArray(fetchSize + 2 * PADSIZE)
    private val finR = FloatArray(fetchSize + 2 * PADSIZE)
    private val fmidL = FloatArray((fetchSize / q + 1.0).toInt() * 2)
    private val fmidR = FloatArray((fetchSize / q + 1.0).toInt() * 2)
    private val foutL = FloatArray(internalBufferSize) // 640 for (44100, 48000), 512 for (48000, 48000) with BUFFER_SIZE = 512 * 4
    private val foutR = FloatArray(internalBufferSize) // 640 for (44100, 48000), 512 for (48000, 48000) with BUFFER_SIZE = 512 * 4
    private val readBuf = ByteArray(fetchSize * 4)

    init {
        printdbg(this, "App.audioMixerBufferSize=${App.audioBufferSize}")
    }

    private fun shift(array: FloatArray, size: Int) {
        System.arraycopy(array, size, array, 0, array.size - size)
        for (i in array.size - size until array.size) { array[i] = 0f }
    }

    fun fetchBytes() {
        val readCount = if (validSamplesInBuf < App.audioBufferSize) fetchSize else 0
        val writeCount = (readCount / q).roundToInt()

        fun getFromReadBuf(i: Int, bytesRead: Int) = if (i < bytesRead) readBuf[i].toUint() else 0

        if (readCount > 0) {
            try {
                shift(finL, readCount)
                shift(finR, readCount)

                val bytesRead = audioReadFun(readBuf)
//                printdbg(this, "Reading audio $readCount samples, got ${bytesRead?.div(4)} samples")

                if (bytesRead == null || bytesRead <= 0) {
//                    printdbg(this, "Music finished; bytesRead = $bytesRead")

                    onAudioFinished()
                }
                else {
                    for (c in 0 until readCount) {
                        val sl = (getFromReadBuf(4 * c + 0, bytesRead) or getFromReadBuf(4 * c + 1, bytesRead).shl(8)).toShort()
                        val sr = (getFromReadBuf(4 * c + 2, bytesRead) or getFromReadBuf(4 * c + 3, bytesRead).shl(8)).toShort()

                        val fl = sl / 32767f
                        val fr = sr / 32767f

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
                    System.arraycopy(fmidL, 0, foutL, validSamplesInBuf, writeCount)
                    System.arraycopy(fmidR, 0, foutR, validSamplesInBuf, writeCount)
                }
                else {
                    // fill in the output buffers
                    System.arraycopy(finL, 0, foutL, validSamplesInBuf, writeCount)
                    System.arraycopy(finR, 0, foutR, validSamplesInBuf, writeCount)
                }

                validSamplesInBuf += writeCount
            }
        }
        else {
//            printdbg(this, "Reading audio zero samples; Buffer: $validSamplesInBuf / $internalBufferSize samples")
        }

//        printdbg(this, "phase = $fPhaseL")
    }

    fun getLR(volume: Double): Pair<FloatArray, FloatArray> {
        // copy into the out
        val outL = FloatArray(App.audioBufferSize) { (foutL[it] * volume).toFloat() }
        val outR = FloatArray(App.audioBufferSize) { (foutR[it] * volume).toFloat() }
        // shift bytes in the fout
        System.arraycopy(foutL, App.audioBufferSize, foutL, 0, validSamplesInBuf - App.audioBufferSize)
        System.arraycopy(foutR, App.audioBufferSize, foutR, 0, validSamplesInBuf - App.audioBufferSize)
        for (i in validSamplesInBuf until App.audioBufferSize) {
            foutL[i] = 0f
            foutR[i] = 0f
        }
        // decrement necessary variables
        validSamplesInBuf -= App.audioBufferSize

        return outL to outR
    }
}