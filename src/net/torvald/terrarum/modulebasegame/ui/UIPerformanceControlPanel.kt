package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*

/**
 * Created by minjaesong on 2023-06-22.
 */
class UIPerformanceControlPanel(remoCon: UIRemoCon?) : UICanvas() {

    override var width = 560

    init {
        handler.allowESCtoClose = false

        ControlPanelCommon.register(this, width, "basegame.performancecontrolpanel", arrayOf(
            arrayOf("", { Lang["MENU_OPTIONS_GAMEPLAY"] }, "h1"),
                arrayOf("autosaveinterval", { Lang["MENU_OPTIONS_AUTOSAVE"] + " (${Lang["CONTEXT_TIME_MINUTE_PLURAL"]})" }, "spinnerimul,5,120,5,60000"),
                arrayOf("notificationshowuptime", { Lang["MENU_OPTIONS_NOTIFICATION_DISPLAY_DURATION"] + " (${Lang["CONTEXT_TIME_SECOND_PLURAL"]})" }, "spinnerimul,2,10,1,1000"),
            arrayOf("", { Lang["MENU_LABEL_GRAPHICS"] }, "h1"),
                arrayOf("atlastexsize", { Lang["MENU_OPTIONS_ATLAS_TEXTURE_SIZE"] }, "spinnersel,1024,2048,4096,8192"),
                arrayOf("lightpasses", { Lang["MENU_OPTIONS_LIGHT_UPDATE_PASSES"] }, "spinner,2,4,1"),
            arrayOf("", { Lang["MENU_MODULES"] }, "h1"),
                arrayOf("enablescriptmods", { Lang["MENU_OPTIONS_ENABLE_SCRIPT_MODS"] }, "toggle"),
                arrayOf("", { "(${Lang["MENU_LABEL_RESTART_REQUIRED"]})" }, "p"),
                arrayOf("", { "${Lang["MENU_LABEL_WARN_ACE"]}" }, "emph"),
            arrayOf("", { Lang["MENU_LABEL_JVM_DNT"] }, "h1"),
                arrayOf("jvm_xmx", { Lang["MENU_OPTIONS_JVM_HEAP_MAX"] + " (GB)" }, "spinner,2,32,1"),
                arrayOf("jvm_extra_cmd", { Lang["MENU_LABEL_EXTRA_JVM_ARGUMENTS"] }, "typein"),
                arrayOf("", { "(${Lang["MENU_LABEL_RESTART_REQUIRED"]})" }, "p"),
            )
        )
    }

    override var height = ControlPanelCommon.getMenuHeight("basegame.performancecontrolpanel")

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        ControlPanelCommon.render("basegame.performancecontrolpanel", width, batch)
        uiItems.forEach { it.render(batch, camera) }
    }

    override fun dispose() {
    }
}