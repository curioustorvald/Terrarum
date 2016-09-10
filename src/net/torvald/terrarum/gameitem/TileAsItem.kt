package net.torvald.terrarum.gameitem

import net.torvald.terrarum.tileproperties.TilePropCodex
import org.newdawn.slick.GameContainer

/**
 * Created by minjaesong on 16-03-15.
 */
class TileAsItem(tileNum: Int) : InventoryItem {

    override var itemID: Int = 0
    override var mass: Double = 0.0
    override var scale: Double = 1.0

    init {
        itemID = tileNum
        mass = TilePropCodex.getProp(tileNum).density / 1000.0
    }

    override fun effectWhileInPocket(gc: GameContainer, delta_t: Int) {
        throw UnsupportedOperationException()
    }

    override fun effectWhenPickedUp(gc: GameContainer, delta_t: Int) {
        throw UnsupportedOperationException()
    }

    override fun primaryUse(gc: GameContainer, delta_t: Int) {
        throw UnsupportedOperationException()
    }

    override fun secondaryUse(gc: GameContainer, delta_t: Int) {
        throw UnsupportedOperationException()
    }

    override fun effectWhenThrown(gc: GameContainer, delta_t: Int) {
        throw UnsupportedOperationException()
    }

    /**
     * Effects applied (continuously or not) while thrown to the world
     */
    override fun effectWhenTakenOut(gc: GameContainer, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Effects applied (continuously or not) while thrown to the world,
     * called by the proxy Actor
     */
    override fun worldActorEffect(gc: GameContainer, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}