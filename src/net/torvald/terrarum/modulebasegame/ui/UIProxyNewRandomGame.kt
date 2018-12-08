package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.random.HQRNG
import net.torvald.terrarum.LoadScreen
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2018-12-08.
 */
class UIProxyNewRandomGame : UICanvas() {

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
        TODO("not implemented")
    }

    override fun endOpening(delta: Float) {
        val ingame = Ingame(Terrarum.batch)
        ingame.gameLoadInfoPayload = Ingame.NewWorldParameters(2400, 800, HQRNG().nextLong())
        //ingame.gameLoadInfoPayload = Ingame.NewWorldParameters(8192, 2048, 0x51621DL)
        ingame.gameLoadMode = Ingame.GameLoadMode.CREATE_NEW

        Terrarum.ingame = ingame
        LoadScreen.screenToLoad = ingame
        Terrarum.setScreen(LoadScreen)
    }

    override fun endClosing(delta: Float) {
        TODO("not implemented")
    }

    override fun dispose() {
        TODO("not implemented")
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }

    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun keyTyped(character: Char): Boolean {
        return super.keyTyped(character)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }
}