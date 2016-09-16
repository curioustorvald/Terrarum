package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.aa.ColouredFastFont
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

/**
 * Created by minjaesong on 16-09-12.
 */
class ColouredTextTerminal(
        override val width: Int, override val height: Int
) : SimpleTextTerminal(Color.white, width, height) {
    override val colours = arrayOf(
            Color(0x00, 0x00, 0x00), // 0 black
            Color(0xff, 0xff, 0xff), // 1 white
            Color(0x55, 0x55, 0x55), // 2 dim grey
            Color(0xaa, 0xaa, 0xaa), // 3 light grey

            Color(0xff, 0xff, 0x00), // 4 yellow
            Color(0xff, 0x66, 0x00), // 5 orange
            Color(0xdd, 0x00, 0x00), // 6 red
            Color(0xff, 0x00, 0x99), // 7 magenta

            Color(0x33, 0x00, 0x99), // 8 purple
            Color(0x00, 0x00, 0xcc), // 9 blue
            Color(0x00, 0x99, 0xff), //10 cyan
            Color(0x66, 0xff, 0x33), //11 lime

            Color(0x00, 0xaa, 0x00), //12 green
            Color(0x00, 0x66, 0x00), //13 dark green
            Color(0x66, 0x33, 0x00), //14 brown
            Color(0x99, 0x66, 0x33)  //15 tan
    )                                // THESE ARE THE STANDARD

    override val coloursCount: Int
        get() = colours.size

    override val backDefault = 0
    override val foreDefault = 3

    override var backColour = backDefault
    override var foreColour = foreDefault

    override val fontRef = "./assets/graphics/fonts/cp949.png"
    override val fontImg = Image(fontRef)
    override val fontW = fontImg.width / 16
    override val fontH = fontImg.height / 16
    override val font = ColouredFastFont(this, fontRef, fontW, fontH)

    override val colourScreen: Color = Color.black
}