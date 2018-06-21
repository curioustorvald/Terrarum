package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.gameactors.PhysTestLuarLander

/**
 * Created by minjaesong on 2018-01-18.
 */
internal object SpawnPhysTestLunarLander : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val mouseX = Terrarum.mouseX
        val mouseY = Terrarum.mouseY
        val lander = PhysTestLuarLander((Terrarum.ingame!! as Ingame).world)

        lander.setPosition(mouseX, mouseY)

        Terrarum.ingame!!.addNewActor(lander)
    }

    override fun printUsage() {
        Echo("control it with arrow keys")
    }
}