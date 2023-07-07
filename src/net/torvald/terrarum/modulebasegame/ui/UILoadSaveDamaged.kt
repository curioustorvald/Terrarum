package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton

/**
 * Created by minjaesong on 2023-07-07.
 */
class UILoadSaveDamaged(val full: UILoadSavegame) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val goButtonWidth = 180
    private val drawX = (Toolkit.drawWidth - 480) / 2
    private val drawY = (App.scr.height - 480) / 2
    private val buttonRowY = drawY + 480 - 24
    private val corruptedBackButton = UIItemTextButton(this, "MENU_LABEL_BACK", (Toolkit.drawWidth - goButtonWidth) / 2, buttonRowY, goButtonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)

    init {
        corruptedBackButton.clickOnceListener = { _,_ ->
            full.changePanelTo(0)
            println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        }

        addUIitem(corruptedBackButton)
    }

    override fun updateUI(delta: Float) {
        corruptedBackButton.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        Toolkit.drawTextCentered(batch, App.fontGame, Lang["ERROR_SAVE_CORRUPTED"], Toolkit.drawWidth, 0, App.scr.height / 2 - 42)

        corruptedBackButton.render(batch, camera)
    }

    override fun dispose() {
    }

}