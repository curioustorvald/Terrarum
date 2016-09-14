package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.aa.ColouredFastFont
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

/**
 * Created by minjaesong on 16-09-12.
 */
class ColouredTextTerminal(override val width: Int, override val height: Int
) : SimpleTextTerminal(Color.white, width, height) {
    override val colours = arrayOf(
            Color(0x00, 0x00, 0x00), // black
            Color(0xff, 0xff, 0xff), // white
            Color(0x55, 0x55, 0x55), // dim grey
            Color(0xaa, 0xaa, 0xaa), // light grey

            Color(0xff, 0xff, 0x00), // yellow
            Color(0xff, 0x66, 0x00), // orange
            Color(0xdd, 0x00, 0x00), // red
            Color(0xff, 0x00, 0x99), // magenta

            Color(0x33, 0x00, 0x99), // purple
            Color(0x00, 0x00, 0xcc), // blue
            Color(0x00, 0x99, 0xff), // cyan
            Color(0x66, 0xff, 0x33), // lime

            Color(0x00, 0xaa, 0x00), // green
            Color(0x00, 0x66, 0x00), // dark green
            Color(0x66, 0x33, 0x00), // brown
            Color(0x99, 0x66, 0x33)  // tan
    )                                // THESE ARE THE STANDARD

    override val coloursCount = colours.size

    override val backDefault = 0
    override val foreDefault = 3

    override var backColour = backDefault
    override var foreColour = foreDefault

    override val fontRef = "./assets/graphics/fonts/CGA.png"
    override val fontImg = Image(fontRef)
    override val fontW = fontImg.width / 16
    override val fontH = fontImg.height / 16
    override val font = ColouredFastFont(this, fontRef, fontW, fontH)

    override val colourScreen = Color.black
}