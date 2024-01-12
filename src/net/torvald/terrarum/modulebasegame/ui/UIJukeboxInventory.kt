package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameitems.ItemFileRef
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemInventoryElemSimple
import net.torvald.terrarum.ui.UIItemInventoryElemWide

/**
 * Created by minjaesong on 2024-01-13.
 */
class UIJukeboxInventory(val parent: UICanvas) : UICanvas(), HasInventory {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val negotiator = object : InventoryTransactionNegotiator() {
        override fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Long) {
            if (item is ItemFileRef && item.mediumIdentifier == "music_disc") {
                player.remove(item, amount)
                fixture.add(item, amount)
            }
        }

        override fun refund(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long) {
            fixture.remove(item, amount)
            player.add(item, amount)
        }

    }

    private val thisInventory = FixtureInventory()

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2
    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset
    private val thisOffsetY =  UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()

    private val fixtureInventoryCells = (0..7).map {
        UIItemInventoryElemWide(this,
            thisOffsetX, thisOffsetY + (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) * it,
            6 * UIItemInventoryElemSimple.height + 5 * UIItemInventoryItemGrid.listGap,
            keyDownFun = { _, _, _, _, _ -> Unit },
            touchDownFun = { gameItem, amount, button, _, _ ->
                if (button == App.getConfigInt("config_mouseprimary")) {
                    if (gameItem != null) {
                        negotiator.refund(getFixtureInventory(), getPlayerInventory(), gameItem, amount)
                    }
                }
            }
        )
    }
    private val playerInventoryUI = UITemplateHalfInventory(this, false)

    init {
        fixtureInventoryCells.forEach {
            addUIitem(it)
        }
        addUIitem(playerInventoryUI)
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun dispose() {
    }

    override fun getNegotiator() = negotiator

    override fun getFixtureInventory(): FixtureInventory {
        TODO()
    }

    override fun getPlayerInventory(): FixtureInventory {
        TODO()
    }


}



class UIJukeboxSonglistPanel(val parent: UICanvas) : UICanvas() {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height



    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }


    override fun dispose() {
    }

}

