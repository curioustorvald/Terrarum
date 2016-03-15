package com.Torvald.Terrarum.GameItem

import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object ItemCodex {
    /**
     * &lt;ItemID or RefID for Actor, TheItem&gt;
     * Will return corresponding Actor if ID >= 32768
     */
    private val itemCodex: HashMap<Long, InventoryItem> = HashMap(
            // hashmap init here
    )


    fun getItem(code: Long): InventoryItem {
        return itemCodex[code]!!
    }
}