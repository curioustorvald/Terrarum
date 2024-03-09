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
object SmelterGuiEventBuilder {

    const val SLOT_INDEX_STRIDE = 16
    const val PRODUCT_SLOT = 0
    const val ORE_SLOT_FIRST = SLOT_INDEX_STRIDE
    const val FIRE_SLOT_FIRST = 2*SLOT_INDEX_STRIDE

    
    
    
    fun getPlayerSlotTouchDownFun(
        clickedOnState: AtomicInteger,

        fireboxItemStatus: SmelterItemStatus,
        oreItemStatus: List<SmelterItemStatus>,

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
            val clicked = clickedOnState.get()

            if (clicked in ORE_SLOT_FIRST until ORE_SLOT_FIRST + 16) {
                val oreItemStatus = oreItemStatus[clicked - ORE_SLOT_FIRST]
                if (oreItemStatus.itm == gameItem.dynamicID) {
                    playerInventory.remove(gameItem.dynamicID, amount)
                    oreItemStatus.changeCount(amount)
                }
                else {
                    playerInventory.remove(gameItem.dynamicID, amount)
                    oreItemStatus.set(gameItem.dynamicID, amount)
                }
            }
            // firebox
            else if (clicked == FIRE_SLOT_FIRST) {
                if (fireboxItemStatus.isNull()) {
                    playerInventory.remove(gameItem.dynamicID, amount)
                    fireboxItemStatus.set(gameItem.dynamicID, amount)
                }
                else if (fireboxItemStatus.itm == gameItem.dynamicID) {
                    playerInventory.remove(gameItem.dynamicID, amount)
                    fireboxItemStatus.changeCount(amount)
                }
            }
        }

