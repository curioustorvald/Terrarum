package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.jme3.math.FastMath

/**
 * Created by minjaesong on 2024-02-12.
 */
class ParamRandomiser(val base: Float, val mult: Float) {

    @Transient private var rngBase0 = Math.random().toFloat() // initial cycle phase (xxxxFuncX)
    @Transient private var rngBase1 = getNewRandom() // flicker P0, etc
    @Transient private var rngBase2 = getNewRandom() // flicker P1, etc
    @Transient private val domain = 18f/64f

    private fun getNewRandom() = base + Math.random().toFloat() * mult

    fun update(delta: Float) {
        // FPS-time compensation
        if (Gdx.graphics.framesPerSecond > 0) {
            rngBase0 += delta
        }

        // reset timer
        if (rngBase0 > domain) {
            rngBase0 -= domain

            // flicker related
            rngBase1 = rngBase2
            rngBase2 = getNewRandom()
        }
    }
    fun get(): Float {
        val funcY = FastMath.interpolateLinear(rngBase0 / domain, rngBase1, rngBase2)
        return funcY
    }

}