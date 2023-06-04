package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.getCellCountVertically
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.ascii85toUUID
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.EMDASH
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream

/**
 * Created by minjaesong on 2023-05-19.
 */
class UIWorldPortalListing(val full: UIWorldPortal) : UICanvas() {


    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val buttonHeight = 24
    private val gridGap = 10



    private val thumbw = 378
    private val textAreaW = thumbw - 32
    private val thumbh = 252
    private val hx = Toolkit.drawWidth.div(2)
    private val y = INVENTORY_CELLS_OFFSET_Y() + 1

    private val listCount = getCellCountVertically(UIItemWorldCellsSimple.height, gridGap)
    private val listHeight = UIItemWorldCellsSimple.height + (listCount - 1) * (UIItemWorldCellsSimple.height + gridGap)

    private val memoryGaugeWidth = textAreaW
    private val deleteButtonWidth = (thumbw - gridGap) / 2
    private val buttonDeleteWorld = UIItemTextButton(this,
        "MENU_LABEL_DELETE",
        hx - gridGap/2 - deleteButtonWidth,
        y + listHeight - buttonHeight,
        deleteButtonWidth,
        readFromLang = true,
        hasBorder = true,
        alignment = UIItemTextButton.Companion.Alignment.CENTRE
    )
    private val buttonRenameWorld = UIItemTextButton(this,
        "MENU_LABEL_RENAME",
        buttonDeleteWorld.posX - gridGap - deleteButtonWidth,
        y + listHeight - buttonHeight,
        deleteButtonWidth,
        readFromLang = true,
        hasBorder = true,
        alignment = UIItemTextButton.Companion.Alignment.CENTRE
    )

    private val worldList = ArrayList<WorldInfo>()
    data class WorldInfo(
        val uuid: UUID,
        val diskSkimmer: DiskSkimmer,
        val dimensionInChunks: Int,
        val seed: Long,
        val lastPlayedString: String,
        val totalPlayedString: String,
        val screenshot: TextureRegion?,
    ) {
        fun dispose() {
            screenshot?.texture?.dispose()
        }
    }

    init {
        CommonResourcePool.addToLoadingList("terrarum-basegame-worldportalicons") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/worldportal_catbar.tga"), 30, 20)
        }
        CommonResourcePool.loadAll()


