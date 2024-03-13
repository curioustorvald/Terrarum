package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.WireEmissionType
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.cos

/**
 * Created by minjaesong on 2024-03-05.
 */
interface InternalActor {

}

/**
 * Created by minjaesong on 2021-07-30.
 */
class WireActor : ActorWithBody, NoSerialise, InternalActor {

    private constructor()

    constructor(id: ActorID) : super(RenderOrder.OVERLAY, PhysProperties.IMMOBILE(), id)

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

    override fun updateImpl(delta: Float) {
    }

    private fun essfun0(x: Double) = -cos(Math.PI * x) / 2.0 + 0.5
    private fun essfun(x: Double) = essfun0(x)

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible && sprite != null) {
            BlendMode.resolve(drawMode, batch)

            // signal wires?
            if (WireCodex.wireProps[wireID]?.accepts == "digital_bit") {
                val strength = world?.getWireEmitStateOf(worldX, worldY, wireID)?.x ?: 0.0

                // draw base (unlit) sprite
                batch.color = Color.WHITE
                (sprite as SheetSpriteAnimation).currentRow = 0
                drawSpriteInGoodPosition(frameDelta, sprite!!, batch, 0, Color.WHITE)

                // draw lit sprite
                val alpha = Color(1f, 1f, 1f, essfun(strength.coerceIn(0.0, 1.0)).toFloat())
                (sprite as SheetSpriteAnimation).currentRow = 1
                drawSpriteInGoodPosition(frameDelta, sprite!!, batch, 0, alpha)
            }
            else {
                (sprite as SheetSpriteAnimation).currentRow = 0
                drawSpriteInGoodPosition(frameDelta, sprite!!, batch, 0, Color.WHITE)
            }


        }
    }
}

/**
 * Created by minjaesong on 2024-03-07.
 */
class WirePortActor : ActorWithBody, NoSerialise, InternalActor {


    private constructor()

    constructor(id: ActorID) : super(RenderOrder.OVERLAY, PhysProperties.IMMOBILE(), id)

    init {
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)
        renderOrder = RenderOrder.OVERLAY
    }

    private var portID: WireEmissionType = ""
    private var worldX = 0
    private var worldY = 0

    /**
     */
    fun setPort(emissionType: WireEmissionType, worldX: Int, worldY: Int) {
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)

        if (portID != emissionType) {
            WireCodex.getWirePortSpritesheet(emissionType)?.let { (sheet, x, y) ->
                if (sprite == null) {
                    makeNewSprite(sheet).let {
                        it.delays = floatArrayOf(Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY)
                        it.setRowsAndFrames(1, 16)
                    }
                }
                else {
                    (sprite as SheetSpriteAnimation).let {
                        it.setSpriteImage(sheet)
                    }
                }

                (sprite as SheetSpriteAnimation).let {
                    it.currentFrame = x
                    it.currentRow = y
                }

                portID = emissionType
            }
        }


        this.worldX = worldX
        this.worldY = worldY

        setPosition((worldX + 0.5) * TILE_SIZE, (worldY + 1.0) * TILE_SIZE - 1.0) // what the fuck?
    }

    override fun updateImpl(delta: Float) {
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible && sprite != null && (Terrarum.ingame!! as TerrarumIngame).selectedWireRenderClass.isNotBlank()) {
            BlendMode.resolve(drawMode, batch)
            drawSpriteInGoodPosition(frameDelta, sprite!!, batch)
        }
    }
}