package net.torvald.terrarum.audio.taudplay

import com.badlogic.gdx.files.FileHandle
import net.torvald.taudplay.TaudConfig
import net.torvald.taudplay.TaudPlayer
import net.torvald.taudplay.gdx.TaudStream
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.AudioBank
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import kotlin.math.abs
import kotlin.math.exp

/**
 * A Taud song (`.taud`) as an [AudioBank]: rendered by taudplay on a thread of its own ([TaudStream]) and read by
 * the mixer in whatever sizes it asks for, at the mixer's own rate (48 kHz), so nothing is resampled.
 *
 * With a lane sheet ([BgmLanes]) the song's faders follow it ([LaneDirector]): a combinatorial song draws its lanes
 * at every playback start; a dual song crossfades its arrangements, and a song with hints leans its roles, by
 * [MusicEnvironment.enclosure], smoothed over [SMOOTHING_SECONDS] and applied whenever it has moved by more than
 * [STEP] (each change fading over the sheet's `fade_ms`).
 *
 * The song is loaded only when the mixer first pulls it, on a thread of its own (a big song takes ~0.1 s and
 * ~20 MB), and dropped again by [reset] (the mixer's stop, which follows the song's end); until it is ready the
 * mixer reads silence. So a playlist of many songs holds only the one that plays.
 *
 * @param preGain extra gain on the music track while this song plays (its music pack's `global_dsp_request`)
 *
 * Created by minjaesong on 2026-11-06.
 */
class TaudAudioBank(
    override val name: String,
    val file: FileHandle,
    val lanes: BgmLanes? = null,
    override var songFinishedHook: (AudioBank) -> Unit = {},
    override val preGain: Float = 1f,
) : AudioBank() {

    override var samplingRate: Float = TerrarumAudioMixerTrack.SAMPLING_RATE.toFloat()
    override var channels: Int = 2
    /** Unknown: a Taud song is a program, and its length is known only once it has played through. */
    override var totalSizeInSamples: Long = 0L

    private val lock = Any()
    private var stream: TaudStream? = null
    private var director: LaneDirector? = null
    private var generation = 0
    private var loading = false
    private var failed = false
    private var position = 0L
    private var xSmooth = 0.0
    private var xApplied = 0.0

    /** The stream that plays now (null while the song loads or after it has ended): its probes and interrupts. */
    val currentStream: TaudStream? get() = synchronized(lock) { stream }

    /** The current playback's draw of a combinatorial song: slot id → the ids of the options drawn. */
    val currentDraw: Map<String, List<String>> get() = synchronized(lock) { director?.lastDraw ?: emptyMap() }

    override fun currentPositionInSamples() = synchronized(lock) { position }

    override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int {
        val frames = bufferL.size
        synchronized(lock) {
            val s = stream
            if (s == null) {
                if (failed) return 0
                if (!loading) load()
                bufferL.fill(0f)
                bufferR.fill(0f)
                return frames // silence while the song loads
            }
            steer(s, frames)
            val got = s.read(bufferL, bufferR, 0, frames)
            position += got
            // a read that ran dry while the song plays on is silence, not the end
            return if (got == 0 && !s.isEnded()) frames else got
        }
    }

    private fun load() {
        loading = true
        val gen = generation
        Thread({
            try {
                val player = TaudPlayer(file.readBytes(), TaudConfig().also { it.sampleRate = samplingRate.toInt() })
                val d = lanes?.let { LaneDirector(it, player.channelCount) }
                val x = MusicEnvironment.enclosure.coerceIn(0.0, 1.0)
                val s = TaudStream(player)
                // the faders are set before play(), so the song starts already mixed
                if (d != null) s.setVoiceGains(d.start(x), 0.0)
                s.play()
                synchronized(lock) {
                    if (gen != generation) {
                        s.dispose()
                    }
                    else {
                        stream = s
                        director = d
                        xSmooth = x
                        xApplied = x
                        loading = false
                    }
                }
                if (d != null && d.lastDraw.isNotEmpty()) printdbg(this, "$name: draw ${d.lastDraw}")
            }
            catch (e: Throwable) {
                e.printStackTrace()
                synchronized(lock) {
                    if (gen == generation) {
                        failed = true
                        loading = false
                    }
                }
            }
        }, "TaudAudioBank-$name").also { it.isDaemon = true }.start()
    }

    /** Follow the surroundings: called by every read, under the lock. */
    private fun steer(s: TaudStream, frames: Int) {
        val d = director ?: return
        if (!d.followsSurroundings) return
        val x = MusicEnvironment.enclosure.coerceIn(0.0, 1.0)
        xSmooth += (x - xSmooth) * (1.0 - exp(-frames / samplingRate / SMOOTHING_SECONDS))
        // settle exactly on an end once close to it, so a lane meant to be silent is
        if ((x == 0.0 || x == 1.0) && abs(xSmooth - x) < SNAP) xSmooth = x
        if (abs(xSmooth - xApplied) > STEP || xSmooth != xApplied && (xSmooth == 0.0 || xSmooth == 1.0)) {
            xApplied = xSmooth
            s.setVoiceGains(d.gainsAt(xSmooth), d.fadeSeconds)
        }
    }

    /** Stop and let the song go (the next read loads it afresh: a new draw, the faders set for where the player is). */
    override fun reset() {
        val old: TaudStream?
        synchronized(lock) {
            generation++
            old = stream
            stream = null
            director = null
            loading = false
            failed = false
            position = 0L
        }
        old?.dispose()
    }

    override fun makeCopy(): AudioBank = TaudAudioBank(name, file, lanes, songFinishedHook, preGain)

    override fun dispose() = reset()

    override fun toString() = "TaudAudioBank($name)"

    companion object {
        /** The time constant x is smoothed by, so that a tunnel mouth does not flutter. */
        const val SMOOTHING_SECONDS = 3.0
        /** How far the smoothed x moves before the faders follow. */
        const val STEP = 0.02
        private const val SNAP = 0.005
    }
}
