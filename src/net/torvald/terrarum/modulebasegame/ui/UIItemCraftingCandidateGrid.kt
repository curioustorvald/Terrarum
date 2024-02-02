package net.torvald.terrarum.modulebasegame.ui

import net.torvald.terrarum.CraftingRecipeCodex
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.ui.UIItemCatBar
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.CraftingCodex
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair

/**
 * Created by minjaesong on 2022-06-28.
 */
class UIItemCraftingCandidateGrid(
    parentUI: UICrafting,
    initialX: Int, initialY: Int,
    horizontalCells: Int, verticalCells: Int,
    drawScrollOnRightside: Boolean = false,
    keyDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Keycode, extra info, keyed button
    touchDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit // Item, Amount, Button, extra info, clicked button
) : UIItemInventoryItemGrid(
    parentUI,
    { TODO() /* UNUSED and MUST NOT BE USED! */ },
    initialX, initialY,
    horizontalCells, verticalCells,
    drawScrollOnRightside,
    drawWallet = false,
    hideSidebar = false,
    keyDownFun = keyDownFun,
    touchDownFun = touchDownFun,
    wheelFun = { _, _, _, _, _, _ -> },
    useHighlightingManager = false
) {

    val craftingRecipes = ArrayList<CraftingCodex.CraftingRecipe>()

    internal val recipesSortList = ArrayList<CraftingCodex.CraftingRecipe>() // a dual to the [inventorySortList] which contains the actual recipes instead of crafting recipes

    private var highlightedRecipe: CraftingCodex.CraftingRecipe? = null

    fun highlightRecipe(recipe: CraftingCodex.CraftingRecipe?, changePage: Boolean = false) {
        items.forEach { it.forceHighlighted = false }

        highlightedRecipe = recipe

        // search for the recipe
        // also need to find what "page" the recipe might be in
        // use it.isCompactMode to find out the current mode
        var ord = 0
        var found = false
        while (ord < craftingRecipes.indices.last) {
            if (recipe == craftingRecipes[ord]) {
                found = true
                break
            }
            ord += 1
        }
        val itemSize = items.size
        val newPage = ord / itemSize

        if (found) {
            if (changePage) {
                itemPage = newPage
                items[ord % itemSize].forceHighlighted = true
            }
            // if we are on the same page, highlight the cell; otherwise, do nothing
            else if (itemPage == newPage) {
                items[ord % itemSize].forceHighlighted = true
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
//        printdbg(this, "Is this recipe craftable? $recipe")
        return UICrafting.recipeToIngredientRecord(player, recipe, nearbyCraftingStations).none {
//            printdbg(this, "    considering ingredient ${it.selectedItem}, ${it.howManyRecipeWants} is required and got ${it.howManyPlayerHas}; crafting station available? ${it.craftingStationAvailable}")
            it.howManyPlayerHas <= 0L || !it.craftingStationAvailable
        }
    }

    override fun scrollItemPage(relativeAmount: Int) {
        super.scrollItemPage(relativeAmount)

        // update highlighter status
        highlightRecipe(highlightedRecipe)
    }

    private var currentFilter1 = arrayOf("")

    override fun rebuild(filter: Array<String>) {
//        printdbg(this, "Rebuilding crafting candidate with following filters: ${filter.joinToString()}")
        currentFilter1 = filter

        // filtering policy: if the player have all the ingredient item (regardless of the amount!), make the recipe visible
        craftingRecipes.clear()
        CraftingRecipeCodex.props.forEach { (_, recipes) ->
            recipes.forEach {
                if (isCraftable((parentUI as UICrafting).getPlayerInventory(), it, (parentUI as UICrafting).nearbyCraftingStations)) {
                    craftingRecipes.add(it)
                }
                else {
//                    printdbg(this, "  Skipping $recipes: insufficient ingredients")
                }
            }
        }

        recipesSortList.clear() // kinda like the output list

        craftingRecipes.forEach {
            if (
                filter.contains((ItemCodex[it.product]?.inventoryCategory ?: throw IllegalArgumentException("Unknown item: ${it.product}"))) ||
                filter[0] == UIItemCatBar.CAT_ALL
                ) {
                recipesSortList.add(it)
            }
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

    override fun rebuild(filterFun: (InventoryPair) -> Boolean) {
        rebuild(currentFilter1)
    }

    override fun rebuild(filterFun: (InventoryPair) -> Boolean, itemAppendix: ItemID) {
        rebuild(currentFilter1)
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        super.scrolled(amountX, amountY)
        return true
    }
}