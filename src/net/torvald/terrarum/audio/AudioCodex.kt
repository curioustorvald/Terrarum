package net.torvald.terrarum.audio

import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.tryDispose

/**
 * Created by minjaesong on 2023-12-02.
 */
class AudioCodex {

    @Transient val audio = HashMap<String, HashSet<FileHandle>>()

    internal constructor()

    /**
     * The audio will be collected as one of many elements in the collection named `identifier`
     *
     * @param identifier identifier of the music, WITHOUT a serial number
     */
    fun addToAudioPool(identifier: String, music: FileHandle) {
        if (audio[identifier] == null) audio[identifier] = HashSet()
        audio[identifier]!!.add(music)
    }

    fun getRandomFootstep(materialID: String) = getRandomAudio("effects.steps.$materialID")
    fun getRandomMining(materialID: String) = getRandomAudio("effects.mining.$materialID")

    /**
     * @param identifier
     */
    fun getRandomAudio(identifier: String): MusicContainer? {
        val file = audio[identifier]?.random()
        return if (file != null) {
            MusicContainer(identifier.substringBeforeLast('.') + "." + file.nameWithoutExtension(), file.file()) {
                it.tryDispose()
            }
        }
        else null
    }

}