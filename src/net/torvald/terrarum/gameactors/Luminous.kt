package net.torvald.terrarum.gameactors

/**
 * Created by minjaesong on 16-03-14.
 */
interface Luminous {

    /**
     * Recommended implementation:
     *
     override var luminosity: Char
        get() = if (actorValue.get("luminosity") != null) {
            actorValue.get("luminosity") as Char
        }
        else {
            0 as Char
        }
        set(value) {
            actorValue.set("luminosity", value)
        }
     */
    var luminosity: Int

}