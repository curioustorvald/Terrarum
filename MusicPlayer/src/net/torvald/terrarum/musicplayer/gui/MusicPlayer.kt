package net.torvald.terrarum.musicplayer.gui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.JsonValue
import com.jme3.math.FastMath
import net.torvald.reflection.extortField
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.*
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
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


    private val nameStrMaxLen = 180
    private val nameFBO = FrameBuffer(Pixmap.Format.RGBA8888, 1024, capsuleHeight, false)

    private val baloonTexture = ModMgr.getGdxFile("musicplayer", "gui/blob.tga").let {
        TextureRegionPack(it, capsuleMosaicSize, capsuleMosaicSize)
    }
    private val textmask = ModMgr.getGdxFile("musicplayer", "gui/textmask.tga").let {
        TextureRegionPack(it, maskOffWidth, capsuleHeight)
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

    private val TRANSITION_LENGTH = 0.6f

    private var colourEdge = Color(0xFFFFFF_40.toInt())
    private val colourBack = Color.BLACK

    private val colourText = Color(0xf0f0f0ff.toInt())
    private val colourMeter = Color(0xddddddff.toInt())
    private val colourMeter2 = Color(0xdddddd80.toInt())

    init {
        setAsAlwaysVisible()

        // test code
//        val albumDir = App.customMusicDir + "/Gapless Test"
        val albumDir = App.customMusicDir + "/FurryJoA 2023 Live"
        val playlistFile = JsonFetcher.invoke("$albumDir/playlist.json")

        val diskJockeyingMode = playlistFile.get("diskJockeyingMode").asString()
        val shuffled = playlistFile.get("shuffled").asBoolean()
        val fileToName = playlistFile.get("titles")

        registerPlaylist(albumDir, fileToName, shuffled, diskJockeyingMode)
    }

    fun registerPlaylist(path: String, fileToName: JsonValue, shuffled: Boolean, diskJockeyingMode: String) {
        ingame.musicGovernor.queueDirectory(path, shuffled, diskJockeyingMode) { filename ->
            fileToName.get(filename).let {
                if (it == null)
                    filename.substringBeforeLast('.').replace('_', ' ').split(" ").map { it.capitalize() }.joinToString(" ")
                else
                    it.asString()
            }
        }

        ingame.musicGovernor.addMusicStartHook { music ->
            setMusicName(music.name)
            transitionRequest = MODE_PLAYING
        }

        ingame.musicGovernor.addMusicStopHook { music ->
            if (diskJockeyingMode == "intermittent") {
                setIntermission()
                transitionRequest = MODE_IDLE
            }
        }
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

            musicPlayingTimer += delta
        }

        updateMeter()


        if (!transitionOngoing) {
            if (mouseUp) {
                transitionRequest = MODE_MOUSE_UP
            }
            else if (mode == MODE_MOUSE_UP) {
                transitionRequest = if (currentMusicName.isEmpty()) MODE_IDLE else MODE_PLAYING
            }
        }

//        printdbg(this, "mode = $mode; req = $transitionRequest")
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
        it[MODE_PLAYING to MODE_PLAYING] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, nameLength, akku / TRANSITION_LENGTH)
        }
        it[MODE_PLAYING to MODE_IDLE] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, nameLength, akku / TRANSITION_LENGTH)
        }

        it[MODE_IDLE to MODE_MOUSE_UP] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, (nameStrMaxLen + METERS_WIDTH + maskOffWidth).toInt(), akku / TRANSITION_LENGTH)
            setUIheight(28, 80, akku / TRANSITION_LENGTH)
        }
        it[MODE_PLAYING to MODE_MOUSE_UP] = { akku ->
            setUIwidthFromTextWidth(nameLengthOld, (nameStrMaxLen + METERS_WIDTH + maskOffWidth).toInt(), akku / TRANSITION_LENGTH)
            setUIheight(28, 80, akku / TRANSITION_LENGTH)
        }
        it[MODE_MOUSE_UP to MODE_PLAYING] = { akku ->
            setUIwidthFromTextWidth((nameStrMaxLen + METERS_WIDTH + maskOffWidth).toInt(), nameLength, akku / TRANSITION_LENGTH)
            setUIheight(80, 28, akku / TRANSITION_LENGTH)
        }
        it[MODE_MOUSE_UP to MODE_IDLE] = { akku ->
            setUIwidthFromTextWidth((nameStrMaxLen + METERS_WIDTH + maskOffWidth).toInt(), 0, akku / TRANSITION_LENGTH)
            setUIheight(80, 28, akku / TRANSITION_LENGTH)
        }

        it[MODE_MOUSE_UP to MODE_MOUSE_UP] = { akku -> }


    }

    private var _posX = 0f // not using provided `posX` as there is one frame delay between update and it actually used to drawing
    private var _posY = 0f

    private var _posXnonStretched = 0f // posXY on PLAYING status

    override val mouseUp: Boolean
        get() = relativeMouseX.toFloat() in _posX-capsuleMosaicSize .. _posX+width+capsuleMosaicSize &&
                relativeMouseY.toFloat() in _posY .. _posY+height

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        batch.end()
        renderNameToFBO(batch, camera, currentMusicName)
        batch.begin()


        _posX = ((Toolkit.drawWidth - width) / 2).toFloat()
        _posY = (App.scr.height - App.scr.tvSafeGraphicsHeight - height).toFloat()
        _posXnonStretched = ((Toolkit.drawWidth - uiWidthFromTextWidth(nameLength)) / 2).toFloat()


        val posXforMusicLine = if (transitionOngoing && modeNext >= MODE_MOUSE_UP || mode >= MODE_MOUSE_UP)
            _posXnonStretched
        else
            _posX

        val widthForFreqMeter = if (transitionOngoing && modeNext >= MODE_MOUSE_UP || mode >= MODE_MOUSE_UP)
            uiWidthFromTextWidth(nameLength)
        else
            width

        blendNormalStraightAlpha(batch)
        drawBaloon(batch, _posX, _posY, width.toFloat(), (height - capsuleHeight.toFloat()).coerceAtLeast(0f))
        drawText(batch, posXforMusicLine, _posY)
        drawFreqMeter(batch, posXforMusicLine + widthForFreqMeter - 18f, _posY + height - (capsuleHeight / 2) + 1f)

        batch.color = Color.WHITE
    }

    private fun drawBaloon(batch: SpriteBatch, x: Float, y: Float, width: Float, height: Float) {
        val x = x - capsuleMosaicSize
        for (k in 0..3 step 3) {
            batch.color = if (k == 0) colourEdge else colourBack// (if (mouseUp) Color.MAROON else colourBack)

            // top left
            batch.draw(baloonTexture.get(k+0, 0), x, y, capsuleMosaicSize.toFloat(), capsuleMosaicSize.toFloat())
            // top
            batch.draw(baloonTexture.get(k+1, 0), x + capsuleMosaicSize, y, width, capsuleMosaicSize.toFloat())
            // top right
            batch.draw(baloonTexture.get(k+2, 0), x + capsuleMosaicSize + width, y, capsuleMosaicSize.toFloat(), capsuleMosaicSize.toFloat())

            // left
            batch.draw(baloonTexture.get(k+0, 1), x, y + capsuleMosaicSize, capsuleMosaicSize.toFloat(), height)
            // centre
            batch.draw(baloonTexture.get(k+1, 1), x + capsuleMosaicSize, y + capsuleMosaicSize, width, height)
            // right
            batch.draw(baloonTexture.get(k+2, 1), x + capsuleMosaicSize + width, y + capsuleMosaicSize, capsuleMosaicSize.toFloat(), height)

            // bottom left
            batch.draw(baloonTexture.get(k+0, 2), x, y + capsuleMosaicSize + height, capsuleMosaicSize.toFloat(), capsuleMosaicSize.toFloat())
            // bottom
            batch.draw(baloonTexture.get(k+1, 2), x + capsuleMosaicSize, y + capsuleMosaicSize + height, width, capsuleMosaicSize.toFloat())
            // bottom right
            batch.draw(baloonTexture.get(k+2, 2), x + capsuleMosaicSize + width, y + capsuleMosaicSize + height, capsuleMosaicSize.toFloat(), capsuleMosaicSize.toFloat())
        }
    }

    private fun drawText(batch: SpriteBatch, posX: Float, posY: Float) {
        batch.color = colourText
        batch.draw(nameFBO.colorBufferTexture, posX - maskOffWidth, posY + height - capsuleHeight + 1)
    }

    private fun renderNameToFBO(batch: SpriteBatch, camera: OrthographicCamera, str: String) {
        val windowEnd = width.toFloat() - METERS_WIDTH - maskOffWidth

        nameFBO.inAction(camera, batch) {
            batch.inUse {
                batch.color = Color.WHITE
                // draw text
                gdxClearAndEnableBlend(0f, 0f, 0f, 0f)
                blendNormalStraightAlpha(batch)
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
    private val lowlim = -36.0f

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
    }

    private fun drawFreqMeter(batch: SpriteBatch, posX: Float, posY: Float) {
        // apply slope to the fft bins, also converts fullscale to decibels
        for (bin in binHeights.indices) {
            val freqR = (TerrarumAudioMixerTrack.SAMPLING_RATED / FFTSIZE) * (bin + 1)
            val magn0 = fftOut.reim[2 * bin].absoluteValue / FFTSIZE * (freqR / 10.0) // apply slope
            val magn = FastMath.interpolateLinear(FFT_SMOOTHING_FACTOR, magn0, oldFFTmagn[bin])
            val magnLog = fullscaleToDecibels(magn)

            val h = (-(magnLog - lowlim) / lowlim * STRIP_W).toFloat().coerceAtLeast(0.5f)

            binHeights[bin] = h

            oldFFTmagn[bin] = magn
        }

        fftBinIndices.mapIndexed { i, range ->
            fftBarHeights[i] = binHeights.slice(range).average().toFloat()
        }

        batch.color = colourMeter2
        fftBarHeights.forEachIndexed { index, h ->
            Toolkit.fillArea(batch, posX + index*4f, posY - h, 3f, 2*h + 1)
        }

        batch.color = colourMeter
        fftBarHeights.forEachIndexed { index, h ->
            Toolkit.fillArea(batch, posX + index*4f, posY - h, 2f, 2*h)
        }
    }

    override fun dispose() {
        baloonTexture.dispose()
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
    private val curveDataX = doubleArrayOf(0.0, 0.15576171875, 0.26171875, 0.40625, 0.59765625, 0.76220703125, 1.0)
    private val curveDataY = doubleArrayOf(0.0, 0.05322265625, 0.19140625, 0.59375, 0.94921875, 1.02880859375, 1.0)
    private val splineFunction = generateCubicSpline(curveDataX, curveDataY)

    fun organicOvershoot(x: Double): Double {
        return splineFunction.value(x)
    }
}