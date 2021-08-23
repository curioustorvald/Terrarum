package net.torvald.terrarum.gameactors.faction

import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.ModMgr

import java.io.IOException

/**
 * Created by minjaesong on 2016-02-15.
 */
object FactionFactory {

    /**
     * @param filename with extension
     */
    @Throws(IOException::class)
    fun create(module: String, path: String): Faction {
        val jsonObj = JsonFetcher(ModMgr.getFile(module, path))
        val factionObj = Faction(jsonObj.getString("factionname"))

        jsonObj.get("factionamicable").asStringArray().forEach { factionObj.addFactionAmicable(it) }
        jsonObj.get("factionneutral").asStringArray().forEach { factionObj.addFactionNeutral(it) }
        jsonObj.get("factionhostile").asStringArray().forEach { factionObj.addFactionHostile(it) }
        jsonObj.get("factionfearful").asStringArray().forEach { factionObj.addFactionFearful(it) }

        return factionObj
    }
}
