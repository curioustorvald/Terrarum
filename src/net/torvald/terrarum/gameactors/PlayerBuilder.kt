package net.torvald.terrarum.gameactors

import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-15.
 */
object PlayerBuilder {
    private val JSONPATH = "./res/raw/"
    private val jsonString = String()

    @Throws(IOException::class, SlickException::class)
    fun create(): Player {
        val p: Player = Player()
        CreatureRawInjector.inject(p.actorValue, "CreatureHuman.json")

        // attach sprite

        // do etc.
        p.actorValue[AVKey._PLAYER_QUICKBARSEL] = 0

        return p
    }
}