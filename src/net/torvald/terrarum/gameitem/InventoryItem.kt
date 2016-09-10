package net.torvald.terrarum.gameitem

import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-01-16.
 */
interface InventoryItem {
    /**
     * Internal ID of an Item, Long
     * 0-4096: Tiles
     * 4097-32767: Static items
     * 32768-16777215: Dynamic items
     * >= 16777216: Actor RefID
     */
    val itemID: Int

    /**
     * Weight of the item
     */
    var mass: Double

    /**
     * Scale of the item. Real mass: mass * (scale^3)
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
     * Effects applied (continuously or not) while thrown to the world
     */
    fun effectWhenTakenOut(gc: GameContainer, delta: Int)

    /**
     * Effects applied (continuously or not) while thrown to the world,
     * called by the proxy Actor
     */
    fun worldActorEffect(gc: GameContainer, delta: Int)
}