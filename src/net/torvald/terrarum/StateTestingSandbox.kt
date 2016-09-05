package net.torvald.terrarum

import net.torvald.colourutil.CIELabUtil.toXYZ
import net.torvald.colourutil.CIELabUtil.toLab
import net.torvald.colourutil.CIELabUtil.toRGB
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by minjaesong on 16-09-05.
 */
class StateTestingSandbox : BasicGameState() {
    val colRGB = Color(0x51621D)
    val colAfter1 = colRGB.toXYZ().toRGB()
    //val colAfter2 = colRGB.toXYZ().toLab().toXYZ().toRGB()

    override fun init(container: GameContainer?, game: StateBasedGame?) {
        println("Color:\n$colRGB")
        println("Color -> XYZ -> Color:\n$colAfter1")
        //println("Color -> XYZ -> Lab -> XYZ -> Color:\n$colAfter2")
    }

    override fun update(container: GameContainer?, game: StateBasedGame?, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getID() = Terrarum.STATE_ID_TEST_SHIT

    override fun render(container: GameContainer?, game: StateBasedGame?, g: Graphics?) {
    }
}