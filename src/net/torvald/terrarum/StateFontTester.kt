package net.torvald.terrarum

import net.torvald.imagefont.GameFontWhite
import net.torvald.terrarum.langpack.Lang
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

    override fun init(gc: GameContainer, game: StateBasedGame) {
        canvas = Graphics(1024, 1024)

        Terrarum.gameLocale = "fiFI"
    }

    override fun update(gc: GameContainer, game: StateBasedGame, delta: Int) {

    }

    override fun render(gc: GameContainer, game: StateBasedGame, g: Graphics) {
        g.font = Terrarum.fontGame

        val text = arrayOf(
                Lang["APP_WARNING_HEALTH_AND_SAFETY"],
                "",
                "90’ 10’ 20” 50 cm",
                "",
                "",
                Lang["MENU_LABEL_PRESS_ANYKEY_CONTINUE"],
                "DGB금융지주의 자회사. 대구광역시에서 쓰는 교통카드인 원패스와 탑패스 그리고 만악의 근원 대경교통카드를 판매 및 정산하고 있다. 본사는",
                "Atlantic Records, it features production from Nick Hexum of 311, Tony Kanal of No Doubt, and Sublime producer Paul Leary."
        )

        for (i in 0..text.size - 1) {
            g.drawString(text[i], 10f, 10f + (g.font.lineHeight * i))
        }

        g.font = Terrarum.fontSmallNumbers

        g.drawString("The true master needs but one channel", 0f, 64f)
        g.drawString("Press a key to start", 0f, 64f + 16f)
    }

    override fun getID(): Int = Terrarum.STATE_ID_TEST_FONT
}