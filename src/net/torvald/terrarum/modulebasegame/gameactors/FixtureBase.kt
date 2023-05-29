package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import java.util.*

typealias BlockBoxIndex = Int

interface Electric {
    val wireEmitterTypes: HashMap<String, BlockBoxIndex>
    val wireEmission: HashMap<BlockBoxIndex, Vector2>
    val wireConsumption: HashMap<BlockBoxIndex, Vector2>
}

/**
 * Protip: do not make child classes take any argument, especially no function (function "classes" have no zero-arg constructor)
 *
 * Initialising Fixture after deserialisation: override `reload()`
 *
 * Created by minjaesong on 2016-06-17.
 */
open class FixtureBase : ActorWithBody, CuedByTerrainChange {

    /** Real time, in nanoseconds */
    @Transient var spawnRequestedTime: Long = 0L
        protected set

    lateinit var blockBox: BlockBox // something like TapestryObject will want to redefine this
    fun blockBoxIndexToPoint2i(it: BlockBoxIndex): Point2i = this.blockBox.width.let { w -> Point2i(it % w, it / w) }
    var blockBoxProps: BlockBoxProps = BlockBoxProps(0)
    @Transient var nameFun: () -> String = { "" }
    @Transient var mainUI: UICanvas? = null
    var inventory: FixtureInventory? = null

    protected var actorThatInstalledThisFixture: UUID? = null

    private constructor() : super(RenderOrder.BEHIND, PhysProperties.IMMOBILE, null)


    /**
     * Making the sprite: do not address the CommonResourcePool directly; just do it like this snippet:
     *
     * ```makeNewSprite(FixtureBase.getSpritesheet("basegame", "sprites/fixtures/tiki_torch.tga", 16, 32))```
     */
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
        setHitboxDimension(TILE_SIZE * blockBox.width, TILE_SIZE * blockBox.height, 0, 0)
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
        protected set

    // something like TapestryObject will want to redefine this
    /**
     * @param action a function with following arguments: posX, posY, offX, offY
     */
    open fun forEachBlockbox(action: (Int, Int, Int, Int) -> Unit) {
        // TODO scale-aware
        worldBlockPos?.let { (posX, posY) ->
            for (y in posY until posY + blockBox.height) {
                for (x in posX until posX + blockBox.width) {
                    action(x, y, x - posX, y - posY)
                }
            }
        }
    }

    override fun updateForTerrainChange(cue: IngameInstance.BlockChangeQueueItem) {
        placeActorBlocks()
    }

