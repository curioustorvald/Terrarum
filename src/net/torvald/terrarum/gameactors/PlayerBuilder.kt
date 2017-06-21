package net.torvald.terrarum.gameactors

import net.torvald.terrarum.TerrarumGDX

/**
 * Created by minjaesong on 16-02-03.
 */
object PlayerBuilder {

    operator fun invoke(): Actor {
        val p: Actor = Player(TerrarumGDX.ingame!!.world.time.currentTimeAsGameDate)
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")

        // attach sprite

        // do etc.
        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL

        return p
    }
}