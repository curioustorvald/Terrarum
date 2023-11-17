package net.torvald.terrarum.audio

import net.torvald.terrarum.getHashStr
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.utils.PasswordBase32
import kotlin.math.log10
import kotlin.math.pow

typealias TrackVolume = Double

class TerrarumAudioMixerTracks {

    val hash = getHashStr()

    var currentTrack: MusicContainer? = null
    var nextTrack: MusicContainer? = null

    var volume: TrackVolume = 1.0
        get() = field
        set(value) {
            field = value
            currentTrack?.gdxMusic?.volume = volume.toFloat()
        }

    var pan = 0.0

    var dBfs: Double
        get() = fullscaleToDecibels(volume)
        set(value) { volume = decibelsToFullscale(value) }

    val filters = arrayListOf<TerrarumAudioFilters>()
    val sidechainInputs = arrayListOf<Pair<TerrarumAudioMixerTracks, TrackVolume>>()

    /**
     * assign nextTrack to currentTrack, then assign nextNext to nextTrack.
     * Whatever is on the currentTrack will be lost.
     */
    fun queueNext(nextNext: MusicContainer? = null) {
        currentTrack = nextTrack
        nextTrack = nextNext
    }

    fun play() {
        currentTrack?.gdxMusic?.play()
    }

    val isPlaying: Boolean?
        get() = currentTrack?.gdxMusic?.isPlaying


    override fun equals(other: Any?) = this.hash == (other as TerrarumAudioMixerTracks).hash
}

fun fullscaleToDecibels(fs: Double) = 10.0 * log10(fs)
fun decibelsToFullscale(db: Double) = 10.0.pow(db / 10.0)