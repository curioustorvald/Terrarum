package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt

typealias BlockBoxIndex = Int
typealias WireEmissionType = String

open class Electric : FixtureBase {

    protected constructor() : super() {
        oldSinkStatus = Array(blockBox.width * blockBox.height) { Vector2() }
    }

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

        oldSinkStatus = Array(blockBox.width * blockBox.height) { Vector2() }
    }

    companion object {
        const val ELECTIC_THRESHOLD_HIGH = 0.9
        const val ELECTRIC_THRESHOLD_LOW = 0.1
        const val ELECTRIC_THRESHOLD_EDGE_DELTA = 0.7
    }

    fun getWireEmitterAt(point: Point2i) = this.wireEmitterTypes[pointToBlockBoxIndex(point)]
    fun getWireEmitterAt(x: Int, y: Int) = this.wireEmitterTypes[pointToBlockBoxIndex(x, y)]
    fun getWireSinkAt(point: Point2i) = this.wireSinkTypes[pointToBlockBoxIndex(point)]
    fun getWireSinkAt(x: Int, y: Int) = this.wireSinkTypes[pointToBlockBoxIndex(x, y)]

    fun setWireEmitterAt(x: Int, y: Int, type: WireEmissionType) { wireEmitterTypes[pointToBlockBoxIndex(x, y)] = type }
    fun setWireSinkAt(x: Int, y: Int, type: WireEmissionType) { wireSinkTypes[pointToBlockBoxIndex(x, y)] = type }
    fun setWireEmissionAt(x: Int, y: Int, emission: Vector2) { wireEmission[pointToBlockBoxIndex(x, y)] = emission }
    fun setWireConsumptionAt(x: Int, y: Int, consumption: Vector2) { wireConsumption[pointToBlockBoxIndex(x, y)] = consumption }

    // these are characteristic properties of the fixture (they have constant value) so must not be serialised
    @Transient val wireEmitterTypes: HashMap<BlockBoxIndex, WireEmissionType> = HashMap()
    @Transient val wireSinkTypes: HashMap<BlockBoxIndex, WireEmissionType> = HashMap()
    @Transient val wireEmission: HashMap<BlockBoxIndex, Vector2> = HashMap()
    @Transient val wireConsumption: HashMap<BlockBoxIndex, Vector2> = HashMap()

    // these are NOT constant so they ARE serialised. Type: Map<SinkType (String) -> Charge (Double>
    // Use case: signal buffer (sinkType=digital_bit), battery (sinkType=electricity), etc.
    val chargeStored: HashMap<String, Double> = HashMap()


    /** Triggered when 'digital_bit' rises from low to high. Edge detection only considers the real component (labeled as 'x') of the vector */
    open fun onRisingEdge(readFrom: BlockBoxIndex) {}
    /** Triggered when 'digital_bit' rises from high to low. Edge detection only considers the real component (labeled as 'x') of the vector */
    open fun onFallingEdge(readFrom: BlockBoxIndex) {}
    /** Triggered when 'digital_bit' is held high. This function WILL NOT be triggered simultaneously with the rising edge. Level detection only considers the real component (labeled as 'x') of the vector */
    open fun onSignalHigh(readFrom: BlockBoxIndex) {}
    /** Triggered when 'digital_bit' is held low. This function WILL NOT be triggered simultaneously with the falling edge. Level detection only considers the real component (labeled as 'x') of the vector */
    open fun onSignalLow(readFrom: BlockBoxIndex) {}


    private val oldSinkStatus: Array<Vector2>

    open fun updateOnWireGraphTraversal(offsetX: Int, offsetY: Int, sinkType: WireEmissionType) {
        val index = pointToBlockBoxIndex(offsetX, offsetY)
        val old = oldSinkStatus[index]
        val wx = offsetX + intTilewiseHitbox.startX.toInt()
        val wy = offsetY + intTilewiseHitbox.startY.toInt()
        val new = WireCodex.getAllWiresThatAccepts("digital_bit").fold(Vector2()) { acc, (id, _) ->
            INGAME.world.getWireEmitStateOf(wx, wy, id).let {
                Vector2(acc.x + (it?.x ?: 0.0), acc.y + (it?.y ?: 0.0))
            }
        }

        if (sinkType == "digital_bit") {
            if (new.x - old.x >= ELECTRIC_THRESHOLD_EDGE_DELTA && new.x >= ELECTIC_THRESHOLD_HIGH)
                onRisingEdge(index)
            else if (old.x - new.x >= ELECTRIC_THRESHOLD_EDGE_DELTA && new.x <= ELECTRIC_THRESHOLD_LOW)
                onFallingEdge(index)
            else if (new.x >= ELECTIC_THRESHOLD_HIGH)
                onSignalHigh(index)
            else if (new.y <= ELECTRIC_THRESHOLD_LOW)
                onSignalLow(index)
        }
    }

    override fun update(delta: Float) {
        super.update(delta)
        oldSinkStatus.indices.forEach { index ->
            val wx = (index % blockBox.width) + intTilewiseHitbox.startX.toInt()
            val wy = (index / blockBox.width) + intTilewiseHitbox.startY.toInt()
            val new = WireCodex.getAllWiresThatAccepts(getWireSinkAt(index % blockBox.width, index / blockBox.width) ?: "").fold(Vector2()) { acc, (id, _) ->
                INGAME.world.getWireEmitStateOf(wx, wy, id).let {
                    Vector2(acc.x + (it?.x ?: 0.0), acc.y + (it?.y ?: 0.0))
                }
            }
            oldSinkStatus[index].set(new)
        }
    }
}

