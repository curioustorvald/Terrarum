package net.torvald.terrarum.gameactors

import net.torvald.terrarum.itemproperties.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.tileproperties.TileCodex
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-15.
 */
class DroppedItem(private val item: InventoryItem) : ActorWithPhysics(Actor.RenderOrder.MIDTOP) {

    init {
        if (item.dynamicID >= ItemCodex.ACTORID_MIN)
            throw RuntimeException("Attempted to create DroppedItem actor of a real actor; the real actor must be dropped instead.")

        isVisible = true

        avBaseMass = if (item.dynamicID < TileCodex.TILE_UNIQUE_MAX)
            TileCodex[item.dynamicID].density / 1000.0
        else
            ItemCodex[item.dynamicID].mass

        scale = ItemCodex[item.dynamicID].scale
    }

    override fun update(gc: GameContainer, delta: Int) {
        item.effectWhenEquipped(gc, delta)
    }

    override fun drawBody(g: Graphics) {

    }
}