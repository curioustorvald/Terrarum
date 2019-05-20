package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.worlddrawer.CreateTileAtlas

/**
 * Created by minjaesong on 2017-02-10.
 */
object PlayerBuilderTestSubject1 {
    operator fun invoke(): IngamePlayer {
        val p: IngamePlayer = IngamePlayer(
                ModMgr.getPath("basegame", "sprites/furry_sprite.properties"),
                ModMgr.getPath("basegame", "sprites/furry_sprite_glow.properties"),
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
        p.reassembleSprite(p.sprite!!, p.spriteGlow)
        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 21, 0)

        p.setPosition(3.0 * CreateTileAtlas.TILE_SIZE, 3.0 * CreateTileAtlas.TILE_SIZE)


        PlayerBuilderSigrid.fillTestInventory(p.inventory)

        //p.actorValue[AVKey.LUMR] = 0.84
        //p.actorValue[AVKey.LUMG] = 0.93
        //p.actorValue[AVKey.LUMB] = 1.37
        //p.actorValue[AVKey.LUMA] = 1.93

        return p
    }
}