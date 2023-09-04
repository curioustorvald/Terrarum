package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import kotlin.math.roundToInt

open class UIItemTransitionContainer(
    private val parent: UICanvas,
    initialX: Int,
    initialY: Int,
    override val width: Int,
    override val height: Int,
    val transitionLength: Float = 0.15f,
    var currentPosition: Float = 0f,
    open val uis: List<UICanvas>
) : UIItem(parent, initialX, initialY) {

    val debugvals = false

    private var transitionRequested = false
    private var transitionOngoing = false
    private var transitionReqSource = currentPosition
    private var transitionReqTarget = currentPosition
    private var transitionTimer = 0f

    private val epsilon = 0.001f

    private fun timeToUpdate(index: Int) = (currentPosition > index - 1 + epsilon && currentPosition < index + 1 - epsilon)

    fun requestTransition(target: Int) {
        if (!transitionOngoing) {
            transitionRequested = true
            transitionReqSource = Math.round(currentPosition).toFloat()
            transitionReqTarget = target.toFloat()
        }
    }

    fun forcePosition(target: Int) {
        transitionOngoing = false
        transitionRequested = false
        transitionTimer = 0f
        currentPosition = target.toFloat()
        onTransition(currentPosition, uis)
    }

    override fun update(delta: Float) {
        super.update(delta)
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.update(delta) }
    }

    open fun onTransition(currentPosition: Float, uis: List<UICanvas>) {}

    open val currentUI: UICanvas
        get() = uis[currentPosition.roundToInt()]

    override fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        super.render(batch, camera)

        if (transitionRequested && !transitionOngoing) {
            transitionRequested = false
            transitionOngoing = true
            transitionTimer = 0f
        }

        if (transitionOngoing) {
            transitionTimer += Gdx.graphics.deltaTime

            currentPosition = Movement.moveLinear(transitionReqSource, transitionReqTarget, transitionTimer, transitionLength)

            if (transitionTimer > transitionLength) {
                transitionOngoing = false
                currentPosition = transitionReqTarget
            }

            onTransition(currentPosition, uis)
        }

        uis.forEachIndexed { index, ui ->
            if (currentPosition > index - 1 + epsilon && currentPosition < index + 1 - epsilon) {
                if (!ui.isOpened && !ui.isOpening) ui.setAsOpen()
                ui.render(batch, camera, parent.opacity)

                if (debugvals) {
                    App.fontSmallNumbers.draw(batch, "$index", 300f + (20 * index), 10f)
                }
            }
            else {
                if (!ui.isClosed && !ui.isClosing) ui.setAsClose()
            }
        }

        if (debugvals) {
            batch.color = Color.WHITE
            App.fontSmallNumbers.draw(batch, "position:$currentPosition", 500f, 10f)
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        currentUI.keyDown(keycode)
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        currentUI.keyUp(keycode)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        currentUI.touchDragged(screenX, screenY, pointer)
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.touchDown(screenX, screenY, pointer, button) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.touchUp(screenX, screenY, pointer, button) }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.scrolled(amountX, amountY) }
        return true
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        currentUI.inputStrobed(e)
    }

    override fun show() {
        uis.forEach { it.show() }
    }

    override fun hide() {
        uis.forEach { it.hide() }
    }

    override fun dispose() {
        uis.forEach { it.dispose() }
    }
}