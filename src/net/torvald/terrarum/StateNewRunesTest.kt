package net.torvald.terrarum

import net.torvald.imagefont.NewRunes
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by SKYHi14 on 2017-03-24.
 */

class StateNewRunesTest : BasicGameState() {

    lateinit var runes: NewRunes

    override fun init(gc: GameContainer, game: StateBasedGame) {
        runes = NewRunes()
        runes.scale = 2
    }

    override fun update(gc: GameContainer, game: StateBasedGame, delta: Int) {

    }

    override fun render(gc: GameContainer, game: StateBasedGame, g: Graphics) {

        g.background = Color(0x282828)
        g.font = runes
        g.color = Color(0x00c0f3)

        val text = arrayOf(
                "ㅎㅏㅣㄹㅏㄹㅍㅏㄴㅌㅏㅈㅣ",
                "ㅈㅔㄹㄷㅏㅢㅈㅓㄴㅅㅓㄹ.", // <<insert troll_face here>>
                ".22884646ㄴㄱ."
        )

        text.forEachIndexed { index, s ->
            g.drawString(s, 30f, 30f + (runes.lineHeight * index))
        }
    }

    override fun getID(): Int = Terrarum.STATE_ID_TEST_FONT
}