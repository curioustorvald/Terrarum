package net.torvald.terrarum.gameactors

import net.torvald.JsonFetcher
import net.torvald.random.Fudge3
import net.torvald.random.HQRNG
import net.torvald.terrarum.langpack.Lang
import com.google.gson.JsonObject
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-02-05.
 */

object CreatureBuilder {

    /**
     * @Param jsonFileName with extension
     */
    @Throws(IOException::class, SlickException::class)
    fun create(jsonFileName: String): ActorWithBody {
        val actor = ActorWithBody()
        CreatureRawInjector.inject(actor.actorValue, jsonFileName)

        return actor
    }
}