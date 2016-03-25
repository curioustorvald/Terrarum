package com.torvald.terrarum.gameactors

import com.torvald.terrarum.gameactors.faction.Faction
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */
interface Factionable {

    var faction: HashSet<Faction>

}