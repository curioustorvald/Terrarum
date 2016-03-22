package com.Torvald.Terrarum

import com.google.gson.JsonObject

/**
 * Created by minjaesong on 16-03-19.
 */
object DefaultConfig {
    fun fetch(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty("smoothlighting", true)
        jsonObject.addProperty("imtooyoungtodie", false)
        jsonObject.addProperty("language", Terrarum.sysLang)
        jsonObject.addProperty("notificationshowuptime", 6500)

        return jsonObject
    }
}