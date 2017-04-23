package net.torvald.terrarum.gameactors

import net.torvald.JsonFetcher
import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.spriteanimation.SpriteAnimation
import com.google.gson.JsonObject
import net.torvald.terrarum.ActorValue
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.faction.FactionFactory
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.mapdrawer.FeaturesDrawer
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.to10bit
import net.torvald.terrarum.toInt
import org.newdawn.slick.Color
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-02-03.
 */

object PlayerBuilderSigrid {

    operator fun invoke(): Player {
        val p = Player(GameDate(-2147483648, 0)) // XD

        p.referenceID = 0x51621D // the only constant of this procedural universe

        p.makeNewSprite(28, 51, ModMgr.getPath("basegame", "sprites/test_player.tga"))
        p.sprite!!.delay = 200
        p.sprite!!.setRowsAndFrames(1, 1)

        p.makeNewSpriteGlow(28, 51, ModMgr.getPath("basegame", "sprites/test_player_glow.tga"))
        p.spriteGlow!!.delay = 200
        p.spriteGlow!!.setRowsAndFrames(1, 1)

        p.actorValue = ActorValue()
        p.actorValue[AVKey.SCALE] = 1.0
        p.actorValue[AVKey.SPEED] = 4.0
        p.actorValue[AVKey.SPEEDBUFF] = 1.0
        p.actorValue[AVKey.ACCEL] = ActorHumanoid.WALK_ACCEL_BASE
        p.actorValue[AVKey.ACCELBUFF] = 1.0
        p.actorValue[AVKey.JUMPPOWER] = 5.0

        p.actorValue[AVKey.BASEMASS] = 80.0
        p.actorValue[AVKey.SCALEBUFF] = 1.0 // Constant 1.0 for player, meant to be used by random mobs
        /**
         * fixed value, or 'base value', from creature strength of Dwarf Fortress.
         * Human race uses 1000. (see CreatureHuman.json)
         */
        p.actorValue[AVKey.STRENGTH] = 1414 // this is test character, after all.
        p.actorValue[AVKey.ENCUMBRANCE] = 1000
        p.actorValue[AVKey.BASEHEIGHT] = 46

        p.actorValue[AVKey.NAME] = "Sigrid"

        p.actorValue[AVKey.INTELLIGENT] = true

        p.actorValue[AVKey.LUMINOSITY] = Color(0x434aff).to10bit()
        //p.actorValue[AVKey.LUMINOSITY] = 214127943 // bright purple

        p.actorValue[AVKey.BASEDEFENCE] = 141

        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue["__aimhelper"] = true // TODO when you'll gonna implement it?

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT)!!, 11, 0)

        p.inventory = ActorInventory(p, 0, ActorInventory.CAPACITY_MODE_NO_ENCUMBER)

        p.setPosition((4096 * FeaturesDrawer.TILE_SIZE).toDouble(), (300 * 16).toDouble())

        p.faction.add(FactionFactory.create("basegame", "factions/FactionSigrid.json"))



        // Test fill up inventory
        val tiles = arrayOf(
                Tile.AIR, Tile.DIRT, Tile.GLASS_CRUDE,
                Tile.GRASS, Tile.GRAVEL, Tile.ICE_MAGICAL, Tile.LANTERN,
                Tile.PLANK_BIRCH, Tile.PLANK_BLOODROSE, Tile.PLANK_EBONY, Tile.PLANK_NORMAL,
                Tile.SANDSTONE, Tile.SANDSTONE_BLACK, Tile.SANDSTONE_GREEN,
                Tile.SANDSTONE_RED, Tile.STONE, Tile.STONE_BRICKS,
                Tile.STONE_QUARRIED, Tile.STONE_TILE_WHITE, Tile.TORCH
        )
        tiles.forEach { p.addItem(it, 999) }
        p.inventory.add(ItemCodex.ITEM_STATIC.first)




        return p
    }
}
