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
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.ui.BasicDebugInfoWindow
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

    private var currentListMode: Int? = null // 0: album, 1: playlist
    private var currentListModeNext: Int? = null // 0: album, 1: playlist
    private var listModeTransitionAkku = 0f
    private var currentListTransitionRequest: Int? = null // 0: album, 1: playlist
    private val listModeTransitionOngoing
        get() = listModeTransitionAkku < LIST_MODE_TRANS_LENGTH


    private val TRANSITION_LENGTH = 0.6f
    private val LIST_MODE_TRANS_LENGTH = 0.3f

    private val colourBack = Color(0xffffff_99.toInt())

    private val colourText = Color(0xffffff_cc.toInt())
    private val colourMeter = Color(0xeeeeee_cc.toInt())
    private val colourMeter2 = Color(0xeeeeee_66.toInt())

    private val colourControlButton = Color(0xeeeeee_cc.toInt())

    private var currentlySelectedAlbum: AlbumProp? = null

    /** Returns the internal playlist of the MusicGovernor */
    private val playlist: List<MusicContainer>
        get() = ingame.musicGovernor.extortField<List<MusicContainer>>("songs")!!

    /** Returns the playlist name from the MusicGovernor. Getting the value from the MusicGovernor
     * is recommended as an ingame interaction may cancel the playback from the playlist from the MusicPlayer
     * (e.g. interacting with a jukebox) */
    private val playlistName: String
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
            if (diskJockeyingMode == "intermittent") {
                setIntermission()
                if (mode <= MODE_PLAYING)
                    transitionRequest = MODE_IDLE
            }
            else if (diskJockeyingMode == "continuous") {

            }
        }

        setPlaylistDisplayVars(playlist)
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

    private var mouseOnButton: Int? = null

    override fun updateUI(delta: Float) {
        // process transition request
        if (transitionRequest != null) {
            modeNext = transitionRequest!!
            transitionAkku = 0f
            transitionRequest = null
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
        if (mode >= MODE_MOUSE_UP && relativeMouseY.toFloat() in posYforControls + 10f .. posYforControls + 10f + BUTTON_HEIGHT) {
            mouseOnButton = if (relativeMouseX.toFloat() in Toolkit.hdrawWidthf - 120f ..  Toolkit.hdrawWidthf - 120f + 5 * BUTTON_WIDTH) {
                ((relativeMouseX.toFloat() - (Toolkit.hdrawWidthf - 120f)) / BUTTON_WIDTH).toInt()
            }
            else null
        }
        else {
            mouseOnButton = null
        }


        // make button work
        if (!playControlButtonLatched && mouseOnButton != null && Terrarum.mouseDown) {
            playControlButtonLatched = true
            when (mouseOnButton) {
                0 -> { // album
                    if (mode < MODE_SHOW_LIST) {
                        if (!transitionOngoing) {
                            transitionRequest = MODE_SHOW_LIST
                            currentListMode = 0
                        }
                    }
                    else if (currentListMode != 0) {
                        if (!listModeTransitionOngoing)
                            currentListTransitionRequest = 0
                    }
                    else {
                        if (!transitionOngoing)
                            transitionRequest = AudioMixer.musicTrack.isPlaying.toInt() * MODE_MOUSE_UP
                    }
                }
                1 -> { // prev
                    getPrevSongFromPlaylist()?.let { ingame.musicGovernor.unshiftPlaylist(it) }
                    AudioMixer.requestFadeOut(AudioMixer.musicTrack, AudioMixer.DEFAULT_FADEOUT_LEN / 3f)
                }
                2 -> { // stop
                    if (AudioMixer.musicTrack.isPlaying) {
                        val thisMusic = AudioMixer.musicTrack.currentTrack
                        AudioMixer.requestFadeOut(AudioMixer.musicTrack, AudioMixer.DEFAULT_FADEOUT_LEN / 3f)
                        AudioMixer.musicTrack.nextTrack = null
                        ingame.musicGovernor.stopMusic()
                        thisMusic?.let { ingame.musicGovernor.queueMusicToPlayNext(it) }
                    }
                    else {
                        ingame.musicGovernor.startMusic()
                    }
                }
                3 -> { // next
                    AudioMixer.requestFadeOut(AudioMixer.musicTrack, AudioMixer.DEFAULT_FADEOUT_LEN / 3f) {
//                        ingame.musicGovernor.startMusic() // it works without this?
                    }
                }
                4 -> { // playlist
                    if (mode < MODE_SHOW_LIST) {
                        if (!transitionOngoing) {
                            transitionRequest = MODE_SHOW_LIST
                            currentListMode = 1
                        }
                    }
                    else if (currentListMode != 1) {
                        if (!listModeTransitionOngoing)
                            currentListTransitionRequest = 1
                    }
                    else {
                        if (!transitionOngoing)
                            transitionRequest = AudioMixer.musicTrack.isPlaying.toInt() * MODE_MOUSE_UP
                    }
                }
            }
        }
        else if (!Terrarum.mouseDown) {
            playControlButtonLatched = false
        }


//        printdbg(this, "mode = $mode; req = $transitionRequest")
    }

    private var playControlButtonLatched = false

    private fun getPrevSongFromPlaylist(): MusicContainer? {
        val list = playlist.slice(playlist.indices) // make copy of the list
        val nowPlaying = AudioMixer.musicTrack.currentTrack ?: return null

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

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
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
        drawControls(App.UPDATE_RATE, batch, _posX, posYforControls)
        drawList(camera, App.UPDATE_RATE, batch, _posX, _posY)

        batch.color = Color.WHITE
    }

    private val playListAnimAkku = FloatArray(8) // how many lines on the list view?
    private val playListAnimLength = 0.2f

    private fun drawList(camera: OrthographicCamera, delta: Float, batch: SpriteBatch, x: Float, y: Float) {
        val (alpha, reverse) = if (mode < MODE_SHOW_LIST && modeNext == MODE_SHOW_LIST)
            (transitionAkku / TRANSITION_LENGTH).let { if (it.isNaN()) 0f else it } to false
        else if (mode == MODE_SHOW_LIST && modeNext < MODE_SHOW_LIST)
            (transitionAkku / TRANSITION_LENGTH).let { if (it.isNaN()) 0f else it } to true
        else if (mode >= MODE_SHOW_LIST)
            1f to false
        else
            0f to false

        val anchorX = Toolkit.hdrawWidthf - width / 2

        if (alpha > 0f) {
            val alpha0 = alpha.coerceIn(0f, 1f).organicOvershoot().coerceAtMost(1f)

            // TODO draw album/playlist
            val (offX2, alpha2) = if (listModeTransitionOngoing) {
                if (currentListModeNext == 0)
                    widthForList - widthForList * (listModeTransitionAkku / LIST_MODE_TRANS_LENGTH) to (listModeTransitionAkku / LIST_MODE_TRANS_LENGTH)
                else
                    -widthForList * (listModeTransitionAkku / LIST_MODE_TRANS_LENGTH) to 1f - (listModeTransitionAkku / LIST_MODE_TRANS_LENGTH)
            }
            else {
                0f to 0f
            }
            val offX1 = 0f//offX2 - widthForList
            val alpha1 = 1f - alpha2
            val drawAlpha = 0.75f * (if (reverse) 1f - alpha0 else alpha0).pow(3f)// + (playListAnimAkku[i].pow(2f) * 1.2f)

            // assuming both view has the same list dimension
            drawAlbumList(camera, delta, batch, (anchorX + offX2).roundToFloat(), (y + 5).roundToFloat(), drawAlpha * alpha1, width / (widthForList + METERS_WIDTH + maskOffWidth))
            drawPlayList(camera, delta, batch, (anchorX + offX1).roundToFloat(), (y + 5).roundToFloat(), drawAlpha * alpha2, width / (widthForList + METERS_WIDTH + maskOffWidth))

            // update playListAnimAkku
            for (i in playListAnimAkku.indices) {
                if (mouseOnButton == i && mode >= MODE_MOUSE_UP && modeNext >= MODE_MOUSE_UP)
                    playListAnimAkku[i] = (playListAnimAkku[i] + (delta / playListAnimLength)).coerceIn(0f, 1f)
                else
                    playListAnimAkku[i] = (playListAnimAkku[i] - (delta / playListAnimLength)).coerceIn(0f, 1f)
            }
        }
    }

    private val widthForList = 320

    private val PLAYLIST_LEFT_GAP = METERS_WIDTH.toInt() + maskOffWidth
    private val PLAYLIST_NAME_LEN = widthForList
    private val PLAYLIST_LINE_HEIGHT = 28f
    private val PLAYLIST_LINES = 7

    private val widthForMouseUp = (nameStrMaxLen + METERS_WIDTH + maskOffWidth).toInt()
    private val heightThin = 28
    private val heightControl = 80
    private val heightList = 103 + PLAYLIST_LINES * PLAYLIST_LINE_HEIGHT.toInt()

    private val playlistNameLenMax = widthForList - 2*maskOffWidth
    private var playlistScroll = 0

    private val playlistFBOs = Array(PLAYLIST_LINES) {
        FrameBuffer(Pixmap.Format.RGBA8888, PLAYLIST_NAME_LEN, PLAYLIST_LINE_HEIGHT.toInt(), false)
    }
    private val playlistNameScrolls = FloatArray(1024) // use absolute index
    private val playlistRealNameLen = IntArray(1024) // use absolute index
    private val playlistNameOverflown = BitSet(1024) // use absolute index

    private fun resetPlaylistDisplay() {
        playlistScroll = 0
        playlistNameScrolls.fill(0f)
        playlistNameOverflown.clear()
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

    private fun drawPlayList(camera: OrthographicCamera, delta: Float, batch: SpriteBatch, x: Float, y: Float, alpha: Float, scale: Float) {
        batch.end()

        playlistFBOs.forEachIndexed { i, it ->
            it.inAction(camera, batch) {
                batch.inUse {
                    batch.color = Color.WHITE
                    gdxClearAndEnableBlend(0f, 0f, 0f, 0f)
                    blendNormalStraightAlpha(batch)

                    // draw text
                    App.fontGameFBO.draw(batch, if (i + playlistScroll in playlist.indices) playlist[i].name else "", maskOffWidth - playlistNameScrolls[i + playlistScroll], (PLAYLIST_LINE_HEIGHT - 24) / 2)

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
                val m1 = playlist.getOrNull(i + playlistScroll)
                val m2 = AudioMixer.musicTrack.currentTrack
                val currentlyPlaying = if (m1 == null || m2 == null) false else (m1 == m2)

                // print number

                // print bars instead of numbers if the song is currently being played
                if (currentlyPlaying) {
                    val xoff = 5
                    val yoff = 5 + 7 + (PLAYLIST_LINE_HEIGHT - 24) / 2
                    // it will set the colour on its own
                    drawFreqMeter(batch, x + xoff, y + yoff + PLAYLIST_LINE_HEIGHT * i * scale, alpha)
                }
                else {
                    val xoff = maskOffWidth + (if (i < 9) 3 else 0)
                    val yoff = 7 + (PLAYLIST_LINE_HEIGHT - 24) / 2
                    batch.color = Color(1f, 1f, 1f, alpha * 0.75f)
                    App.fontSmallNumbers.draw(
                        batch,
                        if (i + playlistScroll in playlist.indices) "${i + playlistScroll + 1}" else "",
                        x + xoff,
                        y + yoff + PLAYLIST_LINE_HEIGHT * i * scale
                    )
                }

                // print the name
                batch.color = Color(1f, 1f, 1f, alpha)
                batch.draw(it.colorBufferTexture, x + PLAYLIST_LEFT_GAP * scale, y + PLAYLIST_LINE_HEIGHT * i * scale, it.width * scale, it.height * scale)

                // separator
                batch.color = Color(1f, 1f, 1f, alpha * 0.25f)
                Toolkit.drawStraightLine(batch, x, y + PLAYLIST_LINE_HEIGHT * (i + 1) * scale, x + width * scale, 1f, false)
            }

            // print the album name
            batch.color = Color(1f, 1f, 1f, alpha * 0.75f)
            Toolkit.drawTextCentered(batch, App.fontGame, playlistName, width, x.roundToInt(), 3 + (y + PLAYLIST_LINE_HEIGHT * PLAYLIST_LINES * scale).roundToInt())
        }
    }

    private fun drawAlbumList(camera: OrthographicCamera, delta: Float, batch: SpriteBatch, x: Float, y: Float, alpha: Float, scale: Float) {
        if (alpha > 0f) {
            batch.color = Color(1f, 1f, 1f, alpha)
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
        // TODO fade away the << o >>, replacing with the page buttons

        if (alpha > 0f) {
            val alpha0 = alpha.coerceIn(0f, 1f).organicOvershoot().coerceAtMost(1f)
            val internalWidth =minOf(widthForMouseUp.toFloat(), width - 20f)
            val separation = internalWidth / 5f
            val anchorX = Toolkit.hdrawWidthf
            val posY = posY + 12f
            for (i in 0..4) {
                batch.color = Color(1f, 1f, 1f,
                            0.75f * (if (reverse) 1f - alpha0 else alpha0).pow(3f) + (playControlAnimAkku[i].pow(2f) * 1.2f)
                        )

                val offset = i - 2
                val posX = anchorX + offset * separation

                val iconY = if (!AudioMixer.musicTrack.isPlaying && i == 2) 1 else 0
                batch.draw(controlButtons.get(i, iconY), (posX - BUTTON_WIDTH / 2).roundToFloat(), posY.roundToFloat())

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
        val inbuf = AudioMixer.musicTrack.extortField<MixerTrackProcessor>("processor")!!.extortField<List<FloatArray>>("fout1")!!
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

        batch.color = colourMeter2 mul Color(1f, 1f, 1f, alpha)
        fftBarHeights.forEachIndexed { index, h ->
            Toolkit.fillArea(batch, posX + index*4f, posY - h, 3f, 2*h + 1)
        }

        batch.color = colourMeter mul Color(1f, 1f, 1f, alpha)
        fftBarHeights.forEachIndexed { index, h ->
            Toolkit.fillArea(batch, posX + index*4f, posY - h, 2f, 2*h)
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

    private fun getAlbumProp(albumDir: File): AlbumProp {
        if (!albumDir.exists()) throw IllegalArgumentException("Album dir does not exist: $albumDir")
        val playlistFile = File(albumDir, "playlist.json")
        if (playlistFile.exists()) {
            val playlistFile = JsonFetcher.invoke(playlistFile)
            val albumName = playlistFile.get("albumName")?.asString() ?: albumDir.name
            val diskJockeyingMode = playlistFile.get("diskJockeyingMode").asString()
            val shuffled = playlistFile.get("shuffled").asBoolean()
            val fileToName = playlistFile.get("titles")
            return AlbumProp(albumDir, albumName, diskJockeyingMode, shuffled, fileToName)
        }
        else {
            return AlbumProp(albumDir, albumDir.name, "intermittent", true, null)
        }
    }

    private data class AlbumProp(
        val ref: File, val name: String,
        val diskJockeyingMode: String, val shuffled: Boolean, val fileToName: JsonValue?,
        val albumArt: TextureRegion? = null
    )

    private fun loadNewAlbum(albumDir: File) {
        val albumProp = getAlbumProp(albumDir)

        AudioMixer.musicTrack.let { track ->
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