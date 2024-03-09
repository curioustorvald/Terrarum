package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.App
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.modulebasegame.gameactors.SmelterItemStatus
import net.torvald.terrarum.ui.UIItemInventoryElemSimple
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by minjaesong on 2024-03-09.
 */
object SmelterCommon {

    fun getPlayerSlotTouchDownFun(
        clickedOnState: AtomicInteger,

        fireboxItem: SmelterItemStatus,
        oreItem: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val playerInventory = getPlayerInventory()
        val amount = if (mouseButton == App.getConfigInt("config_mouseprimary"))
            amount
        else if (mouseButton == App.getConfigInt("config_mousesecondary"))
            1
        else
            null

        // oreslot
        if (amount != null && gameItem != null) {
            if (clickedOnState.get() == 1) {
                if (oreItem.itm == gameItem.dynamicID) {
                    playerInventory.remove(gameItem.dynamicID, amount)
                    oreItem.changeCount(amount)
                }
                else {
                    playerInventory.remove(gameItem.dynamicID, amount)
                    oreItem.set(gameItem.dynamicID, amount)
                }
            }
            // firebox
            else if (clickedOnState.get() == 2) {
                if (fireboxItem.isNull()) {
                    playerInventory.remove(gameItem.dynamicID, amount)
                    fireboxItem.set(gameItem.dynamicID, amount)
                }
                else if (fireboxItem.itm == gameItem.dynamicID) {
                    playerInventory.remove(gameItem.dynamicID, amount)
                    fireboxItem.changeCount(amount)
                }
            }
        }

        itemListUpdateKeepCurrentFilter()
    } }


    fun getPlayerSlotWheelFun(
        clickedOnState: AtomicInteger,

        fireboxItem: SmelterItemStatus,
        oreItem: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit
    ): (GameItem?, Long, Float, Float, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val playerInventory = getPlayerInventory()
        val scrollY = -scrollY
        if (gameItem != null) {
            val addCount1 = scrollY.toLong()

            if (clickedOnState.get() == 1 && (oreItem.isNull() || oreItem.itm == gameItem.dynamicID)) {
                val itemToUse = oreItem.itm ?: gameItem.dynamicID

                val addCount2 = scrollY.toLong().coerceIn(
                    -(playerInventory.searchByID(itemToUse)?.qty ?: 0L),
                    oreItem.qty ?: 0L,
                )

                // add to the inventory slot
                if (oreItem.isNotNull() && addCount1 >= 1L) {
                    playerInventory.add(oreItem.itm!!, addCount2)
                    oreItem.changeCount(-addCount2)
                }
                // remove from the inventory slot
                else if (addCount1 <= -1L) {
                    playerInventory.remove(itemToUse, -addCount2)
                    if (oreItem.isNull())
                        oreItem.set(itemToUse, -addCount2)
                    else
                        oreItem.changeCount(-addCount2)
                }
                if (oreItem.qty == 0L) oreItem.nullify()
                else if (oreItem.isNotNull() && oreItem.qty!! < 0L) throw Error("Item removal count is larger than what was on the slot")
                itemListUpdateKeepCurrentFilter()
            }
            else if (clickedOnState.get() == 2 && (fireboxItem.isNull() || fireboxItem.itm == gameItem.dynamicID)) {
                val itemToUse = fireboxItem.itm ?: gameItem.dynamicID

                val addCount2 = scrollY.toLong().coerceIn(
                    -(playerInventory.searchByID(itemToUse)?.qty ?: 0L),
                    fireboxItem.qty ?: 0L,
                )

                // add to the inventory slot
                if (fireboxItem.isNotNull() && addCount1 >= 1L) {
                    playerInventory.add(fireboxItem.itm!!, addCount2)
                    fireboxItem.changeCount(-addCount2)
                }
                // remove from the inventory slot
                else if (addCount1 <= -1L) {
                    playerInventory.remove(itemToUse, -addCount2)
                    if (fireboxItem.isNull())
                        fireboxItem.set(itemToUse, -addCount2)
                    else
                        fireboxItem.changeCount(-addCount2)
                }
                itemListUpdateKeepCurrentFilter()
            }
            else {
                itemListUpdateKeepCurrentFilter()
            }
        }
    } }





    fun getOreItemSlotTouchDownFun(
        clickedOnState: AtomicInteger,

        getFireboxItemSlot: () -> UIItemInventoryElemSimple,

        playerThings: UITemplateHalfInventory,

        oreItem: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdate: ((InventoryPair) -> Boolean) -> Unit,
        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        if (clickedOnState.get() != 1) {
            clickedOnState.set(1)
            theButton.forceHighlighted = true
            getFireboxItemSlot().forceHighlighted = false
            playerThings.itemList.itemPage = 0
            itemListUpdate { ItemCodex.hasTag(it.itm, "SMELTABLE") }
        }
        else if (oreItem.isNotNull()) {
            val removeCount = if (mouseButton == App.getConfigInt("config_mouseprimary"))
                oreItem.qty
            else if (mouseButton == App.getConfigInt("config_mousesecondary"))
                1L
            else
                null

            if (removeCount != null) {
                getPlayerInventory().add(oreItem.itm!!, removeCount)
                oreItem.changeCount(-removeCount)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }

    fun getOreItemSlotWheelFun(
        clickedOnState: AtomicInteger,

        oreItem: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Float, Float, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val playerInventory = getPlayerInventory()
        val scrollY = -scrollY
        if (clickedOnState.get() == 1 && oreItem.isNotNull()) {
            val removeCount1 = scrollY.toLong()
            val removeCount2 = scrollY.toLong().coerceIn(
                -oreItem.qty!!,
                playerInventory.searchByID(oreItem.itm)?.qty ?: 0L,
            )

            // add to the slot
            if (removeCount1 >= 1L) {
                playerInventory.remove(oreItem.itm!!, removeCount2)
                oreItem.changeCount(removeCount2)
            }
            // remove from the slot
            else if (removeCount1 <= -1L) {
                playerInventory.add(oreItem.itm!!, -removeCount2)
                oreItem.changeCount(removeCount2)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }



    fun getFireboxItemSlotTouchDownFun(
        clickedOnState: AtomicInteger,

        getOreItemSlot: () -> UIItemInventoryElemSimple,

        playerThings: UITemplateHalfInventory,

        fireboxItem: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdate: ((InventoryPair) -> Boolean) -> Unit,
        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        if (clickedOnState.get() != 2) {
            clickedOnState.set(2)
            theButton.forceHighlighted = true
            getOreItemSlot().forceHighlighted = false
            playerThings.itemList.itemPage = 0
            itemListUpdate { ItemCodex.hasTag(it.itm, "COMBUSTIBLE") }
        }
        else if (fireboxItem.isNotNull()) {
            val removeCount = if (mouseButton == App.getConfigInt("config_mouseprimary"))
                fireboxItem.qty
            else if (mouseButton == App.getConfigInt("config_mousesecondary"))
                1L
            else
                null

            if (removeCount != null) {
                getPlayerInventory().add(fireboxItem.itm!!, removeCount)
                fireboxItem.changeCount(-removeCount)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }

    fun getFireboxItemSlotWheelFun(
        clickedOnState: AtomicInteger,

        fireboxItem: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Float, Float, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val playerInventory = getPlayerInventory()
        val scrollY = -scrollY
        if (clickedOnState.get() == 2 && fireboxItem.isNotNull()) {
            val removeCount1 = scrollY.toLong()
            val removeCount2 = scrollY.toLong().coerceIn(
                -fireboxItem.qty!!,
                playerInventory.searchByID(fireboxItem.itm)?.qty ?: 0L,
            )

            // add to the slot
            if (removeCount1 >= 1L) {
                playerInventory.remove(fireboxItem.itm!!, removeCount2)
                fireboxItem.changeCount(removeCount2)
            }
            // remove from the slot
            else if (removeCount1 <= -1L) {
                playerInventory.add(fireboxItem.itm!!, -removeCount2)
                fireboxItem.changeCount(removeCount2)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }



    fun getProductItemSlotTouchDownFun(
        clickedOnState: AtomicInteger,

        getOreItemSlot: () -> UIItemInventoryElemSimple,
        getFireboxItemSlot: () -> UIItemInventoryElemSimple,

        playerThings: UITemplateHalfInventory,

        productItem: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdate: () -> Unit,
        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        if (clickedOnState.get() != 0) {
            clickedOnState.set(0)
            getOreItemSlot().forceHighlighted = false
            getFireboxItemSlot().forceHighlighted = false
            playerThings.itemList.itemPage = 0
            itemListUpdate()
        }

        if (productItem.isNotNull()) {
            val removeCount = if (mouseButton == App.getConfigInt("config_mouseprimary"))
                productItem.qty
            else if (mouseButton == App.getConfigInt("config_mousesecondary"))
                1L
            else
                null

            if (removeCount != null) {
                getPlayerInventory().add(productItem.itm!!, removeCount)
                productItem.changeCount(-removeCount)
            }
            itemListUpdateKeepCurrentFilter()
        }
    } }

    fun getProductItemSlotWheelFun(
        productItem: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Float, Float, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val scrollY = -scrollY
        if (productItem.isNotNull()) {
            val removeCount1 = scrollY.toLong()
            val removeCount2 = scrollY.toLong().coerceIn(
                -productItem.qty!!,
                0L,
            )

            // remove from the slot
            if (removeCount1 <= -1L) {
                getPlayerInventory().add(productItem.itm!!, -removeCount2)
                productItem.changeCount(removeCount2)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }
}