package net.torvald.terrarum

open class MusicGovernor {

    open fun update(ingameInstance: IngameInstance, delta: Float) {

    }

    protected var state = 0 // 0: disabled, 1: playing, 2: waiting
    protected var intermissionAkku = 0f
    protected var intermissionLength = 1f
    protected var musicFired = false

    protected var fadeoutAkku = 0f
    protected var fadeoutLength = 0f
    protected var fadeoutFired = false
    protected var fadeinFired = false

    fun requestFadeOut(length: Float) {
        if (!fadeoutFired) {
            fadeoutLength = length
            fadeoutAkku = 0f
            fadeoutFired = true
        }
    }

    fun requestFadeIn(length: Float) {
        if (!fadeoutFired) {
            fadeoutLength = length
            fadeoutAkku = 0f
            fadeinFired = true
        }
    }
    open fun dispose() {

    }

}
