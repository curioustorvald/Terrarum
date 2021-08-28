package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey


/**
 * Game player (YOU!)
 *
 * Created by minjaesong on 2015-12-31.
 */

class IngamePlayer : ActorHumanoid, HasAssembledSprite {

    override var animDescPath = "invalid"
    override var animDescPathGlow: String? = null

    private constructor()

    constructor(animDescPath: String, animDescPathGlow: String?, born: Long) : super(born) {
        this.animDescPath = animDescPath
        this.animDescPathGlow = animDescPathGlow
        actorValue[AVKey.__HISTORICAL_BORNTIME] = born
    }

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