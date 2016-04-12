package net.torvald.terrarum.console

import net.torvald.terrarum.Game
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-01-19.
 */
class ToggleNoClip : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val status = Terrarum.game.player.isNoClip()

        Terrarum.game.player.setNoClip(!status)
        Echo().execute("Set no-clip status to " + (!status).toString())
    }

    override fun printUsage() {
        Echo().execute("toggle no-clip status of player")
    }
}
