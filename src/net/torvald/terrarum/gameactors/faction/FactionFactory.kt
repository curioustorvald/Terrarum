package net.torvald.terrarum.gameactors.faction

import net.torvald.JsonFetcher
import com.google.gson.JsonObject

import java.io.IOException

/**
 * Created by minjaesong on 16-02-15.
 */
object FactionFactory {

    const val JSONPATH = "./assets/raw/factions/"

    /**
     * @param filename with extension
     */
    @Throws(IOException::class)
    fun create(filename: String): Faction {
        val jsonObj = JsonFetcher(JSONPATH + filename)
        val factionObj = Faction(jsonObj.get("factionname").asString)

        jsonObj.get("factionamicable").asJsonArray.forEach { factionObj.addFactionAmicable(it.asString) }
        jsonObj.get("factionneutral").asJsonArray.forEach { factionObj.addFactionNeutral(it.asString) }
        jsonObj.get("factionhostile").asJsonArray.forEach { factionObj.addFactionHostile(it.asString) }
        jsonObj.get("factionfearful").asJsonArray.forEach { factionObj.addFactionFearful(it.asString) }

        return factionObj
    }
}
