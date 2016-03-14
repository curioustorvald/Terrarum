package com.Torvald.Terrarum;

import com.google.gson.JsonObject;

/**
 * Created by minjaesong on 16-03-12.
 */
public class DefaultConfig {

    public static JsonObject fetch() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("smoothlighting", true);
        jsonObject.addProperty("imtooyoungtodie", false);
        jsonObject.addProperty("language", Terrarum.getSysLang());
        jsonObject.addProperty("notificationshowuptime", 6500);

        return jsonObject;
    }

}
