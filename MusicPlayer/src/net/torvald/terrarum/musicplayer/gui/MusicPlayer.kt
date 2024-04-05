package net.torvald.terrarum.musicplayer.gui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.JsonValue
import com.jme3.math.FastMath
import net.torvald.reflection.extortField
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.*
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.PlaysMusic
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.ui.MouseLatch
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import java.io.File
import java.util.BitSet
import kotlin.math.*

/**
 * When the current music is controlled by the ingame (e.g. Jukebox) the MusicPlayer gets disabled.
 *
 * Created by minjaesong on 2023-12-23.
 */
class MusicPlayer(private val ingame: TerrarumIngame) : UICanvas() {

    private val STRIP_W = 9f
    private val METERS_WIDTH = 2 * STRIP_W
    private val maskOffWidth = 8

    override var width = (METERS_WIDTH).roundToInt()
    override var height = 28

    private var capsuleHeight = 28
    private var capsuleMosaicSize = capsuleHeight / 2 + 1

    private val BUTTON_WIDTH = 48
    private val BUTTON_HEIGHT = 40

    private val nameStrMaxLen = 180
    private val nameFBO = FrameBuffer(Pixmap.Format.RGBA8888, 1024, capsuleHeight, false)

    private val baloonTexture = ModMgr.getGdxFile("musicplayer", "gui/blob.tga").let {
        TextureRegionPack(it, capsuleMosaicSize, capsuleMosaicSize)
    }
    private val textmask = ModMgr.getGdxFile("musicplayer", "gui/textmask.tga").let {
        TextureRegionPack(it, maskOffWidth, capsuleHeight)
    }
    private val controlButtons = ModMgr.getGdxFile("musicplayer", "gui/control_buttons.tga").let {
        TextureRegionPack(it, BUTTON_WIDTH, BUTTON_HEIGHT)
    }

    private val MODE_IDLE = 0
    private val MODE_PLAYING = 1
    private val MODE_MOUSE_UP = 64
    private val MODE_SHOW_LIST = 128

    private var mode = MODE_IDLE
    private var modeNext = MODE_IDLE
    private var transitionAkku = 0f
    private var transitionRequest: Int? = null
    private val transitionOngoing
        get() = transitionAkku < TRANSITION_LENGTH

    private var currentListMode = 0 // 0: album, 1: playlist
    private var currentListModeNext = 0 // 0: album, 1: playlist
    private var listModeTransitionAkku = 0f
    private var currentListTransitionRequest: Int? = null // 0: album, 1: playlist
    private val listModeTransitionOngoing
        get() = listModeTransitionAkku < LIST_MODE_TRANS_LENGTH


    private val TRANSITION_LENGTH = 0.45f
    private val LIST_MODE_TRANS_LENGTH = 0.15f

    private val colourBack = Color(0xffffff_99.toInt())

    private val colourText = Color(0xffffff_cc.toInt())
    private val colourMeter = Color(0xeeeeee_cc.toInt())
    private val colourMeter2 = Color(0xeeeeee_66.toInt())

    private val colourControlButton = Color(0xeeeeee_cc.toInt())

    private var currentlySelectedAlbum: AlbumProp? = null

    /** Returns the internal playlist of the MusicGovernor */
    private val songsInGovernor: List<MusicContainer>
        get() = ingame.musicGovernor.extortField<List<MusicContainer>>("songs")!!

    private val shouldPlayerBeDisabled: Boolean
        get() {
            return App.audioMixer.dynamicTracks.any { it.isPlaying && it.trackingTarget is PlaysMusic }
        }

    /** Returns the playlist name from the MusicGovernor. Getting the value from the MusicGovernor
     * is recommended as an ingame interaction may cancel the playback from the playlist from the MusicPlayer
     * (e.g. interacting with a jukebox) */
    private val internalPlaylistName: String
        get() = ingame.musicGovernor.playlistName

    fun registerPlaylist(path: String, fileToName: JsonValue?, shuffled: Boolean, diskJockeyingMode: String) {
        fun String.isNum(): Boolean {
            try {
                this.toInt()
                return true
            }
            catch (e: NumberFormatException) {
                return false
            }
        }

        ingame.musicGovernor.queueDirectory(path, shuffled, diskJockeyingMode) { filename ->
            fileToName?.get(filename).let {
                if (it == null)
                    filename.substringBeforeLast('.').replace('_', ' ').split(" ").map { it.capitalize() }.let {
                        // if the first token in the list is numeral, drop it
                        if (it.first().isNum())
                            it.subList(1, it.size).joinToString(" ")
                        else
                            it.joinToString(" ")

                    }
                else
                    it.asString()
            }
        }

        ingame.musicGovernor.addMusicStartHook { music ->
            setMusicName(music.name)
            if (mode <= MODE_PLAYING)
                transitionRequest = MODE_PLAYING
        }

        ingame.musicGovernor.addMusicStopHook { music ->
            setIntermission()
            if (mode <= MODE_PLAYING)
                transitionRequest = MODE_IDLE
        }

        setPlaylistDisplayVars(songsInGovernor)
    }

    private var currentMusicName = ""
    private var nameLength = 0 // truncated
    private var nameLengthOld = 0 // truncated
    private var realNameLength = 0 // NOT truncated
    private var nameOverflown = false
    private var nameScroll = 0f
    private var musicPlayingTimer = 0f
    private val NAME_SCROLL_PER_SECOND = 15f

    private fun setIntermission() {
        currentMusicName = ""
        nameLength = 0
        realNameLength = 0
        nameScroll = 0f
        nameOverflown = false
    }

    private fun setMusicName(str: String) {
        currentMusicName = str
        realNameLength = App.fontGameFBO.getWidth(str)
        nameLength = realNameLength.coerceAtMost(nameStrMaxLen)
//        TRANSITION_LENGTH = 1.5f * ((nameLength - nameLengthOld).absoluteValue.toFloat() / nameStrMaxLen)
//        if (TRANSITION_LENGTH.isNaN()) TRANSITION_LENGTH = 0f
        nameOverflown = (realNameLength > nameLength)
        musicPlayingTimer = 0f

//        printdbg(this, "setMusicName $str; strLen = $nameLengthOld -> $nameLength; overflown=$nameOverflown; transitionTime=$TRANSITION_LENGTH")
    }

    private val mouseLatch = MouseLatch()

    private var mouseOnButton: Int? = null
    private var mouseOnList: Int? = null

