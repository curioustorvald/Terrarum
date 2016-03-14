package com.Torvald.Terrarum.Actors

import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-03-14.
 */
interface Actor {

    fun update(gc: GameContainer, delta_t: Int)

    /**
     * Valid RefID is equal to or greater than 32768.
     * @return Reference ID. (32768-0x7FFF_FFFF_FFFF_FFFF)
     */
    var referenceID: Long?
}