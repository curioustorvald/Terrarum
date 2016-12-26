package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.ai.AILuaAPI
import net.torvald.terrarum.gameitem.EquipPosition
import net.torvald.terrarum.gameitem.InventoryItem
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseMathLib
import org.luaj.vm2.lib.jse.JsePlatform
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input
import java.io.InputStreamReader
import java.io.Reader

/**
 * Created by minjaesong on 16-01-31.
 */
open class HumanoidNPC(val luaScript: String, born: GameDate) : ActorHumanoid(born), AIControlled, CanBeAnItem {

    override val scriptPath: String = ""

    protected val luag: Globals = JsePlatform.standardGlobals()

    init {
        AILuaAPI(luag, this)
    }

    // we're having InventoryItem data so that this class could be somewhat universal
    override var itemData: InventoryItem = object : InventoryItem() {
        override var id = referenceID
        override val equipPosition: Int = EquipPosition.HAND_GRIP

        override var mass: Double
            get() = actorValue.getAsDouble(AVKey.BASEMASS)!!
            set(value) {
                actorValue[AVKey.BASEMASS] = value
            }

        override var scale: Double
            get() = actorValue.getAsDouble(AVKey.SCALE)!!
            set(value) {
                actorValue[AVKey.SCALE] = value
            }

        override fun secondaryUse(gc: GameContainer, delta: Int) {
            // TODO place this Actor to the world
        }
    }

    override fun getItemWeight(): Double {
        return mass
    }

    override fun stopUpdateAndDraw() {
        isUpdate = false
        isVisible = false
    }

    override fun resumeUpdateAndDraw() {
        isUpdate = true
        isVisible = true
    }


    init {
        //val inputStream = javaClass.getResourceAsStream(scriptPath)
        //runCommand(InputStreamReader(inputStream), scriptPath)
    }


    override fun update(gc: GameContainer, delta: Int) {
        super.update(gc, delta)
        //runCommand(luaScript)
        luag.load(luaScript).call()

        //moveRight()
    }

    override fun moveLeft() { // hit the buttons on the controller box
        isLeftDown = true
    }

    override fun moveRight() { // hit the buttons on the controller box
        isRightDown = true
    }

    override fun moveUp() { // hit the buttons on the controller box
        isUpDown = true
    }

    override fun moveDown() { // hit the buttons on the controller box
        isDownDown = true
    }

    override fun moveJump() { // hit the buttons on the controller box
        isJumpDown = true
    }

    /** fly toward arbitrary angle  WARNING: the map is looped! */
    override fun moveTo(bearing: Double) {
        // if your NPC should fly, override this
        throw UnsupportedOperationException("Humans cannot fly :p")
    }

    /** fly toward arbitrary coord  WARNING: the map is looped! */
    override fun moveTo(toX: Double, toY: Double) {
        // if your NPC should fly, override this
        throw UnsupportedOperationException("Humans cannot fly :p")
    }

    var currentExecutionThread = Thread()
    var threadRun = false

    fun runCommand(reader: Reader, filename: String) {
        if (!threadRun && !flagDespawn) {
            currentExecutionThread = Thread(ThreadRunCommand(luag, reader, filename))
            currentExecutionThread.start()
            threadRun = true
        }
    }

    fun runCommand(script: String) {
        if (!threadRun && !flagDespawn) {
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
}