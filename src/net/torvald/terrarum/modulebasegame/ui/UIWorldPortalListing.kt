package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureWorldPortal
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
import net.torvald.terrarum.utils.PasswordBase32
import net.torvald.unicode.EMDASH
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.math.ceil

/**
 *
 * "Teleport" button sets the destination and makes the portal surface to 'glow' to indicate the teleportation is ready.
 * Teleportation is initiated with a rising edge of a logic signal.
 *
 * "New World" sets the parameter of the new world, and make the new (not yet generated) world as the teleportation target.
 * THe world generation will be done when the "teleportation" is on going.
 *
 * Created by minjaesong on 2023-05-19.
 */
class UIWorldPortalListing(val full: UIWorldPortal) : UICanvas() {


    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val buttonHeight = 24
    private val gridGap = 10



    private val thumbw = 400
    private val textAreaW = thumbw - 32
    private val thumbh = 268
    private val hx = Toolkit.drawWidth.div(2)
    private val y = INVENTORY_CELLS_OFFSET_Y() + 1 - 34

    private val listCount = getCellCountVertically(UIItemWorldCellsSimple.height, gridGap)
    private val listHeight = UIItemWorldCellsSimple.height + (listCount - 1) * (UIItemWorldCellsSimple.height + gridGap)

    private val memoryGaugeWidth = textAreaW
    private val buttonWidth = (UIItemWorldCellsSimple.width + thumbw - 2 * gridGap) / 4 // thumbw + cellw + gridgap = 4*gridgap + 5x
    private val buttonsY = y + listHeight + gridGap

    private val screencapX = hx - thumbw - gridGap/2
    
    private val buttonSearch = UIItemTextButton(this,
        { Lang["CONTEXT_WORLD_NEW"] },
        screencapX,
        buttonsY,
        buttonWidth,
        hasBorder = true,
        alignment = UIItemTextButton.Companion.Alignment.CENTRE
    ).also {
        it.clickOnceListener = { _,_ ->
            full.queueUpSearchScr()
            full.requestTransition(1)
        }
    }
    private val buttonRename = UIItemTextButton(this,
        { Lang["MENU_LABEL_RENAME"] },
        screencapX + buttonWidth + gridGap,
        buttonsY,
        buttonWidth,
        hasBorder = true,
        alignment = UIItemTextButton.Companion.Alignment.CENTRE
    ).also {
        it.clickOnceListener = { _,_ ->
            full.queueUpRenameScr()
            full.changePanelTo(1)
        }
    }
    private val buttonTeleport = UIItemTextButton(this,
        { Lang["GAME_ACTION_TELEPORT"] },
        screencapX + (buttonWidth + gridGap) * 2,
        buttonsY,
        buttonWidth,
        hasBorder = true,
        alignment = UIItemTextButton.Companion.Alignment.CENTRE
    ).also {
        it.clickOnceListener = { _,_ ->
            if (selected?.worldInfo != null) {
                full.host.teleportRequest = FixtureWorldPortal.TeleportRequest(
                    selected?.worldInfo?.diskSkimmer, null
                )
                full.setAsClose()
                printdbg(this, "Teleport target set: ${full.host.teleportRequest}")
            }
        }
    }
    /*private val buttonShare = UIItemTextButton(this,
        { Lang["MENU_LABEL_SHARE"] },
        screencapX + (buttonWidth + gridGap) * 3,
        buttonsY,
        buttonWidth,
        hasBorder = true,
        alignment = UIItemTextButton.Companion.Alignment.CENTRE,
    ).also {
        it.clickOnceListener = { _,_ ->
            full.queueUpShareScr()
            full.changePanelTo(1)
        }
    }*/
    private val buttonDelete = UIItemTextButton(this,
        { Lang["MENU_LABEL_DELETE_WORLD"] },
        screencapX + (buttonWidth + gridGap) * 3,
        buttonsY,
        buttonWidth,
        hasBorder = true,
        alignment = UIItemTextButton.Companion.Alignment.CENTRE,
        inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD
    ).also {
        it.clickOnceListener = { _,_ ->
            full.queueUpDeleteScr()
            full.changePanelTo(1)
        }
    }


    private val navRemoCon = UIItemListNavBarVertical(full, hx + 6 + UIItemWorldCellsSimple.width, y + 7, listHeight + 2, false)

