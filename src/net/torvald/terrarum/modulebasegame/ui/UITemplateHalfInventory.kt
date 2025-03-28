package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.ui.*

/**
 * Suite of objects for showing player inventory for various UIs involving inventory management.
 *
 * @param parent the parent UI
 * @param drawOnLeft if the inventory should be drawn on the left panel
 * @param getInventoryFun function that returns an inventory. Default value is for player's
 *
 * Created by minjaesong on 2023-10-04.
 */
class UITemplateHalfInventory(
    parent: UICanvas,
    drawOnLeft: Boolean,
    getInventoryFun: () -> FixtureInventory = { INGAME.actorNowPlaying!!.inventory },
    val inventoryNameFun: () -> String = { INGAME.actorNowPlaying!!.actorValue.getAsString(AVKey.NAME).orEmpty().let { it.ifBlank { Lang["GAME_INVENTORY"] } } }
) : UITemplate(parent) {

    companion object {
        const val INVENTORY_NAME_TEXT_GAP = 28

        const val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2

        val XOFFSET_LEFT = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset
        val XOFFSET_RIGHT = XOFFSET_LEFT + (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 7
        val YOFFSET = UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()
    }

    val itemList: UIItemInventoryItemGrid

    internal var itemListKeyDownFun = { gameItem: GameItem?, amount: Long, keycode: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        /* crickets */
    }
    internal var itemListTouchDownFun = { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        /* crickets */
    }
    internal var itemListWheelFun = { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        /* crickets */
    }

    init {
        itemList = UIItemInventoryItemGrid(
            parent,
            getInventoryFun,
            if (drawOnLeft) XOFFSET_LEFT else XOFFSET_RIGHT,
            YOFFSET,
            6, UIInventoryFull.CELLS_VRT,
            drawScrollOnRightside = !drawOnLeft,
            drawWallet = false,
            highlightEquippedItem = false,
            keyDownFun = { a, b, c, d, e -> itemListKeyDownFun(a, b, c, d, e) },
            touchDownFun = {  a, b, c, d, e -> itemListTouchDownFun.invoke(a, b, c, d, e) },
            wheelFun = { a, b, c, d, e, f -> itemListWheelFun.invoke(a, b, c, d, e, f)}
        )
        // make grid mode buttons work together
//        itemListPlayer.gridModeButtons[0].clickOnceListener = { _,_ -> setCompact(false) }
//        itemListPlayer.gridModeButtons[1].clickOnceListener = { _,_ -> setCompact(true) }

    }


    fun rebuild(category: Array<String>) {
        itemList.rebuild(category)
    }

    fun rebuild(predicate: (InventoryPair) -> Boolean) {
        itemList.rebuild(predicate)
    }

    fun rebuild(predicate: (InventoryPair) -> Boolean, appendix: ItemID) {
        itemList.rebuild(predicate, appendix)
    }

    fun setGetInventoryFun(getter: () -> ActorInventory) {
        itemList.getInventory = getter
    }

    override fun update(delta: Float) = itemList.update(delta)
    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        itemList.render(frameDelta, batch, camera)

        batch.color = Color.WHITE
        Toolkit.drawTextCentered(batch, App.fontGame, inventoryNameFun(), width, posX, YOFFSET - INVENTORY_NAME_TEXT_GAP)
    }

    var posX: Int
        get() = itemList.posX
        set(value) { itemList.posX = value }
    var posY: Int
        get() = itemList.posY
        set(value) { itemList.posY = value }
    val width: Int
        get() = itemList.width
    val height: Int
        get() = itemList.height

    override fun getUIitems(): List<UIItem> {
        return listOf(itemList)
    }

    override fun dispose() {
    }
}