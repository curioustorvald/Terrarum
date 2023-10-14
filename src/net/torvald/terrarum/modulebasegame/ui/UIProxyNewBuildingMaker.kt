package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.SanicLoadScreen
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.BuildingMaker
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2018-12-08.
 */
class UIProxyNewBuildingMaker(val remoCon: UIRemoCon) : UICanvas() {

    override var width: Int = 0
    override var height: Int = 0
    override var openCloseTime: Second = 0f

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
        val ingame = BuildingMaker(App.batch)

        Terrarum.setCurrentIngameInstance(ingame)
        SanicLoadScreen.screenToLoad = ingame
        App.setLoadScreen(SanicLoadScreen)
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }
}