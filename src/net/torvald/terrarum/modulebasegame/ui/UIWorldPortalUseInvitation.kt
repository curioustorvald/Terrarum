package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.printStackTrace
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.toBigInt64
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.utils.PasswordBase32
import net.torvald.terrarum.utils.RandomWordsName
import java.util.*

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
        drawX + width - inputWidth + 5, drawY + sizeSelY, inputWidth,
        { "AAAAA BBBBB CCCCC DDDDD EEEEE" }, InputLenCap(31, InputLenCap.CharLenUnit.CODEPOINTS)
    ).also {
        // reset importReturnCode if the text input has changed
        it.onKeyDown = { _ ->
            importReturnCode = 0
        }
    }



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
            val uuid = Common.decodeToUUID(codeInput.getText())
            val world = App.savegameWorlds[uuid]

            App.printdbg(this, "Decoded UUID=$uuid")

            // world exists?
            if (world == null) {
                importReturnCode = 1
            }
            else {
                // add the world to the player's worldbook
                // TODO memory cap check? or disable check against shared worlds?
                full.addWorldToPlayersDict(uuid)
                full.cleanUpWorldDict()

                full.requestTransition(0)
            }
        }
    }

    private var importReturnCode = 0
    private val errorMessages = listOf(
        "", // 0
        Lang["ERROR_WORLD_NOT_FOUND"], // 1
    )

    init {
        addUIitem(backButton)
        addUIitem(searchWorldButton)
        addUIitem(goButton)
        addUIitem(codeInput)
    }

    override fun show() {
        super.show()
        codeInput.clearText()
        importReturnCode = 0
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        // error messages
        if (importReturnCode != 0) {
            batch.color = Toolkit.Theme.COL_RED
            val tby = codeInput.posY
            val btny = backButton.posY
            Toolkit.drawTextCentered(batch, App.fontGame, errorMessages[importReturnCode], Toolkit.drawWidth, 0, (tby + btny) / 2)
        }

        // input labels
        batch.color = Color.WHITE
        App.fontGame.draw(batch, Lang["CREDITS_CODE"], drawX - 4, drawY + sizeSelY)

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