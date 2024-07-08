package net.torvald.terrarum

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.AudioBank
import net.torvald.terrarum.audio.AudioMixer.Companion.DEFAULT_FADEOUT_LEN
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.transaction.Transaction
import net.torvald.terrarum.transaction.TransactionListener
import net.torvald.terrarum.transaction.TransactionState
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * To play the music, create a transaction then pass it to the `runTransaction(Transaction)`
 *
 * Created by minjaesong on 2024-06-28.
 */
object MusicService : TransactionListener() {

    private val currentPlaylistReference = AtomicReference<TerrarumMusicPlaylist?>(null)
    val currentPlaylist: TerrarumMusicPlaylist?; get() = currentPlaylistReference.get()

    override fun getCurrentStatusForTransaction(): TransactionState {
        return TransactionState(
            hashMapOf(
                "currentPlaylist" to currentPlaylistReference.get()
            )
        )
    }

    override fun commitTransaction(state: TransactionState) {
        (state["currentPlaylist"] as TerrarumMusicPlaylist?).let {
            this.currentPlaylistReference.set(it)
        }
    }



    private const val STATE_INTERMISSION = 0
    private const val STATE_FIREPLAY = 1
    private const val STATE_PLAYING = 2

    val currentPlaybackState = AtomicInteger(STATE_INTERMISSION)
    var waitAkku = 0f; private set
    var waitTime = 10f; private set

    private fun enterSTATE_INTERMISSION(waitFor: Float) {
        currentPlaybackState.set(STATE_INTERMISSION)
        waitTime = waitFor
        waitAkku = 0f
        playTransactionOngoing = false
    }

    private fun enterSTATE_FIREPLAY() {
        val state = currentPlaybackState.get()
        if (state == STATE_PLAYING) throw IllegalStateException("Cannot change state PLAYING -> FIREPLAY")

        waitAkku = 0f
        currentPlaybackState.set(STATE_FIREPLAY)
    }

    private fun enterSTATE_PLAYING() {
        val state = currentPlaybackState.get()
        if (state == STATE_INTERMISSION) throw IllegalStateException("Cannot change state INTERMISSION -> PLAYING")
        if (state == STATE_PLAYING) throw IllegalStateException("Cannot change state PLAYING -> PLAYING")

        currentPlaybackState.set(STATE_PLAYING)
    }

    fun getRandomMusicInterval() = 16f + Math.random().toFloat() * 4f // longer gap (16s to 20s)

    private fun enterIntermissionAndWaitForPlaylist() {
        val djmode = currentPlaylist?.diskJockeyingMode ?: "intermittent"
        val time = when (djmode) {
            "intermittent" -> getRandomMusicInterval()
            "continuous" -> 0f
            else -> getRandomMusicInterval()
        }
        enterSTATE_INTERMISSION(time)
        if (djmode == "continuous") enterSTATE_FIREPLAY()
    }

    fun enterIntermission() {
        enterSTATE_INTERMISSION(getRandomMusicInterval())
    }

    fun onMusicFinishing(audio: AudioBank) {
//        printdbg(this, "onMusicFinishing ${audio.name}")
        enterIntermissionAndWaitForPlaylist()
    }

    private var playTransactionOngoing = false

