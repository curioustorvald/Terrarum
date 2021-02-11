package net.torvald.terrarum.worlddrawer

import net.torvald.gdx.graphics.Cvec
import net.torvald.gdx.graphics.UnsafeCvecArray
import net.torvald.terrarum.blockproperties.Block.AIR
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.ui.abs
import net.torvald.terrarum.realestate.LandUtil
import kotlin.system.exitProcess

/**
 * Created by minjaesong on 2020-03-04
 */

internal class LightCalculatorContext(
        private val world: GameWorld,
        private val lightmap: UnsafeCvecArray,
        private val lanternMap: HashMap<BlockAddress, Cvec>
) {
    // No longer in use because of the much efficient light updating method
}