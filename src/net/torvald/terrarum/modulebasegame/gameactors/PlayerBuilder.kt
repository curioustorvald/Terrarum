package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2016-02-03.
 */
object PlayerBuilder {

    operator fun invoke(): Actor {
        val world = (Terrarum.ingame!! as TerrarumIngame).world

        val p: Actor = IngamePlayer("lol", "lol_glow", "lol_emsv", world.worldTime.TIME_T)
        InjectCreatureRaw(p.actorValue, "basegame", "CreaturePlayer.json")

        // attach sprite

        // do etc.
        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL

        return p
    }
}