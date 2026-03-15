package net.torvald.terrarum.tav

import io.airlift.compress.zstd.ZstdInputStream
import java.io.ByteArrayInputStream

/**
 * TAD (TSVM Advanced Audio) decoder.
 * Decodes TAD chunks to Float32 stereo PCM at 32000 Hz.
 *
 * Ported from AudioAdapter.kt in the TSVM project.
 */
object TadDecode {

    // Coefficient scalars per subband (LL + 9 H bands, index 0=LL, 1-9=H bands L9..L1)
    private val COEFF_SCALARS = floatArrayOf(
        64.0f, 45.255f, 32.0f, 22.627f, 16.0f, 11.314f, 8.0f, 5.657f, 4.0f, 2.828f
    )

    // Base quantiser weight table: [channel 0=Mid][channel 1=Side]
    private val BASE_QUANTISER_WEIGHTS = arrayOf(
        floatArrayOf(4.0f, 2.0f, 1.8f, 1.6f, 1.4f, 1.2f, 1.0f, 1.0f, 1.3f, 2.0f),  // Mid
        floatArrayOf(6.0f, 5.0f, 2.6f, 2.4f, 1.8f, 1.3f, 1.0f, 1.0f, 1.6f, 3.2f)   // Side
    )

    private const val LAMBDA_FIXED = 6.0f
    private const val DWT_LEVELS = 9

    /**
     * Cross-chunk persistent state for the TAD de-emphasis IIR filter.
     */
    class TadDecoderState {
        var prevYL: Float = 0.0f
        var prevYR: Float = 0.0f
    }

    // -------------------------------------------------------------------------
    // Full TAD chunk decode
    // -------------------------------------------------------------------------

    /**
     * Decode a single TAD chunk payload.
     * Returns Pair(leftSamples, rightSamples) as Float32 in [-1, 1].
     *
     * @param payload    Zstd-compressed TAD chunk payload
     * @param sampleCount samples per channel
     * @param maxIndex   max quantiser index
     * @param state      persistent de-emphasis state (mutated in-place)
     */
    fun decodeTadChunk(
        payload: ByteArray,
        sampleCount: Int,
        maxIndex: Int,
        state: TadDecoderState
    ): Pair<FloatArray, FloatArray> {
        // Step 1: Zstd decompress
        val decompressed = ZstdInputStream(ByteArrayInputStream(payload)).use { it.readBytes() }

        // Step 2: EZBC 1D decode Mid and Side channels
        val quantMid  = ByteArray(sampleCount)
        val quantSide = ByteArray(sampleCount)

        val midBytesConsumed = EzbcDecode.decode1DChannel(decompressed, 0, decompressed.size, quantMid)
        EzbcDecode.decode1DChannel(
            decompressed, midBytesConsumed, decompressed.size - midBytesConsumed, quantSide
        )

        // Step 3 & 4: Lambda decompanding + dequantise
        val dwtMid  = FloatArray(sampleCount)
        val dwtSide = FloatArray(sampleCount)
        dequantiseCoeffs(0, quantMid,  dwtMid,  sampleCount, maxIndex)
        dequantiseCoeffs(1, quantSide, dwtSide, sampleCount, maxIndex)

        // Step 5: Inverse CDF 9/7 DWT (9 levels)
        DwtUtil.inverseMultilevel1D(dwtMid,  sampleCount, DWT_LEVELS)
        DwtUtil.inverseMultilevel1D(dwtSide, sampleCount, DWT_LEVELS)

        // Step 6: M/S to L/R
        val left  = FloatArray(sampleCount)
        val right = FloatArray(sampleCount)
        msToLR(dwtMid, dwtSide, left, right, sampleCount)

        // Step 7: Gamma expansion
        gammaExpand(left, right, sampleCount)

        // Step 8: De-emphasis IIR (persistent state)
        deemphasis(left, right, sampleCount, state)

        return Pair(left, right)
    }

    // -------------------------------------------------------------------------
    // PCM fallback decoders
    // -------------------------------------------------------------------------

