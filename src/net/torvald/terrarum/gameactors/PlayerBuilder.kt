package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-02-03.
 */
object PlayerBuilder {
    private val JSONPATH = "./assets/raw/"
    private val jsonString = String()

    operator fun invoke(): Actor {
        val p: Actor = Player(Terrarum.ingame!!.world.time.currentTimeAsGameDate)
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")

        // attach sprite

        // do etc.
        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0

        return p
    }
}