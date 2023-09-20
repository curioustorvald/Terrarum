package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.CraftingRecipeCodex
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.UIItemInventoryCatBar
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.itemproperties.CraftingCodex
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory

/**
 * Created by minjaesong on 2022-06-28.
 */
class UIItemCraftingCandidateGrid(
        parentUI: UICrafting, catBar: UIItemInventoryCatBar,
        initialX: Int, initialY: Int,
        horizontalCells: Int, verticalCells: Int,
        drawScrollOnRightside: Boolean = false,
        keyDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Keycode, extra info, keyed button
        touchDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit // Item, Amount, Button, extra info, clicked button
) : UIItemInventoryItemGrid(
        parentUI, catBar,
        { TODO() /* UNUSED and MUST NOT BE USED! */ },
        initialX, initialY,
        horizontalCells, verticalCells,
        drawScrollOnRightside,
        drawWallet = false,
        hideSidebar = false,
        keyDownFun = keyDownFun,
        touchDownFun = touchDownFun,
        useHighlightingManager = false
) {

    val craftingRecipes = ArrayList<CraftingCodex.CraftingRecipe>()

    internal val recipesSortList = ArrayList<CraftingCodex.CraftingRecipe>() // a dual to the [inventorySortList] which contains the actual recipes instead of crafting recipes

    private var highlightedRecipe: CraftingCodex.CraftingRecipe? = null

    fun highlightRecipe(recipe: CraftingCodex.CraftingRecipe?) {
        items.forEach { it.forceHighlighted = false }

        highlightedRecipe = recipe

        recipe?.let {
            items.find { it.extraInfo == recipe }?.let { buttonFound ->
                buttonFound.forceHighlighted = true
            }
        }
    }

    override var isCompactMode = false // this is INIT code
        set(value) {
            field = value
            items = if (value) itemGrid else itemList
            highlightRecipe(highlightedRecipe)
            rebuild(currentFilter)
        }

    private fun isCraftable(player: FixtureInventory, recipe: CraftingCodex.CraftingRecipe, nearbyCraftingStations: List<String>): Boolean {
        return UICrafting.recipeToIngredientRecord(player, recipe, nearbyCraftingStations).none { it.howManyPlayerHas <= 0L || !it.craftingStationAvailable }
    }

    override fun scrollItemPage(relativeAmount: Int) {
        super.scrollItemPage(relativeAmount)

        // update highlighter status
        highlightRecipe(highlightedRecipe)
    }

    override fun rebuild(filter: Array<String>) {
        // filtering policy: if the player have all the ingredient item (regardless of the amount!), make the recipe visible
        craftingRecipes.clear()
        CraftingRecipeCodex.props.forEach { (_, recipes) ->
            recipes.forEach {
                if (isCraftable((parentUI as UICrafting).getPlayerInventory(), it, (parentUI as UICrafting).nearbyCraftingStations)) craftingRecipes.add(it)
            }
        }

        recipesSortList.clear() // kinda like the output list

        craftingRecipes.forEach {
            if ((filter.contains((ItemCodex[it.product]?.inventoryCategory ?: throw IllegalArgumentException("Unknown item: ${it.product}"))) || filter[0] == UIItemInventoryCatBar.CAT_ALL))
                recipesSortList.add(it)
        }

        // map sortList to item list
        for (k in items.indices) {
            val item = items[k]
            // we have an item
            try {
                val sortListItem = recipesSortList[k + itemPage * items.size]
                item.item = ItemCodex[sortListItem.product]
                item.amount = sortListItem.moq * numberMultiplier
                item.itemImage = ItemCodex.getItemImage(sortListItem.product)
                item.extraInfo = sortListItem
                item.forceHighlighted = (item.extraInfo == highlightedRecipe)
            }
            // we do not have an item, empty the slot
            catch (e: IndexOutOfBoundsException) {
                item.item = null
                item.amount = 0
                item.itemImage = null
                item.quickslot = null
                item.equippedSlot = null
                item.extraInfo = null
                item.forceHighlighted = false
            }
        }


        itemPageCount = (recipesSortList.size.toFloat() / items.size.toFloat()).ceilToInt()



        rebuildList = false
    }




    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        super.scrolled(amountX, amountY)
        return true
    }
}