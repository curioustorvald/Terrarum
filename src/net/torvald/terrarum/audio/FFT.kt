package net.torvald.terrarum.audio

import org.apache.commons.math3.exception.MathIllegalStateException
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.TransformType
import org.apache.commons.math3.util.FastMath

data class FComplex(var real: Float = 0f, var imaginary: Float = 0f) {
    operator fun times(other: FComplex) = FComplex(
        this.real * other.real - this.imaginary * other.imaginary,
        this.real * other.imaginary + this.imaginary * other.real
    )
}

/**
 * Modification of the code form JDSP and Apache Commons Math
 *
 * Created by minjaesong on 2023-11-25.
 */
object FFT {

    // org.apache.commons.math3.transform.FastFouriesTransformer.java:370
    fun fft(signal: FloatArray): Array<FComplex> {
        val dataRI = arrayOf(signal.copyOf(), FloatArray(signal.size))

        transformInPlace(dataRI, DftNormalization.STANDARD, TransformType.FORWARD)

        val output = dataRI.toComplexArray()

        return getComplex(output, false)
    }

    // org.apache.commons.math3.transform.FastFouriesTransformer.java:404
    fun ifftAndGetReal(y: Array<FComplex>): FloatArray {
        val dataRI = Array<FloatArray>(2) { FloatArray(y.size) }
        for (i in y.indices) {
            dataRI[0][i] = y[i].real
            dataRI[1][i] = y[i].imaginary
        }

        transformInPlace(dataRI, DftNormalization.STANDARD, TransformType.INVERSE)

        return dataRI[0]
    }

    private fun Array<FloatArray>.toComplexArray(): Array<FComplex> {
        return Array(this[0].size) {
            FComplex(this[0][it], this[1][it])
        }
    }

    // com.github.psambit9791.jdsp.transform.FastFourier.java:190
    /**
     * Returns the complex value of the fast fourier transformed sequence
     * @param onlyPositive Set to True if non-mirrored output is required
     * @throws java.lang.ExceptionInInitializerError if called before executing transform() method
     * @return Complex[] The complex FFT output
     */
    @Throws(ExceptionInInitializerError::class)
    fun getComplex(output: Array<FComplex>, onlyPositive: Boolean): Array<FComplex> {
        val dftout: Array<FComplex> = if (onlyPositive) {
            val numBins: Int = output.size / 2 + 1
            Array<FComplex>(numBins) { FComplex() }
        }
        else {
            Array<FComplex>(output.size) { FComplex() }
        }
        System.arraycopy(output, 0, dftout, 0, dftout.size)
        return dftout
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
    private fun transformInPlace(dataRI: Array<FloatArray>, normalization: DftNormalization, type: TransformType) {
        val dataR = dataRI[0]
        val dataI = dataRI[1]
        val n = dataR.size

        if (n == 1) {
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
        }

        bitReversalShuffle2(dataR, dataI)

        // Do 4-term DFT.

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

        normalizeTransformedData(dataRI, normalization, type)
    }


    /**
     * Applies the proper normalization to the specified transformed data.
     *
     * @param dataRI the unscaled transformed data
     * @param normalization the normalization to be applied
     * @param type the type of transform (forward, inverse) which resulted in the specified data
     */
    private fun normalizeTransformedData(
        dataRI: Array<FloatArray>,
        normalization: DftNormalization, type: TransformType
    ) {
        val dataR = dataRI[0]
        val dataI = dataRI[1]
        val n = dataR.size
        assert(dataI.size == n)
        when (normalization) {
            DftNormalization.STANDARD -> if (type == TransformType.INVERSE) {
                val scaleFactor = 1f / n.toFloat()
                var i = 0
                while (i < n) {
                    dataR[i] *= scaleFactor
                    dataI[i] *= scaleFactor
                    i++
                }
            }

            DftNormalization.UNITARY -> {
                val scaleFactor = (1.0 / FastMath.sqrt(n.toDouble())).toFloat()
                var i = 0
                while (i < n) {
                    dataR[i] *= scaleFactor
                    dataI[i] *= scaleFactor
                    i++
                }
            }

            else -> throw MathIllegalStateException()
        }
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