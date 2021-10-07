package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.spriteassembler.ADProperties
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import java.util.*


/**
 * Game player (YOU!)
 *
 * Created by minjaesong on 2015-12-31.
 */

class IngamePlayer : ActorHumanoid, HasAssembledSprite {

    var UUID = UUID(0L,0L); private set
    internal var worldCurrentlyPlaying: UUID = UUID(0L,0L) // only filled up on save and load; DO NOT USE THIS
    override var animDesc: ADProperties? = null
    override var animDescGlow: ADProperties? = null

    private constructor()

    constructor(animDescPath: String, animDescPathGlow: String?, born: Long) : super(born) {
        animDesc = ADProperties(Gdx.files.internal(animDescPath))
        if (animDescPathGlow != null) animDescGlow = ADProperties(Gdx.files.internal(animDescPathGlow))
        actorValue[AVKey.__HISTORICAL_BORNTIME] = born
    }

    /**
     * Creates new Player instance with empty elements (sprites, actorvalue, etc.).

     * **Use PlayerFactory to build player!**
     */
    init {
        referenceID = Terrarum.PLAYER_REF_ID // TODO assign random ID
        density = BASE_DENSITY
        collisionType = COLLISION_KINEMATIC
        worldCurrentlyPlaying = Terrarum.ingame?.world?.worldIndex ?: UUID(0L,0L)
    }

}