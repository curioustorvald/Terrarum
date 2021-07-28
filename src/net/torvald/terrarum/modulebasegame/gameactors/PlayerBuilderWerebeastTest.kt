package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.worlddrawer.CreateTileAtlas

/**
 * Created by minjaesong on 2021-07-07.
 */
object PlayerBuilderWerebeastTest {
    operator fun invoke(): IngamePlayer {
        val p: IngamePlayer = IngamePlayer(
                ModMgr.getPath("basegame", "sprites/taimu.properties"),
                ModMgr.getPath("basegame", "sprites/taimu_glow.properties"),
                -589141658L // random value thrown
        )
        //InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")

        p.actorValue[AVKey.SCALE] = 1.0
        p.actorValue[AVKey.SPEED] = 6.0
        p.actorValue[AVKey.SPEEDBUFF] = 1.0
        p.actorValue[AVKey.ACCEL] = ActorHumanoid.WALK_ACCEL_BASE
        p.actorValue[AVKey.ACCELBUFF] = 1.0
        p.actorValue[AVKey.JUMPPOWER] = 19.0
        p.actorValue[AVKey.BASEREACH] = 114 // 7 tiles + 2 px

        p.actorValue[AVKey.BASEMASS] = 599.16
        p.actorValue[AVKey.SCALEBUFF] = 1.0 // Constant 1.0 for player, meant to be used by random mobs
        p.actorValue[AVKey.STRENGTH] = 5000
        p.actorValue[AVKey.ENCUMBRANCE] = 10000
        p.actorValue[AVKey.BASEHEIGHT] = 90

        p.actorValue[AVKey.INTELLIGENT] = true

        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue[AVKey.NAME] = "Taimu"


        /*p.makeNewSprite(TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/npc_template_anim_prototype.tga"), 48, 52))
        p.sprite!!.delays = floatArrayOf(2f, 1f/12f) // second value does nothing -- overridden by ActorHumanoid.updateSprite(float)
        p.sprite!!.setRowsAndFrames(2, 4)*/

        p.sprite = SpriteAnimation(p)
        p.spriteGlow = SpriteAnimation(p)
        p.reassembleSprite(p.sprite!!, p.spriteGlow)
        p.setHitboxDimension(22, p.actorValue.getAsInt(AVKey.BASEHEIGHT)!!, 30, 0)

        p.setPosition(3.0 * TILE_SIZE, 3.0 * TILE_SIZE)


        PlayerBuilderSigrid.fillTestInventory(p.inventory)

        //p.actorValue[AVKey.LUMR] = 0.84
        //p.actorValue[AVKey.LUMG] = 0.93
        //p.actorValue[AVKey.LUMB] = 1.37
        //p.actorValue[AVKey.LUMA] = 1.93

        p.actorValue[AVKey.AIRJUMPPOINT] = 0
        p.actorValue[AVKey.SCALE] = 1.0

        return p
    }
}