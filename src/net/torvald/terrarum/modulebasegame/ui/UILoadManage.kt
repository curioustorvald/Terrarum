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
 * Created by minjaesong on 2023-07-05.
 */
class UILoadManage(val full: UILoadSavegame) : UICanvas() {

    override var width: Int = Toolkit.drawWidth
    override var height: Int = App.scr.height

    private val goButtonWidth = 180
    private val drawX = (Toolkit.drawWidth - 480) / 2
    private val drawY = (App.scr.height - 480) / 2
    private val buttonRowY = drawY + 480 - 24
    private val corruptedBackButton = UIItemTextButton(this, "MENU_LABEL_BACK", (Toolkit.drawWidth - goButtonWidth) / 2, buttonRowY, goButtonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
    private val confirmCancelButton = UIItemTextButton(this, "MENU_LABEL_CANCEL", drawX + (240 - goButtonWidth) / 2, buttonRowY, goButtonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true)
    private val confirmDeleteButton = UIItemTextButton(this, "MENU_LABEL_DELETE", drawX + 240 + (240 - goButtonWidth) / 2, buttonRowY, goButtonWidth, true, alignment = UIItemTextButton.Companion.Alignment.CENTRE, hasBorder = true, inactiveCol = Toolkit.Theme.COL_RED, activeCol = Toolkit.Theme.COL_REDD)

    private var mode = 0

    private val MODE_INIT = 0
    private val MODE_DELETE = 16 // are you sure?
    private val MODE_RENAME = 32 // show rename dialogue

    init {
        corruptedBackButton.clickOnceListener = { _,_ -> full.remoCon.openUI(UILoadSavegame(full.remoCon)) }
        confirmCancelButton.clickOnceListener = { _,_ -> full.remoCon.openUI(UILoadSavegame(full.remoCon)) }
        confirmDeleteButton.clickOnceListener = { _,_ ->
            val pu = full.buttonSelectedForDeletion!!.playerUUID
            val wu = full.buttonSelectedForDeletion!!.worldUUID
            App.savegamePlayers[pu]?.moveToRecycle(App.recycledPlayersDir)?.let {
                App.sortedPlayers.remove(pu)
                App.savegamePlayers.remove(pu)
                App.savegamePlayersName.remove(pu)
            }
            // don't delete the world please
            full.remoCon.openUI(UILoadSavegame(full.remoCon))
        }
    }


    override fun updateUI(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        if (mode == MODE_DELETE) {
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_SAVE_WILL_BE_DELETED"], Toolkit.drawWidth, 0, titleTopGradEnd + cellInterval - 46)
            Toolkit.drawTextCentered(batch, App.fontGame, Lang["MENU_LABEL_ARE_YOU_SURE"], Toolkit.drawWidth, 0, titleTopGradEnd + cellInterval + SAVE_CELL_HEIGHT + 36)

            full.buttonSelectedForDeletion!!.render(batch, camera)

            confirmCancelButton.render(batch, camera)
            confirmDeleteButton.render(batch, camera)
        }
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

}