package net.torvald.terrarum.serialise

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64Reader
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.EntryFile
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VirtualDisk
import net.torvald.terrarum.utils.MetaModuleCSVPair
import net.torvald.terrarum.utils.ZipCodedStr
import net.torvald.terrarum.weather.WeatherMixer
import java.io.StringReader

/**
 * Created by minjaesong on 2021-08-23.
 */
object WriteMeta {

    operator fun invoke(ingame: TerrarumIngame, currentPlayTime_t: Long): String {
        val world = ingame.world

        val meta = WorldMeta(
                genver = Common.GENVER,
                savename = world.worldName,
                terrseed = world.generatorSeed,
                randseed0 = RoguelikeRandomiser.RNG.state0,
                randseed1 = RoguelikeRandomiser.RNG.state1,
                weatseed0 = WeatherMixer.RNG.state0,
                weatseed1 = WeatherMixer.RNG.state1,
                playerid = ingame.actorGamer.referenceID,
                creation_t = world.creationTime,
                lastplay_t = world.lastPlayTime,
                playtime_t = world.totalPlayTime + currentPlayTime_t,
                blocks = ModMgr.getFilesFromEveryMod("blocks/blocks.csv").fold(MetaModuleCSVPair()) {
                    map, (modname, file) ->
                    map[modname] = ZipCodedStr(file.readText ())
                    /*return*/map
                },
                items = ModMgr.getFilesFromEveryMod("items/itemid.csv").fold(MetaModuleCSVPair()) {
                    map, (modname, file) ->
                    map[modname] = ZipCodedStr(file.readText ())
                    /*return*/map
                },
                wires = ModMgr.getFilesFromEveryMod("wires/wires.csv").fold(MetaModuleCSVPair()) {
                    map, (modname, file) ->
                    map[modname] = ZipCodedStr(file.readText ())
                    /*return*/map
                },
                materials = ModMgr.getFilesFromEveryMod("materials/materials.csv").fold(MetaModuleCSVPair()) {
                    map, (modname, file) ->
                    map[modname] = ZipCodedStr(file.readText ())
                    /*return*/map
                },
                loadorder = ModMgr.loadOrder.toTypedArray(),
                worlds = ingame.gameworldIndices.toTypedArray()
        )

        return Common.jsoner.toJson(meta)
    }

    fun encodeToByteArray64(ingame: TerrarumIngame, currentPlayTime_t: Long): ByteArray64 {
        val ba = ByteArray64()
        this.invoke(ingame, currentPlayTime_t).toByteArray(Common.CHARSET).forEach { ba.add(it) }
        return ba
    }

    data class WorldMeta(
            val genver: Int = -1,
            val savename: String = "",
            val terrseed: Long = 0,
            val randseed0: Long = 0,
            val randseed1: Long = 0,
            val weatseed0: Long = 0,
            val weatseed1: Long = 0,
            val playerid: ActorID = 0,
            val creation_t: Long = 0,
            val lastplay_t: Long = 0,
            val playtime_t: Long = 0,
            val blocks: MetaModuleCSVPair = MetaModuleCSVPair(),
            val items: MetaModuleCSVPair = MetaModuleCSVPair(),
            val wires: MetaModuleCSVPair = MetaModuleCSVPair(),
            val materials: MetaModuleCSVPair = MetaModuleCSVPair(),
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

}