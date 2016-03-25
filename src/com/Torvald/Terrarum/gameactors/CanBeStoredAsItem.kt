package com.torvald.terrarum.gameactors

import com.torvald.terrarum.gameitem.InventoryItem

/**
 * Created by minjaesong on 16-03-14.
 */
interface CanBeStoredAsItem {

    fun attachItemData()

    fun getItemWeight(): Float

    fun stopUpdateAndDraw()

    fun resumeUpdateAndDraw()

    var itemData: InventoryItem

}