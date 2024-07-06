package net.torvald.terrarum

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.audiobank.MusicContainer
import java.io.File

/**
 * The `musicList` never (read: should not) gets changed, only the `internalIndices` are being changed as
 * the songs are being played.
 *
 * Created by minjaesong on 2024-06-29.
 */
class TerrarumMusicPlaylist(
    /** list of files */
    val musicList: List<MusicContainer>,
    /** name of the album/playlist shown in the [net.torvald.terrarum.musicplayer.gui.MusicPlayer] */
    val name: String,
    /** canonicalPath with path separators converted to forward slash */
    val source: String,
    /** "continuous", "intermittent"; not used by the Playlist itself but by the BackgroundMusicPlayer (aka you are the one who make it actually work) */
    val diskJockeyingMode: String,
    /** if set, the `internalIndices` will be shuffled accordingly, and this happens automatically. (aka you don't need to worry about) */
    val shuffled: Boolean
): Disposable {

    private val internalIndices = ArrayList<Int>()
    private var currentIndexCursor = musicList.size - 1

    init {
        reset()
    }

    fun reset() {
        internalIndices.clear()
        refillInternalIndices()
        refillInternalIndices()
        currentIndexCursor = musicList.size - 1
    }

    private fun checkRefill() {
        if (currentIndexCursor >= internalIndices.size - 2)
            refillInternalIndices()
    }

    fun getCurrent(): MusicContainer {
        checkRefill()

        return musicList[internalIndices[currentIndexCursor]]
    }

    /**
     * For Gapless playback, this function is called by track's pullNextTrack callback (defined in [MusicService.createTransactionPlaylistChange])
     *
     * For intermittent playback, this function is called by the transaction defined in [MusicService.update]
     */
    fun queueNext(): MusicContainer {
        checkRefill()
        currentIndexCursor += 1

        return musicList[internalIndices[currentIndexCursor]]
    }

    fun queuePrev(): MusicContainer {
        if (currentIndexCursor == 0) {
            if (shuffled) {
                musicList.indices.toMutableList().also { if (shuffled) it.shuffle() }.reversed().forEach {
                    internalIndices.add(0, it)
                }
                currentIndexCursor += musicList.size
            }
            else {
                musicList.indices.reversed().forEach {
                    internalIndices.add(0, it)
                }
                currentIndexCursor += musicList.size
            }
        }

        currentIndexCursor -= 1

        return musicList[internalIndices[currentIndexCursor]]
    }

    fun queueNthSong(n: Int): MusicContainer {
        checkRefill()
        internalIndices.add(currentIndexCursor, n)
        currentIndexCursor -= 1
        return musicList[internalIndices[currentIndexCursor]]
    }

    private fun refillInternalIndices() {
        internalIndices.addAll(musicList.indices.toMutableList().also { if (shuffled) it.shuffle() })
    }


    override fun dispose() {
        musicList.forEach {
            it.tryDispose()
        }
    }

    companion object {
        private val validMusicExtensions = hashSetOf("mp3", "wav", "ogg")

        /**
         * Adding songFinishedHook to the songs is a responsibility of the caller.
         */
        fun fromDirectory(musicDir: String, shuffled: Boolean, diskJockeyingMode: String, fileToName: ((String) -> String) = { name: String ->
            name.substringBeforeLast('.').replace('_', ' ').split(" ").map { it.capitalize() }.joinToString(" ")
        }): TerrarumMusicPlaylist {
            val musicDir = musicDir.replace('\\', '/')
            val playlistSource = musicDir
            printdbg(this, "registerSongsFromDir $musicDir")

            val playlistName = musicDir.substringAfterLast('/')

            val playlist = File(musicDir).listFiles()?.sortedBy { it.name }?.filter { Companion.validMusicExtensions.contains(it.extension.lowercase()) }?.mapNotNull {
                printdbg(this, "Music: ${it.absolutePath}")
                try {
                    MusicContainer(
                        fileToName(it.name),
                        it
                    )/*.also { muscon ->

                        printdbg(this, "MusicTitle: ${muscon.name}")

                        muscon.songFinishedHook =  {
                            if (App.audioMixer.musicTrack.currentTrack == it) {
                                stopMusic(this, true, getRandomMusicInterval())
                            }
                        }
                    }*/
                    // adding songFinishedHook must be done by the caller
                }
                catch (e: GdxRuntimeException) {
                    e.printStackTrace()
                    null
                }
            } ?: emptyList() // TODO test code

            return TerrarumMusicPlaylist(
                playlist,
                playlistName,
                musicDir,
                diskJockeyingMode,
                shuffled
            )
        }
    }


}