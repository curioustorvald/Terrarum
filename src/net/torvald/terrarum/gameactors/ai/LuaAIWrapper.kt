package net.torvald.terrarum.gameactors.ai

import net.torvald.terrarum.gameactors.Actor
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.InputStreamReader
import java.io.Reader

/**
 * Created by minjaesong on 2017-02-04.
 */
/*class LuaAIWrapper(private val scriptPath: String) : ActorAI {

    protected val luag: Globals = JsePlatform.standardGlobals()

    /**
     * Initialised in init block.
     * Use lua function "update(delta)" to step the AI.
     */
    protected lateinit var luaInstance: LuaValue

    private lateinit var aiLuaAPI: AILuaAPI

    private lateinit var targetActor: Actor

    /**
     * The initialiser
     *
     * Use ```(p.ai as LuaAIWrapper).attachActor(p)```
     */
    fun attachActor(actor: Actor) {
        targetActor = actor

        luag["io"] = LuaValue.NIL
        luag["os"] = LuaValue.NIL
        luag["luajava"] = LuaValue.NIL
        aiLuaAPI = AILuaAPI(luag, targetActor)
        // load the script and execute it (initialises target script)
        val inputStream = javaClass.getResourceAsStream(scriptPath)
        luaInstance = luag.load(InputStreamReader(inputStream), scriptPath.split(Regex("[\\/]")).last())
        luaInstance.call()
    }

    override fun update(actor: Actor, delta: Float) {
        // run "update()" function in the script
        luag.get("update").call(delta.toLua())
    }

    lateinit var currentExecutionThread: Thread
    var threadRun = false

    fun runCommand(reader: Reader, filename: String) {
        if (!threadRun && !targetActor.flagDespawn) {
            currentExecutionThread = Thread(ThreadRunCommand(luag, reader, filename))
            currentExecutionThread.start()
            threadRun = true
        }
    }

    fun runCommand(script: String) {
        if (!threadRun && !targetActor.flagDespawn) {
            currentExecutionThread = Thread(ThreadRunCommand(luag, script, ""))
            currentExecutionThread.start()
            threadRun = true
        }
    }

    class ThreadRunCommand : Runnable {

        val mode: Int
        val arg1: Any
        val arg2: String
        val lua: Globals

        constructor(luaInstance: Globals, line: String, env: String) {
            mode = 0
            arg1 = line
            arg2 = env
            lua = luaInstance
        }

        constructor(luaInstance: Globals, reader: Reader, filename: String) {
            mode = 1
            arg1 = reader
            arg2 = filename
            lua = luaInstance
        }

        override fun run() {
            try {
                val chunk: LuaValue
                if (mode == 0)
                    chunk = lua.load(arg1 as String, arg2)
                else if (mode == 1)
                    chunk = lua.load(arg1 as Reader, arg2)
                else
                    throw IllegalArgumentException("Unsupported mode: $mode")


                chunk.call()
            }
            catch (e: LuaError) {
                e.printStackTrace(System.err)
            }
        }
    }

    fun Float.toLua(): LuaValue = LuaInteger.valueOf(this.toDouble())
}*/