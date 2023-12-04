package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.TransformType
import org.jtransforms.fft.FloatFFT_1D

private val RE0 = 0
private val IM0 = 1

private val RE1 = -1
private val IM1 = 0

private val mulBuf = FloatArray(2)
@JvmInline value class ComplexArray(val reim: FloatArray) {


    val indices: IntProgression
        get() = 0 until size
    val size: Int
        get() = reim.size / 2

    operator fun times(other: ComplexArray): ComplexArray {
        val out = FloatArray(size * 2) {
            if (it % 2 == 0)
                reim[it+RE0] * other.reim[it+RE0] - reim[it+IM0] * other.reim[it+IM0]
            else
                reim[it+RE1] *other.reim[it+IM1] + reim[it+IM1] * other.reim[it+RE1]

        }

        return ComplexArray(out)
    }

    fun mult(other: ComplexArray, out: ComplexArray) {
        for (it in 0 until size * 2) {
            out.reim[it] =
            if (it % 2 == 0)
                reim[it+RE0] * other.reim[it+RE0] - reim[it+IM0] * other.reim[it+IM0]
            else
                reim[it+RE1] *other.reim[it+IM1] + reim[it+IM1] * other.reim[it+RE1]

        }
    }

    // this is actually slower that having a separate array for mult results
    /*fun inlineMult(other: ComplexArray) {
        for (it in 0 until size * 2) {
            mulBuf[it % 2] = if (it % 2 == 0)
                reim[it+RE0] * other.reim[it+RE0] - reim[it+IM0] * other.reim[it+IM0]
            else
                reim[it+RE1] *other.reim[it+IM1] + reim[it+IM1] * other.reim[it+RE1]

            if (it % 2 == 1) {
                reim[it+RE1] = mulBuf[0]
                reim[it+IM1] = mulBuf[1]
            }
        }
    }*/

    fun getReal(): FloatArray {
        return FloatArray(size) { reim[it * 2] }
    }
}

/**
 * Modification of the code form JDSP and Apache Commons Math
 *
 * Created by minjaesong on 2023-11-25.
 */
object FFT: Disposable {

    private val ffts = hashMapOf(
        128 to FloatFFT_1D(128),
        256 to FloatFFT_1D(256),
        512 to FloatFFT_1D(512),
        1024 to FloatFFT_1D(1024),
        2048 to FloatFFT_1D(2048),
        4096 to FloatFFT_1D(4096),
        8192 to FloatFFT_1D(8192),
        16384 to FloatFFT_1D(16384),
        32768 to FloatFFT_1D(32768),
        65536 to FloatFFT_1D(65536),
    )


    init {
//        Loader.load(org.bytedeco.fftw.global.fftw3::class.java)

        App.disposables.add(this)
    }

    /*private val reLock = ReentrantLock(true)

    private fun getForwardPlan(n: Int, inn: FloatArray, out: FloatArray): fftwf_plan {
        return fftwf_plan_dft_1d(n, inn, out, FFTW_FORWARD, FFTW_ESTIMATE)
    }
    private fun getBackwardPlan(n: Int, inn: FloatArray, out: FloatArray): fftwf_plan {
        return fftwf_plan_dft_1d(n, inn, out, FFTW_BACKWARD, FFTW_ESTIMATE)
    }
    private fun destroyPlan(plan: fftwf_plan) {
        fftwf_destroy_plan(plan)
    }*/

    override fun dispose() {
    }

