package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.compression.Lzma
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import net.torvald.terrarum.weather.WeatherMixer
import java.io.ByteArrayInputStream
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
    bytesToZipdStr(it.toString().toByteArray(Common.CHARSET))
}}",
"items": "${StringBuilder().let {
    ModMgr.getFilesFromEveryMod("items/itemid.csv").forEach { (modname, file) ->
        it.append("\n\n## module: $modname ##\n\n")
        it.append(file.readText())
    }
    bytesToZipdStr(it.toString().toByteArray(Common.CHARSET))
}}",
"wires": "${StringBuilder().let {
    ModMgr.getFilesFromEveryMod("wires/wires.csv").forEach { (modname, file) ->
        it.append("\n\n## module: $modname ##\n\n")
        it.append(file.readText())
    }
    bytesToZipdStr(it.toString().toByteArray(Common.CHARSET))
}}",
"materials": "${StringBuilder().let {
    ModMgr.getFilesFromEveryMod("materials/materials.csv").forEach { (modname, file) ->
        it.append("\n\n## module: $modname ##\n\n")
        it.append(file.readText())
    }
    bytesToZipdStr(it.toString().toByteArray(Common.CHARSET))
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
}

/**
 * @param b a ByteArray
 * @return Bytes in [b] which are GZip'd then Ascii85-encoded
 */
fun bytesToZipdStr(b: ByteArray): String {
    val sb = StringBuilder()
    val bo = ByteArray64GrowableOutputStream()
    val zo = GZIPOutputStream(bo)

    b.forEach {
        zo.write(it.toInt())
    }
    zo.flush(); zo.close()

    val ba = bo.toByteArray64()
    var bai = 0
    val buf = IntArray(4) { Ascii85.PAD_BYTE }
    ba.forEach {
        if (bai > 0 && bai % 4 == 0) {
            sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))
            buf.fill(Ascii85.PAD_BYTE)
        }

        buf[bai % 4] = it.toInt() and 255

        bai += 1
    }; sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))

    return sb.toString()
}

/**
 * @param b a ByteArray
 * @return Bytes in [b] which are LZMA'd then Ascii85-encoded
 */
fun bytesToLzmadStr(b: ByteArray): String {
    val sb = StringBuilder()
    val bi = ByteArrayInputStream(b)
    val bo = ByteArray64GrowableOutputStream()

    Lzma.compress(bi, bo); bo.flush(); bo.close()

    val ba = bo.toByteArray64()
    var bai = 0
    val buf = IntArray(4) { Ascii85.PAD_BYTE }
    ba.forEach {
        if (bai > 0 && bai % 4 == 0) {
            sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))
            buf.fill(Ascii85.PAD_BYTE)
        }

        buf[bai % 4] = it.toInt() and 255

        bai += 1
    }; sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))

    return sb.toString()
}