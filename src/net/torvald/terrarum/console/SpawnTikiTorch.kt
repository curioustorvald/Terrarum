package net.torvald.terrarum.console

import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.gameactors.FixtureTikiTorch

/**
 * Created by minjaesong on 2016-12-17.
 */
internal object SpawnTikiTorch : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val torch = FixtureTikiTorch()
        torch.setPosition(TerrarumGDX.mouseX, TerrarumGDX.mouseY)

        TerrarumGDX.ingame!!.addNewActor(torch)
    }

    override fun printUsage() {
        Echo("Usage: spawntorch")
    }
}