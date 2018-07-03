package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.modulebasegame.Ingame

/**
 * Created by minjaesong on 2016-02-03.
 */
object PlayerBuilder {

    operator fun invoke(): Actor {
        val p: Actor = Player((Terrarum.ingame!! as Ingame).world, (Terrarum.ingame!! as Ingame).world.time.TIME_T)
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")

        // attach sprite

        // do etc.
        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL

        return p
    }
}