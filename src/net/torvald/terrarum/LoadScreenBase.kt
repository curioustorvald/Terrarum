package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.util.CircularArray
import java.util.concurrent.atomic.AtomicLong

open class LoadScreenBase : ScreenAdapter(), Disposable, TerrarumGamescreen {

    open var preLoadJob: (LoadScreenBase) -> Unit = {}
    open var screenToLoad: IngameInstance? = null
    open lateinit var screenLoadingThread: Thread

    internal val messages = CircularArray<String>(20, true)

    open fun addMessage(msg: String) {
        messages.appendHead(msg)
    }

    internal var errorTrapped = false
    internal var doContextChange = false

    var camera = OrthographicCamera(App.scr.wf, App.scr.hf)

    var progress = AtomicLong(0L) // generic variable, interpretation will vary by the screen
    var stageValue = 0

    override fun show() {
        messages.clear()
        doContextChange = false

        if (screenToLoad == null) {
            println("[LoadScreen] Screen to load is not set. Are you testing the UI?")
        }
        else {
            val runnable = {
                try {
                    preLoadJob(this)
                    screenToLoad!!.show()
                }
                catch (e: Exception) {
                    addMessage("$ccR${Lang["ERROR_SAVE_CORRUPTED"].replace(".","")}: $e")
                    errorTrapped = true

                    System.err.println("Error while loading:")
                    e.printStackTrace()
                }
            }
            screenLoadingThread = Thread(runnable, "LoadScreen GameLoader")

            screenLoadingThread.start()
        }


        initViewPort(App.scr.width, App.scr.height)
    }

    fun initViewPort(width: Int, height: Int) {
        // Set Y to point downwards
        camera.setToOrtho(true, width.toFloat(), height.toFloat())

        // Update camera matrix
        camera.update()

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height)
    }

    override fun render(delta: Float) {
        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())

        if (screenToLoad?.gameInitialised ?: false) {
            doContextChange = true
        }

        if (doContextChange) {
            Thread.sleep(80)
            App.setScreen(screenToLoad!!)
        }
    }

    override fun resize(width: Int, height: Int) {
        initViewPort(App.scr.width, App.scr.height)
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
    }
}