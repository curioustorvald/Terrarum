package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.random.HQRNG
import net.torvald.random.XXHash64
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureWorldPortal
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.utils.RandomWordsName
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2023-05-19.
 */
class UIWorldPortalSearch(val full: UIWorldPortal) : UICanvas() {

//    override var width: Int = Toolkit.drawWidth
//    override var height: Int = App.scr.height



    private val hugeTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/huge.png")))
    private val largeTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/large.png")))
    private val normalTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/normal.png")))
    private val smallTex = TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/small.png")))

    private val tex = arrayOf(smallTex, normalTex, largeTex, hugeTex)

    override var width = 480
    override var height = 480

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    private val rng = HQRNG()

    private val inputWidth = 350
    private val inputLineY1 = 90
    private val inputLineY2 = 130
    private val inputLineY3 = 170

    private val sizeSelY = 186 + 40

    private val nameInput = UIItemTextLineInput(this,
        drawX + width - inputWidth + 5, drawY + sizeSelY + inputLineY1, inputWidth,
        { RandomWordsName(4) }, InputLenCap(VirtualDisk.NAME_LENGTH, InputLenCap.CharLenUnit.UTF8_BYTES)
    )

    private val seedInput = UIItemTextLineInput(this,
        drawX + width - inputWidth + 5, drawY + sizeSelY + inputLineY2, inputWidth,
        { rng.nextLong().toString() }, InputLenCap(256, InputLenCap.CharLenUnit.CODEPOINTS)
    )

    private val goButtonWidth = 180
    private val buttonY = drawY + height - 24


    private val radioCellWidth = 120
    private val radioX = (width - (radioCellWidth * tex.size + 9)) / 2
    private var selectedSizeChunks = 0

    private val gridGap = 10
    private val buttonBaseX = (Toolkit.drawWidth - 3 * goButtonWidth - 2 * gridGap) / 2


    private val backButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, buttonBaseX, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            full.requestTransition(0)
        }
    }
    private val useInvitationButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_USE_CODE"] }, buttonBaseX + goButtonWidth + gridGap, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            full.queueUpUseInvitationScr()
            full.requestTransition(2)
        }
    }
    private val goButton: UIItemTextButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CONFIRM_BUTTON"] }, buttonBaseX + (goButtonWidth + gridGap) * 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            val seed = try {
                seedInput.getTextOrPlaceholder().toLong()
            }
            catch (e: NumberFormatException) {
                XXHash64.hash(seedInput.getTextOrPlaceholder().toByteArray(Charsets.UTF_8), 10000)
            }
            val (wx, wy) = TerrarumIngame.WORLDPORTAL_NEW_WORLD_SIZE[sizeSelector.selection]
            val worldParam = TerrarumIngame.NewWorldParameters(wx, wy, seed, nameInput.getTextOrPlaceholder())
            full.host.teleportRequest = FixtureWorldPortal.TeleportRequest(null, worldParam)
            full.setAsClose()
        }
    }

    private val sizeSelector = UIItemInlineRadioButtons(this,
        drawX + radioX, drawY + sizeSelY, radioCellWidth,
        listOf(
            { Lang["CONTEXT_DESCRIPTION_TINY"] },
            { Lang["CONTEXT_DESCRIPTION_SMALL"] },
            { Lang["CONTEXT_DESCRIPTION_BIG"] },
            { Lang["CONTEXT_DESCRIPTION_HUGE"] }
        )
    ).also {
        it.selectionChangeListener = { sel ->
            val (wx, wy) = TerrarumIngame.WORLDPORTAL_NEW_WORLD_SIZE[sel]
            selectedSizeChunks = (wx / LandUtil.CHUNK_W) * (wy / LandUtil.CHUNK_H)
            goButton.isEnabled = (full.chunksUsed + selectedSizeChunks) <= full.chunksMax
        }
    }

    init {
        val (wx, wy) = TerrarumIngame.WORLDPORTAL_NEW_WORLD_SIZE[sizeSelector.selection]
        selectedSizeChunks = (wx / LandUtil.CHUNK_W) * (wy / LandUtil.CHUNK_H)
        goButton.isEnabled = (full.chunksUsed + selectedSizeChunks) <= full.chunksMax

        addUIitem(sizeSelector)
        addUIitem(goButton)
        addUIitem(useInvitationButton)
        addUIitem(backButton)
        addUIitem(seedInput)  // order is important
        addUIitem(nameInput) // because of the IME candidates overlay
    }


    override fun show() {
        uiItems.forEach { it.show() }
        seedInput.clearText()
        seedInput.refreshPlaceholder()
        nameInput.clearText()
        nameInput.refreshPlaceholder()
    }

    private var oldPosX = full.posX

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }


    private val memoryGaugeWidth = radioCellWidth * 4 + 3 * 3 - 32
    private val hx = Toolkit.drawWidth.div(2)
    private val iconGap = 12f
    private val iconSize = 30f
    private val iconSizeGap = iconSize + iconGap
    private val buttonHeight = 24
    val icons = CommonResourcePool.getAsTextureRegionPack("terrarum-basegame-worldportalicons")


    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        val memoryGaugeXpos = hx - memoryGaugeWidth/2
        val memoryGaugeYpos = drawY + sizeSelY + buttonHeight + 10
        val textXpos = memoryGaugeXpos + 3
        val posXDelta = posX - oldPosX

        // ugh why won't you just scroll along??
