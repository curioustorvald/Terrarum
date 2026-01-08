package net.torvald.terrarum.gameworld

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.WireCodex
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.TerrarumIngame.Companion.inUpdateRange
import net.torvald.terrarum.modulebasegame.gameactors.Electric
import net.torvald.terrarum.modulebasegame.gameactors.WireEmissionType
import net.torvald.terrarum.realestate.LandUtil
import org.dyn4j.geometry.Vector2
import kotlin.math.pow

/**
 * Two-layer wire simulation model: Logical graph for signal propagation.
 *
 * The logical graph separates "what changes per tile" (visual brightness) from
 * "what doesn't" (connectivity topology, signal evaluation). This reduces
 * simulation complexity from O(wire_tiles) to O(logical_nodes).
 *
 * Logical nodes are:
 * - Signal sources (fixtures with wireEmitterTypes)
 * - Signal sinks (fixtures with wireSinkTypes)
 * - Junctions (wire tiles with 3+ connections)
 *
 * Wire segments connect logical nodes. Signal propagates along segments with
 * decay calculated per-segment, not per-tile.
 *
 * Created for Terrarum wire simulation refactoring.
 *
 * Created by minjaesong and Claude on 2026-01-08.
 */

typealias BlockBoxIndex = Int

/**
 * Represents a node in the logical wire graph.
 * Nodes are: signal sources, signal sinks, and junctions (3+ connections).
 */
sealed class LogicalWireNode {
    abstract val position: Point2i
    abstract val wireType: ItemID
    abstract var signalStrength: Vector2

    /** Connected segments (populated during graph building) */
    @Transient
    val connectedSegments: MutableList<WireSegment> = mutableListOf()

    /**
     * Fixture-backed node (source or sink).
     * References the Electric fixture that emits or receives signals.
     */
    data class FixtureNode(
        override val position: Point2i,
        override val wireType: ItemID,
        val fixtureRef: Electric,
        val blockBoxIndex: BlockBoxIndex,
        val isEmitter: Boolean,
        val emissionType: WireEmissionType
    ) : LogicalWireNode() {
        override var signalStrength: Vector2 = Vector2(0.0, 0.0)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FixtureNode) return false
            return position == other.position && wireType == other.wireType &&
                    blockBoxIndex == other.blockBoxIndex && isEmitter == other.isEmitter
        }

        override fun hashCode(): Int {
            var result = position.hashCode()
            result = 31 * result + wireType.hashCode()
            result = 31 * result + blockBoxIndex
            result = 31 * result + isEmitter.hashCode()
            return result
        }
    }

    /**
     * Junction node where 3+ wire segments meet.
     * Acts as a signal relay point with potential splitting/merging.
     */
    data class JunctionNode(
        override val position: Point2i,
        override val wireType: ItemID,
        val connectionCount: Int  // 3 for T-junction, 4 for cross
    ) : LogicalWireNode() {
        override var signalStrength: Vector2 = Vector2(0.0, 0.0)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is JunctionNode) return false
            return position == other.position && wireType == other.wireType
        }

        override fun hashCode(): Int {
            var result = position.hashCode()
            result = 31 * result + wireType.hashCode()
            return result
        }
    }
}

/**
 * Represents a wire segment connecting two logical nodes.
 * Signal decays along the segment based on length.
 *
 * Replaces N tile nodes with a single edge, enabling O(1) signal propagation
 * across an entire wire run.
 */
data class WireSegment(
    val wireType: ItemID,
    var startNode: LogicalWireNode,
    var endNode: LogicalWireNode,
    val length: Int,
    val tilePositions: List<Point2i>,
    var startStrength: Double = 0.0,
    var endStrength: Double = 0.0,
    val decayConstant: Double = 1.0
) {
    /**
     * Parametric brightness evaluation - O(1) per tile.
     * @param offset Distance from start node (0 to length)
     * @return Signal strength at that offset
     */
    fun getBrightnessAtOffset(offset: Int): Double {
        if (offset < 0 || offset > length) return 0.0
        // Calculate from whichever end has the stronger signal
        val fromStart = startStrength * decayConstant.pow(offset.toDouble())
        val fromEnd = endStrength * decayConstant.pow((length - offset).toDouble())
        return maxOf(fromStart, fromEnd).coerceAtLeast(0.0)
    }

    /**
     * Get the offset of a tile position within this segment.
     * @return Offset index, or null if position not in segment
     */
    fun getOffsetForPosition(pos: Point2i): Int? {
        val idx = tilePositions.indexOfFirst { it.x == pos.x && it.y == pos.y }
        return if (idx >= 0) idx else null
    }

    /**
     * Get the node at the other end of this segment.
     */
    fun getOtherEnd(node: LogicalWireNode): LogicalWireNode {
        return if (node === startNode || node == startNode) endNode else startNode
    }
}

