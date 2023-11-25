package net.torvald.terrarum.audio

import com.jme3.math.FastMath
import com.jme3.math.FastMath.sin
import net.torvald.terrarum.audio.AudioMixer.SPEED_OF_SOUND
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.BUFFER_SIZE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATEF
import net.torvald.terrarum.roundToFloat
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.tanh

abstract class TerrarumAudioFilter {
    var bypass = false
    protected abstract fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>)
    operator fun invoke(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        if (bypass) {
            outbuf.forEachIndexed { index, outTrack ->
                System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
            }
        }
        else thru(inbuf, outbuf)
    }
}

object NullFilter : TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        outbuf.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
        }
    }
}

object SoftClp : TerrarumAudioFilter() {
    val downForce = arrayOf(1.0f, 1.0f)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        downForce.fill(1.0f)

        for (ch in inbuf.indices) {
            val inn = inbuf[ch]
            val out = outbuf[ch]

            for (i in inn.indices) {
                val u = inn[i] * 0.95f
                val v = tanh(u)
                val diff = (v.absoluteValue / u.absoluteValue)
                out[i] = v

                if (!diff.isNaN()) {
                    downForce[ch] = minOf(downForce[ch], diff)
                }
            }
        }
    }
}

class Scope : TerrarumAudioFilter() {
    val backbufL = Array((4096f / BUFFER_SIZE * 4).roundToInt().coerceAtLeast(1)) { FloatArray(BUFFER_SIZE / 4) }
    val backbufR = Array((4096f / BUFFER_SIZE * 4).roundToInt().coerceAtLeast(1)) { FloatArray(BUFFER_SIZE / 4) }

    private val sqrt2p = 0.7071067811865475

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        // shift buffer
        for (i in backbufL.lastIndex downTo 1) {
            backbufL[i] = backbufL[i - 1]
            backbufR[i] = backbufR[i - 1]
        }
        backbufL[0] = FloatArray(BUFFER_SIZE / 4)
        backbufR[0] = FloatArray(BUFFER_SIZE / 4)

        // plot dots
        for (i in 0 until BUFFER_SIZE/4) {
            val y0 = inbuf[0][i] * 0.7
            val x0 = -inbuf[1][i] * 0.7 // rotate the domain by -90 deg

            val x = (+x0*sqrt2p -y0*sqrt2p) * 1.414
            val y = (-x0*sqrt2p -y0*sqrt2p) * 1.414 // further rotate by -45 deg then flip along the y axis

            backbufL[0][i] = x.toFloat()
            backbufR[0][i] = y.toFloat()
        }

        // copy samples over
        outbuf.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
        }
    }
}


class Lowpass(cutoff0: Float): TerrarumAudioFilter() {

    var cutoff = cutoff0.toDouble(); private set
    private var alpha: Float = 0f

    init {
        setCutoff(cutoff0)
    }

    fun setCutoff(cutoff: Float) {
        val RC: Float = 1f / (cutoff * FastMath.TWO_PI)
        val dt: Float = 1f / SAMPLING_RATEF
        alpha = dt / (RC + dt)
        this.cutoff = cutoff.toDouble()
    }

    fun setCutoff(cutoff: Double) {
//        println("LP Cutoff: $cutoff")
        val RC: Double = 1.0 / (cutoff * Math.PI * 2.0)
        val dt: Double = 1.0 / SAMPLING_RATEF
        alpha = (dt / (RC + dt)).toFloat()
        this.cutoff = cutoff
    }

    val in0 = FloatArray(2)
    val out0 = FloatArray(2)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (ch in outbuf.indices) {
            val out = outbuf[ch]
            val inn = inbuf[ch]


            out[0] = out0[ch] + alpha * (inn[0] - out0[ch])
            
            for (i in 1 until outbuf[ch].size) {
                out[i] = out[i-1] + alpha * (inn[i] - out[i-1])
            }

            out0[ch] = outbuf[ch].last()
            in0[ch] = inbuf[ch].last()
        }
    }

}


class Highpass(cutoff0: Float): TerrarumAudioFilter() {

    var cutoff = cutoff0.toDouble(); private set
    private var alpha: Float = 0f

    val in0 = FloatArray(2)
    val out0 = FloatArray(2)

    init {
        setCutoff(cutoff0)
    }

    fun setCutoff(cutoff: Float) {
//        println("LP Cutoff: $cutoff")
        val RC: Float = 1f / (cutoff * FastMath.TWO_PI)
        val dt: Float = 1f / SAMPLING_RATEF
        alpha = RC / (RC + dt)
        this.cutoff = cutoff.toDouble()
    }

