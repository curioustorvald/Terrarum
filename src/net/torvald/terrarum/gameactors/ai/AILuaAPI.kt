package net.torvald.terrarum.gameactors.ai

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AIControlled
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithSprite
import net.torvald.terrarum.mapdrawer.LightmapRenderer
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.tileproperties.TileCodex
import org.luaj.vm2.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * Created by minjaesong on 16-10-24.
 */
internal class AILuaAPI(g: Globals, actor: ActorWithSprite) {

    // FIXME when actor jumps, the actor releases left/right stick

    init {
        if (actor !is AIControlled)
            throw IllegalArgumentException("The actor is not AIControlled! $actor")

        // load functions and set up constants
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

        g["ai"]["getNearbyTiles"] = GetNearbyTiles(actor)
        g["ai"]["getFloorsHeight"] = GetFloorsHeight(actor)
        g["ai"]["getCeilingsHeight"] = GetCeilingsHeight(actor)
        g["ai"]["getLedgesHeight"] = GetLedgesHeight(actor)

        g["game"] = LuaValue.tableOf()
        g["game"]["version"] = GameVersion()
        g["game"]["versionRaw"] = GameVersionRaw()
    }

    companion object {
        /**
         * Reads arbitrary ActorWithSprite and returns its information as Lua table
         */
        fun composeActorObject(actor: ActorWithSprite): LuaTable {
            val t: LuaTable = LuaTable()

            t["name"] = actor.actorValue.getAsString(AVKey.NAME).toLua()
            t["posX"] = actor.hitbox.centeredX.toLua()
            t["posY"] = actor.hitbox.centeredY.toLua()

            t["veloX"] = actor.veloX.toLua()
            t["veloY"] = actor.veloY.toLua()

            t["width"] = actor.hitbox.width.toLua()
            t["height"] = actor.hitbox.height.toLua()

            t["mass"] = actor.mass.toLua()

            t["collisionType"] = actor.collisionType.toLua()

            t["strength"] = actor.avStrength.toLua()

            val lumrgb: Int = actor.actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
            val MUL_2 = LightmapRenderer.MUL_2
            val MUL = LightmapRenderer.MUL
            val CHMAX = LightmapRenderer.CHANNEL_MAX
            t["luminosityRGB"] = lumrgb.toLua()
            t["luminosity"] = (lumrgb.div(MUL_2).and(CHMAX).times(3) +
                              lumrgb.div(MUL).and(CHMAX).times(4) +
                              lumrgb.and(1023)).div(8.0).toLua() // quick luminosity calculation

            return t
        }

        fun Double.toLua() = LuaValue.valueOf(this)
        fun Int.toLua() = LuaValue.valueOf(this)
        fun String.toLua() = LuaValue.valueOf(this)
        fun Double?.toLua() = if (this == null) LuaValue.NIL else this.toLua()
        fun Int?.toLua() = if (this == null) LuaValue.NIL else this.toLua()
        fun String?.toLua() = if (this == null) LuaValue.NIL else this.toLua()
        fun Boolean.toInt() = if (this) 1 else 0

        operator fun LuaTable.set(index: Int, value: Int) { this[index] = value.toLua() }
    }

    class GetSelfActorInfo(val actor: ActorWithSprite) : ZeroArgFunction() {
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