/**
 * Complete logical wire graph for a world.
 * Stored per wire-type for efficiency.
 */
class LogicalWireGraph(private val world: GameWorld) {

    companion object {
        private const val RIGHT = 1
        private const val DOWN = 2
        private const val LEFT = 4
        private const val UP = 8

        private val DIRECTIONS = intArrayOf(RIGHT, DOWN, LEFT, UP)
        private val DIRECTION_OFFSETS = mapOf(
            RIGHT to Point2i(1, 0),
            DOWN to Point2i(0, 1),
            LEFT to Point2i(-1, 0),
            UP to Point2i(0, -1)
        )

        private fun Int.wireNodeMirror(): Int = when (this) {
            RIGHT -> LEFT
            DOWN -> UP
            LEFT -> RIGHT
            UP -> DOWN
            else -> 0
        }
    }

    /**
     * Graph data for a single wire type.
     */
    data class WireTypeGraph(
        val nodes: MutableList<LogicalWireNode> = mutableListOf(),
        val segments: MutableList<WireSegment> = mutableListOf(),
        val positionToSegment: HashMap<BlockAddress, WireSegment> = HashMap(),
        val positionToNode: HashMap<BlockAddress, LogicalWireNode> = HashMap(),
        var dirty: Boolean = true,
        var structureDirty: Boolean = true  // Set when fixtures are added/removed; requires full rebuild
    ) {
        fun clear() {
            nodes.forEach { it.connectedSegments.clear() }
            nodes.clear()
            segments.clear()
            positionToSegment.clear()
            positionToNode.clear()
            dirty = true
            structureDirty = false  // We're about to rebuild, so structure will be fresh
        }
    }

    private val graphs = HashMap<ItemID, WireTypeGraph>()

    fun getGraph(wireType: ItemID): WireTypeGraph? = graphs[wireType]

    fun getOrCreateGraph(wireType: ItemID): WireTypeGraph {
        return graphs.getOrPut(wireType) { WireTypeGraph() }
    }

    fun markDirty(wireType: ItemID) {
        graphs[wireType]?.dirty = true
    }

    fun markAllDirty() {
        graphs.values.forEach { it.dirty = true }
    }

    /**
     * Mark all graphs as needing structural rebuild.
     * Called when Electric fixtures are spawned or despawned.
     */
    fun markAllStructureDirty() {
        graphs.values.forEach { it.structureDirty = true }
    }

