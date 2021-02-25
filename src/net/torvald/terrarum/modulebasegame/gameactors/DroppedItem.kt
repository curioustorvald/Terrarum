package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex

/**
 * Created by minjaesong on 2016-03-15.
 */
open class DroppedItem(private val item: GameItem) : ActorWithBody(RenderOrder.MIDTOP, PhysProperties.PHYSICS_OBJECT) {

    init {
        if (item.dynamicID.startsWith("actor@"))
            throw RuntimeException("Attempted to create DroppedItem actor of a real actor; the real actor must be dropped instead.")

        isVisible = true

        avBaseMass = if (item.dynamicID.startsWith("item@"))
            ItemCodex[item.dynamicID]!!.mass
        else
            BlockCodex[item.dynamicID].density / 1000.0 // block and wall

        actorValue[AVKey.SCALE] = ItemCodex[item.dynamicID]!!.scale
    }

    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun drawGlow(batch: SpriteBatch) {
        super.drawGlow(batch)
    }

    override fun drawBody(batch: SpriteBatch) {
        super.drawBody(batch)
    }
}