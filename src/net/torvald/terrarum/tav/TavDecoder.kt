package net.torvald.terrarum.tav

import com.badlogic.gdx.graphics.Pixmap
import io.airlift.compress.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * TAV file demuxer and frame/audio coordinator.
 * Owns the InputStream, demuxes packets, and decodes video+audio on a background thread.
 * Provides lock-free SPSC ring buffers for thread-safe GL and audio mixer consumption.
 */
class TavDecoder(
    private val stream: InputStream,
    val looping: Boolean = false
) {

    companion object {
        private const val FRAME_RING_SIZE   = 32
        private const val AUDIO_RING_SIZE   = 65536
        private const val BACK_PRESSURE_SLEEP_MS = 2L

        private val TAV_MAGIC = byteArrayOf(0x1F, 'T'.code.toByte(), 'S'.code.toByte(), 'V'.code.toByte(),
            'M'.code.toByte(), 'T'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte())
    }

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    lateinit var header: TavVideoDecode.TavHeader
        private set

    val videoWidth:   Int   get() = header.width
    val videoHeight:  Int   get() = header.height
    val fps:          Int   get() = header.fps
    val totalFrames:  Long  get() = header.totalFrames
    val hasAudio:     Boolean get() = header.hasAudio
    val isPerceptual: Boolean get() = header.isPerceptual
    val isMonoblock:  Boolean get() = header.isMonoblock

    // -------------------------------------------------------------------------
    // Ring buffers
    // -------------------------------------------------------------------------

    // Video: pre-allocated Pixmap ring buffer
    private lateinit var frameRing: Array<Pixmap>
    val frameReadIdx  = AtomicInteger(0)
    val frameWriteIdx = AtomicInteger(0)

    // Audio: circular Float32 ring
    private lateinit var audioRingL: FloatArray
    private lateinit var audioRingR: FloatArray
    val audioReadPos  = AtomicLong(0L)
    val audioWritePos = AtomicLong(0L)

    // -------------------------------------------------------------------------
    // Thread state
    // -------------------------------------------------------------------------

    private var decodeThread: Thread? = null
    val shouldStop = AtomicBoolean(false)
    val isFinished = AtomicBoolean(false)

    // -------------------------------------------------------------------------
    // Codec state
    // -------------------------------------------------------------------------

    private var prevCoeffsY:  FloatArray? = null
    private var prevCoeffsCo: FloatArray? = null
    private var prevCoeffsCg: FloatArray? = null

    private val tadState = TadDecode.TadDecoderState()

    private var frameCounter = 0
    private var gopFrameCounter = 0  // for grain synthesis RNG continuity

    // -------------------------------------------------------------------------
    // Looping support: remember stream position after header
    // -------------------------------------------------------------------------

    private var streamBuffer: ByteArray? = null      // null when not a resettable stream
    private var headerSize = 0

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    init {
        // Buffer the stream to support reset for looping
        val rawBytes = stream.readBytes()
        streamBuffer = rawBytes
        headerSize = parseHeader(rawBytes)
    }

    private fun parseHeader(bytes: ByteArray): Int {
        // Verify magic
        for (i in 0..7) {
            if (bytes[i] != TAV_MAGIC[i]) throw IllegalArgumentException("Not a TAV file (magic mismatch)")
        }

        var ptr = 8
        val version      = bytes[ptr++].toInt() and 0xFF
        val width        = ((bytes[ptr].toInt() and 0xFF) or ((bytes[ptr+1].toInt() and 0xFF) shl 8)).also { ptr += 2 }
        val height       = ((bytes[ptr].toInt() and 0xFF) or ((bytes[ptr+1].toInt() and 0xFF) shl 8)).also { ptr += 2 }
        val fps          = bytes[ptr++].toInt() and 0xFF
        val totalFrames  = (
            (bytes[ptr  ].toLong() and 0xFF) or
            ((bytes[ptr+1].toLong() and 0xFF) shl 8) or
            ((bytes[ptr+2].toLong() and 0xFF) shl 16) or
            ((bytes[ptr+3].toLong() and 0xFF) shl 24)
        ).also { ptr += 4 }
        val waveletFilter   = bytes[ptr++].toInt() and 0xFF
        val decompLevels    = bytes[ptr++].toInt() and 0xFF
        val qIndexY         = bytes[ptr++].toInt() and 0xFF
        val qIndexCo        = bytes[ptr++].toInt() and 0xFF
        val qIndexCg        = bytes[ptr++].toInt() and 0xFF
        val extraFlags      = bytes[ptr++].toInt() and 0xFF
        val videoFlags      = bytes[ptr++].toInt() and 0xFF
        val encoderQuality  = bytes[ptr++].toInt() and 0xFF
        val channelLayout   = bytes[ptr++].toInt() and 0xFF
        val entropyCoder    = bytes[ptr++].toInt() and 0xFF
        val encoderPreset   = bytes[ptr++].toInt() and 0xFF
        ptr += 2  // reserved + device orientation (ignored) + file role

        header = TavVideoDecode.TavHeader(
            version = version, width = width, height = height,
            fps = fps, totalFrames = totalFrames,
            waveletFilter = waveletFilter, decompLevels = decompLevels,
            qIndexY = qIndexY, qIndexCo = qIndexCo, qIndexCg = qIndexCg,
            extraFlags = extraFlags, videoFlags = videoFlags,
            encoderQuality = encoderQuality, channelLayout = channelLayout,
            entropyCoder = entropyCoder, encoderPreset = encoderPreset
        )

        return ptr  // byte offset to first packet
    }

    private fun allocateBuffers() {
        frameRing = Array(FRAME_RING_SIZE) {
            Pixmap(videoWidth, videoHeight, Pixmap.Format.RGBA8888)
        }
        audioRingL = FloatArray(AUDIO_RING_SIZE)
        audioRingR = FloatArray(AUDIO_RING_SIZE)
    }

    fun start() {
        allocateBuffers()
        shouldStop.set(false)
        isFinished.set(false)

        decodeThread = Thread(::decodeLoop, "tav-decode").also {
            it.isDaemon = true
            it.start()
        }
    }

    fun stop() {
        shouldStop.set(true)
        decodeThread?.join(2000)
        decodeThread = null
    }

    fun dispose() {
        stop()
        if (::frameRing.isInitialized) {
            for (px in frameRing) px.dispose()
        }
    }

    // -------------------------------------------------------------------------
    // Decode loop (background thread)
    // -------------------------------------------------------------------------

    private fun decodeLoop() {
        val bytes = streamBuffer ?: return
        var ptr = headerSize

        try {
            while (!shouldStop.get()) {
                if (ptr >= bytes.size) {
                    if (looping) {
                        ptr = headerSize
                        prevCoeffsY = null; prevCoeffsCo = null; prevCoeffsCg = null
                        tadState.prevYL = 0f; tadState.prevYR = 0f
                        continue
                    } else {
                        isFinished.set(true)
                        break
                    }
                }

                val packetType = bytes[ptr++].toInt() and 0xFF

                when (packetType) {
                    // --- Special fixed-size packets (no payload size) ---
                    0xFF -> { /* sync, no-op */ }
                    0xFE -> { /* NTSC sync, no-op */ }
                    0x00 -> { /* no-op */ }
                    0xF0 -> { /* loop point start, no-op */ }
                    0xF1 -> { /* loop point end, no-op */ }
                    0xFC -> {
                        // GOP sync: 1 extra byte (frame count)
                        if (ptr < bytes.size) ptr++  // skip frame count byte
                    }
                    0xFD -> {
                        // Timecode: 8-byte uint64 nanosecond timestamp
                        ptr += 8
                    }

                    // --- Video packets ---
                    0x10, 0x11 -> {
                        val payloadSize = readInt32LE(bytes, ptr); ptr += 4
                        if (ptr + payloadSize > bytes.size) { isFinished.set(true); break }
                        val payload = bytes.copyOfRange(ptr, ptr + payloadSize); ptr += payloadSize

                        val blockData = if (!header.noZstd)
                            ZstdInputStream(ByteArrayInputStream(payload)).use { it.readBytes() }
                        else payload

                        // Back-pressure: wait until there's space in the ring
                        while (frameRingFull() && !shouldStop.get()) Thread.sleep(BACK_PRESSURE_SLEEP_MS)
                        if (shouldStop.get()) break

                        val result = TavVideoDecode.decodeFrame(
                            blockData, header,
                            prevCoeffsY, prevCoeffsCo, prevCoeffsCg,
                            frameCounter
                        )
                        prevCoeffsY  = result.coeffsY
                        prevCoeffsCo = result.coeffsCo
                        prevCoeffsCg = result.coeffsCg

                        val rgba = result.rgba
                        if (rgba != null) {
                            writeFrameToRing(rgba)
                        } else {
                            // SKIP frame: duplicate the previous ring entry
                            duplicateLastFrame()
                        }
                        frameCounter++
                    }

                    0x12 -> {
                        // GOP Unified
                        val gopSize = bytes[ptr++].toInt() and 0xFF
                        val payloadSize = readInt32LE(bytes, ptr); ptr += 4
                        if (ptr + payloadSize > bytes.size) { isFinished.set(true); break }
                        val payload = bytes.copyOfRange(ptr, ptr + payloadSize); ptr += payloadSize

                        val frames = TavVideoDecode.decodeGop(payload, header, gopSize, frameCounter = gopFrameCounter)
                        gopFrameCounter += gopSize

                        for (rgba in frames) {
                            while (frameRingFull() && !shouldStop.get()) Thread.sleep(BACK_PRESSURE_SLEEP_MS)
                            if (shouldStop.get()) break
                            writeFrameToRing(rgba)
                            frameCounter++
                        }
                    }

                    // --- Audio packets ---
                    0x21 -> {
                        val payloadSize = readInt32LE(bytes, ptr); ptr += 4
                        val payload = bytes.copyOfRange(ptr, ptr + payloadSize); ptr += payloadSize
                        val pcm = TadDecode.decodePcm8(payload)
                        writeAudioToRing(pcm.first, pcm.second, pcm.first.size)
                    }

                    0x22 -> {
                        val payloadSize = readInt32LE(bytes, ptr); ptr += 4
                        val payload = bytes.copyOfRange(ptr, ptr + payloadSize); ptr += payloadSize
                        val pcm = TadDecode.decodePcm16(payload)
                        writeAudioToRing(pcm.first, pcm.second, pcm.first.size)
                    }

                    0x24 -> {
                        // TAD packet structure:
                        // uint16 sampleCount, uint32 outerSize (=compressedSize+7)
                        // TAD chunk header: uint16 sampleCount, uint8 maxIndex, uint32 compressedSize
                        // * Zstd payload
                        val sampleCount  = readInt16LE(bytes, ptr); ptr += 2
                        val outerSize    = readInt32LE(bytes, ptr); ptr += 4  // = compSize + 7

                        val chunkSamples = readInt16LE(bytes, ptr); ptr += 2
                        val maxIndex     = bytes[ptr++].toInt() and 0xFF
                        val compSize     = readInt32LE(bytes, ptr); ptr += 4

                        if (ptr + compSize > bytes.size) { isFinished.set(true); break }
                        val payload = bytes.copyOfRange(ptr, ptr + compSize); ptr += compSize

                        while (audioRingFull(chunkSamples) && !shouldStop.get()) Thread.sleep(BACK_PRESSURE_SLEEP_MS)
                        if (shouldStop.get()) break

                        try {
                            val pcm = TadDecode.decodeTadChunk(payload, chunkSamples, maxIndex, tadState)
                            writeAudioToRing(pcm.first, pcm.second, chunkSamples)
                        } catch (e: Exception) {
                            // Silently drop corrupted audio packets
                        }
                    }

                    // --- Extended header and metadata: read and skip ---
                    0xEF -> {
                        // TAV extended header: uint16 num_kvp, then key-value pairs
                        if (ptr + 2 > bytes.size) { isFinished.set(true); break }
                        val numKvp = readInt16LE(bytes, ptr); ptr += 2
                        repeat(numKvp) {
                            if (ptr + 5 <= bytes.size) {
                                ptr += 4  // key[4]
                                val valueType = bytes[ptr++].toInt() and 0xFF
                                val valueSize = when (valueType) {
                                    0x00 -> 2; 0x01 -> 3; 0x02 -> 4; 0x03 -> 6; 0x04 -> 8
                                    0x10 -> { val len = readInt16LE(bytes, ptr); ptr += 2; len }
                                    else -> 0
                                }
                                ptr += valueSize
                            }
                        }
                    }

                    in 0xE0..0xEE -> {
                        // Standard metadata: uint32 size, * payload
                        val payloadSize = readInt32LE(bytes, ptr); ptr += 4
                        ptr += payloadSize
                    }

                    else -> {
                        // Unknown packet with payload: uint32 size + payload
                        if (ptr + 4 <= bytes.size) {
                            val payloadSize = readInt32LE(bytes, ptr); ptr += 4
                            ptr += payloadSize
                        } else {
                            isFinished.set(true); break
                        }
                    }
                }
            }
        } catch (e: InterruptedException) {
            // Thread interrupted, exit cleanly
        } catch (e: Exception) {
            System.err.println("[TavDecoder] Decode error: ${e.message}")
        }

        if (!shouldStop.get()) isFinished.set(true)
    }

    // -------------------------------------------------------------------------
    // Frame ring operations
    // -------------------------------------------------------------------------

    private fun frameRingFull(): Boolean {
        val write = frameWriteIdx.get()
        val read  = frameReadIdx.get()
        return ((write + 1) % FRAME_RING_SIZE) == (read % FRAME_RING_SIZE)
    }

    private fun writeFrameToRing(rgba: ByteArray) {
        val idx = frameWriteIdx.get() % FRAME_RING_SIZE
        val px  = frameRing[idx]
        val buf = px.pixels
        buf.position(0)
        buf.put(rgba)
        buf.position(0)
        frameWriteIdx.incrementAndGet()
    }

    private fun duplicateLastFrame() {
        val writeIdx = frameWriteIdx.get()
        if (writeIdx == frameReadIdx.get()) return  // ring empty, nothing to duplicate
        val srcIdx  = ((writeIdx - 1 + FRAME_RING_SIZE) % FRAME_RING_SIZE)
        val dstIdx  = writeIdx % FRAME_RING_SIZE
        val src = frameRing[srcIdx]
        val dst = frameRing[dstIdx]
        src.pixels.position(0)
        dst.pixels.position(0)
        dst.pixels.put(src.pixels)
        src.pixels.position(0)
        dst.pixels.position(0)
        frameWriteIdx.incrementAndGet()
    }

    /** Returns the current decoded Pixmap without advancing, or null if no frame available. */
    fun getFramePixmap(): Pixmap? {
        val read  = frameReadIdx.get()
        val write = frameWriteIdx.get()
        if (read == write) return null
        return frameRing[read % FRAME_RING_SIZE]
    }

    /** Advance to the next decoded frame. */
    fun advanceFrame() {
        val read  = frameReadIdx.get()
        val write = frameWriteIdx.get()
        if (read != write) frameReadIdx.incrementAndGet()
    }

    // -------------------------------------------------------------------------
    // Audio ring operations
    // -------------------------------------------------------------------------

    private fun audioRingFull(needed: Int): Boolean {
        val avail = AUDIO_RING_SIZE - (audioWritePos.get() - audioReadPos.get()).toInt()
        return avail < needed
    }

    private fun writeAudioToRing(left: FloatArray, right: FloatArray, count: Int) {
        var writePos = audioWritePos.get()
        for (i in 0 until count) {
            val slot = (writePos % AUDIO_RING_SIZE).toInt()
            audioRingL[slot] = left[i]
            audioRingR[slot] = right[i]
            writePos++
        }
        audioWritePos.set(writePos)
    }

    /**
     * Read audio samples from the ring buffer into the caller's buffers.
     * @return number of samples actually read
     */
    fun readAudioSamples(bufL: FloatArray, bufR: FloatArray): Int {
        val available = (audioWritePos.get() - audioReadPos.get()).toInt().coerceAtMost(bufL.size)
        if (available <= 0) return 0
        var readPos = audioReadPos.get()
        for (i in 0 until available) {
            val slot = (readPos % AUDIO_RING_SIZE).toInt()
            bufL[i] = audioRingL[slot]
            bufR[i] = audioRingR[slot]
            readPos++
        }
        audioReadPos.set(readPos)
        return available
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun readInt32LE(data: ByteArray, offset: Int): Int {
        val b0 = data[offset  ].toInt() and 0xFF
        val b1 = data[offset+1].toInt() and 0xFF
        val b2 = data[offset+2].toInt() and 0xFF
        val b3 = data[offset+3].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun readInt16LE(data: ByteArray, offset: Int): Int {
        val b0 = data[offset  ].toInt() and 0xFF
        val b1 = data[offset+1].toInt() and 0xFF
        return b0 or (b1 shl 8)
    }
}
