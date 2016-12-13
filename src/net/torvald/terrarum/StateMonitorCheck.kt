package net.torvald.terrarum

import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.*
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.*

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

    override fun getID(): Int = Terrarum.STATE_ID_CONFIG_CALIBRATE

    class MonitorCheckUI : UICanvas {
        override var width = Terrarum.WIDTH
        override var height = Terrarum.HEIGHT
        override var openCloseTime = 150

        override var handler: UIHandler? = null

        private val backgroundCol = Color(0x404040)

        private val colourLUT = IntArray(32, { 255.times(it + 1).div(32) })

        val pictograms = ArrayList<Image>()
        lateinit var imageGallery: ItemImageGallery

        val instructionY = Terrarum.HEIGHT / 2//Terrarum.HEIGHT * 9 / 16
        val anykeyY = Terrarum.HEIGHT * 15 / 16

        val maru_alt = Regex("CN|JP|K[RP]|TW")

        init {
            if (Terrarum.gameLocale.length >= 4 && Terrarum.gameLocale.contains(maru_alt))
                pictograms.add(Image("./assets/graphics/gui/monitor_good_alt_maru.png"))
            else
                pictograms.add(Image("./assets/graphics/gui/monitor_good.png"))
            pictograms.add(Image("./assets/graphics/gui/monitor_bad.png"))

            imageGallery = ItemImageGallery(0, instructionY, Terrarum.WIDTH, anykeyY - instructionY, pictograms)
        }

        override fun update(gc: GameContainer, delta: Int) {
        }

        override fun render(gc: GameContainer, g: Graphics) {
            val titleY = Terrarum.HEIGHT * 7 / 16

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
                    g.color = backgroundCol
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
            g.color = backgroundCol
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

            // message text
            /*(1..12).forEach {
                Typography.printCentered(
                        g, Lang["MENU_MONITOR_CALI_LABEL_$it"],
                        instructionY + it.minus(2).times(g.font.lineHeight),
                        this
                )
            }*/

            // message pictogram
            imageGallery.render(gc, g)


            // anykey
            Typography.printCentered(
                    g, Lang["MENU_LABEL_PRESS_ANYKEY"],
                    anykeyY,
                    this
            )

        }

        override fun processInput(gc: GameContainer, delta: Int, input: Input) {
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