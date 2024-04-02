package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.tryDispose

class MusicCache(val trackName: String) : Disposable {

    private val cache = HashMap<String, MusicContainer>()

    fun getOrPut(music: MusicContainer?): MusicContainer? {
        if (music != null && music.toRAM) { // for now only the on-the-RAM tracks are getting cached
            println("Cacheing music ${music.name} for track $trackName")
            return cache.getOrPut(music.name) { music.makeCopy() }
        }
        else return null
    }

    override fun dispose() {
        cache.values.forEach { it.tryDispose() }
    }
}
