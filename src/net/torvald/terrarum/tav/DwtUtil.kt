package net.torvald.terrarum.tav

/**
 * Shared DWT (Discrete Wavelet Transform) utility functions.
 * Provides inverse CDF 9/7, CDF 5/3, and Haar transforms used by both
 * video and audio decoders.
 *
 * Ported from GraphicsJSR223Delegate.kt and AudioAdapter.kt in the TSVM project.
 */
object DwtUtil {

    // CDF 9/7 lifting constants
    private const val ALPHA = -1.586134342f
    private const val BETA  = -0.052980118f
    private const val GAMMA =  0.882911076f
    private const val DELTA =  0.443506852f
    private const val K     =  1.230174105f

    // -------------------------------------------------------------------------
    // 1D Transforms
    // -------------------------------------------------------------------------

    /**
     * Single-level 1D CDF 9/7 inverse lifting transform.
     * Layout: first half = low-pass coefficients, second half = high-pass.
     */
    fun inverse1D(data: FloatArray, length: Int) {
        if (length < 2) return

        val temp = FloatArray(length)
        val half = (length + 1) / 2

        for (i in 0 until half) {
            temp[i] = data[i]
        }
        for (i in 0 until length / 2) {
            if (half + i < length) temp[half + i] = data[half + i]
        }

        // Step 1: Undo scaling
        for (i in 0 until half) temp[i] /= K
        for (i in 0 until length / 2) {
            if (half + i < length) temp[half + i] *= K
        }

        // Step 2: Undo delta update
        for (i in 0 until half) {
            val dCurr = if (half + i < length) temp[half + i] else 0.0f
            val dPrev = if (i > 0 && half + i - 1 < length) temp[half + i - 1] else dCurr
            temp[i] -= DELTA * (dCurr + dPrev)
        }

        // Step 3: Undo gamma predict
        for (i in 0 until length / 2) {
            if (half + i < length) {
                val sCurr = temp[i]
                val sNext = if (i + 1 < half) temp[i + 1] else sCurr
                temp[half + i] -= GAMMA * (sCurr + sNext)
            }
        }

        // Step 4: Undo beta update
        for (i in 0 until half) {
            val dCurr = if (half + i < length) temp[half + i] else 0.0f
            val dPrev = if (i > 0 && half + i - 1 < length) temp[half + i - 1] else dCurr
            temp[i] -= BETA * (dCurr + dPrev)
        }

        // Step 5: Undo alpha predict
        for (i in 0 until length / 2) {
            if (half + i < length) {
                val sCurr = temp[i]
                val sNext = if (i + 1 < half) temp[i + 1] else sCurr
                temp[half + i] -= ALPHA * (sCurr + sNext)
            }
        }

        // Interleave reconstruction
        for (i in 0 until length) {
            if (i % 2 == 0) {
                data[i] = temp[i / 2]
            } else {
                val idx = i / 2
                data[i] = if (half + idx < length) temp[half + idx] else 0.0f
            }
        }
    }

    /**
     * Multi-level 1D CDF 9/7 inverse transform.
     * Uses exact forward-transform lengths in reverse to handle non-power-of-2 sizes.
     */
    fun inverseMultilevel1D(data: FloatArray, length: Int, levels: Int) {
        val lengths = IntArray(levels + 1)
        lengths[0] = length
        for (i in 1..levels) lengths[i] = (lengths[i - 1] + 1) / 2

        for (level in levels - 1 downTo 0) {
            inverse1D(data, lengths[level])
        }
    }

    /**
     * Single-level 2D CDF 9/7 inverse transform.
     * Column inverse first, then row inverse (matching encoder's row-then-column forward order).
     */
    fun inverse2D(data: FloatArray, width: Int, height: Int, currentWidth: Int, currentHeight: Int) {
        val maxSize = maxOf(width, height)
        val tempBuf = FloatArray(maxSize)

        // Column inverse transform (vertical)
        for (x in 0 until currentWidth) {
            for (y in 0 until currentHeight) tempBuf[y] = data[y * width + x]
            inverse1D(tempBuf, currentHeight)
            for (y in 0 until currentHeight) data[y * width + x] = tempBuf[y]
        }

        // Row inverse transform (horizontal)
        for (y in 0 until currentHeight) {
            for (x in 0 until currentWidth) tempBuf[x] = data[y * width + x]
            inverse1D(tempBuf, currentWidth)
            for (x in 0 until currentWidth) data[y * width + x] = tempBuf[x]
        }
    }

