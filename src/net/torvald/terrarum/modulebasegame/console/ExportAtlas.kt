package net.torvald.terrarum.modulebasegame.console

import com.badlogic.gdx.Gdx
import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.App
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import java.io.File

/**
 * Created by minjaesong on 2024-07-15.
 */
class ExportAtlas : ConsoleCommand {

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val dir = App.defaultDir + "/Exports/"
            val dirAsFile = File(dir)
            if (!dirAsFile.exists()) {
                dirAsFile.mkdir()
            }

            PixmapIO2.writeTGA(Gdx.files.absolute("$dir${args[1]}.tga"), App.tileMaker.atlas, false)
        }
    }

    override fun printUsage() {
        Echo("Usage: exportatlas <name>")
        Echo("Exports current tile atlas into an image.")
        Echo("The image can be found at %appdata%/terrarum/Exports")
    }
}