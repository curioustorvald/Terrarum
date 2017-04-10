package net.torvald.terrarum.ui

import net.torvald.terrarum.*
import net.torvald.terrarum.Terrarum.QUICKSLOT_MAX
import net.torvald.terrarum.Terrarum.joypadLabelNinA
import net.torvald.terrarum.Terrarum.joypadLabelNinY
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.langpack.Lang
import org.newdawn.slick.*
import java.util.*

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class UIInventory(
        var actor: Pocketed?,
        override var width: Int,
        override var height: Int,
        var categoryWidth: Int
) : UICanvas, MouseControlled, KeyControlled {

    val inventory: ActorInventory?
        get() = actor?.inventory
    val actorValue: ActorValue
        get() = (actor as Actor).actorValue

    override var handler: UIHandler? = null
    override var openCloseTime: Int = 120

    val itemImagePlaceholder = Image("./assets/item_kari_24.tga")

    val catButtonsToCatIdent = HashMap<String, String>()

    val backgroundColour = Color(0xA0242424.toInt())

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

    val controlHelpHeight = Terrarum.fontGame.lineHeight

    val catButtons = UIItemTextButtonList(
            this,
            arrayOf(
                    "MENU_LABEL_ALL",
                    "GAME_INVENTORY_BLOCKS",
                    "GAME_INVENTORY_WALLS",
                    "GAME_INVENTORY_WEAPONS", // weapons and tools
                    "CONTEXT_ITEM_TOOL_PLURAL",
                    "CONTEXT_ITEM_ARMOR",
                    "GAME_INVENTORY_INGREDIENTS",
                    "GAME_INVENTORY_POTIONS",
                    "CONTEXT_ITEM_MAGIC",
                    "GAME_GENRE_MISC"
                    //"GAME_INVENTORY_FAVORITES",
            ),
            width = categoryWidth,
            height = height - controlHelpHeight,
            verticalGutter = itemStripGutterH,
            readFromLang = true,
            textAreaWidth = 100,
            defaultSelection = 0,
            iconSpriteSheet = SpriteSheet("./assets/graphics/gui/inventory/category.tga", 20, 20),
            iconSpriteSheetIndices = intArrayOf(9,6,7,0,1,2,3,4,5,8),
            highlightBackCol = Color(0xb8b8b8),
            highlightBackBlendMode = BlendMode.MULTIPLY,
            backgroundCol = Color(0,0,0,0), // will use custom background colour!
            backgroundBlendMode = BlendMode.NORMAL,
            kinematic = true
    )

    val itemsStripWidth = ((width - catButtons.width) - (2 * itemStripGutterH + itemInterColGutter)) / 2
    val items = Array(
            2 + height / (UIItemInventoryElem.height + itemStripGutterV - controlHelpHeight) * 2, {
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
        ) })
    val itemsScrollOffset = 0

    var inventorySortList = ArrayList<InventoryPair>()
    private var rebuildList = true

    private val SP = "${0x3000.toChar()}${0x3000.toChar()}${0x3000.toChar()}"
    val listControlHelp: String
        get() = if (Terrarum.environment == RunningEnvironment.PC)
            "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}..${0xe019.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "${0xe034.toChar()} ${Lang["GAME_INVENTORY_DROP"]}"
    else
            "$joypadLabelNinY ${Lang["GAME_INVENTORY_USE"]}$SP" +
            "${0xe011.toChar()}${0xe019.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP" +
            "$joypadLabelNinA ${Lang["GAME_INVENTORY_DROP"]}"
    val listControlClose: String
        get() = if (Terrarum.environment == RunningEnvironment.PC)
            "${0xe037.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"
    else
            "${0xe069.toChar()} ${Lang["GAME_ACTION_CLOSE"]}"

    private var oldCatSelect = -1

    override fun update(gc: GameContainer, delta: Int) {
        catButtons.update(gc, delta)

        if (actor != null && inventory != null) {
            // monitor and check if category selection has been changed
            // OR UI is being opened from closed state
            if (oldCatSelect != catButtons.selectedIndex ||
                    !rebuildList && handler!!.openFired) {
                rebuildList = true
            }


            if (rebuildList) {
                val filter = catButtonsToCatIdent[catButtons.selectedButton.labelText]

                inventorySortList = ArrayList<InventoryPair>()

                // filter items
                inventory?.forEach {
                    if (it.item.category == filter || filter == "__all__")
                        inventorySortList.add(it)
                }

                rebuildList = false

                // sort if needed
                // test sort by name
                inventorySortList.sortBy { it.item.name }

                // map sortList to item list
                for (k in 0..items.size - 1) {
                    // we have an item
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

                        // set equippedslot number
                        for (eq in 0..actor!!.itemEquipped.size - 1) {
                            if (eq < actor!!.itemEquipped.size) {
                                if (actor!!.itemEquipped[eq] == items[k].item) {
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
                    }
                }
            }
        }


        oldCatSelect = catButtons.selectedIndex
    }

    override fun render(gc: GameContainer, g: Graphics) {
        // background
        blendNormal()
        g.color = backgroundColour
        g.fillRect(0f, 0f, width.toFloat(), height.toFloat())


        // cat bar background
        blendMul()
        g.color = Color(0xcccccc)
        g.fillRect(0f, 0f, catButtons.width.toFloat(), height.toFloat())


        catButtons.render(gc, g)


        items.forEach {
            it.render(gc, g)
        }

        // texts
        blendNormal()
        g.color = Color(0xdddddd)
        Typography.printCentered(g, listControlHelp, catButtons.width, height - controlHelpHeight, width - catButtons.width)
        Typography.printCentered(g, listControlClose, 0, height - controlHelpHeight, catButtons.width)
    }


    /** Persuade the UI to rebuild its item list */
    fun rebuildList() {
        rebuildList = true
    }




    ////////////
    // Inputs //
    ////////////

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        handler!!.posX = Movement.fastPullOut(
                handler!!.openCloseCounter.toFloat() / openCloseTime,
                -width.toFloat(),
                0f
        ).roundInt()
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        handler!!.posX = Movement.fastPullOut(
                handler!!.openCloseCounter.toFloat() / openCloseTime,
                0f,
                -width.toFloat()
        ).roundInt()
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        handler!!.posX = 0
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        handler!!.posX = -width
    }

    override fun keyPressed(key: Int, c: Char) {
        items.forEach { it.keyPressed(key, c) }
    }

    override fun mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    }

    override fun keyReleased(key: Int, c: Char) {
    }

    override fun mouseDragged(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
    }

    override fun mousePressed(button: Int, x: Int, y: Int) {
        items.forEach { if (it.mouseUp) it.mousePressed(button, x, y) }
    }

    override fun controllerButtonReleased(controller: Int, button: Int) {
    }

    override fun mouseReleased(button: Int, x: Int, y: Int) {
        items.forEach { if (it.mouseUp) it.mouseReleased(button, x, y) }
    }

    override fun mouseWheelMoved(change: Int) {
    }
}
