package com.torvald.colourutil

import org.newdawn.slick.Color

/**
 * Created by minjaesong on 16-02-11.
 */
interface LimitedColours {

    fun createSlickColor(raw: Int): Color
    fun createSlickColor(r: Int, g: Int, b: Int): Color

    fun create(raw: Int)
    fun create(r: Int, g: Int, b: Int)

    fun toSlickColour(): Color
}
