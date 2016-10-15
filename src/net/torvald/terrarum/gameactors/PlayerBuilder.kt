package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-15.
 */
object PlayerBuilder {
    private val JSONPATH = "./assets/raw/"
    private val jsonString = String()

    @Throws(IOException::class, SlickException::class)
    fun create(): Player {
        val p: Player = Player(Terrarum.ingame.world.time.currentTimeAsGameDate)
        CreatureRawInjector.inject(p.actorValue, "CreatureHuman.json")

        // attach sprite

        // do etc.
        p.actorValue[AVKey.__PLAYER_QUICKBARSEL] = 0

        return p
    }
}