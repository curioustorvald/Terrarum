package net.torvald.terrarum.modulecomputers.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameworld.NetFrame.Companion.toMAC
import net.torvald.terrarum.modulecomputers.ui.UIRingBusAnalyser
import net.torvald.terrarum.modulecomputers.ui.UIRingBusExerciser

/**
 * Created by minjaesong on 2025-03-03.
 */
class FixtureRingBusExerciser : FixtureRingBusCore {

    constructor() : super(
        portEmit = Point2i(0, 0),
        portSink = Point2i(1, 0),
        blockBox = BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_DEBUG_RING_BUS_EXERCISER"] }
    ) {
        this.mainUI = UIRingBusExerciser(this)
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        super.drawBody(frameDelta, batch)

        // draw its own MAC address
        drawUsingDrawFunInGoodPosition(frameDelta) { x, y ->
            App.fontSmallNumbers.draw(batch, super.mac.toMAC(), x, y + 2*TILE_SIZE - 12)
        }
    }
}



/**
 * Created by minjaesong on 2025-03-03.
 */
class FixtureRingBusAnalyser : FixtureRingBusCore {

    constructor() : super(
        portEmit = Point2i(0, 0),
        portSink = Point2i(1, 0),
        blockBox = BlockBox(BlockBox.NO_COLLISION, 2, 1),
        nameFun = { Lang["ITEM_DEBUG_RING_BUS_ANALYSER"] },
    ) {
        this.mainUI = UIRingBusAnalyser(this)
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        super.drawBody(frameDelta, batch)

        // draw its own MAC address
        drawUsingDrawFunInGoodPosition(frameDelta) { x, y ->
            App.fontSmallNumbers.draw(batch, super.mac.toMAC(), x, y + 1*TILE_SIZE - 12)
        }
    }
}

