package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.printStackTrace
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.utils.RandomWordsName

/**
 * Created by minjaesong on 2023-09-03.
 */
class UIWorldPortalUseInvitation(val full: UIWorldPortal) : UICanvas() {

    override var width = 480
    override var height = 480

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    private val inputWidth = 350
    private val inputLineY1 = 90
    private val inputLineY2 = 130

    private val sizeSelY = 186 + 40
    private val goButtonWidth = 180
    private val gridGap = 10
    private val buttonBaseX = (Toolkit.drawWidth - 3 * goButtonWidth - 2 * gridGap) / 2
    private val buttonY = drawY + height - 24


    private val codeInput = UIItemTextLineInput(this,
        drawX + width - inputWidth + 5, drawY + sizeSelY + inputLineY1, inputWidth,
        { "AAAA BB CCCCC DDDDD EEEEE FFFFF" }, InputLenCap(VirtualDisk.NAME_LENGTH, InputLenCap.CharLenUnit.UTF8_BYTES)
    )



    private val backButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_BACK"] }, buttonBaseX, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            full.requestTransition(0)
        }
    }
    private val searchWorldButton = UIItemTextButton(this,
        { Lang["CONTEXT_WORLD_NEW"] }, buttonBaseX + goButtonWidth + gridGap, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->
            full.queueUpSearchScr()
            full.requestTransition(1)
        }
    }
    private val goButton: UIItemTextButton = UIItemTextButton(this,
        { Lang["MENU_LABEL_CONFIRM_BUTTON"] }, buttonBaseX + (goButtonWidth + gridGap) * 2, buttonY, goButtonWidth, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true).also {

        it.clickOnceListener = { _, _ ->

        }
    }

    init {
        addUIitem(backButton)
        addUIitem(searchWorldButton)
        addUIitem(goButton)
        addUIitem(codeInput)
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {

        // input labels
        batch.color = Color.WHITE
        App.fontGame.draw(batch, Lang["CREDITS_CODE"], drawX - 4, drawY + sizeSelY + inputLineY1)

        // control hints
        App.fontGame.draw(batch, full.portalListingControlHelp, 2 + (Toolkit.drawWidth - 560)/2 + 2, (full.yEnd - 20).toInt())


        uiItems.forEach { it.render(batch, camera) }
    }

    override fun dispose() {
    }


    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        super.inputStrobed(e)
    }

}