package net.torvald.terrarum.gameactors

/**
 * Created by minjaesong on 16-04-26.
 */
class WeaponSwung(val itemID: Int) : ActorWithSprite(Actor.RenderOrder.MIDTOP), Luminous {
    // just let the solver use AABB; it's cheap but works just enough

    /**
     * Recommended implementation:
     *
    override var luminosity: Int
    get() = actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
    set(value) {
    actorValue[AVKey.LUMINOSITY] = value
    }
     */
    override var luminosity: Int
        get() = throw UnsupportedOperationException()
        set(value) {
        }
    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    override val lightBoxList: List<Hitbox>
        get() = throw UnsupportedOperationException()

    init {

    }
}