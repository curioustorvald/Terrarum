package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory.Companion.CAPACITY_MODE_COUNT
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-07-08.
 */
internal class FixtureStorageChest : FixtureBase {


    constructor() : super(
            BlockBox(BlockBox.ALLOW_MOVE_DOWN, 1, 1),
            mainUI = UIStorageChest(),
            inventory = FixtureInventory(40, CAPACITY_MODE_COUNT),
            nameFun = { Lang["ITEM_STORAGE_CHEST"] }
    ) {

        (mainUI as UIStorageChest).chestInventory = this.inventory!!
        (mainUI as UIStorageChest).chestNameFun = this.nameFun

        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/storage_chest.tga")

        setHitboxDimension(TILE_SIZE, TILE_SIZE, 1, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, 18, 18)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = MASS


        printStackTrace(this)
    }

    override fun reload() {
        super.reload()
        // doing this is required as when things are deserialised, constructor is called, THEN the fields are
        // filled in, thus the initialised mainUI has a stale reference;
        // we fix it by simply giving a new reference to the mainUI
        (mainUI as UIStorageChest).chestInventory = this.inventory!!
        (mainUI as UIStorageChest).chestNameFun = this.nameFun
    }

    companion object {
        const val MASS = 2.0
    }
}
