package net.torvald.terrarum.audio

import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10
import org.newdawn.slick.Music
import org.newdawn.slick.openal.Audio
import org.newdawn.slick.openal.AudioImpl
import org.newdawn.slick.openal.MODSound
import org.newdawn.slick.openal.StreamSound
import java.util.*

/**
 * Created by minjaesong on 16-07-08.
 */
object AudioMixer {
    const val TRACK_COUNT = 32

    const val TRACK_AMBIENT_ONE = 0 // music track one
    const val TRACK_AMBIENT_ONE_NEXT = 1 // music track two
    const val TRACK_AMBIENT_TWO = 2 // music track three
    const val TRACK_AMBIENT_TWO_NEXT = 3 // music track four

    const val TRACK_UI_ONE = 8
    const val TRACK_UI_TWO = 9

    const val TRACK_SFX_START = 16
    const val TRACK_SFX_END = 31

    val tracks = ArrayList<Int>(TRACK_COUNT) // stores index of ALSource

    init {
        tracks[TRACK_AMBIENT_ONE]
    }

    fun getAudio(track: Int) = tracks[track]

    fun play(channel: Int) {

    }

    /**
     * Queue an SFX to any empty SFX track and play it.
     */
    fun queueSfx(audio: Audio) {

    }

    fun update(delta: Int) {

    }


    class MixerTrack(val audio: Audio, var volume: Float, var pan: Float) {

    }
}
