package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.Codex
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2021-07-28.
 */
class WireProp {

    var id: ItemID = ""
    var numericID: Int = -1
    var nameKey: String = ""

    var renderClass: String = ""
    var accepts: String = ""
    var inputCount: Int = 0
    var inputType: String = ""
    var outputType: String = ""

    var canBranch: Boolean = true

    /**
     * Mainly intended to be used by third-party modules
     */
    val extra = Codex()

    @Transient var tags = HashSet<String>()

    fun hasTag(s: String) = tags.contains(s)
    fun hasAnyTagOf(vararg s: String) = s.any { hasTag(it) }
    fun hasAnyTag(s: Collection<String>) = s.any { hasTag(it) }
    fun hasAnyTag(s: Array<String>) = s.any { hasTag(it) }
    fun hasAllTagOf(vararg s: String) = s.all { hasTag(it) }
    fun hasAllTag(s: Collection<String>) = s.all { hasTag(it) }
    fun hasAllTag(s: Array<String>) = s.all { hasTag(it) }
    fun hasNoTagOf(vararg s: String) = s.none { hasTag(it) }
    fun hasNoTag(s: Collection<String>) = s.none { hasTag(it) }
    fun hasNoTag(s: Array<String>) = s.none { hasTag(it) }

}