package net.torvald.terrarum.tav

import io.airlift.compress.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import kotlin.math.roundToInt

/**
 * TAV video frame decoder (stateless pipeline functions).
 * Handles I-frames (0x10), P-frames (0x11) and GOP Unified (0x12) packets.
 * Supports YCoCg-R colour space only (odd version numbers).
 *
 * Ported from GraphicsJSR223Delegate.kt in the TSVM project.
 */
object TavVideoDecode {

    // Exponential quantiser lookup table (index → value)
    private val QLUT = intArrayOf(
        1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,
        31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,
        61,62,63,64,66,68,70,72,74,76,78,80,82,84,86,88,90,92,94,96,98,100,102,104,106,108,110,112,
        114,116,118,120,122,124,126,128,132,136,140,144,148,152,156,160,164,168,172,176,180,184,188,
        192,196,200,204,208,212,216,220,224,228,232,236,240,244,248,252,256,264,272,280,288,296,304,
        312,320,328,336,344,352,360,368,376,384,392,400,408,416,424,432,440,448,456,464,472,480,488,
        496,504,512,528,544,560,576,592,608,624,640,656,672,688,704,720,736,752,768,784,800,816,832,
        848,864,880,896,912,928,944,960,976,992,1008,1024,1056,1088,1120,1152,1184,1216,1248,1280,
        1312,1344,1376,1408,1440,1472,1504,1536,1568,1600,1632,1664,1696,1728,1760,1792,1824,1856,
        1888,1920,1952,1984,2016,2048,2112,2176,2240,2304,2368,2432,2496,2560,2624,2688,2752,2816,
        2880,2944,3008,3072,3136,3200,3264,3328,3392,3456,3520,3584,3648,3712,3776,3840,3904,3968,
        4032,4096
    )

    private val ANISOTROPY_MULT        = floatArrayOf(5.1f, 3.8f, 2.7f, 2.0f, 1.5f, 1.2f, 1.0f)
    private val ANISOTROPY_BIAS        = floatArrayOf(0.4f, 0.3f, 0.2f, 0.1f, 0.0f, 0.0f, 0.0f)
    private val ANISOTROPY_MULT_CHROMA = floatArrayOf(7.0f, 6.0f, 5.0f, 4.0f, 3.0f, 2.0f, 1.0f)
    private val ANISOTROPY_BIAS_CHROMA = floatArrayOf(1.0f, 0.8f, 0.6f, 0.4f, 0.2f, 0.0f, 0.0f)

    // -------------------------------------------------------------------------
    // Frame data class
    // -------------------------------------------------------------------------

    /** Decoded frame info returned to the caller. */
    data class TavHeader(
        val version: Int,
        val width: Int,
        val height: Int,
        val fps: Int,
        val totalFrames: Long,
        val waveletFilter: Int,
        val decompLevels: Int,
        val qIndexY: Int,
        val qIndexCo: Int,
        val qIndexCg: Int,
        val extraFlags: Int,
        val videoFlags: Int,
        val encoderQuality: Int,
        val channelLayout: Int,
        val entropyCoder: Int,
        val encoderPreset: Int
    ) {
        val hasAudio:    Boolean get() = (extraFlags and 0x01) != 0
        val isLooping:   Boolean get() = (extraFlags and 0x04) != 0
        val isInterlaced:Boolean get() = (videoFlags and 0x01) != 0
        val isLossless:  Boolean get() = (videoFlags and 0x04) != 0
        val noZstd:      Boolean get() = (videoFlags and 0x10) != 0
        val hasNoVideo:  Boolean get() = (videoFlags and 0x80) != 0
        /** Monoblock mode: version 3-6 */
        val isMonoblock: Boolean get() = version in 3..6

        val qY:  Int get() = QLUT.getOrElse(qIndexY  - 1) { 1 }
        val qCo: Int get() = QLUT.getOrElse(qIndexCo - 1) { 1 }
        val qCg: Int get() = QLUT.getOrElse(qIndexCg - 1) { 1 }
        val isPerceptual: Boolean get() = (version % 8) in 5..8
        /** Temporal motion coder: 0=Haar (version<=8), 1=CDF5/3 (version>8) */
        val temporalMotionCoder: Int get() = if (version > 8) 1 else 0
    }

