package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.random.HQRNG
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.WorldgenLoadScreen
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2018-12-08.
 */
class UIProxyNewRandomGame : UICanvas() {

    override var width: Int = 0
    override var height: Int = 0
    override var openCloseTime: Second = 0f

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
        TODO("not implemented")
    }

    override fun endOpening(delta: Float) {
        printdbg(this, "endOpening")


        val ingame = TerrarumIngame(App.batch)
        val worldParam = TerrarumIngame.NewWorldParameters(2880, 1350, HQRNG().nextLong())
//        val worldParam = TerrarumIngame.NewWorldParameters(2880, 1350, 0x51621D)

        //val worldParam = TerrarumIngame.NewWorldParameters(6000, 1800, 0x51621DL) // small
//        val worldParam = TerrarumIngame.NewWorldParameters(9000, 2250, 0x51621DL) // normal
        //val worldParam = TerrarumIngame.NewWorldParDoubleameters(13500, 3000, 0x51621DL) // large
        //val worldParam = TerrarumIngame.NewWorldParameters(22500, 4500, 0x51621DL) // huge
        ingame.gameLoadInfoPayload = worldParam
        ingame.gameLoadMode = TerrarumIngame.GameLoadMode.CREATE_NEW

        Terrarum.setCurrentIngameInstance(ingame)
        //LoadScreen.screenToLoad = ingame
        //AppLoader.setScreen(LoadScreen)
        val loadScreen = WorldgenLoadScreen(ingame, worldParam.width, worldParam.height)
        App.setLoadScreen(loadScreen)
    }

    override fun endClosing(delta: Float) {
        TODO("not implemented")
    }

    override fun dispose() {
        TODO("not implemented")
    }
}