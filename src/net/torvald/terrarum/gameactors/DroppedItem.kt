package net.torvald.terrarum.gameactors

import net.torvald.terrarum.itemproperties.ItemPropCodex
import net.torvald.terrarum.tileproperties.TilePropCodex
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-15.
 */
class DroppedItem constructor(itemID: Int) : ActorWithBody() {

    init {
        if (itemID >= ItemPropCodex.ITEM_UNIQUE_MAX)
            throw RuntimeException("Attempted to create DroppedItem actor of a real actor; the real actor must be dropped instead.")

        isVisible = true

        mass = if (itemID < 4096)
            TilePropCodex.getProp(itemID).density / 1000f
        else
            ItemPropCodex.getProp(itemID).mass
    }

    override fun update(gc: GameContainer, delta_t: Int) {

    }

    override fun drawBody(gc: GameContainer, g: Graphics) {

    }
}