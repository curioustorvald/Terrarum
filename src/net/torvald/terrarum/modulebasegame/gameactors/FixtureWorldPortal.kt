package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIWorldPortal
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-05-28.
 */
class FixtureWorldPortal : FixtureBase {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 5, 2),
        nameFun = { Lang["ITEM_WORLD_PORTAL"] },
            mainUI = UIWorldPortal()
    ) {
        // TODO do something with (mainUI as UIWorldPortal).***
    }


    init {
        val itemImage = FixtureItemBase.getItemImageFromSheet("basegame", "sprites/fixtures/portal_device.tga", 80, 32)

        density = 2900.0
        setHitboxDimension(80, 32, 0, 0)
        makeNewSprite(TextureRegionPack(itemImage.texture, 80, 32)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = FixtureLogicSignalEmitter.MASS
    }

    override fun reload() {
        super.reload()

        // TODO do something with (mainUI as UIWorldPortal).***
    }
}