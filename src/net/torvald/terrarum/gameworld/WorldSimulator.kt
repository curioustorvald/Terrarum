package net.torvald.terrarum.gameworld

import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame.Companion.inUpdateRange
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.Electric
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import org.dyn4j.geometry.Vector2
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2016-08-03.
 */
object WorldSimulator {

    private val DEBUG_STEPPING_MODE = false // use period key

    // FLUID-RELATED STUFFS //

    /**
     * In tiles;
     * square width/height = field * 2
     */
    // TODO: increase the radius and then MULTITHREAD
    const val FLUID_UPDATING_SQUARE_RADIUS = 80 // larger value will have dramatic impact on performance
    const private val DOUBLE_RADIUS = FLUID_UPDATING_SQUARE_RADIUS * 2

    // maps are separated as old-new for obvious reason, also it'll allow concurrent modification
    private val fluidMap = Array(DOUBLE_RADIUS, { FloatArray(DOUBLE_RADIUS) })
    private val fluidTypeMap = Array(DOUBLE_RADIUS, { Array<FluidType>(DOUBLE_RADIUS) { Fluid.NULL } })
    private val fluidNewMap = Array(DOUBLE_RADIUS, { FloatArray(DOUBLE_RADIUS) })
    private val fluidNewTypeMap = Array(DOUBLE_RADIUS, { Array<FluidType>(DOUBLE_RADIUS) { Fluid.NULL } })

    const val FLUID_MAX_MASS = 1f // The normal, un-pressurized mass of a full water cell
    const val FLUID_MAX_COMP = 0.02f // How much excess water a cell can store, compared to the cell above it. A tile of fluid can contain more than MaxMass water.
    const val FLUID_MIN_MASS = 0.0001f //Ignore cells that are almost dry
    const val WIRE_MIN_FLOW = 0.0001f
    const val minFlow = 0.01f
    const val maxSpeed = 1f // max units of water moved out of one block to another, per timestamp

    // END OF FLUID-RELATED STUFFS

    /** Top-left point */
    var updateXFrom = 0
    /** Bottom-right point */
    var updateXTo = 0
    /** Top-left point */
    var updateYFrom = 0
    /** Bottom-right point */
    var updateYTo = 0

    private val ingame: IngameInstance
            get() = Terrarum.ingame!!
    private val world: GameWorld
            get() = ingame.world


    fun resetForThisFrame() {

    }

