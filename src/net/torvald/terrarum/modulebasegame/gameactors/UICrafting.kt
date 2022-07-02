package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.gamepadLabelLEFTRIGHT
import net.torvald.terrarum.App.gamepadLabelStart
import net.torvald.terrarum.UIItemInventoryCatBar.Companion.CAT_ALL
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.CraftingCodex
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemSpinner
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.unicode.getKeycapPC
import kotlin.math.ceil

/**
 * This UI has inventory, but it's just there to display all craftable items and should not be serialised.
 *
 * Created by minjaesong on 2022-03-10.
 */
class UICrafting(val full: UIInventoryFull) : UICanvas(), HasInventory {

    private val catBar: UIItemInventoryCatBar
        get() = full.catBar

    override var width = App.scr.width
    override var height = App.scr.height
    override var openCloseTime: Second = 0.0f

    private val itemListPlayer: UIItemInventoryItemGrid
    private val itemListCraftable: UIItemCraftingCandidateGrid // might be changed to something else
    private val itemListIngredients: UIItemInventoryItemGrid // this one is definitely not to be changed
    private val buttonCraft: UIItemTextButton
    private val spinnerCraftCount: UIItemSpinner

    private val craftables = FixtureInventory() // might be changed to something else
    private val ingredients = FixtureInventory() // this one is definitely not to be changed

    private val negotiator = object : InventoryTransactionNegotiator() {
        override fun accept(player: FixtureInventory, fixture: FixtureInventory, item: GameItem, amount: Long) {
//            TODO()
        }

        override fun reject(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long) {
//            TODO()
        }
    }

    override fun getNegotiator() = negotiator
    override fun getFixtureInventory(): FixtureInventory = craftables

    override fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    private var halfSlotOffset = (UIItemInventoryElemSimple.height + listGap) / 2

    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() - halfSlotOffset
    private val thisOffsetX2 = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 7
    private val thisXend = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 13 - listGap
    private val thisOffsetY =  UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()

    private val TEXT_GAP = 26
    private val LAST_LINE_IN_GRID = ((UIItemInventoryElemWide.height + listGap) * (UIInventoryFull.CELLS_VRT - 2)) + 22//359 // TEMPORARY VALUE!

    private var recipeClicked: CraftingCodex.CraftingRecipe? = null