    fun update(delta: Float) {
        when (currentPlaybackState.get()) {
            STATE_FIREPLAY -> {
                if (!playTransactionOngoing) {
                    playTransactionOngoing = true
                    MusicService.resumePlaylistPlayback(
                        /* onSuccess: () -> Unit */
                        {
                            runTransaction(object : Transaction {
                                private lateinit var nextMusic: MusicContainer

                                override fun start(state: TransactionState) {
                                    nextMusic = (state["currentPlaylist"] as TerrarumMusicPlaylist).queueNext()
                                    App.audioMixer.startMusic(nextMusic)
                                }

                                override fun onSuccess(state: TransactionState) {
//                                    printdbg(this, "FIREPLAY started music (${nextMusic.name})")
                                    currentPlaybackState.set(STATE_PLAYING) // force PLAYING
                                }

                                override fun onFailure(e: Throwable, state: TransactionState) {
//                                    printdbg(this, "FIREPLAY resume OK but startMusic failed, entering intermission")
                                    enterIntermissionAndWaitForPlaylist() // will try again
                                }
                            })
                        },
                        /* onFailure: (Throwable) -> Unit */
                        {
//                            printdbg(this, "FIREPLAY resume failed, entering intermission")
                            enterIntermissionAndWaitForPlaylist() // will try again
                        },
                        // onFinally: () -> Unit
                        {
                            playTransactionOngoing = false
                        }
                    )
                }
                else {
//                    printdbg(this, "FIREPLAY no-op: playTransaction is ongoing")
                }
            }
            STATE_PLAYING -> {
                // onMusicFinishing() will be called when the music finishes; it's on the setOnCompletionListener
            }
            STATE_INTERMISSION -> {
                waitAkku += delta

                if (waitAkku >= waitTime && currentPlaylist != null) {
                    // force FIREPLAY
                    waitAkku = 0f
                    currentPlaybackState.set(STATE_FIREPLAY)
                }
            }
        }
    }


    fun enterScene(id: String) {
        /*val playlist = when (id) {
            "title" -> getTitlePlaylist()
            "ingame" -> getIngameDefaultPlaylist()
            else -> getIngameDefaultPlaylist()
        }

        putNewPlaylist(playlist) {
            // after the fadeout, we'll...
            enterSTATE_FIREPLAY()
        }*/

        stopPlaylistPlayback { }
    }

    fun leaveScene() {
        stopPlaylistPlayback {}
    }


