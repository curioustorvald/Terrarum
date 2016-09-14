package net.torvald.terrarum


import org.apache.commons.csv.CSVRecord
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by minjaesong on 16-09-05.
 */
class StateTestingSandbox : BasicGameState() {



    override fun init(container: GameContainer?, game: StateBasedGame?) {

    }

    override fun update(container: GameContainer?, game: StateBasedGame?, delta: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getID() = Terrarum.STATE_ID_TEST_SHIT

    override fun render(container: GameContainer?, game: StateBasedGame?, g: Graphics?) {
    }

    private fun intVal(rec: CSVRecord, s: String): Int {
        var ret = -1
        try {
            ret = Integer.decode(rec.get(s))!!
        }
        catch (e: NullPointerException) {
        }

        return ret
    }
}