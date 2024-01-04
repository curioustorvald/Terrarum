package net.torvald.terrarum

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.ControlPanelCommon
import net.torvald.terrarum.modulebasegame.ui.ControlPanelOptions
import net.torvald.terrarum.modulebasegame.ui.UIRemoCon
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextSelector
import net.torvald.unicode.TIMES

class ModOptionsHost(val remoCon: UIRemoCon) : UICanvas() {
    init {
        handler.allowESCtoClose = false
    }

    private val moduleAreaHMargin = 48
    private val moduleAreaBorder = 8

    override var width = 560
    override var height = 600//App.scr.height - moduleAreaHMargin * 2

    private val drawX = (Toolkit.drawWidth - width) / 2
    private val drawY = (App.scr.height - height) / 2

    private var currentlySelectedModule = "basegame"

    // List<Pair<proper name, mod key>>
    val configurableMods = ModMgr.moduleInfo.filter { it.value.configPlan.isNotEmpty() }.map { it.value.properName to it.key }.toList().sortedBy { it.second }

    private val modSelectorWidth = 360
    private val modSelector = UIItemTextSelector(this, drawX + (width - modSelectorWidth) / 2, drawY,
        configurableMods.map { { it.first } }, 0, modSelectorWidth, false, font = App.fontUITitle
    ).also { item ->
        item.selectionChangeListener = {
            currentlySelectedModule = configurableMods[it].second

            defer {
                uiItems.clear()
                makeConfig(currentlySelectedModule)
                addUIitem(item)
            }
        }
    }

    init {
        printdbg(this, "configurableMods = ${configurableMods.map { it.second }}")

        addUIitem(modSelector)
    }

    override fun show() {
        super.show()

        makeConfig(currentlySelectedModule)
    }

    private var deferred = {}
    private fun defer(what: () -> Unit) {
        deferred = what
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
            if (options[0].isBlank())
                arrayOf("", labelfun, options[2])
            else
                arrayOf("$modname:${options[0]}", labelfun, options[2])
        }.toTypedArray()

        ControlPanelCommon.register(this, width, "basegame.modcontrolpanel.$modname", modOptions)
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
        deferred(); deferred = {}
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // the actual control panel
        ControlPanelCommon.render("basegame.modcontrolpanel.$currentlySelectedModule", width, batch)
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun dispose() {
    }
}