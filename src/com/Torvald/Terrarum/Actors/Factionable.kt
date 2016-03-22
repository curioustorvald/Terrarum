package com.Torvald.Terrarum.Actors

import com.Torvald.Terrarum.Actors.Faction.Faction
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */
interface Factionable {

    var faction: HashSet<Faction>

}