package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
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
            arrayOf("", { Lang["MENU_OPTIONS_SOUND_VOLUME"] }, "h1"),
                arrayOf("mastervolume", { Lang["MENU_OPTIONS_MASTER_VOLUME"] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("bgmvolume", { Lang["MENU_LABEL_MUSIC"] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("ambientvolume", { Lang["MENU_LABEL_AMBIENT_SOUNDS"] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("sfxvolume", { Lang["CREDITS_SFX"] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
                arrayOf("guivolume", { Lang["MENU_LABEL_INTERFACE"] }, "sliderd,0,1"),
                arrayOf("", { "" }, "pp"),
            arrayOf("", { Lang["MENU_LABEL_AUDIO_ENGINE"] }, "h1"),
                arrayOf("audio_speaker_setup", { Lang["MENU_OPTIONS_SPEAKER_SETUP"] }, "textsel,headphone=MENU_OPTIONS_SPEAKER_HEADPHONE,stereo=MENU_OPTIONS_SPEAKER_STEREO"),
                arrayOf("audio_buffer_size", { Lang["MENU_OPTIONS_AUDIO_BUFFER_SIZE"] }, "spinnersel,128,256,512,1024,2048"),
                arrayOf("", { "${Lang["MENU_LABEL_AUDIO_BUFFER_INSTRUCTION"]}" }, "p"),

        ))
    }

    override var height = ControlPanelCommon.getMenuHeight("basegame.soundcontrolpanel")

    private var oldBufferSize = App.getConfigInt("audio_buffer_size")

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }

        App.getConfigInt("audio_buffer_size").let {
            if (it != oldBufferSize) {
                oldBufferSize = it

                App.reloadAudioProcessor(it)
            }
        }
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
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

}