package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.IngameInstance
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2016-06-17.
 */
open class FixtureBase(val blockBox: BlockBox, val blockBoxProps: BlockBoxProps = BlockBoxProps(0)) :
// disabling physics (not allowing the fixture to move) WILL make things easier in many ways
        ActorWBMovable(RenderOrder.BEHIND, immobileBody = true, usePhysics = false), CuedByTerrainChange {

    private val world: GameWorld
        get() = Terrarum.ingame!!.world

    private val TSIZE = TILE_SIZE.toDouble()

    /**
     * Block-wise position of this fixture when it's placed on the world. Null if it's not on the world
     */
    private var worldBlockPos: Point2i? = null

    /**
     * Adds this instance of the fixture to the world
     *
     * @param posX top-left position of the fixture, tile-wise
     * @param posY top-left position of the fixture, tile-wise
     */
    open fun spawn(posX: Int, posY: Int) {
        // place filler blocks
        // place the filler blocks where:
        //     origin posX: centre-left  if mouseX is on the right-half of the game window,
        //                  centre-right otherwise
        //     origin posY: bottom
        // place the actor within the blockBox where:
        //     posX: centre of the blockBox
        //     posY: bottom of the blockBox
        // using the actor's hitbox

        for (x in posX until posX + blockBox.width) {
            for (y in posY until posY + blockBox.height) {
                world.setTileTerrain(x, y, blockBox.collisionType)
            }
        }

        worldBlockPos = Point2i(posX, posY)

        this.isVisible = true
        this.hitbox.setFromWidthHeight(posX * TSIZE, posY * TSIZE, blockBox.width * TSIZE, blockBox.height * TSIZE)

    }

    /**
     * Update code that runs once for every frame
     */
    open fun updateSelf() {

    }

    /**
     * Removes this instance of the fixture from the world
     */
    open fun despawn() {
        val posX = worldBlockPos!!.x
        val posY = worldBlockPos!!.y

        // remove filler block
        for (x in posX until posX + blockBox.width) {
            for (y in posY until posY + blockBox.height) {
                world.setTileTerrain(x, y, Block.AIR)
            }
        }

        worldBlockPos = null

        this.isVisible = false
    }

    /**
     * Fired by world's BlockChanged event (fired when blocks are placed/removed).
     * The flooding check must run on every frame. use updateSelf() for that.
     *
     * E.g. if a fixture block that is inside of BlockBox is missing, destroy and drop self.
     */
    override fun updateForWorldChange(cue: IngameInstance.BlockChangeQueueItem) {
        // check for marker blocks.
        // if at least one of them is missing, destroy all the markers and drop self as an item

        // you need to implement Dropped Item first to satisfyingly implement this function

        val posX = worldBlockPos!!.x
        val posY = worldBlockPos!!.y
        var dropThis = false

        // remove filler block
        outerLoop@
        for (x in posX until posX + blockBox.width) {
            for (y in posY until posY + blockBox.height) {
                if (world.getTileFromTerrain(x, y) != blockBox.collisionType) {
                    dropThis = true
                    break@outerLoop
                }
            }
        }

        if (dropThis) {
            // fill blockbox with air
            for (x in posX until posX + blockBox.width) {
                for (y in posY until posY + blockBox.height) {
                    if (world.getTileFromTerrain(x, y) == blockBox.collisionType) {
                        world.setTileTerrain(x, y, Block.AIR)
                    }
                }
            }

            // TODO drop self as an item (instance of DroppedItem)
            
        }
    }
}

interface CuedByTerrainChange {
    /**
     * Fired by world's BlockChanged event (fired when blocks are placed/removed).
     * The flooding check must run on every frame. use updateSelf() for that.
     *
     * E.g. if a fixture block that is inside of BlockBox is missing, destroy and drop self.
     */
    fun updateForWorldChange(cue: IngameInstance.BlockChangeQueueItem)
}

/**
 * Standard 32-bit binary flags.
 *
 * (LSB)
 * - 0: fluid resist - when FALSE, the fixture will break itself to item/nothing. For example, crops has this flag FALSE.
 * - 1: don't drop item when broken - when TRUE, the fixture will simply disappear instead of dropping itself. For example, crop has this flag TRUE.
 *
 * (MSB)
 *
 * In the savegame's JSON, this flag set should be stored as signed integer.
 */
inline class BlockBoxProps(val flags: Int) {

}

/**
 * @param collisionType Collision type defined in BlockBox.Companion
 * @param width Width of the block box, tile-wise
 * @param height Height of the block box, tile-wise
 */
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