        addUIitem(buttonRenameWorld)
        addUIitem(buttonDeleteWorld)

    }

    private var chunksUsed = 0
    private val chunksMax = 100000

    private lateinit var worldCells: Array<UIItemWorldCellsSimple>

    private var selected: UIItemWorldCellsSimple? = null
    private var selectedIndex: Int? = null

    override fun show() {
        worldList.clear()
        (INGAME.actorGamer.actorValue.getAsString(AVKey.WORLD_PORTAL_DICT) ?: "").split(",").filter { it.isNotBlank() }.map {
            it.ascii85toUUID().let { it to App.savegameWorlds[it] }
        }.filter { it.second != null }.mapIndexed { index, (uuid, disk) ->

            var chunksCount = 0
            var seed = 0L
            var lastPlayed = 0L
            var totalPlayed = 0L
            var w = 0
            var h = 0
            var thumb: TextureRegion? = null

            JsonFetcher.readFromJsonString(ByteArray64Reader(disk!!.requestFile(-1)!!.contents.getContent() as ByteArray64, Common.CHARSET)).let {
                JsonFetcher.forEachSiblings(it) { name, value ->
                    if (name == "width") w = value.asInt()
                    if (name == "height") h = value.asInt()
                    if (name == "generatorSeed") seed = value.asLong()
                    if (name == "lastPlayTime") lastPlayed = value.asLong()
                    if (name == "totalPlayTime") totalPlayed = value.asLong()
                }
            }
            chunksCount = (w / CHUNK_W) * (h / CHUNK_H)

            disk.requestFile(-2)?.let {
                val zippedTga = (it.contents as EntryFile).bytes
                val gzin = GZIPInputStream(ByteArray64InputStream(zippedTga))
                val tgaFileContents = gzin.readAllBytes(); gzin.close()

                val thumbPixmap = Pixmap(tgaFileContents, 0, tgaFileContents.size)
                val thumbTex = Texture(thumbPixmap)
                thumbTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                thumb = TextureRegion(thumbTex)
            }

            WorldInfo(uuid, disk, chunksCount, seed, lastPlayed.toTimestamp(), totalPlayed.toDurationStamp(), thumb)
        }.let {
            worldList.addAll(it)
        }



        chunksUsed = worldList.sumOf { it.dimensionInChunks }

        worldCells = Array(maxOf(worldList.size, listCount)) {
            UIItemWorldCellsSimple(
                this,
                hx + gridGap / 2,
                y + (gridGap + UIItemWorldCellsSimple.height) * it,
                worldList.getOrNull(it),
                worldList.getOrNull(it)?.diskSkimmer?.getDiskName(Common.CHARSET)
            ).also { button ->
                button.clickOnceListener = { _, _, _ ->
                    selected = button
                    selectedIndex = it
                    updateUIbyButtonSelection()
                }
            }
        }

        uiItems.forEach { it.show() }
        worldCells.forEach { it.show() }
        selected = null

        updateUIbyButtonSelection()
    }

    private fun Long.toTimestamp() = Instant.ofEpochSecond(this)
        .atZone(TimeZone.getDefault().toZoneId())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    private fun Long.toDurationStamp(): String {
        val s = this % 60
        val m = (this / 60) % 60
        val h = (this / 3600) % 24
        val d = this / 86400
        return if (d == 0L)
            "${h.toString().padStart(2,'0')}h${m.toString().padStart(2,'0')}m${s.toString().padStart(2,'0')}s"
        else
            "${d}d${h.toString().padStart(2,'0')}h${m.toString().padStart(2,'0')}m${s.toString().padStart(2,'0')}s"
    }
    private val nullTimestamp = "0000-00-00 --:--:--"
    private val nullDurationStamp = "--h--m--s"
    private fun Int.chunkCountToWorldSize() = when(this) {
        in 0 until 2000 -> "CONTEXT_DESCRIPTION_TINY"
        in 2000 until 4500 -> "CONTEXT_DESCRIPTION_SMALL"
        in 4500 until 10000 -> "CONTEXT_DESCRIPTION_BIG"
        else -> "CONTEXT_DESCRIPTION_HUGE"
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
        worldCells.forEach { it.update(delta) }
    }

    private val iconGap = 12f
    private val iconSize = 30f
    private val textualListHeight = 30f
    private val iconSizeGap = iconSize + iconGap

    private fun updateUIbyButtonSelection() {
        worldTexts = listOf(
            // last played
            icons.get(2, 1) to (selected?.worldInfo?.lastPlayedString ?: nullTimestamp),
            // total played
            icons.get(0, 2) to (selected?.worldInfo?.totalPlayedString ?: nullDurationStamp),
            // world size
            icons.get(1, 2) to (Lang.getOrNull(selected?.worldInfo?.dimensionInChunks?.chunkCountToWorldSize()) ?: "$EMDASH"),
        )

        selectedWorldThumb = selected?.worldInfo?.screenshot

        worldCells.forEach {
            it.highlighted = (selected == it && selected?.worldInfo != null)
        }
    }

    private lateinit var worldTexts: List<Pair<TextureRegion, String>>
    private var selectedWorldThumb: TextureRegion? = null

    val icons = CommonResourcePool.getAsTextureRegionPack("terrarum-basegame-worldportalicons")
    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val memoryGaugeXpos = hx - memoryGaugeWidth - gridGap/2
        val memoryGaugeYpos = y + listHeight - buttonHeight - gridGap - buttonHeight
        val textXpos = memoryGaugeXpos + 3

        // draw background //
        // screencap panel
        batch.color = UIInventoryFull.CELL_COL
        Toolkit.fillArea(batch, hx - thumbw - gridGap/2, y, thumbw, thumbh)


        // draw border //
        // screencap panel
        batch.color = if (selected?.worldInfo == null) Toolkit.Theme.COL_INVENTORY_CELL_BORDER else Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, hx - thumbw - gridGap/2 - 1, y - 1, thumbw + 2, thumbh + 2)


        // memory gauge
        val barCol = UIItemInventoryCellCommonRes.getHealthMeterColour(chunksMax - chunksUsed, 0, chunksMax)
        val barBack = barCol mul UIItemInventoryCellCommonRes.meterBackDarkening

        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, (memoryGaugeXpos - iconSizeGap + 10).toInt(), memoryGaugeYpos, buttonHeight + 6, buttonHeight)
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, (memoryGaugeXpos - iconSizeGap + 10).toInt() - 1, memoryGaugeYpos - 1, buttonHeight + 7, buttonHeight + 2)
        batch.color = Color.WHITE
        batch.draw(icons.get(2, 2), textXpos - iconSizeGap, memoryGaugeYpos + 2f)

        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, memoryGaugeXpos - 1, memoryGaugeYpos - 1, memoryGaugeWidth + 2, buttonHeight + 2)
        batch.color = barBack
        Toolkit.fillArea(batch, memoryGaugeXpos, memoryGaugeYpos, memoryGaugeWidth, buttonHeight)
        batch.color = barCol
        Toolkit.fillArea(batch, memoryGaugeXpos, memoryGaugeYpos, (memoryGaugeWidth * (chunksUsed / chunksMax.toFloat())).ceilInt(), buttonHeight)

        batch.color = Color.WHITE
        if (selected?.worldInfo != null) {
            // background for texts panel
            batch.color = Toolkit.Theme.COL_CELL_FILL
            Toolkit.fillArea(batch, hx - thumbw - gridGap/2, y + thumbh + 3, thumbw, 10 + worldTexts.size * textualListHeight.toInt())
            batch.color = Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, hx - thumbw - gridGap/2 - 1, y + thumbh + 2, thumbw + 2, 10 + worldTexts.size * textualListHeight.toInt() + 2)

            // some texts
            batch.color = Color.WHITE
            worldTexts.forEachIndexed { index, (icon, str) ->
                batch.draw(icon, textXpos - iconSizeGap + 6, y + thumbh + 3+10 + textualListHeight * index)
                App.fontGame.draw(batch, str, textXpos + 6f, y + thumbh + 3+10 + textualListHeight * index)
            }
            // size indicator on the memory gauge
            Toolkit.fillArea(batch, memoryGaugeXpos, memoryGaugeYpos, (memoryGaugeWidth * (selected?.worldInfo!!.dimensionInChunks / chunksMax.toFloat())).ceilInt(), buttonHeight)

            // thumbnail
            selected?.worldInfo?.screenshot?.let {
                batch.draw(it, (hx - thumbw - gridGap/2).toFloat(), y.toFloat(), thumbw.toFloat(), thumbh.toFloat())
            }
        }




        uiItems.forEach { it.render(batch, camera) }
        worldCells.forEach { it.render(batch, camera) }

        // control hints
        batch.color = Color.WHITE
        App.fontGame.draw(batch, full.portalListingControlHelp, hx - thumbw - gridGap/2 + 2, (full.yEnd - 20).toInt())
    }

    override fun hide() {
        uiItems.forEach { it.hide() }
        worldCells.forEach { it.hide() }

        worldCells.forEach { try { it.dispose() } catch (_: GdxRuntimeException) {} }
        worldList.forEach { try { it.dispose() } catch (_: GdxRuntimeException) {} }
    }

    override fun dispose() {
        uiItems.forEach { it.dispose() }
        worldCells.forEach { try { it.dispose() } catch (_: GdxRuntimeException) {} }
        worldList.forEach { try { it.dispose() } catch (_: GdxRuntimeException) {} }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (this.isVisible && mouseInScreen(screenX, screenY)) {
            uiItems.forEach { it.touchDown(screenX, screenY, pointer, button) }
            worldCells.forEach { it.touchDown(screenX, screenY, pointer, button) }
            handler.subUIs.forEach { it.touchDown(screenX, screenY, pointer, button) }
            return true
        }
        else return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (this.isVisible) {
            uiItems.forEach { it.touchUp(screenX, screenY, pointer, button) }
            worldCells.forEach { it.touchUp(screenX, screenY, pointer, button) }
            handler.subUIs.forEach { it.touchUp(screenX, screenY, pointer, button) }
            return true
        }
        else return false
    }
}


