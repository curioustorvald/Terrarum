package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.ControlPanelCommon.makeButton
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.TIMES

/**
 * Created by minjaesong on 2023-06-22.
 */
class UIPerformanceControlPanel(remoCon: UIRemoCon?) : UICanvas() {

    override var width = 560

    init {
        ControlPanelCommon.register(this, width, "basegame.performancecontrolpanel", arrayOf(
            arrayOf("", { Lang["MENU_OPTIONS_GAMEPLAY"] }, "h1"),
                arrayOf("autosaveinterval", { Lang["MENU_OPTIONS_AUTOSAVE"] + " (${Lang["CONTEXT_TIME_MINUTE_PLURAL"]})" }, "spinnerimul,1,120,1,60000"),
                arrayOf("notificationshowuptime", { Lang["MENU_OPTIONS_NOTIFICATION_DISPLAY_DURATION"] + " (${Lang["CONTEXT_TIME_SECOND_PLURAL"]})" }, "spinnerimul,2,10,1,1000"),
            arrayOf("", { Lang["MENU_LABEL_JVM_DNT"] }, "h1"),
                arrayOf("jvm_xmx", { Lang["MENU_OPTIONS_JVM_HEAP_MAX"] + " (GB)" }, "spinner,2,32,1"),
                arrayOf("jvm_extra_cmd", { Lang["MENU_LABEL_EXTRA_JVM_ARGUMENTS"] }, "typein"),
                arrayOf("", { "(${Lang["MENU_LABEL_RESTART_REQUIRED"]})" }, "p"),
        ))
    }

    override var height = ControlPanelCommon.getMenuHeight("basegame.performancecontrolpanel")

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        ControlPanelCommon.render("basegame.performancecontrolpanel", width, batch)
        uiItems.forEach { it.render(batch, camera) }
    }

    override fun dispose() {
    }
}