    // -------------------------------------------------------------------------
    // Subband layout
    // -------------------------------------------------------------------------

    data class SubbandInfo(val level: Int, val subbandType: Int, val coeffStart: Int, val coeffCount: Int)

    fun calculateSubbandLayout(width: Int, height: Int, decompLevels: Int): List<SubbandInfo> {
        val subbands = mutableListOf<SubbandInfo>()
        val llWidth  = width  shr decompLevels
        val llHeight = height shr decompLevels
        subbands.add(SubbandInfo(decompLevels, 0, 0, llWidth * llHeight))
        var offset = llWidth * llHeight

        for (level in decompLevels downTo 1) {
            val lw = width  shr (decompLevels - level + 1)
            val lh = height shr (decompLevels - level + 1)
            val sz = lw * lh

            subbands.add(SubbandInfo(level, 1, offset, sz)); offset += sz
            subbands.add(SubbandInfo(level, 2, offset, sz)); offset += sz
            subbands.add(SubbandInfo(level, 3, offset, sz)); offset += sz
        }
        return subbands
    }

    // -------------------------------------------------------------------------
    // Perceptual weight calculation
    // -------------------------------------------------------------------------

    fun getPerceptualWeight(qIndex: Int, qYGlobal: Int, level0: Int, subbandType: Int, isChroma: Boolean, maxLevels: Int): Float {
        val level = 1.0f + ((level0 - 1.0f) / (maxLevels - 1.0f)) * 5.0f
        val qualityLevel = deriveEncoderQIndex(qIndex, qYGlobal)

        if (!isChroma) {
            if (subbandType == 0) return perceptualLL(level)
            val lh = perceptualLH(level)
            if (subbandType == 1) return lh
            val hl = perceptualHL(qualityLevel, lh)
            val fineDetail = if (level in 1.8f..2.2f) 0.92f else if (level in 2.8f..3.2f) 0.88f else 1.0f
            if (subbandType == 2) return hl * fineDetail
            return perceptualHH(lh, hl, level) * fineDetail
        } else {
            val base = perceptualChromaBasecurve(qualityLevel, level - 1)
            return when (subbandType) {
                0 -> 1.0f
                1 -> base.coerceAtLeast(1.0f)
                2 -> (base * ANISOTROPY_MULT_CHROMA[qualityLevel]).coerceAtLeast(1.0f)
                else -> (base * ANISOTROPY_MULT_CHROMA[qualityLevel] + ANISOTROPY_BIAS_CHROMA[qualityLevel]).coerceAtLeast(1.0f)
            }
        }
    }

    private fun deriveEncoderQIndex(qIndex: Int, qYGlobal: Int): Int {
        if (qIndex > 0) return qIndex - 1
        return when {
            qYGlobal >= 79 -> 0
            qYGlobal >= 47 -> 1
            qYGlobal >= 23 -> 2
            qYGlobal >= 11 -> 3
            qYGlobal >= 5  -> 4
            qYGlobal >= 2  -> 5
            else           -> 6
        }
    }

    private fun perceptualLH(level: Float): Float {
        val H4 = 1.2f; val K = 2f; val K12 = K * 12f; val x = level
        val Lx = H4 - ((K + 1f) / 15f) * (x - 4f)
        val C3 = -1f / 45f * (K12 + 92)
        val G3x = (-x / 180f) * (K12 + 5 * x * x - 60 * x + 252) - C3 + H4
        return if (level >= 4f) Lx else G3x
    }

    private fun perceptualHL(quality: Int, lh: Float): Float =
        lh * ANISOTROPY_MULT[quality] + ANISOTROPY_BIAS[quality]

    private fun perceptualHH(lh: Float, hl: Float, level: Float): Float {
        val kx = (kotlin.math.sqrt(level.toDouble()).toFloat() - 1f) * 0.5f + 0.5f
        return lh * (1f - kx) + hl * kx
    }

    private fun perceptualLL(level: Float): Float {
        val n = perceptualLH(level)
        val m = perceptualLH(level - 1) / n
        return n / m
    }

