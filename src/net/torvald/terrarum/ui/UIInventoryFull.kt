package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.RunningEnvironment
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.UIItemInventoryCatBar
import net.torvald.terrarum.gameactors.InventoryPair
import net.torvald.terrarum.gameactors.Pocketed
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.langpack.Lang
import java.util.ArrayList

/**
 * Created by minjaesong on 2017-10-21.
 */
class UIInventoryFull(
        var actor: Pocketed?,

        toggleKeyLiteral: Int? = null, toggleButtonLiteral: Int? = null,
        // UI positions itself? (you must g.flush() yourself after the g.translate(Int, Int))
        customPositioning: Boolean = false, // mainly used by vital meter
        doNotWarnConstant: Boolean = false
) : UICanvas(toggleKeyLiteral, toggleButtonLiteral, customPositioning, doNotWarnConstant) {

    override var width: Int = Terrarum.WIDTH
    override var height: Int = Terrarum.HEIGHT

    val internalWidth: Int = 630
    val internalHeight: Int = 558 // grad_begin..grad_end..contents..grad_begin..grad_end



    private val SP = "${0x3000.toChar()}${0x3000.toChar()}"
    val listControlHelp: String
        get() = if (Terrarum.environment == RunningEnvironment.PC)
            "${0xe037.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}..${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${0xe034.toChar()} ${Lang["GAME_INVENTORY_DROP"]}"
        else
            "${0xe069.toChar()} ${Lang["GAME_ACTION_CLOSE"]}$SP" +
            "${Terrarum.joypadLabelNinY} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}${0xe010.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${Terrarum.joypadLabelNinA} ${Lang["GAME_INVENTORY_DROP"]}"
    val controlHelpHeight = Terrarum.fontGame.lineHeight.toInt()

    private var encumbrancePerc = 0f
    private var isEncumbered = false


    val catBarWidth = 328
    val catBar = UIItemInventoryCatBar(
            this,
            (Terrarum.WIDTH - catBarWidth) / 2,
            66 + (Terrarum.HEIGHT - internalHeight) / 2,
            catBarWidth
    )
    val catSelection: Int
        get() = catBar.selectedIndex
    val catSelectedIcon: Int
        get() = catBar.selectedIcon

    override var openCloseTime: Second = 1f


    private val itemList: UIItemInventoryDynamicList? =
            if (actor != null) {
                UIItemInventoryDynamicList(
                        this,
                        actor!!.inventory,
                        0 + (Terrarum.WIDTH - internalWidth) / 2,
                        109 + (Terrarum.HEIGHT - internalHeight) / 2
                )
            }
    else null


    init {
        addItem(catBar)
        itemList?.let {
            addItem(it)
        }

    }


    override fun updateUI(delta: Float) {

    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        catBar.render(batch, camera)
        itemList?.render(batch, camera)
    }



    fun rebuildList() {
        itemList?.rebuild()
    }

    override fun dispose() {
        catBar.dispose()
        itemList?.dispose()
    }



    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }



    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }



    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun keyTyped(character: Char): Boolean {
        return super.keyTyped(character)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }


}