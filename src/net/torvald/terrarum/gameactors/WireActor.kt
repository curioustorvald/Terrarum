package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameitems.ItemID

/**
 * FIXME Constructor is super expensive
 *
 * Created by minjaesong on 2021-07-30.
 */
class WireActor : ActorWithBody, NoSerialise {

    companion object {
        val WIRE_NEARBY = arrayOf(
                (+1 to 0), // tileR
                (0 to +1), // tileB
                (-1 to 0), // tileL
                (0 to -1) // tileT
        )
    }

    private constructor()

    constructor(id: ActorID) : super(RenderOrder.OVERLAY, PhysProperties.IMMOBILE, id)

    init {
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)
    }

    private var wireID = ""
    private var worldX = 0
    private var worldY = 0


    /**
     * @param itemID must start with "wire@"
     */
    fun setWire(itemID: ItemID, worldX: Int, worldY: Int, cnx: Int) {
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)

        if (wireID != itemID) {
            if (sprite == null) {
                makeNewSprite(CommonResourcePool.getAsTextureRegionPack(itemID)).let {
                    it.delays = floatArrayOf(1f,1f)
                    it.setRowsAndFrames(2, 16)
                }
            }
            else (sprite as SheetSpriteAnimation).setSpriteImage(CommonResourcePool.getAsTextureRegionPack(itemID))

            wireID = itemID
        }
        this.worldX = worldX
        this.worldY = worldY
        setPosition((worldX + 0.5) * TILE_SIZE, (worldY + 1.0) * TILE_SIZE - 1.0) // what the fuck?

        (sprite as SheetSpriteAnimation).currentRow = 0
        (sprite as SheetSpriteAnimation).currentFrame = cnx
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

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible && sprite != null) {
            if (WireCodex[wireID].accepts == "digital_3bits") {
                // "digital_3bits" must come right after three wires it bundles
                val rootID = wireID.substringBefore(':') + ":"
                var row = 0
                (WireCodex[wireID].numericID - 3 .. WireCodex[wireID].numericID - 1).forEachIndexed { index, it ->
                    val itemID = rootID + it
                    row = row or ((world?.getWireEmitStateOf(worldX, worldY, itemID)?.isNotZero == true).toInt() shl index)
                }
                (sprite as SheetSpriteAnimation).currentRow = row
            }
            else {
                (sprite as SheetSpriteAnimation).currentRow = (world?.getWireEmitStateOf(worldX, worldY, wireID)?.isNotZero == true).toInt()
            }

            BlendMode.resolve(drawMode, batch)
            drawSpriteInGoodPosition(frameDelta, sprite!!, batch)
        }
    }
}