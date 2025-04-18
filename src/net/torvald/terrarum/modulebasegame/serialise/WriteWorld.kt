package net.torvald.terrarum.modulebasegame.serialise

import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.NoSerialise
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.savegame.ByteArray64
import net.torvald.terrarum.savegame.ByteArray64Writer
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.utils.PlayerLastStatus
import net.torvald.terrarum.weather.WeatherMixer
import java.io.File
import java.io.Reader

/**
 * Created by minjaesong on 2021-08-23.
 */
object WriteWorld {

    fun actorAcceptable(actor: Actor): Boolean {
        return actor !is NoSerialise // IngamePlayers is also NoSerialised because they must not be saved with the world
    }

    private fun preWrite(ingame: TerrarumIngame, time_t: Long, actorsList: List<Actor>, playersList: List<IngamePlayer>): GameWorld {
        val world = ingame.world
        val currentPlayTime_t = time_t - ingame.loadedTime_t

        world.comp = Common.getCompIndex()
        world.lastPlayTime = time_t
        world.totalPlayTime += currentPlayTime_t

        world.actors.clear()
        world.actors.addAll(actorsList.map { it.referenceID }.sorted().distinct())

        world.randSeeds[0] = RoguelikeRandomiser.RNG.state0
        world.randSeeds[1] = RoguelikeRandomiser.RNG.state1
        world.randSeeds[2] = WeatherMixer.RNG.state0
        world.randSeeds[3] = WeatherMixer.RNG.state1

        // record all player's last position
        playersList.forEach {
            world.playersLastStatus.put(it.uuid.toString(), PlayerLastStatus(it, ingame.isMultiplayer))
        }

        return world
    }

    // genver must be found on fixed location of the JSON string
    operator fun invoke(oldGenVer: Long?, ingame: TerrarumIngame, time_t: Long, actorsList: List<Actor>, playersList: List<IngamePlayer>): String {
        val s = Common.jsoner.toJson(preWrite(ingame, time_t, actorsList, playersList))
        return """{"genver":${oldGenVer ?: Common.GENVER},${s.substring(1)}"""
    }

    fun encodeToByteArray64(oldGenVer: Long?, ingame: TerrarumIngame, time_t: Long, actorsList: List<Actor>, playersList: List<IngamePlayer>): ByteArray64 {
        val baw = ByteArray64Writer(Common.CHARSET)

        val header = """{"genver":${oldGenVer ?: Common.GENVER}"""
        baw.write(header)
        Common.jsoner.toJson(preWrite(ingame, time_t, actorsList, playersList), baw)
        baw.flush(); baw.close()
        // by this moment, contents of the baw will be:
        //  {"genver":123456{"actorValue":{},......}
        //  (note that first bracket is not closed, and another open bracket after "genver" property)
        // and we want to turn it into this:
        //  {"genver":123456,"actorValue":{},......}
        val ba = baw.toByteArray64()
        ba[header.toByteArray(Common.CHARSET).size.toLong()] = ','.code.toByte()

        return ba
    }

    /**
     * @return Gzipped chunk. Tile numbers are stored in Big Endian.
     */
    fun encodeChunk(layer: BlockLayer, cx: Int, cy: Int): ByteArray64 {
        val ba = ByteArray64()
        for (y in cy * LandUtil.CHUNK_H until (cy + 1) * LandUtil.CHUNK_H) {
            for (x in cx * LandUtil.CHUNK_W until (cx + 1) * LandUtil.CHUNK_W) {
                ba.appendBytes(layer.unsafeToBytes(x, y))
            }
        }

        return Common.zip(ba)
    }
}


/**
 * Created by minjaesong on 2021-08-25.
 */
object ReadWorld {

    operator fun invoke(worldDataStream: Reader, origin: File?): GameWorld =
        Common.jsoner.fromJson(GameWorld::class.java, worldDataStream).also {
            fillInDetails(origin, it)
        }

    private fun fillInDetails(origin: File?, world: GameWorld) {
        world.tileNumberToNameMap.forEach { l, s ->
            world.tileNameToNumberMap[s] = l.toInt()
        }

        ItemCodex.loadFromSave(origin, world.dynamicToStaticTable, world.dynamicItemInventory)
    }

    private val cw = LandUtil.CHUNK_W
    private val ch = LandUtil.CHUNK_H

    fun decodeChunkToLayer(chunk: ByteArray64, targetLayer: BlockLayer, cx: Int, cy: Int) {
        val bytes = Common.unzip(chunk)
        if (bytes.size != cw * ch * targetLayer.bytesPerBlock)
            throw UnsupportedOperationException("Chunk size mismatch: decoded chunk size is ${bytes.size} bytes " +
                                                "where ${LandUtil.CHUNK_W * LandUtil.CHUNK_H * targetLayer.bytesPerBlock} bytes (Int${8 * targetLayer.bytesPerBlock} of ${LandUtil.CHUNK_W}x${LandUtil.CHUNK_H}) were expected")

        for (k in 0 until cw * ch) {
            val offx = k % cw
            val offy = k / cw
            val ba = ByteArray(targetLayer.bytesPerBlock.toInt()) {
                bytes[targetLayer.bytesPerBlock * k + it]
            }

//            try {
                targetLayer.unsafeSetTile(cx * cw + offx, cy * ch + offy, ba)
//            }
//            catch (e: IndexOutOfBoundsException) {
//                printdbgerr(this, "IndexOutOfBoundsException, cx = $cx, cy = $cy, k = $k, offx = $offx, offy = $offy")
//                throw e
//            }
        }
    }

}