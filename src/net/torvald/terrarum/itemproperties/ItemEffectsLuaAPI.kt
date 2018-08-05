package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ai.toLua
import net.torvald.terrarum.modulebasegame.Ingame
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * Created by minjaesong on 2017-04-16.
 */
class ItemEffectsLuaAPI(g: Globals) {

    init {
        g["getMouseTile"] = GetMouseTile()
        g["getMousePos"] = GetMousePos()



        g["world"] = LuaTable()

        g["world"]["strikeEarth"] = StrikeEarth()
        g["world"]["strikeWall"] = StrikeWall()



        g["actor"] = LuaTable()
    }


    class GetMouseTile : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.tableOf(arrayOf(Terrarum.mouseTileX.toLua(), Terrarum.mouseTileY.toLua()))
        }
    }
    class GetMousePos : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.tableOf(arrayOf(Terrarum.mouseX.toLua(), Terrarum.mouseY.toLua()))
        }
    }

    class StrikeEarth : ThreeArgFunction() {
        override fun call(x: LuaValue, y: LuaValue, power: LuaValue): LuaValue {
            (Terrarum.ingame!!.world).inflictTerrainDamage(x.checkint(), y.checkint(), power.checkdouble())
            return LuaValue.NONE
        }
    }
    class StrikeWall : ThreeArgFunction() {
        override fun call(x: LuaValue, y: LuaValue, power: LuaValue): LuaValue {
            (Terrarum.ingame!!.world).inflictWallDamage(x.checkint(), y.checkint(), power.checkdouble())
            return LuaValue.NONE
        }
    }

}