    /** Decode Zstd-compressed interleaved PCMu8 stereo. Returns Float32 L/R. */
    fun decodePcm8(payload: ByteArray): Pair<FloatArray, FloatArray> {
        val decompressed = ZstdInputStream(ByteArrayInputStream(payload)).use { it.readBytes() }
        val sampleCount = decompressed.size / 2
        val left  = FloatArray(sampleCount)
        val right = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            val l = (decompressed[i * 2    ].toInt() and 0xFF) - 128
            val r = (decompressed[i * 2 + 1].toInt() and 0xFF) - 128
            left[i]  = l / 128.0f
            right[i] = r / 128.0f
        }
        return Pair(left, right)
    }

    /** Decode Zstd-compressed interleaved PCM16-LE stereo. Returns Float32 L/R. */
    fun decodePcm16(payload: ByteArray): Pair<FloatArray, FloatArray> {
        val decompressed = ZstdInputStream(ByteArrayInputStream(payload)).use { it.readBytes() }
        val sampleCount = decompressed.size / 4
        val left  = FloatArray(sampleCount)
        val right = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            val lLo = decompressed[i * 4    ].toInt() and 0xFF
            val lHi = decompressed[i * 4 + 1].toInt()
            val rLo = decompressed[i * 4 + 2].toInt() and 0xFF
            val rHi = decompressed[i * 4 + 3].toInt()
            left[i]  = ((lHi shl 8) or lLo).toShort() / 32768.0f
            right[i] = ((rHi shl 8) or rLo).toShort() / 32768.0f
        }
        return Pair(left, right)
    }

    // -------------------------------------------------------------------------
    // Internal pipeline stages
    // -------------------------------------------------------------------------

    private fun lambdaDecompand(quantVal: Byte, maxIndex: Int): Float {
        if (quantVal == 0.toByte()) return 0.0f
        val sign = if (quantVal < 0) -1 else 1
        var absIndex = kotlin.math.abs(quantVal.toInt()).coerceAtMost(maxIndex)
        val normalisedCdf = absIndex.toFloat() / maxIndex
        val cdf = 0.5f + normalisedCdf * 0.5f
        var absVal = -(1.0f / LAMBDA_FIXED) * kotlin.math.ln(2.0f * (1.0f - cdf))
        absVal = absVal.coerceIn(0.0f, 1.0f)
        return sign * absVal
    }

    private fun dequantiseCoeffs(
        channel: Int, quantised: ByteArray, coeffs: FloatArray,
        count: Int, maxIndex: Int
    ) {
        val firstBandSize = count shr DWT_LEVELS
        val sidebandStarts = IntArray(DWT_LEVELS + 2)
        sidebandStarts[0] = 0
        sidebandStarts[1] = firstBandSize
        for (i in 2..DWT_LEVELS + 1) {
            sidebandStarts[i] = sidebandStarts[i - 1] + (firstBandSize shl (i - 2))
        }

        for (i in 0 until count) {
            var sideband = DWT_LEVELS
            for (s in 0..DWT_LEVELS) {
                if (i < sidebandStarts[s + 1]) { sideband = s; break }
            }
            val normalisedVal = lambdaDecompand(quantised[i], maxIndex)
            val weight = BASE_QUANTISER_WEIGHTS[channel][sideband]
            coeffs[i] = normalisedVal * COEFF_SCALARS[sideband] * weight
        }
    }

    private fun msToLR(mid: FloatArray, side: FloatArray, left: FloatArray, right: FloatArray, count: Int) {
        for (i in 0 until count) {
            left[i]  = (mid[i] + side[i]).coerceIn(-1.0f, 1.0f)
            right[i] = (mid[i] - side[i]).coerceIn(-1.0f, 1.0f)
        }
    }

    private fun gammaExpand(left: FloatArray, right: FloatArray, count: Int) {
        for (i in 0 until count) {
            val x = left[i];  val a = kotlin.math.abs(x)
            left[i]  = if (x >= 0) a * a else -(a * a)

            val y = right[i]; val b = kotlin.math.abs(y)
            right[i] = if (y >= 0) b * b else -(b * b)
        }
    }

    // De-emphasis: y[n] = x[n] + 0.5 * y[n-1]  (state persists across chunks)
    private fun deemphasis(left: FloatArray, right: FloatArray, count: Int, state: TadDecoderState) {
        for (i in 0 until count) {
            val yL = left[i]  + 0.5f * state.prevYL
            state.prevYL = yL; left[i] = yL

            val yR = right[i] + 0.5f * state.prevYR
            state.prevYR = yR; right[i] = yR
        }
    }
}
