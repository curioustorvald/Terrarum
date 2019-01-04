package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-02-10.
 */
object PlayerBuilderTestSubject1 {
    operator fun invoke(): IngamePlayer {
        val p: IngamePlayer = IngamePlayer(-589141658L) // random value thrown
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")


        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue[AVKey.NAME] = "Test Subject 1"


        p.makeNewSprite(TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/npc_template_anim_prototype.tga"), 48, 52))
        p.sprite!!.delays = floatArrayOf(2f, 1f/12f) // second value does nothing -- overridden by ActorHumanoid.updateSprite(float)
        p.sprite!!.setRowsAndFrames(2, 4)

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 21, 0)

        p.setPosition(3.0 * FeaturesDrawer.TILE_SIZE, 3.0 * FeaturesDrawer.TILE_SIZE)





        return p
    }
}