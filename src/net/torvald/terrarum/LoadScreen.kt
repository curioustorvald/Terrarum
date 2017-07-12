package net.torvald.terrarum

import com.badlogic.gdx.Screen
import com.badlogic.gdx.ScreenAdapter
import net.torvald.dataclass.HistoryArray

/**
 * Created by minjaesong on 2017-07-13.
 */
object LoadScreen : ScreenAdapter() {

    private lateinit var actualSceneToBeLoaded: Screen
    private lateinit var sceneLoadingThread: Thread

    private val messages = HistoryArray<String>(20)





    fun setMessage(msg: String) {
        messages.add(msg)
    }

    override fun show() {

    }

    override fun render(delta: Float) {

    }
}