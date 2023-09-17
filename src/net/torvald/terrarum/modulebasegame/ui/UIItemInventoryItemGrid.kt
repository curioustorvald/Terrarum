package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.UIItemInventoryCatBar.Companion.CAT_ALL
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.defaultInventoryCellTheme
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.floor

/**
 * Display either extended or compact list
 *
 * Note: everything is pretty much fixed size.
 *
 * Dimension of the whole area: 552x384
 * Number of grids: 10x7
 * Number of lists: 2x7
 *
 * Created by minjaesong on 2017-10-21.
 */
open class UIItemInventoryItemGrid(
        parentUI: UICanvas,
        val catBar: UIItemInventoryCatBar,
        var getInventory: () -> FixtureInventory, // when you're going to display List of Craftables, you could implement a Delegator...? Or just build a virtual inventory
        initialX: Int,
        initialY: Int,
        val horizontalCells: Int,
        val verticalCells: Int,
        val drawScrollOnRightside: Boolean = false,
        val drawWallet: Boolean = true,
        val hideSidebar: Boolean = false,
        keyDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Keycode, extra info, self
        touchDownFun: (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit, // Item, Amount, Button, extra info, self
        protected val useHighlightingManager: Boolean = true, // only used by UIItemCraftingCandidateGrid which addresses buttons directly to set highlighting
        open protected val highlightEquippedItem: Boolean = true, // for some UIs that only cares about getting equipped slot number but not highlighting
        private val colourTheme: InventoryCellColourTheme = defaultInventoryCellTheme
) : UIItem(parentUI, initialX, initialY) {

    // deal with the moving position
    //override var oldPosX = posX
    //override var oldPosY = posY

    var numberMultiplier = 1L

    private val hash = System.nanoTime()

    override val width  = horizontalCells * UIItemInventoryElemSimple.height + (horizontalCells - 1) * listGap
    override val height = verticalCells * UIItemInventoryElemSimple.height + (verticalCells - 1) * listGap

    init {
        CommonResourcePool.addToLoadingList("inventory_walletnumberfont") {
            TextureRegionPack("./assets/graphics/fonts/inventory_wallet_numbers.tga", 20, 9)
        }
        CommonResourcePool.loadAll()
    }

    // info regarding cat icon should not be here, move it to the parent call (e.g. UIInventoryFull, CraftingUI)
    /*val catIconsMeaning = listOf( // sortedBy: catArrangement
            arrayOf(GameItem.Category.WEAPON),
            arrayOf(GameItem.Category.TOOL, GameItem.Category.WIRE),
            arrayOf(GameItem.Category.ARMOUR),
            arrayOf(GameItem.Category.GENERIC),
            arrayOf(GameItem.Category.POTION),
            arrayOf(GameItem.Category.MAGIC),
            arrayOf(GameItem.Category.BLOCK),
            arrayOf(GameItem.Category.WALL),
            arrayOf(GameItem.Category.MISC),
            arrayOf(CAT_ALL)
    )*/
    protected var currentFilter = arrayOf(CAT_ALL)

    private val inventoryUI = parentUI

    var itemPage
        set(value) {
            navRemoCon.itemPage = if (itemPageCount == 0) 0 else (value).fmod(itemPageCount)
            rebuild(currentFilter)
        }
        get() = navRemoCon.itemPage

    var itemPageCount // TODO total size of current category / items.size
        protected set(value) {
            navRemoCon.itemPageCount = value
        }
        get() = navRemoCon.itemPageCount

    var inventorySortList = ArrayList<InventoryPair>()
    protected var rebuildList = true

    protected val walletFont = TextureRegionPack("./assets/graphics/fonts/inventory_wallet_numbers.tga", 20, 9)
    protected var walletText = ""


    companion object {
        const val listGap = 8
        const val LIST_TO_CONTROL_GAP = 12

        fun getEstimatedW(horizontalCells: Int) = horizontalCells * UIItemInventoryElemSimple.height + (horizontalCells - 1) * listGap
        fun getEstimatedH(verticalCells: Int) = verticalCells * UIItemInventoryElemSimple.height + (verticalCells - 1) * listGap

        fun createInvCellGenericKeyDownFun(): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit {
            return { item: GameItem?, amount: Long, keycode: Int, _, _ ->
                if (item != null && Terrarum.ingame != null && keycode in Input.Keys.NUM_0..Input.Keys.NUM_9) {
                    val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
                    if (player != null) {
                        val inventory = player.inventory
                        val slot = if (keycode == Input.Keys.NUM_0) 9 else keycode - Input.Keys.NUM_1
                        val currentSlotItem = inventory.getQuickslotItem(slot)


                        inventory.setQuickslotItem(
                                slot,
                                if (currentSlotItem?.itm != item.dynamicID)
                                    item.dynamicID // register
                                else
                                    null // drop registration
                        )

                        // search for duplicates in the quickbar, except mine
                        // if there is, unregister the other
                        (0..9).minus(slot).forEach {
                            if (inventory.getQuickslotItem(it)?.itm == item.dynamicID) {
                                inventory.setQuickslotItem(it, null)
                            }
                        }
                    }
                }
            }
        }

        fun createInvCellGenericTouchDownFun(listRebuildFun: () -> Unit): (GameItem?, Long, Int, Any?, UIItemInventoryCellBase) -> Unit {
            return { item: GameItem?, amount: Long, button: Int, _, _ ->
                if (button == App.getConfigInt("config_mouseprimary")) {
                    if (item != null && Terrarum.ingame != null) {
                        // equip da shit
                        val itemEquipSlot = item.equipPosition
                        if (itemEquipSlot == GameItem.EquipPosition.NULL) {
                            TODO("Equip position is NULL, does this mean it's single-consume items like a potion? (from item: \"$item\" with itemID: ${item.originalID}/${item.dynamicID})")
                        }
                        val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
                        if (player != null) {

                            if (item != ItemCodex[player.inventory.itemEquipped.get(itemEquipSlot)]) { // if this item is unequipped, equip it
                                player.equipItem(item)

                                // also equip on the quickslot
                                player.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                                    player.inventory.setQuickslotItem(it, item.dynamicID)
                                }
                            }
                            else { // if not, unequip it
                                player.unequipItem(item)

                                // also unequip on the quickslot
                                player.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                                    player.inventory.setQuickslotItem(it, null)
                                }
                            }
                        }
                    }

                    listRebuildFun()
                }
            }
        }

        // COMMON variables because more than one instance of this can be up on the screen
        // This variable must be emptied out when the parent UI hides/closes
        val tooltipShowing = HashMap<Long, Boolean>() // Long: `hash` field on UIItemInventoryItemGrid
    }

    protected val itemGrid = Array<UIItemInventoryCellBase>(horizontalCells * verticalCells) {
        UIItemInventoryElemSimple(
                parentUI = inventoryUI,
                initialX = this.posX + (UIItemInventoryElemSimple.height + listGap) * (it % horizontalCells),
                initialY = this.posY + (UIItemInventoryElemSimple.height + listGap) * (it / horizontalCells),
                item = null,
                amount = UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                drawBackOnNull = true,
                keyDownFun = keyDownFun,
                touchDownFun = touchDownFun,
                highlightEquippedItem = highlightEquippedItem,
                colourTheme = colourTheme
        )
    }
    // automatically determine how much columns are needed. Minimum Width = 5 grids
    private val itemListColumnCount = floor(horizontalCells / 5f).toInt().coerceAtLeast(1)
    private val actualItemCellWidth = (listGap + UIItemInventoryElemSimple.height) * horizontalCells - listGap // in pixels
    private val largeListWidth = ((listGap + actualItemCellWidth) / itemListColumnCount) - (itemListColumnCount - 1).coerceAtLeast(1) * listGap
    protected val itemList = Array<UIItemInventoryCellBase>(verticalCells * itemListColumnCount) {
        UIItemInventoryElemWide(
                parentUI = inventoryUI,
                initialX = this.posX + (largeListWidth + listGap) * (it % itemListColumnCount),
                initialY = this.posY + (UIItemInventoryElemWide.height + listGap) * (it / itemListColumnCount),
                width = largeListWidth,
                item = null,
                amount = UIItemInventoryElemWide.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                drawBackOnNull = true,
                keyDownFun = keyDownFun,
                touchDownFun = touchDownFun,
                highlightEquippedItem = highlightEquippedItem,
                colourTheme = colourTheme
        )
    }

    var items: Array<UIItemInventoryCellBase> = itemList

    open var isCompactMode = false // this is INIT code
        set(value) {
            field = value
            items = if (value) itemGrid else itemList
            rebuild(currentFilter)
        }

    private val iconPosX = if (drawScrollOnRightside)
        posX + width
    else
        posX - UIItemListNavBarVertical.LIST_TO_CONTROL_GAP - UIItemListNavBarVertical.WIDTH - 4

    fun setCustomHighlightRuleMain(predicate: ((UIItemInventoryCellBase) -> Boolean)?) {
        itemGrid.forEach { it.customHighlightRuleMain = predicate }
        itemList.forEach { it.customHighlightRuleMain = predicate }
    }

    fun setCustomHighlightRuleSub(predicate: ((UIItemInventoryCellBase) -> Boolean)?) {
        itemGrid.forEach { it.customHighlightRule2 = predicate }
        itemList.forEach { it.customHighlightRule2 = predicate }
    }

    open fun scrollItemPage(relativeAmount: Int) {
        itemPage = if (itemPageCount == 0) 0 else (itemPage + relativeAmount).fmod(itemPageCount)
    }

    val navRemoCon = UIItemListNavBarVertical(parentUI, iconPosX, posY + 8, height, true, if (isCompactMode) 1 else 0)

    init {
        // initially highlight grid mode buttons
        if (!hideSidebar) {
            navRemoCon.listButtonListener = { _, _ ->
                isCompactMode = false
                rebuild(currentFilter)
            }
            navRemoCon.gridButtonListener = { _, _ ->
                isCompactMode = true
                rebuild(currentFilter)
            }
            navRemoCon.scrollUpListener = { _, it ->
                it.highlighted = false
                scrollItemPage(-1)
            }
            navRemoCon.scrollDownListener = { _, it ->
                it.highlighted = false
                scrollItemPage(1)
            }
            // draw wallet text
            navRemoCon.extraDrawOpOnBottom = { ui, batch ->
                if (drawWallet) {
                    batch.color = Color.WHITE
                    walletText.forEachIndexed { index, it ->
                        batch.draw(
                            walletFont.get(0, it - '0'),
                            ui.gridModeButtons[0].posX - 1f, // scroll button size: 20px, font width: 20 px
                            ui.gridModeButtons[0].posY + height - index * walletFont.tileH - 18f
                        )
                    }
                }
            }
        }
    }


