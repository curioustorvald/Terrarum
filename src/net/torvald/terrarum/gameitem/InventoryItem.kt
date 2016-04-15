package net.torvald.terrarum.gameitem

import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-01-16.
 */
interface InventoryItem {
    /**
     * Internal ID of an Item, Long
     * 0-4096: Tiles
     * 4097-32767: Various items
     * >=32768: Actor RefID
     */
    var itemID: Int

    /**
     * Weight of the item, Float
     */
    var mass: Float

    /**
     * Scale of the item. Real mass: mass * (scale^3)
     */
    var scale: Float

    /**
     * Effects applied while in pocket
     * @param gc
     * *
     * @param delta_t
     */
    fun effectWhileInPocket(gc: GameContainer, delta_t: Int)

    /**
     * Effects applied immediately only once if picked up
     * @param gc
     * *
     * @param delta_t
     */
    fun effectWhenPickedUp(gc: GameContainer, delta_t: Int)

    /**
     * Effects applied while primary button (usually left mouse button) is down
     * @param gc
     * *
     * @param delta_t
     */
    fun primaryUse(gc: GameContainer, delta_t: Int)

    /**
     * Effects applied while secondary button (usually right mouse button) is down
     * @param gc
     * *
     * @param delta_t
     */
    fun secondaryUse(gc: GameContainer, delta_t: Int)

    /**
     * Effects applied immediately only once if thrown from pocket
     * @param gc
     * *
     * @param delta_t
     */
    fun effectWhenThrownAway(gc: GameContainer, delta_t: Int)
}