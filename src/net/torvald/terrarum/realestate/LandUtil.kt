package net.torvald.terrarum.realestate

import net.torvald.terrarum.FactionCodex
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2016-03-27.
 */
object LandUtil {

    const val CHUNK_W = 90
    const val CHUNK_H = 90

    const val LAYER_TERR = 0
    const val LAYER_WALL = 1
    const val LAYER_ORES = 2
    const val LAYER_WIRE = 20
    const val LAYER_FLUID = 3

    fun toChunkNum(world: GameWorld, x: Int, y: Int): Long {
        // coercing and fmod-ing follows ROUNDWORLD rule. See: GameWorld.coerceXY()
        val (x, y) = world.coerceXY(x, y)
        return chunkXYtoChunkNum(world, x / CHUNK_W, y / CHUNK_H)
    }

    fun toChunkXY(world: GameWorld, x: Int, y: Int): Point2i {
        // coercing and fmod-ing follows ROUNDWORLD rule. See: GameWorld.coerceXY()
        val (x, y) = world.coerceXY(x, y)
        return Point2i(x / CHUNK_W, y / CHUNK_H)
    }

    fun chunkXYtoChunkNum(world: GameWorld, cx: Int, cy: Int): Long {
        val ch = (world.height / CHUNK_H).toLong()
        return cx * ch + cy
    }
    fun chunkNumToChunkXY(world: GameWorld, chunkNum: Long): Point2i {
        val ch = world.height / CHUNK_H
        return Point2i((chunkNum / ch).toInt(), (chunkNum % ch).toInt())
    }

    fun getBlockAddr(world: GameWorld, x: Int, y: Int): BlockAddress {
        // coercing and fmod-ing follows ROUNDWORLD rule. See: GameWorld.coerceXY()
        val (x, y) = world.coerceXY(x, y)
        return (world.width.toLong() * y) + x
    }

    fun resolveBlockAddr(world: GameWorld, t: BlockAddress): Point2i =
            Point2i((t % world.width).toInt(), (t / world.width).toInt())

    fun resolveBlockAddr(width: Int, t: BlockAddress): Point2i =
            Point2i((t % width).toInt(), (t / width).toInt())

    /**
     * Get owner ID as an Actor/Faction
     */
    fun resolveOwner(id: Int): Any =
            if (id >= 0)
                INGAME.getActorByID(id)
            else
                FactionCodex.getFactionByID(id)


}