package net.torvald.terrarum

import net.torvald.CSVFetcher
import net.torvald.colourutil.CIELabUtil.toXYZ
import net.torvald.colourutil.CIELabUtil.toLab
import net.torvald.colourutil.CIELabUtil.toRGB
import net.torvald.colourutil.CIELuv
import net.torvald.colourutil.CIELuvUtil.toRawRGB
import net.torvald.colourutil.CIELuvUtil.toLuv
import net.torvald.colourutil.RGB
import org.apache.commons.csv.CSVRecord
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by minjaesong on 16-09-05.
 */
class StateTestingSandbox : BasicGameState() {

    override fun init(container: GameContainer?, game: StateBasedGame?) {
        val records = CSVFetcher("./src/net/torvald/terrarum/tileproperties/tileprop_10bcol.csv")
        records.forEach {
            val tenOpacity = intVal(it, "opacity")
            val tenLum = intVal(it, "lumcolor")

            val eightOpacity = tenOpacity.and(0xff) or
                    tenOpacity.ushr(10).and(0xff).shl(8) or
                    tenOpacity.ushr(20).and(0xff).shl(16)
            val eightLum = tenLum.and(0xff) or
                    tenLum.ushr(10).and(0xff).shl(8) or
                    tenLum.ushr(20).and(0xff).shl(16)

            println("$eightOpacity\t$eightLum")
        }
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