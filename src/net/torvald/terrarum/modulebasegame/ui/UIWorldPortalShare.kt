package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.imagefont.BigAlphNum
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.toBig64
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.utils.PasswordBase32

/**
 * Created by minjaesong on 2023-09-03.
 */
class UIWorldPortalShare(private val full: UIWorldPortal) : UICanvas() {

    override var width = 434
    override var height = 480

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2
    private val goButtonWidth = 180
    private val buttonY = drawY + height - 24


    private val backButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, drawX + (width - goButtonWidth) / 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {
        it.clickOnceListener = { _,_ ->


            full.changePanelTo(0)
        }
    }

    init {
        addUIitem(backButton)
    }

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    private var shareCode = ""

    override fun show() {
        shareCode = Common.encodeUUID(INGAME.world.worldIndex)


        printdbg(this, shareCode)

        wotKeys = (1..4).map { Lang["CONTEXT_WORLD_CODE_SHARE_$it", false] }
    }

    private lateinit var wotKeys: List<String>

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = Color.WHITE

        val textY = drawY + (height/2) - App.fontGame.lineHeight.toInt() * 4 - 2
        val codeY = textY + App.fontGame.lineHeight.toInt() * 5

        // share code background
        batch.color = Toolkit.Theme.COL_INVENTORY_CELL_BORDER
        Toolkit.drawBoxBorder(batch, drawX - 1, codeY - 1, width + 2, BigAlphNum.H + 12)
        batch.color = Toolkit.Theme.COL_CELL_FILL
        Toolkit.fillArea(batch, drawX, codeY, width, BigAlphNum.H + 10)

        // share code
        batch.color = Toolkit.Theme.COL_MOUSE_UP
        Toolkit.drawTextCentered(batch, App.fontBigNumbers, shareCode, width, drawX, codeY + 5)

        // texts
        batch.color = Color.WHITE

        val textboxWidth = wotKeys.maxOf { App.fontGame.getWidth(it) }
        val tx = drawX + (width - textboxWidth) / 2
        wotKeys.forEachIndexed { i, s ->
            App.fontGame.draw(batch, s, tx, textY + App.fontGame.lineHeight.toInt() * i)
        }


        // ui title
        val titlestr = Lang["MENU_LABEL_SHARE"]
        App.fontUITitle.draw(batch, titlestr, drawX + (width - App.fontUITitle.getWidth(titlestr)).div(2).toFloat(), UIInventoryFull.INVENTORY_CELLS_OFFSET_Y() - 36f)

        // control hints
        App.fontGame.draw(batch, full.portalListingControlHelp, (Toolkit.drawWidth - width)/2 + 2, (UIInventoryFull.yEnd - 20).toInt())

        uiItems.forEach { it.render(frameDelta, batch, camera) }

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
