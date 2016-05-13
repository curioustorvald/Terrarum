package net.torvald.terrarum.gameactors.faction

import net.torvald.random.HQRNG
import net.torvald.terrarum.Terrarum
import java.util.HashSet

/**
 * Created by minjaesong on 16-02-15.
 */
class Faction(name: String) : Comparable<Faction> {

    var factionName: String = name
    lateinit var factionAmicable: HashSet<String>
    lateinit var factionNeutral: HashSet<String>
    lateinit var factionHostile: HashSet<String>
    lateinit var factionFearful: HashSet<String>
    var referenceID: Long = generateUniqueID()

    init {
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

    private fun generateUniqueID(): Long {
        var ret: Long
        do {
            ret = HQRNG().nextLong().or(0x80000000L).and(0xFFFFFFFFL) // guaranteed to be 2147483648..4294967295
        } while (FactionCodex.hasFaction(ret)) // check for collision
        return ret
    }

    override fun equals(other: Any?) = referenceID == (other as Faction).referenceID
    override fun hashCode() = (referenceID - 0x80000000L).toInt()
    override fun toString() = "Faction, ID: $referenceID ($factionName)"
    override fun compareTo(other: Faction): Int = (this.referenceID - other.referenceID).toInt().sign()

    fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else this
}
