package net.torvald.terrarum.console

import net.torvald.terrarum.gameactors.PhysTestBall
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.worlddrawer.WorldCamera
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

            val ball = PhysTestBall(Terrarum.ingame!!.world)
            ball.setPosition(mouseX, mouseY)
            ball.elasticity = elasticity
            ball.applyForce(Vector2(xvel, yvel))

            Terrarum.ingame!!.addNewActor(ball)
        }
        else if (args.size == 2) {
            val elasticity = args[1].toDouble()

            val ball = PhysTestBall(Terrarum.ingame!!.world)
            ball.setPosition(mouseX, mouseY)
            ball.elasticity = elasticity

            Terrarum.ingame!!.addNewActor(ball)
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: spawnball elasticity [x velocity] [y velocity]")
    }
}
