package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.util.CircularArray

open class LoadScreenBase : ScreenAdapter(), Disposable {

    open var screenToLoad: IngameInstance? = null
    open lateinit var screenLoadingThread: Thread

    internal val messages = CircularArray<String>(20, true)

    open fun addMessage(msg: String) {
        messages.appendHead(msg)
    }

    internal var errorTrapped = false
    internal var doContextChange = false

    var camera = OrthographicCamera(AppLoader.screenSize.screenWf, AppLoader.screenSize.screenHf)

    override fun show() {
        messages.clear()
        doContextChange = false

        if (screenToLoad == null) {
            println("[LoadScreen] Screen to load is not set. Are you testing the UI?")
        }
        else {
            val runnable = {
                try {
                    screenToLoad!!.show()
                }
                catch (e: Exception) {
                    addMessage("$ccR$e")
                    errorTrapped = true

                    System.err.println("Error while loading:")
                    e.printStackTrace()
                }
            }
            screenLoadingThread = Thread(runnable, "LoadScreen GameLoader")

            screenLoadingThread.start()
        }


        initViewPort(AppLoader.screenSize.screenW, AppLoader.screenSize.screenH)
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
            AppLoader.setScreen(screenToLoad!!)
        }
    }

    override fun resize(width: Int, height: Int) {
        initViewPort(AppLoader.screenSize.screenW, AppLoader.screenSize.screenH)
    }
}