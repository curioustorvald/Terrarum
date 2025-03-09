package net.torvald.terrarum.modulebasegame.gameworld

import net.torvald.terrarum.Point2i
import net.torvald.terrarum.gameworld.TerrarumSavegameExtrafieldSerialisable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by minjaesong on 2025-03-09.
 */
class GameConveyorLinkages : TerrarumSavegameExtrafieldSerialisable {

    internal val ledger = ArrayList<ConveyorBeltInstallation>()

}

data class ConveyorBeltInstallation(
    val type: Int,
    val p: Point2i,
    val q: Point2i,
    val installer: UUID?
)