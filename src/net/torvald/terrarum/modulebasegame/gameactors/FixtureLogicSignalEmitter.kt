package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2021-08-08.
 */
class FixtureLogicSignalEmitter : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = true

    constructor() : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 1),
            nameFun = { Lang["ITEM_LOGIC_SIGNAL_EMITTER"] }
    )

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_source.tga")

        density = 1400.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE, TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = MASS

        setWireEmitterAt(0, 0, "digital_bit")
        setWireEmissionAt(0, 0, Vector2(1.0, 0.0))
    }

    override fun updateSignal() {
        setWireEmissionAt(0, 0, Vector2(1.0, 0.0))
    }

    override fun dispose() {
        removeFromTooltipRecord()
    }

    companion object {
        const val MASS = 1.0
    }

}

