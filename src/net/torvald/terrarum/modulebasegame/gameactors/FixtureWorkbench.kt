package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIWallCalendar
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-09-20.
 */
class FixtureWorkbench : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("basiccrafting")

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 1),
        nameFun = { Lang["ITEM_WORKBENCH"] },
        mainUI = UIWallCalendar()
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/workbench.tga")

        density = BlockCodex[Block.PLANK_NORMAL].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0
    }

}