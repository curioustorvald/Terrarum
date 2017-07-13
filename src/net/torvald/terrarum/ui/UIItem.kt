package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum


/**
 * Created by minjaesong on 15-12-31.
 */
abstract class UIItem(var parentUI: UICanvas) { // do not replace parentUI to UIHandler!

    // X/Y Position relative to the containing canvas
    abstract var posX: Int
    abstract var posY: Int
    abstract val width: Int
    abstract val height: Int

    protected val relativeMouseX: Int
        get() = (Terrarum.mouseScreenX - (parentUI.handler?.posX ?: 0) - this.posX)
    protected val relativeMouseY: Int
        get() = (Terrarum.mouseScreenY - (parentUI.handler?.posY ?: 0) - this.posY)

    /** If mouse is hovering over it */
    open val mouseUp: Boolean
        get() = relativeMouseX in 0..width - 1 && relativeMouseY in 0..height - 1
    /** If mouse is hovering over it and mouse is down */
    open val mousePushed: Boolean
        get() = mouseUp && Gdx.input.isButtonPressed(Terrarum.getConfigInt("mouseprimary")!!)

    abstract fun update(delta: Float)
    abstract fun render(batch: SpriteBatch)

    // keyboard controlled
    abstract fun keyDown(keycode: Int): Boolean
    abstract fun keyUp(keycode: Int): Boolean

    // mouse controlled
    abstract fun mouseMoved(screenX: Int, screenY: Int): Boolean
    abstract fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean
    abstract fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean
    abstract fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean
    abstract fun scrolled(amount: Int): Boolean
    abstract fun dispose()

}