    override fun updateImpl(delta: Float) {
        val shouldPlayerBeDisabled = shouldPlayerBeDisabled

        // process transition request
        if (transitionRequest != null) {
            modeNext = transitionRequest!!
            transitionAkku = 0f
            transitionRequest = null
        }
        if (currentListTransitionRequest != null) {
            currentListModeNext = currentListTransitionRequest!!
            listModeTransitionAkku = 0f
            currentListTransitionRequest = null
        }

        // actually do transition
        if (transitionAkku <= TRANSITION_LENGTH) {
            makeTransition()

            transitionAkku += delta

//            printdbg(this, "On transition... ($transitionAkku / $TRANSITION_LENGTH); width = $width")

            if (transitionAkku >= TRANSITION_LENGTH) {
                mode = modeNext
//                printdbg(this, "Transition complete: nameLengthOld=${nameLengthOld} -> ${nameLength}")
                nameLengthOld = nameLength
                setFinalWidthHeight()
            }
        }
        if (listModeTransitionAkku <= LIST_MODE_TRANS_LENGTH) {
            listViewPanelScroll = FastMath.interpolateLinear(listModeTransitionAkku / LIST_MODE_TRANS_LENGTH, currentListMode.toFloat(), currentListModeNext.toFloat())


            listModeTransitionAkku += delta

//            printdbg(this, "On transition... ($transitionAkku / $TRANSITION_LENGTH); width = $width")

            if (listModeTransitionAkku >= LIST_MODE_TRANS_LENGTH) {
                currentListMode = currentListModeNext!!
                listViewPanelScroll = currentListMode.toFloat()
//                printdbg(this, "Transition complete: nameLengthOld=${nameLengthOld} -> ${nameLength}")
            }
        }

        // scroll music title
        if (mode > MODE_IDLE) {
            if (nameOverflown) {
                when (musicPlayingTimer % 60f) {
                    // start scroll
                    in 0f..5f -> {
                        nameScroll = 0f
                    }
                    in 5f..10f -> {
                        nameScroll = (nameScroll + NAME_SCROLL_PER_SECOND * delta).coerceIn(0f, (realNameLength - nameLength).toFloat())
                    }
                    in 10f..15f -> {
                        nameScroll = (realNameLength - nameLength).toFloat()
                    }
                    // start unscroll
                    in 15f..20f -> {
                        nameScroll = (nameScroll - NAME_SCROLL_PER_SECOND * delta).coerceIn(0f, (realNameLength - nameLength).toFloat())
                    }
                    else -> {
                        nameScroll = 0f
                    }
                }
            }
            
            if (mode >= MODE_SHOW_LIST) {
                for (i in playlistNameScrolls.indices) {
                    if (playlistNameOverflown[i]) { // name overflown
                        when (musicPlayingTimer % 60f) {
                            // start scroll
                            in 0f..5f -> {
                                playlistNameScrolls[i] = 0f
                            }
                            in 5f..10f -> {
                                playlistNameScrolls[i] = (playlistNameScrolls[i] + NAME_SCROLL_PER_SECOND * delta).coerceIn(0f, (playlistRealNameLen[i] - playlistNameLenMax).toFloat())
                            }
                            in 10f..15f -> {
                                playlistNameScrolls[i] = (playlistRealNameLen[i] - playlistNameLenMax).toFloat()
                            }
                            // start unscroll
                            in 15f..20f -> {
                                playlistNameScrolls[i] = (playlistNameScrolls[i] - NAME_SCROLL_PER_SECOND * delta).coerceIn(0f, (playlistRealNameLen[i] - playlistNameLenMax).toFloat())
                            }
                            else -> {
                                playlistNameScrolls[i] = 0f
                            }
                        }
                    }
                }
            }

            musicPlayingTimer += delta
        }

        updateMeter()


        if (!transitionOngoing) {
            if (mouseUp && mode < MODE_MOUSE_UP) {
                transitionRequest = MODE_MOUSE_UP
            }
            else if (!mouseUp && mode == MODE_MOUSE_UP) {
                transitionRequest = if (currentMusicName.isEmpty()) MODE_IDLE else MODE_PLAYING
            }
        }

        // mouse is over which button?
        if (mode >= MODE_MOUSE_UP && relativeMouseY.toFloat() in posYforControls + 12f .. posYforControls + 12f + BUTTON_HEIGHT) {
            mouseOnButton = if (relativeMouseX.toFloat() in Toolkit.hdrawWidthf - 120f ..  Toolkit.hdrawWidthf - 120f + 5 * BUTTON_WIDTH) {
                ((relativeMouseX.toFloat() - (Toolkit.hdrawWidthf - 120f)) / BUTTON_WIDTH).toInt()
            }
            else null
        }
        else {
            mouseOnButton = null
        }

        // mouse is over which list
        mouseOnList = if (mode >= MODE_SHOW_LIST &&
            relativeMouseY.toFloat() in _posY + 9.._posY + 9 + PLAYLIST_LINES*PLAYLIST_LINE_HEIGHT &&
            relativeMouseX.toFloat() in _posX.._posX + width) {

            ((relativeMouseY - (_posY + 9)) / PLAYLIST_LINE_HEIGHT).toInt()
        }
        else null


        // make button work
        if (mouseUp) mouseLatch.latch {
            if (mouseOnButton != null) {
                when (mouseOnButton) {
                    0 -> { // album
                        albumsListCacheIsStale =
                            true // clicking the button will refresh the album list, even if the current view is the album list

                        if (mode < MODE_SHOW_LIST) {
                            if (!transitionOngoing) {
                                transitionRequest = MODE_SHOW_LIST
                                currentListMode =
                                    0 // no list transition anim is needed this time, just set the variable
                                resetAlbumlistScroll()
                            }
                        }
                        else if (currentListMode != 0) {
                            if (!listModeTransitionOngoing)
                                currentListTransitionRequest = 0
                        }
                        else {
                            if (!transitionOngoing)
                                transitionRequest = App.audioMixer.musicTrack.isPlaying.toInt() * MODE_MOUSE_UP
                        }
                    }

                    1 -> { // prev
                        // prev song
                        if (mode < MODE_SHOW_LIST) {
                            getPrevSongFromPlaylist()?.let { ingame.musicGovernor.unshiftPlaylist(it) }
                            if (!shouldPlayerBeDisabled) {
                                App.audioMixer.requestFadeOut(
                                    App.audioMixer.musicTrack,
                                    AudioMixer.DEFAULT_FADEOUT_LEN / 3f
                                ) {
                                    ingame.musicGovernor.startMusic(this) // required for "intermittent" mode
                                    iHitTheStopButton = false
                                    stopRequested = false
                                }
                            }
                        }
                        // prev page in the playlist
                        else if (listViewPanelScroll == 1f) {
                            val scrollMax = ((currentlySelectedAlbum?.length
                                ?: 0).toFloat() / PLAYLIST_LINES).ceilToInt() * PLAYLIST_LINES
                            playlistScroll = (playlistScroll - PLAYLIST_LINES) fmod scrollMax
                        }
                        // prev page in the albumlist
                        else if (listViewPanelScroll == 0f) {
                            val scrollMax = (albumsList.size.toFloat() / PLAYLIST_LINES).ceilToInt() * PLAYLIST_LINES
                            albumlistScroll = (albumlistScroll - PLAYLIST_LINES) fmod scrollMax
                        }
                    }

                    2 -> { // stop
                        if (mode < MODE_SHOW_LIST) { // disable stop button entirely on MODE_SHOW_LIST
                            if (App.audioMixer.musicTrack.isPlaying) {
                                val thisMusic = App.audioMixer.musicTrack.currentTrack
                                App.audioMixer.requestFadeOut(App.audioMixer.musicTrack, AudioMixer.DEFAULT_FADEOUT_LEN / 3f)
                                App.audioMixer.musicTrack.nextTrack = null
                                ingame.musicGovernor.stopMusic(this)
                                if (thisMusic is MusicContainer) thisMusic.let { ingame.musicGovernor.queueMusicToPlayNext(it) }
                                iHitTheStopButton = true
                            }
                            else if (!shouldPlayerBeDisabled) {
                                ingame.musicGovernor.startMusic(this)
                                iHitTheStopButton = false
                                stopRequested = false
                            }
                        }
                    }

                    3 -> { // next
                        // next song
                        if (mode < MODE_SHOW_LIST) {
                            if (!shouldPlayerBeDisabled) {
                                App.audioMixer.requestFadeOut(
                                    App.audioMixer.musicTrack,
                                    AudioMixer.DEFAULT_FADEOUT_LEN / 3f
                                ) {
                                    ingame.musicGovernor.startMusic(this) // required for "intermittent" mode, does seemingly nothing on "continuous" mode
                                    iHitTheStopButton = false
                                    stopRequested = false
                                }
                            }
                        }
                        // next page in the playlist
                        else if (listViewPanelScroll == 1f) {
                            val scrollMax = ((currentlySelectedAlbum?.length
                                ?: 0).toFloat() / PLAYLIST_LINES).ceilToInt() * PLAYLIST_LINES
                            playlistScroll = (playlistScroll + PLAYLIST_LINES) fmod scrollMax
                        }
                        // next page in the albumlist
                        else if (listViewPanelScroll == 0f) {
                            val scrollMax = (albumsList.size.toFloat() / PLAYLIST_LINES).ceilToInt() * PLAYLIST_LINES
                            albumlistScroll = (albumlistScroll + PLAYLIST_LINES) fmod scrollMax
                        }
                    }

                    4 -> { // playlist
                        if (mode < MODE_SHOW_LIST) {
                            if (!transitionOngoing) {
                                transitionRequest = MODE_SHOW_LIST
                                currentListMode =
                                    1 // no list transition anim is needed this time, just set the variable
                                resetPlaylistScroll()
                            }
                        }
                        else if (currentListMode != 1) {
                            if (!listModeTransitionOngoing)
                                currentListTransitionRequest = 1
                        }
                        else {
                            if (!transitionOngoing)
                                transitionRequest = App.audioMixer.musicTrack.isPlaying.toInt() * MODE_MOUSE_UP
                        }
                    }
                }
            }
            // make playlist clicking work
            else if (listViewPanelScroll == 1f && mouseOnList != null) {
                val index = playlistScroll + mouseOnList!!
                val list = songsInGovernor
                if (index < list.size) {
                    // if selected song != currently playing
                    if (App.audioMixer.musicTrack.currentTrack == null || list[index] != App.audioMixer.musicTrack.currentTrack) {
                        // rebuild playlist
                        ingame.musicGovernor.queueIndexFromPlaylist(index)

                        // fade out
                        App.audioMixer.requestFadeOut(App.audioMixer.musicTrack, AudioMixer.DEFAULT_FADEOUT_LEN / 3f) {
                            if (!shouldPlayerBeDisabled) {
                                ingame.musicGovernor.startMusic(this) // required for "intermittent" mode
                                iHitTheStopButton = false
                                stopRequested = false
                            }
                        }
                    }
                }
            }
            // make album list clicking work
            else if (listViewPanelScroll == 0f && mouseOnList != null) {
                val index = albumlistScroll + mouseOnList!!
                val list = albumsList//.map { albumPropCache[it] }

                if (index < list.size) {
                    // if selected album is not the same album currently playing, queue that album immediately
                    // (navigating into the selected album involves too much complication :p)
                    if (ingame.musicGovernor.playlistSource != albumsList[index].canonicalPath) {
                        // fade out
                        App.audioMixer.requestFadeOut(App.audioMixer.musicTrack, AudioMixer.DEFAULT_FADEOUT_LEN / 3f) {
                            loadNewAlbum(albumsList[index])
                            if (!shouldPlayerBeDisabled) {
                                ingame.musicGovernor.startMusic(this) // required for "intermittent" mode
                                iHitTheStopButton = false
                                stopRequested = false
                            }
                            resetPlaylistScroll(App.audioMixer.musicTrack.nextTrack as? MusicContainer)
                        }
                    }
                }
            }
            // click on the music title to return to MODE_MOUSE_UP
            else if (mouseUp && relativeMouseY.toFloat() in _posY + height - capsuleHeight.._posY + height && !transitionOngoing && mode > MODE_MOUSE_UP) {
                transitionRequest = MODE_MOUSE_UP
            }
        }


//        printdbg(this, "mode = $mode; req = $transitionRequest")

        if (shouldPlayerBeDisabled || iHitTheStopButton) {
            if (!stopRequested) {
                stopRequested = true
                ingame.musicGovernor.stopMusic(this)
            }
        }
        else if (ingame.musicGovernor.playCaller is PlaysMusic && !jukeboxStopMonitorAlert && !App.audioMixer.musicTrack.isPlaying) {
            jukeboxStopMonitorAlert = true
            val interval = ingame.musicGovernor.getRandomMusicInterval()
            ingame.musicGovernor.stopMusic(this, false, interval)
        }
        else if (App.audioMixer.musicTrack.isPlaying) {
            jukeboxStopMonitorAlert = false
        }
    }

