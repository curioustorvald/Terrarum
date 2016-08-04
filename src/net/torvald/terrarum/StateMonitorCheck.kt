package net.torvald.terrarum

import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Typography
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIHandler
import net.torvald.terrarum.ui.KeyboardControlled
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by minjaesong on 16-07-06.
 */
class StateMonitorCheck : BasicGameState() {
    private lateinit var uiMonitorCheck: UIHandler

    override fun init(gc: GameContainer, g: StateBasedGame) {
        uiMonitorCheck = UIHandler(MonitorCheckUI())
        uiMonitorCheck.isVisible = true
    }

    override fun update(gc: GameContainer, sbg: StateBasedGame, delta: Int) {
        uiMonitorCheck.update(gc, delta)
    }

    override fun render(gc: GameContainer, sbg: StateBasedGame, g: Graphics) {
        uiMonitorCheck.render(gc, sbg, g)
    }

    override fun keyPressed(key: Int, c: Char) {
        //uiMonitorCheck.setAsClose()
    }

    override fun getID(): Int = Terrarum.SCENE_ID_CONFIG_CALIBRATE

    class MonitorCheckUI : UICanvas {
        override var width = Terrarum.WIDTH
        override var height = Terrarum.HEIGHT
        override var openCloseTime = 150

        override var handler: UIHandler? = null

        private val colourLUT = arrayOf(
                0x08, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38, 0x40,
                0x48, 0x50, 0x58, 0x60, 0x68, 0x70, 0x78, 0x80,
                0x88, 0x90, 0x98, 0xA0, 0xA8, 0xB0, 0xB8, 0xC0,
                0xC8, 0xD0, 0xD8, 0xE0, 0xE8, 0xF0, 0xF8, 0xFF
        )

        override fun update(gc: GameContainer, delta: Int) {
        }

        override fun render(gc: GameContainer, g: Graphics) {
            val titleY = Terrarum.HEIGHT * 7 / 16
            val instructionY = Terrarum.HEIGHT * 9 / 16
            val anykeyY = Terrarum.HEIGHT * 15 / 16

            val barWidthAll = Terrarum.WIDTH.div(100).times(100) * 9 / 10
            val barWidth: Int = barWidthAll / 32 + 1
            val barHeight = 90

            val yCentre = Terrarum.HEIGHT.shr(1)

            val barNumberGap = 5

            g.background = Color.black
            // draw bars
            for (i in 0..31) {
                val labelW = g.font.getWidth(i.plus(1).toString())
                val labelH = g.font.lineHeight
                val barXstart = center(Terrarum.WIDTH, barWidthAll) + i.times(barWidth)
                val barYstart = center(yCentre, barHeight)

                // bar start point indicator
                if (i == 0) {
                    g.color = Color(0x404040)
                    g.drawLine(
                            barXstart.toFloat(), barYstart - barNumberGap - labelH.toFloat(),
                            barXstart.toFloat(), barYstart - barNumberGap.toFloat()
                    )
                }

                // bar numbers
                if (i.plus(1) and 0x1 == 0 || i.plus(1) == 1) {
                    g.color = Color.white
                    g.drawString(
                            i.plus(1).toString(),
                            barXstart + center(barWidth, labelW).toFloat(),
                            barYstart - barNumberGap - labelH.toFloat()
                    )
                }

                // actual bar
                g.color = Color(colourLUT[i], colourLUT[i], colourLUT[i])
                g.fillRect(
                        barXstart.toFloat(),
                        barYstart.toFloat(),
                        barWidth.toFloat(),
                        barHeight.toFloat()
                )
            }

            // messages background
            g.color = Color(0x404040)
            g.fillRect(
                    0f, Terrarum.HEIGHT.shr(1).toFloat(),
                    Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.shr(1).plus(1).toFloat()
            )

            // labels
            g.color = Color.white
            Typography.printCentered(
                    g, Lang["MENU_MONITOR_CALI_TITLE"],
                    titleY,
                    this
            )

            (1..12).forEach {
                Typography.printCentered(
                        g, Lang["MENU_MONITOR_CALI_LABEL_$it"],
                        instructionY + it.minus(2).times(g.font.lineHeight),
                        this
                )
            }

            Typography.printCentered(
                    g, Lang["MENU_LABEL_PRESS_ANYKEY_CONTINUE"],
                    anykeyY,
                    this
            )

        }

        override fun processInput(input: Input) {
        }

        override fun doOpening(gc: GameContainer, delta: Int) {
        }

        override fun doClosing(gc: GameContainer, delta: Int) {
        }

        override fun endOpening(gc: GameContainer, delta: Int) {
        }

        override fun endClosing(gc: GameContainer, delta: Int) {
        }

        private fun center(x1: Int, x2: Int) = x1.minus(x2).div(2)
    }
}