    /**
     * Multi-level 2D CDF 9/7 inverse transform.
     * Uses exact forward-transform dimension sequences.
     */
    fun inverseMultilevel2D(data: FloatArray, width: Int, height: Int, levels: Int,
                             filterType: Int = 1) {
        val widths  = IntArray(levels + 1)
        val heights = IntArray(levels + 1)
        widths[0]  = width
        heights[0] = height
        for (i in 1..levels) {
            widths[i]  = (widths[i - 1]  + 1) / 2
            heights[i] = (heights[i - 1] + 1) / 2
        }

        val maxSize = maxOf(width, height)
        val tempBuf = FloatArray(maxSize)

        for (level in levels - 1 downTo 0) {
            val cw = widths[level]
            val ch = heights[level]
            if (cw < 1 || ch < 1 || (cw == 1 && ch == 1)) continue

            // Column inverse
            for (x in 0 until cw) {
                for (y in 0 until ch) tempBuf[y] = data[y * width + x]
                applyInverse1DByFilter(tempBuf, ch, filterType)
                for (y in 0 until ch) data[y * width + x] = tempBuf[y]
            }

            // Row inverse
            for (y in 0 until ch) {
                for (x in 0 until cw) tempBuf[x] = data[y * width + x]
                applyInverse1DByFilter(tempBuf, cw, filterType)
                for (x in 0 until cw) data[y * width + x] = tempBuf[x]
            }
        }
    }

    private fun applyInverse1DByFilter(data: FloatArray, length: Int, filterType: Int) {
        when (filterType) {
            0    -> dwt53Inverse1D(data, length)
            1    -> inverse1D(data, length)
            255  -> haarInverse1D(data, length)
            else -> inverse1D(data, length)
        }
    }

    // -------------------------------------------------------------------------
    // Haar 1D Inverse
    // -------------------------------------------------------------------------

    fun haarInverse1D(data: FloatArray, length: Int) {
        if (length < 2) return

        val temp = FloatArray(length)
        val half = (length + 1) / 2

        for (i in 0 until half) {
            if (2 * i + 1 < length) {
                temp[2 * i]     = data[i] + data[half + i]
                temp[2 * i + 1] = data[i] - data[half + i]
            } else {
                temp[2 * i] = data[i]
            }
        }

        for (i in 0 until length) data[i] = temp[i]
    }

    // -------------------------------------------------------------------------
    // CDF 5/3 1D Inverse
    // -------------------------------------------------------------------------

    fun dwt53Inverse1D(data: FloatArray, length: Int) {
        if (length < 2) return

        val temp = FloatArray(length)
        val half = (length + 1) / 2

        System.arraycopy(data, 0, temp, 0, length)

        // Undo update step (low-pass)
        for (i in 0 until half) {
            val update = 0.25f * ((if (i > 0) temp[half + i - 1] else 0.0f) +
                                   (if (i < half - 1) temp[half + i] else 0.0f))
            temp[i] -= update
        }

        // Undo predict step and interleave
        for (i in 0 until half) {
            data[2 * i] = temp[i]
            val idx = 2 * i + 1
            if (idx < length) {
                val pred = 0.5f * (temp[i] + (if (i < half - 1) temp[i + 1] else temp[i]))
                data[idx] = temp[half + i] + pred
            }
        }
    }

    // -------------------------------------------------------------------------
    // Temporal inverse DWT (used for GOP decode)
    // -------------------------------------------------------------------------

    /**
     * Apply inverse temporal 1D DWT using Haar or CDF 5/3.
     * @param temporalMotionCoder 0=Haar, 1=CDF 5/3
     */
    fun temporalInverse1D(data: FloatArray, numFrames: Int, temporalMotionCoder: Int = 0) {
        if (numFrames < 2) return
        if (temporalMotionCoder == 0) haarInverse1D(data, numFrames)
        else dwt53Inverse1D(data, numFrames)
    }

    /**
     * Apply inverse 3D DWT to GOP data (spatial inverse first, then temporal inverse).
     */
    fun inverseMultilevel3D(
        gopData: Array<FloatArray>,
        width: Int, height: Int, numFrames: Int,
        spatialLevels: Int, temporalLevels: Int,
        spatialFilter: Int = 1, temporalMotionCoder: Int = 0
    ) {
        // Step 1: Inverse spatial 2D DWT on each temporal frame
        for (t in 0 until numFrames) {
            inverseMultilevel2D(gopData[t], width, height, spatialLevels, spatialFilter)
        }

        if (numFrames < 2) return

        // Step 2: Inverse temporal DWT at each spatial location
        val temporalLengths = IntArray(temporalLevels + 1)
        temporalLengths[0] = numFrames
        for (i in 1..temporalLevels) temporalLengths[i] = (temporalLengths[i - 1] + 1) / 2

        val temporalLine = FloatArray(numFrames)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixelIdx = y * width + x
                for (t in 0 until numFrames) temporalLine[t] = gopData[t][pixelIdx]

                for (level in temporalLevels - 1 downTo 0) {
                    val levelFrames = temporalLengths[level]
                    if (levelFrames >= 2) temporalInverse1D(temporalLine, levelFrames, temporalMotionCoder)
                }

                for (t in 0 until numFrames) gopData[t][pixelIdx] = temporalLine[t]
            }
        }
    }
}