    private var jukeboxStopMonitorAlert = true
    private var iHitTheStopButton = false
    private var stopRequested = false

    private fun resetAlbumlistScroll() {
        val currentlyPlaying = albumsList.indexOfFirst { it.canonicalPath.replace('\\', '/') == ingame.musicGovernor.playlistSource }
        if (currentlyPlaying >= 0) {
            albumlistScroll = (currentlyPlaying / PLAYLIST_LINES) * PLAYLIST_LINES
        }
        else {
            albumlistScroll = 0
        }
    }

    private fun resetPlaylistScroll(song: MusicContainer? = null) {
        val currentlyPlaying = songsInGovernor.indexOf(song ?: App.audioMixer.musicTrack.currentTrack)
        if (currentlyPlaying >= 0) {
            playlistScroll = (currentlyPlaying / PLAYLIST_LINES) * PLAYLIST_LINES
        }
        else {
            playlistScroll = 0
        }
    }

    private fun getPrevSongFromPlaylist(): MusicContainer? {
        val list = songsInGovernor.slice(songsInGovernor.indices) // make copy of the list
        val nowPlaying = App.audioMixer.musicTrack.currentTrack ?: return null

        // find current index
        val currentIndex = list.indexOfFirst { it == nowPlaying }
        if (currentIndex < 0) return null

        val prevIndex = (currentIndex - 1).fmod(list.size)
        return list[prevIndex]
    }

//    private fun smoothstep(x: Float) = (x*x*(3f-2f*x)).coerceIn(0f, 1f)
//    private fun smootherstep(x: Float) = (x*x*x*(x*(6f*x-15f)+10f)).coerceIn(0f, 1f)

    private fun uiWidthFromTextWidth(tw: Int): Int = if (tw == 0) METERS_WIDTH.toInt() else (tw + METERS_WIDTH + maskOffWidth).roundToInt()

    private fun setUIwidthFromTextWidth(widthOld: Int, widthNew: Int, percentage: Float) {
        val percentage = if (percentage.isNaN()) 0f else percentage
        val zeroWidth = uiWidthFromTextWidth(widthOld).toFloat()
        val maxWidth = uiWidthFromTextWidth(widthNew).toFloat()
        val step = organicOvershoot(percentage.coerceIn(0f, 1f).toDouble()).toFloat()

//        printdbg(this, "setUIwidth: $zeroWidth -> $maxWidth; perc = $percentage")

        width = FastMath.interpolateLinearNoClamp(step, zeroWidth, maxWidth).roundToInt()
    }

