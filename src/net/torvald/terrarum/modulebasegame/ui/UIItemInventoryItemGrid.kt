package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.Input
import net.torvald.terrarum.*
import net.torvald.terrarum.UIItemInventoryCatBar.Companion.CAT_ALL
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorInventory
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.InventoryPair
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory.CELLCOLOUR_BLACK
import net.torvald.terrarum.modulebasegame.ui.ItemSlotImageFactory.CELLCOLOUR_BLACK_ACTIVE
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVEN_DEBUG_MODE
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemImageButton
import net.torvald.terrarum.ui.UIItemTextButton.Companion.defaultActiveCol
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*
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
class UIItemInventoryItemGrid(
        parentUI: UICanvas,
        val catBar: UIItemInventoryCatBar,
        val inventory: FixtureInventory, // when you're going to display List of Craftables, you could implement a Delegator...? Or just build a virtual inventory
        initialX: Int,
        initialY: Int,
        val horizontalCells: Int,
        val verticalCells: Int,
        val drawScrollOnRightside: Boolean = false,
        val drawWallet: Boolean = true,
        keyDownFun: (GameItem?, Int, Int) -> Unit,
        touchDownFun: (GameItem?, Int, Int) -> Unit
) : UIItem(parentUI, initialX, initialY) {

    // deal with the moving position
    //override var oldPosX = posX
    //override var oldPosY = posY

    override val width  = horizontalCells * UIItemInventoryElemSimple.height + (horizontalCells - 1) * listGap
    override val height = verticalCells * UIItemInventoryElemSimple.height + (verticalCells - 1) * listGap

    val backColour = CELLCOLOUR_BLACK

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
    private var currentFilter = arrayOf(CAT_ALL)

    private val inventoryUI = parentUI

    var itemPage = 0
        set(value) {
            field = if (itemPageCount == 0) 0 else (value).fmod(itemPageCount)
            rebuild(currentFilter)
        }
    var itemPageCount = 1 // TODO total size of current category / items.size
        private set

    var inventorySortList = ArrayList<InventoryPair>()
    private var rebuildList = true

    val defaultTextColour = Color(0xeaeaea_ff.toInt())

    private val walletFont = TextureRegionPack("./assets/graphics/fonts/inventory_wallet_numbers.tga", 20, 9)
    private var walletText = ""



    companion object {
        const val listGap = 8
        const val LIST_TO_CONTROL_GAP = 12

        fun getEstimatedW(horizontalCells: Int) = horizontalCells * UIItemInventoryElemSimple.height + (horizontalCells - 1) * listGap
        fun getEstimatedH(verticalCells: Int) = verticalCells * UIItemInventoryElemSimple.height + (verticalCells - 1) * listGap

        fun createInvCellGenericKeyDownFun(): (GameItem?, Int, Int) -> Unit {
            return { item: GameItem?, amount: Int, keycode: Int ->
                if (item != null && Terrarum.ingame != null && keycode in Input.Keys.NUM_0..Input.Keys.NUM_9) {
                    val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
                    if (player != null) {
                        val inventory = player.inventory
                        val slot = if (keycode == Input.Keys.NUM_0) 9 else keycode - Input.Keys.NUM_1
                        val currentSlotItem = inventory.getQuickslot(slot)


                        inventory.setQuickBar(
                                slot,
                                if (currentSlotItem?.item != item.dynamicID)
                                    item.dynamicID // register
                                else
                                    null // drop registration
                        )

                        // search for duplicates in the quickbar, except mine
                        // if there is, unregister the other
                        (0..9).minus(slot).forEach {
                            if (inventory.getQuickslot(it)?.item == item.dynamicID) {
                                inventory.setQuickBar(it, null)
                            }
                        }
                    }
                }
            }
        }

        fun createInvCellGenericTouchDownFun(listRebuildFun: () -> Unit): (GameItem?, Int, Int) -> Unit {
            return { item: GameItem?, amount: Int, button: Int ->
                if (item != null && Terrarum.ingame != null) {
                    // equip da shit
                    val itemEquipSlot = item.equipPosition
                    if (itemEquipSlot == GameItem.EquipPosition.NULL) {
                        TODO("Equip position is NULL, does this mean it's single-consume items like a potion? (from item: \"$item\" with itemID: ${item?.originalID}/${item?.dynamicID})")
                    }
                    val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
                    if (player != null) {

                        if (item != ItemCodex[player.inventory.itemEquipped.get(itemEquipSlot)]) { // if this item is unequipped, equip it
                            player.equipItem(item)

                            // also equip on the quickslot
                            player.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                                player.inventory.setQuickBar(it, item.dynamicID)
                            }
                        }
                        else { // if not, unequip it
                            player.unequipItem(item)

                            // also unequip on the quickslot
                            player.actorValue.getAsInt(AVKey.__PLAYER_QUICKSLOTSEL)?.let {
                                player.inventory.setQuickBar(it, null)
                            }
                        }
                    }
                }

                listRebuildFun()
            }
        }
    }

    private val itemGrid = Array<UIItemInventoryCellBase>(horizontalCells * verticalCells) {
        UIItemInventoryElemSimple(
                parentUI = inventoryUI,
                initialX = this.posX + (UIItemInventoryElemSimple.height + listGap) * (it % horizontalCells),
                initialY = this.posY + (UIItemInventoryElemSimple.height + listGap) * (it / horizontalCells),
                item = null,
                amount = UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                mouseoverBackCol = Color(CELLCOLOUR_BLACK_ACTIVE),
                mouseoverBackBlendMode = BlendMode.SCREEN,
                backCol = backColour,
                backBlendMode = BlendMode.NORMAL,
                drawBackOnNull = true,
                inactiveTextCol = defaultTextColour,
                keyDownFun = keyDownFun,
                touchDownFun = touchDownFun
        )
    }
    // automatically determine how much columns are needed. Minimum Width = 5 grids
    private val itemListColumnCount = floor(horizontalCells / 5f).toInt().coerceAtLeast(1)
    private val actualItemCellWidth = (listGap + UIItemInventoryElemSimple.height) * horizontalCells - listGap // in pixels
    private val largeListWidth = ((listGap + actualItemCellWidth) / itemListColumnCount) - (itemListColumnCount - 1).coerceAtLeast(1) * listGap
    private val itemList = Array<UIItemInventoryCellBase>(verticalCells * itemListColumnCount) {
        UIItemInventoryElem(
                parentUI = inventoryUI,
                initialX = this.posX + (largeListWidth + listGap) * (it % itemListColumnCount),
                initialY = this.posY + (UIItemInventoryElem.height + listGap) * (it / itemListColumnCount),
                width = largeListWidth,
                item = null,
                amount = UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                mouseoverBackCol = Color(CELLCOLOUR_BLACK_ACTIVE),
                mouseoverBackBlendMode = BlendMode.SCREEN,
                backCol = backColour,
                backBlendMode = BlendMode.NORMAL,
                drawBackOnNull = true,
                inactiveTextCol = defaultTextColour,
                keyDownFun = keyDownFun,
                touchDownFun = touchDownFun
        )
    }

    private var items: Array<UIItemInventoryCellBase> = itemList

    var isCompactMode = false // this is INIT code
        set(value) {
            items = if (value) itemGrid else itemList
            rebuild(currentFilter)
            field = value
        }

    private val iconPosX = if (drawScrollOnRightside)
        posX + width + LIST_TO_CONTROL_GAP
    else
        posX - LIST_TO_CONTROL_GAP - catBar.catIcons.tileW + 2

    private fun getIconPosY(index: Int) =
            posY - 2 + (4 + UIItemInventoryElem.height - catBar.catIcons.tileH) * index

    /** Long/compact mode buttons */
    private val gridModeButtons = Array<UIItemImageButton>(2) { index ->
        UIItemImageButton(
                parentUI,
                catBar.catIcons.get(index + 14, 0),
                backgroundCol = Color(0),
                activeBackCol = Color(0),
                highlightBackCol = Color(0),
                activeBackBlendMode = BlendMode.NORMAL,
                activeCol = defaultActiveCol,
                initialX = iconPosX,
                initialY = getIconPosY(index),
                highlightable = true
        )
    }

    private val scrollUpButton = UIItemImageButton(
            parentUI,
            catBar.catIcons.get(18, 0),
            backgroundCol = Color(0),
            activeBackCol = Color(0),
            activeBackBlendMode = BlendMode.NORMAL,
            activeCol = defaultActiveCol,
            initialX = iconPosX,
            initialY = getIconPosY(2),
            highlightable = false
    )

    private val scrollDownButton = UIItemImageButton(
            parentUI,
            catBar.catIcons.get(19, 0),
            backgroundCol = Color(0),
            activeBackCol = Color(0),
            activeBackBlendMode = BlendMode.NORMAL,
            activeCol = defaultActiveCol,
            initialX = iconPosX,
            initialY = getIconPosY(3),
            highlightable = false
    )

    fun scrollItemPage(relativeAmount: Int) {
        itemPage = if (itemPageCount == 0) 0 else (itemPage + relativeAmount).fmod(itemPageCount)
    }

    init {
        // initially highlight grid mode buttons
        gridModeButtons[if (isCompactMode) 1 else 0].highlighted = true


        gridModeButtons[0].touchDownListener = { _, _, _, _ ->
            isCompactMode = false
            gridModeButtons[0].highlighted = true
            gridModeButtons[1].highlighted = false
            itemPage = 0
            rebuild(currentFilter)
        }
        gridModeButtons[1].touchDownListener = { _, _, _, _ ->
            isCompactMode = true
            gridModeButtons[0].highlighted = false
            gridModeButtons[1].highlighted = true
            itemPage = 0
            rebuild(currentFilter)
        }

        scrollUpButton.clickOnceListener = { _, _, _ ->
            scrollUpButton.highlighted = false
            scrollItemPage(-1)
        }
        scrollDownButton.clickOnceListener = { _, _, _ ->
            scrollDownButton.highlighted = false
            scrollItemPage(1)
        }

        // if (is.mouseUp) handled by this.touchDown()
    }


    private val upDownButtonGapToDots = 7 // apparent gap may vary depend on the texture itself

    override fun render(batch: SpriteBatch, camera: Camera) {
        val posXDelta = posX - oldPosX
        itemGrid.forEach { it.posX += posXDelta }
        itemList.forEach { it.posX += posXDelta }
        gridModeButtons.forEach { it.posX += posXDelta }
        scrollUpButton.posX += posXDelta
        scrollDownButton.posX += posXDelta


        fun getScrollDotYHeight(i: Int) = scrollUpButton.posY + 10 + upDownButtonGapToDots + 10 * i


        scrollDownButton.posY = getScrollDotYHeight(itemPageCount) + upDownButtonGapToDots



        items.forEach { it.render(batch, camera) }

        gridModeButtons.forEach { it.render(batch, camera) }
        scrollUpButton.render(batch, camera)
        scrollDownButton.render(batch, camera)

        // draw scroll dots
        for (i in 0 until itemPageCount) {
            val colour = if (i == itemPage) Color.WHITE else Color(0xffffff7f.toInt())

            batch.color = colour
            batch.draw(
                    catBar.catIcons.get(if (i == itemPage) 20 else 21, 0),
                    scrollUpButton.posX.toFloat(),
                    getScrollDotYHeight(i).toFloat()
            )
        }

        // draw wallet text
        if (drawWallet) {
            batch.color = Color.WHITE
            walletText.forEachIndexed { index, it ->
                batch.draw(
                        walletFont.get(0, it - '0'),
                        gridModeButtons[0].posX.toFloat(), // scroll button size: 20px, font width: 20 px
                        gridModeButtons[0].posY + height - index * walletFont.tileH.toFloat()
                )
            }
        }

        super.render(batch, camera)

        oldPosX = posX
    }


    override fun update(delta: Float) {
        super.update(delta)

        var tooltipSet = false



        items.forEach {
            it.update(delta)


            // set tooltip accordingly
            if (isCompactMode && it.item != null && it.mouseUp && !tooltipSet) {
                (Terrarum.ingame as? TerrarumIngame)?.setTooltipMessage(
                        if (INVEN_DEBUG_MODE) {
                            it.item?.name + "/Mat: ${it.item?.material?.identifier}"
                        }
                        else {
                            it.item?.name
                        }
                )
                tooltipSet = true
            }
        }

        if (!tooltipSet) {
            (Terrarum.ingame as? TerrarumIngame)?.setTooltipMessage(null)
        }



        gridModeButtons.forEach { it.update(delta) }
        scrollUpButton.update(delta)
        scrollDownButton.update(delta)
    }


    internal fun rebuild(filter: Array<String>) {
        //println("Rebuilt inventory")
        //println("rebuild: actual itempage: $itemPage")


        //val filter = catIconsMeaning[selectedIcon]
        currentFilter = filter

        inventorySortList = ArrayList<InventoryPair>()

        // filter items
        inventory.forEach {
            if ((filter.contains(ItemCodex[it.item]!!.inventoryCategory) || filter[0] == CAT_ALL))
                inventorySortList.add(it)
        }

        // sort if needed
        // test sort by name
        inventorySortList.sortBy { ItemCodex[it.item]!!.name }

        // map sortList to item list
        for (k in items.indices) {
            // we have an item
            try {
                val sortListItem = inventorySortList[k + itemPage * items.size]
                items[k].item = ItemCodex[sortListItem.item]
                items[k].amount = sortListItem.amount
                items[k].itemImage = ItemCodex.getItemImage(sortListItem.item)

                // set quickslot number
                if (inventory is ActorInventory) {
                    for (qs in 1..UIQuickslotBar.SLOT_COUNT) {
                        if (sortListItem.item == inventory.getQuickslot(qs - 1)?.item) {
                            items[k].quickslot = qs % 10 // 10 -> 0, 1..9 -> 1..9
                            break
                        }
                        else
                            items[k].quickslot = null
                    }

                    // set equippedslot number
                    for (eq in inventory.itemEquipped.indices) {
                        if (eq < inventory.itemEquipped.size) {
                            if (inventory.itemEquipped[eq] == items[k].item?.dynamicID) {
                                items[k].equippedSlot = eq
                                break
                            }
                            else
                                items[k].equippedSlot = null
                        }
                    }
                }
            }
            // we do not have an item, empty the slot
            catch (e: IndexOutOfBoundsException) {
                items[k].item = null
                items[k].amount = 0
                items[k].itemImage = null
                items[k].quickslot = null
                items[k].equippedSlot = null
            }
        }


        itemPageCount = (inventorySortList.size.toFloat() / items.size.toFloat()).ceilInt()


        // ¤   42g
        // ¤ 6969g
        // ¤ 2147483647g
        // g is read as "grave" /ɡraːv/ or /ɡɹeɪv/, because it isn't gram.
        walletText = "<;?" + inventory.wallet.toString().padStart(4, '?') + ":"


        rebuildList = false
    }

    override fun dispose() {
        itemList.forEach { it.dispose() }
        itemGrid.forEach { it.dispose() }
        // the icons are using common resources that are disposed when the app quits
        //gridModeButtons.forEach { it.dispose() }
        //scrollUpButton.dispose()
        //scrollDownButton.dispose()
        //walletFont.dispose()
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        super.touchDown(screenX, screenY, pointer, button)

        items.forEach { if (it.mouseUp) it.touchDown(screenX, screenY, pointer, button) }
        gridModeButtons.forEach { if (it.mouseUp) it.touchDown(screenX, screenY, pointer, button) }
        if (scrollUpButton.mouseUp) scrollUpButton.touchDown(screenX, screenY, pointer, button)
        if (scrollDownButton.mouseUp) scrollDownButton.touchDown(screenX, screenY, pointer, button)
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

    override fun scrolled(amount: Int): Boolean {
        super.scrolled(amount)

        // scroll the item list (for now)
        if (mouseUp) {
            scrollItemPage(amount)
        }

        return true
    }
}
