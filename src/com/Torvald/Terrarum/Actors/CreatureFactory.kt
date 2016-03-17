package com.Torvald.Terrarum.Actors

import com.Torvald.JsonFetcher
import com.Torvald.Rand.Fudge3
import com.Torvald.Rand.HQRNG
import com.Torvald.Terrarum.LangPack.Lang
import com.google.gson.JsonObject
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-14.
 */

object CreatureFactory {

    private val JSONPATH = "./res/raw/"

    @JvmStatic
    @Throws(IOException::class, SlickException::class)
    fun build(jsonFileName: String): ActorWithBody {
        val jsonObj = JsonFetcher.readJson(JSONPATH + jsonFileName)
        val actor = ActorWithBody()


        val elementsString = arrayOf("racename", "racenameplural")

        val elementsFloat = arrayOf("baseheight", "basemass", "accel", "toolsize", "encumbrance")

        val elementsFloatVariable = arrayOf("strength", "speed", "jumppower", "scale", "speed")

        val elementsBoolean = arrayOf("intelligent")

        val elementsMultiplyFromOne = arrayOf("physiquemult")


        setAVStrings(actor, elementsString, jsonObj)
        setAVFloats(actor, elementsFloat, jsonObj)
        setAVFloatsVariable(actor, elementsFloatVariable, jsonObj)
        setAVMultiplyFromOne(actor, elementsMultiplyFromOne, jsonObj)
        setAVBooleans(actor, elementsBoolean, jsonObj)

        actor.actorValue.set("accel", Player.WALK_ACCEL_BASE)
        actor.actorValue.set("accelmult", 1f)

        return actor
    }

    /**
     * Fetch and set actor values that have 'variable' appended. E.g. strength
     * @param p
     * *
     * @param elemSet
     * *
     * @param jsonObject
     */
    private fun setAVFloatsVariable(p: ActorWithBody, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            val baseValue = jsonObject.get(s).asFloat
            // roll fudge dice and get value [-3, 3] as [0, 6]
            val varSelected = Fudge3(HQRNG()).rollForArray()
            // get multiplier from json. Assuming percentile
            val multiplier = jsonObject.get(s + "variable").asJsonArray.get(varSelected).asInt
            val realValue = baseValue * multiplier / 100f

            p.actorValue.set(s, realValue)
        }
    }

    /**
     * Fetch and set string actor values
     * @param p
     * *
     * @param elemSet
     * *
     * @param jsonObject
     */
    private fun setAVStrings(p: ActorWithBody, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            val key = jsonObject.get(s).asString
            p.actorValue.set(s, Lang.get(key))
        }
    }

    /**
     * Fetch and set float actor values
     * @param p
     * *
     * @param elemSet
     * *
     * @param jsonObject
     */
    private fun setAVFloats(p: ActorWithBody, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            p.actorValue.set(s, jsonObject.get(s).asFloat)
        }
    }

    /**
     * Fetch and set actor values that should multiplier be applied to the base value of 1.
     * E.g. physiquemult
     * @param p
     * *
     * @param elemSet
     * *
     * @param jsonObject
     */
    private fun setAVMultiplyFromOne(p: ActorWithBody, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            val baseValue = 1f
            // roll fudge dice and get value [-3, 3] as [0, 6]
            val varSelected = Fudge3(HQRNG()).rollForArray()
            // get multiplier from json. Assuming percentile
            val multiplier = jsonObject.get(s).asJsonArray.get(varSelected).asInt
            val realValue = baseValue * multiplier / 100f

            p.actorValue.set(s, realValue)
        }
    }

    private fun setAVBooleans(p: ActorWithBody, elemSet: Array<String>, jsonObject: JsonObject) {
        for (s in elemSet) {
            p.actorValue.set(s, jsonObject.get(s).asBoolean)
        }
    }
}