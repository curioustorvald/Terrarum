package net.torvald.terrarum

import net.torvald.terrarum.App.printdbg
import kotlin.math.roundToInt

class TerrarumScreenSize(scrw: Int = defaultW, scrh: Int = defaultH) {

    companion object {
        const val minimumW = 1080
        const val minimumH = 720
        const val defaultW = 1280
        const val defaultH = 720

        const val TV_SAFE_GRAPHICS = 0.05f // as per EBU recommendation (https://tech.ebu.ch/docs/r/r095.pdf)
        const val TV_SAFE_ACTION = 0.035f // as per EBU recommendation (https://tech.ebu.ch/docs/r/r095.pdf)
    }

    var width: Int = 0; private set
    var height: Int = 0; private set
    var wf: Float = 0f; private set
    var hf: Float = 0f; private set
    var halfw: Int = 0; private set
    var halfh: Int = 0; private set
    var halfwf: Float = 0f; private set
    var halfhf: Float = 0f; private set
    var aspectRatio: Float = 0f; private set
    var chatWidth: Int = 0; private set

    var magn: Float = 0f; private set // this value is stored here so that the initial instance would stay, forcing the players to require restart to apply the screen magnifying

    val tvSafeGraphicsWidth: Int; get() = Math.round(width * TV_SAFE_GRAPHICS)
    val tvSafeGraphicsHeight: Int; get() = Math.round(height * TV_SAFE_GRAPHICS)
    val tvSafeActionWidth: Int; get() = Math.round(width * TV_SAFE_ACTION)
    val tvSafeActionHeight: Int; get() = Math.round(height * TV_SAFE_ACTION)

    /** Apparent window size. `roundToEven(width * magn)` */
    var windowW: Int = 0; private set
    /** Apparent window size. `roundToEven(height * magn)` */
    var windowH: Int = 0; private set

    init {
        setDimension(maxOf(minimumW, scrw), maxOf(minimumH, scrh), App.getConfigDouble("screenmagnifying").toFloat())
    }

    fun setDimension(scrw: Int, scrh: Int, magn: Float,) {
        width = scrw and 0x7FFFFFFE
        height = scrh and 0x7FFFFFFE
        wf = scrw.toFloat()
        hf = scrh.toFloat()
        halfw = width / 2
        halfh = height / 2
        halfwf = wf / 2f
        halfhf = hf / 2f
        aspectRatio = wf / hf
        chatWidth = (width - (width * 0.84375).roundToInt()) and 0x7FFFFFFE

        this.magn = magn

        windowW = (scrw * magn).ceilInt() and 0x7FFFFFFE
        windowH = (scrh * magn).ceilInt() and 0x7FFFFFFE


        printdbg(this, "Window dim: $windowW x $windowH, called by:")
        printStackTrace(this)
    }

}