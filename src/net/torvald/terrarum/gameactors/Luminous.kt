package net.torvald.terrarum.gameactors

import net.torvald.gdx.graphics.Cvec

data class Lightbox(val hitbox: Hitbox, val getLight: () -> Cvec)

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
     */
    val lightBoxList: List<Lightbox>

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    val shadeBoxList: List<Lightbox>
}