    private val worldList = ArrayList<WorldInfo>()
    data class WorldInfo(
        val uuid: UUID,
        val diskSkimmer: DiskSkimmer,
        val dimensionInChunks: Int,
        val seed: Long,
        val lastPlayedString: String,
        val totalPlayedString: String,
        val rawThumbnail: Pixmap?,
    ): Disposable {

        private var screenshot0: TextureRegion? = null

        val screenshot: TextureRegion
            get() {
                if (screenshot0 == null) {
                    val thumbTex = Texture(rawThumbnail)
                    thumbTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                    screenshot0 = TextureRegion(thumbTex)
                }
                return screenshot0!!
            }

        override fun dispose() {
            screenshot0?.texture?.dispose()
        }
    }

    private var showSpinner = true
    private val spinner = CommonResourcePool.getAsTextureRegionPack("inline_loading_spinner")
    private var spinnerTimer = 0f
    private var spinnerFrame = 0
    private val spinnerInterval = 1f / 60

    private fun disableListEditButtons() {
        buttonRename.isEnabled = false
        buttonDelete.isEnabled = false
        buttonTeleport.isEnabled = false
//        buttonShare.isEnabled = false
        currentWorldSelected = false
    }

    private fun highlightListEditButtons(info: WorldInfo?) {
        // will disable the delete and teleport button if the world is equal to the currently playing world

        if (info == null) disableListEditButtons()
        else {
            buttonRename.isEnabled = true
            buttonDelete.isEnabled = info.uuid != INGAME.world.worldIndex
            buttonTeleport.isEnabled = info.uuid != INGAME.world.worldIndex
//            buttonShare.isEnabled = true
            currentWorldSelected = info.uuid == INGAME.world.worldIndex
        }
    }

    private var currentWorldSelected = false

    init {
        navRemoCon.scrollUpListener = { _,_ -> scrollItemPage(-1) }
        navRemoCon.scrollDownListener = { _,_ -> scrollItemPage(1) }

        addUIitem(buttonDelete)
        addUIitem(buttonRename)
        addUIitem(buttonTeleport)
        addUIitem(buttonSearch)
//        addUIitem(buttonShare)
        addUIitem(navRemoCon)
    }

    fun scrollItemPage(relativeAmount: Int) {
        listPage = if (listPageCount == 0) 0 else (listPage + relativeAmount).fmod(listPageCount)
    }

    private lateinit var worldCells: Array<UIItemWorldCellsSimple>

    private var selected: UIItemWorldCellsSimple? = null
    private var selectedIndex: Int? = null

    var listPage
        set(value) {
            navRemoCon.itemPage = if (listPageCount == 0) 0 else (value).fmod(listPageCount)
            rebuildList()
        }
        get() = navRemoCon.itemPage

    var listPageCount // TODO total size of current category / items.size
        protected set(value) {
            navRemoCon.itemPageCount = value
        }
        get() = navRemoCon.itemPageCount

    private fun readWorldList() {
        worldList.clear()
        (INGAME.actorGamer.actorValue.getAsString(AVKey.WORLD_PORTAL_DICT) ?: "").split(",").filter { it.isNotBlank() }.map {
            it.ascii85toUUID().let { it to App.savegameWorlds[it] }
        }.filter { it.second != null }.mapIndexed { index, (uuid, disk0) ->

            val disk = disk0!!.loadable()
            var chunksCount = 0
            var seed = 0L
            var lastPlayed = 0L
            var totalPlayed = 0L
            var w = 0
            var h = 0
            var thumb: Pixmap? = null

            disk.rebuild()

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

                thumb = Pixmap(tgaFileContents, 0, tgaFileContents.size)
            }

            WorldInfo(uuid, disk, chunksCount, seed, lastPlayed.toTimestamp(), totalPlayed.toDurationStamp(), thumb)
        }.let {
            worldList.addAll(it)
        }
        full.chunksUsed = worldList.sumOf { it.dimensionInChunks }
        listPageCount = ceil(worldList.size.toDouble() / listCount).toInt()