//    private val upDownButtonGapToDots = 7 // apparent gap may vary depend on the texture itself

//    private fun getIconPosY(index: Int) =
//        posY + 8 + 26 * index

    override fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        val posXDelta = posX - oldPosX
        itemGrid.forEach { it.posX += posXDelta }
        itemList.forEach { it.posX += posXDelta }
        // define each button's highlighted status from the list of forceHighlighted, then render the button
        items.forEach {
            if (useHighlightingManager) it.forceHighlighted = forceHighlightList.contains(it.item?.dynamicID)
            it.render(batch, camera)
        }

        if (!hideSidebar) {
            navRemoCon.render(batch, camera)
        }

        super.render(batch, camera)

        oldPosX = posX
    }


    override fun update(delta: Float) {
        super.update(delta)


        tooltipShowing[hash] = false

//        printdbg(this, tooltipShowing.entries)

        items.forEach {
            it.update(delta)


            // set tooltip accordingly
            if ((App.IS_DEVELOPMENT_BUILD || isCompactMode) && tooltipShowing[hash] != true && it.item != null && it.mouseUp) {
//                printdbg(this, "calling INGAME.setTooltipMessage by $hash")
                INGAME.setTooltipMessage(
                        if (App.IS_DEVELOPMENT_BUILD) {
                            it.item?.name + "\n(${it.item?.originalID}${if (it.item?.originalID == it.item?.dynamicID) "" else "/${it.item?.dynamicID}"})"
                        }
                        else {
                            it.item?.name
                        }
                )

                tooltipShowing[hash] = true
//                printdbg(this, tooltipShowing.entries)
            }
        }

        if (tooltipShowing.values.all { !it }) {
            INGAME.setTooltipMessage(null)
        }


        if (!hideSidebar) {
            navRemoCon.update(delta)
        }
    }

    private val forceHighlightList = HashSet<ItemID>()
        get() {
            if (!useHighlightingManager) throw IllegalStateException("useHighlightingManager is set to false; you the programmer are in charge of managing the highlighting status of buttons by yourself!")
            return field
        }

    /**
     * Call before rebuild()
     */
    open fun clearForceHighlightList() {
        forceHighlightList.clear()
    }

    /**
     * Call before rebuild()
     */
    open fun addToForceHighlightList(items: List<ItemID>) {
        forceHighlightList.addAll(items)
    }

    open fun removeFromForceHighlightList(items: List<ItemID>) {
        forceHighlightList.removeAll(items)
    }

    open fun rebuild(filter: Array<String>) {
        //println("Rebuilt inventory")
        //println("rebuild: actual itempage: $itemPage")


        //val filter = catIconsMeaning[selectedIcon]
        currentFilter = filter

        inventorySortList.clear()

        // filter items
        getInventory().forEach {
            if ((filter.contains((ItemCodex[it.itm]?.inventoryCategory ?: throw IllegalArgumentException("Unknown item: ${it.itm}"))) || filter[0] == CAT_ALL))
                inventorySortList.add(it)
        }

        // sort if needed
        // test sort by name
        inventorySortList.sortBy { ItemCodex[it.itm]!!.name }

//        items.forEach { it.forceHighlighted = false }

        // map sortList to item list
        for (k in items.indices) {
            val item = items[k]
            // we have an item
            try {
                val sortListItem = inventorySortList[k + itemPage * items.size]
                item.item = ItemCodex[sortListItem.itm]
                item.amount = sortListItem.qty * numberMultiplier
                item.itemImage = ItemCodex.getItemImage(sortListItem.itm)

                // set quickslot number
                if (getInventory() is ActorInventory) {
                    val ainv = getInventory() as ActorInventory

                    for (qs in 1..UIQuickslotBar.SLOT_COUNT) {
                        if (sortListItem.itm == ainv.getQuickslotItem(qs - 1)?.itm) {
                            item.quickslot = qs % 10 // 10 -> 0, 1..9 -> 1..9
                            break
                        }
                        else
                            item.quickslot = null
                    }

                    // set equippedslot number
                    for (eq in ainv.itemEquipped.indices) {
                        if (eq < ainv.itemEquipped.size) {
                            if (ainv.itemEquipped[eq] == item.item?.dynamicID) {
                                item.equippedSlot = eq
                                break
                            }
                            else
                                item.equippedSlot = null
                        }
                    }
                }
            }
            // we do not have an item, empty the slot
            catch (e: IndexOutOfBoundsException) {
                item.item = null
                item.amount = 0
                item.itemImage = null
                item.quickslot = null
                item.equippedSlot = null
            }
        }


        itemPageCount = (inventorySortList.size.toFloat() / items.size.toFloat()).ceilToInt()


        // ¤   42g
        // ¤ 6969g
        // ¤ 2147483647g
        // g is read as "grave" /ɡraːv/ or /ɡɹeɪv/, because it isn't gram.
        walletText = "<;?" + getInventory().wallet.toString().padStart(4, '?') + ":"


        rebuildList = false
    }

    override fun dispose() {
        tooltipShowing.remove(hash)
    }

    override fun hide() {
        tooltipShowing.remove(hash)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)

        items.forEach { if (it.mouseUp) it.touchDown(screenX, screenY, pointer, button) }
        if (!hideSidebar) {
            navRemoCon.touchDown(screenX, screenY, pointer, button)
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchUp(screenX, screenY, pointer, button)

        items.forEach { if (it.mouseUp) it.touchUp(screenX, screenY, pointer, button) }

        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        super.keyDown(keycode)

        items.forEach { if (it.mouseUp) it.keyDown(keycode) }
        rebuild(currentFilter)

        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        super.keyUp(keycode)

        items.forEach { if (it.mouseUp) it.keyUp(keycode) }
        rebuild(currentFilter)

        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        super.scrolled(amountX, amountY)

        // scroll the item list (for now)
        if (mouseUp) {
            scrollItemPage(amountY.toInt())
        }

        return true
    }
}
