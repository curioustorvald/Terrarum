package com.Torvald.Terrarum.GameItem

import com.Torvald.Terrarum.Actors.CanBeStoredAsItem
import com.Torvald.Terrarum.Terrarum
import org.newdawn.slick.GameContainer
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object ItemPropCodex {

    val CSV_PATH = "./src/com/Torvald/Terrarum/ItemProperties/itemprop.csv"

    /**
     * &lt;ItemID or RefID for Actor, TheItem&gt;
     * Will return corresponding Actor if ID >= 32768
     */
    private lateinit var itemCodex: Array<InventoryItem>

    @JvmStatic val ITEM_UNIQUE_MAX = 32768

    @JvmStatic
    fun buildItemProp() {
        itemCodex = arrayOf<InventoryItem>()

        // read prop in csv

    }

    fun getItem(code: Long): InventoryItem {
        if (code < ITEM_UNIQUE_MAX)
            return itemCodex[code as Int]!!
        else {
            for (actor in Terrarum.game.actorContainer) {
                if (actor is CanBeStoredAsItem && actor.referenceID!!.equals(code))
                    return actor.itemData
            }

            throw NullPointerException()
        }
    }
}