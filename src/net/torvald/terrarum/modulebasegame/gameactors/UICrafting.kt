package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.modulebasegame.ui.UIInventoryCells
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton

/**
 * This UI has inventory, but it's just there to display all craftable items and should not be serialised.
 *
 * Created by minjaesong on 2022-03-10.
 */
class UICrafting : UICanvas(
        toggleKeyLiteral = App.getConfigInt("control_key_inventory"),
        toggleButtonLiteral = App.getConfigInt("control_gamepad_start"),
), HasInventory {

    override var width = App.scr.width
    override var height = App.scr.height
    override var openCloseTime: Second = 0.0f

    private val catBar: UIItemInventoryCatBar
    private val itemListPlayer: UIItemInventoryItemGrid
    private val itemListCraftable: UIItemInventoryItemGrid
    private val buttonCraft: UIItemTextButton
    private val buttonCancel: UIItemTextButton

    private val negotiator = object : InventoryNegotiator() {
        override fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Long) {
            TODO()
        }

        override fun reject(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long) {
            TODO()
        }
    }

    override fun getNegotiator() = negotiator
    override fun getFixtureInventory(): FixtureInventory = TODO()
    override fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2



    init {
        catBar = UIItemInventoryCatBar(
                this,
                (App.scr.width - UIInventoryFull.catBarWidth) / 2,
                42 - UIInventoryFull.YPOS_CORRECTION + (App.scr.height - UIInventoryFull.internalHeight) / 2,
                UIInventoryFull.internalWidth,
                UIInventoryFull.catBarWidth,
                false
        )
        catBar.selectionChangeListener = { old, new -> itemListUpdate() }
        val craftableX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() - halfSlotOffset + (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 7
        val craftableY = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()
        val craftButtonsY = craftableY + (UIItemInventoryElemWide.height + listGap) * (UIInventoryFull.CELLS_VRT - 1)
        val gridWidth = (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 7
        val buttonWidth = (gridWidth - listGap) / 2
        itemListCraftable = UIItemInventoryItemGrid(
                this,
                catBar,
                { getFixtureInventory() },
                craftableX, craftableY,
                6, UIInventoryFull.CELLS_VRT - 1, // decrease the internal height so that craft/cancel button would fit in
                drawScrollOnRightside = false,
                drawWallet = false,
                keyDownFun = { _, _, _ -> Unit },
                touchDownFun = { gameItem, amount, _ ->
                    if (gameItem != null) {
                        negotiator.reject(getFixtureInventory(), getPlayerInventory(), gameItem, amount)
                    }
                    itemListUpdate()
                }
        )

        buttonCraft = UIItemTextButton(this, "MENU_LABEL_CRAFT", craftableX, craftButtonsY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
        buttonCancel = UIItemTextButton(this, "MENU_LABEL_CANCEL", craftableX + buttonWidth + listGap, craftButtonsY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)

        buttonCraft.touchDownListener = { _,_,_,_ ->
            TODO()
        }
        buttonCancel.touchDownListener = { _,_,_,_ ->
            TODO()
        }

        // make grid mode buttons work together
        itemListCraftable.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
        itemListCraftable.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }

        itemListPlayer = UIItemInventoryItemGrid(
                this,
                catBar,
                { INGAME.actorNowPlaying!!.inventory }, // literally a player's inventory
                UIInventoryFull.INVENTORY_CELLS_OFFSET_X() - halfSlotOffset,
                craftableY,
                6, UIInventoryFull.CELLS_VRT,
                drawScrollOnRightside = true,
                drawWallet = false,
                keyDownFun = { _, _, _ -> Unit },
                touchDownFun = { gameItem, amount, _ ->
                    if (gameItem != null) {
                        negotiator.accept(getPlayerInventory(), getFixtureInventory(), gameItem, amount)
                    }
                    itemListUpdate()
                }
        )
        itemListPlayer.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
        itemListPlayer.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }

        handler.allowESCtoClose = true

        addUIitem(catBar)
        addUIitem(itemListCraftable)
        addUIitem(itemListPlayer)
    }


    private var openingClickLatched = false

    override fun show() {
        itemListPlayer.getInventory = { INGAME.actorNowPlaying!!.inventory }

        itemListUpdate()

        openingClickLatched = Terrarum.mouseDown
    }

    private fun itemListUpdate() {
        itemListCraftable.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])
    }

    private fun setCompact(yes: Boolean) {
        itemListCraftable.isCompactMode = yes
        itemListCraftable.gridModeButtons[0].highlighted = !yes
        itemListCraftable.gridModeButtons[1].highlighted = yes
        itemListCraftable.itemPage = 0
        itemListCraftable.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

        itemListPlayer.isCompactMode = yes
        itemListPlayer.gridModeButtons[0].highlighted = !yes
        itemListPlayer.gridModeButtons[1].highlighted = yes
        itemListPlayer.itemPage = 0
        itemListPlayer.rebuild(catBar.catIconsMeaning[catBar.selectedIcon])

        itemListUpdate()
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!openingClickLatched) {
            return super.touchDown(screenX, screenY, pointer, button)
        }
        return false
    }

    override fun updateUI(delta: Float) {
        catBar.update(delta)
        itemListCraftable.update(delta)
        itemListPlayer.update(delta)

        if (openingClickLatched && !Terrarum.mouseDown) openingClickLatched = false
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // background fill
        UIInventoryFull.drawBackground(batch)

        // UI items
        batch.color = Color.WHITE

        catBar.render(batch, camera)
        itemListCraftable.render(batch, camera)
        itemListPlayer.render(batch, camera)


        blendNormal(batch)
    }

    override fun doOpening(delta: Float) {
        INGAME.pause()
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        INGAME.resume()
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
        INGAME.setTooltipMessage(null) // required!
    }


    override fun dispose() {
    }
}