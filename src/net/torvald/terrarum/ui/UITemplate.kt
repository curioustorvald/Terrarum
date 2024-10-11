package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.TooltipListener
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent

/**
 * Created by minjaesong on 2024-01-10.
 */
abstract class UITemplate(val parent: UICanvas) : UIItemisable() {

    abstract fun getUIitems(): List<UIItem>

    override fun show() { getUIitems().forEach { it.show() } }
    override fun hide() { getUIitems().forEach { it.hide() } }
    override fun inputStrobed(e: TerrarumKeyboardEvent) { getUIitems().forEach { it.inputStrobed(e) } }
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean { getUIitems().forEach { it.touchDragged(screenX, screenY, pointer) }; return true }
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean { getUIitems().forEach { it.touchDown(screenX, screenY, pointer, button) }; return true }
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean { getUIitems().forEach { it.touchUp(screenX, screenY, pointer, button) }; return true }
    override fun scrolled(amountX: Float, amountY: Float): Boolean { getUIitems().forEach { it.scrolled(amountX, amountY) }; return true }
    override fun keyDown(keycode: Int): Boolean { getUIitems().forEach { it.keyDown(keycode) }; return true }
    override fun keyUp(keycode: Int): Boolean { getUIitems().forEach { it.keyUp(keycode) }; return true }
    override fun keyTyped(char: Char): Boolean { getUIitems().forEach { it.keyTyped(char) }; return true }

}

/**
 * Created by minjaesong on 2024-01-29.
 */
abstract class UIItemisable : TooltipListener(), Disposable {
    abstract fun update(delta: Float)
    abstract fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera)
    open fun show() {}
    open fun hide() {}
    open fun inputStrobed(e: TerrarumKeyboardEvent) {}
    open fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
    open fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
    open fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
    open fun scrolled(amountX: Float, amountY: Float): Boolean = false
    open fun keyDown(keycode: Int): Boolean = false
    open fun keyUp(keycode: Int): Boolean = false
    open fun keyTyped(char: Char): Boolean = false
}