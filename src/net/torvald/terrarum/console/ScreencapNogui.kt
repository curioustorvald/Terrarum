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
                val p = Pixmap.createFromFrameBuffer(0, 0, it.width, it.height)
                PixmapIO2.writeTGA(Gdx.files.absolute(App.defaultDir + "/Exports/${args[1]}.tga"), p, true)
                p.dispose()
            }
            IngameRenderer.screencapRequested = true
            Echo("FBO exported to$ccG Exports/${args[1]}.tga")
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: screencapnogui <output filename>")
    }
}