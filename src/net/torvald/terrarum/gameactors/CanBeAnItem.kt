package net.torvald.terrarum.gameactors

import net.torvald.terrarum.itemproperties.GameItem

/**
 * Created by minjaesong on 2016-01-31.
 */
interface CanBeAnItem {

    fun getItemWeight(): Double

    fun stopUpdateAndDraw()

    fun resumeUpdateAndDraw()

    var itemData: GameItem

}