package com.torvald.terrarum.gameactors.faction

import com.torvald.JsonFetcher
import com.google.gson.JsonObject

import java.io.IOException

/**
 * Created by minjaesong on 16-02-15.
 */
class FactionRelatorFactory {

    @Throws(IOException::class)
    fun build(filename: String): Faction {
        val jsonObj = JsonFetcher.readJson(JSONPATH + filename)
        val factionObj = Faction(jsonObj.get("factionname").asString)


        jsonObj.get("factionamicable").asJsonArray.forEach { s -> factionObj.addFactionAmicable(s.asString) }
        jsonObj.get("factionneutral").asJsonArray.forEach { s -> factionObj.addFactionNeutral(s.asString) }
        jsonObj.get("factionhostile").asJsonArray.forEach { s -> factionObj.addFactionHostile(s.asString) }
        jsonObj.get("factionfearful").asJsonArray.forEach { s -> factionObj.addFactionFearful(s.asString) }

        return factionObj
    }

    companion object {

        val JSONPATH = "./res/raw/"
    }

}
