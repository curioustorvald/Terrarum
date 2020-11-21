package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader

open class UIItemTransitionContainer(
        parent: UICanvas,
        initialX: Int,
        initialY: Int,
        override val width: Int,
        override val height: Int,
        val transitionLength: Float = 0.15f,
        var currentPosition: Float = 0f,
        val uis: Array<out UICanvas>
) : UIItem(parent, initialX, initialY) {

    val debugvals = false

    private var transitionRequested = false
    private var transitionOngoing = false
    private var transitionReqSource = currentPosition
    private var transitionReqTarget = currentPosition
    private var transitionTimer = 0f

    private val epsilon = 0.001f

    private fun timeToUpdate(index: Int) = true//(currentPosition > index - 1 + epsilon && currentPosition < index + 1 - epsilon)

    fun requestTransition(target: Int) {
        if (!transitionOngoing) {
            transitionRequested = true
            transitionReqSource = Math.round(currentPosition).toFloat()
            transitionReqTarget = target.toFloat()
        }
    }

    override fun update(delta: Float) {
        super.update(delta)
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.update(delta) }
    }

    open fun onTransition(currentPosition: Float, uis: Array<out UICanvas>) {}

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)

        if (transitionRequested && !transitionOngoing) {
            transitionRequested = false
            transitionOngoing = true
            transitionTimer = 0f
        }

        if (transitionOngoing) {
            transitionTimer += Gdx.graphics.rawDeltaTime

            currentPosition = UIUtils.moveLinear(transitionReqSource, transitionReqTarget, transitionTimer, transitionLength)

            if (transitionTimer > transitionLength) {
                transitionOngoing = false
                currentPosition = transitionReqTarget
            }

            onTransition(currentPosition, uis)
        }

        uis.forEachIndexed { index, ui ->
            if (currentPosition > index - 1 + epsilon && currentPosition < index + 1 - epsilon) {
                ui.setAsOpen()
                ui.render(batch, camera)

                if (debugvals) {
                    AppLoader.fontSmallNumbers.draw(batch, "$index", 300f + (20 * index), 10f)
                }
            }
            else {
                ui.setAsClose()
            }
        }

        if (debugvals) {
            batch.color = Color.WHITE
            AppLoader.fontSmallNumbers.draw(batch, "position:$currentPosition", 500f, 10f)
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.keyDown(keycode) }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.keyUp(keycode) }
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.mouseMoved(screenX, screenY) }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.touchDragged(screenX, screenY, pointer) }
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

    override fun scrolled(amount: Int): Boolean {
        uis.forEachIndexed { index, ui -> if (timeToUpdate(index)) ui.scrolled(amount) }
        return true
    }

    override fun dispose() {
        uis.forEach { it.dispose() }
    }
}