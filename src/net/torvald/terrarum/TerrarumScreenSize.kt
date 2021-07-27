package net.torvald.terrarum

class TerrarumScreenSize(scrw: Int = defaultW, scrh: Int = defaultH) {

    companion object {
        const val minimumW = 1080
        const val minimumH = 720
        const val defaultW = 1280
        const val defaultH = 720

        const val TV_SAFE_GRAPHICS = 0.05f // as per EBU recommendation (https://tech.ebu.ch/docs/r/r095.pdf)
        const val TV_SAFE_ACTION = 0.035f // as per EBU recommendation (https://tech.ebu.ch/docs/r/r095.pdf)
    }

    var screenW: Int = 0; private set
    var screenH: Int = 0; private set
    var screenWf: Float = 0f; private set
    var screenHf: Float = 0f; private set
    var halfScreenW: Int = 0; private set
    var halfScreenH: Int = 0; private set
    var halfScreenWf: Float = 0f; private set
    var halfScreenHf: Float = 0f; private set
    var aspectRatio: Float = 0f; private set



    val tvSafeGraphicsWidth: Int; get() = Math.round(screenW * TV_SAFE_GRAPHICS)
    val tvSafeGraphicsHeight: Int; get() = Math.round(screenH * TV_SAFE_GRAPHICS)
    val tvSafeActionWidth: Int; get() = Math.round(screenW * TV_SAFE_ACTION)
    val tvSafeActionHeight: Int; get() = Math.round(screenH * TV_SAFE_ACTION)

    init {
        setDimension(maxOf(minimumW, scrw), maxOf(minimumH, scrh))
    }

    fun setDimension(scrw: Int, scrh: Int) {
        screenW = scrw and 0x7FFFFFFE
        screenH = scrh and 0x7FFFFFFE
        screenWf = scrw.toFloat()
        screenHf = scrh.toFloat()
        halfScreenW = screenW / 2
        halfScreenH = screenH / 2
        halfScreenWf = screenWf / 2f
        halfScreenHf = screenHf / 2f
        aspectRatio = screenWf / screenHf
    }

}