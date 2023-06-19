package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.WireCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory.Companion.CAPACITY_MODE_WEIGHT
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIWorldPortal
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import java.util.HashMap

/**
 * Created by minjaesong on 2023-05-28.
 */
class FixtureWorldPortal : Electric {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 5, 2),
        nameFun = { Lang["ITEM_WORLD_PORTAL"] },
        mainUI = UIWorldPortal(),
//        inventory = FixtureInventory(200, CAPACITY_MODE_WEIGHT)
    ) {
        // TODO do something with (mainUI as UIWorldPortal).***
//        (mainUI as UIWorldPortal).let { ui ->
//            ui.transitionalCargo.chestInventory = this.inventory!!
//            ui.transitionalCargo.chestNameFun = this.nameFun
//        }
    }


    init {
        val itemImage = FixtureItemBase.getItemImageFromSheet("basegame", "sprites/fixtures/portal_device.tga", 80, 32)

        density = 2900.0
        setHitboxDimension(80, 32, 0, 0)
        makeNewSprite(TextureRegionPack(itemImage.texture, 80, 32)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = FixtureLogicSignalEmitter.MASS

        setWireSinkAt(2, 1, "digital_bit")
    }

    override fun onRisingEdge(readFrom: BlockBoxIndex) {
        println("[FixtureWorldPortal] teleport! ($readFrom)")
    }

    override fun reload() {
        super.reload()

        // TODO do something with (mainUI as UIWorldPortal).***
    }

}