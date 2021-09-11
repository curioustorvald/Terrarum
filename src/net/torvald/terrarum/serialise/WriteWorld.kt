package net.torvald.terrarum.serialise

import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.GameWorldTitleScreen
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.tvda.ByteArray64
import net.torvald.terrarum.tvda.ByteArray64Writer
import java.io.Reader

/**
 * Created by minjaesong on 2021-08-23.
 */
object WriteWorld {

    fun actorAcceptable(actor: Actor): Boolean {
        return actor.referenceID !in ReferencingRanges.ACTORS_WIRES &&
               actor.referenceID !in ReferencingRanges.ACTORS_WIRES_HELPER &&
               actor != (CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor)
    }

    private fun preWrite(ingame: TerrarumIngame, time_t: Long): GameWorld {
        val world = ingame.world
        val currentPlayTime_t = time_t - ingame.loadedTime_t

        world.genver = Common.GENVER
        world.comp = Common.COMP_GZIP
        world.lastPlayTime = time_t
        world.totalPlayTime += currentPlayTime_t

        val actorIDbuf = ArrayList<ActorID>()
        ingame.actorContainerActive.filter { actorAcceptable(it) }.forEach { actorIDbuf.add(it.referenceID) }
        ingame.actorContainerInactive.filter { actorAcceptable(it) }.forEach { actorIDbuf.add(it.referenceID) }

        world.actors.clear()
        world.actors.addAll(actorIDbuf.sorted().distinct())

        return world
    }

    operator fun invoke(ingame: TerrarumIngame, time_t: Long): String {
        return Common.jsoner.toJson(preWrite(ingame, time_t))
    }

    fun encodeToByteArray64(ingame: TerrarumIngame, time_t: Long): ByteArray64 {
        val baw = ByteArray64Writer(Common.CHARSET)

        Common.jsoner.toJson(preWrite(ingame, time_t), baw)
        baw.flush(); baw.close()

        return baw.toByteArray64()
    }

    /**
     * @return Gzipped chunk. Tile numbers are stored in Big Endian.
     */
    fun encodeChunk(layer: BlockLayer, cx: Int, cy: Int): ByteArray64 {
        val ba = ByteArray64()
        for (y in cy * LandUtil.CHUNK_H until (cy + 1) * LandUtil.CHUNK_H) {
            for (x in cx * LandUtil.CHUNK_W until (cx + 1) * LandUtil.CHUNK_W) {
                val tilenum = layer.unsafeGetTile(x, y)
                ba.add(tilenum.ushr(8).and(255).toByte())
                ba.add(tilenum.and(255).toByte())
            }
        }

        return Common.zip(ba)
    }
}


/**
 * Created by minjaesong on 2021-08-25.
 */
object ReadWorld {

    fun readLayerFormat(worldDataStream: Reader): GameWorld =
            fillInDetails(Common.jsoner.fromJson(GameWorldTitleScreen::class.java, worldDataStream))

    operator fun invoke(worldDataStream: Reader): GameWorld =
            fillInDetails(Common.jsoner.fromJson(GameWorld::class.java, worldDataStream))

    private fun fillInDetails(world: GameWorld): GameWorld {
        world.tileNumberToNameMap.forEach { l, s ->
            world.tileNameToNumberMap[s] = l.toInt()
        }

        return world
    }

    fun readWorldAndSetNewWorld(ingame: TerrarumIngame, worldDataStream: Reader): GameWorld {
        val world = readLayerFormat(worldDataStream)
        ingame.world = world
        return world
    }

    private val cw = LandUtil.CHUNK_W
    private val ch = LandUtil.CHUNK_H

    fun decodeChunkToLayer(chunk: ByteArray64, targetLayer: BlockLayer, cx: Int, cy: Int) {
        val bytes = Common.unzip(chunk)
        if (bytes.size != cw * ch * 2L)
            throw UnsupportedOperationException("Chunk size mismatch: decoded chunk size is ${bytes.size} bytes " +
                                                "where ${LandUtil.CHUNK_W * LandUtil.CHUNK_H * 2L} bytes (Int16 of ${LandUtil.CHUNK_W}x${LandUtil.CHUNK_H}) were expected")

        for (k in 0 until cw * ch) {
            val tilenum = bytes[2L*k].toUint().shl(8) or bytes[2L*k + 1].toUint()
            val offx = k % cw
            val offy = k / cw

//            try {
                targetLayer.unsafeSetTile(cx * cw + offx, cy * ch + offy, tilenum)
//            }
//            catch (e: IndexOutOfBoundsException) {
//                printdbgerr(this, "IndexOutOfBoundsException, cx = $cx, cy = $cy, k = $k, offx = $offx, offy = $offy")
//                throw e
//            }
        }
    }

}