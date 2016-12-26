package net.torvald.terrarum.gameactors.ai

import net.torvald.terrarum.gameactors.AIControlled
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
        if (actor !is AIControlled)
            throw IllegalArgumentException("The actor is not AIControlled! $actor")

        // load things. WARNING: THIS IS MANUAL!
        g["ai"] = LuaValue.tableOf()

        g["ai"]["getSelfActorInfo"] = GetSelfActorInfo(actor)

        g["ai"]["getNearestActor"] = GetNearestActor()
        g["ai"]["getNearestPlayer"] = GetNearestPlayer()

        g["ai"]["getX"] = GetX(actor)
        g["ai"]["getY"] = GetY(actor)
        g["ai"]["moveUp"] = MoveUp(actor)
        g["ai"]["moveDown"] = MoveDown(actor)
        g["ai"]["moveLeft"] = MoveLeft(actor)
        g["ai"]["moveRight"] = MoveRight(actor)
        g["ai"]["moveTo"] = MoveTo(actor)
        g["ai"]["jump"] = Jump(actor)

    }

    companion object {
        /**
         * Reads arbitrary ActorWithBody and returns its information as Lua table
         */
        fun composeActorObject(actor: ActorWithBody): LuaTable {
            val t = LuaValue.tableOf()

            t["name"] = actor.actorValue.getAsString(AVKey.NAME)
            t["posX"] = actor.hitbox.centeredX
            t["posY"] = actor.hitbox.centeredY

            t["veloX"] = actor.veloX
            t["veloY"] = actor.veloY

            t["width"] = actor.hitbox.width
            t["height"] = actor.hitbox.height

            t["mass"] = actor.mass

            t["collisionType"] = actor.collisionType

            t["strength"] = actor.actorValue.getAsInt(AVKey.STRENGTH) ?: 0

            val lumrgb: Int = actor.actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
            val MUL_2 = LightmapRenderer.MUL_2
            val MUL = LightmapRenderer.MUL
            val CHMAX = LightmapRenderer.CHANNEL_MAX
            t["luminosityRGB"] = lumrgb
            t["luminosity"] = (lumrgb.div(MUL_2).and(CHMAX).times(3) +
                              lumrgb.div(MUL).and(CHMAX).times(4) +
                              lumrgb.and(1023)) / 8 // quick luminosity calculation

            return t
        }
    }

    class GetSelfActorInfo(val actor: ActorWithBody) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return composeActorObject(actor)
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

    class MoveLeft(val actor: AIControlled) : ZeroArgFunction() {
        override fun call(): LuaValue {
            actor.moveLeft()
            return LuaValue.NONE
        }
    }

    class MoveRight(val actor: AIControlled) : ZeroArgFunction() {
        override fun call(): LuaValue {
            actor.moveRight()
            return LuaValue.NONE
        }
    }

    class MoveUp(val actor: AIControlled) : ZeroArgFunction() {
        override fun call(): LuaValue {
            actor.moveUp()
            return LuaValue.NONE
        }
    }

    class MoveDown(val actor: AIControlled) : ZeroArgFunction() {
        override fun call(): LuaValue {
            actor.moveDown()
            return LuaValue.NONE
        }
    }

    class MoveTo(val actor: AIControlled) : LuaFunction() {
        override fun call(bearing: LuaValue): LuaValue {
            actor.moveTo(bearing.checkdouble())
            return LuaValue.NONE
        }

        override fun call(toX: LuaValue, toY: LuaValue): LuaValue {
            actor.moveTo(toX.checkdouble(), toY.checkdouble())
            return LuaValue.NONE
        }
    }

    class Jump(val actor: AIControlled) : ZeroArgFunction() {
        override fun call(): LuaValue {
            actor.moveJump()
            return LuaValue.NONE
        }
    }

}
