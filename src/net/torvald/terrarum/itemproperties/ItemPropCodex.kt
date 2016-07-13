package net.torvald.terrarum.itemproperties

import net.torvald.random.HQRNG
import net.torvald.terrarum.gameactors.CanBeAnItem
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gamemap.GameWorld
import org.newdawn.slick.GameContainer
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object ItemPropCodex {

    val CSV_PATH = "./src/com/torvald/terrarum/itemproperties/itemprop.csv"

    /**
     * <ItemID or RefID for Actor, TheItem>
     * Will return corresponding Actor if ID >= 32768
     */
    private lateinit var itemCodex: Array<InventoryItem>

    const val ITEM_UNIQUE_MAX = 32768
    val TILE_MAX = GameWorld.TILES_SUPPORTED

    init {
        itemCodex = arrayOf<InventoryItem>()

        // read prop in csv

    }

    fun getProp(code: Int): InventoryItem {
        if (code < ITEM_UNIQUE_MAX) // generic item
            return itemCodex[code]
        else {
            val a = Terrarum.ingame.getActorByID(code) // actor item
            if (a is CanBeAnItem) return a.itemData

            throw IllegalArgumentException("Attempted to get item data of actor that cannot be an item. ($a)")
        }
    }
}