    private fun perceptualChromaBasecurve(qualityLevel: Int, level: Float): Float =
        1.0f - (1.0f / (0.5f * qualityLevel * qualityLevel + 1.0f)) * (level - 4f)

    // -------------------------------------------------------------------------
    // Dequantisation
    // -------------------------------------------------------------------------

    fun dequantisePerceptual(
        quantised: ShortArray, dequantised: FloatArray,
        subbands: List<SubbandInfo>,
        baseQuantiser: Float, isChroma: Boolean,
        qIndex: Int, qYGlobal: Int, decompLevels: Int
    ) {
        val weights = FloatArray(quantised.size) { 1.0f }
        for (sb in subbands) {
            val w = getPerceptualWeight(qIndex, qYGlobal, sb.level, sb.subbandType, isChroma, decompLevels)
            for (i in 0 until sb.coeffCount) {
                val idx = sb.coeffStart + i
                if (idx < weights.size) weights[idx] = w
            }
        }
        for (i in quantised.indices) {
            if (i < dequantised.size) dequantised[i] = quantised[i] * baseQuantiser * weights[i]
        }
    }

    fun dequantiseUniform(quantised: ShortArray, dequantised: FloatArray, baseQuantiser: Float) {
        for (i in quantised.indices) {
            if (i < dequantised.size) dequantised[i] = quantised[i] * baseQuantiser
        }
    }

    // -------------------------------------------------------------------------
    // Grain synthesis
    // -------------------------------------------------------------------------

    fun grainSynthesis(coeffs: FloatArray, width: Int, height: Int,
                        frameNum: Int, subbands: List<SubbandInfo>, qYGlobal: Int, encoderPreset: Int) {
        if ((encoderPreset and 0x02) != 0) return  // Anime preset: disable grain

        val noiseAmplitude = qYGlobal.coerceAtMost(32) * 0.8f

        for (sb in subbands) {
            if (sb.level == 0) continue  // Skip LL band

            for (i in 0 until sb.coeffCount) {
                val idx = sb.coeffStart + i
                if (idx >= coeffs.size) continue
                val y = idx / width
                val x = idx % width
                val rngVal = grainRng(frameNum.toUInt(), (sb.level + sb.subbandType * 31 + 16777619).toUInt(), x.toUInt(), y.toUInt())
                val noise = grainTriangularNoise(rngVal)
                coeffs[idx] -= noise * noiseAmplitude
            }
        }
    }

    private fun grainRng(frame: UInt, band: UInt, x: UInt, y: UInt): UInt {
        val key = frame * 0x9e3779b9u xor band * 0x7f4a7c15u xor (y shl 16) xor x
        var hash = key
        hash = hash xor (hash shr 16)
        hash = hash * 0x7feb352du
        hash = hash xor (hash shr 15)
        hash = hash * 0x846ca68bu
        hash = hash xor (hash shr 16)
        return hash
    }

    private fun grainTriangularNoise(rngVal: UInt): Float {
        val u1 = (rngVal and 0xFFFFu).toFloat() / 65535.0f
        val u2 = ((rngVal shr 16) and 0xFFFFu).toFloat() / 65535.0f
        return (u1 + u2) - 1.0f
    }

    // -------------------------------------------------------------------------
    // YCoCg-R to RGB
    // -------------------------------------------------------------------------

    /**
     * Convert YCoCg-R float arrays to RGBA8888 byte array.
     * Each pixel = 4 bytes (R, G, B, A=255).
     */
    fun ycocgrToRgba(y: FloatArray, co: FloatArray, cg: FloatArray,
                     width: Int, height: Int,
                     channelLayout: Int = 0): ByteArray {
        val out = ByteArray(width * height * 4)
        for (i in 0 until width * height) {
            val yv = y[i]; val cov = co[i]; val cgv = cg[i]
            val tmp = yv - cgv / 2.0f
            val g   = cgv + tmp
            val b   = tmp - cov / 2.0f
            val r   = cov + b

            out[i * 4    ] = r.roundToInt().coerceIn(0, 255).toByte()
            out[i * 4 + 1] = g.roundToInt().coerceIn(0, 255).toByte()
            out[i * 4 + 2] = b.roundToInt().coerceIn(0, 255).toByte()
            out[i * 4 + 3] = 0xFF.toByte()
        }
        return out
    }