    class GetX(val actor: ActorWithSprite) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(actor.hitbox.centeredX)
        }
    }

    class GetY(val actor: ActorWithSprite) : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(actor.hitbox.centeredY)
        }
    }

    class MoveLeft(val actor: AIControlled) : LuaFunction() {
        override fun call(): LuaValue { // hard key press
            actor.moveLeft()
            return LuaValue.NONE
        }

        /** @param amount [0.0 - 1.0] */
        override fun call(amount: LuaValue): LuaValue { // stick tilt
            actor.moveLeft(amount.checkdouble().toFloat())
            return LuaValue.NONE
        }
    }

    class MoveRight(val actor: AIControlled) : LuaFunction() {
        override fun call(): LuaValue { // hard key press
            actor.moveRight()
            return LuaValue.NONE
        }

        /** @param amount [0.0 - 1.0] */
        override fun call(amount: LuaValue): LuaValue { // stick tilt
            actor.moveRight(amount.checkdouble().toFloat())
            return LuaValue.NONE
        }
    }

    class MoveUp(val actor: AIControlled) : LuaFunction() {
        override fun call(): LuaValue { // hard key press
            actor.moveUp()
            return LuaValue.NONE
        }

        /** @param amount [0.0 - 1.0] */
        override fun call(amount: LuaValue): LuaValue { // stick tilt
            actor.moveUp(amount.checkdouble().toFloat())
            return LuaValue.NONE
        }
    }

    class MoveDown(val actor: AIControlled) : LuaFunction() {
        override fun call(): LuaValue { // hard key press
            actor.moveDown()
            return LuaValue.NONE
        }

        /** @param amount [0.0 - 1.0] */
        override fun call(amount: LuaValue): LuaValue { // stick tilt
            actor.moveDown(amount.checkdouble().toFloat())
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

    class Jump(val actor: AIControlled) : LuaFunction() {
        override fun call(): LuaValue {
            actor.moveJump()
            return LuaValue.NONE
        }

        /** @param amount [0.0 - 1.0] */
        override fun call(amount: LuaValue): LuaValue { // stick tilt
            actor.moveJump(amount.checkdouble().toFloat())
            return LuaValue.NONE
        }
    }

    class GetNearbyTiles(val actor: ActorWithSprite) : OneArgFunction() {
        /** @param radius
         *
         *  3 will return 7x7 array, 0 will return 1x1, 1 will return 3x3
         *
         *  Index: [-3: y][-3: x] ... [0: y][0: x] ... [3: y][3: x] for radius 3
         *  Return value: bitset (int 0-7)
         *      1 -- solidity
         *      2 -- liquidity
         *      3 -- gravity
         */
        override fun call(arg: LuaValue): LuaValue {
            val radius = arg.checkint()

            if (radius < 0) {
                return LuaValue.NONE
            }
            else if (radius > 8) {
                throw IllegalArgumentException("Radius too large -- must be 8 or less")
            }
            else {
                val luatable = LuaTable()
                val feetTilePos = actor.feetPosTile
                for (y in feetTilePos[1] - radius..feetTilePos[1] + radius) {
                    luatable[y - feetTilePos[1]] = LuaTable()

                    for (x in feetTilePos[0] - radius..feetTilePos[0] + radius) {
                        val tile = TileCodex[Terrarum.ingame.world.getTileFromTerrain(x, y) ?: 4096]
                        val solidity = tile.isSolid.toInt()
                        val liquidity = tile.isFluid.toInt()
                        val gravity = tile.isFallable.toInt()
                        val tileFlag: Int = gravity.shl(2) + liquidity.shl(1) + solidity

                        luatable[y - feetTilePos[1]][x - feetTilePos[0]] = tileFlag.toLua()
                    }
                }

                return luatable
            }
        }
    }

    class GetFloorsHeight(val actor: ActorWithSprite) : OneArgFunction() {
        /** @param radius
         *
         *  3 will return len:7 array, 0 will return len:1, 1 will return len:3
         *
         *  Index: [-3] .. [0] .. [3] for radius
         *  Return value: floor height
         *  0: body tile (legs area)
         *  1: tile you can stand on
         *  2+: tiles down there
         */
        override fun call(arg: LuaValue): LuaValue {
            val radius = arg.checkint()

            val searchDownLimit = 12

            if (radius < 0) {
                return LuaValue.NONE
            }
            else if (radius > 8) {
                throw IllegalArgumentException("Radius too large -- must be 8 or less")
            }
            else {
                val luatable = LuaTable()
                val feetTilePos = actor.feetPosTile
                for (x in feetTilePos[0] - radius..feetTilePos[0] + radius) {
                    // search down
                    var searchDownCounter = 0
                    while (true) {
                        val tile = Terrarum.ingame.world.getTileFromTerrain(x, feetTilePos[1] + searchDownCounter) ?: Tile.STONE
                        if (TileCodex[tile].isSolid || searchDownCounter >= searchDownLimit) {
                            luatable[x - feetTilePos[0]] = searchDownCounter
                            break
                        }
                        searchDownCounter++
                    }
                }

                return luatable
            }
        }
    }

    class GetCeilingsHeight(val actor: ActorWithSprite) : OneArgFunction() {
        /** @param arg radius
         *
         *  3 will return 7x7 array, 0 will return 1x1, 1 will return 3x3
         *
         *  Index: [-3] .. [0] .. [3] for radius
         *  Return value: floor height
         *  0: body tile (legs area)
         *  1: body tile (may be vary depend on the size of the actor)
         *  2+: tiles up there
         */
        override fun call(arg: LuaValue): LuaValue {
            val radius = arg.checkint()

            val searchUpLimit = 12

            if (radius < 0) {
                return LuaValue.NONE
            }
            else if (radius > 8) {
                throw IllegalArgumentException("Radius too large -- must be 8 or less")
            }
            else {
                val luatable = LuaTable()
                val feetTilePos = actor.feetPosTile
                for (x in feetTilePos[0] - radius..feetTilePos[0] + radius) {
                    // search up
                    var searchUpCounter = 0
                    while (true) {
                        val tile = Terrarum.ingame.world.getTileFromTerrain(x, feetTilePos[1] - searchUpCounter) ?: Tile.STONE
                        if (TileCodex[tile].isSolid || searchUpCounter >= searchUpLimit) {
                            luatable[x - feetTilePos[0]] = searchUpCounter
                            break
                        }
                        searchUpCounter++
                    }
                }

                return luatable
            }
        }
    }

    class GetLedgesHeight(val actor: ActorWithSprite) : OneArgFunction() {
        /** @param arg radius
         * ==
         *    <- (non-solid found)
         * ==
         * ==
         * ==
         * == @  -> ledge height: 4
         * =================
         */
        override fun call(arg: LuaValue): LuaValue {
            val radius = arg.checkint()

            val searchUpLimit = 12

            if (radius < 0) {
                return LuaValue.NONE
            }
            else if (radius > 8) {
                throw IllegalArgumentException("Radius too large -- must be 8 or less")
            }
            else {
                val luatable = LuaTable()
                val feetTilePos = actor.feetPosTile
                for (x in feetTilePos[0] - radius..feetTilePos[0] + radius) {
                    // search up
                    var searchUpCounter = 0
                    while (true) {
                        val tile = Terrarum.ingame.world.getTileFromTerrain(x, feetTilePos[1] - searchUpCounter) ?: Tile.STONE
                        if (!TileCodex[tile].isSolid || searchUpCounter >= searchUpLimit) {
                            luatable[x - feetTilePos[0]] = searchUpCounter
                            break
                        }
                        searchUpCounter++
                    }
                }

                return luatable
            }
        }
    }




    class GameVersion : ZeroArgFunction() {
        override fun call(): LuaValue {
            return Terrarum.VERSION_STRING.toLua()
        }
    }

    class GameVersionRaw : ZeroArgFunction() {
        override fun call(): LuaValue {
            return Terrarum.VERSION_RAW.toLua()
        }
    }
}