    // org.apache.commons.math3.transform.FastFouriesTransformer.java:370
    fun fft(signal0: FloatArray): ComplexArray {
//        val im = FloatArray(signal0.size)
//        transformInPlace(signal0, im, signal0.size, DftNormalization.STANDARD, TransformType.FORWARD)
//        return ComplexArray(FloatArray(signal0.size) { if (it % 2 == 0) signal0[it / 2] else im[it / 2] })


        // USING FFTW //
        /*lateinit var retObj: ComplexArray
        reLock.lock {
            fftw_init_threads()

            val signal = FloatArray(2 * signal0.size)
            val result = FloatArray(2 * signal0.size)

            val plan = getForwardPlan(signal0.size, signal, result)

            signal0.forEachIndexed { index, fl -> signal[index * 2] = fl }

            fftwf_execute(plan)

            retObj = ComplexArray(result)

            destroyPlan(plan)

            fftwf_cleanup_threads()
        }
        return retObj*/


        // USING JTRANSFORMS //
        val signal = FloatArray(signal0.size * 2) { if (it % 2 == 0) signal0[it / 2] else 0f }
        ffts[signal0.size]!!.complexForward(signal)
        return ComplexArray(signal)
    }

    fun fft(signal0: ComplexArray) {
        ffts[signal0.size]!!.complexForward(signal0.reim)
    }

    fun fftInto(signal0: ComplexArray, out: ComplexArray) {
        System.arraycopy(signal0.reim, 0, out.reim, 0, signal0.reim.size)
        ffts[signal0.size]!!.complexForward(out.reim)
    }

    // org.apache.commons.math3.transform.FastFouriesTransformer.java:404
    fun ifftAndGetReal(signal0: ComplexArray): FloatArray {
//        val re = FloatArray(signal0.size) { signal0.reim[it * 2] }
//        val im = FloatArray(signal0.size) { signal0.reim[it * 2 + 1] }
//        transformInPlace(re, im, re.size, DftNormalization.STANDARD, TransformType.INVERSE)
//        return re


        // USING FFTW //
        /*lateinit var re: FloatArray
        reLock.lock {
            fftw_init_threads()

            val signal = signal0.reim
            val result = FloatArray(2 * signal0.size)

            val plan = getBackwardPlan(signal0.size, signal, result)

            fftwf_execute(plan)

            re = FloatArray(signal0.size) { result[it * 2] }

            destroyPlan(plan)

            fftwf_cleanup_threads()
        }
        return re*/


        // USING JTRANSFORMS //
        ffts[signal0.size]!!.complexInverse(signal0.reim, true)
        return signal0.getReal()
    }

    fun ifftAndGetReal(signal0: ComplexArray, output: FloatArray) {
        ffts[signal0.size]!!.complexInverse(signal0.reim, true)
        for (i in 0 until signal0.size) {
            output[i] = signal0.reim[i * 2]
        }
    }

