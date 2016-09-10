package net.torvald.terrarum

import net.torvald.terrarum.gamecontroller.Key
import net.torvald.terrarum.virtualcomputers.terminal.SimpleTextTerminal
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.*

/**
 * ComputerCraft/OpenComputers like-alike, just for fun!
 *
 * Created by minjaesong on 16-09-07.
 */
class StateVTTest : BasicGameState() {

    val vt = SimpleTextTerminal(SimpleTextTerminal.ELECTRIC_BLUE, 80, 25)

    val vtUI = Image(vt.displayW, vt.displayH)

    override fun init(container: GameContainer, game: StateBasedGame) {

    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        Terrarum.appgc.setTitle("VT — F: ${container.fps}")
        vt.update(container, delta)


    }

    override fun getID() = Terrarum.STATE_ID_TEST_TTY

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        vt.render(container, vtUI.graphics)

        g.drawImage(vtUI,
                Terrarum.WIDTH.minus(vtUI.width).div(2f),
                Terrarum.HEIGHT.minus(vtUI.height).div(2f))

        //vtUI.graphics.flush()
    }

    override fun keyPressed(key: Int, c: Char) {
        super.keyPressed(key, c)

        if (key == Key.RETURN)
            vt.printChar(10.toChar())
        else
            vt.printChar(c)
    }
}