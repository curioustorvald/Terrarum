package net.torvald.terrarum.serialise

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64Reader
import net.torvald.terrarum.weather.WeatherMixer
import java.io.StringReader

/**
 * Created by minjaesong on 2021-08-23.
 */
open class WriteMeta(val ingame: TerrarumIngame) {

    open fun invoke(): String {
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
                playtime_t = world.totalPlayTime,
                blocks = StringBuilder().let {
                    ModMgr.getFilesFromEveryMod("blocks/blocks.csv").forEach { (modname, file) ->
                        it.append(modnameToOrnamentalHeader(modname))
                        it.append(file.readText())
                    }
                    zipStrAndEnascii(it.toString())
                },
                items = StringBuilder().let {
                    ModMgr.getFilesFromEveryMod("items/itemid.csv").forEach { (modname, file) ->
                        it.append(modnameToOrnamentalHeader(modname))
                        it.append(file.readText())
                    }
                    zipStrAndEnascii(it.toString())
                },
                wires = StringBuilder().let {
                    ModMgr.getFilesFromEveryMod("wires/wires.csv").forEach { (modname, file) ->
                        it.append(modnameToOrnamentalHeader(modname))
                        it.append(file.readText())
                    }
                    zipStrAndEnascii(it.toString())
                },
                materials = StringBuilder().let {
                    ModMgr.getFilesFromEveryMod("materials/materials.csv").forEach { (modname, file) ->
                        it.append(modnameToOrnamentalHeader(modname))
                        it.append(file.readText())
                    }
                    zipStrAndEnascii(it.toString())
                },
                loadorder = ModMgr.loadOrder.toTypedArray(),
                worlds = ingame.gameworldIndices.toTypedArray()
        )

        return Common.jsoner.toJson(meta)
    }

    fun encodeToByteArray64(): ByteArray64 {
        val ba = ByteArray64()
        this.invoke().toByteArray(Common.CHARSET).forEach { ba.add(it) }
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
            val blocks: String = "",
            val items: String = "",
            val wires: String = "",
            val materials: String = "",
            val loadorder: Array<String> = arrayOf(), // do not use list; Could not instantiate instance of class: java.util.Collections$SingletonList
            val worlds: Array<Int> = arrayOf() // do not use list; Could not instantiate instance of class: java.util.Collections$SingletonList
    ) {
        override fun equals(other: Any?): Boolean {
            throw UnsupportedOperationException()
        }
    }

    companion object {
        private fun modnameToOrnamentalHeader(s: String) =
                "\n\n${"#".repeat(16 + s.length)}\n" +
                "##  module: $s  ##\n" +
                "${"#".repeat(16 + s.length)}\n\n"

        /**
         * @param [s] a String
         * @return UTF-8 encoded [s] which are GZip'd then Ascii85-encoded
         */
        fun zipStrAndEnascii(s: String): String {
            return Common.bytesToZipdStr(s.toByteArray(Common.CHARSET).iterator())
        }

        fun unasciiAndUnzipStr(s: String): String {
            return ByteArray64Reader(Common.strToBytes(StringReader(s)), Common.CHARSET).readText()
        }
    }
}

