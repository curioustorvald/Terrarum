package net.torvald.terrarum

import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gamecontroller.Key
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.DrawUtil
import net.torvald.terrarum.ui.Typography
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.*

/**
 * Created by minjaesong on 16-08-04.
 */
class StateSplash : BasicGameState() {

    val pictogramCollection = ArrayList<Image>()

    val virtualImageHeight = 100
    var imageBoardHeight = 0
    var imageBoardOffset = 0

    lateinit var fadeSheet: Image
    lateinit var thisG: Graphics

    var opacity = 0f

    val fadeTime = 500
    var fadeTimer = 0

    var anykey_hit = false

    val backgroundColour = Color(0x303030)

    var delta = 0
    val deltathre = 500

    val auto_dismiss = 5000

    var opened = false

    override fun init(container: GameContainer?, game: StateBasedGame?) {
        // pre-load lang
        Lang["MENU_LANGUAGE_THIS"]

        pictogramCollection.add(Image("./assets/graphics/gui/health_take_a_break.png"))
        pictogramCollection.add(Image("./assets/graphics/gui/health_distance.png"))

        fadeSheet = Image(Terrarum.WIDTH, Terrarum.HEIGHT)
        thisG = fadeSheet.graphics
        thisG.font = Terrarum.gameFont
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        this.delta = delta

        // next splash or load next scene
        if (anykey_hit && opacity == 0f) {
            System.exit(0)
        }

        // fade-in
        if (delta < deltathre) {
            if (opacity < 1f && !anykey_hit) {
                opacity = FastMath.interpolateLinear(
                        fadeTimer.toFloat() / fadeTime, 0f, 1f
                )
            }
            else if (opacity > 0f && anykey_hit) {
                opacity = FastMath.interpolateLinear(
                        fadeTimer.toFloat() / fadeTime, 1f, 0f
                )
            }

            if (!opened && fadeTimer >= fadeTime && !anykey_hit) {
                fadeTimer = 0
                opened = true
            }
        }

        // auto dismiss
        if (opened && fadeTimer >= auto_dismiss)
            doAnykeyThingy()

        fadeTimer += delta
    }

    override fun getID(): Int = Terrarum.SCENE_ID_SPLASH

    override fun render(container: GameContainer?, game: StateBasedGame?, g: Graphics) {

        imageBoardHeight = Terrarum.HEIGHT - thisG.font.lineHeight.times(6)
        imageBoardOffset = thisG.font.lineHeight.times(3)

        thisG.color = backgroundColour
        thisG.fillRect(0f, 0f, fadeSheet.width.toFloat(), fadeSheet.height.toFloat())

        thisG.color = Color.white

        Typography.printCentered(thisG, Lang["APP_WARNING_HEALTH_AND_SAFETY"],
                thisG.font.lineHeight * 2)

        Typography.printCentered(thisG, Lang["MENU_LABEL_PRESS_ANYKEY_CONTINUE"],
                Terrarum.HEIGHT - thisG.font.lineHeight.times(3))

        pictogramCollection.forEachIndexed { i, image ->
            DrawUtil.drawCentered(thisG, image, knowYourPlace(i) + imageBoardOffset)
        }

        g.drawImage(fadeSheet, 0f, 0f, Color(1f, 1f, 1f, opacity))
    }

    private fun knowYourPlace(i: Int): Int {
        val gutter = (imageBoardHeight - virtualImageHeight.times(pictogramCollection.size)).toFloat().div(
                pictogramCollection.size + 1f
        )
        return (gutter * i.plus(1) + virtualImageHeight * i).roundInt()
    }

    override fun keyPressed(key: Int, c: Char) {
        doAnykeyThingy()
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
        doAnykeyThingy()
    }

    private fun doAnykeyThingy() {
        if (delta < deltathre && !anykey_hit) {
            anykey_hit = true
            fadeTimer = 0
        }
    }
}