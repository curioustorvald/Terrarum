package net.torvald.terrarum.gameactors

import jdk.incubator.vector.FloatVector
import net.torvald.gdx.graphics.VectorArray

/**
 * Lightbox is defined based on pixelwise position in the world!
 */
class Lightbox() {
    var hitbox: Hitbox = Hitbox(0.0,0.0,0.0,0.0)
    var light: FloatVector = FloatVector.broadcast(VectorArray.SPECIES, 0f)

    constructor(hitbox: Hitbox, light: FloatVector) : this() {
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
 *
 * the interface Luminous is merged with the ActorWithBody -- minjaesong on 2022-09-11
 */
/*interface Luminous {

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
}*/