    /**
     * Rebuild the logical graph for a specific wire type.
     * Scans all wire tiles and fixtures to construct nodes and segments.
     */
    fun rebuild(wireType: ItemID) {
        val graph = getOrCreateGraph(wireType)
        graph.clear()

        val emissionType = WireCodex[wireType].accepts
        val decayConstant = WireCodex.wireDecays[wireType] ?: 1.0

        // Step 1: Find all logical nodes

        // 1a: Fixture nodes (emitters and sinks)
        INGAME.actorContainerActive.filterIsInstance<Electric>().forEach { fixture ->
            if (!fixture.inUpdateRange(world)) return@forEach

            // Emitter ports
            fixture.wireEmitterTypes.forEach { (bbi, wireEmissionType) ->
                if (wireEmissionType == emissionType) {
                    val pos = fixture.worldBlockPos!! + fixture.blockBoxIndexToPoint2i(bbi)
                    // Only add if there's actually a wire at this position
                    if (hasWireAt(pos.x, pos.y, wireType)) {
                        val node = LogicalWireNode.FixtureNode(
                            position = pos,
                            wireType = wireType,
                            fixtureRef = fixture,
                            blockBoxIndex = bbi,
                            isEmitter = true,
                            emissionType = wireEmissionType
                        )
                        graph.nodes.add(node)
                        graph.positionToNode[LandUtil.getBlockAddr(world, pos.x, pos.y)] = node
                    }
                }
            }

            // Sink ports
            fixture.wireSinkTypes.forEach { (bbi, wireSinkType) ->
                if (wireSinkType == emissionType) {
                    val pos = fixture.worldBlockPos!! + fixture.blockBoxIndexToPoint2i(bbi)
                    if (hasWireAt(pos.x, pos.y, wireType)) {
                        val blockAddr = LandUtil.getBlockAddr(world, pos.x, pos.y)
                        // Don't add duplicate node if already an emitter at same position
                        if (!graph.positionToNode.containsKey(blockAddr)) {
                            val node = LogicalWireNode.FixtureNode(
                                position = pos,
                                wireType = wireType,
                                fixtureRef = fixture,
                                blockBoxIndex = bbi,
                                isEmitter = false,
                                emissionType = wireSinkType
                            )
                            graph.nodes.add(node)
                            graph.positionToNode[blockAddr] = node
                        }
                    }
                }
            }
        }

        // 1b: Junction nodes (tiles with 3+ connections)
        world.wirings.forEach { (blockAddr, wiringNode) ->
            if (wiringNode.ws.contains(wireType)) {
                val cnx = world.getWireGraphUnsafe(blockAddr, wireType) ?: 0
                val connectionCount = Integer.bitCount(cnx)

                if (connectionCount >= 3) {
                    val x = (blockAddr % world.width).toInt()
                    val y = (blockAddr / world.width).toInt()

                    // Don't create junction if there's already a fixture node here
                    if (!graph.positionToNode.containsKey(blockAddr)) {
                        val node = LogicalWireNode.JunctionNode(
                            position = Point2i(x, y),
                            wireType = wireType,
                            connectionCount = connectionCount
                        )
                        graph.nodes.add(node)
                        graph.positionToNode[blockAddr] = node
                    }
                }
            }
        }

        // Step 2: Trace segments between nodes
        val visitedEdges = HashSet<Pair<BlockAddress, Int>>()  // (position, direction)

        graph.nodes.forEach { startNode ->
            val startAddr = LandUtil.getBlockAddr(world, startNode.position.x, startNode.position.y)
            val cnx = world.getWireGraphUnsafe(startAddr, wireType) ?: 0

            for (dir in DIRECTIONS) {
                if (cnx and dir != 0) {
                    val edgeKey = startAddr to dir
                    if (visitedEdges.contains(edgeKey)) continue

                    // Trace path in this direction
                    val segment = tracePath(startNode, dir, wireType, decayConstant, graph.positionToNode)

                    if (segment != null) {
                        graph.segments.add(segment)
                        startNode.connectedSegments.add(segment)
                        segment.endNode.connectedSegments.add(segment)

                        // Mark all tiles in segment
                        segment.tilePositions.forEach { pos ->
                            val addr = LandUtil.getBlockAddr(world, pos.x, pos.y)
                            graph.positionToSegment[addr] = segment
                        }

                        // Mark both directions as visited
                        val endAddr = LandUtil.getBlockAddr(world, segment.endNode.position.x, segment.endNode.position.y)
                        visitedEdges.add(startAddr to dir)
                        visitedEdges.add(endAddr to dir.wireNodeMirror())
                    }
                }
            }
        }

        // Handle orphan wire tiles (connected to nothing) - create single-tile segments
        world.wirings.forEach { (blockAddr, wiringNode) ->
            if (wiringNode.ws.contains(wireType) && !graph.positionToSegment.containsKey(blockAddr)) {
                val x = (blockAddr % world.width).toInt()
                val y = (blockAddr / world.width).toInt()
                val pos = Point2i(x, y)

                // Create a degenerate segment for orphan tiles
                val orphanNode = LogicalWireNode.JunctionNode(pos, wireType, 0)
                val segment = WireSegment(
                    wireType = wireType,
                    startNode = orphanNode,
                    endNode = orphanNode,
                    length = 0,
                    tilePositions = listOf(pos),
                    decayConstant = decayConstant
                )
                graph.segments.add(segment)
                graph.positionToSegment[blockAddr] = segment
            }
        }

        graph.dirty = true
    }

