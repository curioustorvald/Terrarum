package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import java.math.BigInteger

/**
 * Created by minjaesong on 2021-08-24.
 */
object WriteActor {

    private val jsoner = Json(JsonWriter.OutputType.json)

    // install custom (de)serialiser
    init {
        jsoner.setSerializer(BigInteger::class.java, object : Json.Serializer<BigInteger> {
            override fun write(json: Json, obj: BigInteger?, knownType: Class<*>?) {
                json.writeValue(obj?.toString())
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): BigInteger {
                return BigInteger(jsonData.asString())
            }
        })
    }

    operator fun invoke(actor: Actor): String {
        return jsoner.toJson(actor)
    }

    fun encodeToByteArray64(actor: Actor): ByteArray64 {
        val ba = ByteArray64()
        this.invoke(actor).toByteArray().forEach { ba.add(it) }
        return ba
    }

}