    // org.apache.commons.math3.transform.FastFouriesTransformer.java:214
    /**
     * Computes the standard transform of the specified complex data. The
     * computation is done in place. The input data is laid out as follows
     * <ul>
     *   <li>{@code dataRI[0][i]} is the real part of the {@code i}-th data point,</li>
     *   <li>{@code dataRI[1][i]} is the imaginary part of the {@code i}-th data point.</li>
     * </ul>
     *
     * @param dataRI the two dimensional array of real and imaginary parts of the data
     * @param normalization the normalization to be applied to the transformed data
     * @param type the type of transform (forward, inverse) to be performed
     * @throws DimensionMismatchException if the number of rows of the specified
     *   array is not two, or the array is not rectangular
     * @throws MathIllegalArgumentException if the number of data points is not
     *   a power of two
     */
    private fun transformInPlace(dataR: FloatArray, dataI: FloatArray, n: Int, normalization: DftNormalization, type: TransformType) {

        /*if (n == 1) {
            return
        }
        else if (n == 2) {
            val srcR0 = dataR[0]
            val srcI0 = dataI[0]
            val srcR1 = dataR[1]
            val srcI1 = dataI[1]

            // X_0 = x_0 + x_1
            dataR[0] = srcR0 + srcR1
            dataI[0] = srcI0 + srcI1
            // X_1 = x_0 - x_1
            dataR[1] = srcR0 - srcR1
            dataI[1] = srcI0 - srcI1
            normalizeTransformedData(dataRI, normalization, type)
            return
        }*/

        bitReversalShuffle2(dataR, dataI)

        // Do 4-term DFT.
        if (type == TransformType.INVERSE) {
            var i0 = 0
            while (i0 < n) {
                val i1 = i0 + 1
                val i2 = i0 + 2
                val i3 = i0 + 3
                val srcR0 = dataR[i0]
                val srcI0 = dataI[i0]
                val srcR1 = dataR[i2]
                val srcI1 = dataI[i2]
                val srcR2 = dataR[i1]
                val srcI2 = dataI[i1]
                val srcR3 = dataR[i3]
                val srcI3 = dataI[i3]

                // 4-term DFT
                // X_0 = x_0 + x_1 + x_2 + x_3
                dataR[i0] = srcR0 + srcR1 + srcR2 + srcR3
                dataI[i0] = srcI0 + srcI1 + srcI2 + srcI3
                // X_1 = x_0 - x_2 + j * (x_3 - x_1)
                dataR[i1] = srcR0 - srcR2 + (srcI3 - srcI1)
                dataI[i1] = srcI0 - srcI2 + (srcR1 - srcR3)
                // X_2 = x_0 - x_1 + x_2 - x_3
                dataR[i2] = srcR0 - srcR1 + srcR2 - srcR3
                dataI[i2] = srcI0 - srcI1 + srcI2 - srcI3
                // X_3 = x_0 - x_2 + j * (x_1 - x_3)
                dataR[i3] = srcR0 - srcR2 + (srcI1 - srcI3)
                dataI[i3] = srcI0 - srcI2 + (srcR3 - srcR1)
                i0 += 4
            }
        }
        else {
            var i0 = 0
            while (i0 < n) {
                val i1 = i0 + 1
                val i2 = i0 + 2
                val i3 = i0 + 3
                val srcR0 = dataR[i0]
                val srcI0 = dataI[i0]
                val srcR1 = dataR[i2]
                val srcI1 = dataI[i2]
                val srcR2 = dataR[i1]
                val srcI2 = dataI[i1]
                val srcR3 = dataR[i3]
                val srcI3 = dataI[i3]

                // 4-term DFT
                // X_0 = x_0 + x_1 + x_2 + x_3
                dataR[i0] = srcR0 + srcR1 + srcR2 + srcR3
                dataI[i0] = srcI0 + srcI1 + srcI2 + srcI3
                // X_1 = x_0 - x_2 + j * (x_3 - x_1)
                dataR[i1] = srcR0 - srcR2 + (srcI1 - srcI3)
                dataI[i1] = srcI0 - srcI2 + (srcR3 - srcR1)
                // X_2 = x_0 - x_1 + x_2 - x_3
                dataR[i2] = srcR0 - srcR1 + srcR2 - srcR3
                dataI[i2] = srcI0 - srcI1 + srcI2 - srcI3
                // X_3 = x_0 - x_2 + j * (x_1 - x_3)
                dataR[i3] = srcR0 - srcR2 + (srcI3 - srcI1)
                dataI[i3] = srcI0 - srcI2 + (srcR1 - srcR3)
                i0 += 4
            }
        }

        var lastN0 = 4
        var lastLogN0 = 2
        while (lastN0 < n) {
            val n0 = lastN0 shl 1
            val logN0 = lastLogN0 + 1
            val wSubN0R = W_SUB_N_R[logN0]
            var wSubN0I = W_SUB_N_I[logN0]
            if (type == TransformType.INVERSE) {
                wSubN0I = -wSubN0I
            }

            // Combine even/odd transforms of size lastN0 into a transform of size N0 (lastN0 * 2).
            var destEvenStartIndex = 0
            while (destEvenStartIndex < n) {
                val destOddStartIndex = destEvenStartIndex + lastN0
                var wSubN0ToRR = 1f
                var wSubN0ToRI = 0f
                for (r in 0 until lastN0) {
                    val grR = dataR[destEvenStartIndex + r]
                    val grI = dataI[destEvenStartIndex + r]
                    val hrR = dataR[destOddStartIndex + r]
                    val hrI = dataI[destOddStartIndex + r]

                    // dest[destEvenStartIndex + r] = Gr + WsubN0ToR * Hr
                    dataR[destEvenStartIndex + r] = grR + wSubN0ToRR * hrR - wSubN0ToRI * hrI
                    dataI[destEvenStartIndex + r] = grI + wSubN0ToRR * hrI + wSubN0ToRI * hrR
                    // dest[destOddStartIndex + r] = Gr - WsubN0ToR * Hr
                    dataR[destOddStartIndex + r] = grR - (wSubN0ToRR * hrR - wSubN0ToRI * hrI)
                    dataI[destOddStartIndex + r] = grI - (wSubN0ToRR * hrI + wSubN0ToRI * hrR)

                    // WsubN0ToR *= WsubN0R
                    val nextWsubN0ToRR = wSubN0ToRR * wSubN0R - wSubN0ToRI * wSubN0I
                    val nextWsubN0ToRI = wSubN0ToRR * wSubN0I + wSubN0ToRI * wSubN0R
                    wSubN0ToRR = nextWsubN0ToRR
                    wSubN0ToRI = nextWsubN0ToRI
                }
                destEvenStartIndex += n0
            }
            lastN0 = n0
            lastLogN0 = logN0
        }

        normalizeTransformedData(dataR, dataI, n, normalization, type)
    }


