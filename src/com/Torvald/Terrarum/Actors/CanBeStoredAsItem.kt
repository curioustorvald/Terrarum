package com.Torvald.Terrarum.Actors

import com.Torvald.Terrarum.GameItem.InventoryItem

/**
 * Created by minjaesong on 16-03-14.
 */
interface CanBeStoredAsItem {

    fun attachItemData()

    fun getItemWeight(): Float

    fun stopUpdateAndDraw()

    fun resumeUpdateAndDraw()

    var itemData: InventoryItem?

}