package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.tryDispose

typealias MaterialID = String

/**
 * Created by minjaesong on 2023-12-02.
 */
class AudioCodex {

    @Transient val footsteps = HashMap<MaterialID, HashSet<FileHandle>>()

    internal constructor()

    fun addToFootstepPool(materialID: MaterialID, music: FileHandle) {
        if (footsteps[materialID] == null) footsteps[materialID] = HashSet()
        footsteps[materialID]!!.add(music)
    }

    fun getRandomFootstep(materialID: MaterialID): MusicContainer? {
        val file = footsteps[materialID]?.random()
        return if (file != null) {
            MusicContainer(file.nameWithoutExtension(), file.file(), Gdx.audio.newMusic(file)) {
                it.tryDispose()
            }
        }
        else null
    }

}