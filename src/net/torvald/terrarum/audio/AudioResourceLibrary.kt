package net.torvald.terrarum.audio

import org.newdawn.slick.openal.Audio
import org.newdawn.slick.openal.AudioLoader
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Created by minjaesong on 16-07-08.
 */
object AudioResourceLibrary {

    // will play as music
    val ambientsForest = ArrayList<Audio>()
    val ambientsMeadow = ArrayList<Audio>()
    val ambientsWindy = ArrayList<Audio>()
    val ambientsWoods = ArrayList<Audio>()

    // will play as sound effect
    val crickets = ArrayList<Audio>()

    init {
        ambientsForest.add(AudioLoader.getStreamingAudio("OGG", File("./assets/sounds/ambient/ambient_forest_01.ogg").toURI().toURL()))

        ambientsMeadow.add(AudioLoader.getStreamingAudio("OGG", File("./assets/sounds/ambient/ambient_meadow_01.ogg").toURI().toURL()))

        ambientsWindy.add(AudioLoader.getStreamingAudio("OGG", File("./assets/sounds/ambient/ambient_windy_01.ogg").toURI().toURL()))

        ambientsWoods.add(AudioLoader.getStreamingAudio("OGG", File("./assets/sounds/ambient/ambient_woods_01.ogg").toURI().toURL()))

        crickets.add(AudioLoader.getAudio("OGG", FileInputStream("./assets/sounds/ambient/crickets_01.ogg")))
        crickets.add(AudioLoader.getAudio("OGG", FileInputStream("./assets/sounds/ambient/crickets_02.ogg")))
    }
}