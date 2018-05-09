package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.UIItemInventoryElem
import net.torvald.terrarum.UIItemInventoryElemSimple
import net.torvald.terrarum.gameactors.ActorInventory
import net.torvald.terrarum.gameactors.InventoryPair
import net.torvald.terrarum.gameactors.ceilInt
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import java.util.ArrayList

/**
 * Display either extended or compact list
 *
 * Note: everything is pretty much fixed size.
 *
 * Dimension of the whole area: 496x384
 * Number of grids: 9x7
 * Number of lists: 2x7
 *
 * Created by minjaesong on 2017-10-21.
 */
class UIItemInventoryDynamicList(
        parentUI: UIInventoryFull,
        val inventory: ActorInventory,
        override var posX: Int,
        override var posY: Int
) : UIItem(parentUI) {

    override val width  = 496
    override val height = 384

    private val catArrangement = parentUI.catArrangement



    val catIconsMeaning = listOf( // sortedBy: catArrangement
            GameItem.Category.WEAPON,
            GameItem.Category.TOOL,
            GameItem.Category.ARMOUR,
            GameItem.Category.GENERIC,
            GameItem.Category.POTION,
            GameItem.Category.MAGIC,
            GameItem.Category.BLOCK,
            GameItem.Category.WALL,
            GameItem.Category.MISC,
            "__all__"
    )

    private val inventoryUI = parentUI

    private val selection: Int
        get() = inventoryUI.catSelection
    private val selectedIcon: Int
        get() = inventoryUI.catSelectedIcon

    private val compactViewCat = setOf(3, 4, 6, 7, 9) // ingredients, potions, blocks, walls, all (spritesheet order)

    var itemPage = 0
    var itemPageCount = 1 // TODO total size of current category / items.size
        private set

    var inventorySortList = ArrayList<InventoryPair>()
    private var rebuildList = true

    val defaultTextColour = Color(0xeaeaea_ff.toInt())

    private val listGap = 8
    private val itemList = Array<UIItemInventoryCellBase>(
            7 * 2, {
        UIItemInventoryElem(
                parentUI = inventoryUI,
                posX = this.posX + (244 + listGap) * (it % 2),
                posY = this.posY + (UIItemInventoryElem.height + listGap) * (it / 2),
                width = 244,
                item = null,
                amount = UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                mouseoverBackCol = Color(0x282828_ff),
                mouseoverBackBlendMode = BlendMode.SCREEN,
                backCol = Color(0x404040_88),
                backBlendMode = BlendMode.NORMAL,
                drawBackOnNull = true,
                inactiveTextCol = defaultTextColour
        ) })
    private val itemGrid = Array<UIItemInventoryCellBase>(
            7 * 9, {
        UIItemInventoryElemSimple(
                parentUI = inventoryUI,
                posX = this.posX + (UIItemInventoryElemSimple.height + listGap) * (it % 9),
                posY = this.posY + (UIItemInventoryElemSimple.height + listGap) * (it / 9),
                item = null,
                amount = UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                mouseoverBackCol = Color(0x282828_ff),
                mouseoverBackBlendMode = BlendMode.SCREEN,
                backCol = Color(0x404040_88),
                backBlendMode = BlendMode.NORMAL,
                drawBackOnNull = true,
                inactiveTextCol = defaultTextColour
        )
    }
    )

    private var items: Array<UIItemInventoryCellBase>
            = if (catArrangement[selection] in compactViewCat) itemGrid else itemList // this is INIT code

    var isCompactMode = (catArrangement[selection] in compactViewCat) // this is INIT code
        set(value) {
            items = if (value) itemGrid else itemList

            rebuild()

            field = value
        }


    private val iconPosX = posX - 12 - parentUI.catIcons.tileW + 2
    private fun getIconPosY(index: Int) =
            posY - 2 + (4 + UIItemInventoryElem.height - (parentUI as UIInventoryFull).catIcons.tileH) * index

    /** Long/compact mode buttons */
    private val gridModeButtons = Array<UIItemImageButton>(2, { index ->
        UIItemImageButton(
                parentUI,
                parentUI.catIcons.get(index + 14, 0),
                activeBackCol = Color(0),
                activeBackBlendMode = BlendMode.NORMAL,
                posX = iconPosX,
                posY = getIconPosY(index),
                highlightable = true
        )
    })

    private val scrollUpButton = UIItemImageButton(
            parentUI,
            parentUI.catIcons.get(18, 0),
            activeBackCol = Color(0),
            activeBackBlendMode = BlendMode.NORMAL,
            posX = iconPosX,
            posY = getIconPosY(2),
            highlightable = false
    )

    private val scrollDownButton = UIItemImageButton(
            parentUI,
            parentUI.catIcons.get(19, 0),
            activeBackCol = Color(0),
            activeBackBlendMode = BlendMode.NORMAL,
            posX = iconPosX,
            posY = getIconPosY(3),
            highlightable = false
    )


    init {
        // initially highlight grid mode buttons
        gridModeButtons[if (isCompactMode) 1 else 0].highlighted = true


        gridModeButtons[0].touchDownListener = { _, _, _, _ ->
            isCompactMode = false
            gridModeButtons[0].highlighted = true
            gridModeButtons[1].highlighted = false
            itemPage = 0
            rebuild()
        }
        gridModeButtons[1].touchDownListener = { _, _, _, _ ->
            isCompactMode = true
            gridModeButtons[0].highlighted = false
            gridModeButtons[1].highlighted = true
            itemPage = 0
            rebuild()
        }

        scrollUpButton.clickOnceListener = { _, _, _ ->
            if (itemPage > 0) itemPage--
            scrollUpButton.highlighted = false
            rebuild()
        }
        scrollDownButton.clickOnceListener = { _, _, _ ->
            if (itemPage < itemPageCount - 1) itemPage++
            scrollDownButton.highlighted = false
            rebuild()
        }

        // if (is.mouseUp) handled by this.touchDown()
    }


    private val upDownButtonGapToDots = 7 // apparent gap may vary depend on the texture itself

    override fun render(batch: SpriteBatch, camera: Camera) {
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
                    (parentUI as UIInventoryFull).catIcons.get(20,0),
                    scrollUpButton.posX.toFloat(),
                    getScrollDotYHeight(i).toFloat()
            )
        }

        super.render(batch, camera)
    }


    override fun update(delta: Float) {
        super.update(delta)

        var tooltipSet = false



        items.forEach {
            it.update(delta)


            // set tooltip accordingly
            if (isCompactMode && it.mouseUp && !tooltipSet) {
                Terrarum.ingame?.setTooltipMessage(it.item?.name)
                tooltipSet = true
            }
        }

        if (!tooltipSet) {
            Terrarum.ingame?.setTooltipMessage(null)
        }



        gridModeButtons.forEach { it.update(delta) }
        scrollUpButton.update(delta)
        scrollDownButton.update(delta)
    }



    internal fun rebuild() {
        //println("Rebuilt inventory")
        //println("rebuild: actual itempage: $itemPage")


        val filter = catIconsMeaning[selectedIcon]

        inventorySortList = ArrayList<InventoryPair>()

        // filter items
        inventory.forEach {
            if (it.item.inventoryCategory == filter || filter == "__all__")
                inventorySortList.add(it)
        }

        // sort if needed
        // test sort by name
        inventorySortList.sortBy { it.item.name }

        // map sortList to item list
        for (k in 0 until items.size) {
            // we have an item
            try {
                val sortListItem = inventorySortList[k + itemPage * items.size]
                items[k].item = sortListItem.item
                items[k].amount = sortListItem.amount
                items[k].itemImage = ItemCodex.getItemImage(sortListItem.item)

                // set quickslot number
                for (qs in 1..UIQuickBar.SLOT_COUNT) {
                    if (sortListItem.item == inventory.getQuickBar(qs - 1)?.item) {
                        items[k].quickslot = qs % 10 // 10 -> 0, 1..9 -> 1..9
                        break
                    }
                    else
                        items[k].quickslot = null
                }

                // set equippedslot number
                for (eq in 0 until inventory.itemEquipped.size) {
                    if (eq < inventory.itemEquipped.size) {
                        if (inventory.itemEquipped[eq] == items[k].item) {
                            items[k].equippedSlot = eq
                            break
                        }
                        else
                            items[k].equippedSlot = null
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


        rebuildList = false
    }

    override fun dispose() {
        itemList.forEach { it.dispose() }
        itemGrid.forEach { it.dispose() }
        gridModeButtons.forEach { it.dispose() }
        scrollUpButton.dispose()
        scrollDownButton.dispose()
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
        items.forEach { if (it.mouseUp) it.touchUp(screenX, screenY, pointer, button) }

        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        super.keyDown(keycode)

        items.forEach { if (it.mouseUp) it.keyDown(keycode) }
        rebuild()

        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        super.keyUp(keycode)

        items.forEach { if (it.mouseUp) it.keyUp(keycode) }
        rebuild()

        return true
    }
}
