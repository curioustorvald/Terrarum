package net.torvald.terrarum.gameitem

import net.torvald.terrarum.itemproperties.Material
import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-01-16.
 */
abstract class InventoryItem {
    /**
     * Internal ID of an Item, Long
     * 0-4095: Tiles
     * 4096-32767: Static items
     * 32768-16777215: Dynamic items
     * >= 16777216: Actor RefID
     */
    abstract val id: Int

    abstract var baseMass: Double

    abstract var baseToolSize: Double?

    /**
     * Where to equip the item
     */
    var equipPosition: Int = EquipPosition.NULL

    var material: Material? = null

    /**
     * Apparent mass of the item. (basemass * scale^3)
     */
    open var mass: Double
        get() = baseMass * scale * scale * scale
        set(value) { baseMass = value / (scale * scale * scale) }

    /**
     * Apparent tool size (or weight in kg). (baseToolSize  * scale^3)
     */
    open var toolSize: Double?
        get() = if (baseToolSize != null) baseToolSize!! * scale * scale * scale else null
        set(value) {
            if (value != null)
                if (baseToolSize != null)
                    baseToolSize = value / (scale * scale * scale)
                else
                    throw NullPointerException("baseToolSize is null; this item is not a tool or you're doing it wrong")
            else
                throw NullPointerException("null input; nullify baseToolSize instead :p")
        }

    /**
     * Scale of the item.
     *
     * For static item, it must be 1.0. If you tinkered the item to be bigger,
     * it must be re-assigned as Dynamic Item
     */
    abstract var scale: Double

    /**
     * Effects applied continuously while in pocket
     */
    open fun effectWhileInPocket(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied immediately only once if picked up
     */
    open fun effectWhenPickedUp(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied (continuously or not) while primary button (usually left mouse button) is down
     */
    open fun primaryUse(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied (continuously or not) while secondary button (usually right mouse button) is down
     */
    open fun secondaryUse(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied immediately only once if thrown from pocket
     */
    open fun effectWhenThrown(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied (continuously or not) when equipped (drawn)
     */
    open fun effectWhenEquipped(gc: GameContainer, delta: Int) { }

    /**
     * Effects applied only once when unequipped
     */
    open fun effectWhenUnEquipped(gc: GameContainer, delta: Int) { }

    
    override fun toString(): String {
        return id.toString()
    }

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return id == (other as InventoryItem).id
    }
}

object EquipPosition {
    const val NULL = -1

    const val ARMOUR = 0
    // you can add alias to address something like LEGGINGS, BREASTPLATE, RINGS, NECKLACES, etc.
    const val BODY_BACK = 1 // wings, jetpacks, etc.
    const val BODY_BUFF2 = 2
    const val BODY_BUFF3 = 3
    const val BODY_BUFF4 = 4
    const val BODY_BUFF5 = 5
    const val BODY_BUFF6 = 6
    const val BODY_BUFF7 = 7
    const val BODY_BUFF8 = 8

    const val HAND_GRIP = 9
    const val HAND_GAUNTLET = 10
    const val HAND_BUFF2 = 11
    const val HAND_BUFF3 = 12
    const val HAND_BUFF4 = 13

    const val FOOTWEAR = 14

    const val HEADGEAR = 15

    const val INDEX_MAX = 15
}