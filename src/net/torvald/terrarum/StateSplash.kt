package net.torvald.terrarum

import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gamecontroller.Key
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.DrawUtil
import net.torvald.terrarum.ui.ItemImageGallery
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
    var fadeTimer = -1

    var anykey_hit = false

    val backgroundColour = Color(0x303030)

    val deltathre = 500

    val auto_dismiss = 6500

    var opened = false

    var init = false

    lateinit var imageGallery: ItemImageGallery

    override fun init(container: GameContainer?, game: StateBasedGame?) {
        // pre-load lang
        Lang["MENU_LANGUAGE_THIS"]

        pictogramCollection.add(Image("./assets/graphics/gui/health_take_a_break.png"))
        pictogramCollection.add(Image("./assets/graphics/gui/health_distance.png"))

        fadeSheet = Image(Terrarum.WIDTH, Terrarum.HEIGHT)
        thisG = fadeSheet.graphics
        thisG.font = Terrarum.fontGame

        imageBoardHeight = Terrarum.HEIGHT - thisG.font.lineHeight.times(6)
        imageBoardOffset = thisG.font.lineHeight.times(3)

        imageGallery = ItemImageGallery(0, imageBoardOffset, Terrarum.WIDTH, imageBoardHeight, pictogramCollection)
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        // next splash or load next scene
        if (anykey_hit && opacity < 0.0001f) {
            game.enterState(Terrarum.STATE_ID_GAME)
        }

        // fade-in
        if (delta < deltathre) {
            init = true
            fadeTimer += delta

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
        if (opened && fadeTimer >= auto_dismiss) {
            doAnykeyThingy()
        }
    }

    override fun getID(): Int = Terrarum.STATE_ID_SPLASH

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        thisG.color = backgroundColour
        thisG.fillRect(0f, 0f, fadeSheet.width.toFloat(), fadeSheet.height.toFloat())

        thisG.color = Color.white

        Typography.printCentered(thisG, Lang["APP_WARNING_HEALTH_AND_SAFETY"],
                thisG.font.lineHeight * 2)

        Typography.printCentered(thisG, Lang["MENU_LABEL_PRESS_ANYKEY"],
                Terrarum.HEIGHT - thisG.font.lineHeight.times(3))

        imageGallery.render(container, thisG)

        g.drawImage(fadeSheet, 0f, 0f, Color(1f, 1f, 1f, opacity))
    }

    override fun keyPressed(key: Int, c: Char) {
        doAnykeyThingy()
    }

    override fun controllerButtonPressed(controller: Int, button: Int) {
        doAnykeyThingy()
    }

    private fun doAnykeyThingy() {
        if (!anykey_hit) {
            anykey_hit = true
            fadeTimer = 0
        }
    }
}