    // something like TapestryObject will want to redefine this
    open protected fun placeActorBlocks() {
        forEachBlockbox { x, y, _, _ ->
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
     * Adds this instance of the fixture to the world. Physical dimension is derived from [blockBox].
     *
     * @param posX0 tile-wise bottem-centre position of the fixture (usually [Terrarum.mouseTileX])
     * @param posY0 tile-wise bottem-centre position of the fixture (usually [Terrarum.mouseTileY])
     * @return true if successfully spawned, false if was not (e.g. space to spawn is occupied by something else)
     */
    open fun spawn(posX0: Int, posY0: Int, installersUUID: UUID?): Boolean {
        // place filler blocks
        // place the filler blocks where:
        //     origin posX: centre-left  if mouseX is on the right-half of the game window,
        //                  centre-right otherwise
        //     origin posY: bottom
        // place the actor within the blockBox where:
        //     posX: centre of the blockBox
        //     posY: bottom of the blockBox
        // using the actor's hitbox


        val posX = (posX0 - blockBox.width.minus(1).div(2)) fmod world!!.width // width.minus(1) so that spawning position would be same as the ghost's position
        val posY = posY0 - blockBox.height + 1

        // set the position of this actor
        worldBlockPos = Point2i(posX, posY)

        // check for existing blocks (and fixtures)
        var hasCollision = false
        forEachBlockbox { x, y, _, _ ->
            if (!hasCollision) {
                val tile = world!!.getTileFromTerrain(x, y)
                if (BlockCodex[tile].isSolid || BlockCodex[tile].isActorBlock) {
                    hasCollision = true
                }
            }
        }
        if (hasCollision) {
            printdbg(this, "cannot spawn fixture ${nameFun()} at F${INGAME.WORLD_UPDATE_TIMER}, has tile collision; xy=($posX,$posY) tDim=(${blockBox.width},${blockBox.height})")
            return false
        }
        printdbg(this, "spawn fixture ${nameFun()} at F${INGAME.WORLD_UPDATE_TIMER}, xy=($posX,$posY) tDim=(${blockBox.width},${blockBox.height})")


        // fill the area with the filler blocks
        placeActorBlocks()


        this.isVisible = true
        this.hitbox.setFromWidthHeight(
                posX * TILE_SIZED,
                posY * TILE_SIZED,
                blockBox.width * TILE_SIZED,
                blockBox.height * TILE_SIZED
        )

        // actually add this actor into the world
        INGAME.queueActorAddition(this)
        spawnRequestedTime = System.nanoTime()

        actorThatInstalledThisFixture = installersUUID

        return true
    }

    /**
     * Identical to `spawn(Int, Int)` except it takes user-defined hitbox dimension instead of taking value from [blockBox].
     * Useful if [blockBox] cannot be determined on the time of the constructor call.
     *
     * @param posX tile-wise bottem-centre position of the fixture
     * @param posY tile-wise bottem-centre position of the fixture
     * @param thbw tile-wise Hitbox width
     * @param thbh tile-wise Hitbox height
     * @return true if successfully spawned, false if was not (e.g. space to spawn is occupied by something else)
     */
    open fun spawn(posX0: Int, posY0: Int, installersUUID: UUID?, thbw: Int, thbh: Int): Boolean {
        val posX = (posX0 - thbw.minus(1).div(2)) fmod world!!.width // width.minus(1) so that spawning position would be same as the ghost's position
        val posY = posY0 - thbh + 1

        // set the position of this actor
        worldBlockPos = Point2i(posX, posY)

        // define physical position
        this.hitbox.setFromWidthHeight(
                posX * TILE_SIZED,
                posY * TILE_SIZED,
                blockBox.width * TILE_SIZED,
                blockBox.height * TILE_SIZED
        )

        // check for existing blocks (and fixtures)
        var hasCollision = false
        forEachBlockbox { x, y, _, _ ->
            if (!hasCollision) {
                val tile = world!!.getTileFromTerrain(x, y)
                if (BlockCodex[tile].isSolid || BlockCodex[tile].isActorBlock) {
                    hasCollision = true
                }
            }
        }
        if (hasCollision) {
            printdbg(this, "cannot spawn fixture ${nameFun()} at F${INGAME.WORLD_UPDATE_TIMER}, has tile collision; xy=($posX,$posY) tDim=(${blockBox.width},${blockBox.height})")
            return false
        }
        printdbg(this, "spawn fixture ${nameFun()} at F${INGAME.WORLD_UPDATE_TIMER}, xy=($posX,$posY) tDim=(${blockBox.width},${blockBox.height})")


        // fill the area with the filler blocks
        placeActorBlocks()


        this.isVisible = true


        // actually add this actor into the world
        INGAME.queueActorAddition(this)
        spawnRequestedTime = System.nanoTime()

        actorThatInstalledThisFixture = installersUUID

        return true
    }

    /** force disable despawn when inventory is not empty */
    val canBeDespawned: Boolean get() = inventory?.isEmpty() ?: true

    /**
     * Removes this instance of the fixture from the world
     */
    open fun despawn() {

        if (canBeDespawned) {
            printdbg(this, "despawn at T${INGAME.WORLD_UPDATE_TIMER}: ${nameFun()}")
//            printStackTrace(this)

            // remove filler block
            forEachBlockbox { x, y, _, _ ->
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
        }
        else {
            printdbg(this, "failed to despawn at T${INGAME.WORLD_UPDATE_TIMER}: ${nameFun()}")
            printdbg(this, "cannot despawn a fixture with non-empty inventory")
        }
    }

    private fun dropSelfAsAnItem() {
        val drop = ItemCodex.fixtureToItemID(this)
        INGAME.queueActorAddition(DroppedItem(drop, hitbox.startX, hitbox.startY - 1.0))
    }

    protected var dropItem = false

    override fun update(delta: Float) {
        // FIXME retrieving fixture by mining relied on a quirk that mining a actorblock would also drop the fixture.
        // FIXME since that particular method of operation causes so much problems, it is required to implement the
        // FIXME said feature "correctly"
        /*if (!flagDespawn && worldBlockPos != null) {
            // removal-by-player because player is removing the filler block by pick
            // no-flagDespawn check is there to prevent item dropping when externally despawned
            // (e.g. picked the fixture up in which case the fixture must not drop itself to the world; it must go into the actor's inventory)
            forEachBlockbox { x, y, _, _ ->
                if (!BlockCodex[world!!.getTileFromTerrain(x, y)].isActorBlock) {
                    flagDespawn = true
                    dropItem = true
                }
            }
        }*/
        if (!canBeDespawned) flagDespawn = false // actively deny despawning request if cannot be despawned
        if (canBeDespawned && flagDespawn) despawn()
        if (canBeDespawned && dropItem) dropSelfAsAnItem()
        // actual actor removal is performed by the TerrarumIngame.killOrKnockdownActors
        super.update(delta)
    }

    /**
     * An alternative to `super.update()`
     */
    fun updateWithCustomActorBlockFun(delta: Float, actorBlockFillingFunction: () -> Unit) {
        if (!flagDespawn && worldBlockPos != null) {
            actorBlockFillingFunction()
        }
        if (!canBeDespawned) flagDespawn = false
        if (canBeDespawned && flagDespawn) despawn()
        if (canBeDespawned && dropItem) dropSelfAsAnItem()
        // actual actor removal is performed by the TerrarumIngame.killOrKnockdownActors
        super.update(delta)
    }

    override fun flagDespawn() {
        if (canBeDespawned) {
            printdbg(this, "Fixture at (${this.intTilewiseHitbox}) flagging despawn: ${this.javaClass.canonicalName}")
            flagDespawn = true
        }
        else {
            printdbg(this, "Fixture at (${this.intTilewiseHitbox}) CANNOT be despawned: ${this.javaClass.canonicalName}")
        }
    }

    /**
     * Also see: [net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase.Companion]
     */
    companion object {
        fun getSpritesheet(module: String, path: String, tileW: Int, tileH: Int): TextureRegionPack {
            val id = "$module/${path.replace('\\','/')}"
            return (CommonResourcePool.getOrPut(id) {
                TextureRegionPack(ModMgr.getGdxFile(module, path), tileW, tileH)
            } as TextureRegionPack)
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
 * - 0: fluid resist - when FALSE, the fixture will break itself to item/nothing.
 *       For example, crops has this flag FALSE.
 * - 1: don't drop item when broken - when TRUE, the fixture will simply disappear instead of
 *       dropping itself. For example, crop has this flag TRUE.
 *
 * (MSB)
 *
 * In the savegame's JSON, this flag set should be stored as signed integer.
 */
@JvmInline
value class BlockBoxProps(val flags: Int) {

}

/**
 * To not define the blockbox, simply use BlockBox.NULL
 *
 * Null blockbox means the fixture won't bar any block placement.
 * Fixtures like paintings will want to have such feature. (e.g. torch placed on top; buried)
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
