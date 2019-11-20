package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import net.torvald.util.CircularArray

open class LoadScreenBase : ScreenAdapter() {

    open var screenToLoad: IngameInstance? = null
    open lateinit var screenLoadingThread: Thread

    internal val messages = CircularArray<String>(20, true)

    open fun addMessage(msg: String) {
        messages.appendHead(msg)
    }

    internal var errorTrapped = false
    internal var doContextChange = false

    var camera = OrthographicCamera(AppLoader.screenW.toFloat(), AppLoader.screenH.toFloat())

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


        initViewPort(AppLoader.screenW, AppLoader.screenH)
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
        if (doContextChange) {
            Thread.sleep(80)
            AppLoader.setScreen(LoadScreen.screenToLoad!!)
        }
    }

    override fun resize(width: Int, height: Int) {
        initViewPort(AppLoader.screenW, AppLoader.screenH)
    }
}