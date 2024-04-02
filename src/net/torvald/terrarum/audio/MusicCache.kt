package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.tryDispose

class MusicCache(val trackName: String) : Disposable {

    private val cache = HashMap<String, MusicContainer>()

    fun getOrPut(music: MusicContainer?): MusicContainer? {
        if (music != null && !music.toRAM)
            return music
        if (music != null && music.toRAM) // for now only the on-the-RAM tracks are getting cached
            return cache.getOrPut(music.name) { music.makeCopy() }
        return null
    }

    override fun dispose() {
        cache.values.forEach { it.tryDispose() }
    }
}
