package net.torvald.terrarum.audio

import com.jme3.math.FastMath
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.AUDIO_BUFFER_SIZE
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

        private val BS = AUDIO_BUFFER_SIZE
        private val MP3_CHUNK_SIZE = 1152
    }

    private val q = inputSamplingRate.toDouble() / SAMPLING_RATE // <= 1.0

    private val fetchSize = (BS.toFloat() / MP3_CHUNK_SIZE).ceilToInt() * MP3_CHUNK_SIZE // fetchSize is always multiple of MP3_CHUNK_SIZE, even if the audio is NOT MP3
    private val internalBufferSize = fetchSize * 3


    private fun resampleBlock(innL: FloatArray, innR: FloatArray, outL: FloatArray, outR: FloatArray) {
        fun getInnL(i: Int) = if (i > innL.lastIndex) 0f else if (i in innL.indices) innL[i] else 0f//finOldL[TAPS + i]
        fun getInnR(i: Int) = if (i > innR.lastIndex) 0f else if (i in innR.indices) innR[i] else 0f//finOldR[TAPS + i]

        for (sampleIdx in outL.indices) {
            val x = fPhaseL + q * sampleIdx
            var sx = 0.0
            for (i in x.floorToInt() - TAPS + 1..x.floorToInt() + TAPS) {
                sx += getInnL(i) * L(x - i)
            }
            outL[sampleIdx] = sx.toFloat()
        }
        fPhaseL = -((fPhaseL + q * outL.size) % 1.0)
        innL.takeLast(TAPS).forEachIndexed { index, fl -> finOldL[index] = fl }

        for (sampleIdx in outR.indices) {
            val x = fPhaseR + q * sampleIdx
            var sx = 0.0
            for (i in x.floorToInt() - TAPS + 1..x.floorToInt() + TAPS) {
                sx += getInnR(i) * L(x - i)
            }
            outR[sampleIdx] = sx.toFloat()
        }
        fPhaseR = -((fPhaseR + q * outR.size) % 1.0)
        innR.takeLast(TAPS).forEachIndexed { index, fl -> finOldR[index] = fl }
    }

    var validSamplesInBuf = 0

    val finOldL = FloatArray(TAPS)
    val finOldR = FloatArray(TAPS)
    var fPhaseL = 0.0
    var fPhaseR = 0.0
    val foutL = FloatArray(internalBufferSize) // 640 for (44100, 48000), 512 for (48000, 48000) with BUFFER_SIZE = 512 * 4
    val foutR = FloatArray(internalBufferSize) // 640 for (44100, 48000), 512 for (48000, 48000) with BUFFER_SIZE = 512 * 4

    fun fetchBytes() {
        val readCount = if (validSamplesInBuf < BS) fetchSize else 0
        val writeCount = (readCount / q + fPhaseL).toInt()
        val readBuf = ByteArray(readCount * 4)
        val finL = FloatArray(readCount)
        val finR = FloatArray(readCount)
        val foutL = FloatArray(writeCount)
        val foutR = FloatArray(writeCount)

        fun getFromReadBuf(i: Int, bytesRead: Int) = if (i < bytesRead) readBuf[i].toUint() else 0

        if (readCount > 0) {
            try {
                val bytesRead = audioReadFun(readBuf)
//                printdbg(this, "Reading audio $readCount samples, got ${bytesRead?.div(4)} samples")

                if (bytesRead == null || bytesRead <= 0) {
//                    printdbg(this, "Music finished; bytesRead = $bytesRead")

                    onAudioFinished()
                }
                else {
                    for (c in 0 until readCount) {
                        val sl = (getFromReadBuf(4 * c + 0, bytesRead) or getFromReadBuf(
                            4 * c + 1,
                            bytesRead
                        ).shl(8)).toShort()
                        val sr = (getFromReadBuf(4 * c + 2, bytesRead) or getFromReadBuf(
                            4 * c + 3,
                            bytesRead
                        ).shl(8)).toShort()

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
                    resampleBlock(finL, finR, foutL, foutR)

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
        else {
//            printdbg(this, "Reading audio zero samples; Buffer: $validSamplesInBuf / $internalBufferSize samples")
        }
    }

    fun getLR(volume: Double): Pair<FloatArray, FloatArray> {
        // copy into the out
        val outL = FloatArray(BS) { (foutL[it] * volume).toFloat() }
        val outR = FloatArray(BS) { (foutR[it] * volume).toFloat() }
        // shift bytes in the fout
        System.arraycopy(foutL, BS, foutL, 0, validSamplesInBuf - BS)
        System.arraycopy(foutR, BS, foutR, 0, validSamplesInBuf - BS)
        for (i in validSamplesInBuf until BS) {
            foutL[i] = 0f
            foutR[i] = 0f
        }
        // decrement necessary variables
        validSamplesInBuf -= BS

        return outL to outR
    }
}