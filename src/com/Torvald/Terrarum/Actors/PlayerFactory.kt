package com.Torvald.Terrarum.Actors

import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-15.
 */
object PlayerFactory {
    private val JSONPATH = "./res/raw/"
    private val jsonString = String()

    @JvmStatic
    @Throws(IOException::class, SlickException::class)
    fun build(jsonFileName: String): Player {
        var p: Player = CreatureFactory.build("CreatureHuman") as Player

        // attach sprite

        // do etc.

        return p
    }
}