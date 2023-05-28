package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull.Companion.INVENTORY_CELLS_OFFSET_Y
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryItemGrid.Companion.listGap
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarum.serialise.ascii85toUUID
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButton
import java.util.*

/**
 * Created by minjaesong on 2023-05-19.
 */
class UIWorldPortalListing(val full: UIWorldPortal) : UICanvas() {


    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val cellHeight = 48
    private val buttonHeight = 24
    private val gridGap = listGap

    private val worldList = ArrayList<Pair<UUID, DiskSkimmer>>()
    private var selectedWorld: DiskSkimmer? = null


    private val cellCol = UIInventoryFull.CELL_COL
    private var highlightCol: Color = Color.WHITE




    private val thumbw = 360
    private val thumbh = 240
    private val hx = Toolkit.drawWidth.div(2)
    private val y = INVENTORY_CELLS_OFFSET_Y()

    private val listCount = UIInventoryFull.CELLS_VRT
    private val listHeight = cellHeight * listCount + gridGap * (listCount - 1)

    private val deleteButtonWidth = 80
    private val buttonReset = UIItemTextButton(this,
        "MENU_LABEL_DELETE_WORLD",
        hx - gridGap/2 - deleteButtonWidth,
        y + listHeight - buttonHeight,
        deleteButtonWidth,
        readFromLang = true,
        hasBorder = true,
        alignment = UIItemTextButton.Companion.Alignment.CENTRE
    )

    init {
        CommonResourcePool.addToLoadingList("terrarum-basegame-worldportalicons") {
            TextureRegion(Texture(ModMgr.getGdxFile("basegame", "gui/worldportal_catbar.tga")), 20, 20).also {
                it.flip(false, false)
            }
        }
        CommonResourcePool.loadAll()


        addUIitem(buttonReset)

    }

    override fun show() {
        worldList.clear()
        worldList.addAll((INGAME.actorGamer.actorValue.getAsString(AVKey.WORLD_PORTAL_DICT) ?: "").split(",").filter { it.isNotBlank() }.map {
            it.ascii85toUUID().let { it to App.savegameWorlds[it] }
        }.filter { it.second != null } as List<Pair<UUID, DiskSkimmer>>)

    }

    override fun updateUI(delta: Float) {

    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {


        // draw background //
        // screencap panel
        batch.color = cellCol
        Toolkit.fillArea(batch, hx - thumbw - gridGap/2, y, thumbw, thumbh)


        // draw border //
        // screencap panel
        batch.color = highlightCol
        Toolkit.drawBoxBorder(batch, hx - thumbw - gridGap/2 - 1, y - 1, thumbw + 2, thumbh + 2)
        // memory gauge
        Toolkit.drawBoxBorder(batch, hx - 330 - gridGap/2 - 1, y + listHeight - 1, 240 + 2, buttonHeight + 2)


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