package net.torvald.terrarum

open class BackgroundMusicPlayer {

    open fun update(ingameInstance: IngameInstance, delta: Float) {

    }

    protected var musicState = 0 // 0: disabled, 1: playing, 2: waiting
    protected var intermissionAkku = 0f
    protected var intermissionLength = 1f
    protected var musicFired = false


    open fun dispose() {

    }

}
