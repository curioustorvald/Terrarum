package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.faction.FactionFactory
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2016-02-03.
 */

object PlayerBuilderSigrid {

    operator fun invoke(): IngamePlayer {
        val p = IngamePlayer("lol",  - 9223372036854775807L) // XD

        p.referenceID = 0x51621D // the only constant of this procedural universe


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

        p.inventory = ActorInventory(p, 0, ActorInventory.CAPACITY_MODE_NO_ENCUMBER)

        p.faction.add(FactionFactory.create("basegame", "factions/FactionSigrid.json"))



        // Test fill up inventory
        fillTestInventory(p.inventory)


        return p
    }

    fun fillTestInventory(inventory: ActorInventory) {
        val blocks = arrayOf(
                Block.AIR, Block.DIRT, Block.GLASS_CRUDE, Block.GLASS_CLEAN,
                Block.GRASS, Block.GRAVEL, Block.ICE_MAGICAL, Block.LANTERN,
                Block.PLANK_BIRCH, Block.PLANK_BLOODROSE, Block.PLANK_EBONY, Block.PLANK_NORMAL,
                Block.SANDSTONE, Block.SANDSTONE_BLACK, Block.SANDSTONE_GREEN,
                Block.SANDSTONE_RED, Block.STONE, Block.STONE_BRICKS,
                Block.STONE_QUARRIED, Block.STONE_TILE_WHITE, Block.TORCH,
                Block.DAYLIGHT_CAPACITOR, Block.ICE_FRAGILE,
                Block.SUNSTONE,
                Block.ORE_COPPER,
                Block.PLATFORM_STONE, Block.PLATFORM_WOODEN, Block.PLATFORM_BIRCH, Block.PLATFORM_BLOODROSE, Block.PLATFORM_EBONY
        ) + (Block.ILLUMINATOR_WHITE .. Block.ILLUMINATOR_BLACK).toList()
        val walls = arrayOf(
                Block.AIR, Block.DIRT, Block.GLASS_CRUDE, Block.GLASS_CLEAN,
                Block.GRASSWALL, Block.ICE_MAGICAL,
                Block.PLANK_BIRCH, Block.PLANK_BLOODROSE, Block.PLANK_EBONY, Block.PLANK_NORMAL,
                Block.SANDSTONE, Block.SANDSTONE_BLACK, Block.SANDSTONE_GREEN,
                Block.SANDSTONE_RED, Block.STONE, Block.STONE_BRICKS,
                Block.STONE_QUARRIED, Block.STONE_TILE_WHITE
        )
        blocks.forEach { inventory.add(it, 9995) }
        walls.forEach { inventory.add(it + 4096, 9995) }
        inventory.add(8448) // copper pick
        inventory.add(8449) // iron pick
        inventory.add(8450) // steel pick
        inventory.add(8466) // wire piece
        inventory.add(9000) // TEST water bucket
        inventory.add(9001) // TEST lava bucket
    }
}
