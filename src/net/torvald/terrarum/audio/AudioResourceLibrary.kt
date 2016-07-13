package net.torvald.terrarum.audio

import org.newdawn.slick.openal.Audio
import org.newdawn.slick.openal.AudioLoader
import java.io.FileInputStream
import java.util.*

/**
 * Created by minjaesong on 16-07-08.
 */
object AudioResourceLibrary {

    val ambientsForest = ArrayList<Audio>()
    val ambientsMeadow = ArrayList<Audio>()
    val ambientsWindy = ArrayList<Audio>()
    val ambientsWoods = ArrayList<Audio>()
    val crickets = ArrayList<Audio>()

    init {
        ambientsForest.add(AudioLoader.getAudio("ogg", FileInputStream("./res/sounds/ambient/ambient_forest_01.ogg")))

        ambientsMeadow.add(AudioLoader.getAudio("ogg", FileInputStream("./res/sounds/ambient/ambient_meadow_01.ogg")))

        ambientsWindy.add(AudioLoader.getAudio("ogg", FileInputStream("./res/sounds/ambient/ambient_windy_01.ogg")))

        ambientsWoods.add(AudioLoader.getAudio("ogg", FileInputStream("./res/sounds/ambient/ambient_woods_01.ogg")))

        crickets.add(AudioLoader.getAudio("ogg", FileInputStream("./res/sounds/ambient/crickets_01.ogg")))
        crickets.add(AudioLoader.getAudio("ogg", FileInputStream("./res/sounds/ambient/crickets_02.ogg")))
    }
}