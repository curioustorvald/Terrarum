package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.FixtureTikiTorch
import net.torvald.terrarum.gamecontroller.mouseX
import net.torvald.terrarum.gamecontroller.mouseY

/**
 * Created by minjaesong on 2016-12-17.
 */
object SpawnTikiTorch : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val torch = FixtureTikiTorch()
        torch.setPosition(Terrarum.appgc.mouseX, Terrarum.appgc.mouseY)

        Terrarum.ingame.addActor(torch)
    }

    override fun printUsage() {
        Echo("Usage: spawntorch")
    }
}