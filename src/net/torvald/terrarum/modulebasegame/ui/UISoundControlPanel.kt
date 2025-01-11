package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.terrarum.audio.dsp.Comp
import net.torvald.terrarum.audio.dsp.Lowpass
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2023-07-15.
 */
class UISoundControlPanel(remoCon: UIRemoCon?) : UICanvas() {

    override var width = 560

    init {
        handler.allowESCtoClose = false

        ControlPanelCommon.register(this, width, "basegame.soundcontrolpanel", arrayOf(
            arrayOf("", { Lang["MENU_OPTIONS_SOUND_VOLUME", true] }, "h1"),
                arrayOf("mastervolume", { Lang["MENU_OPTIONS_MASTER_VOLUME", true] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("bgmvolume", { Lang["MENU_LABEL_MUSIC", true] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("ambientvolume", { Lang["MENU_LABEL_AMBIENT_SOUNDS", true] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("sfxvolume", { Lang["CREDITS_SFX", true] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("guivolume", { Lang["MENU_LABEL_INTERFACE", true] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("audio_dsp_compressor_ratio", { Lang["MENU_OPTIONS_AUDIO_COMP", true] }, "textsel,none=MENU_OPTIONS_DISABLE,light=MENU_OPTIONS_LIGHT,heavy=MENU_OPTIONS_STRONG"),
            arrayOf("", { Lang["MENU_LABEL_AUDIO_ENGINE", true] }, "h1"),
                arrayOf("audio_speaker_setup", { Lang["MENU_OPTIONS_SPEAKER_SETUP", true] }, "textsel,headphone=MENU_OPTIONS_SPEAKER_HEADPHONE,stereo=MENU_OPTIONS_SPEAKER_STEREO"),
                arrayOf("audio_buffer_size", { Lang["MENU_OPTIONS_AUDIO_BUFFER_SIZE", true] }, "spinnersel,128,256,512,1024,2048"),
                arrayOf("", { "${Lang["MENU_LABEL_AUDIO_BUFFER_INSTRUCTION"]}" }, "p"),

        ))
    }

    private val compDict = mapOf(
        "none" to 1f,
        "light" to 1.8f,
        "heavy" to 5.0f
    )

    override var height = ControlPanelCommon.getMenuHeight("basegame.soundcontrolpanel")

    private var oldBufferSize = App.getConfigInt("audio_buffer_size")
    private var oldCompRatio = App.getConfigString("audio_dsp_compressor_ratio")

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }

        App.getConfigString("audio_dsp_compressor_ratio").let {
            if (it != oldCompRatio) {
                oldCompRatio = it

                App.audioMixer.masterTrack.getFilter<Comp>().ratio = compDict[it] ?: 1f
            }
        }

        App.getConfigInt("audio_buffer_size").let {
            if (it != oldBufferSize) {
                oldBufferSize = it

                App.reloadAudioProcessor(it)
            }
        }
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // undo sound fadeout/muting when this panel is opened
        if (handler.openCloseCounter == 0f && App.audioMixer.fadeBus.getFilter<Lowpass>().cutoff < SAMPLING_RATE / 2) {
            App.audioMixer.requestLowpassOut(0.25)
            App.audioMixer.requestFadeIn(App.audioMixer.fadeBus, 0.25, 1.0)
        }

        ControlPanelCommon.render("basegame.soundcontrolpanel", width, batch)
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun dispose() {
    }

    companion object {
        private val compDict = mapOf(
            "none" to 1f,
            "light" to 1.8f,
            "heavy" to 5.0f
        )

        fun setupCompRatioByCurrentConfig() {
            App.getConfigString("audio_dsp_compressor_ratio").let {
                App.audioMixer.masterTrack.getFilter<Comp>().ratio = compDict[it] ?: 1f
            }
        }
    }

}