        printdbg(this, "worldList.size=${worldList.size}")
    }
    private fun rebuildList() {
        worldCells = Array(listCount) { it0 ->
            val it = it0 + listCount * listPage
            UIItemWorldCellsSimple(
                this,
                hx + gridGap / 2,
                y + (gridGap + UIItemWorldCellsSimple.height) * it0,
                worldList.getOrNull(it),
                worldList.getOrNull(it)?.diskSkimmer?.getDiskName(Common.CHARSET)
            ).also { button ->
                button.clickOnceListener = { _, _ ->
                    full.selectedButton = button
                    selected = button
                    selectedIndex = it
                    highlightListEditButtons(worldList.getOrNull(it))
                    updateUIbyButtonSelection()
                }
            }
        }
    }

    private var threadFired = false

    override fun show() {
        super.show()

        listPage = 0
        showSpinner = true

        uiItems.forEach { it.show() }
        worldCells.forEach { it.show() }
        selected = null

        disableListEditButtons()
        updateUIbyButtonSelection()

        if (!threadFired) {
            threadFired = true
            Thread {
                readWorldList()
                rebuildList()
                showSpinner = false
                threadFired = false
            }.start()
        }
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

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }
        if (::worldCells.isInitialized) worldCells.forEach { it.update(delta) }

        if (currentWorldSelected) {
            if (buttonTeleport.mouseUp || buttonDelete.mouseUp)
                acquireTooltip(Lang["CONTEXT_THIS_IS_A_WORLD_CURRENTLY_PLAYING"])
            else
                releaseTooltip()
        }

        if (showSpinner) {
            spinnerTimer += delta
            while (spinnerTimer > spinnerInterval) {
                spinnerFrame = (spinnerFrame + 1) % 32
                spinnerTimer -= spinnerInterval
            }
        }
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
    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        val memoryGaugeXpos = hx - memoryGaugeWidth - gridGap/2
        val memoryGaugeYpos = y + listHeight - buttonHeight
        val textXpos = memoryGaugeXpos + 3

        // draw background //
        // screencap panel
        batch.color = UIInventoryFull.CELL_COL
        Toolkit.fillArea(batch, screencapX, y, thumbw, thumbh)


        // draw border //
        // screencap panel
        batch.color = if (selected?.worldInfo == null) Toolkit.Theme.COL_INVENTORY_CELL_BORDER else Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, screencapX - 1, y - 1, thumbw + 2, thumbh + 2)


        // memory gauge
        val barCol = UIItemInventoryCellCommonRes.getHealthMeterColour(full.chunksMax - full.chunksUsed, 0, full.chunksMax)
        val barBack = barCol mul UIItemInventoryCellCommonRes.meterBackDarkening

        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, (memoryGaugeXpos - iconSizeGap + 10).toInt(), memoryGaugeYpos, buttonHeight + 5, buttonHeight)
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, (memoryGaugeXpos - iconSizeGap + 10).toInt() - 1, memoryGaugeYpos - 1, buttonHeight + 7, buttonHeight + 2)
        batch.color = Color.WHITE
        batch.draw(icons.get(2, 2), textXpos - iconSizeGap, memoryGaugeYpos + 2f)

        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, memoryGaugeXpos - 1, memoryGaugeYpos - 1, memoryGaugeWidth + 2, buttonHeight + 2)
        batch.color = barBack
        Toolkit.fillArea(batch, memoryGaugeXpos, memoryGaugeYpos, memoryGaugeWidth, buttonHeight)
        batch.color = barCol
        Toolkit.fillArea(batch, memoryGaugeXpos, memoryGaugeYpos, (memoryGaugeWidth * (full.chunksUsed / full.chunksMax.toFloat())).ceilToInt(), buttonHeight)

        batch.color = Color.WHITE
        if (selected?.worldInfo != null) {
            // background for texts panel
            batch.color = Toolkit.Theme.COL_CELL_FILL
            Toolkit.fillArea(batch, screencapX, y + thumbh + 3, thumbw, 10 + worldTexts.size * textualListHeight.toInt())
            batch.color = Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, screencapX - 1, y + thumbh + 2, thumbw + 2, 10 + worldTexts.size * textualListHeight.toInt() + 2)

            // some texts
            batch.color = Color.WHITE
            worldTexts.forEachIndexed { index, (icon, str) ->
                batch.draw(icon, textXpos - iconSizeGap + 6, y + thumbh + 3+10 + textualListHeight * index)
                App.fontGame.draw(batch, str, textXpos + 6f, y + thumbh + 3+10 + textualListHeight * index - 2f)
            }
            // size indicator on the memory gauge
            Toolkit.fillArea(batch, memoryGaugeXpos, memoryGaugeYpos, (memoryGaugeWidth * (selected?.worldInfo!!.dimensionInChunks / full.chunksMax.toFloat())).ceilToInt(), buttonHeight)

            // thumbnail
            selected?.worldInfo?.screenshot?.let {
                batch.draw(it, (screencapX).toFloat(), y.toFloat(), thumbw.toFloat(), thumbh.toFloat())
            }
        }

        // loading spinner
        if (showSpinner) {
            val spin = spinner.get(spinnerFrame % 8, spinnerFrame / 8)
            batch.draw(spin, screencapX + (thumbw - spin.regionWidth) / 2f, y + (thumbh - spin.regionHeight) / 2f)
        }


        uiItems.forEach { it.render(frameDelta, batch, camera) }
        if (::worldCells.isInitialized) worldCells.forEach { it.render(frameDelta, batch, camera) }

        // control hints
        batch.color = Color.WHITE
        App.fontGame.draw(batch, full.portalListingControlHelp, screencapX + 2, (UIInventoryFull.yEnd - 20).toInt())
    }

    override fun hide() {
        super.hide()
        if (::worldCells.isInitialized) worldCells.forEach { it.hide() }

        if (::worldCells.isInitialized) worldCells.forEach { it.tryDispose() }
        worldList.forEach { it.tryDispose() }
    }

    override fun dispose() {
        uiItems.forEach { it.dispose() }
        if (::worldCells.isInitialized) worldCells.forEach { it.tryDispose() }
        worldList.forEach { it.tryDispose() }
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
            if (::worldCells.isInitialized) worldCells.forEach { it.touchUp(screenX, screenY, pointer, button) }
            handler.subUIs.forEach { it.touchUp(screenX, screenY, pointer, button) }
            return true
        }
        else return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (this.isVisible) {
            uiItems.forEach { it.scrolled(amountX, amountY) }
            if (::worldCells.isInitialized) worldCells.forEach { it.scrolled(amountX, amountY) }
            handler.subUIs.forEach { it.scrolled(amountX, amountY) }
            return true
        }
        else return false
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        INGAME.pause()
        clearTooltip()
    }

    override fun doClosing(delta: Float) {
        super.doClosing(delta)
        INGAME.resume()
        clearTooltip()
    }

}


