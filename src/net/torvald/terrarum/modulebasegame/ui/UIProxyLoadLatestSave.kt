package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.Second
import net.torvald.terrarum.serialise.LoadSavegame
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2021-09-13.
 */
class UIProxyLoadLatestSave : UICanvas() {

    override var width: Int = 0
    override var height: Int = 0
    override var openCloseTime: Second = 0f

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
        if (App.savegames.size > 0) {
            LoadSavegame(App.savegames[0])
        }
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }
}