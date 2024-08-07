package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.AssembledSpriteAnimation
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey

/**
 * Created by minjaesong on 2021-07-07.
 */
object PlayerBuilderWerebeastTest {
    operator fun invoke(): IngamePlayer {
        val p: IngamePlayer = IngamePlayer(
            ModMgr.getGdxFile("basegame", "sprites/taimu2.properties").path(),
            ModMgr.getGdxFile("basegame", "sprites/taimu2_glow.properties").path(),
            ModMgr.getGdxFile("basegame", "sprites/taimu2_emsv.properties").path(),
            -589141658L // random value thrown
        )
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureWerebeastBossBase.json")


        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue[AVKey.NAME] = "Taimu"

        p.animDesc?.let { p.sprite = AssembledSpriteAnimation(it, p, false, false) }
        p.animDescGlow?.let { p.spriteGlow = AssembledSpriteAnimation(it, p, true, false) }
        p.animDescEmissive?.let { p.spriteEmissive = AssembledSpriteAnimation(it, p, false, true) }
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