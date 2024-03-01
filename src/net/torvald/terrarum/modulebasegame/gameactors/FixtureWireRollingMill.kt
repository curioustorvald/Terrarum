package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UICrafting
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2024-03-02.
 */
class FixtureWireRollingMill : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("wirerollingmill")

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_WIRE_ROLLING_MILL"] },
        mainUI = UICrafting(null)
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/wire_rolling_mill.tga")

        density = BlockCodex[Block.STONE].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 50.0
    }

}