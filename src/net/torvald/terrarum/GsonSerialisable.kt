package net.torvald.terrarum

import com.google.gson.JsonObject

/**
 * Created by minjaesong on 2018-05-18.
 */
interface GsonSerialisable {

    /**
     * Will modify itself according to the input gson. Not sure it's even necessary so please test.
     */
    fun read(gson: JsonObject)

}