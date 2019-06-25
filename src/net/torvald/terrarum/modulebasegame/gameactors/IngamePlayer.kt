package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.terrarum.Terrarum


/**
 * Game player (YOU!)
 *
 * Created by minjaesong on 2015-12-31.
 */

class IngamePlayer(
        override var animDescPath: String,
        override var animDescPathGlow: String? = null,
        born: Long
) : ActorHumanoid(born), HasAssembledSprite {

    /**
     * Creates new Player instance with empty elements (sprites, actorvalue, etc.).

     * **Use PlayerFactory to build player!**
     */
    init {
        referenceID = Terrarum.PLAYER_REF_ID // forcibly set ID
        density = BASE_DENSITY
        collisionType = COLLISION_KINEMATIC
    }

}