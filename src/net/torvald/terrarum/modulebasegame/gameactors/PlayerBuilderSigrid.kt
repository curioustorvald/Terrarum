package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.faction.FactionFactory
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.terrarum.*

/**
 * Created by minjaesong on 2016-02-03.
 */

object PlayerBuilderSigrid {

    operator fun invoke(): IngamePlayer {
        val p = IngamePlayer("lol", "lol_glow", - 9223372036854775807L) // XD

        //p.referenceID = 0x51621D // the only constant of this procedural universe


        p.makeNewSprite(TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/test_player.tga"), 28, 51))
        p.sprite!!.setRowsAndFrames(1, 1)

        p.makeNewSpriteGlow(TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/test_player_glow.tga"), 28, 51))
        p.spriteGlow!!.setRowsAndFrames(1, 1)

        p.actorValue[AVKey.SCALE] = 1.0
        p.actorValue[AVKey.SPEED] = 4.0
        p.actorValue[AVKey.SPEEDBUFF] = 1.0
        p.actorValue[AVKey.ACCEL] = ActorHumanoid.WALK_ACCEL_BASE
        p.actorValue[AVKey.ACCELBUFF] = 1.0
        p.actorValue[AVKey.JUMPPOWER] = 13.0

        p.actorValue[AVKey.BASEMASS] = 80.0
        p.actorValue[AVKey.SCALEBUFF] = 1.0 // Constant 1.0 for player, meant to be used by random mobs
        /**
         * fixed value, or 'base value', from creature strength of Dwarf Fortress.
         * Human race uses 1000. (see CreatureHuman.json)
         */
        p.actorValue[AVKey.STRENGTH] = 1414 // this is test character, after all.
        p.actorValue[AVKey.ENCUMBRANCE] = 1000
        p.actorValue[AVKey.BASEHEIGHT] = 46
        p.actorValue[AVKey.BASEREACH] = 50

        p.actorValue[AVKey.NAME] = "Sigrid"

        p.actorValue[AVKey.INTELLIGENT] = true

        p.actorValue[AVKey.LUMR] = 0.84
        p.actorValue[AVKey.LUMG] = 0.93
        p.actorValue[AVKey.LUMB] = 1.37
        p.actorValue[AVKey.LUMA] = 1.93

        p.actorValue[AVKey.BASEDEFENCE] = 141

        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue["__aimhelper"] = true // TODO when you'll gonna implement it?

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT)!!, 11, 0)

        p.inventory = ActorInventory(p, 0, FixtureInventory.CAPACITY_MODE_NO_ENCUMBER)

        p.faction.add(FactionFactory.create("basegame", "factions/FactionSigrid.json"))



        // Test fill up inventory
        fillTestInventory(p.inventory)


        return p
    }

    fun fillTestInventory(inventory: ActorInventory) {

        AppLoader.tileMaker.tags.forEach { t, _ ->
            inventory.add(t, 9995)
            try {
                inventory.add("wall@"+t, 9995) // this code will try to add nonexisting wall items, do not get surprised with NPEs
            }
            catch (e: Throwable) {
                System.err.println("[PlayerBuilder] $e")
            }
        }

        // item ids are defined in <module>/items/itemid.csv

        inventory.add("item@basegame:1", 16) // copper pick
        inventory.add("item@basegame:2") // iron pick
        inventory.add("item@basegame:3") // steel pick
        inventory.add("item@basegame:5", 385930603) // test tiki torch
        inventory.add("item@basegame:6", 95) // storage chest
        inventory.add("item@basegame:7", 1) // wire debugger
        inventory.add("item@basegame:8", 9995) // power source

        WireCodex.getAll().forEach {
            try {
                inventory.add(it.id, 9995)
            }
            catch (e: Throwable) {
                System.err.println("[PlayerBuilder] $e")
            }
        }
    }
}