/**
 * Protip: do not make child classes take any argument, especially no function (function "classes" have no zero-arg constructor)
 *
 * Initialising Fixture after deserialisation: override `reload()`
 *
 * Created by minjaesong on 2016-06-17.
 */
open class FixtureBase : ActorWithBody, CuedByTerrainChange {

    @Transient open val spawnNeedsWall: Boolean = false
    @Transient open val spawnNeedsFloor: Boolean = true

    /** Real time, in nanoseconds */
    @Transient var spawnRequestedTime: Long = 0L
        protected set

    lateinit var blockBox: BlockBox // something like TapestryObject will want to redefine this

    fun blockBoxIndexToPoint2i(it: BlockBoxIndex): Point2i = this.blockBox.width.let { w -> Point2i(it % w, it / w) }
    fun pointToBlockBoxIndex(point: Point2i) = point.y * this.blockBox.width + point.x
    fun pointToBlockBoxIndex(x: Int, y: Int) = y * this.blockBox.width + x

    @Transient var blockBoxProps: BlockBoxProps = BlockBoxProps(0)
    @Transient var nameFun: () -> String = { "" }
    @Transient var mainUI: UICanvas? = null
    var inventory: FixtureInventory? = null

//    @Transient var mainUIopenFun: ((UICanvas) -> Unit)? = null

    internal var actorThatInstalledThisFixture: UUID? = null

    protected constructor() : super(RenderOrder.BEHIND, PhysProperties.IMMOBILE, null)
    protected constructor(renderOrder: RenderOrder, physProp: PhysProperties, id: ActorID?) : super(renderOrder, physProp, id)

    /**
     * Callend whenever the fixture was spawned successfully.
     *
     * @param tx bottom-centre tilewise point of the spawned fixture
     * @param ty bottom-centre tilewise point of the spawned fixture
     */
    open fun onSpawn(tx: Int, ty: Int) {}

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

