package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.UIItemInventoryCatBar.Companion.CAT_ALL
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isWall
import net.torvald.terrarum.itemproperties.CraftingCodex
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.CraftingStation
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemSpinner
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.unicode.getKeycapPC
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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

    private val thisOffsetX = UIInventoryFull.INVENTORY_CELLS_OFFSET_X() + UIItemInventoryElemSimple.height + listGap - halfSlotOffset
    private val thisOffsetX2 = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 7
    private val thisXend = thisOffsetX + (listGap + UIItemInventoryElemWide.height) * 13 - listGap
    private val thisOffsetY =  UIInventoryFull.INVENTORY_CELLS_OFFSET_Y()
    private val cellsWidth = (listGap + UIItemInventoryElemWide.height) * 6 - listGap

    private val TEXT_GAP = 26
    private val LAST_LINE_IN_GRID = ((UIItemInventoryElemWide.height + listGap) * (UIInventoryFull.CELLS_VRT - 2)) + 22//359 // TEMPORARY VALUE!

    private var recipeClicked: CraftingCodex.CraftingRecipe? = null

    private val catAll = arrayOf(CAT_ALL)

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

    private fun _getItemListPlayer() = itemListPlayer
    private fun _getItemListIngredients() = itemListIngredients
    private fun _getItemListCraftables() = itemListCraftable

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
                            val items = recipeToIngredientRecord(player, recipe, nearbyCraftingStations)

                            val score = items.fold(1L) { acc, item ->
                                (item.howManyPlayerHas).times(16L) + 1L
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

                            _getItemListPlayer().let {
                                it.removeFromForceHighlightList(oldSelectedItems)
                                filterPlayerListUsing(recipeClicked)
                                it.addToForceHighlightList(selectedItems)
                                it.rebuild(catAll)
                            }
                            _getItemListIngredients().rebuild(catAll)

                            // highlighting CraftingCandidateButton by searching for the buttons that has the recipe
                            _getItemListCraftables().let {
                                // turn the highlights off
                                it.items.forEach { it.forceHighlighted = false }

                                // search for the recipe
                                // also need to find what "page" the recipe might be in
                                // use it.isCompactMode to find out the current mode
                                var ord = 0
                                while (ord < it.craftingRecipes.indices.last) {
                                    if (recipeClicked == it.craftingRecipes[ord]) break
                                    ord += 1
                                }
                                val itemSize = it.items.size

                                it.itemPage = ord / itemSize
                                it.items[ord % itemSize].forceHighlighted = true
                            }

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
        )
        // make grid mode buttons work together
