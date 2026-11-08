package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ControlHint
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitems.FixtureInteractionBlocked
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.ui.Movement
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UINotControllable
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.unicode.getKeycapPC
import net.torvald.unicode.getMouseButton
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2026-02-10.
 */
@UINotControllable
class UIWorldControlHint : UICanvas() {

    init {
        handler.allowESCtoClose = false
        handler.setAsAlwaysVisible()
    }

    private fun getHintText(control: ControlHint): String {
        return if (control.primary != null && control.secondary != null)
            "${getMouseButton(App.getConfigInt("control_mouse_primary"))} ${Lang[control.primary!!]}\u3000" +
                    "${getMouseButton(App.getConfigInt("control_mouse_secondary"))} ${Lang[control.secondary!!]}"
        else if (control.primary != null)
            "${getMouseButton(App.getConfigInt("control_mouse_primary"))} ${Lang[control.primary!!]}\u3000"
        else if (control.secondary != null)
            "${getMouseButton(App.getConfigInt("control_mouse_secondary"))} ${Lang[control.secondary!!]}"
        else
            ""
    }

    // the most generic control hints for the fixtures
    private val genericFixtureHint = ControlHint("GAME_INVENTORY_USE", "GAME_ACTION_PICK_UP")


    private var cachedText = ""

    override var width = 480
    override var height = App.fontGame.lineHeight.toInt()

    override var openCloseTime = 0.2f

    override val mouseUp = false

    private var opacityCounter = 0f

    /**
     * @return the control hint of the fixture under the mouse, or null if there is none (or the mouse is out of reach)
     */
    private fun getFixtureControlHint(): ControlHint? {
        val actor = INGAME.actorNowPlaying ?: INGAME.actorGamer
        var hint: ControlHint? = null
        mouseInInteractableRange(actor) { mwx, mwy, mtx, mty ->
            val fixturesUnderMouse = (INGAME as TerrarumIngame).getActorsUnderMouse(mwx, mwy).filterIsInstance<FixtureBase>()
            val itemOnGrip = ItemCodex[(actor as? Pocketed)?.inventory?.itemEquipped?.get(GameItem.EquipPosition.HAND_GRIP)]

            // fixture-specific hint; follows TerrarumIngame.worldPrimaryClickStart in that the item is used instead when it's FixtureInteractionBlocked
            if (itemOnGrip !is FixtureInteractionBlocked)
                hint = fixturesUnderMouse.firstOrNull()?.getControlHint()

            if (hint == null && fixturesUnderMouse.any {
                it.canBeDespawned && System.nanoTime() - it.spawnRequestedTime > 50000000 // give freshly spawned fixtures 0.05 seconds of immunity
            })
                hint = genericFixtureHint

            0L
        }
        return hint
    }

    override fun updateImpl(delta: Float) {
        // always build the text from the hint to accomodate a language change
        val newText = getHintText(getFixtureControlHint() ?: INGAME.getCurrentControlHint())

        if (newText.isNotEmpty()) {
            cachedText = newText
            opacityCounter = (opacityCounter + delta).coerceAtMost(openCloseTime)
        }
        else {
            opacityCounter = (opacityCounter - delta).coerceAtLeast(0f)
            if (opacityCounter <= 0f) cachedText = ""
        }

        handler.opacity = opacityCounter / openCloseTime
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = Color.WHITE

        val text = cachedText

        val offX = Toolkit.drawWidthf - (App.scr.tvSafeGraphicsWidth * 1.25f).roundToInt().toFloat() - App.fontGame.getWidth(text)
        val offY = App.scr.height - height - App.scr.tvSafeGraphicsHeight - 4f

        App.fontGame.draw(batch, text, offX, offY)
    }

    override fun dispose() {
    }

    // overridden to not touch the tooltips
    override fun doOpening(delta: Float) {
        handler.opacity = handler.openCloseCounter / openCloseTime
    }

    // overridden to not touch the tooltips
    override fun doClosing(delta: Float) {
        handler.opacity = handler.openCloseCounter / openCloseTime
    }

    // overridden to not touch the tooltips
    override fun endOpening(delta: Float) {
        handler.opacity = 1f
    }

    // overridden to not touch the tooltips
    override fun endClosing(delta: Float) {
        handler.opacity = 0f
    }

}