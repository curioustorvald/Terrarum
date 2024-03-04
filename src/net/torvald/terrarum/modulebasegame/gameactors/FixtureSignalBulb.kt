package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2024-03-01.
 */
class FixtureSignalBulb : Electric {

    @Transient override val spawnNeedsFloor = false
    @Transient override val spawnNeedsWall = true

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 1, 1),
        nameFun = { Lang["ITEM_COPPER_BULB"] }
    )

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/copper_bulb.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/copper_bulb_emissive.tga")

        density = 1400.0
        setHitboxDimension(TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)).let {
            it.setRowsAndFrames(2,1)
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }

        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)).let {
            it.setRowsAndFrames(2,1)
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }

        actorValue[AVKey.BASEMASS] = FixtureLogicSignalEmitter.MASS


        setWireSinkAt(0, 0, "digital_bit")
    }

    private fun light(state: Boolean) {
        (sprite as SheetSpriteAnimation).currentRow = state.toInt()
        (spriteEmissive as SheetSpriteAnimation).currentRow = state.toInt()
    }

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)
        light(getWireStateAt(0, 0, "digital_bit").x >= ELECTIC_THRESHOLD_HIGH)
    }
}