package net.torvald.terrarum.modulecomputers.tsvmperipheral

import net.torvald.terrarum.App
import net.torvald.terrarum.audio.AudioBank
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.gameactors.Actor
import net.torvald.tsvm.VM
import net.torvald.tsvm.peripheral.TsvmAudioSink

/**
 * Bridges a TSVM [net.torvald.tsvm.peripheral.AudioAdapter] into Terrarum's spatial audio mixer.
 *
 * The AudioAdapter has four internal playheads, each of which would normally play straight to a host
 * OpenAL device. We instead install [sinkFactory] as [VM.audioSinkFactory] so each playhead writes
 * into a per-playhead pair of ring buffers (left + right). Those rings are summed down into exactly
 * **two** Terrarum dynamic mixer tracks — one carrying the VM's left output, the other its right —
 * both of which follow the fixture in the world so the sound is positioned correctly.
 *
 * Lifecycle: [start] checks out the two tracks and begins playback; [stop] releases everything. The
 * sinks provide back-pressure (the writer blocks when a ring is full) so the AudioAdapter render
 * threads are paced to the real 32 kHz sample rate instead of free-running.
 *
 * Created by minjaesong on 2026-06-28.
 */
class TsvmAudioBridge(private val trackingTarget: Actor) {

    private val leftRings  = Array(CHANNELS) { FloatRing(RING_CAPACITY) }
    private val rightRings = Array(CHANNELS) { FloatRing(RING_CAPACITY) }

    private val leftBank  = StreamingMixBank("TSVM-AUDIO-L", leftRings)
    private val rightBank = StreamingMixBank("TSVM-AUDIO-R", rightRings)

    private var leftTrack: TerrarumAudioMixerTrack? = null
    private var rightTrack: TerrarumAudioMixerTrack? = null

    @Volatile private var running = false

    /** Install this on `vm.audioSinkFactory` BEFORE the AudioAdapter is constructed. */
    val sinkFactory: (VM, Int, Int, Int, Int) -> TsvmAudioSink =
        { _, channelIndex, _, bufSize, bufCount ->
            val ch = channelIndex.coerceIn(0, CHANNELS - 1)
            object : TsvmAudioSink {
                override val bufferSize = bufSize
                override val bufferCount = bufCount
                private var scratchL = FloatArray(512)
                private var scratchR = FloatArray(512)

                override fun writeStereoSamplesUI8(samples: ByteArray, offset: Int, numPairs: Int) {
                    if (!running) return
                    if (scratchL.size < numPairs) { scratchL = FloatArray(numPairs); scratchR = FloatArray(numPairs) }
                    var i = offset
                    for (n in 0 until numPairs) {
                        // unsigned 8-bit PCM, 128 == silence
                        scratchL[n] = ((samples[i].toInt() and 0xFF) - 128) / 128f
                        scratchR[n] = ((samples[i + 1].toInt() and 0xFF) - 128) / 128f
                        i += 2
                    }
                    leftRings[ch].writeBlocking(scratchL, numPairs)
                    rightRings[ch].writeBlocking(scratchR, numPairs)
                }

                override fun setVolume(volume: Float) {
                    // AudioAdapter sends the VM's master volume here; apply to both tracks.
                    leftTrack?.volume = volume.toDouble()
                    rightTrack?.volume = volume.toDouble()
                }

                override fun dispose() {}
            }
        }

    fun start() {
        if (running) return
        running = true

        leftRings.forEach { it.reopen() }
        rightRings.forEach { it.reopen() }

        leftTrack = checkoutTrack(leftBank)
        rightTrack = checkoutTrack(rightBank)
    }

    private fun checkoutTrack(bank: AudioBank): TerrarumAudioMixerTrack {
        val track = App.audioMixer.getFreeTrack() ?: App.audioMixer.getFreeTrackNoMatterWhat()
        track.trackingTarget = trackingTarget
        track.currentTrack = bank
        track.volume = 1.0
        track.play()
        return track
    }

