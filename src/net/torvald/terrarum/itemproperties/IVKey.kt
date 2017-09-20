package net.torvald.terrarum.itemproperties

/**
 * Created by minjaesong on 2016-09-09.
 */
object IVKey {
    /** Weapon types or Armour types. Does not affect inventory categorising */
    const val ITEMTYPE = "itemtype" // "sword1h", "sword2h", "pick", "hammer", "tile", "wall", etc
    const val UUID = "uuid" // some items need UUID to be stored

    const val BASE_WEAPON_POWER = "baseweaponpower"
    const val BASE_PICK_POWER = "basepickpower"


    object ItemType {
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