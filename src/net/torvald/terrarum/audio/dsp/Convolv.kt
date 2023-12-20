package net.torvald.terrarum.audio.dsp

import com.jme3.math.FastMath
import net.torvald.terrarum.App.setDebugTime
import net.torvald.terrarum.audio.*
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.AUDIO_BUFFER_SIZE
import java.io.File

class Convolv(ir: File, val gain: Float = 1f / 512f): TerrarumAudioFilter() {

    val fftLen: Int
    private val convFFT: Array<ComplexArray>
    private val inbuf: Array<ComplexArray>

    private val BLOCKSIZE = TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE

    var processingSpeed = 1f; private set

    private val partSizes: IntArray
    private val partOffsets: IntArray

    init {
        if (!ir.exists()) {
            throw IllegalArgumentException("Impulse Response file '${ir.path}' does not exist.")
        }

        val sampleCount = (ir.length().toInt() / 8)//.coerceAtMost(65536)
        fftLen = FastMath.nextPowerOfTwo(sampleCount)

        println("IR '${ir.path}' Sample Count = $sampleCount; FFT Length = $fftLen")

        val conv = Array(2) { FloatArray(fftLen) }
        inbuf = Array(2) { ComplexArray(FloatArray(fftLen * 2)) }

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


        // fill up part* dictionary
        // define "master" array
        var c = AUDIO_BUFFER_SIZE
        val master0 = arrayListOf(c)
        while (c < fftLen) {
            master0.add(c)
            c *= 2
        }
        partSizes = master0.toIntArray()
        partOffsets = master0.toIntArray().also { it[0] = 0 }
    }

    private val realtime = (BLOCKSIZE / TerrarumAudioMixerTrack.SAMPLING_RATEF * 1000000000L)
    private val fftIn = ComplexArray(FloatArray(fftLen * 2))
    private val fftMult = ComplexArray(FloatArray(fftLen * 2))
    private val fftOut_lL = FloatArray(fftLen) // small l/r: input audio
    private val fftOut_rL = FloatArray(fftLen) // large L/R: impulse response
    private val fftOut_lR = FloatArray(fftLen)
    private val fftOut_rR = FloatArray(fftLen)

    private fun convolve(x: ComplexArray, h: ComplexArray, output: FloatArray) {
        FFT.fftInto(x, fftIn)
        fftIn.mult(h, fftMult)
        FFT.ifftAndGetReal(fftMult, output)
    }

    /**
     * https://thewolfsound.com/fast-convolution-fft-based-overlap-add-overlap-save-partitioned/
     */
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        val t1 = System.nanoTime()



        push(gain, inbuf[0], this.inbuf[0])
        push(gain, inbuf[1], this.inbuf[1])

        convolve(this.inbuf[0], convFFT[0], fftOut_lL)
        convolve(this.inbuf[1], convFFT[0], fftOut_rL)
        convolve(this.inbuf[0], convFFT[1], fftOut_lR)
        convolve(this.inbuf[1], convFFT[1], fftOut_rR)

        for (i in 0 until BLOCKSIZE) {
            outbuf[0][i] = fftOut_lL[fftLen - BLOCKSIZE + i] + fftOut_rL[fftLen - BLOCKSIZE + i]
            outbuf[1][i] = fftOut_rR[fftLen - BLOCKSIZE + i] + fftOut_lR[fftLen - BLOCKSIZE + i]
        }



        val ptime = System.nanoTime() - t1
        setDebugTime("audio.convolve", ptime)
        processingSpeed = realtime / ptime
    }


    fun push(gain: Float, samples: FloatArray, buf: ComplexArray) {
        System.arraycopy(buf.reim, samples.size * 2, buf.reim, 0, buf.reim.size - samples.size * 2)
        val baseI = buf.reim.size - samples.size * 2
        samples.forEachIndexed { index, fl ->
            buf.reim[baseI + index * 2 + 0] = fl * gain
            buf.reim[baseI + index * 2 + 1] = 0f
        }
    }

}