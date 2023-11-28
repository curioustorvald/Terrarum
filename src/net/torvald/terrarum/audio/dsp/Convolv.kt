package net.torvald.terrarum.audio.dsp

import com.jme3.math.FastMath
import net.torvald.terrarum.App.measureDebugTime
import net.torvald.terrarum.App.setDebugTime
import net.torvald.terrarum.audio.*
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.BUFFER_SIZE
import java.io.File

class Convolv(ir: File, val gain: Float = 1f / 256f): TerrarumAudioFilter() {

    val fftLen: Int
    private val convFFT: Array<ComplexArray>
    private val convFFTpartd: Array<Array<ComplexArray>> // index: Channel, partition, frequencies
    private val inputPartd: Array<Array<FloatArray>> // index: Channel, partition, frequencies
    private val inbuf: Array<FloatArray>

    private val BLOCKSIZE = TerrarumAudioMixerTrack.BUFFER_SIZE / 4

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
        inbuf = Array(2) { FloatArray(fftLen) }

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
        var c = BUFFER_SIZE / 4
        val master0 = arrayListOf(c)
        while (c < fftLen) {
            master0.add(c)
            c *= 2
        }
        partSizes = master0.toIntArray()
        partOffsets = master0.toIntArray().also { it[0] = 0 }


        convFFTpartd = Array(2) {
            Array(partSizes.size) {
                ComplexArray(FloatArray(2*partSizes[it]))
            }
        }
        inputPartd = Array(2) {
            Array(partSizes.size) {
                FloatArray(partSizes[it])
            }
        }
        fillUnevenly(convFFT[0], convFFTpartd[0])
        fillUnevenly(convFFT[1], convFFTpartd[1])
    }

    private fun fillUnevenly(source: ComplexArray, dest: Array<ComplexArray>) {
        for (i in partSizes.indices) {
            val len = 2*partSizes[i]
            val offset = 2*partOffsets[i]
            System.arraycopy(source.reim, offset, dest[i].reim, 0, len)
        }
    }
    private fun fillUnevenly(source: FloatArray, dest: Array<FloatArray>) {
        for (i in partSizes.indices) {
            val len = partSizes[i]
            val offset = partOffsets[i]
            System.arraycopy(source, offset, dest[i], 0, len)
        }
    }
    private fun concatParts(source: List<ComplexArray>, dest: ComplexArray) {
        for (i in partSizes.indices) {
            val len = 2*partSizes[i]
            val offset = 2*partOffsets[i]
            System.arraycopy(source[i].reim, 0, dest.reim, offset, len)
        }
    }

    private val realtime = (BLOCKSIZE / TerrarumAudioMixerTrack.SAMPLING_RATEF * 1000000000L)
    /**
     * https://thewolfsound.com/fast-convolution-fft-based-overlap-add-overlap-save-partitioned/
     */
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {

        val t1 = System.nanoTime()


        for (ch in outbuf.indices) {
            push(inbuf[ch].applyGain(gain), this.inbuf[ch])
            val inputFFT = FFT.fft(this.inbuf[ch])
            val Y = inputFFT * convFFT[ch]
            val y = FFT.ifftAndGetReal(Y)
            System.arraycopy(y, fftLen - BLOCKSIZE, outbuf[ch], 0, BLOCKSIZE)
        }


        val ptime = System.nanoTime() - t1
        setDebugTime("audio.convolve", ptime)
        processingSpeed = realtime / ptime
    }

}