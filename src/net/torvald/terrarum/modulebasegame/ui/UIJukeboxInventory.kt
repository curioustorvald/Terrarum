package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.modulebasegame.gameitems.ItemFileRef
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.defaultInventoryCellTheme
import net.torvald.terrarum.modulebasegame.ui.UIJukebox.Companion.SLOT_SIZE
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UIItemInventoryElemWide

/**
 * Created by minjaesong on 2024-01-13.
 */
class UIJukeboxInventory(val parent: UIJukebox) : UICanvas() {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2
    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset
    private val thisOffsetY =  UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()

//    private var currentFreeSlot: Int

    private val playerInventory: ActorInventory
        get() = INGAME.actorNowPlaying!!.inventory

    private val fixtureDiscCell: Array<UIItemInventoryElemWide> = (0 until SLOT_SIZE).map { index ->
        UIItemInventoryElemWide(this,
            thisOffsetX, thisOffsetY + (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) * index,
            6 * UIItemInventoryElemSimple.height + 5 * UIItemInventoryItemGrid.listGap,
            keyDownFun = { _, _, _, _, _ -> Unit },
            touchDownFun = { _, _, _, _, _ -> Unit },
        )
    }.toTypedArray()

    private val operatedByTheInstaller: Boolean
        get() = true/*if (parent.parent.actorThatInstalledThisFixture == null) true
                else if (parent.parent.actorThatInstalledThisFixture != null && INGAME.actorNowPlaying is IngamePlayer)
                    (parent.parent.actorThatInstalledThisFixture == (INGAME.actorNowPlaying as IngamePlayer).uuid)
                else false*/

    init {
        fixtureDiscCell.forEachIndexed { index, thisButton ->
            thisButton.touchDownFun = { gameItem, amount, mouseButton, _, _ ->
                if (operatedByTheInstaller && mouseButton == App.getConfigInt("config_mouseprimary")) {
                    if (gameItem != null) {
                        // if the disc being removed is the same disc being played, stop the playback
                        if (index == parent.parent.discCurrentlyPlaying) {
                            parent.parent.stopGracefully()
                        }

                        // remove the disc
                        fixtureDiscCell[index].item = null
                        parent.discInventory.removeAt(index)
                        playerInventory.add(gameItem)


                        rebuild()
                    }
                }
            }

            addUIitem(thisButton)
        }
    }

    private val playerInventoryUI = UITemplateHalfInventory(this, false).also {
        it.itemListTouchDownFun = { gameItem, _, _, _, _ ->

            if (operatedByTheInstaller && parent.discInventory.size < SLOT_SIZE && gameItem != null) {
                parent.discInventory.add(gameItem.dynamicID)
                playerInventory.remove(gameItem)

                rebuild()
            }
        }

        addUIitem(it)
    }


    private val playerInventoryFilterFun = { (itm, _): InventoryPair ->
        ItemCodex[itm].let {
            it is ItemFileRef && it.mediumIdentifier == "music_disc"
        }
    }

