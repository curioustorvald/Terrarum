package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.ui.UICanvas
import org.dyn4j.geometry.Vector2

typealias BlockBoxIndex = Int

interface Electric {
    val wireEmitterTypes: HashMap<String, BlockBoxIndex>
    val wireEmission: HashMap<BlockBoxIndex, Vector2>
    val wireConsumption: HashMap<BlockBoxIndex, Vector2>
}

/**
 * Protip: do not make child classes take any argument, especially no function (function "classes" have no zero-arg constructor)
 *
 * Created by minjaesong on 2016-06-17.
 */
open class FixtureBase : ActorWithBody, CuedByTerrainChange {

    lateinit var blockBox: BlockBox // something like TapestryObject will want to redefine this
    fun blockBoxIndexToPoint2i(it: BlockBoxIndex): Point2i = this.blockBox.width.let { w -> Point2i(it % w, it / w) }
    var blockBoxProps: BlockBoxProps = BlockBoxProps(0)
    @Transient var nameFun: () -> String = { "" }
    @Transient var mainUI: UICanvas? = null
    var inventory: FixtureInventory? = null

    protected constructor() : super(RenderOrder.BEHIND, PhysProperties.IMMOBILE, null)


    constructor(
                blockBox0: BlockBox,
                blockBoxProps: BlockBoxProps = BlockBoxProps(0),
                renderOrder: RenderOrder = RenderOrder.MIDDLE,
                nameFun: () -> String,
                mainUI: UICanvas? = null,
                inventory: FixtureInventory? = null,
                id: ActorID? = null
    ) : super(renderOrder, PhysProperties.IMMOBILE, id) {
        blockBox = blockBox0
        this.blockBoxProps = blockBoxProps
        this.renderOrder = renderOrder
        this.nameFun = nameFun
        this.mainUI = mainUI
        this.inventory = inventory

        if (mainUI != null)
            App.disposables.add(mainUI)
    }


    /**
     * Tile-wise position of this fixture when it's placed on the world, top-left origin. Null if it's not on the world
     */
    var worldBlockPos: Point2i? = null
        private set

    fun forEachBlockbox(action: (Int, Int) -> Unit) {
        worldBlockPos?.let { (posX, posY) ->
            for (y in posY until posY + blockBox.height) {
                for (x in posX until posX + blockBox.width) {
                    action(x, y)
                }
            }
        }
    }

    override fun updateForTerrainChange(cue: IngameInstance.BlockChangeQueueItem) {
        fillFillerBlock()
    }

    private fun fillFillerBlock() {
        forEachBlockbox { x, y ->
            //printdbg(this, "fillerblock ${blockBox.collisionType} at ($x, $y)")
            if (blockBox.collisionType == BlockBox.ALLOW_MOVE_DOWN) {
                // if the collision type is allow_move_down, only the top surface tile should be "the platform"
                // lower part must not have such property (think of the table!)
                // TODO does this ACTUALLY work ?!
                world!!.setTileTerrain(x, y, if (y == worldBlockPos!!.y) BlockBox.ALLOW_MOVE_DOWN else BlockBox.NO_COLLISION, true)
            }
            else
                world!!.setTileTerrain(x, y, blockBox.collisionType, true)
        }
    }

    /**
     * Adds this instance of the fixture to the world
     *
     * @param posX tile-wise top-left position of the fixture
     * @param posY tile-wise top-left position of the fixture
     * @return true if successfully spawned, false if was not (e.g. occupied space)
     */
    open fun spawn(posX: Int, posY: Int): Boolean {
        // place filler blocks
        // place the filler blocks where:
        //     origin posX: centre-left  if mouseX is on the right-half of the game window,
        //                  centre-right otherwise
        //     origin posY: bottom
        // place the actor within the blockBox where:
        //     posX: centre of the blockBox
        //     posY: bottom of the blockBox
        // using the actor's hitbox


        // wrap x-position
        val posX = posX fmod world!!.width


        // check for existing blocks (and fixtures)
        var hasCollision = false
        checkForCollision@
        for (y in posY until posY + blockBox.height) {
            for (x in posX until posX + blockBox.width) {
                val tile = world!!.getTileFromTerrain(x, y)
                if (BlockCodex[tile].isSolid || tile in Block.actorblocks) {
                    hasCollision = true
                    break@checkForCollision
                }
            }
        }

        if (hasCollision) return false

        printdbg(this, "spawn ${nameFun()}")

        // set the position of this actor
        worldBlockPos = Point2i(posX, posY)

        // fill the area with the filler blocks
        fillFillerBlock()


        this.isVisible = true
        this.hitbox.setFromWidthHeight(posX * TILE_SIZED, posY * TILE_SIZED, blockBox.width * TILE_SIZED, blockBox.height * TILE_SIZED)

        // actually add this actor into the world
        INGAME.queueActorAddition(this)


        return true

    }

