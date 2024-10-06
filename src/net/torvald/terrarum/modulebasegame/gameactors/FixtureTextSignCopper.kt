package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Item
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.ItemTextSignCopper
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.TIMES
import org.dyn4j.geometry.Vector2
import java.util.*

/**
 * Created by minjaesong on 2024-03-20.
 */
class FixtureTextSignCopper : Electric {

    @Transient override val spawnNeedsCeiling = true
    @Transient override val spawnNeedsFloor = false
    @Transient override val spawnNeedsWall = false



    var text = "헬로 월드!"
    var panelCount = 5

    private var initialised = false

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        renderOrder = RenderOrder.BEHIND,
        nameFun = { Lang["ITEM_COPPER_SIGN"] }
    ) {
        reload()
    }

    constructor(text: String, panelCount: Int) : super(
        BlockBox(BlockBox.NO_COLLISION, panelCount, 2),
        renderOrder = RenderOrder.BEHIND,
        nameFun = { Lang["ITEM_COPPER_SIGN"] }
    ) {
        this.text = text
        this.panelCount = panelCount
        reload()
    }

    fun _itemise(actor: ActorHumanoid): GameItem {
        return ItemTextSignCopper(Item.COPPER_SIGN).makeDynamic(actor.inventory).also {
            it.extra["signContent"] = text
            it.extra["signPanelCount"] = panelCount
            it.nameSecondary = "[$panelCount${TIMES}2] $text"
        }
    }

    override fun itemise(): ItemID {
        val item = _itemise(INGAME.actorNowPlaying!!)
        return item.dynamicID
    }

    override fun spawn(posX: Int, posY: Int, installersUUID: UUID?): Boolean = spawnUsingCustomBoxSize(posX, posY, installersUUID, panelCount.coerceAtLeast(2), 2)

    override fun reload() {
        super.reload()

        if (!initialised) {
            initialised = true

            blockBox = BlockBox(BlockBox.NO_COLLISION, panelCount, 2)
            setHitboxDimension(TILE_SIZE * blockBox.width, TILE_SIZE * blockBox.height, 0, 2)
            oldSinkStatus = Array(blockBox.width * blockBox.height) { Vector2() }
            newSinkStatus = Array(blockBox.width * blockBox.height) { Vector2() }
        }

        // must be re-spawned on reload to make it visible after load
        spawnUsingCustomBoxSize(
            intTilewiseHitbox.canonicalX.toInt(),
            intTilewiseHitbox.canonicalY.toInt(),
            actorThatInstalledThisFixture,
            panelCount.coerceAtLeast(2),
            2
        )
        setEmitterAndSink()
        setSprites(panelCount, text)
        updateSignal()
    }
    private fun setEmitterAndSink() {
        clearStatus()
        setWireSinkAt(0, 0, "digital_bit")
        setWireSinkAt(panelCount - 1, 0, "digital_bit")
    }

    init {
        CommonResourcePool.addToLoadingList("pixmap:copper_sign") {
            Pixmap(ModMgr.getGdxFile("basegame", "sprites/fixtures/text_sign_glass_copper.tga"))
        }
        CommonResourcePool.loadAll()
    }

    @Transient private var textLit: TextureRegion? = null
    @Transient private var textUnlit: TextureRegion? = null
    @Transient private var textLitEmsv: TextureRegion? = null
    @Transient private var textUnlitEmsv: TextureRegion? = null


    /**
     * This only constructs the base panel
     */
    private fun setSprites(panelCount: Int, text: String) {
        val panelCount = panelCount.coerceAtLeast(2)
        val pixmap = CommonResourcePool.getAs<Pixmap>("pixmap:copper_sign")

        val W = TILE_SIZE
        val H = 4 * TILE_SIZE
        val ROW0 = 0
        val ROW1 = H

        // construct the sprite

        val pixmapSprite = Pixmap(W * panelCount, H, Pixmap.Format.RGBA8888)
        val pixmapSpriteEmsv = Pixmap(W * panelCount, H, Pixmap.Format.RGBA8888)

        val pixmapOverlay = Pixmap(W * panelCount, H / 2, Pixmap.Format.RGBA8888)
        val pixmapOverlayUnlit = Pixmap(W * panelCount, H / 2, Pixmap.Format.RGBA8888)
        val pixmapOverlayEmsv = Pixmap(W * panelCount, H / 2, Pixmap.Format.RGBA8888)
        val pixmapOverlayUnlitEmsv = Pixmap(W * panelCount, H / 2, Pixmap.Format.RGBA8888)

        pixmapSprite.drawPixmap(pixmap, 0, 0, 0, ROW0, W, H)
        pixmapSprite.drawPixmap(pixmap, W * (panelCount - 1), 0, 32, ROW0, W, H)
        pixmapSpriteEmsv.drawPixmap(pixmap, 0, 0, 0, ROW1, W, H)
        pixmapSpriteEmsv.drawPixmap(pixmap, W * (panelCount - 1), 0, 32, ROW1, W, H)
        for (mid in 1 until panelCount - 1) {
            pixmapSprite.drawPixmap(pixmap, W * mid, 0, 16, ROW0, W, H)
            pixmapSpriteEmsv.drawPixmap(pixmap, W * mid, 0, 16, ROW1, W, H)
        }

        for (tiles in 0 until panelCount) {
            pixmapOverlay.drawPixmap(pixmap, W * tiles, 0, 48, 0, W, H)
            pixmapOverlayUnlit.drawPixmap(pixmap, W * tiles, 0, 48, 32, W, H)
            pixmapOverlayEmsv.drawPixmap(pixmap, W * tiles, 0, 48, 64, W, H)
            pixmapOverlayUnlitEmsv.drawPixmap(pixmap, W * tiles, 0, 48, 96, W, H)
        }

        // construct the text FBO
        val fboTextLit = FrameBuffer(Pixmap.Format.RGBA8888, W * panelCount, H / 2, false)
        val fboTextUnlit = FrameBuffer(Pixmap.Format.RGBA8888, W * panelCount, H / 2, false)
        val fboTextLitEmsv = FrameBuffer(Pixmap.Format.RGBA8888, W * panelCount, H / 2, false)
        val fboTextUnlitEmsv = FrameBuffer(Pixmap.Format.RGBA8888, W * panelCount, H / 2, false)

        val fbo = listOf(fboTextLit, fboTextUnlit, fboTextLitEmsv, fboTextUnlitEmsv)
        val overlays = listOf(pixmapOverlay, pixmapOverlayUnlit, pixmapOverlayEmsv, pixmapOverlayUnlitEmsv).map { Texture(it) }
        val batch = SpriteBatch()
        val camera = OrthographicCamera(fboTextLit.width.toFloat(), fboTextLit.height.toFloat())
        fbo.forEachIndexed { index, it ->
            it.inAction(camera, batch) {

                gdxClearAndEnableBlend(Color.CLEAR)

                batch.color = Color.WHITE
                batch.inUse { _ ->
                    blendNormalStraightAlpha(batch)
                    val tw = App.fontGame.getWidth(text)
                    App.fontGame.draw(batch, text, 1 + (it.width - tw) / 2, 3)


                    blendAlphaMask(batch)
                    batch.draw(overlays[index], 0f, 0f)
                }

            }
        }

        textLit = TextureRegion(fboTextLit.colorBufferTexture)
        textUnlit = TextureRegion(fboTextUnlit.colorBufferTexture)
        textLitEmsv = TextureRegion(fboTextLitEmsv.colorBufferTexture)
        textUnlitEmsv = TextureRegion(fboTextUnlitEmsv.colorBufferTexture)


        // make sprite out of the constructed pixmaps
        makeNewSprite(TextureRegionPack(Texture(pixmapSprite), W * panelCount, H / 2)).let {
            it.setRowsAndFrames(2, 1)
            it.delays = FloatArray(2) { Float.POSITIVE_INFINITY }
        }
        makeNewSpriteEmissive(TextureRegionPack(Texture(pixmapSpriteEmsv), W * panelCount, H / 2)).let {
            it.setRowsAndFrames(2, 1)
            it.delays = FloatArray(2) { Float.POSITIVE_INFINITY }
        }

        // clean up
        pixmapSprite.dispose()
        pixmapSpriteEmsv.dispose()
        pixmapOverlay.dispose()
        pixmapOverlayUnlit.dispose()
        pixmapOverlayEmsv.dispose()
        pixmapOverlayUnlitEmsv.dispose()
        overlays.forEach { it.dispose() }
        batch.dispose()
    }


    @Transient var lit = true

    override fun drawEmissive(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible && spriteEmissive != null) {
            blendNormalStraightAlpha(batch)
            drawSpriteInGoodPosition(frameDelta, spriteEmissive!!, batch)
            drawTextureInGoodPosition(frameDelta, if (lit) textLitEmsv!! else textUnlitEmsv!!, batch)
        }
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        if (isVisible && sprite != null) {
            drawSpriteInGoodPosition(frameDelta, sprite!!, batch)
            drawTextureInGoodPosition(frameDelta, if (lit) textLit!! else textUnlit!!, batch)
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

    override fun updateSignal() {
        lit = !(isSignalHigh(0, 0) || isSignalHigh(panelCount - 1, 0)) // isHigh and isLow are not mutually exclusive!

        (sprite as? SheetSpriteAnimation)?.currentRow = 1 - lit.toInt()
        (spriteEmissive as? SheetSpriteAnimation)?.currentRow = 1 - lit.toInt()
    }

    override fun dispose() {
        tooltipShowing.remove(tooltipHash)
        sprite?.dispose()
        spriteGlow?.dispose()
        spriteEmissive?.dispose()
        textLit?.texture?.dispose()
        textUnlit?.texture?.dispose()
        textLitEmsv?.texture?.dispose()
        textUnlitEmsv?.texture?.dispose()
    }
}