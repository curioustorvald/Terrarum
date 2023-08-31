package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2023-07-15.
 */
class UISoundControlPanel(remoCon: UIRemoCon?) : UICanvas() {

    override var width = 560

    init {
        ControlPanelCommon.register(this, width, "basegame.soundcontrolpanel", arrayOf(
            arrayOf("mastervolume", { Lang["MENU_OPTIONS_SOUND_VOLUME"] }, "sliderd,0,1"),
            arrayOf("", { "" }, "p"),
            arrayOf("bgmvolume", { Lang["MENU_LABEL_BACKGROUND_MUSIC"] }, "sliderd,0,1"),
            arrayOf("", { "" }, "p"),
            arrayOf("musicvolume", { Lang["MENU_LABEL_MUSIC"] }, "sliderd,0,1"),
            arrayOf("", { "" }, "p"),
            arrayOf("sfxvolume", { Lang["CREDITS_SFX"] }, "sliderd,0,1"),

        ))
    }

    override var height = ControlPanelCommon.getMenuHeight("basegame.soundcontrolpanel")

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        ControlPanelCommon.render("basegame.soundcontrolpanel", width, batch)
        uiItems.forEach { it.render(batch, camera) }
    }

    override fun dispose() {
    }
}