class UIItemWorldCellsSimple(
    val parent: UIWorldPortalListing,
    initialX: Int,
    initialY: Int,
    internal val worldInfo: UIWorldPortalListing.WorldInfo? = null,
    internal var worldName: String? = null,
) : UIItem(parent, initialX, initialY) {

    var forceMouseDown = false

    companion object {
        const val width = 400
        const val height = 46
    }

    override val width: Int = 400
    override val height: Int = 46

    private val icons = CommonResourcePool.getAsTextureRegionPack("terrarum-basegame-worldportalicons")

    var highlighted = false

    fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera, offX: Int, offY: Int) {
        super.render(frameDelta, batch, camera)

        val posX = posX + offX
        val posY = posY + offY

        // draw background
        batch.color = UIInventoryFull.CELL_COL
        Toolkit.fillArea(batch, posX, posY, width, height)

        val mouseUp = mouseUp && worldInfo != null && !forceMouseDown

        val bcol = if (highlighted && !forceMouseDown || mouseUp && mousePushed) Toolkit.Theme.COL_SELECTED
        else if (mouseUp) Toolkit.Theme.COL_MOUSE_UP else (if (worldInfo == null && !forceMouseDown) Toolkit.Theme.COL_INVENTORY_CELL_BORDER else Toolkit.Theme.COL_INACTIVE)
        val tcol = if (highlighted && !forceMouseDown || mouseUp && mousePushed) Toolkit.Theme.COL_SELECTED
        else if (mouseUp) Toolkit.Theme.COL_MOUSE_UP else (if (worldInfo == null && !forceMouseDown) Toolkit.Theme.COL_INACTIVE else Toolkit.Theme.COL_LIST_DEFAULT)

        // draw border
        batch.color = bcol
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        // draw texts
        batch.color = tcol
        batch.draw(icons.get(0, 1), posX + 4f, posY + 1f)
        App.fontGame.draw(batch, worldName ?: "$EMDASH", posX + 32, posY - 1)
        batch.draw(icons.get(1, 1), posX + 4f, posY + 25f)
        App.fontGame.draw(batch, if (worldInfo?.seed == null) "$EMDASH" else "${(if (worldInfo.seed > 0) "+" else "")}${worldInfo.seed}" , posX + 32, posY + 23)
        // text separator
        batch.color = bcol.cpy().sub(0f,0f,0f,0.65f)
        Toolkit.fillArea(batch, posX + 2, posY + 23, width - 4, 1)
    }


    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        render(frameDelta, batch, camera, 0, 0)
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {

        if (mouseUp) {
            parent.scrollItemPage(amountY.toInt())
        }

        return true
    }

    override fun dispose() {
    }
}