    fun setCutoff(cutoff: Double) {
//        println("LP Cutoff: $cutoff")
        val RC: Double = 1.0 / (cutoff * Math.PI * 2.0)
        val dt: Double = 1.0 / SAMPLING_RATEF
        alpha = (RC / (RC + dt)).toFloat()
        this.cutoff = cutoff
    }

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (ch in outbuf.indices) {
            val out = outbuf[ch]
            val inn = inbuf[ch]

            out[0] = alpha * (out0[ch] + inn[0] - in0[ch])

            for (i in 1 until outbuf[ch].size) {
                out[i] = alpha * (out[i-1] + inn[i] - inn[i-1])
            }

            out0[ch] = outbuf[ch].last()
            in0[ch] = inbuf[ch].last()
        }
    }

}


object Buffer : TerrarumAudioFilter() {
    init {
        bypass = true
    }

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        bypass = true
    }
}

/**
 * The input audio must be monaural
 *
 * @param pan -1 for far-left, 0 for centre, 1 for far-right
 * @param soundSpeed speed of the sound in meters per seconds
 * @param earDist distance between ears in meters
 */
class BinoPan(var pan: Float, var earDist: Float = 0.18f): TerrarumAudioFilter() {

    private val PANNING_CONST = 3.0 // 3dB panning rule

    private val delayLine = FloatArray(BUFFER_SIZE / 4)

    private fun getFrom(index: Float, buf0: FloatArray, buf1: FloatArray): Float {
        val index = index.toInt() // TODO resampling
        return if (index >= 0) buf1[index]
        else buf0[buf0.size + index]
    }

    private val delays = arrayOf(0f, 0f)
    private val mults = arrayOf(1f, 1f)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        val angle = pan * 1.5707963f
        val timeDiffMax = earDist / SPEED_OF_SOUND * SAMPLING_RATEF
        val delayInSamples = (timeDiffMax * sin(angle)).absoluteValue
        val volMultDbThis = PANNING_CONST * pan.absoluteValue
        val volMultFsThis = decibelsToFullscale(volMultDbThis).toFloat()
        val volMUltFsOther = 1f / volMultFsThis

        if (pan >= 0) {
            delays[0] = delayInSamples
            delays[1] = 0f
        }
        else {
            delays[0] = 0f
            delays[1] = delayInSamples
        }

        if (pan >= 0) {
            mults[0] = volMUltFsOther
            mults[1] = volMultFsThis
        }
        else {
            mults[0] = volMultFsThis
            mults[1] = volMUltFsOther
        }

        for (ch in 0..1) {
            for (i in 0 until BUFFER_SIZE / 4) {
                outbuf[ch][i] = getFrom(i - delays[ch], delayLine, inbuf[0]) * mults[ch]
            }
        }

        push(inbuf[0], delayLine)
    }
}

class Bitcrush(var steps: Int, var inputGain: Float = 1f): TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (ch in outbuf.indices) {
            for (i in 0 until BUFFER_SIZE / 4) {
                val inn = ((inbuf[ch][i] * inputGain).coerceIn(-1f, 1f) + 1f) / 2f // 0f..1f
                val stepped = (inn * (steps - 1)).roundToFloat() / (steps - 1)
                val out = (stepped * 2f) - 1f // -1f..1f
                outbuf[ch][i] = out
            }
        }
    }
}

class Reverb(val delayMS: Float = 36f, var feedback: Float = 0.92f, var lowpass: Float = 1200f): TerrarumAudioFilter() {

    private val highpass = 80f

    private var delay = (SAMPLING_RATEF * delayMS / 1000f).roundToInt()
    private val bufSize = delay + 2

    private val buf = Array(2) { FloatArray(bufSize) }

    private fun unshift(sample: Float, buf: FloatArray) {
        for (i in bufSize - 1 downTo 1) {
            buf[i] = buf[i - 1]
        }
        buf[0] = sample
    }

    private val out0 = FloatArray(2)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        val RCLo: Float = 1f / (lowpass * FastMath.TWO_PI)
        val RCHi: Float = 1f / (highpass * FastMath.TWO_PI)
        val dt: Float = 1f / SAMPLING_RATEF
        val alphaLo = dt / (RCLo + dt)
        val alphaHi = RCHi / (RCHi + dt)

        for (ch in outbuf.indices) {
            for (i in 0 until BUFFER_SIZE / 4) {
                val inn = inbuf[ch][i]

                // reverb
                val rev = buf[ch][delay - 1]
                val out = inn - rev * feedback

                // fill lpbuf
                val lp0 = buf[ch][0]
                val lp = lp0 + alphaLo * (out - lp0)
                unshift(lp, buf[ch])

                outbuf[ch][i] = out
            }
        }
    }
}

class Convolv(ir: File, val gain: Float = 1f / 256f): TerrarumAudioFilter() {

