package net.torvald.terrarum.gameactors

import net.torvald.JsonFetcher
import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.spriteanimation.SpriteAnimation
import com.google.gson.JsonObject
import net.torvald.terrarum.console.ActorHumanoid
import net.torvald.terrarum.gameactors.faction.FactionFactory
import net.torvald.terrarum.mapdrawer.MapDrawer
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-14.
 */

object PBSigrid {

    fun create(): Player {
        val p = Player(GameDate(-2147483648, 0)) // XD

        p.sprite = SpriteAnimation()
        p.sprite!!.setDimension(28, 51)
        p.sprite!!.setSpriteImage("assets/graphics/sprites/test_player.png")
        p.sprite!!.setDelay(200)
        p.sprite!!.setRowsAndFrames(1, 1)
        p.sprite!!.setAsVisible()

        p.spriteGlow = SpriteAnimation()
        p.spriteGlow!!.setDimension(28, 51)
        p.spriteGlow!!.setSpriteImage("assets/graphics/sprites/test_player_glow.png")
        p.spriteGlow!!.setDelay(200)
        p.spriteGlow!!.setRowsAndFrames(1, 1)
        p.spriteGlow!!.setAsVisible()

        p.actorValue = ActorValue()
        p.actorValue[AVKey.SCALE] = 1.0
        p.actorValue[AVKey.SPEED] = 4.0
        p.actorValue[AVKey.SPEEDMULT] = 1.0
        p.actorValue[AVKey.ACCEL] = ActorHumanoid.WALK_ACCEL_BASE
        p.actorValue[AVKey.ACCELMULT] = 1.0
        p.actorValue[AVKey.JUMPPOWER] = 5.0

        p.actorValue[AVKey.BASEMASS] = 80.0
        p.actorValue[AVKey.PHYSIQUEMULT] = 1 // Constant 1.0 for player, meant to be used by random mobs
        /**
         * fixed value, or 'base value', from creature strength of Dwarf Fortress.
         * Human race uses 1000. (see CreatureHuman.json)
         */
        p.actorValue[AVKey.STRENGTH] = 1414 // this is test character, after all.
        p.actorValue[AVKey.ENCUMBRANCE] = 1000
        p.actorValue[AVKey.BASEHEIGHT] = 46

        p.actorValue[AVKey.NAME] = "Sigrid"

        p.actorValue[AVKey.INTELLIGENT] = true

        p.actorValue[AVKey.LUMINOSITY] = 0//95487100

        p.actorValue[AVKey.BASEDEFENCE] = 141

        p.actorValue[AVKey.__PLAYER_QUICKBARSEL] = 0
        p.actorValue["__selectedtile"] = 147 // test code; replace with <tile_item>.primaryUse(gc, delta)
        p.actorValue["__aimhelper"] = true // TODO when you'll gonna implement it?

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT)!!, 10, 0)

        p.inventory = ActorInventory(0x7FFFFFFF, true)

        p.setPosition((4096 * MapDrawer.TILE_SIZE).toDouble(), (300 * 16).toDouble())

        p.faction.add(FactionFactory.create("FactionSigrid.json"))

        return p
    }
}
