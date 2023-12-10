package net.torvald.terrarum.audio

import com.jme3.math.FastMath
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.BUFFER_SIZE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.floorToInt
import net.torvald.terrarum.serialise.toUint
import org.dyn4j.Epsilon
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.sin

/**
 * Audio is assumed to be 2 channels, 16 bits
 *
 * Created by minjaesong on 2023-11-17.
 */
class AudioProcessBuf(inputSamplingRate: Int, val audioReadFun: (ByteArray) -> Int?, val onAudioFinished: () -> Unit) {

    private val doResample = inputSamplingRate != SAMPLING_RATE

    companion object {
        private val epsilon: Double = Epsilon.E

        private val TAPS = 4 // 2*a tap lanczos intp. Lower = greater artefacts

        fun L(x: Double): Double = if (x.absoluteValue < epsilon)
            1.0
        else if (-TAPS <= x && x < TAPS)
            (TAPS * sin(PI * x) * sin(PI * x / TAPS)) / (PI * PI * x * x)
        else
            0.0

        private val BS = BUFFER_SIZE / 4
    }

    private val gcd = FastMath.getGCD(inputSamplingRate, SAMPLING_RATE) // 300 for 44100, 48000

    private val samplesIn = inputSamplingRate / gcd // 147 for 44100
    private val samplesOut = SAMPLING_RATE / gcd // 160 for 48000

    private val internalBufferSize = samplesOut * ((BS.toFloat()) / samplesOut).ceilToInt() // (512 / 160) -> 640 for 44100, 48000


    private fun resampleBlock(inn: FloatArray, out: FloatArray) {
        fun getInn(i: Int) = if (i in inn.indices) inn[i] else 0f

        for (sampleIdx in out.indices) {
            val x = (inn.size.toDouble() / out.size) * sampleIdx
            var sx = 0.0
            for (i in x.floorToInt() - TAPS + 1..x.floorToInt() + TAPS) {
                sx += getInn(i) * L(x - i)
            }
            out[sampleIdx] = sx.toFloat()
        }
    }

    var validSamplesInBuf = 0

    val foutL = FloatArray(internalBufferSize) // 640 for (44100, 48000), 512 for (48000, 48000) with BUFFER_SIZE = 512 * 4
    val foutR = FloatArray(internalBufferSize) // 640 for (44100, 48000), 512 for (48000, 48000) with BUFFER_SIZE = 512 * 4

    fun fetchBytes() {
        val readCount = ((internalBufferSize - validSamplesInBuf) / samplesOut) * samplesIn // in samples (441 or 588 for 44100, 48000)
        val writeCount = ((internalBufferSize - validSamplesInBuf) / samplesOut) * samplesOut // in samples (480 or 640 for 44100, 48000)
        val readBuf = ByteArray(readCount * 4)
        val finL = FloatArray(readCount)
        val finR = FloatArray(readCount)
        val foutL = FloatArray(writeCount)
        val foutR = FloatArray(writeCount)

        fun getFromReadBuf(i: Int, bytesRead: Int) = if (i < bytesRead) readBuf[i].toUint() else 0

        try {
            val bytesRead = audioReadFun(readBuf)

            if (bytesRead == null || bytesRead <= 0) onAudioFinished()
            else {
                for(c in 0 until readCount) {
                    val sl = (getFromReadBuf(4 * c + 0, bytesRead) or getFromReadBuf(4 * c + 1, bytesRead).shl(8)).toShort()
                    val sr = (getFromReadBuf(4 * c + 2, bytesRead) or getFromReadBuf(4 * c + 3, bytesRead).shl(8)).toShort()

                    val fl = sl / 32767f
                    val fr = sr / 32767f

                    finL[c] = fl
                    finR[c] = fr
                }
            }
        }
        catch (e: Throwable) {
            e.printStackTrace()
        }
        finally {
            if (doResample) {
                // perform resampling
                resampleBlock(finL, foutL)
                resampleBlock(finR, foutR)

                // fill in the output buffers
                System.arraycopy(foutL, 0, this.foutL, validSamplesInBuf, writeCount)
                System.arraycopy(foutR, 0, this.foutR, validSamplesInBuf, writeCount)
            }
            else {
                // fill in the output buffers
                System.arraycopy(finL, 0, this.foutL, validSamplesInBuf, writeCount)
                System.arraycopy(finR, 0, this.foutR, validSamplesInBuf, writeCount)
            }

            validSamplesInBuf += writeCount
        }
    }

    fun getLR(volume: Double): Pair<FloatArray, FloatArray> {
        // copy into the out
        val outL = FloatArray(BS) { (foutL[it] * volume).toFloat() }
        val outR = FloatArray(BS) { (foutR[it] * volume).toFloat() }
        // shift bytes in the fout
        System.arraycopy(foutL, BS, foutL, 0, validSamplesInBuf - BS)
        System.arraycopy(foutR, BS, foutR, 0, validSamplesInBuf - BS)
        // decrement necessary variables
        validSamplesInBuf -= BS

        return outL to outR
    }
}