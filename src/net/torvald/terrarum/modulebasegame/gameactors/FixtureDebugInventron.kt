package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIDebugInventron
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2025-02-01.
 */
class FixtureDebugInventron : FixtureBase {

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 2),
        nameFun = { Lang["ITEM_DEBUG_INVENTRON"] },
        mainUI = UIDebugInventron()
    )

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/mysterious_atm.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/mysterious_atm_emsv.tga")


        density = 2800.0
        setHitboxDimension(TILE_SIZE * 2, TILE_SIZE * 2, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE * 2, TILE_SIZE * 2)).let {
            it.setRowsAndFrames(1,1)
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, TILE_SIZE * 2, TILE_SIZE * 2)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 2000.0
    }
}