//        itemListPlayer.gridModeButtons[0].clickOnceListener = { _,_ -> setCompact(false) }
//        itemListPlayer.gridModeButtons[1].clickOnceListener = { _,_ -> setCompact(true) }



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
                        val selectedItem = getItemForIngredient(playerInventory, ingredient)
                        selectedItems.add(selectedItem)
                        ingredients.add(selectedItem, ingredient.qty)
                    }

                    _getItemListPlayer().removeFromForceHighlightList(oldSelectedItems)
                    _getItemListPlayer().addToForceHighlightList(selectedItems)
                    filterPlayerListUsing(recipeClicked)
                    _getItemListIngredients().rebuild(catAll)

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
            itemListIngredients.rebuild(catAll)
            itemListCraftable.numberMultiplier = it.toLong()
            itemListCraftable.rebuild(catAll)
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
                    itemListPlayer.rebuild(catAll)
                    itemListCraftable.rebuild(catAll)
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
        addUIitem(itemListPlayer)
        addUIitem(spinnerCraftCount)
        addUIitem(buttonCraft)
    }

    private fun filterPlayerListUsing(recipe: CraftingCodex.CraftingRecipe?) {
        if (recipe == null)
            itemListPlayer.rebuild(catAll)
        else {
            val items = recipe.ingredients.flatMap { getItemCandidatesForIngredient(getPlayerInventory(), it).map { it.itm } }.sorted()
            val filterFun = { pair: InventoryPair ->
                items.binarySearch(pair.itm) >= 0
            }
            itemListPlayer.rebuild(filterFun)
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
        itemListPlayer.removeFromForceHighlightList(oldSelectedItems)

        oldSelectedItems.remove(old.itm)
        oldSelectedItems.add(new)

        itemListPlayer.addToForceHighlightList(oldSelectedItems)
        filterPlayerListUsing(recipe)

        // change highlight status of itemListIngredients
        itemListIngredients.getInventory().let {
            val amount = old.qty
            it.remove(old.itm, amount)
            it.add(new, amount)
        }
        itemListIngredients.rebuild(catAll)
    }

    private fun highlightCraftingCandidateButton(recipe: CraftingCodex.CraftingRecipe?) { // a proxy function
        itemListCraftable.highlightRecipe(recipe)
        itemListCraftable.rebuild(catAll)
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
        itemListPlayer.removeFromForceHighlightList(oldSelectedItems)
        itemListIngredients.rebuild(catAll)

        refreshCraftButtonStatus()
    }

    private fun resetSpinner(value: Long = 1L) {
        spinnerCraftCount.resetToSmallest()
        itemListIngredients.numberMultiplier = value
        itemListCraftable.numberMultiplier = value
    }

    private var openingClickLatched = false

    override fun show() {
        nearbyCraftingStations = getCraftingStationsWithinReach()
//        printdbg(this, "Nearby crafting stations: $nearbyCraftingStations")

        itemListPlayer.getInventory = { INGAME.actorNowPlaying!!.inventory }
        itemListUpdate()

        openingClickLatched = Terrarum.mouseDown

        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)

        resetUI()
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

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        // NO super.render due to an infinite recursion
        this.uiItems.forEach { it.render(batch, camera) }

        batch.color = Color.WHITE

        // text label for two inventory grids
        val craftingLabel = Lang["GAME_CRAFTING"]
        val ingredientsLabel = Lang["GAME_INVENTORY_INGREDIENTS"]
        val playerName = INGAME.actorNowPlaying!!.actorValue.getAsString(AVKey.NAME).orEmpty().let { it.ifBlank { Lang["GAME_INVENTORY"] } }

        App.fontGame.draw(batch, craftingLabel, thisOffsetX + (cellsWidth - App.fontGame.getWidth(craftingLabel)) / 2, thisOffsetY - TEXT_GAP)
        App.fontGame.draw(batch, ingredientsLabel, thisOffsetX + (cellsWidth - App.fontGame.getWidth(ingredientsLabel)) / 2, thisOffsetY + LAST_LINE_IN_GRID - TEXT_GAP)
        App.fontGame.draw(batch, playerName, thisOffsetX2 + (cellsWidth - App.fontGame.getWidth(playerName)) / 2, thisOffsetY - TEXT_GAP)


        // control hints
        val controlHintXPos = thisOffsetX + 2f
        blendNormalStraightAlpha(batch)
        App.fontGame.draw(batch, controlHelp, controlHintXPos, full.yEnd - 20)

        

        //draw player encumb
        // encumbrance meter
        val encumbranceText = Lang["GAME_INVENTORY_ENCUMBRANCE"]
        // encumbrance bar will go one row down if control help message is too long
        val encumbBarXPos = thisXend - UIInventoryCells.weightBarWidth + 36
        val encumbBarTextXPos = encumbBarXPos - 6 - App.fontGame.getWidth(encumbranceText)
        val encumbBarYPos = full.yEnd-20 + 3f +
                            if (App.fontGame.getWidth(full.listControlHelp) + 2 + controlHintXPos >= encumbBarTextXPos)
                                App.fontGame.lineHeight
                            else 0f
        App.fontGame.draw(batch, encumbranceText, encumbBarTextXPos, encumbBarYPos - 3f)
        // encumbrance bar background
        blendNormalStraightAlpha(batch)
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
                    min(UIInventoryCells.weightBarWidth, max(1f, UIInventoryCells.weightBarWidth * encumbrancePerc)),
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
        UIItemInventoryItemGrid.tooltipShowing.clear()
        INGAME.setTooltipMessage(null) // required!
    }

    override fun endClosing(delta: Float) {
        super.endClosing(delta)
        resetUI()
        UIItemInventoryItemGrid.tooltipShowing.clear()
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
                        throw NullPointerException("Item with tag '${ingredient.key}' not found. Possible cause: game or a module not updated or installed")
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
            val hasStation = if (recipe.workbench.isBlank()) true else nearbyCraftingStations.contains(recipe.workbench)
            return recipe.ingredients.map { ingredient ->
                val selectedItem = getItemForIngredient(inventory, ingredient)
                val howManyPlayerHas = inventory.searchByID(selectedItem)?.qty ?: 0L
                val howManyTheRecipeWants = ingredient.qty

                RecipeIngredientRecord(selectedItem, howManyPlayerHas, howManyTheRecipeWants, hasStation)
            }
        }
    }
}