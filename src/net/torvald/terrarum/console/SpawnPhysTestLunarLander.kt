package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.PhysTestLuarLander
import net.torvald.terrarum.worlddrawer.WorldCamera

/**
 * Created by minjaesong on 2018-01-18.
 */
internal object SpawnPhysTestLunarLander : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val mouseX = Terrarum.mouseX
        val mouseY = Terrarum.mouseY
        val lander = PhysTestLuarLander(Terrarum.ingame!!.world)

        lander.setPosition(mouseX, mouseY)

        Terrarum.ingame!!.addNewActor(lander)
    }

    override fun printUsage() {
        Echo("control it with arrow keys")
    }
}