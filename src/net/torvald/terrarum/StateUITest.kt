package net.torvald.terrarum

import net.torvald.terrarum.gameactors.ActorInventory
import net.torvald.terrarum.gameactors.InventoryPair
import net.torvald.terrarum.gameitem.IVKey
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIHandler
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.*

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class StateUITest : BasicGameState() {

    val ui: UIHandler

    val inventory = ActorInventory()

    init {
        ui = UIHandler(SimpleUI(inventory, 800, Terrarum.HEIGHT - 160))

        ui.posX = 0
        ui.posY = 60
        ui.isVisible = true


        // these are the test codes.
        // Item properties must be pre-composed using CSV/JSON, and read and made into the item instance
        // using factory/builder pattern. @see ItemCodex
        inventory.add(object : InventoryItem() {
            init {
                itemProperties[IVKey.ITEMTYPE] = IVKey.ItemType.HAMMER
            }
            override val id: Int = 5656
            override val isUnique: Boolean = true
            override var originalName: String = "Test tool"
            override var baseMass: Double = 12.0
            override var baseToolSize: Double? = 8.0
            override var category: String = InventoryItem.Category.TOOL
            override var maxDurability: Double = 10.0
            override var durability: Double = 6.43
        })
        inventory.getByID(5656)!!.item.name = "Test tool"

        inventory.add(object : InventoryItem() {
            init {
                itemProperties[IVKey.ITEMTYPE] = IVKey.ItemType.ARTEFACT
            }
            override val id: Int = 4633
            override val isUnique: Boolean = true
            override var originalName: String = "CONTEXT_ITEM_QUEST_NOUN"
            override var baseMass: Double = 1.4
            override var baseToolSize: Double? = null
            override var category: String = InventoryItem.Category.MISC
        })

        inventory.add(ItemCodex[16], 543)
    }

    override fun init(container: GameContainer?, game: StateBasedGame?) {
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        Terrarum.appgc.setTitle("${Terrarum.NAME} — F: ${Terrarum.appgc.fps}")

        ui.update(container, delta)
    }

    override fun getID() = Terrarum.STATE_ID_TEST_UI1

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        ui.render(container, game, g)
    }
}



private class SimpleUI(
        val inventory: ActorInventory,
        override var width: Int,
        override var height: Int
) : UICanvas {
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

                    items[k].quickslot = Random().nextInt(10) // TODO test
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
