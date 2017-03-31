package net.torvald.terrarum.ui

import net.torvald.terrarum.*
import net.torvald.terrarum.Terrarum.QUICKSLOT_MAX
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitem.InventoryItem
import org.newdawn.slick.*
import java.util.*

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class UIInventory(
        val actor: Pocketed,
        override var width: Int,
        override var height: Int
) : UICanvas {

    val inventory: ActorInventory
        get() = actor.inventory
    val actorValue: ActorValue
        get() = (actor as Actor).actorValue

    override var handler: UIHandler? = null
    override var openCloseTime: Int = UICanvas.OPENCLOSE_GENERIC

    val itemImagePlaceholder = Image("./assets/item_kari_24.tga")

    val catButtonsToCatIdent = HashMap<String, String>()

    val backgroundColour = Color(0x1c1c1c)

    init {
        catButtonsToCatIdent.put("GAME_INVENTORY_WEAPONS", InventoryItem.Category.WEAPON)
        catButtonsToCatIdent.put("CONTEXT_ITEM_TOOL_PLURAL", InventoryItem.Category.TOOL)
        catButtonsToCatIdent.put("CONTEXT_ITEM_ARMOR", InventoryItem.Category.ARMOUR)
        catButtonsToCatIdent.put("GAME_INVENTORY_INGREDIENTS", InventoryItem.Category.GENERIC)
        catButtonsToCatIdent.put("GAME_INVENTORY_POTIONS", InventoryItem.Category.POTION)
        catButtonsToCatIdent.put("CONTEXT_ITEM_MAGIC", InventoryItem.Category.MAGIC)
        catButtonsToCatIdent.put("GAME_INVENTORY_BLOCKS", InventoryItem.Category.BLOCK)
        catButtonsToCatIdent.put("GAME_INVENTORY_WALLS", InventoryItem.Category.WALL)
        catButtonsToCatIdent.put("GAME_GENRE_MISC", InventoryItem.Category.MISC)

        // special filter
        catButtonsToCatIdent.put("MENU_LABEL_ALL", "__all__")

    }

    val itemStripGutterV = 6
    val itemStripGutterH = 8
    val itemInterColGutter = 8

    val catButtons = UIItemTextButtonList(
            this,
            arrayOf(
                    "MENU_LABEL_ALL",
                    "GAME_INVENTORY_WEAPONS", // weapons and tools
                    "CONTEXT_ITEM_TOOL_PLURAL",
                    "CONTEXT_ITEM_ARMOR",
                    "GAME_INVENTORY_INGREDIENTS",
                    "GAME_INVENTORY_POTIONS",
                    "CONTEXT_ITEM_MAGIC",
                    "GAME_INVENTORY_BLOCKS",
                    "GAME_INVENTORY_WALLS",
                    "GAME_GENRE_MISC"
                    //"GAME_INVENTORY_FAVORITES",
            ),
            width = (width / 3 / 100) * 100, // chop to hundreds unit (100, 200, 300, ...) with the black magic of integer division
            height = height,
            verticalGutter = itemStripGutterH,
            readFromLang = true,
            textAreaWidth = 100,
            defaultSelection = 0,
            iconSpriteSheet = SpriteSheet("./assets/graphics/gui/inventory/category.tga", 20, 20),
            iconSpriteSheetIndices = intArrayOf(9,0,1,2,3,4,5,6,7,8),
            highlightBackCol = backgroundColour screen Color(0x0c0c0c),
            highlightBackBlendMode = BlendMode.NORMAL,
            backgroundCol = Color(0x383838),
            kinematic = true
    )

    val itemsStripWidth = ((width - catButtons.width) - (2 * itemStripGutterH + itemInterColGutter)) / 2
    val items = Array(
            2 + height / (UIItemInventoryElem.height + itemStripGutterV) * 2, {
        UIItemInventoryElem(
                parentUI = this,
                posX = catButtons.width + if (it % 2 == 0) itemStripGutterH else (itemStripGutterH + itemsStripWidth + itemInterColGutter),
                posY = itemStripGutterH + it / 2 * (UIItemInventoryElem.height + itemStripGutterV),
                width = itemsStripWidth,
                item = null,
                amount = UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT,
                itemImage = null,
                mouseoverBackCol = Color(0x282828),
                mouseoverBackBlendMode = BlendMode.SCREEN,
                drawBackOnNull = false
                //backCol = Color(0x101010),
                //backBlendMode = BlendMode.SCREEN
        ) })
    val itemsScrollOffset = 0

    var inventorySortList = ArrayList<InventoryPair>()
    var rebuildList = true

    private var oldCatSelect = -1

    override fun update(gc: GameContainer, delta: Int) {
        Terrarum.gameLocale = "koKR" // hot swap this to test

        catButtons.update(gc, delta)


        // monitor and check if category selection has been changed
        if (oldCatSelect != catButtons.selectedIndex) {
            rebuildList = true
        }


        if (rebuildList) {
            val filter = catButtonsToCatIdent[catButtons.selectedButton.labelText]

            inventorySortList = ArrayList<InventoryPair>()

            // filter items
            inventory.forEach {
                if (it.item.category == filter || filter == "__all__")
                    inventorySortList.add(it)
            }

            rebuildList = false

            // sort if needed
            // test sort by name
            inventorySortList.sortBy { it.item.name }

            // map sortList to item list
            for (k in 0..items.size - 1) {
                try {
                    val sortListItem = inventorySortList[k + itemsScrollOffset]
                    items[k].item = sortListItem.item
                    items[k].amount = sortListItem.amount
                    items[k].itemImage = itemImagePlaceholder

                    // set quickslot number
                    for (qs in 1..QUICKSLOT_MAX) {
                        if (-sortListItem.item.id == actorValue.getAsInt(AVKey.__PLAYER_QSPREFIX + qs)) {
                            items[k].quickslot = qs % 10 // 10 -> 0, 1..9 -> 1..9
                            break
                        }
                        else
                            items[k].quickslot = null
                    }

                    for (eq in 0..actor.itemEquipped.size - 1) {
                        if (eq < actor.itemEquipped.size) {
                            if (actor.itemEquipped[eq] == items[k].item) {
                                items[k].equippedSlot = eq
                                break
                            }
                            else
                                items[k].equippedSlot = null
                        }
                    }
                }
                catch (e: IndexOutOfBoundsException) {
                    items[k].item = null
                    items[k].amount = 0
                    items[k].itemImage = null
                    items[k].quickslot = null
                }
            }
        }


        oldCatSelect = catButtons.selectedIndex
    }

    override fun render(gc: GameContainer, g: Graphics) {
        g.color = backgroundColour
        g.fillRect(0f, 0f, width.toFloat(), height.toFloat())

        catButtons.render(gc, g)


        items.forEach {
            it.render(gc, g)
        }
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        UICanvas.doOpeningFade(handler, openCloseTime)
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        UICanvas.doClosingFade(handler, openCloseTime)
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        UICanvas.endOpeningFade(handler)
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        UICanvas.endClosingFade(handler)
    }
}
