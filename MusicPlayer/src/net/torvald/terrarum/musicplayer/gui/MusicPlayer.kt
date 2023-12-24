package net.torvald.terrarum.musicplayer.gui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.jme3.math.FastMath
import net.torvald.reflection.extortField
import net.torvald.terrarum.*
import net.torvald.terrarum.audio.*
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
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
    private val nameFBO = FrameBuffer(Pixmap.Format.RGBA8888, nameStrMaxLen + 2*maskOffWidth, capsuleHeight, false)

    private val baloonTexture = ModMgr.getGdxFile("musicplayer", "gui/blob.tga").let {
        TextureRegionPack(it, capsuleMosaicSize, capsuleMosaicSize)
    }
    private val textmask = ModMgr.getGdxFile("musicplayer", "gui/textmask.tga").let {
        TextureRegionPack(it, maskOffWidth, capsuleHeight)
    }

    private val MODE_IDLE = 0
    private val MODE_NOW_PLAYING = 1
    private val MODE_PLAYING = 2
    private val MODE_MOUSE_UP = 64
    private val MODE_SHOW_LIST = 128

    private var mode = MODE_IDLE
    private var modeNext = MODE_IDLE
    private var transitionAkku = 0f
    private var transitionRequest: Int? = null

    private var TRANSITION_LENGTH = 0.44444445f

    private var textScroll = 0f

    private var colourEdge = Color(0xFFFFFF_40.toInt())
    private val colourBack = Color.BLACK

    private val colourText = Color(0xf0f0f0ff.toInt())
    private val colourMeter = Color(0xddddddff.toInt())
    private val colourMeter2 = Color(0xdddddd80.toInt())

    init {
        setAsAlwaysVisible()

        ingame.musicGovernor.addMusicStartHook { music ->
            setMusicName(music.name)
            transitionRequest = MODE_NOW_PLAYING
        }
        ingame.musicGovernor.addMusicStopHook { music ->
            setIntermission()
            transitionRequest = MODE_IDLE
        }
    }

    private var renderFBOreq: String? = ""
    private var renderFBOhistory: String? = ""
    private var nameLength = 0
    private var nameOverflown = false

    private fun setIntermission() {
        renderFBOreq = ""
        nameOverflown = false
    }

    private fun setMusicName(str: String) {
        renderFBOreq = str
        nameLength = App.fontGameFBO.getWidth(str)
        TRANSITION_LENGTH = 0.6666667f * (nameLength.coerceAtMost(nameStrMaxLen).toFloat() / nameStrMaxLen)
        nameOverflown = (nameLength > nameStrMaxLen)
    }

    override fun updateUI(delta: Float) {
        // process transition request
        if (transitionRequest != null) {
            modeNext = transitionRequest!!
            transitionAkku = 0f
            transitionRequest = null
        }

        // actually do transition
        if (mode != modeNext) {
            makeTransition()

            if (renderFBOhistory?.isNotBlank() == true) {
                renderFBOreq = renderFBOhistory // continuously call the renderNameToFBO
            }

            transitionAkku += delta

            if (transitionAkku > TRANSITION_LENGTH) {
                mode = modeNext
            }
        }

        updateMeter()
    }

    private fun smoothstep(x: Float) = (x*x*(3f-2f*x)).coerceIn(0f, 1f)
    private fun smootherstep(x: Float) = (x*x*x*(x*(6f*x-15f)+10f)).coerceIn(0f, 1f)

    private fun setUIwidthFromTextWidth(textW: Int, percentage: Float) {
        val zeroWidth = METERS_WIDTH
        val maxWidth = (textW + METERS_WIDTH + maskOffWidth).roundToInt().toFloat()
        val step = smootherstep(percentage)
        width = FastMath.interpolateLinear(step, zeroWidth, maxWidth).roundToInt()
    }

    // changes ui width
    private fun makeTransition() {
        transitionDB[mode to modeNext]?.invoke(transitionAkku)
    }

    private val transitionDB = HashMap<Pair<Int, Int>, (Float) -> Unit>().also {
        it[MODE_IDLE to MODE_NOW_PLAYING] = { akku ->
            setUIwidthFromTextWidth(nameLength, akku / TRANSITION_LENGTH)
        }
        it[MODE_NOW_PLAYING to MODE_IDLE] = { akku ->
            setUIwidthFromTextWidth(nameLength, (1f - (akku / TRANSITION_LENGTH).coerceAtMost(1f)))
        }
    }


    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        if (renderFBOreq != null) {
            batch.end()
            renderFBOhistory = renderFBOreq?.substring(0)
            renderNameToFBO(batch, camera, renderFBOreq!!, 0f..width.toFloat() - METERS_WIDTH.toInt() - maskOffWidth)
            batch.begin()
            renderFBOreq = null
        }

        val posX = ((Toolkit.drawWidth - width) / 2).toFloat()
        val posY = (App.scr.height - App.scr.tvSafeGraphicsHeight - height).toFloat()

        blendNormalStraightAlpha(batch)
        drawBaloon(batch, posX, posY, width.toFloat(), height - capsuleHeight.toFloat())
        drawText(batch, posX, posY)
        drawFreqMeter(batch, posX + width - 18, posY + height - (capsuleHeight / 2) + 1)

        batch.color = Color.WHITE
    }

    private fun drawBaloon(batch: SpriteBatch, x: Float, y: Float, width: Float, height: Float) {
        val x = x - capsuleMosaicSize
        for (k in 0..3 step 3) {
            batch.color = if (k == 0) colourEdge else colourBack

            // top left
            batch.draw(baloonTexture.get(k, 0), x, y)
            // top
            batch.draw(baloonTexture.get(k+1, 0), x + capsuleMosaicSize, y, width, capsuleMosaicSize.toFloat())
            // top right
            batch.draw(baloonTexture.get(k+2, 0), x + capsuleMosaicSize + width, y)
            if (height > 0) {
                // left
                batch.draw(baloonTexture.get(k, 1), x, y + capsuleMosaicSize)
                // centre
                batch.draw(baloonTexture.get(k+1, 1), x + capsuleMosaicSize, y + capsuleMosaicSize, width, height)
                // right
                batch.draw(baloonTexture.get(k+2, 1), x + capsuleMosaicSize + width, y + capsuleMosaicSize)
            }
            // bottom left
            batch.draw(baloonTexture.get(k, 2), x, y + capsuleMosaicSize + height)
            // bottom
            batch.draw(baloonTexture.get(k+1, 2), x + capsuleMosaicSize, y + capsuleMosaicSize + height, width, capsuleMosaicSize.toFloat())
            // bottom right
            batch.draw(baloonTexture.get(k+2, 2), x + capsuleMosaicSize + width, y + capsuleMosaicSize + height)
        }
    }

    private fun drawText(batch: SpriteBatch, posX: Float, posY: Float) {
        batch.color = colourText
        batch.draw(nameFBO.colorBufferTexture, posX - maskOffWidth, posY - 1)
    }

    private fun renderNameToFBO(batch: SpriteBatch, camera: OrthographicCamera, str: String, window: ClosedFloatingPointRange<Float>) {
        nameFBO.inAction(camera, batch) {
            batch.inUse {
                batch.color = Color.WHITE
                // draw text
                gdxClearAndEnableBlend(0f, 0f, 0f, 0f)
                blendNormalStraightAlpha(batch)
                App.fontGameFBO.draw(batch, str, maskOffWidth.toFloat() - textScroll, 0f)

                // mask off the area
                batch.color = Color.WHITE
                blendAlphaMask(batch)
                batch.draw(textmask.get(0, 0), window.start, 0f)
                batch.draw(textmask.get(1, 0), window.start + maskOffWidth, 0f, window.endInclusive - window.start, capsuleHeight.toFloat())
                batch.draw(textmask.get(2, 0), window.start + window.endInclusive + maskOffWidth, 0f)
                batch.draw(textmask.get(3, 0), window.start + window.endInclusive + 2 * maskOffWidth, 0f, 1000f, capsuleHeight.toFloat())
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
}