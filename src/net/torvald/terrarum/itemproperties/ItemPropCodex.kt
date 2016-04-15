package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.gameactors.CanBeAnItem
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.Terrarum
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

    @JvmStatic val ITEM_UNIQUE_MAX = 32768

    @JvmStatic
    fun buildItemProp() {
        itemCodex = arrayOf<InventoryItem>()

        // read prop in csv

    }

    fun getItem(code: Int): InventoryItem {
        if (code < ITEM_UNIQUE_MAX)
            return itemCodex[code]
        else {
            for (actor in Terrarum.game.actorContainer) {
                if (actor is CanBeAnItem && actor.referenceID == code)
                    return actor.itemData
            }

            throw NullPointerException()
        }
    }
}