package net.torvald.terrarum.serialise

import net.torvald.terrarum.App
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.tvda.*
import net.torvald.terrarum.weather.WeatherMixer

/**
 * Created by minjaesong on 2021-08-23.
 */
object WriteMeta {

    operator fun invoke(ingame: TerrarumIngame, time_t: Long): String {
        val world = ingame.world
        val currentPlayTime_t = time_t - ingame.loadedTime_t

        val meta = WorldMeta(
                genver = Common.GENVER,
                savename = world.worldName,
                randseed0 = RoguelikeRandomiser.RNG.state0,
                randseed1 = RoguelikeRandomiser.RNG.state1,
                weatseed0 = WeatherMixer.RNG.state0,
                weatseed1 = WeatherMixer.RNG.state1,
                playerid = ingame.actorGamer.referenceID,
                creation_t = ingame.creationTime,
                lastplay_t = time_t,
                playtime_t = ingame.totalPlayTime + currentPlayTime_t,
                loadorder = ModMgr.loadOrder.toTypedArray(),
                worlds = ingame.gameworldIndices.toTypedArray()
        )

        return Common.jsoner.toJson(meta)
    }

    fun encodeToByteArray64(ingame: TerrarumIngame, time_t: Long): ByteArray64 {
        val ba = ByteArray64()
        this.invoke(ingame, time_t).toByteArray(Common.CHARSET).forEach { ba.add(it) }
        return ba
    }

    data class WorldMeta(
            val genver: Int = -1,
            val savename: String = "",
            val randseed0: Long = 0,
            val randseed1: Long = 0,
            val weatseed0: Long = 0,
            val weatseed1: Long = 0,
            val playerid: ActorID = 0,
            val creation_t: Long = 0,
            val lastplay_t: Long = 0,
            val playtime_t: Long = 0,
            val loadorder: Array<String> = arrayOf(), // do not use list; Could not instantiate instance of class: java.util.Collections$SingletonList
            val worlds: Array<Int> = arrayOf() // do not use list; Could not instantiate instance of class: java.util.Collections$SingletonList
    ) {

        override fun equals(other: Any?): Boolean {
            throw UnsupportedOperationException()
        }
    }

    private fun modnameToOrnamentalHeader(s: String) =
            "\n\n${"#".repeat(16 + s.length)}\n" +
            "##  module: $s  ##\n" +
            "${"#".repeat(16 + s.length)}\n\n"
}


/**
 * Created by minjaesong on 2021-09-03.
 */
object ReadMeta {

    operator fun invoke(savefile: VirtualDisk): WriteMeta.WorldMeta {
        val metaFile = savefile.entries[-1]!!
        val metaReader = ByteArray64Reader((metaFile.contents as EntryFile).getContent(), Common.CHARSET)
        return Common.jsoner.fromJson(WriteMeta.WorldMeta::class.java, metaReader)
    }

    fun fromDiskEntry(metaFile: DiskEntry): WriteMeta.WorldMeta {
        val metaReader = ByteArray64Reader((metaFile.contents as EntryFile).getContent(), Common.CHARSET)
        return Common.jsoner.fromJson(WriteMeta.WorldMeta::class.java, metaReader)
    }

}