        itemListUpdateKeepCurrentFilter()
    } }


    fun getPlayerSlotWheelFun(
        clickedOnState: AtomicInteger,

        fireboxItemStatus: SmelterItemStatus,
        oreItemStatus: List<SmelterItemStatus>,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit
    ): (GameItem?, Long, Float, Float, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val playerInventory = getPlayerInventory()
        val scrollY = -scrollY
        if (gameItem != null) {
            val addCount1 = scrollY.toLong()
            val clicked = clickedOnState.get()
            if (clicked in ORE_SLOT_FIRST until ORE_SLOT_FIRST + 16) {
                val oreItemStatus = oreItemStatus[clicked - ORE_SLOT_FIRST]
                if ((oreItemStatus.isNull() || oreItemStatus.itm == gameItem.dynamicID)) {
                    val itemToUse = oreItemStatus.itm ?: gameItem.dynamicID

                    val addCount2 = scrollY.toLong().coerceIn(
                        -(playerInventory.searchByID(itemToUse)?.qty ?: 0L),
                        oreItemStatus.qty ?: 0L,
                    )

                    // add to the inventory slot
                    if (oreItemStatus.isNotNull() && addCount1 >= 1L) {
                        playerInventory.add(oreItemStatus.itm!!, addCount2)
                        oreItemStatus.changeCount(-addCount2)
                    }
                    // remove from the inventory slot
                    else if (addCount1 <= -1L) {
                        playerInventory.remove(itemToUse, -addCount2)
                        if (oreItemStatus.isNull())
                            oreItemStatus.set(itemToUse, -addCount2)
                        else
                            oreItemStatus.changeCount(-addCount2)
                    }
                    if (oreItemStatus.qty == 0L) oreItemStatus.nullify()
                    else if (oreItemStatus.isNotNull() && oreItemStatus.qty!! < 0L) throw Error("Item removal count is larger than what was on the slot")
                    itemListUpdateKeepCurrentFilter()
                }
            }
            else if (clicked == FIRE_SLOT_FIRST && (fireboxItemStatus.isNull() || fireboxItemStatus.itm == gameItem.dynamicID)) {
                val itemToUse = fireboxItemStatus.itm ?: gameItem.dynamicID

                val addCount2 = scrollY.toLong().coerceIn(
                    -(playerInventory.searchByID(itemToUse)?.qty ?: 0L),
                    fireboxItemStatus.qty ?: 0L,
                )

                // add to the inventory slot
                if (fireboxItemStatus.isNotNull() && addCount1 >= 1L) {
                    playerInventory.add(fireboxItemStatus.itm!!, addCount2)
                    fireboxItemStatus.changeCount(-addCount2)
                }
                // remove from the inventory slot
                else if (addCount1 <= -1L) {
                    playerInventory.remove(itemToUse, -addCount2)
                    if (fireboxItemStatus.isNull())
                        fireboxItemStatus.set(itemToUse, -addCount2)
                    else
                        fireboxItemStatus.changeCount(-addCount2)
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

        buttonsToUnhighlight: () -> List<UIItemInventoryElemSimple>,

        playerThings: UITemplateHalfInventory,

        oreItemStatus: SmelterItemStatus, oreSlotIndex: Int,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdate: ((InventoryPair) -> Boolean) -> Unit,
        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        if (clickedOnState.get() != ORE_SLOT_FIRST + oreSlotIndex) {
            clickedOnState.set(ORE_SLOT_FIRST + oreSlotIndex)
            theButton.forceHighlighted = true
            buttonsToUnhighlight().forEach { it.forceHighlighted = false }
            playerThings.itemList.itemPage = 0
            itemListUpdate { ItemCodex.hasTag(it.itm, "SMELTABLE") }
        }
        else if (oreItemStatus.isNotNull()) {
            val removeCount = if (mouseButton == App.getConfigInt("config_mouseprimary"))
                oreItemStatus.qty
            else if (mouseButton == App.getConfigInt("config_mousesecondary"))
                1L
            else
                null

            if (removeCount != null) {
                getPlayerInventory().add(oreItemStatus.itm!!, removeCount)
                oreItemStatus.changeCount(-removeCount)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }

    fun getOreItemSlotWheelFun(
        clickedOnState: AtomicInteger,

        oreItemStatus: SmelterItemStatus, oreSlotIndex: Int,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Float, Float, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val playerInventory = getPlayerInventory()
        val scrollY = -scrollY
        if (clickedOnState.get() == ORE_SLOT_FIRST + oreSlotIndex && oreItemStatus.isNotNull()) {
            val removeCount1 = scrollY.toLong()
            val removeCount2 = scrollY.toLong().coerceIn(
                -oreItemStatus.qty!!,
                playerInventory.searchByID(oreItemStatus.itm)?.qty ?: 0L,
            )

            // add to the slot
            if (removeCount1 >= 1L) {
                playerInventory.remove(oreItemStatus.itm!!, removeCount2)
                oreItemStatus.changeCount(removeCount2)
            }
            // remove from the slot
            else if (removeCount1 <= -1L) {
                playerInventory.add(oreItemStatus.itm!!, -removeCount2)
                oreItemStatus.changeCount(removeCount2)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }



    fun getFireboxItemSlotTouchDownFun(
        clickedOnState: AtomicInteger,

        buttonsToUnhighlight: () -> List<UIItemInventoryElemSimple>,

        playerThings: UITemplateHalfInventory,

        fireboxItemStatus: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdate: ((InventoryPair) -> Boolean) -> Unit,
        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        if (clickedOnState.get() != FIRE_SLOT_FIRST) {
            clickedOnState.set(FIRE_SLOT_FIRST)
            theButton.forceHighlighted = true
            buttonsToUnhighlight().forEach { it.forceHighlighted = false }
            playerThings.itemList.itemPage = 0
            itemListUpdate { ItemCodex.hasTag(it.itm, "COMBUSTIBLE") }
        }
        else if (fireboxItemStatus.isNotNull()) {
            val removeCount = if (mouseButton == App.getConfigInt("config_mouseprimary"))
                fireboxItemStatus.qty
            else if (mouseButton == App.getConfigInt("config_mousesecondary"))
                1L
            else
                null

            if (removeCount != null) {
                getPlayerInventory().add(fireboxItemStatus.itm!!, removeCount)
                fireboxItemStatus.changeCount(-removeCount)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }

    fun getFireboxItemSlotWheelFun(
        clickedOnState: AtomicInteger,

        fireboxItemStatus: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Float, Float, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val playerInventory = getPlayerInventory()
        val scrollY = -scrollY
        if (clickedOnState.get() == FIRE_SLOT_FIRST && fireboxItemStatus.isNotNull()) {
            val removeCount1 = scrollY.toLong()
            val removeCount2 = scrollY.toLong().coerceIn(
                -fireboxItemStatus.qty!!,
                playerInventory.searchByID(fireboxItemStatus.itm)?.qty ?: 0L,
            )

            // add to the slot
            if (removeCount1 >= 1L) {
                playerInventory.remove(fireboxItemStatus.itm!!, removeCount2)
                fireboxItemStatus.changeCount(removeCount2)
            }
            // remove from the slot
            else if (removeCount1 <= -1L) {
                playerInventory.add(fireboxItemStatus.itm!!, -removeCount2)
                fireboxItemStatus.changeCount(removeCount2)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }



    fun getProductItemSlotTouchDownFun(
        clickedOnState: AtomicInteger,

        buttonsToUnhighlight: () -> List<UIItemInventoryElemSimple>,

        playerThings: UITemplateHalfInventory,

        productItemStatus: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdate: () -> Unit,
        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, mouseButton: Int, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        if (clickedOnState.get() != PRODUCT_SLOT) {
            clickedOnState.set(PRODUCT_SLOT)
            buttonsToUnhighlight().forEach { it.forceHighlighted = false }
            playerThings.itemList.itemPage = 0
            itemListUpdate()
        }

        if (productItemStatus.isNotNull()) {
            val removeCount = if (mouseButton == App.getConfigInt("config_mouseprimary"))
                productItemStatus.qty
            else if (mouseButton == App.getConfigInt("config_mousesecondary"))
                1L
            else
                null

            if (removeCount != null) {
                getPlayerInventory().add(productItemStatus.itm!!, removeCount)
                productItemStatus.changeCount(-removeCount)
            }
            itemListUpdateKeepCurrentFilter()
        }
    } }

    fun getProductItemSlotWheelFun(
        productItemStatus: SmelterItemStatus,

        getPlayerInventory: () -> ActorInventory,

        itemListUpdateKeepCurrentFilter: () -> Unit

    ): (GameItem?, Long, Float, Float, Any?, UIItemInventoryCellBase) -> Unit { return { gameItem: GameItem?, amount: Long, scrollX: Float, scrollY: Float, itemExtraInfo: Any?, theButton: UIItemInventoryCellBase ->
        val scrollY = -scrollY
        if (productItemStatus.isNotNull()) {
            val removeCount1 = scrollY.toLong()
            val removeCount2 = scrollY.toLong().coerceIn(
                -productItemStatus.qty!!,
                0L,
            )

            // remove from the slot
            if (removeCount1 <= -1L) {
                getPlayerInventory().add(productItemStatus.itm!!, -removeCount2)
                productItemStatus.changeCount(removeCount2)
            }
            itemListUpdateKeepCurrentFilter()
        }
        else {
            itemListUpdateKeepCurrentFilter()
        }
    } }
}