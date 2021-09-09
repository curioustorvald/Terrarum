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

    var width: Int = 0; private set
    var height: Int = 0; private set
    var wf: Float = 0f; private set
    var hf: Float = 0f; private set
    var halfw: Int = 0; private set
    var halfh: Int = 0; private set
    var halfwf: Float = 0f; private set
    var halfhf: Float = 0f; private set
    var aspectRatio: Float = 0f; private set



    val tvSafeGraphicsWidth: Int; get() = Math.round(width * TV_SAFE_GRAPHICS)
    val tvSafeGraphicsHeight: Int; get() = Math.round(height * TV_SAFE_GRAPHICS)
    val tvSafeActionWidth: Int; get() = Math.round(width * TV_SAFE_ACTION)
    val tvSafeActionHeight: Int; get() = Math.round(height * TV_SAFE_ACTION)

    init {
        setDimension(maxOf(minimumW, scrw), maxOf(minimumH, scrh))
    }

    fun setDimension(scrw: Int, scrh: Int) {
        width = scrw and 0x7FFFFFFE
        height = scrh and 0x7FFFFFFE
        wf = scrw.toFloat()
        hf = scrh.toFloat()
        halfw = width / 2
        halfh = height / 2
        halfwf = wf / 2f
        halfhf = hf / 2f
        aspectRatio = wf / hf
    }

}