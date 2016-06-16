package net.torvald.terrarum.console

import net.torvald.imagefont.GameFontBase
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-06-16.
 */
class Seed : ConsoleCommand {
    val ccG = GameFontBase.colToCode["g"]
    val ccW = GameFontBase.colToCode["w"]
    val ccY = GameFontBase.colToCode["y"]
    //                                tsalagi

    override fun execute(args: Array<String>) {
        Echo().execute("${ccY}Map$ccW: $ccG${Terrarum.game.map.generatorSeed}")
        // TODO display randomiser seed
    }

    override fun printUsage() {
        Echo().execute("prints out the generator seed of the current game.")
    }
}