    fun stop() {
        if (!running) return
        running = false

        // unblock any AudioAdapter render thread parked in writeBlocking
        leftRings.forEach { it.close() }
        rightRings.forEach { it.close() }

        leftTrack?.let { it.currentTrack = null; it.trackingTarget = null; it.stop() }
        rightTrack?.let { it.currentTrack = null; it.trackingTarget = null; it.stop() }
        leftTrack = null
        rightTrack = null
    }

    companion object {
        private const val CHANNELS = 4           // AudioAdapter playhead count
        private const val RING_CAPACITY = 8192   // ~256 ms at 32 kHz; absorbs scheduling jitter
    }

    /**
     * AudioBank that, on every read, sums a set of single-producer ring buffers (one VM playhead each)
     * into a mono signal duplicated to both output channels. Always returns a full buffer so the mixer
     * treats it as an endless live stream (silence is emitted when no playhead is producing).
     */
    private inner class StreamingMixBank(
        override val name: String,
        private val rings: Array<FloatRing>
    ) : AudioBank() {
        override val notCopyable = true // keep our live instance; do NOT let the mixer clone it
        override fun makeCopy() = this

        override var samplingRate = 32000f // VM AudioAdapter.SAMPLING_RATE
        override var channels = 2
        override var totalSizeInSamples = Long.MAX_VALUE
        private var pos = 0L
        override fun currentPositionInSamples() = pos

        override var songFinishedHook: (AudioBank) -> Unit = {}

        override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int {
            val n = bufferL.size
            java.util.Arrays.fill(bufferL, 0, n, 0f)
            for (ring in rings) ring.readSummedInto(bufferL, n)
            System.arraycopy(bufferL, 0, bufferR, 0, n)
            pos += n
            return n
        }

        override fun reset() { /* live stream: nothing to rewind */ }
        override fun dispose() {}
    }

    /**
     * Bounded single-producer / single-consumer float ring buffer.
     *
     * The producer is one AudioAdapter render thread; the consumer is the Terrarum mixer thread. The
     * producer blocks when full (back-pressure → paces the VM to its true sample rate); the consumer
     * never blocks (it reads what is available and the bank pads the rest with silence). [close]
     * permanently wakes a blocked producer so the VM can be torn down without hanging.
     */
    private class FloatRing(private val capacity: Int) {
        private val buf = FloatArray(capacity)
        private var head = 0
        private var tail = 0
        private var size = 0
        private var closed = false
        private val lock = Any()
        // Kotlin hides Object.wait/notify behind Any, so reach them through a java.lang.Object view.
        private val monitor get() = lock as java.lang.Object

        companion object {
            private const val BACKPRESSURE_TIMEOUT_MS = 250L
        }

        fun close() = synchronized(lock) { closed = true; monitor.notifyAll() }
        fun reopen() = synchronized(lock) { closed = false; head = 0; tail = 0; size = 0 }

        fun writeBlocking(src: FloatArray, count: Int) {
            synchronized(lock) {
                var written = 0
                while (written < count) {
                    if (closed) return
                    if (size >= capacity) {
                        try { monitor.wait(BACKPRESSURE_TIMEOUT_MS) }
                        catch (e: InterruptedException) { Thread.currentThread().interrupt(); return }
                        if (size >= capacity) return // timed out (consumer stalled) → drop remainder
                        continue
                    }
                    val n = minOf(count - written, capacity - size)
                    var i = 0
                    while (i < n) {
                        val chunk = minOf(n - i, capacity - tail)
                        System.arraycopy(src, written + i, buf, tail, chunk)
                        tail = (tail + chunk) % capacity
                        i += chunk
                    }
                    size += n
                    written += n
                    monitor.notifyAll()
                }
            }
        }

        /** Adds up to [count] available samples onto dst[0..count) (does not clear dst first). */
        fun readSummedInto(dst: FloatArray, count: Int) {
            synchronized(lock) {
                val n = minOf(count, size)
                var i = 0
                while (i < n) {
                    val chunk = minOf(n - i, capacity - head)
                    for (k in 0 until chunk) dst[i + k] += buf[head + k]
                    head = (head + chunk) % capacity
                    i += chunk
                }
                size -= n
                if (n > 0) monitor.notifyAll()
            }
        }
    }
}
