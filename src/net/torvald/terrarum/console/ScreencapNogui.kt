package net.torvald.terrarum.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.App
import net.torvald.terrarum.ccG
import net.torvald.terrarum.modulebasegame.IngameRenderer

object ScreencapNogui: ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            IngameRenderer.screencapExportCallback = {
                val w = 960
                val h = 640
                val p = Pixmap.createFromFrameBuffer((it.width - w).ushr(1), (it.height - h).ushr(1), w, h)
                PixmapIO2.writeTGA(Gdx.files.absolute(App.defaultDir + "/Exports/${args[1]}.tga"), p, true)
                p.dispose()
            }
            IngameRenderer.screencapRequested = true
            Echo("FBO exported to$ccG Exports/${args[1]}.tga")
        }
    }

    override fun printUsage() {

    }
}