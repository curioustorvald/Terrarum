package net.torvald.terrarum

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * ComputerCraft/OpenComputers like-alike, just for fun!
 *
 * Created by minjaesong on 16-09-07.
 */
class StateVTTest : BasicGameState() {
    override fun init(container: GameContainer?, game: StateBasedGame?) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(container: GameContainer?, game: StateBasedGame?, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getID() = Terrarum.STATE_ID_TEST_TTY

    override fun render(container: GameContainer?, game: StateBasedGame?, g: Graphics?) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}