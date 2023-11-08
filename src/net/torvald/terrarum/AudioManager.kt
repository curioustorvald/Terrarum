package net.torvald.terrarum

/**
 * Created by minjaesong on 2023-11-07.
 */
object AudioManager {

    /** Returns a master volume */
    val masterVolume: Float
        get() = App.getConfigDouble("mastervolume").toFloat()

    /** Returns a (master volume * bgm volume) */
    val musicVolume: Float
        get() = (App.getConfigDouble("bgmvolume") * App.getConfigDouble("mastervolume")).toFloat()

    /** Returns a (master volume * sfx volume */
    val ambientVolume: Float
        get() = (App.getConfigDouble("sfxvolume") * App.getConfigDouble("mastervolume")).toFloat()

}