package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Json
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.savegame.EntryFile
import net.torvald.terrarum.serialise.ascii85toUUID
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2023-05-19.
 */
class UIWorldPortalListing(val full: UIWorldPortal) : UICanvas() {


    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val cellHeight = 48
    private val buttonHeight = 24
    private val gridGap = listGap

    private val worldList = ArrayList<WorldInfo>()
    private var selectedWorld: DiskSkimmer? = null


    private val cellCol = UIInventoryFull.CELL_COL
    private var highlightCol: Color = Color.WHITE




    private val thumbw = 360
    private val thumbh = 240
    private val hx = Toolkit.drawWidth.div(2)
    private val y = INVENTORY_CELLS_OFFSET_Y()

    private val listCount = UIInventoryFull.CELLS_VRT
    private val listHeight = cellHeight * listCount + gridGap * (listCount - 1)

    private val memoryGaugeWidth = 360
    private val deleteButtonWidth = (memoryGaugeWidth - gridGap) / 2
    private val buttonDeleteWorld = UIItemTextButton(this,
        "MENU_LABEL_DELETE_WORLD",
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

    data class WorldInfo(
        val uuid: UUID,
        val diskSkimmer: DiskSkimmer,
        val dimensionInChunks: Int
    )

    init {
        CommonResourcePool.addToLoadingList("terrarum-basegame-worldportalicons") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/worldportal_catbar.tga"), 20, 20)
        }
        CommonResourcePool.loadAll()


        addUIitem(buttonRenameWorld)
        addUIitem(buttonDeleteWorld)

    }

    private var chunksUsed = 0
    private val chunksMax = 100000

    override fun show() {
        worldList.clear()
        worldList.addAll((INGAME.actorGamer.actorValue.getAsString(AVKey.WORLD_PORTAL_DICT) ?: "").split(",").filter { it.isNotBlank() }.map {
            it.ascii85toUUID().let { it to App.savegameWorlds[it] }
        }.filter { it.second != null }.map { (uuid, disk) ->
            chunksUsed = worldList.sumOf {
                var w = 0
                var h = 0
                JsonFetcher.readFromJsonString(ByteArray64Reader((disk!!.requestFile(-1)!!.contents.getContent() as EntryFile).bytes, Charsets.UTF_8)).let {
                    JsonFetcher.forEachSiblings(it) { name, value ->
                        if (name == "width") w = value.asInt()
                        if (name == "height") h = value.asInt()
                    }
                }
                (w / CHUNK_W) * (h / CHUNK_H)
            }
            WorldInfo(uuid, disk!!, chunksUsed)
        } as List<WorldInfo>)

        chunksUsed = worldList.sumOf { it.dimensionInChunks }
    }

    override fun updateUI(delta: Float) {

    }


    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val memoryGaugeXpos = hx - memoryGaugeWidth - gridGap/2
        val memoryGaugeYpos = y + listHeight - buttonHeight - gridGap - buttonHeight
        val icons = CommonResourcePool.getAsTextureRegionPack("terrarum-basegame-worldportalicons")

        // draw background //
        // screencap panel
        batch.color = cellCol
        Toolkit.fillArea(batch, hx - thumbw - gridGap/2, y, thumbw, thumbh)


        // draw border //
        // screencap panel
        batch.color = highlightCol
        Toolkit.drawBoxBorder(batch, hx - thumbw - gridGap/2 - 1, y - 1, thumbw + 2, thumbh + 2)


        // memory gauge
        val barCol = UIItemInventoryCellCommonRes.getHealthMeterColour(chunksMax - chunksUsed, 0, chunksMax)
        val barBack = barCol mul UIItemInventoryCellCommonRes.meterBackDarkening
        batch.color = Color.WHITE
        batch.draw(icons.get(2, 2), memoryGaugeXpos - 32f, memoryGaugeYpos + 2f)
        Toolkit.drawBoxBorder(batch, memoryGaugeXpos - 1, memoryGaugeYpos - 1, memoryGaugeWidth + 2, buttonHeight + 2)
        batch.color = barBack
        Toolkit.fillArea(batch, memoryGaugeXpos, memoryGaugeYpos, memoryGaugeWidth, buttonHeight)
        batch.color = barCol
        Toolkit.fillArea(batch, memoryGaugeXpos, memoryGaugeYpos, (memoryGaugeWidth * (chunksUsed / chunksMax.toFloat())).ceilInt(), buttonHeight)



        uiItems.forEach { it.render(batch, camera) }

    }

    override fun dispose() {

    }
}


class UIItemWorldCellsSimple(
    parent: UILoadDemoSavefiles,
    initialX: Int,
    initialY: Int,
    val skimmer: DiskSkimmer
) : UIItem(parent, initialX, initialY) {

    override val width: Int = 360
    override val height: Int = 46

    private val cellCol = UIInventoryFull.CELL_COL
    private var highlightCol: Color = Color.WHITE

    override fun dispose() {
    }
}