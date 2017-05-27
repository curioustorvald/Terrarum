package net.torvald.terrarum.gameactors

import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.blockproperties.BlockCodex
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-15.
 */
open class DroppedItem(private val item: GameItem) : ActorWithPhysics(Actor.RenderOrder.MIDTOP) {

    init {
        if (item.dynamicID >= ItemCodex.ACTORID_MIN)
            throw RuntimeException("Attempted to create DroppedItem actor of a real actor; the real actor must be dropped instead.")

        isVisible = true

        avBaseMass = if (item.dynamicID < BlockCodex.TILE_UNIQUE_MAX)
            BlockCodex[item.dynamicID].density / 1000.0
        else
            ItemCodex[item.dynamicID].mass

        scale = ItemCodex[item.dynamicID].scale
    }

    override fun update(gc: GameContainer, delta: Int) {
    }

    override fun drawBody(g: Graphics) {
        super.drawBody(g)
    }

    override fun drawGlow(g: Graphics) {
        super.drawGlow(g)
    }
}