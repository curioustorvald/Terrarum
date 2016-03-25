package com.torvald.terrarum.gameactors

import com.torvald.spriteanimation.SpriteAnimation

/**
 * Created by minjaesong on 16-03-25.
 */
object PFCynthia {

    fun create(): Player {
        val p: Player = Player()
        CreatureRawInjector.inject(p.actorValue, "CreatureHuman.json")

        p.actorValue["selectedtile"] = 16

        p.sprite = SpriteAnimation()
        p.sprite!!.setDimension(26, 42)
        p.sprite!!.setSpriteImage("res/graphics/sprites/test_player_2.png")
        p.sprite!!.setDelay(200)
        p.sprite!!.setRowsAndFrames(1, 1)
        p.sprite!!.setAsVisible()

        p.setHitboxDimension(15, 40, 9, 0)

        p.setPosition((4096 * 16).toFloat(), (300 * 16).toFloat())

        return p
    }

}