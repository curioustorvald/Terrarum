package com.torvald.terrarum.gameactors

import com.torvald.JsonFetcher
import com.torvald.terrarum.gameactors.faction.Faction
import com.torvald.spriteanimation.SpriteAnimation
import com.google.gson.JsonObject
import com.torvald.terrarum.gameactors.faction.FactionFactory
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-14.
 */

object PFSigrid {

    fun create(): Player {
        val p = Player()

        p.sprite = SpriteAnimation()
        p.sprite!!.setDimension(28, 51)
        p.sprite!!.setSpriteImage("res/graphics/sprites/test_player.png")
        p.sprite!!.setDelay(200)
        p.sprite!!.setRowsAndFrames(1, 1)
        p.sprite!!.setAsVisible()

        p.spriteGlow = SpriteAnimation()
        p.spriteGlow!!.setDimension(28, 51)
        p.spriteGlow!!.setSpriteImage("res/graphics/sprites/test_player_glow.png")
        p.spriteGlow!!.setDelay(200)
        p.spriteGlow!!.setRowsAndFrames(1, 1)
        p.spriteGlow!!.setAsVisible()

        p.actorValue = ActorValue()
        p.actorValue["scale"] = 1.0f
        p.actorValue["speed"] = 4.0f
        p.actorValue["speedmult"] = 1.0f
        p.actorValue["accel"] = Player.WALK_ACCEL_BASE
        p.actorValue["accelmult"] = 1.0f

        p.actorValue["jumppower"] = 5f

        p.actorValue["basemass"] = 80f

        p.actorValue["physiquemult"] = 1 // Constant 1.0 for player, meant to be used by random mobs
        /**
         * fixed value, or 'base value', from creature strength of Dwarf Fortress.
         * Human race uses 1000. (see CreatureHuman.json)
         */
        p.actorValue["strength"] = 1414
        p.actorValue["encumbrance"] = 1000

        p.actorValue["name"] = "Sigrid"

        p.actorValue["intelligent"] = true

        p.actorValue["luminosity"] = 5980540

        p.actorValue["selectedtile"] = 16

        //p.setHitboxDimension(18, 46, 8, 0)
        p.setHitboxDimension(15, 46, 10, 0)

        p.inventory = ActorInventory(0x7FFFFFFF, true)

        p.setPosition((4096 * 16).toFloat(), (300 * 16).toFloat())

        p.faction.add(FactionFactory.create("FactionSigrid.json"))

        return p
    }
}
