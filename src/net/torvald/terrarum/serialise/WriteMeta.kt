package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import net.torvald.terrarum.weather.WeatherMixer
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-08-23.
 */
open class WriteMeta(val ingame: TerrarumIngame) {

    open fun invoke(): String {
        val world = ingame.world
        
        val props = hashMapOf<String, Any>(
            "genver" to 4,
            "savename" to world.worldName,
            "terrseed" to world.generatorSeed,
            "randseed0" to RoguelikeRandomiser.RNG.state0,
            "randseed1" to RoguelikeRandomiser.RNG.state1,
            "weatseed0" to WeatherMixer.RNG.state0,
            "weatseed1" to WeatherMixer.RNG.state1,
            "playerid" to ingame.theRealGamer.referenceID,
            "creation_t" to world.creationTime,
            "lastplay_t" to world.lastPlayTime,
            "playtime_t" to world.totalPlayTime,

            // CSVs
            "blocks" to StringBuilder().let {
                ModMgr.getFilesFromEveryMod("blocks/blocks.csv").forEach { (modname, file) ->
                    it.append("\n\n## module: $modname ##\n\n")
                    it.append(file.readText())
                }
                bytesToZipdStr(it.toString().toByteArray())
            },

            "items" to StringBuilder().let {
                ModMgr.getFilesFromEveryMod("items/itemid.csv").forEach { (modname, file) ->
                    it.append("\n\n## module: $modname ##\n\n")
                    it.append(file.readText())
                }
                bytesToZipdStr(it.toString().toByteArray())
            },

            "wires" to StringBuilder().let {
                ModMgr.getFilesFromEveryMod("wires/wires.csv").forEach { (modname, file) ->
                    it.append("\n\n## module: $modname ##\n\n")
                    it.append(file.readText())
                }
                bytesToZipdStr(it.toString().toByteArray())
            },

            // TODO fluids
            "materials" to StringBuilder().let {
                ModMgr.getFilesFromEveryMod("materials/materials.csv").forEach { (modname, file) ->
                    it.append("\n\n## module: $modname ##\n\n")
                    it.append(file.readText())
                }
                bytesToZipdStr(it.toString().toByteArray())
            },

            "loadorder" to ModMgr.loadOrder,
            "worlds" to ingame.gameworldIndices
        )
        
        return Json(JsonWriter.OutputType.json).toJson(props)
    }

    /**
     * @param b a ByteArray
     * @return Bytes in [b] which are GZip'd then Ascii85-encoded
     */
    private fun bytesToZipdStr(b: ByteArray): String {
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
            }

            buf[bai % 4] = it.toInt() and 255

            bai += 1
        }; sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))

        return sb.toString()
    }
}