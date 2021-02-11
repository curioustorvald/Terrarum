package net.torvald.terrarum.modulebasegame.gameworld

import com.badlogic.gdx.Input
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.FluidType
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.TILE_SIZE
import org.khelekore.prtree.*
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


    private lateinit var actorsRTree: PRTree<ActorWithBody>

    fun resetForThisFrame() {

    }

    /** Must be called BEFORE the actors update -- actors depend on the R-Tree for various things */
    operator fun invoke(player: ActorHumanoid?, delta: Float) {
        // build the r-tree that will be used during a single frame of updating
        actorsRTree = PRTree(actorMBRConverter, 24)
        actorsRTree.load(ingame.actorContainerActive.filterIsInstance<ActorWithBody>())



        //printdbg(this, "============================")

        if (player != null) {
            updateXFrom = player.hitbox.centeredX.div(CreateTileAtlas.TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundToInt()
            updateYFrom = player.hitbox.centeredY.div(CreateTileAtlas.TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundToInt()
            updateXTo = updateXFrom + DOUBLE_RADIUS
            updateYTo = updateYFrom + DOUBLE_RADIUS
        }
        //moveFluids(delta)
        displaceFallables(delta)

        //printdbg(this, "============================")
    }

    /**
     * @return list of actors under the bounding box given, list may be empty if no actor is under the point.
     */
    fun getActorsAt(startPoint: Point2d, endPoint: Point2d): List<ActorWithBody> {
        val outList = ArrayList<ActorWithBody>()
        actorsRTree.find(startPoint.x, startPoint.y, endPoint.x, endPoint.y, outList)
        return outList
    }

    fun getActorsAt(worldX: Double, worldY: Double): List<ActorWithBody> {
        val outList = ArrayList<ActorWithBody>()
        actorsRTree.find(worldX, worldY, worldX + 1.0, worldY + 1.0, outList)
        return outList
    }

    /** Will use centre point of the actors
     * @return List of DistanceResult, list may be empty */
    fun findKNearestActors(from: ActorWithBody, maxHits: Int): List<DistanceResult<ActorWithBody>> {
        return actorsRTree.nearestNeighbour(actorDistanceCalculator, null, maxHits, object : PointND {
            override fun getDimensions(): Int = 2
            override fun getOrd(axis: Int): Double = when(axis) {
                0 -> from.hitbox.centeredX
                1 -> from.hitbox.centeredY
                else -> throw IllegalArgumentException("nonexistent axis $axis for ${dimensions}-dimensional object")
            }
        })
    }
    /** Will use centre point of the actors
     * @return Pair of: the actor, distance from the actor; null if none found */
    fun findNearestActors(from: ActorWithBody): DistanceResult<ActorWithBody>? {
        val t = findKNearestActors(from, 1)
        return if (t.isNotEmpty())
            t[0]
        else
            null
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

        if (AppLoader.IS_DEVELOPMENT_BUILD) {
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
        if (!DEBUG_STEPPING_MODE || DEBUG_STEPPING_MODE && KeyToggler.isOn (Input.Keys.PERIOD)) {
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
                        world.setTileTerrain(x, y, Block.AIR)
                        world.setTileTerrain(x, y + fallDownCounter, currentTile)

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

            if (DEBUG_STEPPING_MODE) {
                KeyToggler.forceSet(Input.Keys.PERIOD, false)
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


    private val actorMBRConverter = object : MBRConverter<ActorWithBody> {
        override fun getDimensions(): Int = 2
        override fun getMin(axis: Int, t: ActorWithBody): Double =
                when (axis) {
                    0 -> t.hitbox.startX
                    1 -> t.hitbox.startY
                    else -> throw IllegalArgumentException("nonexistent axis $axis for ${dimensions}-dimensional object")
                }

        override fun getMax(axis: Int, t: ActorWithBody): Double =
                when (axis) {
                    0 -> t.hitbox.endX
                    1 -> t.hitbox.endY
                    else -> throw IllegalArgumentException("nonexistent axis $axis for ${dimensions}-dimensional object")
                }
    }

    // simple euclidean norm, squared
    private val actorDistanceCalculator = DistanceCalculator<ActorWithBody> { t: ActorWithBody, p: PointND ->
        val dist1 = (p.getOrd(0) - t.hitbox.centeredX).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()
        // ROUNDWORLD implementation
        val dist2 = (p.getOrd(0) - (t.hitbox.centeredX - world.width * TILE_SIZE)).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()
        val dist3 = (p.getOrd(0) - (t.hitbox.centeredX + world.width * TILE_SIZE)).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()

        minOf(dist1, minOf(dist2, dist3))
    }
}