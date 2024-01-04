package net.torvald.terrarum

/**
 * Created by minjaesong on 2022-12-25.
 */
interface GameUpdateGovernor {
    /**
     * @param tickInterval reciprocal of "tick rate" or "frame rate", depending on the type of the governor
     * @param updateFunction what to do on `update`. Takes one argument that is delta-time
     * @param renderFunction what to do on `render`. Takes one argument that is delta-time
     */
    fun update(deltaTime: Float, tickInterval: Second, updateFunction: (Float) -> Unit, renderFunction: (Float) -> Unit)
    fun reset()
}

object Anarchy : GameUpdateGovernor {
    override fun update(
        deltaTime: Float,
        tickInterval: Second,
        updateFunction: (Float) -> Unit,
        renderFunction: (Float) -> Unit
    ) {
        updateFunction(deltaTime)
        renderFunction(deltaTime)
    }

    override fun reset() {
    }
}

object ConsistentUpdateRate : GameUpdateGovernor {

    private var akku = 0f

    override fun update(deltaTime: Float, tickInterval: Second, updateFunction: (Float) -> Unit, renderFunction: (Float) -> Unit) {
        akku += deltaTime

        var i = 0L
        while (akku >= tickInterval) {
            App.measureDebugTime("Ingame.Update") { updateFunction(tickInterval) } // update-delta
            akku -= tickInterval
            i += 1
        }
        App.setDebugTime("Ingame.UpdateCounter", i)

        /** RENDER CODE GOES HERE */
        App.measureDebugTime("Ingame.Render") { renderFunction(deltaTime) } // frame-delta, should be identical to Gdx.graphics.deltaTime
    }

    override fun reset() {
        akku = 0f
    }
}