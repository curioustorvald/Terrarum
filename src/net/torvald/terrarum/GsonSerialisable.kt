package net.torvald.terrarum

import com.google.gson.JsonObject

/**
 * Created by minjaesong on 2018-05-18.
 */
interface GsonSerialisable {

    fun read(gson: JsonObject)
    fun write(targetGson: JsonObject)

}