package net.torvald.terrarum

import net.torvald.terrarum.gamecontroller.Key
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by SKYHi14 on 2017-04-22.
 */
class StateStutterTest : BasicGameState() {

    private val testImage = Image(4096, 1728)
    private val testImageG = testImage.graphics

    override fun init(container: GameContainer?, game: StateBasedGame?) {
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        Terrarum.appgc.setTitle("${Terrarum.NAME} — F: ${Terrarum.appgc.fps}")


        if (container.input.isKeyDown(Key.UP))
            dy -= moveDelta
        if (container.input.isKeyDown(Key.DOWN))
            dy += moveDelta
        if (container.input.isKeyDown(Key.LEFT))
            dx -= moveDelta
        if (container.input.isKeyDown(Key.RIGHT))
            dx += moveDelta
    }

    override fun getID() = Terrarum.STATE_ID_TEST_REFRESHRATE

    private var imageMade = false

    private var moveDelta = 3
    private var dx = 0
    private var dy = 0

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        if (!imageMade) {
            testImageG.font = Terrarum.fontGame
            testImageG.color = Color.white

            (0x3400..0x9FFF).forEach {
                testImageG.drawString(
                        "${it.toChar()}",
                        (it - 0x3400) % 256 * 16f,
                        (it - 0x3400) / 256 * 16f
                )
            }

            testImageG.flush()

            imageMade = true
        }

        g.translate(-dx.toFloat(), -dy.toFloat())
        g.drawImage(testImage, 0f, 0f)
    }
}