    // -------------------------------------------------------------------------
    // Temporal quantiser scale helpers (for GOP decode)
    // -------------------------------------------------------------------------

    private fun getTemporalSubbandLevel(frameIdx: Int, numFrames: Int, temporalLevels: Int): Int {
        val framesPerLevel0 = numFrames shr temporalLevels
        return when {
            frameIdx < framesPerLevel0         -> 0
            frameIdx < (numFrames shr 1)       -> 1
            else                               -> 2
        }
    }

    private fun getTemporalQuantiserScale(encoderPreset: Int, temporalLevel: Int): Float {
        val beta  = if (encoderPreset and 0x01 == 1) 0.0f else 0.6f
        val kappa = if (encoderPreset and 0x01 == 1) 1.0f else 1.14f
        return Math.pow(2.0, (beta * Math.pow(temporalLevel.toDouble(), kappa.toDouble()))).toFloat()
    }

    // -------------------------------------------------------------------------
    // Coefficients from block data (significance-map or EZBC)
    // -------------------------------------------------------------------------

    private fun readInt32LE(data: ByteArray, offset: Int): Int {
        val b0 = data[offset  ].toInt() and 0xFF
        val b1 = data[offset+1].toInt() and 0xFF
        val b2 = data[offset+2].toInt() and 0xFF
        val b3 = data[offset+3].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    /**
     * Decode quantised coefficients from block data (single frame).
     * Supports EZBC (entropyCoder=1) and 2-bit significance map (entropyCoder=0).
     */
    private fun extractCoefficients(
        blockData: ByteArray, offset: Int,
        coeffCount: Int, channelLayout: Int, entropyCoder: Int,
        qY: ShortArray, qCo: ShortArray, qCg: ShortArray
    ) {
        if (entropyCoder == 1) {
            EzbcDecode.decode2D(blockData, offset, channelLayout, qY, qCo, qCg, null)
        } else {
            extractCoeffsSigMap(blockData, offset, coeffCount, channelLayout, qY, qCo, qCg)
        }
    }

    /** 2-bit significance map decoder (legacy format). */
    private fun extractCoeffsSigMap(
        data: ByteArray, offset: Int, coeffCount: Int, channelLayout: Int,
        outY: ShortArray, outCo: ShortArray, outCg: ShortArray
    ) {
        val hasY    = (channelLayout and 4) == 0
        val hasCoCg = (channelLayout and 2) == 0

        val mapBytes = (coeffCount * 2 + 7) / 8

        val yMapStart  = if (hasY)    { offset }                           else -1
        val coMapStart = if (hasCoCg) { offset + (if (hasY) mapBytes else 0) } else -1
        val cgMapStart = if (hasCoCg) { coMapStart + mapBytes }            else -1

        var yOthers = 0; var coOthers = 0; var cgOthers = 0

        fun countOthers(mapStart: Int): Int {
            var cnt = 0
            for (i in 0 until coeffCount) {
                val bitPos = i * 2
                val byteIdx = bitPos / 8; val bitOffset = bitPos % 8
                val byteVal = data[mapStart + byteIdx].toInt() and 0xFF
                var code = (byteVal shr bitOffset) and 0x03
                if (bitOffset == 7 && byteIdx + 1 < mapBytes) {
                    val nb = data[mapStart + byteIdx + 1].toInt() and 0xFF
                    code = (code and 0x01) or ((nb and 0x01) shl 1)
                }
                if (code == 3) cnt++
            }
            return cnt
        }

        if (hasY)    yOthers  = countOthers(yMapStart)
        if (hasCoCg) { coOthers = countOthers(coMapStart); cgOthers = countOthers(cgMapStart) }

        val numChannels = if (hasY && hasCoCg) 3 else if (hasY) 1 else 2
        var valueOffset = offset + mapBytes * numChannels

        val yValStart  = if (hasY)    { val s = valueOffset; valueOffset += yOthers  * 2; s } else -1
        val coValStart = if (hasCoCg) { val s = valueOffset; valueOffset += coOthers * 2; s } else -1
        val cgValStart = if (hasCoCg) { val s = valueOffset; valueOffset += cgOthers * 2; s } else -1

        fun decodeChannel(mapStart: Int, valStart: Int, out: ShortArray) {
            var vIdx = 0
            for (i in 0 until coeffCount) {
                val bitPos = i * 2; val byteIdx = bitPos / 8; val bitOffset = bitPos % 8
                val byteVal = data[mapStart + byteIdx].toInt() and 0xFF
                var code = (byteVal shr bitOffset) and 0x03
                if (bitOffset == 7 && byteIdx + 1 < mapBytes) {
                    code = (code and 0x01) or ((data[mapStart + byteIdx + 1].toInt() and 0x01) shl 1)
                }
                out[i] = when (code) {
                    0 -> 0
                    1 -> 1
                    2 -> (-1).toShort()
                    3 -> {
                        val vp = valStart + vIdx * 2; vIdx++
                        val lo = data[vp  ].toInt() and 0xFF
                        val hi = data[vp+1].toInt()
                        ((hi shl 8) or lo).toShort()
                    }
                    else -> 0
                }
            }
        }

        if (hasY)    decodeChannel(yMapStart,  yValStart,  outY)
        if (hasCoCg) { decodeChannel(coMapStart, coValStart, outCo); decodeChannel(cgMapStart, cgValStart, outCg) }
    }

    // -------------------------------------------------------------------------
    // I-frame / P-frame decode (monoblock only)
    // -------------------------------------------------------------------------

    /**
     * Decode an I-frame or P-frame packet payload (already Zstd-decompressed).
     * Returns RGBA8888 pixels and the new float coefficients for P-frame reference.
     *
     * @param blockData    decompressed block data (after ZstdInputStream)
     * @param header       parsed TAV header
     * @param prevCoeffsY/Co/Cg  previous frame coefficients for P-frame delta (null for I-frame)
     * @param frameNum     frame counter for grain synthesis RNG
     * @return Triple(rgbaPixels, newCoeffsY, newCo, newCg) — newCoeffs are null for GOP frames
     */
    fun decodeFrame(
        blockData: ByteArray,
        header: TavHeader,
        prevCoeffsY:  FloatArray?,
        prevCoeffsCo: FloatArray?,
        prevCoeffsCg: FloatArray?,
        frameNum: Int
    ): FrameDecodeResult {
        val width  = header.width
        val height = header.height
        val coeffCount = width * height

        var ptr = 0

        // Read tile header (4 bytes)
        val modeRaw = blockData[ptr++].toInt() and 0xFF
        val qYOverride  = blockData[ptr++].toInt() and 0xFF
        val qCoOverride = blockData[ptr++].toInt() and 0xFF
        val qCgOverride = blockData[ptr++].toInt() and 0xFF

        val baseMode   = modeRaw and 0x0F
        val haarNibble = modeRaw shr 4
        val haarLevel  = if (baseMode == 0x02 && haarNibble > 0) haarNibble + 1 else 0

        val qY  = if (qYOverride  != 0) QLUT[qYOverride  - 1] else header.qY
        val qCo = if (qCoOverride != 0) QLUT[qCoOverride - 1] else header.qCo
        val qCg = if (qCgOverride != 0) QLUT[qCgOverride - 1] else header.qCg

        val quantY   = ShortArray(coeffCount)
        val quantCo  = ShortArray(coeffCount)
        val quantCg  = ShortArray(coeffCount)

        val floatY   = FloatArray(coeffCount)
        val floatCo  = FloatArray(coeffCount)
        val floatCg  = FloatArray(coeffCount)

        val subbands = calculateSubbandLayout(width, height, header.decompLevels)

        when (baseMode) {
            0x00 -> { // SKIP - caller should copy previous frame
                return FrameDecodeResult(null, prevCoeffsY, prevCoeffsCo, prevCoeffsCg, frameMode = 'S')
            }
            0x01 -> { // INTRA
                extractCoefficients(blockData, ptr, coeffCount, header.channelLayout, header.entropyCoder,
                                    quantY, quantCo, quantCg)

                if (header.isPerceptual) {
                    dequantisePerceptual(quantY,  floatY,  subbands, qY.toFloat(),  false, header.encoderQuality, header.qY, header.decompLevels)
                    dequantisePerceptual(quantCo, floatCo, subbands, qCo.toFloat(), true,  header.encoderQuality, header.qY, header.decompLevels)
                    dequantisePerceptual(quantCg, floatCg, subbands, qCg.toFloat(), true,  header.encoderQuality, header.qY, header.decompLevels)
                } else {
                    dequantiseUniform(quantY,  floatY,  qY.toFloat())
                    dequantiseUniform(quantCo, floatCo, qCo.toFloat())
                    dequantiseUniform(quantCg, floatCg, qCg.toFloat())
                }

                grainSynthesis(floatY, width, height, frameNum, subbands, header.qY, header.encoderPreset)

                DwtUtil.inverseMultilevel2D(floatY,  width, height, header.decompLevels, header.waveletFilter)
                DwtUtil.inverseMultilevel2D(floatCo, width, height, header.decompLevels, header.waveletFilter)
                DwtUtil.inverseMultilevel2D(floatCg, width, height, header.decompLevels, header.waveletFilter)
            }
            0x02 -> { // DELTA
                extractCoefficients(blockData, ptr, coeffCount, header.channelLayout, header.entropyCoder,
                                    quantY, quantCo, quantCg)

                val deltaY  = FloatArray(coeffCount) { quantY[it].toFloat()  * qY }
                val deltaCo = FloatArray(coeffCount) { quantCo[it].toFloat() * qCo }
                val deltaCg = FloatArray(coeffCount) { quantCg[it].toFloat() * qCg }

                if (haarLevel > 0) {
                    DwtUtil.inverseMultilevel2D(deltaY,  width, height, haarLevel, 255)
                    DwtUtil.inverseMultilevel2D(deltaCo, width, height, haarLevel, 255)
                    DwtUtil.inverseMultilevel2D(deltaCg, width, height, haarLevel, 255)
                }

                val pY  = prevCoeffsY  ?: FloatArray(coeffCount)
                val pCo = prevCoeffsCo ?: FloatArray(coeffCount)
                val pCg = prevCoeffsCg ?: FloatArray(coeffCount)

                for (i in 0 until coeffCount) {
                    floatY[i]  = pY[i]  + deltaY[i]
                    floatCo[i] = pCo[i] + deltaCo[i]
                    floatCg[i] = pCg[i] + deltaCg[i]
                }

                grainSynthesis(floatY, width, height, frameNum, subbands, header.qY, header.encoderPreset)

                DwtUtil.inverseMultilevel2D(floatY,  width, height, header.decompLevels, header.waveletFilter)
                DwtUtil.inverseMultilevel2D(floatCo, width, height, header.decompLevels, header.waveletFilter)
                DwtUtil.inverseMultilevel2D(floatCg, width, height, header.decompLevels, header.waveletFilter)
            }
        }

        val rgba = ycocgrToRgba(floatY, floatCo, floatCg, width, height, header.channelLayout)
        return FrameDecodeResult(rgba, floatY.clone(), floatCo.clone(), floatCg.clone())
    }

    data class FrameDecodeResult(
        val rgba: ByteArray?,           // null on SKIP frames
        val coeffsY:  FloatArray?,
        val coeffsCo: FloatArray?,
        val coeffsCg: FloatArray?,
        val frameMode: Char = ' '
    )

    // -------------------------------------------------------------------------
    // GOP Unified decode (0x12 packet)
    // -------------------------------------------------------------------------

    /**
     * Decode a GOP Unified packet.
     * @param gopPayload  Zstd-compressed unified block data
     * @param header      TAV header
     * @param gopSize     number of frames in this GOP
     * @param frameCounter  global frame counter at start of GOP (for grain synthesis RNG)
     * @return list of RGBA8888 byte arrays, one per frame
     */
    fun decodeGop(
        gopPayload: ByteArray,
        header: TavHeader,
        gopSize: Int,
        temporalLevels: Int = 2,
        frameCounter: Int = 0
    ): List<ByteArray> {
        val width  = header.width
        val height = header.height
        val pixels = width * height

        // Decompress
        val decompressed = ZstdInputStream(ByteArrayInputStream(gopPayload)).use { it.readBytes() }

        // Extract per-frame quantised coefficients
        val quantisedCoeffs = decodeGopUnifiedBlock(decompressed, gopSize, pixels, header)

        val gopWidth  = header.width
        val gopHeight = header.height

        val gopY  = Array(gopSize) { FloatArray(pixels) }
        val gopCo = Array(gopSize) { FloatArray(pixels) }
        val gopCg = Array(gopSize) { FloatArray(pixels) }

        val subbands = calculateSubbandLayout(gopWidth, gopHeight, header.decompLevels)

        // Dequantise with temporal scaling
        for (t in 0 until gopSize) {
            val temporalLevel = getTemporalSubbandLevel(t, gopSize, temporalLevels)
            val temporalScale = getTemporalQuantiserScale(header.encoderPreset, temporalLevel)
            val baseQY  = kotlin.math.round(header.qY  * temporalScale).toFloat().coerceIn(1.0f, 4096.0f)
            val baseQCo = kotlin.math.round(header.qCo * temporalScale).toFloat().coerceIn(1.0f, 4096.0f)
            val baseQCg = kotlin.math.round(header.qCg * temporalScale).toFloat().coerceIn(1.0f, 4096.0f)

            if (header.isPerceptual) {
                dequantisePerceptual(quantisedCoeffs[t][0], gopY[t],  subbands, baseQY,  false, header.encoderQuality, header.qY, header.decompLevels)
                dequantisePerceptual(quantisedCoeffs[t][1], gopCo[t], subbands, baseQCo, true,  header.encoderQuality, header.qY, header.decompLevels)
                dequantisePerceptual(quantisedCoeffs[t][2], gopCg[t], subbands, baseQCg, true,  header.encoderQuality, header.qY, header.decompLevels)
            } else {
                for (i in 0 until pixels) {
                    gopY[t][i]  = quantisedCoeffs[t][0][i] * baseQY
                    gopCo[t][i] = quantisedCoeffs[t][1][i] * baseQCo
                    gopCg[t][i] = quantisedCoeffs[t][2][i] * baseQCg
                }
            }
        }

        // Grain synthesis on each GOP frame
        for (t in 0 until gopSize) {
            grainSynthesis(gopY[t], gopWidth, gopHeight, frameCounter + t, subbands, header.qY, header.encoderPreset)
        }

        // Inverse 3D DWT
        DwtUtil.inverseMultilevel3D(gopY,  gopWidth, gopHeight, gopSize, header.decompLevels, temporalLevels, header.waveletFilter, header.temporalMotionCoder)
        DwtUtil.inverseMultilevel3D(gopCo, gopWidth, gopHeight, gopSize, header.decompLevels, temporalLevels, header.waveletFilter, header.temporalMotionCoder)
        DwtUtil.inverseMultilevel3D(gopCg, gopWidth, gopHeight, gopSize, header.decompLevels, temporalLevels, header.waveletFilter, header.temporalMotionCoder)

        // Convert each frame to RGBA
        return (0 until gopSize).map { t ->
            ycocgrToRgba(gopY[t], gopCo[t], gopCg[t], width, height, header.channelLayout)
        }
    }

    /** Decode unified GOP block to per-frame per-channel ShortArrays. */
    private fun decodeGopUnifiedBlock(
        data: ByteArray, numFrames: Int, numPixels: Int, header: TavHeader
    ): Array<Array<ShortArray>> {
        val output = Array(numFrames) { Array(3) { ShortArray(numPixels) } }

        if (header.entropyCoder == 1) {
            // EZBC: [frame_size(4)][frame_ezbc]...
            var ptr2 = 0
            for (frame in 0 until numFrames) {
                if (ptr2 + 4 > data.size) break
                val frameSize = readInt32LE(data, ptr2); ptr2 += 4
                if (ptr2 + frameSize > data.size) break
                EzbcDecode.decode2D(data, ptr2, header.channelLayout,
                    output[frame][0], output[frame][1], output[frame][2], null)
                ptr2 += frameSize
            }
        } else {
            // 2-bit significance map (legacy), all frames concatenated
            decodeSigMapGop(data, numFrames, numPixels, header.channelLayout, output)
        }

        return output
    }

    private fun decodeSigMapGop(
        data: ByteArray, numFrames: Int, numPixels: Int, channelLayout: Int,
        output: Array<Array<ShortArray>>
    ) {
        val hasY    = (channelLayout and 4) == 0
        val hasCoCg = (channelLayout and 2) == 0
        val mapBytesPerFrame = (numPixels * 2 + 7) / 8

        var readPtr = 0
        val yMapsStart  = if (hasY)    { val s = readPtr; readPtr += mapBytesPerFrame * numFrames; s } else -1
        val coMapsStart = if (hasCoCg) { val s = readPtr; readPtr += mapBytesPerFrame * numFrames; s } else -1
        val cgMapsStart = if (hasCoCg) { val s = readPtr; readPtr += mapBytesPerFrame * numFrames; s } else -1

        var yOthers = 0; var coOthers = 0; var cgOthers = 0

        fun countOthers(mapsStart: Int): Int {
            var cnt = 0
            for (frame in 0 until numFrames) {
                val frameMapOffset = frame * mapBytesPerFrame
                for (i in 0 until numPixels) {
                    val bitPos = i * 2; val byteIdx = bitPos / 8; val bitOffset = bitPos % 8
                    val byteVal = data.getOrElse(mapsStart + frameMapOffset + byteIdx) { 0 }.toInt() and 0xFF
                    var code = (byteVal shr bitOffset) and 0x03
                    if (bitOffset == 7 && byteIdx + 1 < mapBytesPerFrame) {
                        val nb = data.getOrElse(mapsStart + frameMapOffset + byteIdx + 1) { 0 }.toInt() and 0xFF
                        code = (code and 0x01) or ((nb and 0x01) shl 1)
                    }
                    if (code == 3) cnt++
                }
            }
            return cnt
        }

        if (hasY)    yOthers  = countOthers(yMapsStart)
        if (hasCoCg) { coOthers = countOthers(coMapsStart); cgOthers = countOthers(cgMapsStart) }

        val yValStart  = readPtr; readPtr += yOthers  * 2
        val coValStart = readPtr; readPtr += coOthers * 2
        val cgValStart = readPtr

        var yVIdx = 0; var coVIdx = 0; var cgVIdx = 0

        for (frame in 0 until numFrames) {
            val frameMapOffset = frame * mapBytesPerFrame
            for (i in 0 until numPixels) {
                val bitPos = i * 2; val byteIdx = bitPos / 8; val bitOffset = bitPos % 8

                fun getCode(mapsStart: Int): Int {
                    val byteVal = data.getOrElse(mapsStart + frameMapOffset + byteIdx) { 0 }.toInt() and 0xFF
                    var code = (byteVal shr bitOffset) and 0x03
                    if (bitOffset == 7 && byteIdx + 1 < mapBytesPerFrame) {
                        val nb = data.getOrElse(mapsStart + frameMapOffset + byteIdx + 1) { 0 }.toInt() and 0xFF
                        code = (code and 0x01) or ((nb and 0x01) shl 1)
                    }
                    return code
                }

                fun readVal(valStart: Int, vIdx: Int): Short {
                    val vp = valStart + vIdx * 2
                    return if (vp + 1 < data.size) {
                        val lo = data[vp  ].toInt() and 0xFF
                        val hi = data[vp+1].toInt()
                        ((hi shl 8) or lo).toShort()
                    } else 0
                }

                if (hasY) {
                    output[frame][0][i] = when (getCode(yMapsStart)) {
                        1 -> 1; 2 -> (-1).toShort(); 3 -> { val v = readVal(yValStart, yVIdx); yVIdx++; v }; else -> 0
                    }
                }
                if (hasCoCg) {
                    output[frame][1][i] = when (getCode(coMapsStart)) {
                        1 -> 1; 2 -> (-1).toShort(); 3 -> { val v = readVal(coValStart, coVIdx); coVIdx++; v }; else -> 0
                    }
                    output[frame][2][i] = when (getCode(cgMapsStart)) {
                        1 -> 1; 2 -> (-1).toShort(); 3 -> { val v = readVal(cgValStart, cgVIdx); cgVIdx++; v }; else -> 0
                    }
                }
            }
        }
    }

}
