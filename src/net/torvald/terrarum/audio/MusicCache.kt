package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.tryDispose

class MusicCache(val trackName: String) : Disposable {

    private val cache = HashMap<String, AudioBank>()

    fun getOrPut(music: AudioBank?): AudioBank? {
        if (music != null && !music.notCopyable)
            return cache.getOrPut(music.name) { music.makeCopy() }
        else if (music != null)
            return music
        return null
    }

    override fun dispose() {
        cache.values.forEach { it.tryDispose() }
    }
}