//        seedInput.posX += posXDelta
//        nameInput.posX += posXDelta // is it fixed now?

        // TODO teleporter memory usage and whatnot

        batch.color = Color.WHITE
        // ui title
        val titlestr = Lang["CONTEXT_WORLD_NEW"]
        App.fontUITitle.draw(batch, titlestr, drawX + (width - App.fontUITitle.getWidth(titlestr)).div(2).toFloat(), INVENTORY_CELLS_OFFSET_Y() - 36f)

        // draw size previews
        val texture = tex[sizeSelector.selection.coerceAtMost(tex.lastIndex)]
        val tx = drawX + (width - texture.regionWidth) / 2
        val ty = drawY + (160 - texture.regionHeight) / 2
        batch.draw(texture, tx.toFloat(), ty.toFloat())
        // border
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, tx - 1, ty - 1, texture.regionWidth + 2, texture.regionHeight + 2)

        batch.color = Color.WHITE
        // size selector title
        val sizestr = Lang["MENU_OPTIONS_SIZE"]
        App.fontGame.draw(batch, sizestr, drawX + (width - App.fontGame.getWidth(sizestr)).div(2).toFloat(), drawY + sizeSelY - 40f)

        // name/seed input labels
        App.fontGame.draw(batch, Lang["MENU_NAME"], drawX - 4, drawY + sizeSelY + inputLineY1)
        App.fontGame.draw(batch, Lang["CONTEXT_PLACE_COORDINATE"], drawX - 4, drawY + sizeSelY + inputLineY2)

        val (wx, wy) = TerrarumIngame.NEW_WORLD_SIZE[sizeSelector.selection]
        val etaSec = Worldgen.getEstimationSec(wx, wy)
        val etaMin = etaSec.div(60f).roundToInt().coerceAtLeast(1)
        val etaText = Lang.getAndUseTemplate("CONTEXT_ESTIMATED_MINUTES_PLURAL", true, etaMin)
        val etaTextPrint = etaText + (if (App.IS_DEVELOPMENT_BUILD) " ($etaSec s)" else "")

        Toolkit.drawTextCentered(batch, App.fontGame, etaTextPrint, width, drawX, drawY + sizeSelY + inputLineY3)

        // memory gauge
        val chunksUsed = full.chunksUsed
        val barCol = UIItemInventoryCellCommonRes.getHealthMeterColour(full.chunksMax - chunksUsed, 0, full.chunksMax)
        val barBack = barCol mul UIItemInventoryCellCommonRes.meterBackDarkening
        val gaugeUsedWidth = (memoryGaugeWidth * (chunksUsed / full.chunksMax.toFloat())).ceilToInt()
        val gaugeEstimatedFullWidth = (memoryGaugeWidth * ((chunksUsed + selectedSizeChunks) / full.chunksMax.toFloat())).ceilToInt()
        val gaugeExtraWidth = (gaugeEstimatedFullWidth - gaugeUsedWidth)//.coerceIn(0 .. (memoryGaugeWidth - gaugeUsedWidth)) // deliberately NOT coercing

        // memory icon
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, 16 + (memoryGaugeXpos - iconSizeGap + 10).toInt(), memoryGaugeYpos, buttonHeight + 5, buttonHeight)
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, 16 + (memoryGaugeXpos - iconSizeGap + 10).toInt() - 1, memoryGaugeYpos - 1, buttonHeight + 7, buttonHeight + 2)
        batch.color = Color.WHITE
        batch.draw(icons.get(2, 2), 16 + textXpos - iconSizeGap, memoryGaugeYpos + 2f)
        // the gauge
        batch.color = barBack
        Toolkit.fillArea(batch, 16 + memoryGaugeXpos, memoryGaugeYpos, memoryGaugeWidth, buttonHeight)
        batch.color = barCol
        Toolkit.fillArea(batch, 16 + memoryGaugeXpos, memoryGaugeYpos, gaugeUsedWidth, buttonHeight)
        // extra gauge to show estimated memory usage
        batch.color = Color.WHITE
        Toolkit.fillArea(batch, 16 + memoryGaugeXpos + gaugeUsedWidth, memoryGaugeYpos, gaugeExtraWidth, buttonHeight)
        // gauge border
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, 16 + memoryGaugeXpos - 1, memoryGaugeYpos - 1, memoryGaugeWidth + 2, buttonHeight + 2)

        // control hints
        batch.color = Color.WHITE
        App.fontGame.draw(batch, full.portalListingControlHelp, 2 + (Toolkit.drawWidth - 560)/2 + 2, (UIInventoryFull.yEnd - 20).toInt())

        uiItems.forEach { it.render(frameDelta, batch, camera) }

        oldPosX = posX
    }

    override fun dispose() {
        hugeTex.texture.dispose()
        largeTex.texture.dispose()
        normalTex.texture.dispose()
        smallTex.texture.dispose()
    }

}