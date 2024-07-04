package net.torvald.terrarum

import net.torvald.terrarum.audio.AudioMixer.Companion.DEFAULT_FADEOUT_LEN
import net.torvald.terrarum.transaction.Transaction
import net.torvald.terrarum.transaction.TransactionListener
import net.torvald.terrarum.transaction.TransactionState

/**
 * To play the music, create a transaction then pass it to the `runTransaction(Transaction)`
 *
 * Created by minjaesong on 2024-06-28.
 */
object MusicService : TransactionListener() {

    var currentPlaylist: TerrarumMusicPlaylist? = null; private set

    override fun getCurrentStatusForTransaction(): TransactionState {
        return TransactionState(
            mutableMapOf(
                "currentPlaylist" to currentPlaylist
            )
        )
    }

    override fun commitTransaction(state: TransactionState) {
        this.currentPlaylist = state["currentPlaylist"] as TerrarumMusicPlaylist?
    }

    /**
     * Puts the given playlist to this object if the transaction successes. If the given playlist is same as the
     * current playlist, the transaction will successfully finish immediately; otherwise the given playlist will
     * be reset as soon as the transaction starts. Note that the resetting behaviour is NOT atomic. (the given
     * playlist will stay in reset state even if the transaction fails)
     *
     * The old playlist will be disposed of if and only if the transaction was successful.
     *
     * @param playlist An instance of a [TerrarumMusicPlaylist] to be changed into
     * @param onSuccess What to do after the transaction. Default behaviour is: `App.audioMixer.startMusic(playlist.getCurrent())`
     */
    private fun createTransactionPlaylistChange(playlist: TerrarumMusicPlaylist, onSuccess: () -> Unit = {
        App.audioMixer.startMusic(playlist.getCurrent())
    }): Transaction {
        return object : Transaction {
            var oldPlaylist: TerrarumMusicPlaylist? = null

            override fun start(state: TransactionState) {
                oldPlaylist = state["currentPlaylist"] as TerrarumMusicPlaylist?

                if (oldPlaylist == playlist) return

                playlist.reset()

                // request fadeout
                if (App.audioMixer.musicTrack.isPlaying) {
                    var fadedOut = false

                    App.audioMixer.requestFadeOut(App.audioMixer.musicTrack) {
                        // put new playlist
                        state["currentPlaylist"] = playlist

                        fadedOut = true
                    }

                    waitUntil { fadedOut }
                }
                else {
                    /* do nothing */
                }
            }

            override fun onSuccess(state: TransactionState) {
                oldPlaylist?.dispose()
                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {}
        }
    }

    private fun createTransactionForNextMusicInPlaylist(onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            override fun start(state: TransactionState) {
                var fadedOut = false
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack) {
                    // callback: play next song in the playlist
                    App.audioMixer.startMusic((state["currentPlaylist"] as TerrarumMusicPlaylist).getNext())
                    fadedOut = true
                }

                waitUntil { fadedOut }
            }

            override fun onSuccess(state: TransactionState) {
                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {}
        }
    }

    private fun createTransactionForPrevMusicInPlaylist(onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            override fun start(state: TransactionState) {
                var fadedOut = false
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack) {
                    // callback: play prev song in the playlist
                    App.audioMixer.startMusic((state["currentPlaylist"] as TerrarumMusicPlaylist).getPrev())
                    fadedOut = true
                }

                waitUntil { fadedOut }
            }

