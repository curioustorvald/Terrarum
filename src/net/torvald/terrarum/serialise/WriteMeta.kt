package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.compression.Lzma
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64InputStream
import net.torvald.terrarum.weather.WeatherMixer
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-08-23.
 */
open class WriteMeta(val ingame: TerrarumIngame) {

    open fun invoke(): String {
        val world = ingame.world
        
        val json = """{
"genver": ${Common.GENVER},
"savename": "${world.worldName}",
"terrseed": ${world.generatorSeed},
"randseed0": ${RoguelikeRandomiser.RNG.state0},
"randseed1": ${RoguelikeRandomiser.RNG.state1},
"weatseed0": ${WeatherMixer.RNG.state0},
"weatseed1": ${WeatherMixer.RNG.state1},
"playerid": ${ingame.actorGamer.referenceID},
"creation_t": ${world.creationTime},
"lastplay_t": ${world.lastPlayTime},
"playtime_t": ${world.totalPlayTime},
"blocks": "${StringBuilder().let {
    ModMgr.getFilesFromEveryMod("blocks/blocks.csv").forEach { (modname, file) ->
        it.append("\n\n## module: $modname ##\n\n")
        it.append(file.readText())
    }
    zipStrAndEnascii(it.toString())
}}",
"items": "${StringBuilder().let {
    ModMgr.getFilesFromEveryMod("items/itemid.csv").forEach { (modname, file) ->
        it.append("\n\n## module: $modname ##\n\n")
        it.append(file.readText())
    }
    zipStrAndEnascii(it.toString())
}}",
"wires": "${StringBuilder().let {
    ModMgr.getFilesFromEveryMod("wires/wires.csv").forEach { (modname, file) ->
        it.append("\n\n## module: $modname ##\n\n")
        it.append(file.readText())
    }
    zipStrAndEnascii(it.toString())
}}",
"materials": "${StringBuilder().let {
    ModMgr.getFilesFromEveryMod("materials/materials.csv").forEach { (modname, file) ->
        it.append("\n\n## module: $modname ##\n\n")
        it.append(file.readText())
    }
    zipStrAndEnascii(it.toString())
}}",
"loadorder": [${ModMgr.loadOrder.map { "\"${it}\"" }.joinToString()}],
"worlds": [${ingame.gameworldIndices.joinToString()}]
}"""
        
        return json
    }

    fun encodeToByteArray64(): ByteArray64 {
        val ba = ByteArray64()
        this.invoke().toByteArray(Common.CHARSET).forEach { ba.add(it) }
        return ba
    }

    data class WorldMeta(
            val genver: Int,
            val savename: String
    )

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

