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
    private var currentIndexCursor = musicList.size

    init {
        reset()
    }

    fun reset() {
        internalIndices.clear()
        refillInternalIndices()
        refillInternalIndices()
        currentIndexCursor = musicList.size
    }

    private fun checkRefill() {
        if (internalIndices.size < currentIndexCursor + 1)
            refillInternalIndices()
    }

    fun getCurrent(): MusicContainer {
        checkRefill()

        return musicList[currentIndexCursor]
    }

    fun getNext(): MusicContainer {
        checkRefill()
        currentIndexCursor += 1

        return musicList[currentIndexCursor]
    }

    fun getPrev(): MusicContainer {
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

        return musicList[currentIndexCursor]
    }


    private fun refillInternalIndices() {
        internalIndices.addAll(musicList.indices.toMutableList().also { if (shuffled) it.shuffle() })
    }

    inline fun getNthSong(n: Int) = musicList[n]

    override fun dispose() {
        musicList.forEach {
            it.tryDispose()
        }
    }

    companion object {
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

            val playlist = File(musicDir).listFiles()?.sortedBy { it.name }?.mapNotNull {
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