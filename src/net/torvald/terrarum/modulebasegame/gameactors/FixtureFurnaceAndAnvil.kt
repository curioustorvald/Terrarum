package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.gdx.graphics.Cvec
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.audio.audiobank.MusicContainer
import net.torvald.terrarum.audio.dsp.Gain
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.gameparticles.ParticleVanishingSprite
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UICrafting
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-12-05.
 */
class FixtureFurnaceAndAnvil : FixtureBase, CraftingStation {

    @Transient override val tags = listOf("metalworking")

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 3, 2), // temporary value, will be overwritten by spawn()
        nameFun = { Lang["ITEM_FURNACE_AND_ANVIL"] },
        mainUI = UICrafting(null)
    ) {
        CommonResourcePool.addToLoadingList("particles-tiki_smoke.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "particles/bigger_smoke.tga"), 16, 16)
        }
        CommonResourcePool.loadAll()


        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/metalworking_furnace_and_anvil.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/metalworking_furnace_and_anvil_emsv.tga")

        density = BlockCodex[Block.STONE].density.toDouble()
        setHitboxDimension(itemImage.texture.width, itemImage.texture.height, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, itemImage.texture.width, itemImage.texture.height)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 100.0

        despawnHook = {
            stopAudio(static) {
                it.filters[filterIndex] = NullFilter
            }
        }
    }

    @Transient val static = MusicContainer("bonfire", ModMgr.getFile("basegame", "audio/effects/static/bonfire.ogg"), true)

    @Transient override var lightBoxList = arrayListOf(Lightbox(Hitbox(0.0, 0.0, TerrarumAppConfiguration.TILE_SIZED * 2, TerrarumAppConfiguration.TILE_SIZED * 2), Cvec(0.5f, 0.18f, 0f, 0f)))

    @Transient private val actorBlocks = arrayOf(
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

    @Transient private var audioStatus = 0

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        // emit smokes TODO: only when hot
        if (spawnTimer >= nextDelay) {
            (Terrarum.ingame as TerrarumIngame).addParticle(
                ParticleVanishingSprite(
                    CommonResourcePool.getAsTextureRegionPack("particles-tiki_smoke.tga"),
                    25f, true, hitbox.startX + TerrarumAppConfiguration.TILE_SIZED, hitbox.startY + 21, false, (Math.random() * 256).toInt()
                )
            )

            spawnTimer -= nextDelay
            nextDelay = Math.random().toFloat() * 0.25f + 0.25f

            (sprite as? SheetSpriteAnimation)?.delays?.set(0, Math.random().toFloat() * 0.4f + 0.1f)
        }

        spawnTimer += delta


        // update sound randomiser
        volRand.update(delta)


        // manage audio
        getTrackByAudio(static).let {
//            printdbg(this, "Checking out track ${it?.name}")
            if (it != null) {

                if (audioStatus == 0) {
                    startAudio(static) {
                        it.filters[filterIndex] = Gain(0f)
                        audioStatus = 1
                    }
                }
            }
            else {
//                printdbg(this, "Track is null! (old audio status=$audioStatus")
                audioStatus = 0
            }

            if (it != null) {
                if (it.processor.streamBuf != null || it.playRequested.get()) {
                    if (it.filters[filterIndex] !is Gain) // just in case...
                        it.filters[filterIndex] = Gain(0f)

                    (it.filters[filterIndex] as Gain).gain = 0.4f * volRand.get()
                }
                else {
                    audioStatus = 0
                }
            }
        }
    }

    @Transient private val filterIndex = 0
    @Transient private val volRand = ParamRandomiser(0.8f, 0.4f)

    override fun dispose() {
        super.dispose()
        static.dispose()
    }
}