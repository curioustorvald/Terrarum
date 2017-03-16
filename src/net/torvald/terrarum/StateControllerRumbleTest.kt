package net.torvald.terrarum

import net.torvald.terrarum.gamecontroller.Key
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.fmod
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by SKYHi14 on 2017-03-15.
 */
class StateControllerRumbleTest : BasicGameState() {
    override fun init(container: GameContainer?, game: StateBasedGame?) {
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        Terrarum.appgc.setTitle("${GAME_NAME} — Do not pull out the controller!")

        KeyToggler.update(container.input)

        if (Terrarum.controller != null) {
            for (i in 0..minOf(rumblerCount - 1, 9)) {
                Terrarum.controller!!.setRumblerStrength(i, if (KeyToggler.isOn(2 + i)) 1f else 0f)
            }
        }
    }

    private var rumblerCount = Terrarum.controller?.rumblerCount ?: 0

    override fun getID() = Terrarum.STATE_ID_TOOL_RUMBLE_DIAGNOSIS

    override fun render(gc: GameContainer, game: StateBasedGame, g: Graphics) {
        g.font = Terrarum.fontGame
        g.color = Color.white

        if (Terrarum.controller != null) {
            g.drawString("Controller: ${Terrarum.controller!!.name}", 10f, 10f)
            g.drawString("Rumbler count: ${rumblerCount}", 10f, 30f)
            g.drawString("Rumblers", 10f, 70f)
            for (i in 0..minOf(rumblerCount - 1, 9)) {
                g.color = if (KeyToggler.isOn(2 + i)) Color(0x55ff55) else Color(0x808080)
                //g.drawString("$i", 10f + i * 16f, 90f)

                g.drawString("$i — ${Terrarum.controller!!.getRumblerName(i)}", 10f, 90f + 20 * i)
            }
        }
        else {
            g.drawString("Controller not found.", 10f, 10f)
        }
    }

}