    /** Must be called BEFORE the actors update -- actors depend on the R-Tree for various things */
    operator fun invoke(player: ActorHumanoid?, delta: Float) {


        //printdbg(this, "============================")

        if (player != null) {
            updateXFrom = player.hitbox.centeredX.div(TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundToInt()
            updateYFrom = player.hitbox.centeredY.div(TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundToInt()
            updateXTo = updateXFrom + DOUBLE_RADIUS
            updateYTo = updateYFrom + DOUBLE_RADIUS
        }

        degrass()

        App.measureDebugTime("WorldSimulator.fluids") {
            //moveFluids(delta)
        }
        App.measureDebugTime("WorldSimulator.fallables") {
            displaceFallables(delta)
        }
        App.measureDebugTime("WorldSimulator.wires") {
            simulateWires(delta)
        }

        //printdbg(this, "============================")
    }



    fun degrass() {
        if (ingame.terrainChangeQueue.isNotEmpty()) { App.measureDebugTime("WorldSimulator.degrass") {

            //val grassPlacedByPlayer = ArrayList<IngameInstance.BlockChangeQueueItem>()

            ingame.terrainChangeQueue.forEach {
                if (BlockCodex[it.new].isSolid) {
                    if (world.getTileFromTerrain(it.posX, it.posY + 1) == Block.GRASS) {
                        //grassPlacedByPlayer.add(it)
                        world.setTileTerrain(it.posX, it.posY + 1, Block.DIRT, true)
                    }
                }
            }

            // kill grasses surrounded by dirts in cruciform formation
            // NOPE this part would not work; environment-depending degrassing must be done by the "grass spread simulator"
            /*val for_y_start = (WorldCamera.y.toFloat() / TILE_SIZE).floorInt()
            val for_y_end = for_y_start + BlocksDrawer.tilesInVertical - 1

            val for_x_start = (WorldCamera.x.toFloat() / TILE_SIZE).floorInt()
            val for_x_end = for_x_start + BlocksDrawer.tilesInHorizontal - 1
            for (y in for_y_start..for_y_end) {
                for (x in for_x_start..for_x_end) {
                    // do not de-grass ones placed by player
                    var nogo = false
                    grassPlacedByPlayer.forEach {
                        if (x == it.posX && y == it.posY) nogo = true
                    }

                    if (!nogo) {
                        val tile = world.getTileFromTerrain(x, y)
                        if (tile == Block.GRASS) {
                            if (world.getTileFromTerrain(x - 1, y) != Block.GRASS &&
                                world.getTileFromTerrain(x + 1, y) != Block.GRASS &&
                                world.getTileFromTerrain(x, y - 1) != Block.GRASS &&
                                world.getTileFromTerrain(x, y + 1) != Block.GRASS) {
                                world.setTileTerrain(x, y, Block.DIRT, true)
                            }
                        }
                    }
                }
            }*/

        } }
    }

    /**
     * displace fluids. Note that the code assumes the gravity pulls things downward ONLY,
     * which means you'll need to modify the code A LOT if you're going to implement zero- or
     * reverse-gravity.
     *
     * Procedure: CP world fluidmap -> sim on fluidmap -> CP fluidmap world
     * TODO multithread
     */
    fun moveFluids(delta: Float) {
        makeFluidMapFromWorld()

        simCompression()

        if (App.IS_DEVELOPMENT_BUILD) {
            monitorIllegalFluidSetup() // non-air non-zero fluid is kinda inevitable
        }

        fluidmapToWorld()
    }

    fun isFlowable(type: FluidType, worldX: Int, worldY: Int): Boolean {
        val fluid = world.getFluid(worldX, worldY)
        val tile = world.getTileFromTerrain(worldX, worldY)

        // true if target's type is the same as mine, or it's NULL (air)
        return ((fluid.type sameAs type || fluid.type sameAs Fluid.NULL) && !BlockCodex[tile].isSolid)
    }

    /**
     * displace fallable tiles. It is scanned bottom-left first. To achieve the sens ofreal
     * falling, each tiles are displaced by ONLY ONE TILE below.
     */
    fun displaceFallables(delta: Float) {
        /*for (y in updateYFrom..updateYTo) {
            for (x in updateXFrom..updateXTo) {
                val tile = world.getTileFromTerrain(x, y) ?: Block.STONE
                val tileBelow = world.getTileFromTerrain(x, y + 1) ?: Block.STONE

                if (tile.maxSupport()) {
                    // displace fluid. This statement must precede isSolid()
                    if (tileBelow.isFluid()) {
                        // remove tileThis to create air pocket
                        world.setTileTerrain(x, y, Block.AIR)

                        pour(x, y, drain(x, y, tileBelow.fluidLevel().toInt()))
                        // place our tile
                        world.setTileTerrain(x, y + 1, tile)
                    }
                    else if (!tileBelow.isSolid()) {
                        world.setTileTerrain(x, y, Block.AIR)
                        world.setTileTerrain(x, y + 1, tile)
                    }
                }
            }
        }*/

        // displace fallables (TODO implement blocks with fallable supports e.g. scaffolding)
        // only displace SINGLE BOTTOMMOST block on single X-coord (this doesn't mean they must fall only one block)
        // so that the "falling" should be visible to the end user
        for (x in updateXFrom..updateXTo) {
            var fallDownCounter = 0
            var fallableStackProcessed = false
            // one "stack" is a contiguous fallable blocks, regardless of the actual block number
            // when you are simulating the gradual falling, it is natural to process all the "stacks" at the same run,
            // otherwise you'll get an artefact.
            for (y in updateYTo downTo updateYFrom) {
                val currentTile = world.getTileFromTerrain(x, y)
                val prop = BlockCodex[currentTile]
                val isAir = currentTile == Block.AIR
                val support = prop.maxSupport
                val isFallable = support != -1

                // mark the beginnig of the new "stack"
                if (fallableStackProcessed && !isFallable) {
                    fallableStackProcessed = false
                } // do not chain with "else if"

                // process the gradual falling of the selected "stack"
                if (!fallableStackProcessed && fallDownCounter != 0 && isFallable) {
                    // replace blocks
                    world.setTileTerrain(x, y, Block.AIR, true)
                    world.setTileTerrain(x, y + fallDownCounter, currentTile, true)

                    fallableStackProcessed = true
                }
                else if (!isAir) {
                    fallDownCounter = 0
                }
                else if (!isFallable && fallDownCounter < FALLABLE_MAX_FALL_SPEED) {
                    fallDownCounter += 1
                }
            }
        }


    }


    fun disperseHeat(delta: Float) {

    }





    /*
    Explanation of get_stable_state_b (well, kind-of) :

    if x <= 1, all water goes to the lower cell
        * a = 0
        * b = 1

    if x > 1 & x < 2*MaxMass + MaxCompress, the lower cell should have MaxMass + (upper_cell/MaxMass) * MaxCompress
        b = MaxMass + (a/MaxMass)*MaxCompress
        a = x - b

        ->

        b = MaxMass + ((x - b)/MaxMass)*MaxCompress ->
            b = MaxMass + (x*MaxCompress - b*MaxCompress)/MaxMass
            b*MaxMass = MaxMass^2 + (x*MaxCompress - b*MaxCompress)
            b*(MaxMass + MaxCompress) = MaxMass*MaxMass + x*MaxCompress

            * b = (MaxMass*MaxMass + x*MaxCompress)/(MaxMass + MaxCompress)
        * a = x - b;

    if x >= 2 * MaxMass + MaxCompress, the lower cell should have upper+MaxCompress

        b = a + MaxCompress
        a = x - b

        ->

        b = x - b + MaxCompress ->
        2b = x + MaxCompress ->

        * b = (x + MaxCompress)/2
        * a = x - b
      */
    private fun getStableStateB(totalMass: Float): Float {
        if (totalMass <= 1)
            return 1f
        else if (totalMass < 2f * FLUID_MAX_MASS + FLUID_MAX_COMP)
            return (FLUID_MAX_MASS * FLUID_MAX_MASS + totalMass * FLUID_MAX_COMP) / (FLUID_MAX_MASS + FLUID_MAX_COMP)
        else
            return (totalMass + FLUID_MAX_COMP) / 2f
    }

    private fun simCompression() {
        // before data: fluidMap/fluidTypeMap
        // after data: fluidNewMap/fluidNewTypeMap

        // FIXME water doesn't disappear when they should
        // FIXME >as it turns out, fluid FUCKING MULTIPLIES themselves (wut D:)

        for (y in 1 until fluidMap.size - 1) {
            for (x in 1 until fluidMap[0].size - 1) {
                val worldX = x + updateXFrom
                val worldY = y + updateYFrom
                val remainingType = fluidTypeMap[y][x]

                // check solidity
                if (!isFlowable(remainingType, worldX, worldY)) continue
                // check if the fluid is a same kind
                //if (!isFlowable(type, worldX, worldY))) continue


                // Custom push-only flow
                var flow = 0f
                var remainingMass = fluidMap[y][x]
                //val remainingType = fluidTypeMap[y][x]
                if (remainingMass <= 0) continue

                // The block below this one
                if (isFlowable(remainingType, worldX, worldY + 1)) {
                    flow = getStableStateB(remainingMass + fluidMap[y + 1][x]) - fluidMap[y + 1][x]
                    if (flow > minFlow) {
                        flow *= 0.5f // leads to smoother flow
                    }
                    flow = flow.coerceIn(0f, minOf(maxSpeed, remainingMass))

                    fluidNewMap[y][x] -= flow
                    fluidNewMap[y + 1][x] += flow
                    fluidNewTypeMap[y + 1][x] = remainingType
                    remainingMass -= flow
                }

                if (remainingMass <= 0) continue

                // Left
                if (isFlowable(remainingType, worldX - 1, worldY)) {
                    // Equalise the amount fo water in this block and its neighbour
                    flow = (fluidMap[y][x] - fluidMap[y][x - 1]) / 4f
                    if (flow > minFlow) {
                        flow *= 0.5f
                    }
                    flow = flow.coerceIn(0f, remainingMass)

                    fluidNewMap[y][x] -= flow
                    fluidNewMap[y][x - 1] += flow
                    fluidNewTypeMap[y][x - 1] = remainingType
                    remainingMass -= flow
                }

                if (remainingMass <= 0) continue

                // Right
                if (isFlowable(remainingType, worldX + 1, worldY)) {
                    // Equalise the amount fo water in this block and its neighbour
                    flow = (fluidMap[y][x] - fluidMap[y][x + 1]) / 4f
                    if (flow > minFlow) {
                        flow *= 0.5f
                    }
                    flow = flow.coerceIn(0f, remainingMass)

                    fluidNewMap[y][x] -= flow
                    fluidNewMap[y][x + 1] += flow
                    fluidNewTypeMap[y][x + 1] = remainingType
                    remainingMass -= flow
                }

                if (remainingMass <= 0) continue

                // Up; only compressed water flows upwards
                if (isFlowable(remainingType, worldX, worldY - 1)) {
                    flow = remainingMass - getStableStateB(remainingMass + fluidMap[y - 1][x])
                    if (flow > minFlow) {
                        flow *= 0.5f
                    }
                    flow = flow.coerceIn(0f, minOf(maxSpeed, remainingMass))

                    fluidNewMap[y][x] -= flow
                    fluidNewMap[y - 1][x] += flow
                    fluidNewTypeMap[y - 1][x] = remainingType
                    remainingMass -= flow
                }


            }
        }

    }

    private val FALLABLE_MAX_FALL_SPEED = 2

    private fun monitorIllegalFluidSetup() {
        for (y in fluidMap.indices) {
            for (x in fluidMap[0].indices) {
                val fluidData = world.getFluid(x + updateXFrom, y + updateYFrom)
                if (fluidData.amount < 0f) {
                    throw InternalError("Negative amount of fluid at (${x + updateXFrom},${y + updateYFrom}): $fluidData")
                }
            }
        }
    }

    private fun makeFluidMapFromWorld() {
        //printdbg(this, "Scan area: ($updateXFrom,$updateYFrom)..(${updateXFrom + fluidMap[0].size},${updateYFrom + fluidMap.size})")

        for (y in fluidMap.indices) {
            for (x in fluidMap[0].indices) {
                val fluidData = world.getFluid(x + updateXFrom, y + updateYFrom)
                fluidMap[y][x] = fluidData.amount
                fluidTypeMap[y][x] = fluidData.type
                fluidNewMap[y][x] = fluidData.amount
                fluidNewTypeMap[y][x] = fluidData.type

                /*if (x + updateXFrom == 60 && y + updateYFrom == 256) {
                    printdbg(this, "making array amount ${fluidData.amount} for (60,256)")
                }*/
            }
        }
    }

    private fun fluidmapToWorld() {
        for (y in fluidMap.indices) {
            for (x in fluidMap[0].indices) {
                world.setFluid(x + updateXFrom, y + updateYFrom, fluidNewTypeMap[y][x], fluidNewMap[y][x])
            }
        }
    }


    fun ItemID.isFallable() = BlockCodex[this].maxSupport





    private val wiresimOverscan = 60

    /**
     * @return List of FixtureBases, safe to cast into Electric
     */
    private fun wiresimGetSourceBlocks(): List<FixtureBase> =
            Terrarum.ingame!!.actorContainerActive.filter {
                it is FixtureBase && it is Electric && it.inUpdateRange(world) && it.wireEmitterTypes.isNotEmpty()
            } as List<FixtureBase>

    private fun simulateWires(delta: Float) {
        wiresimGetSourceBlocks().let { sources ->
            // signal-emitting fixtures must set emitState of its own tiles via update()
            sources.forEach {
                (it as Electric).wireEmitterTypes.forEach { wireType, bbi ->

                    val startingPoint = WireGraphCursor(it.worldBlockPos!! + it.blockBoxIndexToPoint2i(bbi))
                    val signal = (it as Electric).wireEmission[bbi] ?: Vector2(0.0, 0.0)

                    world.getAllWiringGraph(startingPoint.x, startingPoint.y)?.keys?.filter { WireCodex[it].accepts == wireType }?.forEach { wire ->
                        traverseWireGraph(world, wire, startingPoint, signal)
                    }
                }
            }
        }
    }

    private fun traverseWireGraph(world: GameWorld, wire: ItemID, startingPoint: WireGraphCursor, signal: Vector2) {

        fun getAdjacent(cnx: Int, point: WireGraphCursor): List<WireGraphCursor> {
            val r = ArrayList<WireGraphCursor>()
            for (dir in intArrayOf(RIGHT, DOWN, LEFT, UP)) {
                if (cnx and dir != 0) r.add(point.copy().moveOneCell(dir))
            }
            return r
        }

        var point = startingPoint.copy()
        val points = Queue<WireGraphCursor>() // a queue, enqueued at the end
        val marked = HashSet<Long>()

        fun mark(point: WireGraphCursor) {
            marked.add(point.longHash())
            // do some signal action
            world.setWireEmitStateOf(point.x, point.y, wire, signal)
        }

        fun isMarked(point: WireGraphCursor) = marked.contains(point.longHash())
        fun enq(point: WireGraphCursor) = points.addFirst(point.copy())
        fun deq() = points.removeLast()

        enq(point)
        mark(point)

        while (points.notEmpty()) {
            point = deq()
            // TODO if we found a power receiver, do something to it
            world.getWireGraphOf(point.x, point.y, wire)?.let { connections ->
                for (x in getAdjacent(connections, point)) {
                    if (!isMarked(x)) {
                        mark(x)
                        enq(x)
                    }
                }
            }
        }
    }


    private const val RIGHT = 1
    private const val DOWN = 2
    private const val LEFT = 4
    private const val UP = 8

    private fun getNearbyTilesPos(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
                Point2i(x + 1, y),
                Point2i(x, y - 1),
                Point2i(x - 1, y),
                Point2i(x, y + 1) // don't know why but it doesn't work if I don't flip Y
        )
    }

    data class WireGraphCursor(
            var x: Int,
            var y: Int,
            var fromWhere: Int, //1: right, 2: down, 4: left, 8: up, 0: *shrug*
            var len: Int
    ) {
        constructor(point2i: Point2i): this(point2i.x, point2i.y, 0, 0)

        fun moveOneCell(dir: Int): WireGraphCursor {
            when (dir) {
                1 -> { x += 1; fromWhere = LEFT }
                2 -> { y += 1; fromWhere = UP }
                4 -> { x -= 1; fromWhere = RIGHT }
                8 -> { y -= 1; fromWhere = DOWN }
                else -> throw IllegalArgumentException("Unacceptable direction: $dir")
            }
            len += 1

            return this
        }

        fun longHash() = x.toLong().shl(32) or y.toLong()
    }
}