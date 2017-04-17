package net.torvald.terrarum.gameactors

import net.torvald.terrarum.itemproperties.InventoryItem

/**
 * Created by minjaesong on 16-01-31.
 */
interface CanBeAnItem {

    fun getItemWeight(): Double

    fun stopUpdateAndDraw()

    fun resumeUpdateAndDraw()

    var itemData: InventoryItem

}