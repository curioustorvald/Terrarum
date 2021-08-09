package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

class FixtureLogicSignalEmitter(nameFun: () -> String) : FixtureBase(BlockBox(BlockBox.NO_COLLISION, 1, 1), nameFun = nameFun) {

    override val wireEmitterType = "digital_bit"
    override val wireEmission = Vector2(1.0, 0.0)

    init {
        density = 1400.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, -1)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("basegame-sprites-fixtures-signal_source.tga").texture, TILE_SIZE, TILE_SIZE))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = MASS
    }

    override fun dispose() { }

    companion object {
        const val MASS = 1.0
    }

    override fun update(delta: Float) {
        // set emit
        worldBlockPos?.let { (x, y) ->
            WireCodex.getAll().filter { it.renderClass == "signal" }.forEach { prop ->
                world?.setWireEmitStateOf(x, y, prop.id, wireEmission)
            }
        }
    }
}

