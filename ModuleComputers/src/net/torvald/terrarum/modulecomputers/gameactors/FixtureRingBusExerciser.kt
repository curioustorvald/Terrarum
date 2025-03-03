package net.torvald.terrarum.modulecomputers.gameactors

import net.torvald.terrarum.Point2i
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulecomputers.ui.UIRingBusExerciser

/**
 * Created by minjaesong on 2025-03-03.
 */
class FixtureRingBusExerciser : FixtureRingBusCore {

    constructor() : super(
        portEmit = Point2i(0, 0),
        portSink = Point2i(1, 0),
        blockBox = BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_DEBUG_RING_BUS_EXERCISER"] },
        mainUI = UIRingBusExerciser(this)
    )


}



/**
 * Created by minjaesong on 2025-03-03.
 */
class FixtureRingBusAnalyser : FixtureRingBusCore {

    constructor() : super(
        portEmit = Point2i(0, 0),
        portSink = Point2i(1, 0),
        blockBox = BlockBox(BlockBox.NO_COLLISION, 2, 1),
        nameFun = { Lang["ITEM_DEBUG_RING_BUS_Analyser"] },
        mainUI = UIRingBusAnalyser()
    )


}

