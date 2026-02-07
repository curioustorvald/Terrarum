package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleAlias
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.gameactors.ActorTestPlatform

/**
 * Created by minjaesong on 2026-02-08.
 */
@ConsoleAlias("spawnplatform")
internal object SpawnMovingPlatform : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val mouseX = Terrarum.mouseX
        val mouseY = Terrarum.mouseY

        val platform = ActorTestPlatform()
        // setPosition places bottom-centre at the given point; offset Y so the platform is centred at cursor
        platform.setPosition(mouseX, mouseY + platform.hitbox.height / 2.0)

        INGAME.queueActorAddition(platform)

        Echo("Spawned ActorTestPlatform at (${"%.1f".format(mouseX)}, ${"%.1f".format(mouseY)})")
    }

    override fun printUsage() {
        Echo("usage: spawnplatform")
        Echo("Spawns a test moving platform centred at the mouse cursor.")
    }
}