    private val fftLen: Int
    private val convFFT: Array<Array<FComplex>>
    private val convFFTpartd: Array<Array<Array<FComplex>>> // index: Channel, partition, frequencies
    private val inbuf: Array<FloatArray>

    private val BLOCKSIZE = BUFFER_SIZE / 4

    var processingSpeed = 1f; private set

    private val partSizes: IntArray
    private val partOffsets: IntArray

    init {
        if (!ir.exists()) {
            throw IllegalArgumentException("Impulse Response file '${ir.path}' does not exist.")
        }

        val sampleCount = ir.length().toInt() / 8
        fftLen = FastMath.nextPowerOfTwo(sampleCount)

//        println("IR Sample Count = $sampleCount; FFT Length = $fftLen")

        val conv = Array(2) { FloatArray(fftLen) }
        inbuf = Array(2) { FloatArray(fftLen) }
//        outbuf = Array(2) { DoubleArray(fftLen) }

        ir.inputStream().let {
            for (i in 0 until sampleCount) {
                val f1 = Float.fromBits(it.read().and(255) or
                        it.read().and(255).shl(8) or
                        it.read().and(255).shl(16) or
                        it.read().and(255).shl(24))
                val f2 = Float.fromBits(it.read().and(255) or
                        it.read().and(255).shl(8) or
                        it.read().and(255).shl(16) or
                        it.read().and(255).shl(24))
                conv[0][i] = f1
                conv[1][i] = f2
            }

            it.close()
        }

        // fourier-transform the 'conv'
        convFFT = Array(2) {
            FFT.fft(conv[it])
        }

//        println("convFFT Length = ${convFFT[0].size}")

        if (fftLen < BUFFER_SIZE) // buffer size is always 4x the samples in the buffer
            throw Error("FIR size is too small (minimum: $BUFFER_SIZE samples)")


        val partitions = ArrayList<Int>()
        var cnt = fftLen
        while (cnt > BUFFER_SIZE / 4) {
            cnt /= 2
            partitions.add(cnt)
        }
        partitions.add(cnt)

        partSizes = partitions.reversed().toIntArray()
        partOffsets = partSizes.clone().also { it[0] = 0 }

        // allocate arrays
        convFFTpartd = Array(2) { ch ->
            Array(partSizes.size) { partNo ->
                Array(partSizes[partNo]) {
                    convFFT[ch][partOffsets[partNo] + it]
                }
            }
        }
    }

    /**
     * https://thewolfsound.com/fast-convolution-fft-based-overlap-add-overlap-save-partitioned/
     */
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
//        println("Convolv thru")

        val t1 = System.nanoTime()
        for (ch in outbuf.indices) {
            push(inbuf[ch].applyGain(gain), this.inbuf[ch])

            val inputFFT = FFT.fft(this.inbuf[ch])

            val Y = multiply(inputFFT, convFFT[ch])
            val y = FFT.ifftAndGetReal(Y)

            val u = y.takeLast(BLOCKSIZE).toFloatArray()

            System.arraycopy(u, 0, outbuf[ch], 0, BLOCKSIZE)
        }
        val t2 = System.nanoTime()
        val ptime = (t2 - t1).toFloat()
        val realtime = BLOCKSIZE / SAMPLING_RATEF * 1000000000L
        processingSpeed = realtime / ptime
    }

    private fun multiply(X: Array<FComplex>, H: Array<FComplex>): Array<FComplex> {
        return Array(X.size) { X[it] * H[it] }
    }

}

object XYtoMS: TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (i in 0 until BUFFER_SIZE / 4) {
            val X = inbuf[0][i]
            val Y = inbuf[1][i]
            val M = (X + Y) / 2f
            val S = (X - Y) / 2f
            outbuf[0][i] = M
            outbuf[1][i] = S
        }
    }
}

object MStoXY: TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (i in 0 until BUFFER_SIZE / 4) {
            val M = inbuf[0][i]
            val S = inbuf[1][i]
            val X = M + S
            val Y = M - S
            outbuf[0][i] = X
            outbuf[1][i] = Y
        }
    }
}

class Gain(var gain: Float): TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (i in 0 until BUFFER_SIZE / 4) {
            outbuf[0][i] = inbuf[0][i] * gain
            outbuf[1][i] = inbuf[1][i] * gain
        }
    }
}

fun FloatArray.applyGain(gain: Float = 1f) = this.map { it * gain }.toFloatArray()
fun push(samples: FloatArray, buf: FloatArray) {
    System.arraycopy(buf, samples.size, buf, 0, buf.size - samples.size)
    System.arraycopy(samples, 0, buf, buf.size - samples.size, samples.size)
}