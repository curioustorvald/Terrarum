package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIStorageChest
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2024-03-03.
 */
class FixtureTable : FixtureBase {
    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 2),
        nameFun = { Lang["ITEM_TABLE_OAK"] }
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/table_1.tga")

        setHitboxDimension(2*TILE_SIZE, 2*TILE_SIZE, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, 32, 33)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0
    }
}

/**
 * Created by minjaesong on 2024-03-03.
 */
class FixtureTableEbony : FixtureBase {
    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 2),
        nameFun = { Lang["ITEM_TABLE_EBONY"] }
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/table_2.tga")

        setHitboxDimension(2*TILE_SIZE, 2*TILE_SIZE, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, 32, 33)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0
    }
}

/**
 * Created by minjaesong on 2024-03-03.
 */
class FixtureTableBirch : FixtureBase {
    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 2),
        nameFun = { Lang["ITEM_TABLE_BIRCH"] }
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/table_3.tga")

        setHitboxDimension(2*TILE_SIZE, 2*TILE_SIZE, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, 32, 33)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0
    }
}

/**
 * Created by minjaesong on 2024-03-03.
 */
class FixtureTableRosewood : FixtureBase {
    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 2),
        nameFun = { Lang["ITEM_TABLE_ROSEWOOD"] }
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/table_4.tga")

        setHitboxDimension(2*TILE_SIZE, 2*TILE_SIZE, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, 32, 33)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0
    }
}