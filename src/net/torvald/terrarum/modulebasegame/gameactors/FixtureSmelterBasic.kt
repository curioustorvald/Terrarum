package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameparticles.ParticleVanishingSprite
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.UISmelterBasic
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * No GUI yet!
 *
 * Created by minjaesong on 2023-12-04.
 */
class FixtureSmelterBasic : FixtureBase, CraftingStation {

    var fuelCaloriesNow = 0.0 // arbitrary number, may as well be watts or joules
    var fuelCaloriesMax: Double? = null
    var temperature = 0f // 0f..1f
    var progress = 0f // 0f..1f

    var oreItem: InventoryPair? = null
    var fireboxItem: InventoryPair? = null
    var productItem: InventoryPair? = null

    override val canBeDespawned: Boolean
        get() = oreItem == null && fireboxItem == null && productItem == null

    @Transient override val tags = listOf("basicsmelter")

    init {
        CommonResourcePool.addToLoadingList("basegame/sprites/fixtures/smelter_tall.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/smelter_tall.tga"), 48, 64)
        }
        CommonResourcePool.addToLoadingList("basegame/sprites/fixtures/smelter_tall_emsv.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/smelter_tall_emsv.tga"), 48, 64)
        }
        CommonResourcePool.loadAll()
    }

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 3, 4), // temporary value, will be overwritten by spawn()
        nameFun = { Lang["ITEM_SMELTER_SMALL"] },
    ) {
        CommonResourcePool.addToLoadingList("particles-tiki_smoke.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "particles/bigger_smoke.tga"), 16, 16)
        }
        CommonResourcePool.loadAll()



        density = BlockCodex[Block.STONE].density.toDouble()
        setHitboxDimension(48, 64, 0, 0)

        makeNewSprite(CommonResourcePool.getAsTextureRegionPack("basegame/sprites/fixtures/smelter_tall.tga")).let {
            it.setRowsAndFrames(1,1)
        }
        makeNewSpriteEmissive(CommonResourcePool.getAsTextureRegionPack("basegame/sprites/fixtures/smelter_tall_emsv.tga")).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 100.0

        this.mainUI = UISmelterBasic(this)
    }

    @Transient val light = Cvec(0.5f, 0.18f, 0f, 0f)

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

    private var nextDelayBase = 0.25f // use smokiness value of the item
    private var nextDelay = 0.25f // use smokiness value of the item
    private var spawnTimer = 0f

    companion object {
        @Transient val FUEL_CONSUMPTION = 1f
        @Transient val CALORIES_PER_ROASTING = 10 * 60 // 10 seconds @ 60 ticks per second
    }

    @Transient private val RNG = HQRNG()

    fun changeFireboxItemCount(delta: Long) {
        fireboxItem!!.qty += delta
        if (fireboxItem!!.qty <= 0L) {
            fireboxItem = null
        }
    }

    fun changeOreItemCount(delta: Long) {
        oreItem!!.qty += delta
        if (oreItem!!.qty <= 0L) {
            oreItem = null
        }
    }

    fun changeProductItemCount(delta: Long) {
        productItem!!.qty += delta
        if (productItem!!.qty <= 0L) {
            productItem = null
        }
    }

    override fun drawEmissive(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible && spriteEmissive != null) {
            BlendMode.resolve(drawMode, batch)

            (spriteEmissive as SheetSpriteAnimation).currentFrame = 1 // unlit
            drawSpriteInGoodPosition(frameDelta, spriteEmissive!!, batch, 2, Color.WHITE)

            (spriteEmissive as SheetSpriteAnimation).currentFrame = 0 // lit
            val r = 1f - (temperature - 1f).sqr()
            val g = (2f * temperature - 1f).coerceIn(0f, 1f)
            drawSpriteInGoodPosition(frameDelta, spriteEmissive!!, batch, 2, Color(r, g, g, 1f))
        }

        // debug display of hIntTilewiseHitbox
        if (KeyToggler.isOn(Input.Keys.F9)) {
            val blockMark = CommonResourcePool.getAsTextureRegionPack("blockmarkings_common").get(0, 0)

            for (y in 0..intTilewiseHitbox.height.toInt() + 1) {
                batch.color = if (y == intTilewiseHitbox.height.toInt() + 1) HITBOX_COLOURS1 else HITBOX_COLOURS0
                for (x in 0..intTilewiseHitbox.width.toInt()) {
                    batch.draw(blockMark,
                        (intTilewiseHitbox.startX.toFloat() + x) * TerrarumAppConfiguration.TILE_SIZEF,
                        (intTilewiseHitbox.startY.toFloat() + y) * TerrarumAppConfiguration.TILE_SIZEF
                    )
                }
            }

            batch.color = Color.WHITE
        }
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible && sprite != null) {
            BlendMode.resolve(drawMode, batch)

            (sprite as SheetSpriteAnimation).currentFrame = 1 // unlit
            drawSpriteInGoodPosition(frameDelta, sprite!!, batch, forcedColourFilter = Color.WHITE)

            (sprite as SheetSpriteAnimation).currentFrame = 2 // lit overlay
            val r = 1f - (temperature - 1f).sqr()
            val g = (2f * temperature - 1f).coerceIn(0f, 1f)
            drawSpriteInGoodPosition(frameDelta, sprite!!, batch, forcedColourFilter = Color(1f, g, g, r))
        }

        // debug display of hIntTilewiseHitbox
        if (KeyToggler.isOn(Input.Keys.F9)) {
            val blockMark = CommonResourcePool.getAsTextureRegionPack("blockmarkings_common").get(0, 0)

            for (y in 0..intTilewiseHitbox.height.toInt() + 1) {
                batch.color = if (y == intTilewiseHitbox.height.toInt() + 1) HITBOX_COLOURS1 else HITBOX_COLOURS0
                for (x in 0..intTilewiseHitbox.width.toInt()) {
                    batch.draw(blockMark,
                        (intTilewiseHitbox.startX.toFloat() + x) * TerrarumAppConfiguration.TILE_SIZEF,
                        (intTilewiseHitbox.startY.toFloat() + y) * TerrarumAppConfiguration.TILE_SIZEF
                    )
                }
            }

            batch.color = Color.WHITE
        }
    }

    override fun update(delta: Float) {
        super.update(delta)

        val oreItemProp = ItemCodex[oreItem?.itm]
        val fuelItemProp = ItemCodex[fireboxItem?.itm]

        // consume fuel
        if (fuelCaloriesNow > 0f) {
            fuelCaloriesNow -= FUEL_CONSUMPTION

            // raise temperature
            temperature += 1f /2048f
        }
        // take fuel from the item slot
        else if (fuelCaloriesNow <= 0f && fuelItemProp?.calories != null && fireboxItem!!.qty > 0L) {
            fuelCaloriesNow = fuelItemProp.calories
            fuelCaloriesMax = fuelItemProp.calories
            nextDelayBase = fuelItemProp.smokiness
            nextDelay = (nextDelayBase * (1.0 + RNG.nextTriangularBal() * 0.1)).toFloat()

            changeFireboxItemCount(-1)
        }
        // no item on the slot
        else if (fuelCaloriesNow <= 0f && fireboxItem == null) {
            nextDelayBase = Float.POSITIVE_INFINITY
            nextDelay = Float.POSITIVE_INFINITY
        }

        // tick a thermal loss
        temperature -= 1f / 4096f
        temperature = temperature.coerceIn(0f, 1f)


        // emit smokes only when there is something burning
        if (spawnTimer >= nextDelay) {
            (Terrarum.ingame as TerrarumIngame).addParticle(
                ParticleVanishingSprite(
                    CommonResourcePool.getAsTextureRegionPack("particles-tiki_smoke.tga"),
                    25f, true, hitbox.startX + TILE_SIZED, hitbox.startY + 16, false, (Math.random() * 256).toInt()
                )
            )

            spawnTimer -= nextDelay
            nextDelay = (nextDelayBase * (1.0 + RNG.nextTriangularBal() * 0.1)).toFloat()

            (sprite as? SheetSpriteAnimation)?.delays?.set(0, Math.random().toFloat() * 0.4f + 0.1f)
        }

        // roast items
        if (oreItem != null &&
            fuelCaloriesNow > 0f &&
            oreItemProp?.smeltingProduct != null &&
            (productItem == null || oreItemProp.smeltingProduct == productItem!!.itm)
        ) {

            progress += temperature

            if (progress >= CALORIES_PER_ROASTING) {
                val smeltingProduct = oreItemProp.smeltingProduct!!
                if (productItem == null)
                    productItem = InventoryPair(smeltingProduct, 1L)
                else
                    changeProductItemCount(1)

                // take the ore item
                changeOreItemCount(-1)

                progress = 0f
            }
        }
        else if (oreItem == null) {
            progress = 0f
        }

        // update lightbox
        lightBoxList.forEach {
            it.light = light.cpy().mul(temperature)
        }

        if (temperature > 0.001f)
            spawnTimer += delta
        else
            spawnTimer = 0f
    }

}