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
    operator fun invoke(module: String, jsonFileName: String): ActorWithSprite {
        val actor = ActorWithSprite(Actor.RenderOrder.MIDDLE)
        InjectCreatureRaw(actor.actorValue, module, jsonFileName)


        actor.actorValue[AVKey.__ACTION_TIMER] = 0.0

        return actor
    }
}