package net.torvald.terrarum.console

import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysTestBall
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-05.
 */
class SpawnPhysTestBall : ConsoleCommand {
    @Throws(Exception::class)
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val mouseX = Terrarum.appgc.input.mouseX
            val mouseY = Terrarum.appgc.input.mouseY

            val elasticity = args[1].toFloat()

            val ball = PhysTestBall()
            ball.setPosition(
                    (mouseX + MapCamera.cameraX).toFloat(),
                    (mouseY + MapCamera.cameraY).toFloat()
            )
            ball.elasticity = elasticity

            Terrarum.game.addActor(ball)
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo().execute("usage: spawnball [elasticity]")
    }
}
