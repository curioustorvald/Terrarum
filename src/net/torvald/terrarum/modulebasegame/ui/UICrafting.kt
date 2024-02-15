package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isWall
import net.torvald.terrarum.itemproperties.CraftingCodex
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.CraftingStation
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.modulebasegame.ui.UITemplateHalfInventory.Companion.INVENTORY_NAME_TEXT_GAP
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.ui.UIItemCatBar.Companion.FILTER_CAT_ALL
import net.torvald.unicode.getKeycapPC
import kotlin.math.ceil

/**
 * This UI has inventory, but it's just there to display all craftable items and should not be serialised.
 *
 * Created by minjaesong on 2022-03-10.
 */
class UICrafting(val full: UIInventoryFull?) : UICanvas(
    toggleKeyLiteral = if (full == null) "control_key_inventory" else null,
    toggleButtonLiteral = if (full == null) "control_gamepad_start" else null
), HasInventory {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val playerThings = UITemplateHalfInventory(this, false).also {
        it.itemListTouchDownFun = { gameItem, _, _, _, _ -> recipeClicked?.let { recipe -> gameItem?.let { gameItem ->
            val itemID = gameItem.dynamicID
            // don't rely on highlightedness of the button to determine the item on the button is the selected
            // ingredient (because I don't fully trust my code lol)
            val targetItemToAlter = recipe.ingredients.filter { (key, mode) -> // altering recipe doesn't make sense if player selected a recipe that requires no tag-ingredients
                val tags = key.split(',')
                val wantsWall = tags.contains("WALL")
                (mode == CraftingCodex.CraftingItemKeyMode.TAG && gameItem.hasAllTags(tags) && (wantsWall == gameItem.originalID.isWall())) // true if (wants wall and is wall) or (wants no wall and is not wall)
            }.let {
                if (it.size > 1)
                    println("[UICrafting] Your recipe seems to have two similar ingredients defined\n" +
                            "affected ingredients: ${it.joinToString()}\n" +
                            "the recipe: ${recipe}")
                it.firstOrNull()
            }

            targetItemToAlter?.let { (key, mode) ->
                val oldItem = _getItemListIngredients().getInventory().first { (itm, qty) ->
                    val tags = key.split(',')
                    val wantsWall = tags.contains("WALL")
                    (mode == CraftingCodex.CraftingItemKeyMode.TAG && ItemCodex[itm]!!.hasAllTags(tags) && (wantsWall == itm.isWall())) // true if (wants wall and is wall) or (wants no wall and is not wall)
                }
                changeIngredient(recipe, oldItem, itemID)
                refreshCraftButtonStatus()
            }
        } } }
    }

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

        override fun refund(fixture: FixtureInventory, player: FixtureInventory, item: GameItem, amount: Long) {
//            TODO()
        }
    }

    override fun getNegotiator() = negotiator
    override fun getFixtureInventory(): FixtureInventory = craftables
    override fun getPlayerInventory(): FixtureInventory = INGAME.actorNowPlaying!!.inventory

    private val halfSlotOffset = (UIItemInventoryElemSimple.height + listGap) / 2

    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + listGap - halfSlotOffset
    private val thisOffsetX2 = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 7
    private val thisXend = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 13 - listGap
    private val thisOffsetY =  UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()
    private val cellsWidth = (listGap + UIItemInventoryElemWide.height) * 6 - listGap

    private val LAST_LINE_IN_GRID = ((UIItemInventoryElemWide.height + listGap) * (UIInventoryFull.CELLS_VRT - 2)) + 22//359 // TEMPORARY VALUE!

    private var recipeClicked: CraftingCodex.CraftingRecipe? = null

    private val controlHelp: String
        get() = if (App.environment == RunningEnvironment.PC)
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}"
        else
            "$gamepadLabelStart ${Lang["GAME_ACTION_CLOSE"]}\u3000 " +
            "$gamepadLabelLEFTRIGHT ${Lang["GAME_OBJECTIVE_MULTIPLIER"]}\u3000 " +
            "${App.gamepadLabelWest} ${Lang["GAME_ACTION_CRAFT"]}"

    private val oldSelectedItems = ArrayList<ItemID>()

    private val craftMult
        get() = spinnerCraftCount.value.toLong()

    private fun _getItemListPlayer() = playerThings.itemList
    private fun _getItemListIngredients() = itemListIngredients
    private fun _getItemListCraftables() = itemListCraftable

    init {
        val craftButtonsY = thisOffsetY + 23 + (UIItemInventoryElemWide.height + listGap) * (UIInventoryFull.CELLS_VRT - 1)
        val buttonWidth = (UIItemInventoryElemWide.height + listGap) * 3 - listGap - 2

        // ingredient list
        itemListIngredients = UIItemInventoryItemGrid(
            this,
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
            wheelFun = { _, _, _, _, _, _ -> },
            touchDownFun = { gameItem, amount, _, _, _ -> gameItem?.let { gameItem ->
                // if the clicked item is craftable one, present its recipe to the player //

                CraftingRecipeCodex.getRecipesFor(gameItem.originalID)?.let { recipes ->
                    // select most viable recipe (completely greedy search)
                    val player = getPlayerInventory()
                    // list of [Score, Ingredients, Recipe]
                    recipes.map { recipe ->
                        // list of (Item, How many player has, How many the recipe requires)
                        val items = recipeToIngredientRecord(player, recipe, nearbyCraftingStations)

                        val score = items.fold(1L) { acc, item ->
                            (item.howManyPlayerHas).times(16L) + 1L
                        }

                        listOf(score, items, recipe)
                    }.maxByOrNull { it[0] as Long }?.let { (_, items, recipe) ->
                        val items = items as List<RecipeIngredientRecord>
                        val recipe = recipe as CraftingCodex.CraftingRecipe

                        // change selected recipe to mostViableRecipe then update the UIs accordingly
                        val selectedItems = ArrayList<ItemID>()

                        resetSpinner()

                        ingredients.clear()
                        recipeClicked = recipe

                        items.forEach {
                            val itm = it.selectedItem
                            val qty = it.howManyRecipeWants

                            selectedItems.add(itm)
                            ingredients.add(itm, qty)
                        }

                        _getItemListPlayer().let {
                            it.removeFromForceHighlightList(oldSelectedItems)
                            //filterPlayerListUsing(recipeClicked) // ???
                            it.addToForceHighlightList(selectedItems)
                            filterPlayerListUsing(recipeClicked)
                        }

                        _getItemListIngredients().rebuild(FILTER_CAT_ALL)

                        _getItemListCraftables().highlightRecipe(recipeClicked, true)

                        oldSelectedItems.clear()
                        oldSelectedItems.addAll(selectedItems)

                        refreshCraftButtonStatus()
                    }
                }
            } }
        )

        // make sure grid buttons for ingredients do nothing (even if they are hidden!)
        itemListIngredients.navRemoCon.listButtonListener = { _,_, -> }
        itemListIngredients.navRemoCon.gridButtonListener = { _,_, -> }
        itemListIngredients.isCompactMode = true
        itemListIngredients.setCustomHighlightRuleSub {
            it.item?.let { ingredient ->
                return@setCustomHighlightRuleSub getPlayerInventory().searchByID(ingredient.dynamicID)?.let { itemOnPlayer ->
                    itemOnPlayer.qty * craftMult >= it.amount * craftMult
                } == true
            }
            false
        }



        // crafting list to the left
        itemListCraftable = UIItemCraftingCandidateGrid(
            this,
            thisOffsetX,
            thisOffsetY,
            6, UIInventoryFull.CELLS_VRT - 2, // decrease the internal height so that craft/cancel button would fit in
            keyDownFun = { _, _, _, _, _ -> },
            touchDownFun = { gameItem, amount, _, recipe0, button ->
                (recipe0 as? CraftingCodex.CraftingRecipe).let { recipe ->
                    val selectedItems = ArrayList<ItemID>()

                    val playerInventory = getPlayerInventory()
                    ingredients.clear()
                    recipeClicked = recipe
//                        printdbg(this, "Recipe selected: $recipe")
                    recipe?.ingredients?.forEach { ingredient ->
                        val selectedItem = getItemForIngredient(playerInventory, ingredient)
                        selectedItems.add(selectedItem)
                        ingredients.add(selectedItem, ingredient.qty)
                    }

                    _getItemListPlayer().removeFromForceHighlightList(oldSelectedItems)
                    _getItemListPlayer().addToForceHighlightList(selectedItems)
                    _getItemListPlayer().itemPage = 0
                    filterPlayerListUsing(recipeClicked)
                    _getItemListIngredients().rebuild(FILTER_CAT_ALL)

                    highlightCraftingCandidateButton(recipe)

                    oldSelectedItems.clear()
                    oldSelectedItems.addAll(selectedItems)

                    refreshCraftButtonStatus()
                }
            }
        )
        buttonCraft = UIItemTextButton(this,
            { Lang["GAME_ACTION_CRAFT"] }, thisOffsetX + 3 + buttonWidth + listGap, craftButtonsY, buttonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
        spinnerCraftCount = UIItemSpinner(this, thisOffsetX + 1, craftButtonsY, 1, 1, App.getConfigInt("basegame:gameplay_max_crafting"), 1, buttonWidth, numberToTextFunction = {"×\u200A${it.toInt()}"})
        spinnerCraftCount.selectionChangeListener = {
            itemListIngredients.numberMultiplier = it.toLong()
            itemListIngredients.rebuild(FILTER_CAT_ALL)
            itemListCraftable.numberMultiplier = it.toLong()
            itemListCraftable.rebuild(FILTER_CAT_ALL)
            refreshCraftButtonStatus()
        }


        buttonCraft.clickOnceListener = { _,_ ->
            getPlayerInventory().let { player -> recipeClicked?.let { recipe ->
                // check if player has enough amount of ingredients
                val itemCraftable = itemListIngredients.getInventory().all { (itm, qty) ->
                    (player.searchByID(itm)?.qty ?: -1) >= qty * craftMult
                }


                if (itemCraftable) {
                    itemListIngredients.getInventory().forEach { (itm, qty) ->
                        player.remove(itm, qty * craftMult)
                    }
                    player.add(recipe.product, recipe.moq * craftMult)

                    // reset selection status after a crafting to hide the possible artefact where no-longer-craftable items are still displayed due to ingredient depletion
                    resetUI() // also clears forcehighlightlist
                    playerThings.rebuild(FILTER_CAT_ALL)
                    itemListCraftable.rebuild(FILTER_CAT_ALL)
                }
            } }
            refreshCraftButtonStatus()
        }
        // make grid mode buttons work together
//        itemListCraftable.gridModeButtons[0].clickOnceListener = { _,_ -> setCompact(false) }
//        itemListCraftable.gridModeButtons[1].clickOnceListener = { _,_ -> setCompact(true) }

        handler.allowESCtoClose = true

        addUIitem(itemListCraftable)
        addUIitem(itemListIngredients)
        addUIitem(playerThings)
        addUIitem(spinnerCraftCount)
        addUIitem(buttonCraft)
    }

    private fun filterPlayerListUsing(recipe: CraftingCodex.CraftingRecipe?) {
        if (recipe == null)
            playerThings.rebuild(FILTER_CAT_ALL)
        else {
            val items = recipe.ingredients.flatMap {
                getItemCandidatesForIngredient(getPlayerInventory(), it).map { it.itm }
            }.sorted()

            val filterFun = { pair: InventoryPair ->
                items.binarySearch(pair.itm) >= 0
            }
            playerThings.rebuild(filterFun, recipe.product)
        }
    }

    var nearbyCraftingStations = emptyList<String>(); protected set

    fun getCraftingStationsWithinReach(): List<String> {
        val reach = INGAME.actorNowPlaying!!.actorValue.getAsDouble(AVKey.REACH)!! * (INGAME.actorNowPlaying!!.actorValue.getAsDouble(AVKey.REACHBUFF) ?: 1.0) * INGAME.actorNowPlaying!!.scale
        val nearbyCraftingStations = INGAME.findKNearestActors(INGAME.actorNowPlaying!!, 256) {
            it is CraftingStation && (distBetweenActors(it, INGAME.actorNowPlaying!!) < reach)
        }
        return nearbyCraftingStations.flatMap { (it.get() as CraftingStation).tags }
    }

    private fun changeIngredient(recipe: CraftingCodex.CraftingRecipe?, old: InventoryPair, new: ItemID) {
        playerThings.itemList.removeFromForceHighlightList(oldSelectedItems)

        oldSelectedItems.remove(old.itm)
        oldSelectedItems.add(new)

        playerThings.itemList.addToForceHighlightList(oldSelectedItems)
        playerThings.itemList.itemPage = 0
        filterPlayerListUsing(recipe)

        // change highlight status of itemListIngredients
        itemListIngredients.getInventory().let {
            val amount = old.qty
            it.remove(old.itm, amount)
            it.add(new, amount)
        }
        itemListIngredients.rebuild(FILTER_CAT_ALL)
    }

    private fun highlightCraftingCandidateButton(recipe: CraftingCodex.CraftingRecipe?) { // a proxy function
        itemListCraftable.highlightRecipe(recipe)
        itemListCraftable.rebuild(FILTER_CAT_ALL)
    }

    /**
     * Updates Craft! button so that the button is correctly highlighted
     */
    fun refreshCraftButtonStatus() {
        val itemCraftable = if (itemListIngredients.getInventory().totalUniqueCount < 1) false
        else getPlayerInventory().let { player ->
            // check if player has enough amount of ingredients
            itemListIngredients.getInventory().all { (itm, qty) ->
                (player.searchByID(itm)?.qty ?: -1) >= qty * craftMult
            }
        }

        buttonCraft.isEnabled = itemCraftable
    }

    // reset whatever player has selected to null and bring UI to its initial state
    fun resetUI() {
        // reset spinner
        resetSpinner()

        // reset selected recipe status
        recipeClicked = null
        filterPlayerListUsing(recipeClicked)
        highlightCraftingCandidateButton(null)
        ingredients.clear()
        playerThings.removeFromForceHighlightList(oldSelectedItems)
        itemListIngredients.rebuild(FILTER_CAT_ALL)

        // reset scroll
        itemListCraftable.itemPage = 0
        playerThings.itemList.itemPage = 0

        refreshCraftButtonStatus()
    }

    private fun resetSpinner() {
        spinnerCraftCount.resetToSmallest()
        itemListIngredients.numberMultiplier = 1L
        itemListCraftable.numberMultiplier = 1L
    }

    private var openingClickLatched = false

    override fun show() {
        nearbyCraftingStations = getCraftingStationsWithinReach()
//        printdbg(this, "Nearby crafting stations: $nearbyCraftingStations")

        playerThings.setGetInventoryFun { INGAME.actorNowPlaying!!.inventory }
        itemListUpdate()

        openingClickLatched = Terrarum.mouseDown

        tooltipShowing.clear()
        INGAME.setTooltipMessage(null)

        resetUI()
    }

    private var encumbrancePerc = 0f

    private fun itemListUpdate() {
        // let itemlists be sorted
        itemListCraftable.rebuild(FILTER_CAT_ALL)
        playerThings.rebuild(FILTER_CAT_ALL)
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

    override fun updateImpl(delta: Float) {
        // NO super.update due to an infinite recursion
        this.uiItems.forEach { it.update(delta) }

        if (openingClickLatched && !Terrarum.mouseDown) openingClickLatched = false
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // NO super.render due to an infinite recursion
        this.uiItems.forEach { it.render(frameDelta, batch, camera) }

        batch.color = Color.WHITE

        // text label for two inventory grids
        val craftingLabel = Lang["GAME_CRAFTING"]
        val ingredientsLabel = Lang["GAME_INVENTORY_INGREDIENTS"]

        App.fontGame.draw(batch, craftingLabel, thisOffsetX + (cellsWidth - App.fontGame.getWidth(craftingLabel)) / 2, thisOffsetY - INVENTORY_NAME_TEXT_GAP)
        App.fontGame.draw(batch, ingredientsLabel, thisOffsetX + (cellsWidth - App.fontGame.getWidth(ingredientsLabel)) / 2, thisOffsetY + LAST_LINE_IN_GRID - INVENTORY_NAME_TEXT_GAP)


        // control hints
        val controlHintXPos = thisOffsetX + 2f
        blendNormalStraightAlpha(batch)
        App.fontGame.draw(batch, controlHelp, controlHintXPos, UIInventoryFull.yEnd - 20)

        
        if (INGAME.actorNowPlaying != null) {
            //draw player encumb
            val encumbBarXPos = thisXend - UIInventoryCells.weightBarWidth + 36
            val encumbBarYPos = UIInventoryFull.yEnd - 20 + 3f
            UIInventoryCells.drawEncumbranceBar(batch, encumbBarXPos, encumbBarYPos, encumbrancePerc, INGAME.actorNowPlaying!!.inventory)
        }


        blendNormalStraightAlpha(batch)
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.setTooltipMessage(null)
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.setTooltipMessage(null)
    }

    override fun endOpening(delta: Float) {
        super.endOpening(delta)
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        resetUI()
        tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }


    override fun dispose() {
    }

    companion object {
        data class RecipeIngredientRecord(
            val selectedItem: ItemID,
            val howManyPlayerHas: Long,
            val howManyRecipeWants: Long,
            val craftingStationAvailable: Boolean,
        )

        fun getItemCandidatesForIngredient(inventory: FixtureInventory, ingredient: CraftingCodex.CraftingIngredients): List<InventoryPair> {
            return if (ingredient.keyMode == CraftingCodex.CraftingItemKeyMode.TAG) {
                val tags = ingredient.key.split(',')
                val wantsWall = tags.contains("WALL")
                // If the player has the required item, use it; otherwise, will take an item from the ItemCodex
                inventory.filter { (itm, qty) ->
                    ItemCodex[itm]?.hasAllTags(tags) == true && qty >= ingredient.qty && (wantsWall == itm.isWall()) // true if (wants wall and is wall) or (wants no wall and is not wall)
                }
            }
            else {
                listOf(InventoryPair(ingredient.key, -1))
            }
        }

        fun getItemForIngredient(inventory: FixtureInventory, ingredient: CraftingCodex.CraftingIngredients): ItemID {
            val candidate = getItemCandidatesForIngredient(inventory, ingredient)

            return if (ingredient.keyMode == CraftingCodex.CraftingItemKeyMode.TAG) {
                candidate.maxByOrNull { it.qty }?.itm ?: (
                    (ItemCodex.itemCodex.firstNotNullOfOrNull { if (it.value.hasTag(ingredient.key)) it.key else null }) ?:
                        throw NullPointerException("Item with tag '${ingredient.key}' not found. Possible cause: game or a module not updated or installed (ingredient: $ingredient)")
                )
            }
            else {
                ingredient.key
            }
        }

        /**
         * For each ingredient of the recipe, returns list of (ingredient, how many the player has the ingredient, how many the recipe wants)
         */
        fun recipeToIngredientRecord(inventory: FixtureInventory, recipe: CraftingCodex.CraftingRecipe, nearbyCraftingStations: List<String>): List<RecipeIngredientRecord> {
            val hasStation = if (recipe.workbench.isBlank()) true else nearbyCraftingStations.containsAll(recipe.workbench.split(','))
            return recipe.ingredients.map { ingredient ->
                val selectedItem = getItemForIngredient(inventory, ingredient)
                val howManyPlayerHas = inventory.searchByID(selectedItem)?.qty ?: 0L
                val howManyTheRecipeWants = ingredient.qty

                RecipeIngredientRecord(selectedItem, howManyPlayerHas, howManyTheRecipeWants, hasStation)
            }
        }
    }
}