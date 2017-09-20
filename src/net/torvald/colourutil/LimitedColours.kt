package net.torvald.colourutil

import com.badlogic.gdx.graphics.Color

/**
 * Created by minjaesong on 2016-02-11.
 */
interface LimitedColours {

    fun createGdxColor(raw: Int): Color
    fun createGdxColor(r: Int, g: Int, b: Int): Color

    fun create(raw: Int)
    fun create(r: Int, g: Int, b: Int)

    fun toGdxColour(): Color
}
