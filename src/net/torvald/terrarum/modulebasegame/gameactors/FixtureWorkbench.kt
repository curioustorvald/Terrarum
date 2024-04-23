package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UICrafting
import net.torvald.terrarum.modulebasegame.ui.UIEngravingTextSign
import net.torvald.terrarum.modulebasegame.ui.UIMusicalWorkbench
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-09-20.
 */
class FixtureWorkbench : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("basiccrafting")

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 1),
        nameFun = { Lang["ITEM_WORKBENCH"] },
        mainUI = UICrafting(null)
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

/**
 * Created by minjaesong on 2024-03-14.
 */
class FixtureElectricWorkbench : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("soldering")

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_ELECTRIC_WORKBENCH"] },
        mainUI = UICrafting(null)
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/electric_workbench.tga")

        density = BlockCodex[Block.PLANK_NORMAL].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, 32, 32)).let {
            it.setRowsAndFrames(2,1)
        }

        actorValue[AVKey.BASEMASS] = 40.0
    }

}

/**
 * Created by minjaesong on 2024-03-22.
 */
class FixtureEngravingWorkbench : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("engraving")

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_ENGRAVING_WORKBENCH"] },
        mainUI = UIEngravingTextSign()
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/engraving_workbench.tga")

        density = BlockCodex[Block.PLANK_NORMAL].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, 32, 32)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 40.0
    }

}

/**
 * Created by minjaesong on 2024-04-22.
 */
class FixtureMusicalWorkbench : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("musicbox")

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_MUSICAL_WORKBENCH"] },
        mainUI = UIMusicalWorkbench()
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/musical_workbench.tga")

        density = BlockCodex[Block.PLANK_NORMAL].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, 32, 32)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 40.0
    }
}