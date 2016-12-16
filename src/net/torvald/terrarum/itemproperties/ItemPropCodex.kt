package net.torvald.terrarum.itemproperties

import net.torvald.random.HQRNG
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.gameactors.CanBeAnItem
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gamecontroller.mouseTileX
import net.torvald.terrarum.gamecontroller.mouseTileY
import net.torvald.terrarum.gameitem.EquipPosition
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.tileproperties.TileProp
import net.torvald.terrarum.tileproperties.TilePropCodex
import org.apache.commons.csv.CSVRecord
import org.newdawn.slick.GameContainer
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object ItemPropCodex {

    /**
     * <ItemID or RefID for Actor, TheItem>
     * Will return corresponding Actor if ID >= 16777216
     */
    private val itemCodex = HashMap<Int, InventoryItem>()
    private val dynamicItemDescription = HashMap<Int, KVHashMap>()

    val ITEM_TILE_MAX = GameWorld.TILES_SUPPORTED - 1 // 4095
    val ITEM_COUNT_MAX = 16777216
    val ITEM_DYNAMIC_MAX = ITEM_COUNT_MAX - 1
    val ITEM_STATIC_MAX = 32767
    val ITEM_DYNAMIC_MIN = ITEM_STATIC_MAX + 1
    val ITEM_STATIC_MIN = ITEM_TILE_MAX + 1 // 4096

    init {
        // tile items
        for (i in 0..ITEM_TILE_MAX) {
            itemCodex[i] = object : InventoryItem() {
                override val id: Int = i
                override val equipPosition = EquipPosition.HAND_GRIP
                override var mass: Double = TilePropCodex[i].density / 1000.0
                override var scale: Double = 1.0 // no need to set setter as scale would not change

                override fun primaryUse(gc: GameContainer, delta: Int) {
                    // TODO base punch attack
                }

                override fun secondaryUse(gc: GameContainer, delta: Int) {
                    // TODO check if occupied by ANY ActorWithBodies

                    Terrarum.ingame.world.setTileTerrain(
                            gc.mouseTileX,
                            gc.mouseTileY,
                            i
                    )
                }
            }
        }

        // read prop in csv and fill itemCodex

        // read from save (if applicable) and fill dynamicItemDescription
    }

    operator fun get(code: Int): InventoryItem {
        if (code < ITEM_STATIC_MAX) // generic item
            return itemCodex[code]!! // from CSV
        else if (code < ITEM_DYNAMIC_MAX) {
            TODO("read from dynamicitem description (JSON)")
        }
        else {
            val a = Terrarum.ingame.getActorByID(code) // actor item
            if (a is CanBeAnItem) return a.itemData

            throw IllegalArgumentException("Attempted to get item data of actor that cannot be an item. ($a)")
        }
    }

    fun hasItem(itemID: Int): Boolean = dynamicItemDescription.containsKey(itemID)
}