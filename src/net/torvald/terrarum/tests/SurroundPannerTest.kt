
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.files.FileHandle
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSlider

/**
 * Created by minjaesong on 2018-05-18.
 */
class SurroundPannerTest : JFrame() {

    val mixerPanel = JPanel(BorderLayout()) // LR slider

    val mixerPanSlider = JSlider(JSlider.HORIZONTAL, -32768, 32767, 0)

    init {
        val sliderPanel = JPanel(); sliderPanel.add(mixerPanSlider)
        mixerPanel.add(sliderPanel, BorderLayout.CENTER)

        this.add(mixerPanel, BorderLayout.CENTER)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.isVisible = true
        this.setSize(400, 600)
    }

}

class AudioPlayerSlave : Game() {

    lateinit var audioSample: FileHandle
    lateinit var gdxSound: Sound
    var soundID = 0L
    lateinit var surroundPanner: SurroundPannerTest


    override fun create() {
        audioSample = Gdx.files.internal("assets/loopey.wav")
        gdxSound = Gdx.audio.newSound(audioSample)
        surroundPanner = SurroundPannerTest()
        soundID = gdxSound.loop()
    }

    override fun render() {
        gdxSound.setPan(soundID, surroundPanner.mixerPanSlider.value.toFloat() / 32768f, 0.5f)
    }

    override fun dispose() {
        super.dispose()
        gdxSound.dispose()
    }

}

fun main(args: Array<String>) {
    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = true
    appConfig.width = 256
    appConfig.height = 256
    appConfig.backgroundFPS = 20
    appConfig.foregroundFPS = 20

    LwjglApplication(AudioPlayerSlave(), appConfig)
}



/*
package net.torvald.terrarum.audio.surroundpanner

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.audio.SpatialAudioMixer
import java.awt.BorderLayout
import java.io.StringReader
import java.util.*
import javax.swing.*

/**
 * Created by minjaesong on 2018-05-18.
 */
class SurroundPannerTest(val panningMatrix: String) : JFrame() {

    val panningSettings = Properties()

    val mixerPanel = JPanel(BorderLayout()) // LR slider, options (pan-rear threshold: Double, panning law: Double)
    val matricesPanel = JPanel(BorderLayout()) // show and edit panning matrix


    val mixerPanSlider = JSlider(JSlider.HORIZONTAL, -32768, 32767, 0)
    val mixerPanRearSelector = JSpinner(SpinnerNumberModel(6667, 0, 10000, 1))
    val mixerPanLawSelector = JSpinner(SpinnerListModel(arrayOf(0.0, -3.0, -4.5, -6.0)))

    init {
        val sliderPanel = JPanel(); sliderPanel.add(mixerPanSlider)
        sliderPanel.isVisible = true
        val panRearPanel = JPanel(); panRearPanel.add(JLabel("Pan-rear threshold")); panRearPanel.add(mixerPanRearSelector)
        panRearPanel.isVisible = true
        val panLawPanel = JPanel(); panLawPanel.add(JLabel("Panning law")); panLawPanel.add(mixerPanLawSelector)
        panLawPanel.isVisible = true
        val optionsPanel = JPanel(); optionsPanel.add(panRearPanel); optionsPanel.add(panLawPanel)
        optionsPanel.isVisible = true
        mixerPanel.add(sliderPanel, BorderLayout.CENTER)
        mixerPanel.add(optionsPanel, BorderLayout.SOUTH)




        panningSettings.load(StringReader(panningMatrix))


        this.add(mixerPanel, BorderLayout.CENTER)
        this.add(matricesPanel, BorderLayout.SOUTH)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.isVisible = true
        this.setSize(400, 600)
    }

}

class AudioPlayerSlave : Game() {

    lateinit var audioSample: FileHandle
    lateinit var gdxSound: Sound
    var soundID = 0L
    lateinit var surroundPanner: SurroundPannerTest


    override fun create() {
        audioSample = Gdx.files.internal("assets/loopey.wav")
        gdxSound = Gdx.audio.newSound(audioSample)





        surroundPanner = SurroundPannerTest(SpatialAudioMixer.PRESET_QUADRAPHONIC)


        soundID = gdxSound.loop()
    }

    override fun render() {
        gdxSound.setPan(soundID, surroundPanner.mixerPanSlider.value.toFloat() / 32768f, 1f)
    }

    override fun dispose() {
        super.dispose()
        gdxSound.dispose()
    }

}

fun main(args: Array<String>) {
    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = true
    appConfig.width = 256
    appConfig.height = 256
    appConfig.backgroundFPS = 20
    appConfig.foregroundFPS = 20

    LwjglApplication(AudioPlayerSlave(), appConfig)
}
 */