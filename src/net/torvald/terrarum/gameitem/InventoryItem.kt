package net.torvald.terrarum.gameitem

import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-01-16.
 */
interface InventoryItem {
    /**
     * Internal ID of an Item, Long
     * 0-4095: Tiles
     * 4096-32767: Static items
     * 32768-16777215: Dynamic items
     * >= 16777216: Actor RefID
     */
    val itemID: Int

    /**
     * Where to equip the item
     */
    val equipPosition: Int

    /**
     * Base mass of the item. Real mass must be calculated from
     *      mass * scale^3
     */
    var mass: Double

    /**
     * Scale of the item.
     *
     * For static item, it must be 1.0. If you tinkered the item to be bigger,
     * it must be re-assigned as Dynamic Item
     */
    var scale: Double

    /**
     * Effects applied continuously while in pocket
     */
    fun effectWhileInPocket(gc: GameContainer, delta: Int)

    /**
     * Effects applied immediately only once if picked up
     */
    fun effectWhenPickedUp(gc: GameContainer, delta: Int)

    /**
     * Effects applied (continuously or not) while primary button (usually left mouse button) is down
     */
    fun primaryUse(gc: GameContainer, delta: Int)

    /**
     * Effects applied (continuously or not) while secondary button (usually right mouse button) is down
     */
    fun secondaryUse(gc: GameContainer, delta: Int)

    /**
     * Effects applied immediately only once if thrown from pocket
     */
    fun effectWhenThrown(gc: GameContainer, delta: Int)

    /**
     * Effects applied (continuously or not) when equipped
     */
    fun effectWhenEquipped(gc: GameContainer, delta: Int)

    /**
     * Effects applied only once when unequipped
     */
    fun effectWhenUnEquipped(gc: GameContainer, delta: Int)
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