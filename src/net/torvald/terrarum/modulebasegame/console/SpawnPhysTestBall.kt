package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.modulebasegame.gameactors.PhysTestBall
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2016-03-05.
 */
internal object SpawnPhysTestBall : ConsoleCommand {
    @Throws(Exception::class)
    override fun execute(args: Array<String>) {
        val mouseX = Terrarum.mouseX
        val mouseY = Terrarum.mouseY

        if (args.size >= 3) {
            val elasticity = args[1].toDouble()

            val xvel = args[2].toDouble()
            val yvel = if (args.size >= 4) args[3].toDouble() else 0.0

            val ball = PhysTestBall()
            ball.setPosition(mouseX, mouseY)
            ball.elasticity = elasticity
            ball.applyAcceleration(Vector2(xvel, yvel))

            INGAME.queueActorAddition(ball)
        }
        else if (args.size == 2) {
            val elasticity = args[1].toDouble()

            val ball = PhysTestBall()
            ball.setPosition(mouseX, mouseY)
            ball.elasticity = elasticity

            INGAME.queueActorAddition(ball)
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: spawnball elasticity [x velocity] [y velocity]")
    }
}