    val everyBlockboxPos: List<Pair<Int, Int>>?
        get() = worldBlockPos?.let { (posX, posY) ->
            (posX until posX + blockBox.width).toList().cartesianProduct((posY until posY + blockBox.height).toList())
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
     * Condition for (if the tile is solid) is always implied regardless of this function. See [canSpawnHere0]
     */
    open fun canSpawnOnThisFloor(itemID: ItemID): Boolean {
        return true
    }

    fun canSpawnHere(posX0: Int, posY0: Int): Boolean {
        val posX = (posX0 - blockBox.width.minus(1).div(2)) fmod world!!.width // width.minus(1) so that spawning position would be same as the ghost's position
        val posY = posY0 - blockBox.height + 1
        return canSpawnHere0(posX, posY)
    }

    open fun canSpawnHere0(posX: Int, posY: Int): Boolean {
        val everyBlockboxPos = (posX until posX + blockBox.width).toList().cartesianProduct((posY until posY + blockBox.height).toList())

        // check for existing blocks (and fixtures)
        var cannotSpawn = false
        worldBlockPos = Point2i(posX, posY)

        cannotSpawn = everyBlockboxPos.any { (x, y) -> !BlockCodex[world!!.getTileFromTerrain(x, y)].hasTag("INCONSEQUENTIAL") }

        // check for walls, if spawnNeedsWall = true
        if (spawnNeedsWall) {
            cannotSpawn = cannotSpawn or everyBlockboxPos.any { (x, y) -> !BlockCodex[world!!.getTileFromWall(x, y)].isSolid }
        }

        // check for floors, if spawnNeedsFloor == true
        if (spawnNeedsFloor) {
            val y = posY + blockBox.height
            val xs = posX until posX + blockBox.width
            cannotSpawn = cannotSpawn or xs.any { x ->
                world!!.getTileFromTerrain(x, y).let {
                    !BlockCodex[it].isSolid || !canSpawnOnThisFloor(it)
                }
            }
        }

        return !cannotSpawn
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

        if (!canSpawnHere(posX0, posY0)) {
            printdbg(this, "cannot spawn fixture1 ${nameFun()} at F${INGAME.WORLD_UPDATE_TIMER}, has tile collision; xy=($posX,$posY) tDim=(${blockBox.width},${blockBox.height})")
            printStackTrace(this)
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
        this.intTilewiseHitbox.setFromWidthHeight(
            posX.toDouble(),
            posY.toDouble(),
            blockBox.width.toDouble(),
            blockBox.height.toDouble()
        )

        // actually add this actor into the world
        INGAME.queueActorAddition(this)
        spawnRequestedTime = System.nanoTime()

        actorThatInstalledThisFixture = installersUUID

        makeNoiseAndDust(posX, posY)

        onSpawn(posX0, posY0)

        return true
    }

    /**
     * @param posX top-left
     * @param posY top-left
     */
    open fun makeNoiseAndDust(posX: Int, posY: Int) {
        val posYb = posY + blockBox.height - 1
        val posXc = posX + blockBox.width / 2

        // make some noise
        var soundSource =
            if (spawnNeedsWall) 1
            else if (spawnNeedsFloor) 0
            else 2
        // 1: wall, 0: floor, 2: if wall is not solid, use wall; else, use floor
        val wallTile = world!!.getTileFromWall(posXc, posYb)
        val terrTile = world!!.getTileFromTerrain(posXc, posYb + 1)

        if (soundSource == 2) {
            soundSource = if (BlockCodex[wallTile].isSolid)
                1
            else
                0
        }

        when (soundSource) {
            1 -> PickaxeCore.makeNoise(this, wallTile)
            0 -> PickaxeCore.makeNoise(this, terrTile)
        }

        // make some dust
        if (soundSource == 0) {
            val y = posY + blockBox.height
            for (x in posX until posX + blockBox.width) {
                val tile = world!!.getTileFromTerrain(x, y)
                PickaxeCore.makeDust(tile, x, y - 1, 4 + (Math.random() + Math.random()).roundToInt())
            }
        }
        else {
            for (y in posY until posY + blockBox.height) {
                for (x in posX until posX + blockBox.width) {
                    val tile = world!!.getTileFromWall(x, y)
                    PickaxeCore.makeDust(tile, x, y, 2 + (Math.random() + Math.random()).roundToInt())
                }
            }
        }
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
        this.intTilewiseHitbox.setFromWidthHeight(
            posX.toDouble(),
            posY.toDouble(),
            blockBox.width.toDouble(),
            blockBox.height.toDouble()
        )

        // check for existing blocks (and fixtures)
        if (!canSpawnHere0(posX, posY)) {
            printdbg(this, "cannot spawn fixture2 ${nameFun()} at F${INGAME.WORLD_UPDATE_TIMER}, has tile collision; xy=($posX,$posY) tDim=(${blockBox.width},${blockBox.height})")
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

        makeNoiseAndDust(posX, posY)

        return true
    }

    /** force disable despawn when inventory is not empty */
    open val canBeDespawned: Boolean get() = inventory?.isEmpty() ?: true

    @Transient open var despawnHook: (FixtureBase) -> Unit = {}

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

            despawnHook(this)
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

    /**
     * This function MUST BE super-called for make despawn call to work at all.
     */
    override fun update(delta: Float) {
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
 * - 0: fluid intolerance - when SET, the fixture will break itself to item/nothing (depends on the flag #1).
 *       For example, crops have this flag SET.
 * - 1: no drops - when SET, the fixture will simply disappear instead of dropping itself.
 *       For example, crops have this flag SET.
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
