package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.random.HQRNG
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.FancyWorldgenLoadScreen
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.PlayerBuilderWerebeastTest
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.utils.RandomWordsName

/**
 * Created by minjaesong on 2018-12-08.
 */
class UIProxyNewRandomGame(val remoCon: UIRemoCon) : UICanvas() {

    override var width: Int = 0
    override var height: Int = 0
    override var openCloseTime: Second = 0f

    override fun updateImpl(delta: Float) {
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
        printdbg(this, "endOpening")

        val ingame = TerrarumIngame(App.batch)
        val worldParam = TerrarumIngame.NewGameParams(
//                PlayerBuilderTestSubject1(),
                PlayerBuilderWerebeastTest(),
                TerrarumIngame.NewWorldParameters(2880, 1350, HQRNG().nextLong(), RandomWordsName(4))
        ) {}
//        val worldParam = TerrarumIngame.NewWorldParameters(2880, 1350, 0x51621D)

//        val worldParam = TerrarumIngame.NewWorldParameters(6030, 1800, HQRNG().nextLong()) // small
//        val worldParam = TerrarumIngame.NewWorldParameters(9000, 2250, HQRNG().nextLong()) // normal
//        val worldParam = TerrarumIngame.NewWorldParameters(13500, 2970, HQRNG().nextLong()) // large
//        val worldParam = TerrarumIngame.NewWorldParameters(22500, 4500, HQRNG().nextLong()) // huge
        ingame.gameLoadInfoPayload = worldParam
        ingame.gameLoadMode = TerrarumIngame.GameLoadMode.CREATE_NEW

        Terrarum.setCurrentIngameInstance(ingame)
        //LoadScreen.screenToLoad = ingame
        //AppLoader.setScreen(LoadScreen)
        val loadScreen = FancyWorldgenLoadScreen(ingame, worldParam.newWorldParams.width, worldParam.newWorldParams.height)
        App.setLoadScreen(loadScreen)
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }
}