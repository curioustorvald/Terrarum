package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
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
    getInventoryFun: () -> FixtureInventory = { INGAME.actorNowPlaying!!.inventory }
) : UITemplate(parent) {

    val itemList: UIItemInventoryItemGrid

    private val halfSlotOffset = (UIItemInventoryElemSimple.height + UIItemInventoryItemGrid.listGap) / 2
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
            parent,
            getInventoryFun,
            if (drawOnLeft) thisOffsetX else thisOffsetX2,
            thisOffsetY,
            6, UIInventoryFull.CELLS_VRT,
            drawScrollOnRightside = !drawOnLeft,
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

    inline fun update(delta: Float) = itemList.update(delta)
    inline fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) = itemList.render(frameDelta, batch, camera)

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
}