    /**
     * Puts the given playlist to this object if the transaction successes. If the given playlist is same as the
     * current playlist, the transaction will successfully finish immediately; otherwise the given playlist will
     * be reset as soon as the transaction starts. Note that the resetting behaviour is NOT atomic. (the given
     * playlist will stay in reset state even if the transaction fails)
     *
     * When the transaction was successful, the old playlist gets disposed of, then the songFinishedHook of
     * the songs in the new playlist will be overwritten, before `onSuccess` is called.
     *
     * The old playlist will be disposed of if and only if the transaction was successful.
     *
     * @param playlist An instance of a [TerrarumMusicPlaylist] to be changed into
     * @param onSuccess What to do after the transaction
     */
    private fun createTransactionPlaylistChange(playlist: TerrarumMusicPlaylist, onSuccess: () -> Unit): Transaction {
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
                    // put new playlist
                    state["currentPlaylist"] = playlist
                }
            }

            override fun onSuccess(state: TransactionState) {
                oldPlaylist?.dispose()

                (state["currentPlaylist"] as TerrarumMusicPlaylist?)?.let {
                    // set songFinishedHook for every song
                    it.musicList.forEach {
                        it.songFinishedHook = {
                            onMusicFinishing(it)
                        }
                    }

                    // set gaplessness of the Music track
                    App.audioMixer.musicTrack.let { track ->
                        track.doGaplessPlayback = (it.diskJockeyingMode == "continuous")
                        if (track.doGaplessPlayback) {
                            track.pullNextTrack = {
                                track.currentTrack = MusicService.currentPlaylist!!.queueNext()
                            }
                        }
                    }
                }

                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Puts the given playlist to this object if the transaction successes. If the given playlist is same as the
     * current playlist, the transaction will successfully finish immediately; otherwise the given playlist will
     * be reset as soon as the transaction starts. Note that the resetting behaviour is NOT atomic. (the given
     * playlist will stay in reset state even if the transaction fails)
     *
     * When the transaction was successful, the old playlist gets disposed of, then the songFinishedHook of
     * the songs in the new playlist will be overwritten, before `onSuccess` is called.
     *
     * The old playlist will be disposed of if and only if the transaction was successful.
     *
     * @param playlist An instance of a [TerrarumMusicPlaylist] to be changed into
     * @param onSuccess What to do after the transaction
     */
    private fun createTransactionPlaylistChangeAndPlayImmediately(playlist: TerrarumMusicPlaylist, onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            var oldPlaylist: TerrarumMusicPlaylist? = null
            var oldState = currentPlaybackState.get()
            var oldAkku = waitAkku
            var oldTime = waitTime

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
                    // put new playlist
                    state["currentPlaylist"] = playlist
                }

                oldState = currentPlaybackState.get()
                oldAkku = waitAkku
                oldTime = waitTime

                enterSTATE_INTERMISSION(0f)
                enterSTATE_FIREPLAY()
            }

            override fun onSuccess(state: TransactionState) {
                oldPlaylist?.dispose()

                (state["currentPlaylist"] as TerrarumMusicPlaylist?)?.let {
                    // set songFinishedHook for every song
                    it.musicList.forEach {
                        it.songFinishedHook = {
                            onMusicFinishing(it)
                        }
                    }

                    // set gaplessness of the Music track
                    App.audioMixer.musicTrack.let { track ->
                        track.doGaplessPlayback = (it.diskJockeyingMode == "continuous")
                        if (track.doGaplessPlayback) {
                            track.pullNextTrack = {
                                track.currentTrack = MusicService.currentPlaylist!!.queueNext()
                            }
                        }
                    }
                }

                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {
                e.printStackTrace()

                currentPlaybackState.set(oldState)
                waitAkku = oldAkku
                waitTime = oldTime
            }
        }
    }

    private fun createTransactionForNextMusicInPlaylist(onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            var oldState = currentPlaybackState.get()
            var oldAkku = waitAkku
            var oldTime = waitTime

            override fun start(state: TransactionState) {
                var fadedOut = false
                var err: Throwable? = null
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack) {
                    try {
                        // do nothing, really

                        fadedOut = true
                    }
                    catch (e: Throwable) {
                        err = e
                    }
                }

                waitUntil { fadedOut || err != null }
                if (err != null) throw err!!

                oldState = currentPlaybackState.get()
                oldAkku = waitAkku
                oldTime = waitTime

                enterSTATE_INTERMISSION(0f)
                enterSTATE_FIREPLAY()
            }

            override fun onSuccess(state: TransactionState) {
                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {
                e.printStackTrace()

                currentPlaybackState.set(oldState)
                waitAkku = oldAkku
                waitTime = oldTime
            }
        }
    }

    private fun createTransactionForPrevMusicInPlaylist(onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            var oldState = currentPlaybackState.get()
            var oldAkku = waitAkku
            var oldTime = waitTime

            override fun start(state: TransactionState) {
                var fadedOut = false
                var err: Throwable? = null
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack) {
                    try {
                        // unshift the playlist
                        // FIREPLAY always pulls next track, that's why we need two prev()
                        (state["currentPlaylist"] as TerrarumMusicPlaylist).queuePrev()
                        (state["currentPlaylist"] as TerrarumMusicPlaylist).queuePrev()

                        fadedOut = true
                    }
                    catch (e: Throwable) {
                        err = e
                    }
                }

                waitUntil { fadedOut || err != null }
                if (err != null) throw err!!

                oldState = currentPlaybackState.get()
                oldAkku = waitAkku
                oldTime = waitTime

                enterSTATE_INTERMISSION(0f)
                enterSTATE_FIREPLAY()
            }

            override fun onSuccess(state: TransactionState) {
                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {
                // reshift the playlist
                (state["currentPlaylist"] as TerrarumMusicPlaylist).queueNext()

                e.printStackTrace()

                currentPlaybackState.set(oldState)
                waitAkku = oldAkku
                waitTime = oldTime
            }
        }
    }

    private fun createTransactionForNthMusicInPlaylist(index: Int, onSuccess: () -> Unit): Transaction {
        return object : Transaction {
            var oldState = currentPlaybackState.get()
            var oldAkku = waitAkku
            var oldTime = waitTime

            override fun start(state: TransactionState) {
                var fadedOut = false
                var err: Throwable? = null
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack) {
                    try {
                        // callback: play prev song in the playlist
                        // queue the nth song on the playlist, the actual playback will be done by the state machine update
                        (state["currentPlaylist"] as TerrarumMusicPlaylist).queueNthSong(index)

                        fadedOut = true
                    }
                    catch (e: Throwable) {
                        err = e
                    }
                }

                waitUntil { fadedOut || err != null }
                if (err != null) throw err!!

                oldState = currentPlaybackState.get()
                oldAkku = waitAkku
                oldTime = waitTime

                enterSTATE_INTERMISSION(0f)
                enterSTATE_FIREPLAY()
            }

            override fun onSuccess(state: TransactionState) {
                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {
                e.printStackTrace()

                currentPlaybackState.set(oldState)
                waitAkku = oldAkku
                waitTime = oldTime
            }
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

                enterSTATE_INTERMISSION(Float.POSITIVE_INFINITY)
            }

            override fun onSuccess(state: TransactionState) {
                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {
                e.printStackTrace()
            }
        }
    }

    private fun createTransactionForPlaylistResume(onSuccess: () -> Unit, onFailure: (Throwable) -> Unit): Transaction {
        return object : Transaction {
            var oldState = currentPlaybackState.get()
            var oldAkku = waitAkku
            var oldTime = waitTime

            override fun start(state: TransactionState) {
                enterSTATE_FIREPLAY()
            }

            override fun onSuccess(state: TransactionState) {
                onSuccess()
            }
            override fun onFailure(e: Throwable, state: TransactionState) {
                e.printStackTrace()

                currentPlaybackState.set(oldState)
                waitAkku = oldAkku
                waitTime = oldTime

                onFailure(e)
            }
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
                var err: Throwable? = null
                // request fadeout
                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack, DEFAULT_FADEOUT_LEN / 2.0) {
                    try {
                        // callback: let the caller actually take care of playing the audio
                        action()

                        fadedOut = true
                    }
                    catch (e: Throwable) {
                        err = e
                        e.printStackTrace()
                    }
                }

                waitUntil { fadedOut || err != null }
                if (err != null) throw err!!

                // enter intermission state
                enterSTATE_INTERMISSION(Float.POSITIVE_INFINITY)

                // wait until the interjected music finishes
                waitUntil { musicFinished() }
            }

            override fun onSuccess(state: TransactionState) {
                onSuccess()
                enterSTATE_INTERMISSION(getRandomMusicInterval())
            }
            override fun onFailure(e: Throwable, state: TransactionState) {
                e.printStackTrace()
                onFailure(e)
                enterSTATE_INTERMISSION(getRandomMusicInterval())
            }
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
            runTransaction(createTransactionPlaylistChange(playlist, {}))
    }
    fun putNewPlaylistAndResumePlayback(playlist: TerrarumMusicPlaylist, onSuccess: (() -> Unit)? = null) {
        if (onSuccess != null)
            runTransaction(createTransactionPlaylistChangeAndPlayImmediately(playlist, onSuccess))
        else
            runTransaction(createTransactionPlaylistChangeAndPlayImmediately(playlist, {}))
    }
    fun putNewPlaylist(playlist: TerrarumMusicPlaylist, onSuccess: () -> Unit, onFinally: () -> Unit) {
        runTransaction(createTransactionPlaylistChange(playlist, onSuccess), onFinally)
    }

    /** Normal playlist playback will resume after the transaction, after the onSuccess/onFailure */
    fun playMusicalFixture(action: () -> Unit, musicFinished: () -> Boolean, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        runTransaction(createTransactionPausePlaylistForMusicalFixture(action, musicFinished, onSuccess, onFailure))
    }
    /** Normal playlist playback will resume after the transaction, after the onSuccess/onFailure but before the onFinally */
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

    fun resumePlaylistPlayback(onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) {
        runTransaction(createTransactionForPlaylistResume(onSuccess, onFailure))
    }
    fun resumePlaylistPlayback(onSuccess: () -> Unit, onFailure: (Throwable) -> Unit, onFinally: () -> Unit) {
        runTransaction(createTransactionForPlaylistResume(onSuccess, onFailure), onFinally)
    }
}