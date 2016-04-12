package com.torvald.terrarum.gameactors

import com.torvald.JsonFetcher
import com.torvald.random.Fudge3
import com.torvald.random.HQRNG
import com.torvald.terrarum.langpack.Lang
import com.google.gson.JsonObject
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-14.
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