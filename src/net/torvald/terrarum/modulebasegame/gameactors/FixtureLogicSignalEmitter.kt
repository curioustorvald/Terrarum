package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

class FixtureLogicSignalEmitter : FixtureBase, Electric {

    override val wireEmitterTypes: HashMap<String, BlockBoxIndex> = HashMap()
    override val wireEmission: HashMap<BlockBoxIndex, Vector2> = HashMap()
    override val wireConsumption: HashMap<BlockBoxIndex, Vector2> = HashMap()


    constructor() : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 1),
            nameFun = { Lang["ITEM_LOGIC_SIGNAL_EMITTER"] }
    )

    init {
        println("INIT AGAIN FixtureLogicSignalEmitter")

        density = 1400.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, -1)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("basegame-sprites-fixtures-signal_source.tga").texture, TILE_SIZE, TILE_SIZE))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = MASS

        wireEmitterTypes["digital_bit"] = 0
        wireEmission[0] = Vector2(1.0, 0.0)
    }

    override fun dispose() { }

    companion object {
        const val MASS = 1.0
    }

    override fun update(delta: Float) {
        // set emit
        /*worldBlockPos?.let { (x, y) ->
            WireCodex.getAll().filter { it.accepts == "digital_bit" }.forEach { prop ->
                // only set a state of wire that actually exists on the world
                if (world?.getWireGraphOf(x, y, prop.id) != null)
                    world?.setWireEmitStateOf(x, y, prop.id, wireEmission[0]!!)
            }
        }*/
    }
}

