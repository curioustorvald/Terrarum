package com.torvald.terrarum.gameactors

import com.torvald.JsonFetcher
import com.torvald.random.Fudge3
import com.torvald.random.HQRNG
import com.torvald.terrarum.langpack.Lang
import com.google.gson.JsonObject
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-25.
 */
object CreatureRawInjector {

    const val JSONPATH = "./res/raw/creatures/"
    private const val MULTIPLIER_RAW_ELEM_SUFFIX = "mult"

    /**
     * 'Injects' creature raw ActorValue to the ActorValue reference provided.
     *
     * @param actorValueRef ActorValue object to be injected.
     * @param jsonFileName with extension
     */
    @Throws(IOException::class, SlickException::class)
    fun inject(actorValueRef: ActorValue, jsonFileName: String) {
        val jsonObj = JsonFetcher.readJson(JSONPATH + jsonFileName)

        val elementsString = arrayOf("racename", "racenameplural")
        val elementsFloat = arrayOf("baseheight", "basemass", "accel", "toolsize", "encumbrance")
        val elementsFloatVariable = arrayOf("strength", "speed", "jumppower", "scale", "speed")
        val elementsBoolean = arrayOf("intelligent")
        // val elementsMultiplyFromOne = arrayOf()

        setAVStrings(actorValueRef, elementsString, jsonObj)
        setAVFloats(actorValueRef, elementsFloat, jsonObj)
        setAVFloatsVariable(actorValueRef, elementsFloatVariable, jsonObj)
        // setAVMultiplyFromOne(actorValueRef, elementsMultiplyFromOne, jsonObj)
        setAVBooleans(actorValueRef, elementsBoolean, jsonObj)

        actorValueRef["accel"] = Player.WALK_ACCEL_BASE
        actorValueRef["accelmult"] = 1f
    }

    /**
     * Fetch and set actor values that have 'variable' appended. E.g. strength
     * @param avRef
     * *
     * @param elemSet
     * *
     * @param jsonObject
     */
    private fun setAVFloatsVariable(avRef: ActorValue, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            val baseValue = jsonObject.get(s).asFloat
            // roll fudge dice and get value [-3, 3] as [0, 6]
            val varSelected = Fudge3(HQRNG()).rollForArray()
            // get multiplier from json. Assuming percentile
            val multiplier = jsonObject.get(s + MULTIPLIER_RAW_ELEM_SUFFIX).asJsonArray.get(varSelected).asInt
            val realValue = baseValue * multiplier / 100f

            avRef[s] = realValue
            avRef[s + MULTIPLIER_RAW_ELEM_SUFFIX] = 1.0f // use multiplied value as 'base' for all sort of things
        }
    }

    /**
     * Fetch and set string actor values
     * @param avRef
     * *
     * @param elemSet
     * *
     * @param jsonObject
     */
    private fun setAVStrings(avRef: ActorValue, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            val key = jsonObject.get(s).asString
            avRef[s] = Lang[key]
        }
    }

    /**
     * Fetch and set float actor values
     * @param avRef
     * *
     * @param elemSet
     * *
     * @param jsonObject
     */
    private fun setAVFloats(avRef: ActorValue, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            avRef[s] = jsonObject.get(s).asFloat
        }
    }

    /**
     * Fetch and set actor values that should multiplier be applied to the base value of 1.
     * E.g. physiquemult
     * @param avRef
     * *
     * @param elemSet
     * *
     * @param jsonObject
     */
    private fun setAVMultiplyFromOne(avRef: ActorValue, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            val baseValue = 1f
            // roll fudge dice and get value [-3, 3] as [0, 6]
            val varSelected = Fudge3(HQRNG()).rollForArray()
            // get multiplier from json. Assuming percentile
            val multiplier = jsonObject.get(s).asJsonArray.get(varSelected).asInt
            val realValue = baseValue * multiplier / 100f

            avRef[s] = realValue
        }
    }

    private fun setAVBooleans(avRef: ActorValue, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            avRef[s] = jsonObject.get(s).asBoolean
        }
    }
}