package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.tryDispose

typealias MaterialID = String

/**
 * Created by minjaesong on 2023-12-02.
 */
class AudioCodex: Disposable {

    @Transient val footsteps = HashMap<MaterialID, HashSet<MusicContainer>>()

    internal constructor()

    fun addToFootstepPool(materialID: MaterialID, music: FileHandle) {
        if (footsteps[materialID] == null) footsteps[materialID] = HashSet()
        footsteps[materialID]!!.add(
            MusicContainer(music.nameWithoutExtension(), music.file(), Gdx.audio.newMusic(music)) {}
        )
    }

    fun getRandomFootstep(materialID: MaterialID) = footsteps[materialID]?.random()

    override fun dispose() {
        footsteps.values.forEach { it.forEach { it.gdxMusic.tryDispose() } }
    }
}