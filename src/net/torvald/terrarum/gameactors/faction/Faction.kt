package net.torvald.terrarum.gameactors.faction

import net.torvald.random.HQRNG
import net.torvald.terrarum.Terrarum
import java.util.HashSet

/**
 * Created by minjaesong on 16-02-15.
 */
class Faction(factionName: String) {

    lateinit var factionName: String
    lateinit var factionAmicable: HashSet<String>
    lateinit var factionNeutral: HashSet<String>
    lateinit var factionHostile: HashSet<String>
    lateinit var factionFearful: HashSet<String>
    var factionID: Long = generateUniqueID()

    init {
        this.factionName = factionName
        factionAmicable = HashSet<String>()
        factionNeutral = HashSet<String>()
        factionHostile = HashSet<String>()
        factionFearful = HashSet<String>()
    }

    fun renewFactionName(factionName: String) {
        this.factionName = factionName
    }

    fun addFactionAmicable(faction: String) {
        factionAmicable.add(faction)
    }

    fun addFactionNeutral(faction: String) {
        factionNeutral.add(faction)
    }

    fun addFactionHostile(faction: String) {
        factionHostile.add(faction)
    }

    fun addFactionFearful(faction: String) {
        factionFearful.add(faction)
    }

    fun removeFactionAmicable(faction: String) {
        factionAmicable.remove(faction)
    }

    fun removeFactionNeutral(faction: String) {
        factionNeutral.remove(faction)
    }

    fun removeFactionHostile(faction: String) {
        factionHostile.remove(faction)
    }

    fun removeFactionFearful(faction: String) {
        factionFearful.remove(faction)
    }

    fun generateUniqueID(): Long {
        fun Long.abs() = if (this < 0) -this else this
        return HQRNG().nextLong().abs() // set new ID
    }
}
