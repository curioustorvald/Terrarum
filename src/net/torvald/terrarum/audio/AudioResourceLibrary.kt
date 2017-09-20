package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import net.torvald.terrarum.ModMgr
import java.util.*

/**
 * Created by minjaesong on 2016-07-08.
 */
object AudioResourceLibrary {

    // will play as music
    val ambientsForest = ArrayList<Music>()
    val ambientsMeadow = ArrayList<Music>()
    val ambientsWindy = ArrayList<Music>()
    val ambientsWoods = ArrayList<Music>()

    // will play as sound effect
    val crickets = ArrayList<Sound>()

    init {
        ambientsForest.add(Gdx.audio.newMusic(ModMgr.getGdxFile("basegame", "sounds/ambient/ambient_forest_01.ogg")))
        ambientsMeadow.add(Gdx.audio.newMusic(ModMgr.getGdxFile("basegame", "sounds/ambient/ambient_meadow_01.ogg")))
        ambientsWindy.add(Gdx.audio.newMusic(ModMgr.getGdxFile("basegame", "sounds/ambient/ambient_windy_01.ogg")))
        ambientsWoods.add(Gdx.audio.newMusic(ModMgr.getGdxFile("basegame", "sounds/ambient/ambient_woods_01.ogg")))

        crickets.add(Gdx.audio.newSound(ModMgr.getGdxFile("basegame", "sounds/ambient/crickets_01.ogg")))
        crickets.add(Gdx.audio.newSound(ModMgr.getGdxFile("basegame", "sounds/ambient/crickets_02.ogg")))
    }
}