package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.toInt

/**
 * FIXME Constructor is super expensive
 *
 * Created by minjaesong on 2021-07-30.
 */
class WireActor(id: ActorID) : ActorWithBody(RenderOrder.OVERLAY, PhysProperties.IMMOBILE, id) {

    companion object {
        val WIRE_NEARBY = arrayOf(
                (+1 to 0), // tileR
                (0 to +1), // tileB
                (-1 to 0), // tileL
                (0 to -1) // tileT
        )
    }

    init {
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)
    }

    private var wireID = ""
    private var worldX = 0
    private var worldY = 0


    /**
     * @param itemID must start with "wire@"
     */
    fun setWire(itemID: ItemID, worldX: Int, worldY: Int) {
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)

        if (wireID != itemID) {
            if (sprite == null) {
                makeNewSprite(CommonResourcePool.getAsTextureRegionPack(itemID))
                sprite!!.delays = floatArrayOf(1f,1f)
                sprite!!.setRowsAndFrames(2, 16)
            }
            else sprite!!.setSpriteImage(CommonResourcePool.getAsTextureRegionPack(itemID))

            wireID = itemID
        }
        this.worldX = worldX
        this.worldY = worldY
        setPosition((worldX + 0.5) * TILE_SIZE, (worldY + 1.0) * TILE_SIZE - 1.0) // what the fuck?

        sprite!!.currentRow = 0

        val nearbyTiles = getNearbyTilesPos(worldX, worldY).map { world!!.getAllWiresFrom(it.x, it.y) }
        var ret = 0
        for (i in 0..3) {
            if (nearbyTiles[i]?.contains(itemID) == true) {
                ret = ret or (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }
        sprite!!.currentFrame = ret
        sprite!!.flipVertical = true // turns out the sprites are rendered upside-down by default :(
    }

    private fun getNearbyTilesPos(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
                Point2i(x + 1, y),
                Point2i(x, y + 1),
                Point2i(x - 1, y),
                Point2i(x, y - 1)
        )
    }

    override fun update(delta: Float) {
    }

    override fun drawBody(batch: SpriteBatch) {
        if (isVisible && sprite != null) {
            if (WireCodex[wireID].accepts == "digital_3bits") {
                // "digital_3bits" must come right after three wires it bundles
                val rootID = wireID.substringBefore(':') + ":"
                var row = 0
                (WireCodex[wireID].numericID - 3 .. WireCodex[wireID].numericID - 1).forEachIndexed { index, it ->
                    val itemID = rootID + it
                    row = row or ((world?.getWireEmitStateOf(worldX, worldY, itemID)?.isNotZero == true).toInt() shl index)
                }
                sprite?.currentRow = row
            }
            else {
                sprite?.currentRow = (world?.getWireEmitStateOf(worldX, worldY, wireID)?.isNotZero == true).toInt()
            }

            BlendMode.resolve(drawMode, batch)
            drawSpriteInGoodPosition(sprite!!, batch)
        }
    }
}