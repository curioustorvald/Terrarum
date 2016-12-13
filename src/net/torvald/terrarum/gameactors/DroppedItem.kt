package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.itemproperties.ItemPropCodex
import net.torvald.terrarum.tileproperties.TilePropCodex
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-15.
 */
class DroppedItem(private val item: InventoryItem) : ActorWithBody() {

    init {
        if (item.itemID >= ItemPropCodex.ITEM_COUNT_MAX)
            throw RuntimeException("Attempted to create DroppedItem actor of a real actor; the real actor must be dropped instead.")

        isVisible = true

        mass = if (item.itemID < TilePropCodex.TILE_UNIQUE_MAX)
            TilePropCodex.getProp(item.itemID).density / 1000.0
        else
            ItemPropCodex.getProp(item.itemID).mass

        scale = ItemPropCodex.getProp(item.itemID).scale
    }

    override fun update(gc: GameContainer, delta: Int) {
        item.effectWhenTakenOut(gc, delta)
    }

    override fun drawBody(gc: GameContainer, g: Graphics) {

    }
}