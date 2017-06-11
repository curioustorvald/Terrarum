package net.torvald.terrarum.virtualcomputer.peripheral

import net.torvald.terrarum.ModMgr
import org.luaj.vm2.Globals
import org.newdawn.slick.Graphics
import org.newdawn.slick.SpriteSheet
import org.newdawn.slick.SpriteSheetFont

/**
 * Created by minjaesong on 2017-05-31.
 */
class PeripheralCharLCD(val width: Int, val height: Int) : Peripheral("charLCD") {
    companion object {
        private val fontSheet = SpriteSheet(ModMgr.getPath("dwarventech", "mt-32.tga"), 16, 16)
        private val font = SpriteSheetFont(fontSheet, 0.toChar())
        private val fontW = fontSheet.width / fontSheet.horizontalCount
        private val fontH = fontSheet.height / fontSheet.verticalCount
    }

    override fun loadLib(globals: Globals) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toString(): String {
        return super.toString()
    }

    override val memSize = width * height

    var cursor: Int = 0 // character LCDs are mostly single long line wrapped

    val memory = ByteArray(memSize) // temporary; replace with proper VMPeripheralWrapper

    /**
     * @param g Frame Buffer that holds the display of LCD screen
     */
    fun render(g: Graphics) {
        g.font = PeripheralCharLCD.font

        memory.forEachIndexed { index, byte ->
            g.drawString("${byte.toChar()}", (index % width) * fontW.toFloat(), (index / width) * fontH.toFloat())
        }
    }
}