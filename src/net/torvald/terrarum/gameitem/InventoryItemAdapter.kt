package net.torvald.terrarum.gameitem

import org.newdawn.slick.GameContainer

/**
 * Created by SKYHi14 on 2016-12-12.
 */
abstract class InventoryItemAdapter : InventoryItem {
    override abstract val itemID: Int
    override abstract val equipPosition: Int
    override abstract var mass: Double
    override abstract var scale: Double

    override fun effectWhileInPocket(gc: GameContainer, delta: Int) {
    }

    override fun effectWhenPickedUp(gc: GameContainer, delta: Int) {
    }

    override fun primaryUse(gc: GameContainer, delta: Int) {
    }

    override fun secondaryUse(gc: GameContainer, delta: Int) {
    }

    override fun effectWhenThrown(gc: GameContainer, delta: Int) {
    }

    override fun effectWhenEquipped(gc: GameContainer, delta: Int) {
    }

    override fun effectWhenUnEquipped(gc: GameContainer, delta: Int) {
    }
}