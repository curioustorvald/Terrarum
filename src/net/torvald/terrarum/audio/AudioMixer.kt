package net.torvald.terrarum.audio

import org.lwjgl.openal.AL10
import org.newdawn.slick.openal.Audio
import org.newdawn.slick.openal.AudioImpl
import org.newdawn.slick.openal.MODSound
import java.util.*

/**
 * Created by minjaesong on 16-07-08.
 */
object AudioMixer {
    const val TRACK_COUNT = 32

    const val TRACK_AMBIENT_ONE = 0
    const val TRACK_AMBIENT_ONE_NEXT = 1
    const val TRACK_AMBIENT_TWO = 2
    const val TRACK_AMBIENT_TWO_NEXT = 3

    const val TRACK_UI_ONE = 8
    const val TRACK_UI_TWO = 9

    const val TRACK_SFX_START = 16
    const val TRACK_SFX_END = 31

    val tracks = ArrayList<MixerTrack>(TRACK_COUNT)

    init {

    }

    fun getAudio(track: Int) = tracks[track]

    /**
     * Queue an SFX to any empty SFX track and play it.
     */
    fun queueSfx(audio: Audio) {

    }

    fun update() {

    }

    class MixerTrack(val audio: Audio, var volume: Float, var pan: Float) {

    }
}