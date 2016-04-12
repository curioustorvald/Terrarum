package com.torvald.terrarum.gameactors

import com.torvald.JsonFetcher
import com.torvald.terrarum.gameactors.faction.Faction
import com.torvald.spriteanimation.SpriteAnimation
import com.google.gson.JsonObject
import com.torvald.terrarum.gameactors.faction.FactionFactory
import com.torvald.terrarum.mapdrawer.MapDrawer
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-14.
 */

object PBSigrid {

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
        p.actorValue[AVKey.SCALE] = 1.0f
        p.actorValue[AVKey.SPEED] = 4.0f
        p.actorValue[AVKey.SPEEDMULT] = 1.0f
        p.actorValue[AVKey.ACCEL] = Player.WALK_ACCEL_BASE
        p.actorValue[AVKey.ACCELMULT] = 1.0f
        p.actorValue[AVKey.JUMPPOWER] = 5f

        p.actorValue[AVKey.BASEMASS] = 80f
        p.actorValue[AVKey.PHYSIQUEMULT] = 1 // Constant 1.0 for player, meant to be used by random mobs
        /**
         * fixed value, or 'base value', from creature strength of Dwarf Fortress.
         * Human race uses 1000. (see CreatureHuman.json)
         */
        p.actorValue[AVKey.STRENGTH] = 1414
        p.actorValue[AVKey.ENCUMBRANCE] = 1000
        p.actorValue[AVKey.BASEHEIGHT] = 46

        p.actorValue[AVKey.NAME] = "Sigrid"

        p.actorValue[AVKey.INTELLIGENT] = true

        p.actorValue[AVKey.LUMINOSITY] = 95487100

        p.actorValue[AVKey.BASEDEFENCE] = 141

        p.actorValue["selectedtile"] = 16

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT)!!, 10, 0)

        p.inventory = ActorInventory(0x7FFFFFFF, true)

        p.setPosition((4096 * MapDrawer.TILE_SIZE).toFloat(), (300 * 16).toFloat())

        p.faction.add(FactionFactory.create("FactionSigrid.json"))

        return p
    }
}
