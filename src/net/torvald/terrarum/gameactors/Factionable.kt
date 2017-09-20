package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameactors.faction.Faction
import java.util.*

/**
 * Created by minjaesong on 2016-02-15.
 */
interface Factionable {

    var faction: HashSet<Faction>

}