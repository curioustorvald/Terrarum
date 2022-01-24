package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
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
        CommonResourcePool.addToLoadingList("basegame-sprites-fixtures-signal_source.tga") {
            val t = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "sprites/fixtures/signal_source.tga")))
            t.flip(false, false)
            /*return*/t
        }
        CommonResourcePool.loadAll()

        density = 1400.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, -1)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("basegame-sprites-fixtures-signal_source.tga").texture, TILE_SIZE, TILE_SIZE))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = MASS
    }

    override fun update(delta: Float) {
        // the values does not get preserved on save reload??
        wireEmitterTypes["digital_bit"] = 0
        wireEmission[0] = Vector2(1.0, 0.0)

        super.update(delta)
    }

    override fun dispose() { }

    companion object {
        const val MASS = 1.0
    }

}

