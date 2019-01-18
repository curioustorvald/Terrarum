package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.files.FileHandle
import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.gameworld.time_t


/**
 * Game player (YOU!)
 *
 * Created by minjaesong on 2015-12-31.
 */

class IngamePlayer(override var animDesc: FileHandle, born: time_t) : ActorHumanoid(born), HasAssembledSprite {

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