class UIItemWorldCellsSimple(
    parent: UIWorldPortalListing,
    initialX: Int,
    initialY: Int,
    internal val worldInfo: UIWorldPortalListing.WorldInfo? = null,
    internal val worldName: String? = null,
) : UIItem(parent, initialX, initialY) {

    companion object {
        const val width = 378
        const val height = 46
    }

    override val width: Int = 378
    override val height: Int = 46

    private val icons = CommonResourcePool.getAsTextureRegionPack("terrarum-basegame-worldportalicons")

    var highlighted = false

    override fun show() {
        super.show()
    }

    override fun hide() {
        super.hide()
    }

    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)


        // draw background
        batch.color = UIInventoryFull.CELL_COL
        Toolkit.fillArea(batch, posX, posY, width, height)

        val mouseUp = mouseUp && worldInfo != null

        val bcol = if (highlighted || mouseUp && mousePushed) Toolkit.Theme.COL_SELECTED
        else if (mouseUp) Toolkit.Theme.COL_MOUSE_UP else (if (worldInfo == null) Toolkit.Theme.COL_INVENTORY_CELL_BORDER else Toolkit.Theme.COL_INACTIVE)
        val tcol = if (highlighted || mouseUp && mousePushed) Toolkit.Theme.COL_SELECTED
        else if (mouseUp) Toolkit.Theme.COL_MOUSE_UP else (if (worldInfo == null) Toolkit.Theme.COL_INACTIVE else Toolkit.Theme.COL_LIST_DEFAULT)

        // draw border
        batch.color = bcol
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        // draw texts
        batch.color = tcol
        batch.draw(icons.get(0, 1), posX + 4f, posY + 1f)
        App.fontGame.draw(batch, worldName ?: "$EMDASH", posX + 32, posY + 1)
        batch.draw(icons.get(1, 1), posX + 4f, posY + 25f)
        App.fontGame.draw(batch, if (worldInfo?.seed == null) "$EMDASH" else "${(if (worldInfo.seed > 0) "+" else "")}${worldInfo.seed}" , posX + 32, posY + 25)
        // text separator
        batch.color = bcol.cpy().sub(0f,0f,0f,0.65f)
        Toolkit.fillArea(batch, posX + 2, posY + 23, width - 4, 1)

    }

    override fun dispose() {
    }
}