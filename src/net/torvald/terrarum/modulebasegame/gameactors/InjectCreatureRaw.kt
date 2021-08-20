package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.random.Fudge3
import net.torvald.terrarum.langpack.Lang
import com.google.gson.JsonObject
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorValue
import java.security.SecureRandom

/**
 * Created by minjaesong on 2016-03-25.
 */
object InjectCreatureRaw {

    private const val JSONMULT = "mult" // one appears in JSON files
    private const val BUFF = AVKey.BUFF // one appears in JSON files

    /**
     * 'Injects' creature raw ActorValue to the ActorValue reference provided.
     *
     * @param actorValueRef ActorValue object to be injected.
     * @param jsonFileName with extension
     */
    operator fun invoke(actorValueRef: ActorValue, module: String, jsonFileName: String) {
        val jsonObj = JsonFetcher(ModMgr.getPath(module, "creatures/$jsonFileName"))

        jsonObj.keySet().filter { !it.startsWith("_") }.forEach { key ->

            val diceRollers = ArrayList<String>()

            jsonObj[key].let {
                if (it.isJsonPrimitive) {
                    val raw = it.asString
                    val lowraw = raw.toLowerCase()
                    // can the value be cast to Boolean?
                    if (lowraw == "true") actorValueRef[key] = true
                    else if (lowraw == "false") actorValueRef[key] = false
                    else {
                        try {
                            actorValueRef[key] =
                                    if (raw.contains('.')) it.asDouble
                                    else it.asInt
                        }
                        catch (e: NumberFormatException) {
                            actorValueRef[key] = raw
                        }
                    }
                }
                else if (key.endsWith(JSONMULT) && it.isJsonArray) {
                    diceRollers.add(key)
                }
                else {
                    printdbg(this, "Unknown Creature Raw key: $key")
                }
            }

            diceRollers.forEach { keymult ->
                val keybase = keymult.substring(0, keymult.length - 4)
                val baseValue = jsonObj[keybase].asDouble
                val selected = Fudge3(SecureRandom()).rollForArray()
                val mult = jsonObj[keymult].asJsonArray.get(selected).asInt
                val realValue = baseValue * mult / 100.0

                actorValueRef[keybase] = realValue
                actorValueRef[keybase + BUFF] = 1.0
            }
        }
    }
}