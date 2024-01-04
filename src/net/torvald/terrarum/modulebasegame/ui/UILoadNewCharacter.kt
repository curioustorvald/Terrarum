package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2023-07-05.
 */
class UILoadNewCharacter(val full: UILoadSavegame) : UICanvas() {
    init {
        handler.allowESCtoClose = false
    }

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    override fun updateUI(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

}