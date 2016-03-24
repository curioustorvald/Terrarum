package com.Torvald.Terrarum.Actors

import com.Torvald.JsonFetcher
import com.Torvald.Terrarum.Actors.Faction.Faction
import com.Torvald.spriteAnimation.SpriteAnimation
import com.google.gson.JsonObject
import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-03-14.
 */

object PFSigrid {

    private val FACTION_PATH = "./res/raw/"

    @JvmStatic
    @Throws(SlickException::class)
    fun build(): Player {
        val p = Player()

        p.sprite = SpriteAnimation()
        p.sprite!!.setDimension(28, 51)
        p.sprite!!.setSpriteImage("res/graphics/sprites/test_player.png")
        p.sprite!!.setDelay(200)
        p.sprite!!.setRowsAndFrames(1, 1)
        p.sprite!!.setAsVisible()

        p.spriteGlow = SpriteAnimation()
        p.spriteGlow!!.setDimension(28, 51)
        p.spriteGlow!!.setSpriteImage("res/graphics/sprites/test_player_glow.png")
        p.spriteGlow!!.setDelay(200)
        p.spriteGlow!!.setRowsAndFrames(1, 1)
        p.spriteGlow!!.setAsVisible()

        p.actorValue = ActorValue()
        p.actorValue.set("scale", 1.0f)
        p.actorValue.set("speed", 4.0f)
        p.actorValue.set("speedmult", 1.0f)
        p.actorValue.set("accel", Player.WALK_ACCEL_BASE)
        p.actorValue.set("accelmult", 1.0f)

        p.actorValue.set("jumppower", 5f)

        p.actorValue.set("basemass", 80f)

        p.actorValue.set("physiquemult", 1) // Constant 1.0 for player, meant to be used by random mobs
        /**
         * fixed value, or 'base value', from creature strength of Dwarf Fortress.
         * Human race uses 1000. (see CreatureHuman.json)
         */
        p.actorValue.set("strength", 1414)
        p.actorValue.set("encumbrance", 1000)

        p.actorValue.set("name", "Sigrid")

        p.actorValue.set("intelligent", true)

        p.actorValue.set("luminosity", 5980540)

        p.actorValue.set("selectedtile", 16)

        p.setHitboxDimension(18, 46, 8, 0)

        p.inventory = ActorInventory(0x7FFFFFFF, true)

        p.setPosition((4096 * 16).toFloat(), (300 * 16).toFloat())

        p.faction.add(loadFactioningData("FactionSigrid.json"))

        return p
    }

    private fun loadFactioningData(filename: String): Faction {
        var jsonObject: JsonObject = JsonObject()
        try {
            jsonObject = JsonFetcher.readJson(FACTION_PATH + filename)
        } catch (e: IOException) {
            e.printStackTrace()
            System.exit(-1)
        }

        val faction = Faction(jsonObject.get("factionname").asString)

        jsonObject.get("factionamicable").asJsonArray.forEach { jobj -> faction.addFactionAmicable(jobj.asString) }
        jsonObject.get("factionneutral").asJsonArray.forEach { jobj -> faction.addFactionNeutral(jobj.asString) }
        jsonObject.get("factionhostile").asJsonArray.forEach { jobj -> faction.addFactionHostile(jobj.asString) }
        jsonObject.get("factionfearful").asJsonArray.forEach { jobj -> faction.addFactionFearful(jobj.asString) }

        return faction
    }
}
