package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton

class UIWorldPortalDelete(private val full: UIWorldPortal) : UICanvas() {

    override var width = 480
    override var height = 480

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2
    private val goButtonWidth = 180
    private val buttonY = drawY + height - 24

    private val deleteButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_DELETE"] }, drawX + (width/2 - goButtonWidth) / 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true, inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD).also {
            it.clickOnceListener = { _,_ ->
                full.removeWorldfromDict(full.selectedButton!!.worldInfo!!.uuid)
                full.changePanelTo(0)
            }
    }
    private val cancelButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CANCEL"] }, drawX + width/2 + (width/2 - goButtonWidth) / 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
            it.clickOnceListener = { _,_ ->
                full.changePanelTo(0)
            }
    }

    init {
        addUIitem(deleteButton)
        addUIitem(cancelButton)
    }


    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }


    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        full.selectedButton?.let {
            val buttonYdelta = (App.scr.tvSafeGraphicsHeight + 172 + 36) - it.posY
            val buttonXdelta = (Toolkit.drawWidth - it.width) / 2 - it.posX
            it.render(frameDelta, batch, camera, buttonXdelta, buttonYdelta)
        }

        uiItems.forEach { it.render(frameDelta, batch, camera) }

        batch.color = Color.WHITE
        // ui title
        val titlestr = Lang["MENU_LABEL_DELETE_WORLD"]
        App.fontUITitle.draw(batch, titlestr, drawX + (width - App.fontUITitle.getWidth(titlestr)).div(2).toFloat(), UIInventoryFull.INVENTORY_CELLS_OFFSET_Y() - 36f)


//        Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_SAVE_WILL_BE_DELETED"], Toolkit.drawWidth, 0, (App.scr.tvSafeGraphicsHeight + 172 + 36) - 46)
        Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_ARE_YOU_SURE"], Toolkit.drawWidth, 0, (App.scr.tvSafeGraphicsHeight + 172 + 36) + UIItemWorldCellsSimple.height + 36)


        // control hints
        batch.color = Color.WHITE
        App.fontGame.draw(batch, full.portalListingControlHelp, (Toolkit.drawWidth - width)/2 + 2, (UIInventoryFull.yEnd - 20).toInt())
    }

    override fun dispose() {
    }

    override fun doOpening(delta: Float) {
        full.selectedButton?.forceMouseDown = true
    }

    override fun doClosing(delta: Float) {
        full.selectedButton?.forceMouseDown = false
    }

}
