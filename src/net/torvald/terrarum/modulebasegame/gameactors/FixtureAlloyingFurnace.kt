package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.audio.dsp.PreGain
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameparticles.ParticleVanishingSprite
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.ui.UIAlloyingFurnace
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2024-03-09.
 */
class FixtureAlloyingFurnace : FixtureBase {


    var fuelCaloriesNow = 0.0 // arbitrary number, may as well be watts or joules
    var fuelCaloriesMax: Double? = null
    var temperature = 0f // 0f..1f
    var progress = 0f // 0f..1f

    internal var oreItem1: InventoryPair? = null
    internal var oreItem2: InventoryPair? = null
    internal var fireboxItem: InventoryPair? = null
    internal var productItem: InventoryPair? = null

    @Transient val oreItem1Status = object : SmelterItemStatus {
        override fun set(itm: ItemID, qty: Long) {
            if (oreItem1 != null) oreItem1!!.set(itm, qty)
            else oreItem1 = InventoryPair(itm, qty)
        }
        override fun changeCount(delta: Long) {
            oreItem1!!.qty += delta
            if (oreItem1!!.qty <= 0L) {
                oreItem1 = null
            }
        }
        override fun nullify() {
            oreItem1 = null
        }
        override fun isNull(): Boolean {
            return oreItem1 == null
        }
        override val itm: ItemID?
            get() = oreItem1?.itm
        override val qty: Long?
            get() = oreItem1?.qty
    }
    @Transient val oreItem2Status = object : SmelterItemStatus {
        override fun set(itm: ItemID, qty: Long) {
            if (oreItem2 != null) oreItem2!!.set(itm, qty)
            else oreItem2 = InventoryPair(itm, qty)
        }
        override fun changeCount(delta: Long) {
            oreItem2!!.qty += delta
            if (oreItem2!!.qty <= 0L) {
                oreItem2 = null
            }
        }
        override fun nullify() {
            oreItem2 = null
        }
        override fun isNull(): Boolean {
            return oreItem2 == null
        }
        override val itm: ItemID?
            get() = oreItem2?.itm
        override val qty: Long?
            get() = oreItem2?.qty
    }
    @Transient val fireboxItemStatus = object : SmelterItemStatus {
        override fun set(itm: ItemID, qty: Long) {
            if (fireboxItem != null) fireboxItem!!.set(itm, qty)
            else fireboxItem = InventoryPair(itm, qty)
        }
        override fun changeCount(delta: Long) {
            fireboxItem!!.qty += delta
            if (fireboxItem!!.qty <= 0L) {
                fireboxItem = null
            }
        }
        override fun nullify() {
            fireboxItem = null
        }
        override fun isNull(): Boolean {
            return fireboxItem == null
        }
        override val itm: ItemID?
            get() = fireboxItem?.itm
        override val qty: Long?
            get() = fireboxItem?.qty
    }
    @Transient val productItemStatus = object : SmelterItemStatus {
        override fun set(itm: ItemID, qty: Long) {
            if (productItem != null) productItem!!.set(itm, qty)
            else productItem = InventoryPair(itm, qty)
        }
        override fun changeCount(delta: Long) {
            productItem!!.qty += delta
            if (productItem!!.qty <= 0L) {
                productItem = null
            }
        }
        override fun nullify() {
            productItem = null
        }
        override fun isNull(): Boolean {
            return productItem == null
        }
        override val itm: ItemID?
            get() = productItem?.itm
        override val qty: Long?
            get() = productItem?.qty
    }


    override val canBeDespawned: Boolean
        get() = oreItem1 == null && oreItem2 == null && fireboxItem == null && productItem == null


