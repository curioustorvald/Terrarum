package net.torvald.terrarum

import net.torvald.imagefont.GameFontWhite
import org.newdawn.slick.Font
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by minjaesong on 16-06-28.
 */
class StateFontTester : BasicGameState() {
    val textToPrint = "Font printer 서체 인쇄기"

    lateinit var canvas: Graphics
    lateinit var gameFont: Font

    override fun init(gc: GameContainer, game: StateBasedGame) {
        canvas = Graphics(1024, 1024)

        gameFont = GameFontWhite()
    }

    override fun update(gc: GameContainer, game: StateBasedGame, delta: Int) {

    }

    override fun render(gc: GameContainer, game: StateBasedGame, g: Graphics) {
        g.font = gameFont
        g.drawString(textToPrint, 10f, 10f)
    }

    override fun getID(): Int = Terrarum.SCENE_ID_TEST_FONT
}