package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.utils.Queue
import net.torvald.random.HQRNG
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Controllable
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.GameWorld.Companion.FLUID
import net.torvald.terrarum.modulebasegame.TerrarumIngame.Companion.inUpdateRange
import net.torvald.terrarum.modulebasegame.gameactors.*
import net.torvald.terrarum.modulebasegame.gameitems.AxeCore
import net.torvald.terrarum.realestate.LandUtil.CHUNK_H
import net.torvald.terrarum.realestate.LandUtil.CHUNK_W
import org.dyn4j.geometry.Vector2
import kotlin.math.cosh
import kotlin.math.min
import kotlin.math.pow
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
    private val fluidMap = Array(DOUBLE_RADIUS) { FloatArray(DOUBLE_RADIUS) }
    private val fluidTypeMap = Array(DOUBLE_RADIUS) { Array(DOUBLE_RADIUS) { Fluid.NULL } }
    private val fluidNewMap = Array(DOUBLE_RADIUS) { FloatArray(DOUBLE_RADIUS) }
    private val fluidNewTypeMap = Array(DOUBLE_RADIUS) { Array(DOUBLE_RADIUS) { Fluid.NULL } }

    const val FLUID_MAX_MASS = 1f // The normal, un-pressurized mass of a full water cell
    const val FLUID_MAX_COMP = 0.01f // How much excess water a cell can store, compared to the cell above it. A tile of fluid can contain more than MaxMass water.
