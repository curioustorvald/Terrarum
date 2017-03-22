package net.torvald.terrarum.gameitem

/**
 * Created by minjaesong on 16-09-09.
 */
object IVKey {
    const val ITEMTYPE = "itemtype" // "sword1h", "sword2h", "pick", "hammer", "tile", "wall", etc
    const val UUID = "uuid" // some items need UUID to be stored


    object ItemType {
        const val BLOCK = "tile"
        const val WALL  = "wall"
        // tools
        const val PICK  = "pick"
        const val HAMMER= "hammer"
        // weapons
        const val SWORDJAB = "sword1h"
        const val SWORDSWING = "sword2h"
        // generic
        const val ARTEFACT = "artefact" // or Key Items
    }
}