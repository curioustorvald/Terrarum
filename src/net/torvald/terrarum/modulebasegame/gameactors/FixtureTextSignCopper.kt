package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
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

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        renderOrder = RenderOrder.BEHIND,
        nameFun = { Lang["ITEM_COPPER_SIGN"] }
    )

    override fun spawn(posX: Int, posY: Int, installersUUID: UUID?): Boolean = spawn(posX, posY, installersUUID, panelCount.coerceAtLeast(2), 2)

    override fun reload() {
        super.reload()

        // must be re-spawned on reload to make it visible after load
        spawn(
            intTilewiseHitbox.canonicalX.toInt(),
            intTilewiseHitbox.canonicalY.toInt(),
            actorThatInstalledThisFixture,
            panelCount.coerceAtLeast(2),
            2
        )
        setEmitterAndSink()
        setSprites(panelCount)
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

    @Transient private var textOverlay: SheetSpriteAnimation? = null
    @Transient private var textOverlayEmissive: SheetSpriteAnimation? = null

    /**
     * This only constructs the base panel
     */
    private fun setSprites(panelCount: Int) {
        val panelCount = panelCount.coerceAtLeast(2)
        val pixmap = CommonResourcePool.getAs<Pixmap>("pixmap:copper_sign")

        val W = TILE_SIZE
        val H = 4 * TILE_SIZE
        val ROW0 = 0
        val ROW1 = H

        val pixmapSprite = Pixmap(W * panelCount, H, Pixmap.Format.RGBA8888)
        val pixmapSpriteEmsv = Pixmap(W * panelCount, H, Pixmap.Format.RGBA8888)
        val pixmapOverlay = Pixmap(W * panelCount, H, Pixmap.Format.RGBA8888)
        val pixmapOverlayEmsv = Pixmap(W * panelCount, H, Pixmap.Format.RGBA8888)

        pixmapSprite.drawPixmap(pixmap, 0, 0, 0, ROW0, W, H)
        pixmapSprite.drawPixmap(pixmap, W * (panelCount - 1), 0, 32, ROW0, W, H)
        pixmapSpriteEmsv.drawPixmap(pixmap, 0, 0, 0, ROW1, W, H)
        pixmapSpriteEmsv.drawPixmap(pixmap, W * (panelCount - 1), 0, 32, ROW1, W, H)
        for (mid in 1 until panelCount - 1) {
            pixmapSprite.drawPixmap(pixmap, W * mid, 0, 16, ROW0, W, H)
            pixmapSpriteEmsv.drawPixmap(pixmap, W * mid, 0, 16, ROW1, W, H)
        }

        for (tiles in 0 until panelCount) {
            pixmapOverlay.drawPixmap(pixmap, W * tiles, 0, 48, ROW0, W, H)
            pixmapOverlayEmsv.drawPixmap(pixmap, W * tiles, 0, 48, ROW1, W, H)
        }

        makeNewSprite(TextureRegionPack(Texture(pixmapSprite), W * panelCount, H / 2)).let {
            it.setRowsAndFrames(2, 1)
            it.delays = FloatArray(1) { Float.POSITIVE_INFINITY }
        }
        makeNewSpriteEmissive(TextureRegionPack(Texture(pixmapSpriteEmsv), W * panelCount, H / 2)).let {
            it.setRowsAndFrames(2, 1)
            it.delays = FloatArray(1) { Float.POSITIVE_INFINITY }
        }

        textOverlay = SheetSpriteAnimation(this).also {
            it.setSpriteImage(TextureRegionPack(Texture(pixmapOverlay), W, H / 2))
            it.setRowsAndFrames(2, 1)
        }
        textOverlayEmissive = SheetSpriteAnimation(this).also {
            it.setSpriteImage(TextureRegionPack(Texture(pixmapOverlayEmsv), W, H / 2))
            it.setRowsAndFrames(2, 1)
        }


        pixmapSprite.dispose()
        pixmapSpriteEmsv.dispose()
        pixmapOverlay.dispose()
        pixmapOverlayEmsv.dispose()
    }


    @Transient var lit = true

    override fun drawEmissive(frameDelta: Float, batch: SpriteBatch) {
        super.drawEmissive(frameDelta, batch)
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        super.drawBody(frameDelta, batch)
    }

    override fun updateSignal() {
        lit = !(isSignalHigh(0, 0) || isSignalHigh(panelCount - 1, 0)) // isHigh and isLow are not mutually exclusive!

        (sprite as? SheetSpriteAnimation)?.currentRow = 1 - lit.toInt()
        (spriteEmissive as? SheetSpriteAnimation)?.currentRow = 1 - lit.toInt()
        textOverlay?.currentRow = 1 - lit.toInt()
        textOverlayEmissive?.currentRow = 1 - lit.toInt()
    }

    override fun dispose() {
        super.dispose()
        if (textOverlay != null) App.disposables.add(textOverlay)
        if (textOverlayEmissive != null) App.disposables.add(textOverlayEmissive)
    }
}