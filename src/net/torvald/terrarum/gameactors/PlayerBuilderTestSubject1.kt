package net.torvald.terrarum.gameactors

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.worlddrawer.FeaturesDrawer

/**
 * Created by SKYHi14 on 2017-02-10.
 */
object PlayerBuilderTestSubject1 {
    operator fun invoke(): Player {
        val p: Player = Player(GameDate(100, 143)) // random value thrown
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")


        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue[AVKey.NAME] = "Test Subject 1"


        p.makeNewSprite(48, 52, ModMgr.getPath("basegame", "sprites/npc_template_anim_prototype.tga"))
        p.sprite!!.delay = 200
        p.sprite!!.setRowsAndFrames(2, 4)

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 21, 0)

        p.setPosition(4096.0 * FeaturesDrawer.TILE_SIZE, 300.0 * FeaturesDrawer.TILE_SIZE)





        return p
    }
}