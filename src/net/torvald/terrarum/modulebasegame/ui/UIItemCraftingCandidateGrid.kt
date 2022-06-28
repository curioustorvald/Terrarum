package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.CraftingRecipeCodex
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.UIItemInventoryCatBar
import net.torvald.terrarum.ceilInt
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.itemproperties.CraftingCodex
import net.torvald.terrarum.modulebasegame.gameactors.UICrafting

/**
 * Created by minjaesong on 2022-06-28.
 */
class UIItemCraftingCandidateGrid(
        parentUI: UICrafting, catBar: UIItemInventoryCatBar,
        initialX: Int, initialY: Int,
        horizontalCells: Int, verticalCells: Int,
        drawScrollOnRightside: Boolean = false,
        keyDownFun: (GameItem?, Long, Int, Any?) -> Unit, // Item, Amount, Keycode, extra info
        touchDownFun: (GameItem?, Long, Int, Any?) -> Unit // Item, Amount, Button, extra info
) : UIItemInventoryItemGrid(
        parentUI, catBar,
        { TODO() /* UNUSED and MUST NOT BE USED! */ },
        initialX, initialY,
        horizontalCells, verticalCells,
        drawScrollOnRightside,
        drawWallet = false,
        hideSidebar = false,
        keyDownFun = keyDownFun,
        touchDownFun = touchDownFun
) {

    val craftingRecipes = ArrayList<CraftingCodex.CraftingRecipe>()

    init {
    }

    internal val recipesSortList = ArrayList<CraftingCodex.CraftingRecipe>() // a dual to the [inventorySortList] which contains the actual recipes instead of crafting recipes

    override fun rebuild(filter: Array<String>) {
        // test fill craftingRecipes with every possible recipes in the game
        craftingRecipes.clear()
        CraftingRecipeCodex.props.forEach { (_, recipes) -> craftingRecipes.addAll(recipes) }


        recipesSortList.clear() // kinda like the output list

        craftingRecipes.forEach {
            if ((filter.contains((ItemCodex[it.product]?.inventoryCategory ?: throw IllegalArgumentException("Unknown item: ${it.product}"))) || filter[0] == UIItemInventoryCatBar.CAT_ALL))
                recipesSortList.add(it)
        }

        // map sortList to item list
        for (k in items.indices) {
            // we have an item
            try {
                val sortListItem = recipesSortList[k + itemPage * items.size]
                items[k].item = ItemCodex[sortListItem.product]
                items[k].amount = sortListItem.moq
                items[k].itemImage = ItemCodex.getItemImage(sortListItem.product)
                items[k].extraInfo = sortListItem

                // set quickslot number
                /*if (getInventory() is ActorInventory) {
                    val ainv = getInventory() as ActorInventory

                    for (qs in 1..UIQuickslotBar.SLOT_COUNT) {
                        if (sortListItem.product == ainv.getQuickslotItem(qs - 1)?.itm) {
                            items[k].quickslot = qs % 10 // 10 -> 0, 1..9 -> 1..9
                            break
                        }
                        else
                            items[k].quickslot = null
                    }

                    // set equippedslot number
                    for (eq in ainv.itemEquipped.indices) {
                        if (eq < ainv.itemEquipped.size) {
                            if (ainv.itemEquipped[eq] == items[k].item?.dynamicID) {
                                items[k].equippedSlot = eq
                                break
                            }
                            else
                                items[k].equippedSlot = null
                        }
                    }
                }*/
            }
            // we do not have an item, empty the slot
            catch (e: IndexOutOfBoundsException) {
                items[k].item = null
                items[k].amount = 0
                items[k].itemImage = null
                items[k].quickslot = null
                items[k].equippedSlot = null
                items[k].extraInfo = null
            }
        }


        itemPageCount = (recipesSortList.size.toFloat() / items.size.toFloat()).ceilInt()

        rebuildList = false
    }
}