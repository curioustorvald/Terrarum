package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.weather.WeatherMixer

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
                it.toString()
            },

            "items" to StringBuilder().let {
                ModMgr.getFilesFromEveryMod("items/itemid.csv").forEach { (modname, file) ->
                    it.append("\n\n## module: $modname ##\n\n")
                    it.append(file.readText())
                }
                it.toString()
            },

            "wires" to StringBuilder().let {
                ModMgr.getFilesFromEveryMod("wires/wires.csv").forEach { (modname, file) ->
                    it.append("\n\n## module: $modname ##\n\n")
                    it.append(file.readText())
                }
                it.toString()
            },

            // TODO fluids
            "materials" to StringBuilder().let {
                ModMgr.getFilesFromEveryMod("materials/materials.csv").forEach { (modname, file) ->
                    it.append("\n\n## module: $modname ##\n\n")
                    it.append(file.readText())
                }
                it.toString()
            },

            "loadorder" to ModMgr.loadOrder,
            "worlds" to ingame.gameworldIndices
        )
        
        return Json(JsonWriter.OutputType.json).toJson(props)
    }

}