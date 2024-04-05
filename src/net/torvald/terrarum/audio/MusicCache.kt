package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.tryDispose

class MusicCache(val trackName: String) : Disposable {

    private val cache = HashMap<String, AudioBank>()

    fun getOrPut(music: AudioBank?): AudioBank? {
        if (music != null)
            return cache.getOrPut(music.name) { music.makeCopy() }
        return null
    }

    override fun dispose() {
        cache.values.forEach { it.tryDispose() }
    }
}
