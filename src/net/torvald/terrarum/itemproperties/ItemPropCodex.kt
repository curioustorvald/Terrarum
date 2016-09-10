package net.torvald.terrarum.itemproperties

import net.torvald.random.HQRNG
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.gameactors.CanBeAnItem
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameworld.GameWorld
import org.newdawn.slick.GameContainer
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object ItemPropCodex {

    val CSV_PATH = "./src/com/torvald/terrarum/itemproperties/itemprop.csv"

    /**
     * <ItemID or RefID for Actor, TheItem>
     * Will return corresponding Actor if ID >= 16777216
     */
    private val itemCodex = ArrayList<InventoryItem>()
    private val dynamicItemDescription = HashMap<Int, KVHashMap>()

    val ITEM_TILE_MAX = GameWorld.TILES_SUPPORTED
    val ITEM_COUNT_MAX = 16777216
    val ITEM_DYNAMIC_MAX = ITEM_COUNT_MAX - 1
    val ITEM_STATIC_MAX = 32767
    val ITEM_DYNAMIC_MIN = ITEM_STATIC_MAX + 1
    val ITEM_STATIC_MIN = ITEM_TILE_MAX

    init {
        // read prop in csv and fill itemCodex

        // read from save (if applicable) and fill dynamicItemDescription
    }

    fun getProp(code: Int): InventoryItem {
        if (code < ITEM_STATIC_MAX) // generic item
            return itemCodex[code] // from CSV
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