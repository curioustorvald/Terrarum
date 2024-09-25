package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.AssembledSpriteAnimation
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.AVKey

/**
 * Created by minjaesong on 2017-02-10.
 */
object PlayerBuilderTestSubject1 {
    operator fun invoke(): IngamePlayer {
        val p: IngamePlayer = IngamePlayer(
            ModMgr.getGdxFile("basegame", "sprites/fofu.properties").path(),
            ModMgr.getGdxFile("basegame", "sprites/fofu_glow.properties").path(),
            ModMgr.getGdxFile("basegame", "sprites/fofu_emsv.properties").path(),
            0L // random value thrown
        )
        InjectCreatureRaw(p.actorValue, "basegame", "CreaturePlayer.json")


        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue[AVKey.NAME] = "Test Subject 1"


        // TODO make null animation if animDesc is null
        p.animDesc?.let { p.sprite = AssembledSpriteAnimation(it, p, false, false) }
        p.animDescGlow?.let { p.spriteGlow = AssembledSpriteAnimation(it, p, true, false) }
        p.animDescEmissive?.let { p.spriteEmissive = AssembledSpriteAnimation(it, p, true, true) }

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 21, 0)

        // ingame must teleport the player to the spawn point
        // see `TerrarumIngame.render`
//        p.setPosition(3.0 * TILE_SIZE, 3.0 * TILE_SIZE)


//        PlayerBuilderSigrid.fillTestInventory(p.inventory) // commenting out: test is over
        p.actorValue[AVKey.GAMEMODE] = "survival"


        //p.actorValue[AVKey.LUMR] = 0.84
        //p.actorValue[AVKey.LUMG] = 0.93
        //p.actorValue[AVKey.LUMB] = 1.37
        //p.actorValue[AVKey.LUMA] = 1.93

        giveFreeStarterPackUntilPotsWithItemsAreImplemented(p)

        p.actorValue[AVKey.AIRJUMPPOINT] = 0
        p.actorValue[AVKey.SCALE] = 1.0

        return p
    }

    private fun giveFreeStarterPackUntilPotsWithItemsAreImplemented(p: IngamePlayer) {
        p.inventory.add("basegame:176", 20) // torches
    }
}