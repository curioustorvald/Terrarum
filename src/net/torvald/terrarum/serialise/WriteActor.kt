package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import java.math.BigInteger

/**
 * Created by minjaesong on 2021-08-24.
 */
object WriteActor {

    operator fun invoke(actor: IngamePlayer): String {
        return Common.jsoner.toJson(actor)
    }

    fun encodeToByteArray64(actor: IngamePlayer): ByteArray64 {
        val ba = ByteArray64()
        this.invoke(actor).toByteArray().forEach { ba.add(it) }
        return ba
    }

}