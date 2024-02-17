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
            arrayOf("", { Lang["MENU_OPTIONS_GAMEPLAY", true] }, "h1"),
                arrayOf("autosaveinterval", { Lang["MENU_OPTIONS_AUTOSAVE", true] + " (${Lang["CONTEXT_TIME_MINUTE_PLURAL"].lowercase()})" }, "spinnerimul,5,120,5,60000"),
                arrayOf("notificationshowuptime", { Lang["MENU_OPTIONS_NOTIFICATION_DISPLAY_DURATION"] + " (${Lang["CONTEXT_TIME_SECOND_PLURAL"].lowercase()})" }, "spinnerimul,2,10,1,1000"),
                arrayOf("savegamecomp", { Lang["MENU_OPTIONS_SAVEFORMAT", true] }, "textsel,zstd=MENU_OPTIONS_SAVEFORMAT_SMALL,snappy=MENU_OPTIONS_SAVEFORMAT_FAST"),
            arrayOf("", { Lang["MENU_MODULES", true] }, "h1"),
                arrayOf("", { "(${Lang["MENU_LABEL_RESTART_REQUIRED"]})" }, "p"),
                arrayOf("enablescriptmods", { Lang["MENU_OPTIONS_ENABLE_SCRIPT_MODS", true] }, "toggle"),
                arrayOf("", { "${Lang["MENU_LABEL_WARN_ACE"]}" }, "emph"),
            arrayOf("", { Lang["MENU_LABEL_JVM_DNT", true] }, "h1"),
                arrayOf("", { "(${Lang["MENU_LABEL_RESTART_REQUIRED"]})" }, "p"),
                arrayOf("jvm_xmx", { Lang["MENU_OPTIONS_JVM_HEAP_MAX", true] + " (GB)" }, "spinner,2,32,1"),
                arrayOf("jvm_extra_cmd", { Lang["MENU_LABEL_EXTRA_JVM_ARGUMENTS", true] }, "typein"),
            )
        )
    }

    override var height = ControlPanelCommon.getMenuHeight("basegame.performancecontrolpanel")

    override fun updateImpl(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        ControlPanelCommon.render("basegame.performancecontrolpanel", width, batch)
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun dispose() {
    }
}