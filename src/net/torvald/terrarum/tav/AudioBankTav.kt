package net.torvald.terrarum.tav

import net.torvald.terrarum.audio.AudioBank

/**
 * AudioBank adapter wrapping a TavDecoder's audio ring buffer.
 * Reports 32000 Hz sampling rate; the audio pipeline resamples to 48000 Hz automatically.
 * Lifecycle is managed by VideoSpriteAnimation — dispose() is a no-op here.
 */
class AudioBankTav(
    private val decoder: TavDecoder,
    override var songFinishedHook: (AudioBank) -> Unit = {}
) : AudioBank() {

    override val notCopyable = true
    override val name = "tav-audio"

    /** TAD native sample rate; AudioProcessBuf resamples to 48000 Hz. */
    override var samplingRate = 32000f
    override var channels = 2

    override var totalSizeInSamples: Long =
        decoder.totalFrames * (32000L / decoder.fps.coerceAtLeast(1))

    override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int =
        decoder.readAudioSamples(bufferL, bufferR)

    override fun currentPositionInSamples(): Long = decoder.audioReadPos.get()

    override fun reset() { /* reset is handled at decoder level by VideoSpriteAnimation */ }

    override fun makeCopy(): AudioBank = throw UnsupportedOperationException("AudioBankTav is not copyable")

    /** Lifecycle managed by VideoSpriteAnimation; do not dispose the decoder here. */
    override fun dispose() {}
}
