package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.AVKey

/**
 * Created by minjaesong on 2017-02-10.
 */
object PlayerBuilderTestSubject1 {
    operator fun invoke(): IngamePlayer {
        val p: IngamePlayer = IngamePlayer(
                ModMgr.getGdxFile("basegame", "sprites/test_sprite.properties").path(),
                ModMgr.getGdxFile("basegame", "sprites/test_sprite_glow.properties").path(),
                -589141658L // random value thrown
        )
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")


        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue[AVKey.NAME] = "Test Subject 1"


        /*p.makeNewSprite(TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/npc_template_anim_prototype.tga"), 48, 52))
        p.sprite!!.delays = floatArrayOf(2f, 1f/12f) // second value does nothing -- overridden by ActorHumanoid.updateSprite(float)
        p.sprite!!.setRowsAndFrames(2, 4)*/

        p.sprite = SpriteAnimation(p)
        p.spriteGlow = SpriteAnimation(p)
        p.reassembleSprite(p.sprite, p.spriteGlow, null)
        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 21, 0)

        // ingame must teleport the player to the spawn point
        // see `TerrarumIngame.render`
//        p.setPosition(3.0 * TILE_SIZE, 3.0 * TILE_SIZE)


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