    val canBeDespawned: Boolean get() = inventory?.isEmpty() ?: true

    /**
     * Removes this instance of the fixture from the world
     */
    open fun despawn() {
        if (canBeDespawned) {
            printdbg(this, "despawn ${nameFun()}")

            // remove filler block
            forEachBlockbox { x, y ->
                world!!.setTileTerrain(x, y, Block.AIR, true)
            }

            worldBlockPos = null
            mainUI?.dispose()

            this.isVisible = false

            if (this is Electric) {
                wireEmitterTypes.clear()
                wireEmission.clear()
                wireConsumption.clear()
            }

            val drop = ItemCodex.fixtureToItemID(this)
            INGAME.queueActorAddition(DroppedItem(drop, hitbox.startX, hitbox.startY - 1.0))
        }
        else {
            printdbg(this, "cannot despawn a fixture with non-empty inventory")
        }
    }
    override fun update(delta: Float) {
        // if not flagged to despawn and not actually despawned (which sets worldBlockPos as null), always fill up fillerBlock
        if (!flagDespawn && worldBlockPos != null) {
            // for removal-by-player because player is removing the filler block by pick
            forEachBlockbox { x, y ->
                if (world!!.getTileFromTerrain(x, y) != blockBox.collisionType) {
                    flagDespawn = true
                }
            }

        }
        if (!canBeDespawned) flagDespawn = false
        else if (flagDespawn) despawn()
        // actual actor removal is performed by the TerrarumIngame.killOrKnockdownActors
        super.update(delta)
    }

    override fun flagDespawn() {
        if (canBeDespawned) flagDespawn = true
    }
}

interface CuedByTerrainChange {
    /**
     * Fired by world's BlockChanged event (fired when blocks are placed/removed).
     * The flooding check must run on every frame. use updateSelf() for that.
     *
     * E.g. if a fixture block that is inside of BlockBox is missing, destroy and drop self.
     */
    fun updateForTerrainChange(cue: IngameInstance.BlockChangeQueueItem)
}

interface CuedByWallChange {
    fun updateForWallChange(cue: IngameInstance.BlockChangeQueueItem)
}

interface CuedByWireChange {
    fun updateForWireChange(cue: IngameInstance.BlockChangeQueueItem)
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
 * To not define the blockbox, simply use BlockBox.NULL
 *
 * Null blockbox means the fixture won't bar any block placement. Fixtures like paintings will want to have such feature. (e.g. torch placed on top; buried)
 *
 * @param collisionType Collision type defined in BlockBox.Companion
 * @param width Width of the block box, tile-wise
 * @param height Height of the block box, tile-wise
 */
data class BlockBox(
        val collisionType: ItemID = NO_COLLISION,
        val width: Int = 0,
        val height: Int = 0) {

    /*fun redefine(collisionType: Int, width: Int, height: Int) {
        redefine(collisionType)
        redefine(width, height)
    }

    fun redefine(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun redefine(collisionType: Int) {
        this.collisionType = collisionType
    }*/

    companion object {
        const val NO_COLLISION = Block.ACTORBLOCK_NO_COLLISION
        const val FULL_COLLISION = Block.ACTORBLOCK_FULL_COLLISION
        const val ALLOW_MOVE_DOWN = Block.ACTORBLOCK_ALLOW_MOVE_DOWN
        const val NO_PASS_RIGHT = Block.ACTORBLOCK_NO_PASS_RIGHT
        const val NO_PASS_LEFT = Block.ACTORBLOCK_NO_PASS_LEFT

        val NULL = BlockBox()
    }
}