package net.torvald.terrarum.blockproperties

import net.torvald.terrarum.gameitem.ItemID

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

}