    private fun setUIheight(heightOld: Int, heightNew: Int, percentage: Float) {
        val percentage = if (percentage.isNaN()) 0f else percentage
        val step = organicOvershoot(percentage.coerceIn(0f, 1f).toDouble()).toFloat()
        height = FastMath.interpolateLinearNoClamp(step, heightOld.toFloat(), heightNew.toFloat()).roundToInt().coerceAtLeast(capsuleHeight)
    }

    // changes ui width
    private fun makeTransition() {
        transitionDB[mode to modeNext].let {
            if (it == null) throw NullPointerException("No transition for $mode -> $modeNext")
            it.invoke(transitionAkku)
        }
    }

    private val transitionDB = HashMap<Pair<Int, Int>, (Float) -> Unit>().also {
        it[MODE_IDLE to MODE_IDLE] = { akku -> }
        it[MODE_IDLE to MODE_PLAYING] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, nameLength, akku / TRANSITION_LENGTH)
        }
        it[MODE_IDLE to MODE_MOUSE_UP] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, widthForMouseUp, akku / TRANSITION_LENGTH)
            setUIheight(heightThin, heightControl, akku / TRANSITION_LENGTH)
        }


        it[MODE_PLAYING to MODE_PLAYING] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, nameLength, akku / TRANSITION_LENGTH)
        }
        it[MODE_PLAYING to MODE_IDLE] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, nameLength, akku / TRANSITION_LENGTH)
        }
        it[MODE_PLAYING to MODE_MOUSE_UP] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, widthForMouseUp, akku / TRANSITION_LENGTH)
            setUIheight(heightThin, heightControl, akku / TRANSITION_LENGTH)
        }


        it[MODE_MOUSE_UP to MODE_PLAYING] = { akku ->
            setUIwidthFromTextWidth(widthForMouseUp, nameLength, akku / TRANSITION_LENGTH)
            setUIheight(heightControl, heightThin, akku / TRANSITION_LENGTH)
        }
        it[MODE_MOUSE_UP to MODE_IDLE] = { akku ->
            setUIwidthFromTextWidth(widthForMouseUp, 0, akku / TRANSITION_LENGTH)
            setUIheight(heightControl, heightThin, akku / TRANSITION_LENGTH)
        }
        it[MODE_MOUSE_UP to MODE_SHOW_LIST] = { akku ->
            setUIwidthFromTextWidth(widthForMouseUp, widthForList, akku / TRANSITION_LENGTH)
            setUIheight(heightControl, heightList, akku / TRANSITION_LENGTH)
        }
        it[MODE_MOUSE_UP to MODE_MOUSE_UP] = { akku -> }


        it[MODE_SHOW_LIST to MODE_MOUSE_UP] = { akku ->
            setUIwidthFromTextWidth(widthForList, widthForMouseUp, akku / TRANSITION_LENGTH)
            setUIheight(heightList, heightControl, akku / TRANSITION_LENGTH)
        }
        it[MODE_SHOW_LIST to MODE_PLAYING] = { akku ->
            setUIwidthFromTextWidth(widthForList, nameLength, akku / TRANSITION_LENGTH)
            setUIheight(heightList, heightThin, akku / TRANSITION_LENGTH)
        }
        it[MODE_SHOW_LIST to MODE_IDLE] = { akku ->
            setUIwidthFromTextWidth(widthForList, nameLength, akku / TRANSITION_LENGTH)
            setUIheight(heightList, heightThin, akku / TRANSITION_LENGTH)
        }
        it[MODE_SHOW_LIST to MODE_SHOW_LIST] = { akku -> }

    }

    private fun setFinalWidthHeight() {
        when (mode) {
            MODE_IDLE -> {
                width = uiWidthFromTextWidth(0)
                height = heightThin
            }
            MODE_PLAYING -> {
                width = uiWidthFromTextWidth(nameLength)
                height = heightThin
            }
            MODE_MOUSE_UP -> {
                width = uiWidthFromTextWidth(widthForMouseUp)
                height = heightControl
            }
            MODE_SHOW_LIST -> {
                width = uiWidthFromTextWidth(widthForList)
                height = heightList
            }
        }
    }

    private var _posX = 0f // not using provided `posX` as there is one frame delay between update and it actually used to drawing
    private var _posY = 0f

    private var widthForFreqMeter = 0
    private var posXforMusicLine = 0f
    private var posYforMusicLine = 0f
    private var posYforControls = 0f

    override val mouseUp: Boolean
        get() = relativeMouseX.toFloat() in _posX-capsuleMosaicSize .. _posX+width+capsuleMosaicSize &&
                relativeMouseY.toFloat() in _posY .. _posY+height

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        widthForFreqMeter = if (transitionOngoing && modeNext >= MODE_MOUSE_UP || mode >= MODE_MOUSE_UP)
            uiWidthFromTextWidth(nameLength)
        else
            width



        batch.end()
        renderNameToFBO(batch, camera, currentMusicName, widthForFreqMeter.toFloat())
        batch.begin()


        _posX = ((Toolkit.drawWidth - width) / 2).toFloat()
        _posY = (App.scr.height - App.scr.tvSafeGraphicsHeight - height).toFloat()

        posYforControls = if
            (mode <= MODE_MOUSE_UP && modeNext <= MODE_MOUSE_UP) _posY
        else
            (App.scr.height - App.scr.tvSafeGraphicsHeight - minOf(height, heightControl)).toFloat()


        val _posXnonStretched = ((Toolkit.drawWidth - uiWidthFromTextWidth(nameLength)) / 2).toFloat()


        posXforMusicLine = if (transitionOngoing && modeNext >= MODE_MOUSE_UP || mode >= MODE_MOUSE_UP)
            _posXnonStretched
        else
            _posX


        posYforMusicLine = _posY + height - capsuleHeight + 1



        blendNormalStraightAlpha(batch)
        drawBaloon(batch, _posX, _posY, width.toFloat(), (height - capsuleHeight.toFloat()).coerceAtLeast(0f))
        drawText(batch, posXforMusicLine, posYforMusicLine)
        drawFreqMeter(batch, posXforMusicLine + widthForFreqMeter - 18f, _posY + height - (capsuleHeight / 2) + 1f)
        drawControls(frameDelta, batch, _posX, posYforControls)
        drawList(camera, frameDelta, batch, _posX, _posY)

        batch.color = Color.WHITE


        // TEST CODE
        listViewPanelScroll = currentListMode.toFloat()
    }

    private val widthForList = 320
    private val hwidthForList = widthForList / 2f

    private val PLAYLIST_LEFT_GAP = METERS_WIDTH.toInt() + maskOffWidth
    private val PLAYLIST_NAME_LEN = widthForList
    private val PLAYLIST_LINE_HEIGHT = 28f
    private val PLAYLIST_LINES = App.getConfigInt("musicplayer:playlistlines").let { if (it < 4) 4 else it }

    private val playListAnimAkku = FloatArray(PLAYLIST_LINES) // how many control buttons?
    private val playListAnimLength = 0.16f

    private var listViewPanelScroll = 0f // 0: show albums, 1: show playlist, anything inbetween: transition ongoing


    private fun drawList(camera: OrthographicCamera, delta: Float, batch: SpriteBatch, x: Float, y: Float) {
        val alphaLeft = (1f - listViewPanelScroll).pow(2f)
        val alphaRight = listViewPanelScroll.pow(2f)

        var (drawAlpha, reverse) = if (mode < MODE_SHOW_LIST && modeNext == MODE_SHOW_LIST)
            (transitionAkku / TRANSITION_LENGTH).let { if (it.isNaN()) 0f else it } to false
        else if (mode == MODE_SHOW_LIST && modeNext < MODE_SHOW_LIST)
            (transitionAkku / TRANSITION_LENGTH).let { if (it.isNaN()) 0f else it } to true
        else if (mode >= MODE_SHOW_LIST)
            1f to false
        else
            0f to false

        drawAlpha = drawAlpha.coerceIn(0f, 1f).organicOvershoot().coerceAtMost(1f)
        drawAlpha = 0.75f * (if (reverse) 1f - drawAlpha else drawAlpha).pow(3f)

        val anchorX = Toolkit.hdrawWidthf - width / 2

        val posXLeft = FastMath.interpolateLinear(listViewPanelScroll, 0f, -hwidthForList)
        val posXRight = FastMath.interpolateLinear(listViewPanelScroll, hwidthForList, 0f)


        // assuming both view has the same list dimension
        drawAlbumList(camera, delta, batch, (anchorX + posXLeft).roundToFloat(), (y + 5).roundToFloat(), drawAlpha * alphaLeft, width / (widthForList + METERS_WIDTH + maskOffWidth))
        drawPlayList(camera, delta, batch, (anchorX + posXRight).roundToFloat(), (y + 5).roundToFloat(), drawAlpha * alphaRight, width / (widthForList + METERS_WIDTH + maskOffWidth))

        // update playListAnimAkku
        for (i in playListAnimAkku.indices) {
            if (mouseOnList == i && mode >= MODE_SHOW_LIST && modeNext >= MODE_SHOW_LIST)
                playListAnimAkku[i] = (playListAnimAkku[i] + (delta / playListAnimLength)).coerceIn(0f, 1f)
            else
                playListAnimAkku[i] = (playListAnimAkku[i] - (delta / playListAnimLength)).coerceIn(0f, 1f)
        }
    }


    private val widthForMouseUp = (nameStrMaxLen + METERS_WIDTH + maskOffWidth).toInt()
    private val heightThin = 28
    private val heightControl = 80
    private val heightList = 103 + PLAYLIST_LINES * PLAYLIST_LINE_HEIGHT.toInt()

    private val playlistNameLenMax = widthForList - 2*maskOffWidth
    private var playlistScroll = 0

    private val playlistFBOs = Array(PLAYLIST_LINES) { FrameBuffer(Pixmap.Format.RGBA8888, PLAYLIST_NAME_LEN, PLAYLIST_LINE_HEIGHT.toInt(), false) }
    private val playlistNameScrolls = FloatArray(1024) // use absolute index
    private val playlistRealNameLen = IntArray(1024) // use absolute index
    private val playlistNameOverflown = BitSet(1024) // use absolute index

    private val albumlistFBOs = Array(PLAYLIST_LINES) { FrameBuffer(Pixmap.Format.RGBA8888, PLAYLIST_NAME_LEN, PLAYLIST_LINE_HEIGHT.toInt(), false) }
    private val albumlistNameScrolls = FloatArray(1024) // use absolute index
    private val albumlistRealNameLen = IntArray(1024) // use absolute index
    private val albumlistNameOverflown = BitSet(1024) // use absolute index
    private var albumlistScroll = 0


    private fun resetPlaylistDisplay() {
        playlistScroll = 0
        playlistNameScrolls.fill(0f)
        playlistNameOverflown.clear()
    }

    private fun resetAlbumlistDisplay() {
        albumlistScroll = 0
        albumlistNameScrolls.fill(0f)
        albumlistNameOverflown.clear()
    }
    
    private fun setPlaylistDisplayVars(plist: List<MusicContainer>) {
        resetPlaylistDisplay()
        plist.forEachIndexed { i, music ->
            val len = App.fontGameFBO.getWidth(music.name)
            val overflown = (len >= playlistNameLenMax)
            playlistRealNameLen[i] = len
            if (overflown) playlistNameOverflown.set(i)
        }
    }

    private fun setAlbumlistDisplayVars(plist: List<File>) {
        resetAlbumlistDisplay()
        plist.forEachIndexed { i, file ->
            val prop = albumPropCache[file]
            val len = App.fontGameFBO.getWidth(prop.name)
            val overflown = (len >= playlistNameLenMax)
            albumlistRealNameLen[i] = len
            if (overflown) albumlistNameOverflown.set(i)
        }
    }

    private fun drawPlayList(camera: OrthographicCamera, delta: Float, batch: SpriteBatch, x: Float, y: Float, alpha: Float, scale: Float) {
        batch.end()

        playlistFBOs.forEachIndexed { i, it ->
            val pnum = i + playlistScroll
            it.inAction(camera, batch) {
                batch.inUse {
                    batch.color = Color.WHITE
                    gdxClearAndEnableBlend(0f, 0f, 0f, 0f)
                    blendNormalStraightAlpha(batch)

                    // draw text
                    App.fontGameFBO.draw(batch, if (pnum in songsInGovernor.indices) songsInGovernor[pnum].name else "", maskOffWidth - playlistNameScrolls[pnum], (PLAYLIST_LINE_HEIGHT - 24) / 2)

                    // mask off the area
                    batch.color = Color.WHITE
                    blendAlphaMask(batch)
                    batch.draw(textmask.get(0, 0), 0f, 0f, maskOffWidth.toFloat(), PLAYLIST_LINE_HEIGHT)
                    batch.draw(textmask.get(1, 0), maskOffWidth.toFloat(), 0f, PLAYLIST_NAME_LEN - 2f * maskOffWidth, PLAYLIST_LINE_HEIGHT)
                    batch.draw(textmask.get(2, 0), PLAYLIST_NAME_LEN - maskOffWidth.toFloat(), 0f, maskOffWidth.toFloat(), PLAYLIST_LINE_HEIGHT)

                    blendNormalStraightAlpha(batch) // qnd hack to make sure this line gets called, otherwise the screen briefly goes blank when the playlist view is closed
                    Toolkit.fillArea(batch, 999f, 999f, 1f, 1f)
                }
            }
        }

        batch.begin()
        blendNormalStraightAlpha(batch)
        if (alpha > 0f) {
            playlistFBOs.forEachIndexed { i, it ->
                val alpha2 = alpha + (playListAnimAkku[i] * 0.2f)
                val pnum = i + playlistScroll

                val m1 = songsInGovernor.getOrNull(pnum)
                val m2 = App.audioMixer.musicTrack.currentTrack
                val currentlyPlaying = if (m1 == null || m2 == null) false else (m1 == m2)

                // print number

                // print bars instead of numbers if the song is currently being played
                if (currentlyPlaying) {
                    val xoff = 6
                    val yoff = 5 + 7 + (PLAYLIST_LINE_HEIGHT - 24) / 2
                    // it will set the colour on its own
                    drawFreqMeter(batch, x + xoff, y + yoff + PLAYLIST_LINE_HEIGHT * i * scale, alpha)
                }
                else {
                    val xoff = maskOffWidth + (if (pnum < 9) 3 else 0)
                    val yoff = 7 + (PLAYLIST_LINE_HEIGHT - 24) / 2
                    batch.color = Color(1f, 1f, 1f, alpha * 0.75f)
                    App.fontSmallNumbers.draw(
                        batch,
                        if (pnum in songsInGovernor.indices) "${pnum + 1}" else "",
                        x + xoff,
                        y + yoff + PLAYLIST_LINE_HEIGHT * i * scale
                    )
                }

                // print the name
                batch.color = Color(1f, 1f, 1f, alpha2)
                batch.draw(it.colorBufferTexture, x + PLAYLIST_LEFT_GAP * scale, y + PLAYLIST_LINE_HEIGHT * i * scale, it.width * scale, it.height * scale)

                // separator
                batch.color = Color(1f, 1f, 1f, alpha * 0.25f)
                Toolkit.drawStraightLine(batch, x, y + PLAYLIST_LINE_HEIGHT * (i + 1) * scale, x + width * scale, 1f, false)
            }

            // print the album name
            batch.color = Color(1f, 1f, 1f, alpha * 0.75f)
            Toolkit.drawTextCentered(batch, App.fontGame, internalPlaylistName, width, x.roundToInt(), 3 + (y + PLAYLIST_LINE_HEIGHT * PLAYLIST_LINES * scale).roundToInt())
        }
    }

    private fun drawAlbumList(camera: OrthographicCamera, delta: Float, batch: SpriteBatch, x: Float, y: Float, alpha: Float, scale: Float) {
        batch.end()

        val albumsList = albumsList.subList(0, albumsList.size) // make a copy

        albumlistFBOs.forEachIndexed { i, it ->
            val pnum = i + albumlistScroll
            it.inAction(camera, batch) {
                batch.inUse {
                    batch.color = Color.WHITE
                    gdxClearAndEnableBlend(0f, 0f, 0f, 0f)
                    blendNormalStraightAlpha(batch)

                    // draw text
                    App.fontGameFBO.draw(batch, if (pnum in albumsList.indices) albumPropCache[albumsList[pnum]].name else "", maskOffWidth - albumlistNameScrolls[pnum], (PLAYLIST_LINE_HEIGHT - 24) / 2)

                    // mask off the area
                    batch.color = Color.WHITE
                    blendAlphaMask(batch)
                    batch.draw(textmask.get(0, 0), 0f, 0f, maskOffWidth.toFloat(), PLAYLIST_LINE_HEIGHT)
                    batch.draw(textmask.get(1, 0), maskOffWidth.toFloat(), 0f, PLAYLIST_NAME_LEN - 2f * maskOffWidth, PLAYLIST_LINE_HEIGHT)
                    batch.draw(textmask.get(2, 0), PLAYLIST_NAME_LEN - maskOffWidth.toFloat(), 0f, maskOffWidth.toFloat(), PLAYLIST_LINE_HEIGHT)

                    blendNormalStraightAlpha(batch) // qnd hack to make sure this line gets called, otherwise the screen briefly goes blank when the albumlist view is closed
                    Toolkit.fillArea(batch, 999f, 999f, 1f, 1f)
                }
            }
        }

        batch.begin()
        blendNormalStraightAlpha(batch)
        if (alpha > 0f) {
            albumlistFBOs.forEachIndexed { i, it ->
                val alpha2 = alpha + (playListAnimAkku[i] * 0.2f)
                val pnum = i + albumlistScroll

                val currentlyPlaying = if (pnum in albumsList.indices) {
                    val m1 = ingame.musicGovernor.playlistSource
                    val m2 = albumsList[pnum].canonicalPath.replace('\\', '/')
                    (m1 == m2)
                }
                else false

                // print number

                // print bars instead of numbers if the song is currently being played
                if (currentlyPlaying) {
                    val xoff = 6
                    val yoff = 5 + 7 + (PLAYLIST_LINE_HEIGHT - 24) / 2
                    // it will set the colour on its own
                    drawFreqMeter(batch, x + xoff, y + yoff + PLAYLIST_LINE_HEIGHT * i * scale, alpha)
                }
                else {
                    /*val xoff = maskOffWidth + (if (pnum < 9) 3 else 0)
                    val yoff = 7 + (PLAYLIST_LINE_HEIGHT - 24) / 2
                    batch.color = Color(1f, 1f, 1f, alpha * 0.75f)
                    App.fontSmallNumbers.draw(
                        batch,
                        if (pnum in albumsList.indices) "${pnum + 1}" else "",
                        x + xoff,
                        y + yoff + PLAYLIST_LINE_HEIGHT * i * scale
                    )*/
                }

                // print the name
                batch.color = Color(1f, 1f, 1f, alpha2)
                batch.draw(it.colorBufferTexture, x + PLAYLIST_LEFT_GAP * scale, y + PLAYLIST_LINE_HEIGHT * i * scale, it.width * scale, it.height * scale)

                // separator
                batch.color = Color(1f, 1f, 1f, alpha * 0.25f)
                Toolkit.drawStraightLine(batch, x, y + PLAYLIST_LINE_HEIGHT * (i + 1) * scale, x + width * scale, 1f, false)
            }
        }
    }

    private fun drawBaloon(batch: SpriteBatch, x: Float, y: Float, width: Float, height: Float) {
        val x = x - capsuleMosaicSize
        batch.color = colourBack// (if (mouseUp) Color.MAROON else colourBack)

        // top left
        batch.draw(baloonTexture.get(0, 0), x, y, capsuleMosaicSize.toFloat(), capsuleMosaicSize.toFloat())
        // top
        batch.draw(baloonTexture.get(1, 0), x + capsuleMosaicSize, y, width, capsuleMosaicSize.toFloat())
        // top right
        batch.draw(baloonTexture.get(2, 0), x + capsuleMosaicSize + width, y, capsuleMosaicSize.toFloat(), capsuleMosaicSize.toFloat())

        // left
        batch.draw(baloonTexture.get(0, 1), x, y + capsuleMosaicSize, capsuleMosaicSize.toFloat(), height)
        // centre
        batch.draw(baloonTexture.get(1, 1), x + capsuleMosaicSize, y + capsuleMosaicSize, width, height)
        // right
        batch.draw(baloonTexture.get(2, 1), x + capsuleMosaicSize + width, y + capsuleMosaicSize, capsuleMosaicSize.toFloat(), height)

        // bottom left
        batch.draw(baloonTexture.get(0, 2), x, y + capsuleMosaicSize + height, capsuleMosaicSize.toFloat(), capsuleMosaicSize.toFloat())
        // bottom
        batch.draw(baloonTexture.get(1, 2), x + capsuleMosaicSize, y + capsuleMosaicSize + height, width, capsuleMosaicSize.toFloat())
        // bottom right
        batch.draw(baloonTexture.get(2, 2), x + capsuleMosaicSize + width, y + capsuleMosaicSize + height, capsuleMosaicSize.toFloat(), capsuleMosaicSize.toFloat())
    }

    private val playControlAnimAkku = FloatArray(5) // how many control buttons?
    private val playControlAnimLength = 0.2f

    private fun drawControls(delta: Float, batch: SpriteBatch, posX: Float, posY: Float) {
        val (alpha, reverse) = if (mode < MODE_MOUSE_UP && modeNext == MODE_MOUSE_UP)
            (transitionAkku / TRANSITION_LENGTH).let { if (it.isNaN()) 0f else it } to false
        else if (mode == MODE_MOUSE_UP && modeNext < MODE_MOUSE_UP)
            (transitionAkku / TRANSITION_LENGTH).let { if (it.isNaN()) 0f else it } to true
        else if (mode >= MODE_MOUSE_UP)
            1f to false
        else
            0f to false

        val p = organicOvershoot((transitionAkku / TRANSITION_LENGTH.toDouble()).coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0).toFloat()
        val buttonFadePerc =
                if (modeNext == MODE_SHOW_LIST)
                    p
                else if (mode == MODE_SHOW_LIST && modeNext == MODE_MOUSE_UP)
                    1f - p
                else
                    0f

        if (alpha > 0f) {
            val alpha0 = alpha.coerceIn(0f, 1f).organicOvershoot().coerceAtMost(1f)
            val internalWidth =minOf(widthForMouseUp.toFloat(), width - 20f)
            val separation = internalWidth / 5f
            val anchorX = Toolkit.hdrawWidthf
            val posY = posY + 12f
            for (i in 0..4) {
                val alphaBase  = 0.75f * (if (reverse) 1f - alpha0 else alpha0).pow(3f) + (playControlAnimAkku[i] * 0.2f)
                val alphaBase2 = 0.75f * (if (reverse) 1f - alpha0 else alpha0).pow(3f)

                val offset = i - 2
                val posX = anchorX + offset * separation


                val btnX = (posX - BUTTON_WIDTH / 2).roundToFloat()
                val btnY = posY.roundToFloat()

                // actually draw icon
                // prev/next button
                if (i == 1 || i == 3) {
                    // prev/next song button
                    batch.color = Color(1f, 1f, 1f, alphaBase * (1f - buttonFadePerc))
                    batch.draw(controlButtons.get(i, 0), btnX, btnY)
                    // prev/next page button
                    batch.color = Color(1f, 1f, 1f, alphaBase * buttonFadePerc)
                    batch.draw(controlButtons.get(i, 1), btnX, btnY)
                }
                // stop button
                else if (i == 2) {
                    // get correct stop/play button
                    val iconY = if (!App.audioMixer.musicTrack.isPlaying) 1 else 0
                    // fade if avaliable
                    batch.color = Color(1f, 1f, 1f, alphaBase * (1f - buttonFadePerc))
                    batch.draw(controlButtons.get(i, iconY), btnX, btnY)
                    // page number with fade

                    for (mode in 0..1) {
                        val alphaNum = if (mode == 0) 1f - listViewPanelScroll else listViewPanelScroll
                        batch.color = Color(1f, 1f, 1f, alphaBase2 * buttonFadePerc * alphaNum) // don't use mouse-up effect
                        val (thisPage, totalPage) = if (mode == 0)
                            albumlistScroll.div(PLAYLIST_LINES).plus(1) to albumsList.size.toFloat().div(PLAYLIST_LINES).ceilToInt()
                        else
                            playlistScroll.div(PLAYLIST_LINES).plus(1) to (currentlySelectedAlbum?.length ?: 0).toFloat().div(PLAYLIST_LINES).ceilToInt()
                        Toolkit.drawTextCentered(
                            batch, App.fontSmallNumbers,
                            "${thisPage.toString().padStart(4,' ')}/" +
                                    "${totalPage.toString().padEnd(4,' ')}",
                            120, anchorX.toInt() - 60, btnY.toInt() + 14
                        )
                    }

                }
                // else button
                else {
                    batch.color = Color(1f, 1f, 1f, alphaBase)
                    batch.draw(controlButtons.get(i, 0), btnX, btnY)
                }


                // update playControlAnimAkku
                if (mouseOnButton == i && mode >= MODE_MOUSE_UP && modeNext >= MODE_MOUSE_UP)
                    playControlAnimAkku[i] = (playControlAnimAkku[i] + (delta / playControlAnimLength)).coerceIn(0f, 1f)
                else
                    playControlAnimAkku[i] = (playControlAnimAkku[i] - (delta / playControlAnimLength)).coerceIn(0f, 1f)
            }
//            printdbg(this, "playControlAnimAkku=${playControlAnimAkku.joinToString()}")
        }
    }

    private fun drawText(batch: SpriteBatch, posX: Float, posY: Float) {
        batch.color = colourText
        batch.draw(nameFBO.colorBufferTexture, posX - maskOffWidth, posY)
    }

    private fun renderNameToFBO(batch: SpriteBatch, camera: OrthographicCamera, str: String, width: Float) {
        val windowEnd = width - METERS_WIDTH - maskOffWidth

        nameFBO.inAction(camera, batch) {
            batch.inUse {
                batch.color = Color.WHITE
                gdxClearAndEnableBlend(0f, 0f, 0f, 0f)
                blendNormalStraightAlpha(batch)

                // draw text
                App.fontGameFBO.draw(batch, str, maskOffWidth.toFloat() - nameScroll, 2f)

                // mask off the area
                batch.color = Color.WHITE
                blendAlphaMask(batch)
                batch.draw(textmask.get(0, 0), 0f, 0f)
                batch.draw(textmask.get(1, 0), maskOffWidth.toFloat(), 0f, windowEnd, capsuleHeight.toFloat())
                batch.draw(textmask.get(2, 0), windowEnd + maskOffWidth, 0f)
                batch.draw(textmask.get(3, 0), windowEnd + 2 * maskOffWidth, 0f, 1000f, capsuleHeight.toFloat())
            }
        }
    }

    private val FFTSIZE = 1024
    private val inBuf = Array(2) { FloatArray(FFTSIZE) }
    private fun sin2(x: Double) = sin(x).pow(2)
    private val fftWin = FloatArray(FFTSIZE) { sin2(PI * it / FFTSIZE).toFloat() } // hann
    private val oldFFTmagn = DoubleArray(FFTSIZE / 2) { 0.0 }
    private val chsum = ComplexArray(FloatArray(FFTSIZE * 2))
    private val fftOut = ComplexArray(FloatArray(FFTSIZE * 2))
    private val binHeights = FloatArray(FFTSIZE / 2)
    private val FFT_SMOOTHING_FACTOR = BasicDebugInfoWindow.getSmoothingFactor(2048)
    private val lowlim = -34.0f // -40dB to -6dB scale will be used
    private val dbOffset = fullscaleToDecibels(0.5) // -6.02

    private val fftBinIndices = arrayOf(
        0..3,
        4..12,
        13..39,
        40..121,
        122 until FFTSIZE / 2
    ) // 60-18000 at 1024 (https://www.desmos.com/calculator/vkxhrzfam3)
    private val fftBarHeights = FloatArray(5)

    private fun updateMeter() {
        val inbuf = App.audioMixer.musicTrack.extortField<MixerTrackProcessor>("processor")!!.extortField<List<FloatArray>>("fout1")!!
        push(inbuf[0], inBuf[0])
        push(inbuf[1], inBuf[1])
        for (i in 0 until FFTSIZE) {
            chsum.reim[2*i] = (inBuf[0][i] + inBuf[1][i]) * fftWin[i]
        }

        FFT.fftInto(chsum, fftOut)

        // apply slope to the fft bins, also converts fullscale to decibels
        for (bin in binHeights.indices) {
            val freqR = (TerrarumAudioMixerTrack.SAMPLING_RATED / FFTSIZE) * (bin + 1)
            val magn0 = fftOut.reim[2 * bin].absoluteValue / FFTSIZE * (freqR / 20.0) // apply slope
            val magn = FastMath.interpolateLinear(FFT_SMOOTHING_FACTOR, magn0, oldFFTmagn[bin])
            val magnLog = fullscaleToDecibels(magn) - dbOffset

            val h = (-(magnLog - lowlim) / lowlim * STRIP_W).toFloat().coerceAtLeast(0.5f)

            binHeights[bin] = h.coerceAtMost(STRIP_W)

            oldFFTmagn[bin] = magn
        }
    }

    private fun drawFreqMeter(batch: SpriteBatch, posX: Float, posY: Float, alpha: Float = 1f) {
        fftBinIndices.mapIndexed { i, range ->
            fftBarHeights[i] = binHeights.slice(range).average().toFloat()
        }



        fftBarHeights.forEachIndexed { index, h ->
            val hInt = h.toInt().toFloat()
            val hFrac = h - hInt

            // top fraction part shade
            batch.color = colourMeter2 mul Color(1f, 1f, 1f, alpha * hFrac)
            Toolkit.fillArea(batch, posX + index*4f, posY - hInt - 1, 3f, 1f)
            // integer part shade
            batch.color = colourMeter2 mul Color(1f, 1f, 1f, alpha)
            Toolkit.fillArea(batch, posX + index*4f, posY - hInt, 3f, 2*hInt)
            // bottom fraction part shade
            batch.color = colourMeter2 mul Color(1f, 1f, 1f, alpha * hFrac)
            Toolkit.fillArea(batch, posX + index*4f, posY + hInt, 3f, 2f)

            // top fraction part
            batch.color = colourMeter mul Color(1f, 1f, 1f, alpha * hFrac)
            Toolkit.fillArea(batch, posX + index*4f, posY - hInt - 1, 2f, 1f)
            // integer part
            batch.color = colourMeter mul Color(1f, 1f, 1f, alpha)
            Toolkit.fillArea(batch, posX + index*4f, posY - hInt, 2f, 2*hInt)
            // bottom fraction part
            batch.color = colourMeter mul Color(1f, 1f, 1f, alpha * hFrac)
            Toolkit.fillArea(batch, posX + index*4f, posY + hInt, 2f, 1f)
        }
    }

    override fun dispose() {
        baloonTexture.dispose()
        nameFBO.dispose()
        playlistFBOs.forEach { it.dispose() }
    }


    private fun push(samples: FloatArray, buf: FloatArray) {
        if (samples.size >= FFTSIZE) {
            // overwrite
            System.arraycopy(samples, samples.size - buf.size, buf, 0, buf.size)
        }
        else {
            // shift samples
            System.arraycopy(buf, samples.size, buf, 0, buf.size - samples.size)
            // write to the buf
            System.arraycopy(samples, 0, buf, buf.size - samples.size, samples.size)
        }
    }

    private fun generateCubicSpline(x: DoubleArray, y: DoubleArray): PolynomialSplineFunction {
        val interpolator = SplineInterpolator()
        return interpolator.interpolate(x, y)
    }

    // Function to calculate values using the generated cubic spline
    // Spline fit of the cubic-bezier(0.5, 0, 0.25,1.25) (https://www.desmos.com/calculator/k436wurcij)
    private val curveDataX = doubleArrayOf(0.0, 0.0853881835938, 0.15576171875, 0.26171875, 0.40625, 0.59765625, 0.76220703125, 0.8706665030906, 1.0)
    private val curveDataY = doubleArrayOf(0.0, 0.0139770507813, 0.05322265625, 0.19140625, 0.59375, 0.94921875, 1.02880859375, 1.02996826172, 1.0)
    private val splineFunction = generateCubicSpline(curveDataX, curveDataY)

    fun organicOvershoot(x: Double): Double {
        return splineFunction.value(x)
    }

    private fun Float.organicOvershoot() = splineFunction.value(this.toDouble()).toFloat()

    private var albumsListCacheIsStale = false
    var albumsList: List<File> = listAlbumsQuick()
        private set
        get() {
            if (albumsListCacheIsStale) {
                albumsListCacheIsStale = false
                field = listAlbumsQuick()
            }

            return field
        }

    /**
     * Preferably called whenever an "album list view" is requested
     */
    private fun listAlbumsQuick(): List<File> {
        val out = ArrayList<File>()
        File(App.customMusicDir).listFiles()?.filter { it.isDirectory }?.forEach {
            out.add(it)
        }
        return out
    }

    private val permittedExts = hashSetOf("WAV", "OGG", "MP3")

    private val albumPropCache = object : HashMap<File, AlbumProp>() {
        // impl of getOrPut
        override fun get(key: File): AlbumProp {
            if (containsKey(key)) return super.get(key)!!
            else {
                val p = getAlbumProp(key)
                put(key, p)
                return p
            }
        }
    }

    private fun getAlbumProp(albumDir: File): AlbumProp {
        if (!albumDir.exists()) throw IllegalArgumentException("Album dir does not exist: $albumDir")
        val fileCount = albumDir.listFiles { _, name -> permittedExts.contains(name.substringAfterLast('.').uppercase()) }?.size ?: 0
        val playlistFile = File(albumDir, "playlist.json")
        if (playlistFile.exists()) {
            val playlistFile = JsonFetcher.invoke(playlistFile)
            val albumName = playlistFile.get("albumName")?.asString() ?: albumDir.name
            val diskJockeyingMode = playlistFile.get("diskJockeyingMode").asString()
            val shuffled = playlistFile.get("shuffled").asBoolean()
            val fileToName = playlistFile.get("titles")
            return AlbumProp(albumDir, albumName, diskJockeyingMode, shuffled, fileToName, fileCount)
        }
        else {
            return AlbumProp(albumDir, albumDir.name, "intermittent", true, null, fileCount)
        }
    }

    private data class AlbumProp(
        val ref: File, val name: String,
        val diskJockeyingMode: String, val shuffled: Boolean, val fileToName: JsonValue?,
        val length: Int,
        val albumArt: TextureRegion? = null
    )

    private fun loadNewAlbum(albumDir: File) {
        val albumProp = albumPropCache[albumDir]

        App.audioMixer.musicTrack.let { track ->
            track.doGaplessPlayback = (albumProp.diskJockeyingMode == "continuous")
            if (track.doGaplessPlayback) {
                track.pullNextTrack = {
                    track.currentTrack = ingame.musicGovernor.pullNextMusicTrack(true)
                    setMusicName(track.currentTrack?.name ?: "")
                }
            }
        }

        currentlySelectedAlbum = albumProp

        registerPlaylist(albumDir.absolutePath, albumProp.fileToName, albumProp.shuffled, albumProp.diskJockeyingMode)

        // scroll playlist to the page current song is
    }

    init {
        if (App.getConfigBoolean("musicplayer:usemusicplayer")) {
            setAsAlwaysVisible()

            listAlbumsQuick().let {
                if (it.isNotEmpty()) loadNewAlbum(it.random())
            }
        }
    }
}