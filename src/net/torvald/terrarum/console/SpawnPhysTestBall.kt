package net.torvald.terrarum.console

import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithSprite
import net.torvald.terrarum.gameactors.PhysTestBall
import net.torvald.terrarum.mapdrawer.TilesDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.mapdrawer.MapCamera
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 16-03-05.
 */
internal object SpawnPhysTestBall : ConsoleCommand {
    @Throws(Exception::class)
    override fun execute(args: Array<String>) {
        val mouseX = Terrarum.appgc.input.mouseX
        val mouseY = Terrarum.appgc.input.mouseY

        if (args.size >= 3) {
            val elasticity = args[1].toDouble()

            val xvel = args[2].toDouble()
            val yvel = if (args.size >= 4) args[3].toDouble() else 0.0

            val ball = PhysTestBall()
            ball.setPosition(
                    (mouseX + MapCamera.x).toDouble(),
                    (mouseY + MapCamera.y).toDouble()
            )
            ball.elasticity = elasticity
            ball.applyForce(Vector2(xvel, yvel))

            Terrarum.ingame.addNewActor(ball)
        }
        else if (args.size == 2) {
            val elasticity = args[1].toDouble()

            val ball = PhysTestBall()
            ball.setPosition(
                    (mouseX + MapCamera.x).toDouble(),
                    (mouseY + MapCamera.y).toDouble()
            )
            ball.elasticity = elasticity

            Terrarum.ingame.addNewActor(ball)
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: spawnball elasticity [x velocity] [y velocity]")
    }
}
