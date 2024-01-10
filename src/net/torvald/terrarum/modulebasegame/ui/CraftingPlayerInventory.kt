package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.ui.*

/**
 * Suite of objects for showing player inventory for various crafting UIs.
 *
 * Created by minjaesong on 2023-10-04.
 */
class CraftingPlayerInventory(val full: UIInventoryFull, val crafting: UICanvas) : UITemplate(crafting) {

    val itemList: UIItemInventoryItemGrid

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2
    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap - halfSlotOffset
    private val thisOffsetX2 = thisOffsetX + (UIItemInventoryItemGrid.listGap + UIItemInventoryElemWide.height) * 7
    private val thisOffsetY =  UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()

    internal var itemListKeyDownFun = { gameItem: GameItem?, amount: Long, keycode: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        /* crickets */
    }
    internal var itemListTouchDownFun = { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        /* crickets */
    }

    init {
        itemList = UIItemInventoryItemGrid(
            crafting,
            full.catBar,
            { INGAME.actorNowPlaying!!.inventory }, // literally a player's inventory
            thisOffsetX2,
            thisOffsetY,
            6, UIInventoryFull.CELLS_VRT,
            drawScrollOnRightside = true,
            drawWallet = false,
            highlightEquippedItem = false,
            keyDownFun = { a, b, c, d, e -> itemListKeyDownFun(a, b, c, d, e) },
            touchDownFun = {  a, b, c, d, e -> itemListTouchDownFun.invoke(a, b, c, d, e) }
        )
        // make grid mode buttons work together
//        itemListPlayer.gridModeButtons[0].clickOnceListener = { _,_ -> setCompact(false) }
//        itemListPlayer.gridModeButtons[1].clickOnceListener = { _,_ -> setCompact(true) }

    }

    inline fun rebuild(category: Array<String>) {
        itemList.rebuild(category)
    }

    inline fun rebuild(noinline predicate: (InventoryPair) -> Boolean) {
        itemList.rebuild(predicate)
    }

    inline fun rebuild(noinline predicate: (InventoryPair) -> Boolean, appendix: ItemID) {
        itemList.rebuild(predicate, appendix)
    }

    inline fun removeFromForceHighlightList(items: List<ItemID>) {
        itemList.removeFromForceHighlightList(items)
    }

    inline fun setGetInventoryFun(noinline getter: () -> ActorInventory) {
        itemList.getInventory = getter
    }

    override fun getUIitems(): List<UIItem> {
        return listOf(itemList)
    }
}