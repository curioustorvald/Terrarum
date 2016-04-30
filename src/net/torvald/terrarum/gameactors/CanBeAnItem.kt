package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameitem.InventoryItem

/**
 * Created by minjaesong on 16-03-14.
 */
interface CanBeAnItem {

    fun attachItemData()

    fun getItemWeight(): Double

    fun stopUpdateAndDraw()

    fun resumeUpdateAndDraw()

    var itemData: InventoryItem

}