    /**
     * Applies the proper normalization to the specified transformed data.
     *
     * @param dataRI the unscaled transformed data
     * @param normalization the normalization to be applied
     * @param type the type of transform (forward, inverse) which resulted in the specified data
     */
    private fun normalizeTransformedData(
        dataR: FloatArray, dataI: FloatArray, n: Int,
        normalization: DftNormalization, type: TransformType
    ) {
//        assert(dataI.size == n)
//        when (normalization) {
//            DftNormalization.STANDARD ->
                if (type == TransformType.INVERSE) {
                    val scaleFactor = 1f / n.toFloat()
                    var i = 0
                    while (i < n) {
                        dataR[i] *= scaleFactor
                        dataI[i] *= scaleFactor
                        i++
                    }
                }

        /*    DftNormalization.UNITARY -> {
                val scaleFactor = (1.0 / FastMath.sqrt(n.toDouble())).toFloat()
                var i = 0
                while (i < n) {
                    dataR[i] *= scaleFactor
                    dataI[i] *= scaleFactor
                    i++
                }
            }

            else -> throw MathIllegalStateException()
        }*/
    }

    /**
     * Performs identical index bit reversal shuffles on two arrays of identical
     * size. Each element in the array is swapped with another element based on
     * the bit-reversal of the index. For example, in an array with length 16,
     * item at binary index 0011 (decimal 3) would be swapped with the item at
     * binary index 1100 (decimal 12).
     *
     * @param a the first array to be shuffled
     * @param b the second array to be shuffled
     */
    private fun bitReversalShuffle2(a: FloatArray, b: FloatArray) {
        val n = a.size
        assert(b.size == n)
        val halfOfN = n shr 1
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                // swap indices i & j
                var temp = a[i]
                a[i] = a[j]
                a[j] = temp
                temp = b[i]
                b[i] = b[j]
                b[j] = temp
            }
            var k = halfOfN
            while (k in 1..j) {
                j -= k
                k = k shr 1
            }
            j += k
        }
    }

    private val W_SUB_N_R = FFTConsts.W_SUB_N_R.map { it.toFloat() }.toFloatArray()
    private val W_SUB_N_I = FFTConsts.W_SUB_N_I.map { it.toFloat() }.toFloatArray()

}