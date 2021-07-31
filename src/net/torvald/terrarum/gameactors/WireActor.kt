package net.torvald.terrarum.gameactors

import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameitem.ItemID

/**
 * Created by minjaesong on 2021-07-30.
 */
class WireActor(id: ActorID) : ActorWithBody(RenderOrder.WIRES, PhysProperties.IMMOBILE) {

    init {
        referenceID = id
        setHitboxDimension(2, 2, 0, 0)
    }

    private var oldWireId = ""

    /**
     * @param itemID must start with "wire@"
     */
    fun setWire(itemID: ItemID, worldX: Int, worldY: Int) {
        if (oldWireId != itemID) {
            if (sprite == null) {
                makeNewSprite(CommonResourcePool.getAsTextureRegionPack(itemID))
                sprite!!.delays = floatArrayOf(1f,1f)
                sprite!!.setRowsAndFrames(2, 16)
            }
            else sprite!!.setSpriteImage(CommonResourcePool.getAsTextureRegionPack(itemID))

            oldWireId = itemID
        }
        setPosition(worldX * TILE_SIZE + 1.0, (worldY + 1.0) * TILE_SIZE - 1.0) // what the fuck?
        sprite!!.currentRow = 1
        sprite!!.currentFrame = 15
    }

    override fun update(delta: Float) {
        // set autotiling here
        // hint: manipulate `sprite!!.currentFrame`

    }

}