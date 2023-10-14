package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.ControlPanelCommon
import net.torvald.terrarum.modulebasegame.ui.ControlPanelOptions
import net.torvald.terrarum.modulebasegame.ui.UIRemoCon
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.unicode.TIMES

class ModOptionsHost(val remoCon: UIRemoCon) : UICanvas() {
    init {
        handler.allowESCtoClose = false
    }

    private val moduleAreaHMargin = 48
    private val moduleAreaBorder = 8

    override var width = App.scr.width - UIRemoCon.remoConWidth - moduleAreaHMargin
    override var height = App.scr.height - moduleAreaHMargin * 2

    init {

    }

    private var currentlySelectedModule = "basegame"

    override fun show() {
        super.show()

        makeConfig(currentlySelectedModule)
    }


    private fun makeConfig(modname: String) {
        val mod = ModMgr.moduleInfo[modname]!!
        if (mod.configPlan.isEmpty()) return

        val modOptions: ControlPanelOptions = mod.configPlan.map {
            val options = it.split("->")
            val labelfun = if (options[1].startsWith("Lang:")) {
                { Lang[options[1].substringAfter(":")] }
            }
            else {
                { options[1] }
            }
            arrayOf("$modname:${options[0]}", labelfun, options[2])
        }.toTypedArray()

        ControlPanelCommon.register(this, width, "basegame.modcontrolpanel.$modname", modOptions)
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        // TODO draw currently editing mod name

        ControlPanelCommon.render("basegame.modcontrolpanel.$currentlySelectedModule", width, batch)
        uiItems.forEach { it.render(batch, camera) }
    }

    override fun dispose() {
    }
}