            override fun onSuccess(state: TransactionState) {
                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {}
        }
    }

    private fun createTransactionForNthMusicInPlaylist(index: Int, onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            override fun start(state: TransactionState) {
                var fadedOut = false
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack) {
                    // callback: play prev song in the playlist
                    App.audioMixer.startMusic((state["currentPlaylist"] as TerrarumMusicPlaylist).getNthSong(index))
                    fadedOut = true
                }

                waitUntil { fadedOut }
            }

            override fun onSuccess(state: TransactionState) { onSuccess() }
            override fun onFailure(e: Throwable, state: TransactionState) {}
        }
    }

    private fun createTransactionForPlaylistStop(onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            override fun start(state: TransactionState) {
                var fadedOut = false
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack) {
                    fadedOut = true
                }

                waitUntil { fadedOut }
            }

            override fun onSuccess(state: TransactionState) { onSuccess() }
            override fun onFailure(e: Throwable, state: TransactionState) {}
        }
    }

    private fun createTransactionForPlaylistResume(onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            override fun start(state: TransactionState) {
                App.audioMixer.startMusic((state["currentPlaylist"] as TerrarumMusicPlaylist).getCurrent())
            }

            override fun onSuccess(state: TransactionState) { onSuccess() }
            override fun onFailure(e: Throwable, state: TransactionState) {}
        }
    }

    private fun createTransactionPausePlaylistForMusicalFixture(
        action: () -> Unit,
        musicFinished: () -> Boolean,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ): Transaction {
        return object : Transaction {
            override fun start(state: TransactionState) {
                var fadedOut = false
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack, DEFAULT_FADEOUT_LEN / 2.0) {
                    // callback: let the caller actually take care of playing the audio
                    action()

                    fadedOut = true
                }

                waitUntil { fadedOut }

                // wait until the interjected music finishes
                waitUntil { musicFinished() }
            }

            override fun onSuccess(state: TransactionState) { onSuccess() }
            override fun onFailure(e: Throwable, state: TransactionState) { onFailure(e) }
        }

        // note to self: wait() and notify() using a lock object is impractical as the Java thread can wake up
        // randomly regardless of the notify(), which results in the common pattern of
        //     while (!condition) { lock.wait() }
        // and if we need extra condition (i.e. musicFinished()), it's just a needlessly elaborate way of spinning,
        // UNLESS THE THING MUST BE SYNCHRONISED WITH SOMETHING
    }

    private fun waitUntil(escapeCondition: () -> Boolean) {
        while (!escapeCondition()) {
            Thread.sleep(4L)
        }
    }

    fun putNewPlaylist(playlist: TerrarumMusicPlaylist, onSuccess: (() -> Unit)? = null) {
        if (onSuccess != null)
            runTransaction(createTransactionPlaylistChange(playlist, onSuccess))
        else
            runTransaction(createTransactionPlaylistChange(playlist))
    }
    fun putNewPlaylist(playlist: TerrarumMusicPlaylist, onSuccess: () -> Unit, onFinally: () -> Unit) {
        runTransaction(createTransactionPlaylistChange(playlist, onSuccess), onFinally)
    }

    fun playMusicalFixture(action: () -> Unit, musicFinished: () -> Boolean, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        runTransaction(createTransactionPausePlaylistForMusicalFixture(action, musicFinished, onSuccess, onFailure))
    }
    fun playMusicalFixture(action: () -> Unit, musicFinished: () -> Boolean, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit, onFinally: () -> Unit = {}) {
        runTransaction(createTransactionPausePlaylistForMusicalFixture(action, musicFinished, onSuccess, onFailure), onFinally)
    }

    fun playNextSongInPlaylist(onSuccess: () -> Unit) {
        runTransaction(createTransactionForNextMusicInPlaylist(onSuccess))
    }
    fun playNextSongInPlaylist(onSuccess: () -> Unit, onFinally: () -> Unit) {
        runTransaction(createTransactionForNextMusicInPlaylist(onSuccess), onFinally)
    }

    fun playPrevSongInPlaylist(onSuccess: () -> Unit) {
        runTransaction(createTransactionForPrevMusicInPlaylist(onSuccess))
    }
    fun playPrevSongInPlaylist(onSuccess: () -> Unit, onFinally: () -> Unit) {
        runTransaction(createTransactionForPrevMusicInPlaylist(onSuccess), onFinally)
    }

    fun playNthSongInPlaylist(index: Int, onSuccess: () -> Unit) {
        runTransaction(createTransactionForNthMusicInPlaylist(index, onSuccess))
    }
    fun playNthSongInPlaylist(index: Int, onSuccess: () -> Unit, onFinally: () -> Unit) {
        runTransaction(createTransactionForNthMusicInPlaylist(index, onSuccess), onFinally)
    }

    fun stopPlaylistPlayback(onSuccess: () -> Unit) {
        runTransaction(createTransactionForPlaylistStop(onSuccess))
    }
    fun stopPlaylistPlayback(onSuccess: () -> Unit, onFinally: () -> Unit) {
        runTransaction(createTransactionForPlaylistStop(onSuccess), onFinally)
    }

    fun resumePlaylistPlayback(onSuccess: () -> Unit) {
        runTransaction(createTransactionForPlaylistResume(onSuccess))
    }
    fun resumePlaylistPlayback(onSuccess: () -> Unit, onFinally: () -> Unit) {
        runTransaction(createTransactionForPlaylistResume(onSuccess), onFinally)
    }
}