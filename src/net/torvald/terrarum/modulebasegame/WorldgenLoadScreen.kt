package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.ScreenAdapter
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.IngameInstance
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.util.HistoryArray
import kotlin.math.roundToInt

/**
 * World loading screen with minecraft 1.14-style preview
 *
 * Created by minjaesong on 2019-11-09.
 */
class WorldgenLoadScreen(private var world: GameWorld, private var screenToLoad: IngameInstance) : ScreenAdapter() {

    // a Class impl is chosen to make resize-handling easier, there's not much benefit making this a singleton anyway

    companion object {
        private const val WIDTH_RATIO = 0.6
    }

    private val previewWidth = (AppLoader.screenW * WIDTH_RATIO).roundToInt()
    private val previewHeight = (AppLoader.screenW * WIDTH_RATIO * world.height / world.width).roundToInt()

    private lateinit var screenLoadingThread: Thread


    private val messages = HistoryArray<String>(20)

}