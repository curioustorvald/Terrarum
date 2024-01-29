package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.gdx.graphics.Cvec
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.gameparticles.ParticleVanishingSprite
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UICrafting
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull
import net.torvald.terrarum.modulebasegame.ui.UISmelterBasic
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * No GUI yet!
 *
 * Created by minjaesong on 2023-12-04.
 */
class FixtureSmelterBasic : FixtureBase, CraftingStation {

    var fuelCaloriesNow = 0f
    var fuelCaloriesMax: Float? = null
    var temperature = 0f // 0f..1f
    var progress = 0f

    var oreItem: InventoryPair? = null
    var fireboxItem: InventoryPair? = null
    var productItem: InventoryPair? = null

    override val canBeDespawned: Boolean
        get() = oreItem == null && fireboxItem == null && productItem == null

    @Transient override val tags = listOf("basicsmelter")

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 3, 4), // temporary value, will be overwritten by spawn()
        nameFun = { Lang["ITEM_SMELTER_SMALL"] },
    ) {
        CommonResourcePool.addToLoadingList("particles-tiki_smoke.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "particles/bigger_smoke.tga"), 16, 16)
        }
        CommonResourcePool.loadAll()


        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/smelter_tall.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/smelter_tall_emsv.tga")

        density = BlockCodex[Block.STONE].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 100.0

        this.mainUI = UISmelterBasic(this)
    }

    @Transient override var lightBoxList = arrayListOf(Lightbox(Hitbox(0.0, 2*TILE_SIZED, TILE_SIZED * 2, TILE_SIZED * 2), Cvec(0.5f, 0.18f, 0f, 0f)))

    @Transient private val actorBlocks = arrayOf(
        arrayOf(Block.ACTORBLOCK_NO_COLLISION, Block.ACTORBLOCK_NO_COLLISION, null),
        arrayOf(Block.ACTORBLOCK_NO_COLLISION, Block.ACTORBLOCK_NO_COLLISION, null),
        arrayOf(Block.ACTORBLOCK_NO_COLLISION, Block.ACTORBLOCK_NO_COLLISION, null),
        arrayOf(Block.ACTORBLOCK_NO_COLLISION, Block.ACTORBLOCK_NO_COLLISION, Block.ACTORBLOCK_NO_COLLISION),
    )
    override fun placeActorBlocks() {
        forEachBlockbox { x, y, ox, oy ->
            val tile = actorBlocks[oy][ox]
            if (tile != null) {
                world!!.setTileTerrain(x, y, tile, true)
            }
        }
    }

    private var nextDelay = 0.25f
    private var spawnTimer = 0f

    override fun update(delta: Float) {
        super.update(delta)


        // emit smokes TODO: only when hot
        if (spawnTimer >= nextDelay) {
            (Terrarum.ingame as TerrarumIngame).addParticle(
                ParticleVanishingSprite(
                CommonResourcePool.getAsTextureRegionPack("particles-tiki_smoke.tga"),
                25f, true, hitbox.startX + TILE_SIZED, hitbox.startY + 16, false, (Math.random() * 256).toInt()
            ))

            spawnTimer -= nextDelay
            nextDelay = Math.random().toFloat() * 0.25f + 0.25f

            (sprite as? SheetSpriteAnimation)?.delays?.set(0, Math.random().toFloat() * 0.4f + 0.1f)
        }

        spawnTimer += delta
    }

}