    init {
        CommonResourcePool.addToLoadingList("basegame/sprites/fixtures/alloying_furnace.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/alloying_furnace.tga"), 32, 32)
        }
        CommonResourcePool.addToLoadingList("basegame/sprites/fixtures/alloying_furnace_emsv.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/alloying_furnace_emsv.tga"), 32, 32)
        }
        CommonResourcePool.loadAll()
    }


    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2), // temporary value, will be overwritten by spawn()
        nameFun = { Lang["ITEM_ALLOYING_FURNACE"] },
    ) {
        CommonResourcePool.addToLoadingList("particles-tiki_smoke.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "particles/bigger_smoke.tga"), 16, 16)
        }
        CommonResourcePool.loadAll()



        density = BlockCodex[Block.STONE].density.toDouble()
        setHitboxDimension(32, 32, 0, 0)

        makeNewSprite(CommonResourcePool.getAsTextureRegionPack("basegame/sprites/fixtures/alloying_furnace.tga")).let {
            it.setRowsAndFrames(1,1)
        }
        makeNewSpriteEmissive(CommonResourcePool.getAsTextureRegionPack("basegame/sprites/fixtures/alloying_furnace_emsv.tga")).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 100.0

        this.mainUI = UIAlloyingFurnace(this)
    }


    @Transient val static = MusicContainer("bonfire", ModMgr.getFile("basegame", "audio/effects/static/bonfire.ogg"), true)
    @Transient val light = Cvec(0.5f, 0.18f, 0f, 0f)

    @Transient override var lightBoxList = arrayListOf(Lightbox(Hitbox(0.0, 0.0, TILE_SIZED * 2, TILE_SIZED * 2), light))

    override fun getBlockBoxPositions(posX: Int, posY: Int): List<Pair<Int, Int>> {
        return listOf(
            (posX+0 to posY+0),
            (posX+0 to posY+1), (posX+1 to posY+1),
        )
    }

    private var nextDelayBase = 0.25f // use smokiness value of the item
    private var nextDelay = 0.25f // use smokiness value of the item
    private var spawnTimer = 0f

    @Transient private val RNG = HQRNG()

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

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        val fuelItemProp = ItemCodex[fireboxItem?.itm]

        // consume fuel
        if (fuelCaloriesNow > 0f) {
            fuelCaloriesNow -= FixtureSmelterBasic.FUEL_CONSUMPTION

            // raise temperature
            temperature += 1f /2048f
        }
        // take fuel from the item slot
        else if (fuelCaloriesNow <= 0f && fuelItemProp?.calories != null && fireboxItem!!.qty > 0L) {
            fuelCaloriesNow = fuelItemProp.calories
            fuelCaloriesMax = fuelItemProp.calories
            nextDelayBase = fuelItemProp.smokiness
            nextDelay = (nextDelayBase * (1.0 + RNG.nextTriangularBal() * 0.1)).toFloat()

            fireboxItemStatus.changeCount(-1)
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
                    25f, true, hitbox.startX + TILE_SIZED / 2, hitbox.startY + 16, false, (Math.random() * 256).toInt()
                )
            )

            spawnTimer -= nextDelay
            nextDelay = (nextDelayBase * (1.0 + RNG.nextTriangularBal() * 0.1)).toFloat()

            (sprite as? SheetSpriteAnimation)?.delays?.set(0, Math.random().toFloat() * 0.4f + 0.1f)
        }

        val smeltingProduct = CraftingRecipeCodex.getSmeltingProductOf(oreItem1?.itm, oreItem2?.itm)

        // roast items
        if (oreItem1 != null &&
            oreItem2 != null &&
            temperature > 0f &&
            smeltingProduct != null &&
            (productItem == null || smeltingProduct.item == productItem!!.itm)
        ) {

            progress += temperature

            if (progress >= FixtureSmelterBasic.CALORIES_PER_ROASTING) {
                val moq = smeltingProduct.moq
                val smeltingProduct = smeltingProduct.item

                // check if the item even exists
                if (ItemCodex[smeltingProduct] == null) throw NullPointerException("No item prop for $smeltingProduct")

                if (productItem == null)
                    productItem = InventoryPair(smeltingProduct, moq)
                else
                    productItemStatus.changeCount(1)

                // take the ore item
                oreItem1Status.changeCount(-1)
                oreItem2Status.changeCount(-1)

                progress = 0f
            }
        }
        else if (oreItem1 == null || oreItem2 == null) {
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


        // update sound randomiser
        volRand.update(delta)


        // manage audio
        // FIXME this code is also killing the audio played by the other fixture (FixtureFurnaceAndAnvil)
        getTrackByAudio(static).let {
            if (it == null || (temperature > 0f && !it.isPlaying && !it.playRequested.get())) {
                startAudio(static) {
                    it.filters[filterIndex] = PreGain(0f)
                }
            }
            else if (it != null && it.isPlaying && temperature <= 0f) {
                stopAudio(static) {
                    it.filters[filterIndex] = NullFilter
                }
            }

            if (it != null) {
                if (it.filters[filterIndex] !is PreGain) // just in case...
                    it.filters[filterIndex] = PreGain(0f)

                (it.filters[filterIndex] as PreGain).gain = (it.maxVolume * temperature * volRand.get()).toFloat()
            }
        }

        inOperation = (temperature > 0.001f)
        chunkAnchoring = inOperation // update immediately instead of waiting for the next update
    }

    @Transient private val filterIndex = 0
    @Transient private val volRand = ParamRandomiser(0.8f, 0.4f)

    override fun dispose() {
        super.dispose()
        static.dispose()
    }
}