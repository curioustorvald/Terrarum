package net.torvald.terrarum.itemproperties

import net.torvald.point.Point2d
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.gameactors.CanBeAnItem
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithSprite
import net.torvald.terrarum.gamecontroller.mouseTileX
import net.torvald.terrarum.gamecontroller.mouseTileY
import net.torvald.terrarum.gameitem.EquipPosition
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.tileproperties.TileCodex
import org.newdawn.slick.GameContainer
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object ItemCodex {

    /**
     * <ItemID or RefID for Actor, TheItem>
     * Will return corresponding Actor if ID >= 16777216
     */
    private val itemCodex = HashMap<Int, InventoryItem>()
    private val dynamicItemDescription = HashMap<Int, KVHashMap>()

    val ITEM_TILE_MAX = GameWorld.TILES_SUPPORTED - 1 // 4095
    val ITEM_COUNT_MAX = 1048576
    val ITEM_DYNAMIC_MAX = ITEM_COUNT_MAX - 1
    val ITEM_STATIC_MAX = 32767
    val ITEM_DYNAMIC_MIN = ITEM_STATIC_MAX + 1
    val ITEM_STATIC_MIN = ITEM_TILE_MAX + 1 // 4096

    init {
        // tile items
        for (i in 0..ITEM_TILE_MAX) {
            itemCodex[i] = object : InventoryItem() {
                override val id: Int = i
                override var baseMass: Double = TileCodex[i].density / 1000.0
                override var baseToolSize: Double? = null
                override var equipPosition = EquipPosition.HAND_GRIP
                override var category = "block"
                override val originalName = TileCodex[i].nameKey

                override fun primaryUse(gc: GameContainer, delta: Int) {
                    // TODO base punch attack
                }

                override fun secondaryUse(gc: GameContainer, delta: Int) {
                    val mousePoint = Point2d(gc.mouseTileX.toDouble(), gc.mouseTileY.toDouble())
                    // linear search filter (check for intersection with tilewise mouse point and tilewise hitbox)
                    Terrarum.ingame!!.actorContainer.forEach {
                        if (it is ActorWithSprite && it.tilewiseHitbox.intersects(mousePoint))
                            return
                    }
                    // filter passed, do the job
                    Terrarum.ingame!!.world.setTileTerrain(
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
            val a = Terrarum.ingame!!.getActorByID(code) // actor item
            if (a is CanBeAnItem) return a.itemData

            throw IllegalArgumentException("Attempted to get item data of actor that cannot be an item. ($a)")
        }
    }

    fun hasItem(itemID: Int): Boolean = dynamicItemDescription.containsKey(itemID)
}