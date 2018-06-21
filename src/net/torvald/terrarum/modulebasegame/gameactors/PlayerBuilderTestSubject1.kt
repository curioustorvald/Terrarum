package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-02-10.
 */
object PlayerBuilderTestSubject1 {
    operator fun invoke(): Player {
        val p: Player = Player((Terrarum.ingame!! as Ingame).world, GameDate(100, 143)) // random value thrown
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")


        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue[AVKey.NAME] = "Test Subject 1"


        p.makeNewSprite(TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/npc_template_anim_prototype.tga"), 48, 52))
        p.sprite!!.delay = 0.2f
        p.sprite!!.setRowsAndFrames(2, 4)

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 21, 0)

        p.setPosition(4096.0 * FeaturesDrawer.TILE_SIZE, 300.0 * FeaturesDrawer.TILE_SIZE)





        return p
    }
}