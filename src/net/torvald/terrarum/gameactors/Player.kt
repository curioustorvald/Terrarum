package net.torvald.terrarum.gameactors

import net.torvald.terrarum.ui.UIQuickBar
import org.newdawn.slick.GameContainer

/**
 * Game player (YOU!)
 *
 * Created by minjaesong on 15-12-31.
 */

class Player(born: GameDate) : ActorHumanoid(born) {

    internal val quickBarRegistration = IntArray(UIQuickBar.SLOT_COUNT, { -1 })

    companion object {
        @Transient const val PLAYER_REF_ID: Int = 0x91A7E2
    }

    /**
     * Creates new Player instance with empty elements (sprites, actorvalue, etc.).

     * **Use PlayerFactory to build player!**

     * @throws SlickException
     */
    init {
        referenceID = PLAYER_REF_ID // forcibly set ID
        density = BASE_DENSITY
        collisionType = COLLISION_KINEMATIC
    }

    override fun update(gc: GameContainer, delta: Int) {
        super.update(gc, delta)
    }

}