    override fun show() {
        super.show()
        rebuild()
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun dispose() {
    }

    private fun rebuild() {
        playerInventoryUI.rebuild(playerInventoryFilterFun)

        for (index in 0 until SLOT_SIZE) {
            if (index in parent.discInventory.indices) {
                val itemID = parent.discInventory[index]
                val gameItem = ItemCodex[itemID]
                fixtureDiscCell[index].item = gameItem
                fixtureDiscCell[index].itemImage = gameItem?.itemImage
            }
            else {
                fixtureDiscCell[index].item = null
                fixtureDiscCell[index].itemImage = null
            }
        }
    }

}



class UIJukeboxSonglistPanel(val parent: UIJukebox) : UICanvas() {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2

    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset
    private val thisOffsetX2 = thisOffsetX + (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 7

    private val songButtonColourTheme = defaultInventoryCellTheme.copy(
        cellHighlightNormalCol = Color(0xfec753c8.toInt()),
        cellBackgroundCol = Color(0x704c20c8.toInt())
    )

    private val rows = SLOT_SIZE / 2
    private val vgap = 48
    private val internalHeight = (UIItemJukeboxSonglist.height + vgap) * (rows - 1)

    private val ys = (0 until rows).map {
        App.scr.halfh - (internalHeight / 2) + (UIItemJukeboxSonglist.height + vgap) * it
    }


    private val jukeboxPlayButtons = (0 until SLOT_SIZE).map { index ->
        UIItemJukeboxSonglist(this,
            if (index % 2 == 0) thisOffsetX else thisOffsetX2,
            ys[index.shr(1)],
            6 * UIItemInventoryElemSimple.height + 5 * UIItemInventoryItemGrid.listGap,
            index = index,
            colourTheme = songButtonColourTheme,
            keyDownFun = { _, _, _ -> Unit },
            touchDownFun = { index, button, _ ->
                if (button == App.getConfigInt("config_mouseprimary") && !parent.parent.musicIsPlaying) {
                    parent.parent.playDisc(index)
                }
            }
        )
    }.toTypedArray()

    fun rebuild() {

        jukeboxPlayButtons.forEachIndexed { index, button ->
            if (index in parent.discInventory.indices) {
                parent.discInventory[index].let {
                    val item = ItemCodex[it] as? ItemFileRef
                    button.title = item?.name ?: ""
                    button.artist = item?.author ?: ""
                }
            }
            else {
                button.title = ""
                button.artist = ""
            }
        }
    }

    override fun show() {
        super.show()
        rebuild()
    }

    init {
        jukeboxPlayButtons.forEach {
            addUIitem(it)
        }
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }


    override fun dispose() {
    }

}


class UIItemJukeboxSonglist(
    parentUI: UICanvas,
    initialX: Int,
    initialY: Int,
    override val width: Int,
    val index: Int,
    var title: String = "",
    var artist: String = "",

    var keyDownFun: (Int, Int, UIItemJukeboxSonglist) -> Unit, // Index, Keycode, self
    var touchDownFun: (Int, Int, UIItemJukeboxSonglist) -> Unit, // Index, Button,  self

    var colourTheme: InventoryCellColourTheme = UIItemInventoryCellCommonRes.defaultInventoryCellTheme
) : UIItem(parentUI, initialX, initialY) {

    companion object {
        val height = 48
    }

    override val height = Companion.height

    private val textOffsetX = 6
    private val textOffsetY = 1


    /** Custom highlight rule to highlight tihs button to primary accent colour (blue by default).
     * Set to `null` to use default rule:
     *
     * "`equippedSlot` defined and set to `highlightEquippedItem`" or "`forceHighlighted`" */
    var customHighlightRuleMain: ((UIItemJukeboxSonglist) -> Boolean)? = null
    /** Custom highlight rule to highlight this button to secondary accent colour (yellow by default). Set to `null` to use default rule (which does nothing). */
    var customHighlightRule2: ((UIItemJukeboxSonglist) -> Boolean)? = null


    override fun keyDown(keycode: Int): Boolean {
        keyDownFun(index, keycode, this)
        super.keyDown(keycode)
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (mouseUp) {
            touchDownFun(index, button, this)
            super.touchDown(screenX, screenY, pointer, button)
        }
        return true
    }


    private var highlightToMainCol = false
    private var highlightToSubCol = false

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)

        val isPlaying = (index == (parentUI as UIJukeboxSonglistPanel).parent.parent.discCurrentlyPlaying)
        highlightToMainCol = customHighlightRuleMain?.invoke(this) ?: false || isPlaying
        highlightToSubCol = customHighlightRule2?.invoke(this) ?: false

        // cell background
        batch.color = colourTheme.cellBackgroundCol
        Toolkit.fillArea(batch, posX, posY, width, height)

        // cell border
        batch.color = if (highlightToMainCol) colourTheme.cellHighlightMainCol
        else if (highlightToSubCol) colourTheme.cellHighlightSubCol
        else if (mouseUp && title.isNotEmpty()) colourTheme.cellHighlightMouseUpCol
        else colourTheme.cellHighlightNormalCol
        Toolkit.drawBoxBorder(batch, posX, posY, width, height)


        if (title.isNotEmpty()) {
            blendNormalStraightAlpha(batch)


            // if mouse is over, text lights up
            // highlight item name and count (blocks/walls) if the item is equipped
            batch.color = if (highlightToMainCol) colourTheme.textHighlightMainCol
                else if (highlightToSubCol) colourTheme.textHighlightSubCol
                else if (mouseUp && title.isNotEmpty()) colourTheme.textHighlightMouseUpCol
                else colourTheme.textHighlightNormalCol

            // draw title
            Toolkit.drawTextCentered(batch, App.fontGame, title, width, posX, posY + textOffsetY)
            // draw artist
            batch.color = batch.color.cpy().mul(0.75f, 0.75f, 0.75f, 1f)
            Toolkit.drawTextCentered(batch, App.fontGame, artist, width, posX, posY + App.fontGame.lineHeight.toInt() - 2*textOffsetY)
        }

        // see IFs above?
        batch.color = Color.WHITE

    }

    override fun dispose() {
    }
}