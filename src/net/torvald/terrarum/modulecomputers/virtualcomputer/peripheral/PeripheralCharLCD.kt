package net.torvald.terrarum.modulecomputers.virtualcomputer.peripheral

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.ModMgr
import org.luaj.vm2.Globals

/**
 * Created by minjaesong on 2017-05-31.
 */
class PeripheralCharLCD(val width: Int, val height: Int) : net.torvald.terrarum.modulecomputers.virtualcomputer.peripheral.Peripheral("charLCD") {
    /*companion object {
        private val fontSheet = BitmapFont(ModMgr.getPath("dwarventech", "mt-32.tga"), 16, 16)
        private val font = BitmapFont(fontSheet, 0.toChar())
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
    fun render(batch: SpriteBatch) {
        memory.forEachIndexed { index, byte ->
            font.draw(batch, "${byte.toChar()}", (index % width) * fontW.toFloat(), (index / width) * fontH.toFloat())
        }
    }*/
    override fun loadLib(globals: Globals) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toString(): String {
        return super.toString()
    }

    override val memSize: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}