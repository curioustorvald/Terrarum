package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-10-03.
 */
class FixtureInductionMotor : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 1, 1),
        nameFun = { Lang["ITEM_INDUCTION_MOTOR"] }
    )

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/induction_motor.tga")

        density = 7874.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE, TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = MASS

        setWireEmitterAt(0, 0, "axle")
        setWireEmissionAt(0, 0, Vector2(16.0, 1024.0)) // speed and torque
    }

    override fun updateSignal() {
        setWireEmissionAt(0, 0, Vector2(16.0, 1024.0)) // speed and torque
    }

    override fun dispose() {
        tooltipShowing.remove(tooltipHash)
    }

    companion object {
        const val MASS = 20.0
    }


}