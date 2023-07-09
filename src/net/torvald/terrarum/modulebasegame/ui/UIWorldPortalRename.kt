package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*
import net.torvald.unicode.EMDASH

class UIWorldPortalRename(private val full: UIWorldPortal) : UICanvas() {

    override var width = 480
    override var height = 480

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2
    private val goButtonWidth = 180
    private val buttonY = drawY + height - 24

    private val inputWidth = UIItemWorldCellsSimple.width


    private val nameInput = UIItemTextLineInput(this,
        (Toolkit.drawWidth - inputWidth) / 2, drawY + 300, inputWidth,
        { "" }, InputLenCap(256, InputLenCap.CharLenUnit.CODEPOINTS)
    )

    private val renameButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_RENAME"] }, drawX + (width/2 - goButtonWidth) / 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->
            val newName = nameInput.getText().trim()
            if (newName.isNotBlank()) {
                full.selectedButton!!.worldInfo!!.uuid.let { uuid ->
                    App.savegameWorldsName[uuid] = newName
                    App.savegameWorlds[uuid]!!.renameWorld(newName)
                    full.selectedButton!!.worldName = newName
                }
            }

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
        addUIitem(renameButton)
        addUIitem(cancelButton)
        addUIitem(nameInput)
    }


    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    private var oldPosX = full.posX

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val posXDelta = posX - oldPosX

        // ugh why won't you just scroll along??
//        nameInput.posX += posXDelta // is it fixed now?

        batch.color = Color.WHITE
        // ui title
        val titlestr = Lang["MENU_LABEL_RENAME"]
        App.fontUITitle.draw(batch, titlestr, drawX + (width - App.fontUITitle.getWidth(titlestr)).div(2).toFloat(), UIInventoryFull.INVENTORY_CELLS_OFFSET_Y() - 36f)

        full.selectedButton?.let {
            val buttonYdelta = (App.scr.tvSafeGraphicsHeight + 172 + 36) - it.posY
            val buttonXdelta = (Toolkit.drawWidth - it.width) / 2 - it.posX
            it.render(batch, camera, buttonXdelta, buttonYdelta)
        }

        uiItems.forEach { it.render(batch, camera) }


        oldPosX = posX
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
