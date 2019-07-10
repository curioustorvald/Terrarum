package net.torvald.terrarum.modulecomputers.virtualcomputer.computer

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.InputStream

/**
 * New plan: screw teletype and gui; only the simple 80*24 (size may mary) dumb terminal
 *
 * Created by minjaesong on 2019-07-09.
 */
class LuaComputerVM(val display: MDA) {

    val luaInstance: Globals = JsePlatform.standardGlobals()

    val stdout = MDAPrintStream(display)
    val stderr = MDAPrintStream(display)
    val stdin = LuaComputerInputStream(this)


    init {
        // bit-bit32 alias
        luaInstance["bit"] = luaInstance["bit32"]

        // set input/outputstreams
        luaInstance.STDOUT = stdout
        luaInstance.STDERR = stderr
        luaInstance.STDIN = stdin

        luaInstance.addOneArgFun("upgoer") { p0 ->
            display.println("Up-goer ${p0.toint()} goes up!")
            LuaValue.NIL
        }

        luaInstance.addOneArgFun("perkele.upgoer") { p0 ->
            display.println("Up-goer ${p0.toint()} goes up!")
            LuaValue.NIL
        }

        luaInstance.addOneArgFun("perkele.saatana.jumalauta.vittu.upgoer") { p0 ->
            display.println("Up-goer ${p0.toint()} goes up!")
            LuaValue.NIL
        }

        luaInstance.load("""print('Hello, world!') print('Ready.')""").invoke()
        luaInstance.load("""print(upgoer)""").invoke()
        luaInstance.load("""upgoer(1)""").invoke()
        luaInstance.load("""perkele.upgoer(2)""").invoke()
        luaInstance.load("""perkele.saatana.jumalauta.vittu.upgoer(5)""").invoke()
    }

}

class LuaComputerInputStream(val host: LuaComputerVM) : InputStream() {
    override fun read(): Int {
        TODO("not implemented")
    }
}

/**
 * Install a function into the lua.
 * @param identifier How you might call this lua function. E.g. "term.println"
 */
fun Globals.addOneArgFun(identifier: String, function: (p0: LuaValue) -> LuaValue) {
    val theActualFun = object : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            return function(p0)
        }
    }

    val tableNames = identifier.split('.')

    if (tableNames.isEmpty()) throw IllegalArgumentException("Identifier is empty")

    //println(tableNames)

    if (this[tableNames[0]].isnil()) {
        this[tableNames[0]] = LuaValue.tableOf()
    }
    else if (!this[tableNames[0]].istable()) {
        throw IllegalStateException("Redefinition: '${tableNames[0]}' (${this[tableNames[0]]})")
    }

    var currentTable = this[tableNames[0]]

    // turn nils into tables
    if (tableNames.size > 1) {
        tableNames.slice(1..tableNames.lastIndex).forEachIndexed { index, it ->
            if (currentTable[it].isnil()) {
                currentTable[it] = LuaValue.tableOf()
            }
            else if (!currentTable[it].istable()) {
                throw IllegalStateException("Redefinition: '${tableNames.slice(0..(index + 1)).joinToString(".")}' (${currentTable[it]})")
            }

            currentTable = currentTable[it]
        }

        // actually put the function onto the target
        // for some reason, memoisation doesn't work here so we use recursion to reach the target table as generated above
        fun putIntoTheTableRec(luaTable: LuaValue, recursionCount: Int) {
            if (recursionCount == tableNames.lastIndex - 1) {
                luaTable[tableNames[tableNames.lastIndex]] = theActualFun
            }
            else {
                putIntoTheTableRec(luaTable[tableNames[recursionCount + 1]], recursionCount + 1)
            }
        }

        putIntoTheTableRec(this[tableNames[0]], 0)
    }
    else {
        this[tableNames[0]] = theActualFun
    }
}

// don't add ZeroArgFun, TwoArgFun, ThreeArgFun until you make sure addOneArgFun to work!