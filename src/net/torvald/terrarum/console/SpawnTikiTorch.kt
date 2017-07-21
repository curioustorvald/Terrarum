package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.FixtureTikiTorch

/**
 * Created by minjaesong on 2016-12-17.
 */
internal object SpawnTikiTorch : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val torch = FixtureTikiTorch(Terrarum.ingame!!.world)
        torch.setPosition(Terrarum.mouseX, Terrarum.mouseY)

        Terrarum.ingame!!.addNewActor(torch)
    }

    override fun printUsage() {
        Echo("Usage: spawntorch")
    }
}