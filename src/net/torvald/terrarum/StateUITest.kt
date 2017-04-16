package net.torvald.terrarum

import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitem.IVKey
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.ui.*
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class StateUITest : BasicGameState() {

    val ui: UIHandler

    private val actor = object : Actor(RenderOrder.FRONT), Pocketed {
        override fun update(gc: GameContainer, delta: Int) {
            TODO("not implemented")
        }

        override fun run() {
            TODO("not implemented")
        }

        override var inventory: ActorInventory = ActorInventory(this, 100, ActorInventory.CAPACITY_MODE_WEIGHT)
    }

    init {
        ui = UIHandler(UIInventory(actor, 900, Terrarum.HEIGHT - 160, 220))

        ui.posX = 0
        ui.posY = 60
        ui.isVisible = true



        // these are the test codes.
        // Item properties must be pre-composed using CSV/JSON, and read and made into the item instance
        // using factory/builder pattern. @see ItemCodex
        actor.actorValue[AVKey.__PLAYER_QSPREFIX + "3"] = Tile.STONE

        actor.inventory.add(object : InventoryItem() {
            init {
                itemProperties[IVKey.ITEMTYPE] = IVKey.ItemType.HAMMER
            }
            override val id: Int = 5656
            override val isUnique: Boolean = true
            override var originalName: String = "Test tool"
            override var baseMass: Double = 12.0
            override var baseToolSize: Double? = 8.0
            override var inventoryCategory: String = InventoryItem.Category.TOOL
            override var maxDurability: Int = 143
            override var durability: Float = 64f
            override var consumable = false
        })
        actor.inventory.getByID(5656)!!.item.name = "Test tool"

        actor.inventory.add(object : InventoryItem() {
            init {
                itemProperties[IVKey.ITEMTYPE] = IVKey.ItemType.ARTEFACT
            }
            override val id: Int = 4633
            override val isUnique: Boolean = true
            override var originalName: String = "CONTEXT_ITEM_QUEST_NOUN"
            override var baseMass: Double = 1.4
            override var baseToolSize: Double? = null
            override var inventoryCategory: String = InventoryItem.Category.MISC
            override var consumable = false
        })

        actor.inventory.add(ItemCodex[16], 543)

        actor.inventory.getByID(Tile.STONE)!!.item equipTo actor
    }

    override fun init(container: GameContainer?, game: StateBasedGame?) {
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        Terrarum.appgc.setTitle("${Terrarum.NAME} — F: ${Terrarum.appgc.fps}")

        ui.update(container, delta)
    }

    override fun getID() = Terrarum.STATE_ID_TEST_UI1

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        blendNormal()
        g.color = Color.green
        g.fillRect(0f, 0f, 2048f, 2048f)



        ui.render(container, game, g)
    }
}
