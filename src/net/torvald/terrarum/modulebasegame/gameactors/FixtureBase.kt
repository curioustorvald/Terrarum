package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.Point2d
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.ActorWBMovable

/**
 * Created by minjaesong on 2016-06-17.
 */
open class FixtureBase(val blockBox: BlockBox) :
// disabling physics (not allowing the fixture to move) WILL make things easier
        ActorWBMovable(RenderOrder.BEHIND, immobileBody = true, usePhysics = false) {

    /**
     * Block-wise position of this fixture when it's placed on the world. Null if it's not on the world
     */
    private var worldBlockPos: Point2d? = null

    /**
     * Adds this instance of the fixture to the world
     */
    open fun spawn() {
        // place filler blocks
        // place the filler blocks where:
        //     origin posX: centre-left  if mouseX is on the right-half of the game window,
        //                  centre-right otherwise
        //     origin posY: bottom
        // place the actor within the blockBox where:
        //     posX: centre of the blockBox
        //     posY: bottom of the blockBox
        // using the actor's hitbox



    }

    /**
     * Removes this instance of the fixture from the world
     */
    open fun despawn() {
        // remove filler block
    }
}

data class BlockBox(var collisionType: Int, var width: Int, var height: Int) {

    fun redefine(collisionType: Int, width: Int, height: Int) {
        redefine(collisionType)
        redefine(width, height)
    }

    fun redefine(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun redefine(collisionType: Int) {
        this.collisionType = collisionType
    }

    companion object {
        const val NO_COLLISION = Block.ACTORBLOCK_NO_COLLISION
        const val FULL_COLLISION = Block.ACTORBLOCK_FULL_COLLISION
        const val ALLOW_MOVE_DOWN = Block.ACTORBLOCK_ALLOW_MOVE_DOWN
        const val NO_PASS_RIGHT = Block.ACTORBLOCK_NO_PASS_RIGHT
        const val NO_PASS_LEFT = Block.ACTORBLOCK_NO_PASS_LEFT
    }
}