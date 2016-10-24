package net.torvald.terrarum.gameactors.ai

import net.torvald.colourutil.CIELab
import net.torvald.colourutil.CIELabUtil
import net.torvald.colourutil.RGB
import net.torvald.colourutil.toLab
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.mapdrawer.LightmapRenderer
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * Created by minjaesong on 16-10-24.
 */
internal class AILuaAPI(g: Globals, actor: ActorWithBody) {

    init {
        // load things. WARNING: THIS IS MANUAL!
        g["ai"] = LuaValue.tableOf()
        g["ai"]["getNearestActor"] = GetNearestActor()
        g["ai"]["getNearestPlayer"] = GetNearestPlayer()
        g["ai"]["getX"] = GetX(actor)
        g["ai"]["getY"] = GetY(actor)
    }

    companion object {
        fun composeActorObject(actor: ActorWithBody): LuaTable {
            val t = LuaValue.tableOf()

            t["name"] = actor.actorValue.getAsString(AVKey.NAME)
            t["posX"] = actor.hitbox.centeredX
            t["posY"] = actor.hitbox.centeredY

            t["veloX"] = actor.veloX
            t["veloY"] = actor.veloY

            t["width"] = actor.hitbox.width
            t["height"] = actor.hitbox.height

            val lumrgb: Int = actor.actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
            t["luminosity_rgb"] = lumrgb
            t["luminosity"] = RGB( // perceived luminosity
                    lumrgb.div(LightmapRenderer.MUL_2).mod(LightmapRenderer.MUL) / LightmapRenderer.CHANNEL_MAX_FLOAT,
                    lumrgb.div(LightmapRenderer.MUL_2).mod(LightmapRenderer.MUL) / LightmapRenderer.CHANNEL_MAX_FLOAT,
                    lumrgb.div(LightmapRenderer.MUL_2).mod(LightmapRenderer.MUL) / LightmapRenderer.CHANNEL_MAX_FLOAT
            ).toLab().L.div(100.0)

            return t
        }
    }

    /** ai.getNearestActor(nullable any criterion, nullable number range) */
    class GetNearestActor() : LuaFunction() {
        override fun call(): LuaValue {
            return LuaValue.NONE
        }

        override fun call(crit: LuaValue): LuaValue {
            return LuaValue.NONE
        }

        override fun call(crit: LuaValue, range: LuaValue): LuaValue {
            return LuaValue.NONE
        }
    }

    /** ai.getNearestPlayer(nullable any criterion, nullable number range) */
    class GetNearestPlayer() : LuaFunction() {
        override fun call(): LuaValue {
            return LuaValue.NONE
        }

        override fun call(crit: LuaValue): LuaValue {
            return LuaValue.NONE
        }

        override fun call(crit: LuaValue, range: LuaValue): LuaValue {
            return LuaValue.NONE
        }
    }

    class GetX(val actor: ActorWithBody) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(actor.hitbox.centeredX)
        }
    }

    class GetY(val actor: ActorWithBody) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(actor.hitbox.centeredY)
        }
    }

}