    private val catAll = arrayOf(CAT_ALL)

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(App.getConfigInt("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}\u3000 " +
            "$gamepadLabelLEFTRIGHT ${Lang["GAME_OBJECTIVE_MULTIPLIER"]}\u3000 " +
            "${App.gamepadLabelWest} ${Lang["GAME_ACTION_CRAFT"]}"

    private val oldSelectedItems = ArrayList<ItemID>()

    private val craftMult
        get() = spinnerCraftCount.value.toLong()

    private fun _getItemListPlayer() = itemListPlayer
    private fun _getItemListIngredients() = itemListIngredients

    init {
        val craftButtonsY = thisOffsetY + 23 + (UIItemInventoryElemWide.height + listGap) * (UIInventoryFull.CELLS_VRT - 1)
        val buttonWidth = (UIItemInventoryElemWide.height + listGap) * 3 - listGap - 2

        // ingredient list
        itemListIngredients =  UIItemInventoryItemGrid(
                this,
                catBar,
                { ingredients },
                thisOffsetX,
                thisOffsetY + LAST_LINE_IN_GRID,
                6, 1,
                drawScrollOnRightside = false,
                drawWallet = false,
                hideSidebar = true,
                colourTheme = UIItemInventoryCellCommonRes.defaultInventoryCellTheme.copy(
                        cellHighlightSubCol = Toolkit.Theme.COL_INACTIVE
                ),
                keyDownFun = { _, _, _, _, _ -> },
                touchDownFun = { gameItem, amount, _, _, _ -> gameItem?.let { gameItem ->
                    // if the item is craftable one, load its recipe instead
                    CraftingRecipeCodex.getRecipesFor(gameItem.originalID)?.let { recipes ->
                        // select most viable recipe (completely greedy search)
                        val player = getPlayerInventory()
                        // list of [Score, Ingredients, Recipe]
                        recipes.map { recipe ->
                            // list of (Item, How many player has, How many the recipe requires)
                            val items = recipe.ingredients.map { ingredient ->
                                val selectedItem = if (ingredient.keyMode == CraftingCodex.CraftingItemKeyMode.TAG) {
                                    // If the player has the required item, use it; otherwise, will take an item from the ItemCodex
                                    player.itemList.filter { (itm, qty) ->
                                        ItemCodex[itm]?.tags?.contains(ingredient.key) == true && qty >= ingredient.qty
                                    }.maxByOrNull { it.qty }?.itm ?: ((ItemCodex.itemCodex.firstNotNullOfOrNull { if (it.value.tags.contains(ingredient.key)) it.key else null }) ?: throw NullPointerException("Item with tag '${ingredient.key}' not found. Possible cause: game or a module not updated or installed"))
                                }
                                else {
                                    ingredient.key
                                }

                                val howManyPlayerHas = player.searchByID(selectedItem)?.qty ?: 0L

                                val howManyTheRecipeWants = ingredient.qty

                                listOf(selectedItem, howManyPlayerHas, howManyTheRecipeWants)
                            }

                            val score = items.fold(1L) { acc, item ->
                                (item[1] as Long).times(16L) + 1L
                            }

                            listOf(score, items, recipe)
                        }.maxByOrNull { it[0] as Long }?.let { (_, items, recipe) ->
                            val items = items as List<List<*>>
                            val recipe = recipe as CraftingCodex.CraftingRecipe

                            // change selected recipe to mostViableRecipe then update the UIs accordingly
                            // FIXME recipe highlighting will not change correctly!
                            val selectedItems = ArrayList<ItemID>()

                            // auto-dial the spinner so that player would just have to click the Craft! button (for the most time, that is)
                            val howManyRequired = craftMult * amount
                            val howManyPlayerHas = player.searchByID(gameItem.dynamicID)?.qty ?: 0
                            val howManyPlayerMightNeed = ceil((howManyRequired - howManyPlayerHas).toDouble() / recipe.moq).toLong()
                            resetSpinner(howManyPlayerMightNeed.coerceIn(1L, App.getConfigInt("basegame:gameplay_max_crafting").toLong()))

                            ingredients.clear()
                            recipeClicked = recipe

                            items.forEach {
                                val itm = it[0] as ItemID
                                val qty = it[2] as Long

                                selectedItems.add(itm)
                                ingredients.add(itm, qty)
                            }

                            _getItemListPlayer().removeFromForceHighlightList(oldSelectedItems)
                            _getItemListPlayer().addToForceHighlightList(selectedItems)
                            _getItemListPlayer().rebuild(catAll)
                            _getItemListIngredients().rebuild(catAll)

                            // TODO highlightCraftingCandidateButton by searching for the buttons that has the recipe

                            oldSelectedItems.clear()
                            oldSelectedItems.addAll(selectedItems)

                            refreshCraftButtonStatus()
                        }
                    }
                } }
        )

        // make sure grid buttons for ingredients do nothing (even if they are hidden!)
        itemListIngredients.gridModeButtons[0].touchDownListener = { _,_,_,_ -> }
        itemListIngredients.gridModeButtons[1].touchDownListener = { _,_,_,_ -> }
        itemListIngredients.isCompactMode = true
        itemListIngredients.setCustomHighlightRuleSub {
            it.item?.let { ingredient ->
                return@setCustomHighlightRuleSub getPlayerInventory().searchByID(ingredient.dynamicID)?.let { itemOnPlayer ->
                    itemOnPlayer.qty * craftMult >= it.amount * craftMult
                } == true
            }
            false
        }


        // player inventory to the right
        itemListPlayer = UIItemInventoryItemGrid(
                this,
                catBar,
                { INGAME.actorNowPlaying!!.inventory }, // literally a player's inventory
                thisOffsetX2,
                thisOffsetY,
                6, UIInventoryFull.CELLS_VRT,
                drawScrollOnRightside = true,
                drawWallet = false,
                highlightEquippedItem = false,
                keyDownFun = { _, _, _, _, _ -> },
                touchDownFun = { gameItem, amount, _, _, button -> recipeClicked?.let { recipe -> gameItem?.let { gameItem ->
                    val itemID = gameItem.dynamicID
                    // don't rely on highlightedness of the button to determine the item on the button is the selected
                    // ingredient (because I don't fully trust my code lol)
                    val targetItemToAlter = recipe.ingredients.filter { // altering recipe doesn't make sense if player selected a recipe that requires no tag-ingredients
                        (it.keyMode == CraftingCodex.CraftingItemKeyMode.TAG && gameItem.tags.contains(it.key))
                    }.let {
                        if (it.size > 1)
                            println("[UICrafting] Your recipe seems to have two similar ingredients defined\n" +
                                    "affected ingredients: ${it.joinToString()}\n" +
                                    "the recipe: ${recipe}")
                        it.firstOrNull()
                    }

                    targetItemToAlter?.let {
                        val oldItem = _getItemListIngredients().getInventory().itemList.first { itemPair ->
                            (it.keyMode == CraftingCodex.CraftingItemKeyMode.TAG && ItemCodex[itemPair.itm]!!.tags.contains(it.key))
                        }
                        changeIngredient(oldItem, itemID)
                        refreshCraftButtonStatus()
                    }
                } } }
        )
        // make grid mode buttons work together
//        itemListPlayer.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
//        itemListPlayer.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }



        // crafting list to the left
        itemListCraftable = UIItemCraftingCandidateGrid(
                this,
                catBar,
                thisOffsetX,
                thisOffsetY,
                6, UIInventoryFull.CELLS_VRT - 2, // decrease the internal height so that craft/cancel button would fit in
                keyDownFun = { _, _, _, _, _ -> },
                touchDownFun = { gameItem, amount, _, recipe0, button ->
                    (recipe0 as? CraftingCodex.CraftingRecipe)?.let { recipe ->
                        val selectedItems = ArrayList<ItemID>()

                        val playerInventory = getPlayerInventory()
                        ingredients.clear()
                        recipeClicked = recipe
//                        printdbg(this, "Recipe selected: $recipe")
                        recipe.ingredients.forEach { ingredient ->
                            val selectedItem: ItemID = if (ingredient.keyMode == CraftingCodex.CraftingItemKeyMode.TAG) {
                                // If the player has the required item, use it; otherwise, will take an item from the ItemCodex
                                val selectedItem = playerInventory.itemList.filter { (itm, qty) ->
                                    ItemCodex[itm]?.tags?.contains(ingredient.key) == true && qty >= ingredient.qty
                                }.maxByOrNull { it.qty }?.itm ?: ((ItemCodex.itemCodex.firstNotNullOfOrNull { if (it.value.tags.contains(ingredient.key)) it.key else null }) ?: throw NullPointerException("Item with tag '${ingredient.key}' not found. Possible cause: game or a module not updated or installed"))

//                                printdbg(this, "Adding ingredients by tag ${selectedItem} (${ingredient.qty})")
                                selectedItem
                            }
                            else {
//                                printdbg(this, "Adding ingredients by name ${ingredient.key} (${ingredient.qty})")
                                ingredient.key
                            }

                            selectedItems.add(selectedItem)
                            ingredients.add(selectedItem, ingredient.qty)
                        }

                        _getItemListPlayer().removeFromForceHighlightList(oldSelectedItems)
                        _getItemListPlayer().addToForceHighlightList(selectedItems)
                        _getItemListPlayer().rebuild(catAll)
                        _getItemListIngredients().rebuild(catAll)

                        highlightCraftingCandidateButton(button)

                        oldSelectedItems.clear()
                        oldSelectedItems.addAll(selectedItems)

                        refreshCraftButtonStatus()
                    }
                }
        )
        buttonCraft = UIItemTextButton(this, "GAME_ACTION_CRAFT", thisOffsetX + 3 + buttonWidth + listGap, craftButtonsY, buttonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
        spinnerCraftCount = UIItemSpinner(this, thisOffsetX + 1, craftButtonsY, 1, 1, App.getConfigInt("basegame:gameplay_max_crafting"), 1, buttonWidth, numberToTextFunction = {"×\u200A${it.toInt()}"})
        spinnerCraftCount.selectionChangeListener = {
            itemListIngredients.numberMultiplier = it.toLong()
            itemListIngredients.rebuild(catAll)
            itemListCraftable.numberMultiplier = it.toLong()
            itemListCraftable.rebuild(catAll)
            refreshCraftButtonStatus()
        }


        buttonCraft.touchDownListener = { _,_,_,_ ->
            getPlayerInventory().let { player -> recipeClicked?.let { recipe ->
                // check if player has enough amount of ingredients
                val itemCraftable = itemListIngredients.getInventory().itemList.all { (itm, qty) ->
                    (player.searchByID(itm)?.qty ?: -1) >= qty * craftMult
                }


                if (itemCraftable) {
                    itemListIngredients.getInventory().itemList.forEach { (itm, qty) ->
                        player.remove(itm, qty * craftMult)
                    }
                    player.add(recipe.product, recipe.moq * craftMult)

                    // reset selection status after a crafting to hide the possible artefact where no-longer-craftable items are still displayed due to ingredient depletion
                    resetUI() // also clears forcehighlightlist
                    itemListPlayer.rebuild(catAll)
                    itemListCraftable.rebuild(catAll)
                }
            } }
            refreshCraftButtonStatus()
        }
        // make grid mode buttons work together
//        itemListCraftable.gridModeButtons[0].touchDownListener = { _,_,_,_ -> setCompact(false) }
//        itemListCraftable.gridModeButtons[1].touchDownListener = { _,_,_,_ -> setCompact(true) }

        handler.allowESCtoClose = true

        addUIitem(itemListCraftable)
        addUIitem(itemListIngredients)
        addUIitem(itemListPlayer)
        addUIitem(spinnerCraftCount)
        addUIitem(buttonCraft)
    }

    private fun changeIngredient(old: InventoryPair, new: ItemID) {
        itemListPlayer.removeFromForceHighlightList(oldSelectedItems)

        oldSelectedItems.remove(old.itm)
        oldSelectedItems.add(new)

        itemListPlayer.addToForceHighlightList(oldSelectedItems)
        itemListPlayer.rebuild(catAll)

        // change highlight status of itemListIngredients
        itemListIngredients.getInventory().let {
            val amount = old.qty
            it.remove(old.itm, amount)
            it.add(new, amount)
        }
        itemListIngredients.rebuild(catAll)
    }

    private fun highlightCraftingCandidateButton(button: UIItemInventoryCellBase?) { // a proxy function
        itemListCraftable.highlightButton(button)
        itemListCraftable.rebuild(catAll)
    }

    /**
     * Updates Craft! button so that the button is correctly highlighted
     */
    fun refreshCraftButtonStatus() {
        val itemCraftable = if (itemListIngredients.getInventory().itemList.size < 1) false
        else getPlayerInventory().let { player ->
            // check if player has enough amount of ingredients
            itemListIngredients.getInventory().itemList.all { (itm, qty) ->
                (player.searchByID(itm)?.qty ?: -1) >= qty * craftMult
            }
        }

        buttonCraft.isActive = itemCraftable
    }

    // reset whatever player has selected to null and bring UI to its initial state
    fun resetUI() {
        // reset spinner
        resetSpinner()
        // reset selected recipe status
        recipeClicked = null
        highlightCraftingCandidateButton(null)
        ingredients.clear()
        itemListPlayer.removeFromForceHighlightList(oldSelectedItems)
        itemListIngredients.rebuild(catAll)

        refreshCraftButtonStatus()
    }

    private fun resetSpinner(value: Long = 1L) {
        spinnerCraftCount.value = value
        spinnerCraftCount.fboUpdateLatch = true
        itemListIngredients.numberMultiplier = value
        itemListCraftable.numberMultiplier = value
    }

    private var openingClickLatched = false

    override fun show() {
        itemListPlayer.getInventory = { INGAME.actorNowPlaying!!.inventory }
        itemListUpdate()

        openingClickLatched = Terrarum.mouseDown

        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    private var encumbrancePerc = 0f

    private fun itemListUpdate() {
        // let itemlists be sorted
        itemListCraftable.rebuild(catAll)
        itemListPlayer.rebuild(catAll)
        encumbrancePerc = getPlayerInventory().let {
            it.capacity.toFloat() / it.maxCapacity
        }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!openingClickLatched) {
            return super.touchDown(screenX, screenY, pointer, button)
        }
        return false
    }

    override fun updateUI(delta: Float) {
        // NO super.update due to an infinite recursion
        this.uiItems.forEach { it.update(delta) }

        if (openingClickLatched && !Terrarum.mouseDown) openingClickLatched = false
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // NO super.render due to an infinite recursion
        this.uiItems.forEach { it.render(batch, camera) }

        batch.color = Color.WHITE

        // text label for two inventory grids
        App.fontGame.draw(batch, Lang["GAME_CRAFTING"], thisOffsetX + 2, thisOffsetY - TEXT_GAP)
        App.fontGame.draw(batch, Lang["GAME_INVENTORY_INGREDIENTS"], thisOffsetX + 2, thisOffsetY + LAST_LINE_IN_GRID - TEXT_GAP)
        App.fontGame.draw(batch, Lang["GAME_INVENTORY"], thisOffsetX2 + 2, thisOffsetY - TEXT_GAP)


        // control hints
        val controlHintXPos = thisOffsetX.toFloat()
        blendNormal(batch)
        App.fontGame.draw(batch, controlHelp, controlHintXPos, full.yEnd - 20)

        

        //draw player encumb
        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        // encumbrance bar will go one row down if control help message is too long
        val encumbBarXPos = thisXend - UIInventoryCells.weightBarWidth
        val encumbBarTextXPos = encumbBarXPos - 6 - App.fontGame.getWidth(encumbranceText)
        val encumbBarYPos = full.yEnd-20 + 3f +
                            if (App.fontGame.getWidth(full.listControlHelp) + 2 + controlHintXPos >= encumbBarTextXPos)
                                App.fontGame.lineHeight
                            else 0f
        App.fontGame.draw(batch, encumbranceText, encumbBarTextXPos, encumbBarYPos - 3f)
        // encumbrance bar background
        blendNormal(batch)
        val encumbCol = UIItemInventoryCellCommonRes.getHealthMeterColour(1f - encumbrancePerc, 0f, 1f)
        val encumbBack = encumbCol mul UIItemInventoryCellCommonRes.meterBackDarkening
        batch.color = encumbBack
        Toolkit.fillArea(batch,
                encumbBarXPos, encumbBarYPos,
                UIInventoryCells.weightBarWidth, UIInventoryFull.controlHelpHeight - 6f
        )
        // encumbrance bar
        batch.color = encumbCol
        Toolkit.fillArea(batch,
                encumbBarXPos, encumbBarYPos,
                if (full.actor.inventory.capacityMode == FixtureInventory.CAPACITY_MODE_NO_ENCUMBER)
                    1f
                else // make sure 1px is always be seen
                    minOf(UIInventoryCells.weightBarWidth, maxOf(1f, UIInventoryCells.weightBarWidth * encumbrancePerc)),
                UIInventoryFull.controlHelpHeight - 6f
        )
        // debug text
        batch.color = Color.LIGHT_GRAY
        if (App.IS_DEVELOPMENT_BUILD) {
            App.fontSmallNumbers.draw(batch,
                    "${full.actor.inventory.capacity}/${full.actor.inventory.maxCapacity}",
                    encumbBarTextXPos,
                    encumbBarYPos + UIInventoryFull.controlHelpHeight - 4f
            )
        }


        blendNormal(batch)
    }

    override fun doOpening(delta: Float) {
        resetUI()

        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun endClosing(delta: Float) {
        resetUI()
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }


    override fun dispose() {
    }
}