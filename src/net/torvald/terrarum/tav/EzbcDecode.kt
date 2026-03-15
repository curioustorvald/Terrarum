package net.torvald.terrarum.tav

/**
 * EZBC (Embedded Zero Block Coding) entropy decoder.
 * Provides both 2D (video) and 1D (audio) variants.
 *
 * Ported from GraphicsJSR223Delegate.kt and AudioAdapter.kt in the TSVM project.
 */
object EzbcDecode {

    // -------------------------------------------------------------------------
    // Shared bitstream reader
    // -------------------------------------------------------------------------

    private class BitstreamReader(private val data: ByteArray, private val startOffset: Int, private val size: Int) {
        private var bytePos = startOffset
        private var bitPos = 0
        private val endPos = startOffset + size

        fun readBit(): Int {
            if (bytePos >= endPos) return 0
            val bit = (data[bytePos].toInt() shr bitPos) and 1
            bitPos++
            if (bitPos == 8) { bitPos = 0; bytePos++ }
            return bit
        }

        fun readBits(numBits: Int): Int {
            var value = 0
            for (i in 0 until numBits) value = value or (readBit() shl i)
            return value
        }

        fun bytesConsumed(): Int = (bytePos - startOffset) + if (bitPos > 0) 1 else 0
    }

    // -------------------------------------------------------------------------
    // 2D EZBC decode (video coefficients, ShortArray output)
    // -------------------------------------------------------------------------

    /**
     * Decode a single EZBC channel (2D variant for video).
     * Header: 8-bit MSB bitplane, 16-bit width, 16-bit height.
     */
    fun decode2DChannel(ezbcData: ByteArray, offset: Int, size: Int, outputCoeffs: ShortArray) {
        val bs = BitstreamReader(ezbcData, offset, size)

        val msbBitplane = bs.readBits(8)
        val width  = bs.readBits(16)
        val height = bs.readBits(16)

        if (width * height != outputCoeffs.size) {
            System.err.println("[EZBC-2D] Dimension mismatch: ${width}x${height} != ${outputCoeffs.size}")
            return
        }

        outputCoeffs.fill(0)

        val significant = BooleanArray(outputCoeffs.size)

        data class Block(val x: Int, val y: Int, val w: Int, val h: Int)

        var insignificantQueue = ArrayList<Block>()
        var nextInsignificant  = ArrayList<Block>()
        var significantQueue   = ArrayList<Block>()
        var nextSignificant    = ArrayList<Block>()

        insignificantQueue.add(Block(0, 0, width, height))

        fun processSignificantBlockRecursive(block: Block, bitplane: Int, threshold: Int) {
            if (block.w == 1 && block.h == 1) {
                val idx = block.y * width + block.x
                val signBit = bs.readBit()
                outputCoeffs[idx] = (if (signBit == 1) -threshold else threshold).toShort()
                significant[idx] = true
                nextSignificant.add(block)
                return
            }

            var midX = block.w / 2; if (midX == 0) midX = 1
            var midY = block.h / 2; if (midY == 0) midY = 1

            // Top-left
            val tl = Block(block.x, block.y, midX, midY)
            if (bs.readBit() == 1) processSignificantBlockRecursive(tl, bitplane, threshold)
            else nextInsignificant.add(tl)

            // Top-right
            if (block.w > midX) {
                val tr = Block(block.x + midX, block.y, block.w - midX, midY)
                if (bs.readBit() == 1) processSignificantBlockRecursive(tr, bitplane, threshold)
                else nextInsignificant.add(tr)
            }

            // Bottom-left
            if (block.h > midY) {
                val bl = Block(block.x, block.y + midY, midX, block.h - midY)
                if (bs.readBit() == 1) processSignificantBlockRecursive(bl, bitplane, threshold)
                else nextInsignificant.add(bl)
            }

            // Bottom-right
            if (block.w > midX && block.h > midY) {
                val br = Block(block.x + midX, block.y + midY, block.w - midX, block.h - midY)
                if (bs.readBit() == 1) processSignificantBlockRecursive(br, bitplane, threshold)
                else nextInsignificant.add(br)
            }
        }

        for (bitplane in msbBitplane downTo 0) {
            val threshold = 1 shl bitplane

            for (block in insignificantQueue) {
                if (bs.readBit() == 0) nextInsignificant.add(block)
                else processSignificantBlockRecursive(block, bitplane, threshold)
            }

            for (block in significantQueue) {
                val idx = block.y * width + block.x
                if (bs.readBit() == 1) {
                    val bitValue = 1 shl bitplane
                    if (outputCoeffs[idx] < 0) outputCoeffs[idx] = (outputCoeffs[idx] - bitValue).toShort()
                    else outputCoeffs[idx] = (outputCoeffs[idx] + bitValue).toShort()
                }
                nextSignificant.add(block)
            }

            insignificantQueue = nextInsignificant;  nextInsignificant  = ArrayList()
            significantQueue   = nextSignificant;    nextSignificant    = ArrayList()
        }
    }

