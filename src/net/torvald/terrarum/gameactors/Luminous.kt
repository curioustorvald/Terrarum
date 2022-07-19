package net.torvald.terrarum.gameactors

import net.torvald.gdx.graphics.Cvec

/**
 * Lightbox is defined based on pixelwise position in the world!
 */
class Lightbox() {
    var hitbox: Hitbox = Hitbox(0.0,0.0,0.0,0.0)
    var light: Cvec = Cvec()

    constructor(hitbox: Hitbox, light: Cvec) : this() {
        this.hitbox = hitbox
        this.light = light
    }

    operator fun component1() = hitbox
    operator fun component2() = light
}

/**
 * For actors that either emits or blocks lights
 *
 * Created by minjaesong on 2016-02-19.
 */
interface Luminous {

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     *
     * NOTE: MUST NOT SERIALISE (use `@Transient`)
     */
    val lightBoxList: List<Lightbox>

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     *
     * NOTE: MUST NOT SERIALISE (use `@Transient`)
     */
    val shadeBoxList: List<Lightbox>
}