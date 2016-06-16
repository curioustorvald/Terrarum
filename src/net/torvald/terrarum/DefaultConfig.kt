package net.torvald.terrarum

import com.google.gson.JsonObject

/**
 * Created by minjaesong on 16-03-12.
 */
object DefaultConfig {
    fun fetch(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty("smoothlighting", true)
        jsonObject.addProperty("imtooyoungtodie", false) // perma-death
        jsonObject.addProperty("language", Terrarum.sysLang)
        jsonObject.addProperty("notificationshowuptime", 6500)
        jsonObject.addProperty("multithread", true) // experimental!

        return jsonObject
    }
}