    /**
     * Decode all channels from an EZBC block.
     * Format: [size_y(4)][ezbc_y][size_co(4)][ezbc_co][size_cg(4)][ezbc_cg]...
     */
    fun decode2D(
        compressedData: ByteArray, offset: Int,
        channelLayout: Int,
        outputY: ShortArray?, outputCo: ShortArray?, outputCg: ShortArray?, outputAlpha: ShortArray?
    ) {
        val hasY     = (channelLayout and 4) == 0
        val hasCoCg  = (channelLayout and 2) == 0
        val hasAlpha = (channelLayout and 1) != 0

        var ptr = offset

        fun readSize(): Int {
            val b0 = compressedData[ptr  ].toInt() and 0xFF
            val b1 = compressedData[ptr+1].toInt() and 0xFF
            val b2 = compressedData[ptr+2].toInt() and 0xFF
            val b3 = compressedData[ptr+3].toInt() and 0xFF
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }

        if (hasY && outputY != null) {
            val sz = readSize(); ptr += 4
            decode2DChannel(compressedData, ptr, sz, outputY); ptr += sz
        }
        if (hasCoCg && outputCo != null) {
            val sz = readSize(); ptr += 4
            decode2DChannel(compressedData, ptr, sz, outputCo); ptr += sz
        }
        if (hasCoCg && outputCg != null) {
            val sz = readSize(); ptr += 4
            decode2DChannel(compressedData, ptr, sz, outputCg); ptr += sz
        }
        if (hasAlpha && outputAlpha != null) {
            val sz = readSize(); ptr += 4
            decode2DChannel(compressedData, ptr, sz, outputAlpha); ptr += sz
        }
    }

    // -------------------------------------------------------------------------
    // 1D EZBC decode (audio coefficients, ByteArray output)
    // -------------------------------------------------------------------------

    /**
     * Decode a single EZBC channel (1D variant for TAD audio).
     * Header: 8-bit MSB bitplane, 16-bit coefficient count.
     * @return number of bytes consumed from [input]
     */
    fun decode1DChannel(input: ByteArray, inputOffset: Int, inputSize: Int, coeffs: ByteArray): Int {
        val bs = BitstreamReader(input, inputOffset, inputSize)

        val msbBitplane = bs.readBits(8)
        val count = bs.readBits(16)

        coeffs.fill(0)

        data class Block(val start: Int, val length: Int)

        val states = BooleanArray(count)  // significant flags

        var insignificantQueue = ArrayList<Block>()
        var nextInsignificant  = ArrayList<Block>()
        var significantQueue   = ArrayList<Block>()
        var nextSignificant    = ArrayList<Block>()

        insignificantQueue.add(Block(0, count))

        fun processSignificantBlockRecursive(block: Block, bitplane: Int) {
            if (block.length == 1) {
                val idx = block.start
                val signBit = bs.readBit()
                val absVal = 1 shl bitplane
                coeffs[idx] = (if (signBit != 0) -absVal else absVal).toByte()
                states[idx] = true
                nextSignificant.add(block)
                return
            }

            val mid = maxOf(1, block.length / 2)

            val left = Block(block.start, mid)
            if (bs.readBit() != 0) processSignificantBlockRecursive(left, bitplane)
            else nextInsignificant.add(left)

            if (block.length > mid) {
                val right = Block(block.start + mid, block.length - mid)
                if (bs.readBit() != 0) processSignificantBlockRecursive(right, bitplane)
                else nextInsignificant.add(right)
            }
        }

        for (bitplane in msbBitplane downTo 0) {
            for (block in insignificantQueue) {
                if (bs.readBit() == 0) nextInsignificant.add(block)
                else processSignificantBlockRecursive(block, bitplane)
            }

            for (block in significantQueue) {
                val idx = block.start
                if (bs.readBit() != 0) {
                    val sign = if (coeffs[idx] < 0) -1 else 1
                    val absVal = kotlin.math.abs(coeffs[idx].toInt())
                    coeffs[idx] = (sign * (absVal or (1 shl bitplane))).toByte()
                }
                nextSignificant.add(block)
            }

            insignificantQueue = nextInsignificant;  nextInsignificant  = ArrayList()
            significantQueue   = nextSignificant;    nextSignificant    = ArrayList()
        }

        return bs.bytesConsumed()
    }
}
