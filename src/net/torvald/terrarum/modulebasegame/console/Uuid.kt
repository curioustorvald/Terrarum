package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.ccG
import net.torvald.terrarum.ccY
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

/**
 * Created by minjaesong on 2023-08-22.
 */
object Uuid : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val worldUUID = INGAME.world.worldIndex
        val playerUUID = INGAME.actorGamer.uuid
        Echo("${ccY}World UUID: ${ccG}$worldUUID")
        Echo("${ccY}Player UUID: ${ccG}$playerUUID")
    }

    override fun printUsage() {
    }
}