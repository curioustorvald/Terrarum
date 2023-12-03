package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * No GUI yet!
 *
 * Created by minjaesong on 2023-12-04.
 */
class FixtureSmelterBasic : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("basicsmelter")

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 2), // temporary value, will be overwritten by spawn()
        nameFun = { Lang["ITEM_SMELTER_BASIC"] }
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/smelter_basic.tga")
//        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/smelter_basic_glow.tga") // put this sprite to the hypothetical "SpriteIllum"

        density = BlockCodex[Block.STONE].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }
        /*makeNewSpriteGlow(TextureRegionPack(itemImage2.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }*/

        actorValue[AVKey.BASEMASS] = 50.0

        mainUIopenFun = { ui ->
            (mainUI as? UIInventoryFull)?.openCrafting(mainUI!!.handler)
        }
    }

    @Transient override var lightBoxList = arrayListOf(Lightbox(Hitbox(0.0, 0.0, TILE_SIZED * 2, TILE_SIZED * 2), Cvec(0.5f, 0.18f, 0f, 0f)))

    @Transient private val actorBlocks = arrayOf(
        arrayOf(Block.ACTORBLOCK_ALLOW_MOVE_DOWN, null),
        arrayOf(Block.ACTORBLOCK_NO_COLLISION, Block.ACTORBLOCK_ALLOW_MOVE_DOWN)
    )
    override fun placeActorBlocks() {
        forEachBlockbox { x, y, ox, oy ->
            val tile = actorBlocks[oy][ox]
            if (tile != null) {
                world!!.setTileTerrain(x, y, tile, true)
            }
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