    /**
     * Trace a wire path from a node in a given direction until hitting another node.
     */
    private fun tracePath(
        startNode: LogicalWireNode,
        initialDirection: Int,
        wireType: ItemID,
        decayConstant: Double,
        positionToNode: HashMap<BlockAddress, LogicalWireNode>
    ): WireSegment? {
        val path = mutableListOf<Point2i>()
        path.add(startNode.position)

        var currentPos = startNode.position
        var direction = initialDirection

        // Move to first tile in the direction
        val offset = DIRECTION_OFFSETS[direction] ?: return null
        currentPos = Point2i(currentPos.x + offset.x, currentPos.y + offset.y)

        val maxIterations = 100000  // Safety limit
        var iterations = 0

        while (iterations++ < maxIterations) {
            val blockAddr = LandUtil.getBlockAddr(world, currentPos.x, currentPos.y)

            // Check if we've reached another logical node
            val endNode = positionToNode[blockAddr]
            if (endNode != null && endNode !== startNode) {
                path.add(currentPos)
                return WireSegment(
                    wireType = wireType,
                    startNode = startNode,
                    endNode = endNode,
                    length = path.size - 1,  // Length is number of edges, not vertices
                    tilePositions = path.toList(),
                    decayConstant = decayConstant
                )
            }

            // Check if wire exists here
            val cnx = world.getWireGraphUnsafe(blockAddr, wireType)
            if (cnx == null || cnx == 0) {
                // Dead end - create endpoint node
                val deadEndNode = LogicalWireNode.JunctionNode(
                    position = Point2i(currentPos.x, currentPos.y),
                    wireType = wireType,
                    connectionCount = 1
                )
                // Note: we don't add dead end nodes to the main node list
                return WireSegment(
                    wireType = wireType,
                    startNode = startNode,
                    endNode = deadEndNode,
                    length = path.size,
                    tilePositions = path.toList() + listOf(currentPos),
                    decayConstant = decayConstant
                )
            }

            path.add(currentPos)

            // Find the exit direction (not the direction we came from)
            val entryDir = direction.wireNodeMirror()
            var exitDir: Int? = null

            for (dir in DIRECTIONS) {
                if (dir != entryDir && (cnx and dir) != 0) {
                    // Check if target is connected back
                    val nextOffset = DIRECTION_OFFSETS[dir] ?: continue
                    val nextPos = Point2i(currentPos.x + nextOffset.x, currentPos.y + nextOffset.y)
                    val nextCnx = world.getWireGraphOf(nextPos.x, nextPos.y, wireType) ?: 0

                    if ((nextCnx and dir.wireNodeMirror()) != 0) {
                        exitDir = dir
                        break
                    }
                }
            }

            if (exitDir == null) {
                // Dead end
                return WireSegment(
                    wireType = wireType,
                    startNode = startNode,
                    endNode = LogicalWireNode.JunctionNode(currentPos, wireType, 1),
                    length = path.size - 1,
                    tilePositions = path.toList(),
                    decayConstant = decayConstant
                )
            }

            // Move to next tile
            val nextOffset = DIRECTION_OFFSETS[exitDir]!!
            currentPos = Point2i(currentPos.x + nextOffset.x, currentPos.y + nextOffset.y)
            direction = exitDir
        }

        return null  // Safety: exceeded max iterations
    }

    private fun hasWireAt(x: Int, y: Int, wireType: ItemID): Boolean {
        val blockAddr = LandUtil.getBlockAddr(world, x, y)
        return world.wirings[blockAddr]?.ws?.contains(wireType) == true
    }

    /**
     * Rebuild graphs for all wire types present in the world.
     */
    fun rebuildAll() {
        // Collect all wire types
        val wireTypes = HashSet<ItemID>()
        world.wirings.values.forEach { wiringNode ->
            wireTypes.addAll(wiringNode.ws)
        }

        wireTypes.forEach { wireType ->
            rebuild(wireType)
        }
    }
}
