package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.gameactors.FixtureTikiTorch

/**
 * Created by minjaesong on 2016-12-17.
 */
internal object SpawnTikiTorch : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val torch = FixtureTikiTorch()
        torch.setPosition(Terrarum.mouseX, Terrarum.mouseY)

        Terrarum.ingame!!.addNewActor(torch)
    }

    override fun printUsage() {
        Echo("Usage: spawntorch")
    }
}