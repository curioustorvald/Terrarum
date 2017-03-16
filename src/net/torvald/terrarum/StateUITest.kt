package net.torvald.terrarum

import net.torvald.terrarum.gameactors.ActorInventory
import net.torvald.terrarum.gameactors.InventoryPair
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIHandler
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class StateUITest : BasicGameState() {

    val ui: UIHandler

    val inventory = ActorInventory()

    init {
        ui = UIHandler(SimpleUI(inventory))

        ui.posX = 50
        ui.posY = 30
        ui.isVisible = true


        inventory.add(object : InventoryItem() {
            override val id: Int = 5656
            override var originalName: String = "Test tool"
            override var baseMass: Double = 12.0
            override var baseToolSize: Double? = 8.0
            override var category: String = "tool"
            override var maxDurability: Double = 10.0
            override var durability: Double = 10.0
        })
        inventory.getByID(5656)!!.item.name = "Test tool"

        inventory.add(object : InventoryItem() {
            override val id: Int = 4633
            override var originalName: String = "CONTEXT_ITEM_QUEST_NOUN"
            override var baseMass: Double = 1.4
            override var baseToolSize: Double? = null
            override var category: String = "bulk"
        })
    }

    override fun init(container: GameContainer?, game: StateBasedGame?) {
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        ui.update(container, delta)
    }

    override fun getID() = Terrarum.STATE_ID_TEST_UI

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        ui.render(container, game, g)
    }
}







private class SimpleUI(val inventory: ActorInventory) : UICanvas {
    override var width = 700
    override var height = 480 // multiple of 40 (2 * font.lineHeight)
    override var handler: UIHandler? = null
    override var openCloseTime: Int = UICanvas.OPENCLOSE_GENERIC

    val itemImage = Image("./assets/item_kari_24.tga")

    val buttons = UIItemTextButtonList(
            this,
            arrayOf(
                    "GAME_INVENTORY_WEAPONS", // weapons and tools
                    "CONTEXT_ITEM_EQUIPMENT_PLURAL",
                    "CONTEXT_ITEM_ARMOR",
                    "GAME_INVENTORY_INGREDIENTS",
                    "GAME_INVENTORY_POTIONS",
                    "CONTEXT_ITEM_MAGIC",
                    "GAME_INVENTORY_BLOCKS",
                    "GAME_INVENTORY_WALLS",
                    "GAME_INVENTORY_FAVORITES",
                    "MENU_LABEL_ALL"
            ),
            width = (width / 3 / 100) * 100, // chop to hundreds unit (100, 200, 300, ...) with the black magic of integer division
            height = height,
            readFromLang = true,
            highlightBackCol = Color(0x202020),
            highlightBackBlendMode = BlendMode.NORMAL,
            backgroundCol = Color(0x383838),
            kinematic = true
    )

    val itemStripGutterV = 4
    val itemStripGutterH = 48

    val itemsStripWidth = width - buttons.width - 2 * itemStripGutterH
    val items = Array(height / (UIItemInventoryElem.height + itemStripGutterV), { UIItemInventoryElem(
            parentUI = this,
            posX = buttons.width + itemStripGutterH,
            posY = it * (UIItemInventoryElem.height + itemStripGutterV),
            width = itemsStripWidth,
            item = null,
            amount = UIItemInventoryElem.UNIQUE_ITEM_HAS_NO_AMOUNT,
            itemImage = null,
            backCol = Color(255, 255, 255, 0x30)
    ) })
    val itemsScrollOffset = 0

    var inventorySortList = ArrayList<InventoryPair>()
    var rebuildList = true

    override fun update(gc: GameContainer, delta: Int) {
        Terrarum.gameLocale = "en" // hot swap this to test

        buttons.update(gc, delta)


        // test fill: just copy the inventory, fuck sorting
        if (rebuildList) {
            inventorySortList = ArrayList<InventoryPair>()
            inventory.forEach { inventorySortList.add(it) }
            rebuildList = false

            // sort if needed //

            inventorySortList.forEachIndexed { index, pair ->
                if (index - itemsScrollOffset >= 0 && index < items.size + itemsScrollOffset) {
                    items[index - itemsScrollOffset].item = pair.item
                    items[index - itemsScrollOffset].amount = pair.amount
                    items[index - itemsScrollOffset].itemImage = itemImage
                }
            }
        }

    }

    override fun render(gc: GameContainer, g: Graphics) {
        g.color = Color(0x202020)
        g.fillRect(0f, 0f, width.toFloat(), height.toFloat())

        buttons.render(gc, g)


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

    override fun endClosing(gc: GameContainer, delta: Int) {7
        UICanvas.endClosingFade(handler)
    }
}