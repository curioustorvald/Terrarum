package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.Ingame

/**
 * Created by minjaesong on 2016-01-19.
 */
internal object ToggleNoClip : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val status = (Terrarum.ingame!! as Ingame).playableActor.isNoClip

        (Terrarum.ingame!! as Ingame).playableActor.isNoClip = !status
        Echo("Set no-clip status to " + (!status).toString())
    }

    override fun printUsage() {
        Echo("toggle no-clip status of player")
    }
}
