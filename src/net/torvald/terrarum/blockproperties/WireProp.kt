package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.Codex
import net.torvald.terrarum.TaggedProp
import net.torvald.terrarum.gameitems.ItemID

/**
 * Created by minjaesong on 2021-07-28.
 */
class WireProp : TaggedProp {

    var id: ItemID = ""
    var numericID: Int = -1
    var nameKey: String = ""

    var renderClass: String = ""
    var accepts: String = ""
    var inputCount: Int = 0
    var inputType: String = ""
    var outputType: String = ""

    var branching: Int = 0 // 0: can't; 1: tee-only, 2: cross-only, 3: tee and cross

    /**
     * Mainly intended to be used by third-party modules
     */
    val extra = Codex()

    @Transient var tags = HashSet<String>()

    override fun hasTag(s: String) = tags.contains(s)
}