package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull
import net.torvald.terrarum.modulebasegame.ui.UIWallCalendar
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-09-20.
 */
class FixtureWorkbench : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("basiccrafting")

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 1),
        nameFun = { Lang["ITEM_WORKBENCH"] }
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/workbench.tga")

        density = BlockCodex[Block.PLANK_NORMAL].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0

        mainUIopenFun = { ui ->
            (mainUI as? UIInventoryFull)?.openCrafting(mainUI!!.handler)
        }
    }

    @Transient private var mainUIhookHackInstalled = false
    override fun update(delta: Float) {
        // adding UI to the fixture as players may right-click on the workbenches instead of pressing a keyboard key
        (INGAME as? TerrarumIngame)?.let { ingame ->
            if (!mainUIhookHackInstalled && ingame.uiInventoryPlayerReady) {
                mainUIhookHackInstalled = true
                this.mainUI = ingame.uiInventoryPlayer // this field is initialised only after a full load so this hack is necessary
            }
        }

        super.update(delta)
    }

}