//    const val FLUID_MIN_MASS = net.torvald.terrarum.gameworld.FLUID_MIN_MASS //Ignore cells that are almost dry (smaller than epsilon of float16)
    const val minFlow = 1f / 512f

    // END OF FLUID-RELATED STUFFS

    /** Top-left point */
    var updateXFrom = 0
    /** Bottom-right point */
    var updateXTo = 0
    /** Top-left point */
    var updateYFrom = 0
    /** Bottom-right point */
    var updateYTo = 0

    private val ingame: TerrarumIngame
            get() = Terrarum.ingame!! as TerrarumIngame
    private val world: GameWorld
            get() = ingame.world


    fun resetForThisFrame() {

    }

    private val rng = HQRNG()

    /** Must be called BEFORE the actors update -- actors depend on the R-Tree for various things */
    operator fun invoke(player: ActorHumanoid?, delta: Float) {


        //printdbg(this, "============================")

        if (player != null) {
            updateXFrom = player.hitbox.centeredX.div(TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundToInt()
            updateYFrom = player.hitbox.centeredY.div(TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundToInt()
            updateXTo = updateXFrom + DOUBLE_RADIUS
            updateYTo = updateYFrom + DOUBLE_RADIUS
        }

        if (ingame.terrainChangeQueue.isNotEmpty()) { App.measureDebugTime("WorldSimulator.degrass") { buryGrassImmediately() } }
        App.measureDebugTime("WorldSimulator.growGrass") { growOrKillGrass() }
        App.measureDebugTime("WorldSimulator.fluids") { moveFluids(delta) }
        App.measureDebugTime("WorldSimulator.fallables") { displaceFallables(delta) }
        App.measureDebugTime("WorldSimulator.wires") { simulateWires(delta) }
        App.measureDebugTime("WorldSimulator.collisionDroppedItem") { collideDroppedItems() }
        App.measureDebugTime("WorldSimulator.dropTreeLeaves") { dropTreeLeaves() }


        //printdbg(this, "============================")
    }



    fun buryGrassImmediately() {
        ingame.terrainChangeQueue.toList().forEach {
            if (it != null) {
                val blockProp = BlockCodex[it.new]
                if (blockProp.isSolid && !blockProp.isActorBlock) {
                    if (world.getTileFromTerrain(it.posX, it.posY + 1) == Block.GRASS) {
                        //grassPlacedByPlayer.add(it)
                        world.setTileTerrain(it.posX, it.posY + 1, Block.DIRT, true)
                    }
                }
            }
        }
    }

    private fun sech2(x: Float) = 1f / cosh(x).sqr()

    fun growOrKillGrass() {
        // season-dependent growth rate
        // https://www.desmos.com/calculator/4u5npfxgak
        val baseCount = 2 * world.worldTime.timeDelta
        val season = world.worldTime.ecologicalSeason.coerceIn(0f, 5f) // 1->1.0,  2.5->3.0, 4->1.0
        val seasonalMult = 1f + sech2((season - 2.5f)) * 2f
        val repeatCount = (baseCount * seasonalMult).ditherToInt()

        repeat(repeatCount) {
            val rx = rng.nextInt(updateXFrom, updateXTo + 1)
            val ry = rng.nextInt(updateYFrom, updateYTo + 1)
            val tile = world.getTileFromTerrain(rx, ry)
            // if the dirt tile has a grass and an air tile nearby, put grass to it
            if (tile == Block.DIRT) {
                val nearby8 = getNearbyTiles8(rx, ry)
                val nearby4 = listOf(nearby8[0], nearby8[2], nearby8[4], nearby8[6])
                if (nearby8.any { !BlockCodex[it].isSolid } && nearby4.any { it == Block.GRASS }) {
                    world.setTileTerrain(rx, ry, Block.GRASS, false)
                }
            }
            // if the grass tile is confined, kill it
            else if (tile == Block.GRASS) {
                val nearby = getNearbyTiles8(rx, ry)
                if (nearby.all { BlockCodex[it].isSolid }) {
                    world.setTileTerrain(rx, ry, Block.DIRT, false)
                }
            }
        }
    }

    fun dropTreeLeaves() {
        repeat(26 * world.worldTime.timeDelta) {
            val rx = rng.nextInt(updateXFrom, updateXTo + 1)
            val ry = rng.nextInt(updateYFrom, updateYTo + 1)
            val tile = world.getTileFromTerrain(rx, ry)
            // if the dirt tile has a grass and an air tile nearby, put grass to it
            if (BlockCodex[tile].hasAllTagsOf("TREE", "LEAVES")) {
                val nearby8 = getNearbyTiles8(rx, ry)
                val nearbyCount = nearby8.count { BlockCodex[it].hasTag("TREE") }

                if (nearbyCount <= 1) {
                    AxeCore.removeLeaf(rx, ry)
                }
            }
        }
    }

    fun collideDroppedItems() {
        ingame.actorContainerActive.filter { it is DroppedItem }.forEach { droppedItem0 ->
            val droppedItem = droppedItem0 as DroppedItem
            if (droppedItem.canBePickedUp()) {
                val actors = ingame.findKNearestActors(droppedItem0 as ActorWithBody, 64) { it is Controllable && it is Pocketed }
                for (result in actors) {
                    val actor = result.get()
                    // if hitbox overlaps, pick up
                    val s = actor.scale
                    val pickupDistSqr = (96f * s).sqr()// TODO refer to the actorValue
//                    println("${result.distance}\pickupDistSqr")
                    if (result.distance < pickupDistSqr) {
                        droppedItem.onItemPickup(actor)
                        break
                    }
                }
            }
        }
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

    fun isSolid(worldX: Int, worldY: Int): Boolean {
        val tile = world.getTileFromTerrain(worldX, worldY)
        return BlockCodex[tile].isSolid
    }

    fun isFlowable(type: ItemID, worldX: Int, worldY: Int): Boolean {
        val fluid = world.getFluid(worldX, worldY)
        val tile = world.getTileFromTerrain(worldX, worldY)

        // true if target's type is the same as mine, or it's NULL (air)
        return (((fluid.type == type && fluid.amount < FLUID_MAX_MASS + FLUID_MAX_COMP) || fluid.type == Fluid.NULL) && !BlockCodex[tile].isSolid)
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
                // don't let the falling sand destroy the precious storage chest
                val isAir = prop.hasTag("INCONSEQUENTIAL")
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

                val maxSpeed = 1f / FluidCodex[remainingType].viscosity.sqr()

                // check solidity
                if (isSolid(worldX, worldY)) continue

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
                    flow = flow.coerceIn(0f, min(maxSpeed, remainingMass))

                    fluidNewMap[y][x] -= flow
                    fluidNewMap[y + 1][x] += flow
                    fluidNewTypeMap[y + 1][x] = remainingType
                    remainingMass -= flow

                    INGAME.modified(FLUID, x / CHUNK_W, y / CHUNK_H)
                    INGAME.modified(FLUID, x / CHUNK_W, (y + 1) / CHUNK_H)
                }

                if (remainingMass <= 0) continue

                // Left
                if (isFlowable(remainingType, worldX - 1, worldY)) {
                    // Equalise the amount fo water in this block and its neighbour
                    flow = (fluidMap[y][x] - fluidMap[y][x - 1]) / 4f
                    if (flow > minFlow) {
                        flow *= 0.5f
                    }
                    flow = flow.coerceIn(0f, min(maxSpeed, remainingMass))

                    fluidNewMap[y][x] -= flow
                    fluidNewMap[y][x - 1] += flow
                    fluidNewTypeMap[y][x - 1] = remainingType
                    remainingMass -= flow

                    INGAME.modified(FLUID, x / CHUNK_W, y / CHUNK_H)
                    INGAME.modified(FLUID, (x - 1) / CHUNK_W, y / CHUNK_H)
                }

                if (remainingMass <= 0) continue

                // Right
                if (isFlowable(remainingType, worldX + 1, worldY)) {
                    // Equalise the amount fo water in this block and its neighbour
                    flow = (fluidMap[y][x] - fluidMap[y][x + 1]) / 4f
                    if (flow > minFlow) {
                        flow *= 0.5f
                    }
                    flow = flow.coerceIn(0f, min(maxSpeed, remainingMass))

                    fluidNewMap[y][x] -= flow
                    fluidNewMap[y][x + 1] += flow
                    fluidNewTypeMap[y][x + 1] = remainingType
                    remainingMass -= flow

                    INGAME.modified(FLUID, x / CHUNK_W, y / CHUNK_H)
                    INGAME.modified(FLUID, (x + 1) / CHUNK_W, y / CHUNK_H)
                }

                if (remainingMass <= 0) continue

                // Up; only compressed water flows upwards
                if (isFlowable(remainingType, worldX, worldY - 1)) {
                    flow = remainingMass - getStableStateB(remainingMass + fluidMap[y - 1][x])
                    if (flow > minFlow) {
                        flow *= 0.5f
                    }
                    flow = flow.coerceIn(0f, min(maxSpeed, remainingMass))

                    fluidNewMap[y][x] -= flow
                    fluidNewMap[y - 1][x] += flow
                    fluidNewTypeMap[y - 1][x] = remainingType
                    remainingMass -= flow

                    INGAME.modified(FLUID, x / CHUNK_W, y / CHUNK_H)
                    INGAME.modified(FLUID, x / CHUNK_W, (y - 1) / CHUNK_H)
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
    private fun wiresimGetSourceBlocks(): List<Electric> =
            INGAME.actorContainerActive.filterIsInstance<Electric>().filter {
                it.inUpdateRange(world) && it.wireEmitterTypes.isNotEmpty()
            }

    private val wireSimMarked = HashSet<Long>()
    private val wireSimPoints = Queue<WireGraphCursor>()
    private val oldTraversedNodes = ArrayList<WireGraphCursor>()
    private val fixtureCache = HashMap<Point2i, Pair<Electric, WireEmissionType>>() // also instance of Electric

    private fun simulateWires(delta: Float) {
        // unset old wires before we begin
        oldTraversedNodes.forEach { (x, y, _, _, wire) ->
            world.getAllWiringGraph(x, y)?.get(wire)?.emt?.set(0.0, 0.0)
        }

        oldTraversedNodes.clear()
        fixtureCache.clear()

        wiresimGetSourceBlocks().let { sources ->
            // signal-emitting fixtures must set emitState of its own tiles via update()
            sources.forEach {
                it.wireEmitterTypes.forEach { (bbi, wireType) ->

                    val startingPoint = it.worldBlockPos!! + it.blockBoxIndexToPoint2i(bbi)
                    val signal = it.wireEmission[bbi] ?: Vector2(0.0, 0.0)

                    world.getAllWiringGraph(startingPoint.x, startingPoint.y)?.keys?.filter { WireCodex[it].accepts == wireType }?.forEach { wire ->
                        val simStartingPoint = WireGraphCursor(startingPoint, wire)
                        wireSimMarked.clear()
                        wireSimPoints.clear()
                        traverseWireGraph(world, wire, simStartingPoint, signal, wireType)
                    }
                }
            }
        }
    }

    private fun calculateDecay(signal: Vector2, dist: Int, wire: ItemID, signalType: WireEmissionType): Vector2 {
        val d = WireCodex.wireDecays[wire] ?: 1.0
        return signal * d.pow(dist.toDouble())
    }

    private fun traverseWireGraph(world: GameWorld, wire: ItemID, startingPoint: WireGraphCursor, signal: Vector2, signalType: WireEmissionType) {

        val emissionType = WireCodex[wire].accepts

        fun getAdjacent(cnx: Int, point: WireGraphCursor): List<WireGraphCursor> {
            val r = ArrayList<WireGraphCursor>()
            for (dir in intArrayOf(RIGHT, DOWN, LEFT, UP)) {
                if (cnx and dir != 0) r.add(point.copy().moveOneCell(dir))
            }
            return r
        }

        var point = startingPoint.copy()

        fun mark(point: WireGraphCursor) {
            wireSimMarked.add(point.longHash())
            oldTraversedNodes.add(point.copy())
            // do some signal action
            world.setWireEmitStateOf(point.x, point.y, wire, calculateDecay(signal, point.len, wire, signalType))
        }

        fun isMarked(point: WireGraphCursor) = wireSimMarked.contains(point.longHash())
        fun enq(point: WireGraphCursor) = wireSimPoints.addFirst(point.copy())
        fun deq() = wireSimPoints.removeLast()

        enq(point)
        mark(point)

        while (wireSimPoints.notEmpty()) {
            point = deq()
            // TODO if we found a power receiver, do something to it
            world.getWireGraphOf(point.x, point.y, wire)?.let { connections ->
                for (x in getAdjacent(connections, point)) {
                    if (!isMarked(x)) {
                        mark(x)
                        enq(x)
                    }
                }

                // do something with the power receiver
                val tilePoint = Point2i(point.x, point.y)
                var fixture = fixtureCache[tilePoint]
                var tileOffsetFromFixture: Point2i? = null
                if (fixture == null) {
                    INGAME.getActorsAt(point.x * TILE_SIZED, point.y * TILE_SIZED).filterIsInstance<Electric>().firstOrNull().let { found ->
                        if (found != null) {
                            // get offset from the fixture's origin
                            tileOffsetFromFixture = found.intTilewiseHitbox.let { Point2i(it.startX.toInt(), it.startY.toInt()) } - tilePoint

//                            println("$tilePoint; ${found.javaClass.canonicalName}, $tileOffsetFromFixture, ${found.getWireSinkAt(tileOffsetFromFixture!!)}")

                            if (found.getWireSinkAt(tileOffsetFromFixture!!) == emissionType) {
                                fixtureCache[tilePoint] = found to emissionType
                                fixture = found to emissionType
                            }
                        }
                    }
                }
                fixture?.first?.updateOnWireGraphTraversal(tileOffsetFromFixture!!.x, tileOffsetFromFixture!!.y, fixture!!.second)

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

    private fun getNearbyTilesPos8(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
            Point2i(x + 1, y),
            Point2i(x + 1, y + 1),
            Point2i(x, y + 1),
            Point2i(x - 1, y + 1),
            Point2i(x - 1, y),
            Point2i(x - 1, y - 1),
            Point2i(x, y - 1),
            Point2i(x + 1, y - 1)
        )
    }
    private fun getNearbyTiles(x: Int, y: Int): List<ItemID> {
        return getNearbyTilesPos(x, y).map { world.getTileFromTerrain(it.x, it.y) }
    }
    private fun getNearbyTiles8(x: Int, y: Int): List<ItemID> {
        return getNearbyTilesPos8(x, y).map { world.getTileFromTerrain(it.x, it.y) }
    }
    private fun getNearbyWalls(x: Int, y: Int): List<ItemID> {
        return getNearbyTilesPos(x, y).map { world.getTileFromWall(it.x, it.y) }
    }
    private fun getNearbyWalls8(x: Int, y: Int): List<ItemID> {
        return getNearbyTilesPos8(x, y).map { world.getTileFromWall(it.x, it.y) }
    }

    data class WireGraphCursor(
            var x: Int,
            var y: Int,
            var fromWhere: Int, //1: right, 2: down, 4: left, 8: up, 0: *shrug*
            var len: Int,
            var wire: ItemID
    ) {
        constructor(point2i: Point2i, wire: ItemID): this(point2i.x, point2i.y, 0, 0, wire)

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