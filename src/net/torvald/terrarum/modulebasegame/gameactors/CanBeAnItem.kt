package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.gameitem.GameItem

/**
 * Created by minjaesong on 2016-01-31.
 */
interface CanBeAnItem {

    fun getItemWeight(): Double

    fun stopUpdateAndDraw()

    fun resumeUpdateAndDraw()

    var itemData: GameItem

}