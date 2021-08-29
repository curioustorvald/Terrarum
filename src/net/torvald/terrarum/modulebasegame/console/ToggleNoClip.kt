package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleAlias
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2016-01-19.
 */
@ConsoleAlias("nc,noclip")
internal object ToggleNoClip : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
        if (player == null) return


        val status = player.isNoClip

        player.isNoClip = !status
        Echo("Set no-clip status to " + (!status).toString())
    }

    override fun printUsage() {
        Echo("toggle no-clip status of player")
    }
}
