package net.torvald.terrarum

/**
 * Created by minjaesong on 2024-10-11.
 */
object TooltipManager {

    /**
     * Special hash values:
     * - 10001: Encumbrance bar
     * - 10002: Pickaxe
     */
    val tooltipShowing = HashMap<Long, Boolean>()
}

/**
 * Tooltip works like a "bus" principle: you acquire the control on the tooltip-bus to show a tooltip; you
 * release the control to the bus to hide it. `unsetToolTip()` will release the bus and forcibly closes
 * the tooltip UI.
 *
 * Created by minjaesong on 2024-10-11.
 */
abstract class TooltipListener {

    open val tooltipHash = System.nanoTime()

    /**
     * Acquire a control over the "tooltip bus". If the message is null, `releaseTooltip()` will be invoked instead.
     */
    fun acquireTooltip(message: String?) {
        if (message == null) return releaseTooltip()

        INGAME.setTooltipMessage(message)
        TooltipManager.tooltipShowing[tooltipHash] = true
    }

    fun releaseTooltip() {
//        TooltipManager.tooltipShowing[tooltipHash] = false // I doubt this is not really necessary...?
        TooltipManager.tooltipShowing.remove(tooltipHash)
    }

    fun removeFromTooltipRecord() {
        TooltipManager.tooltipShowing.remove(tooltipHash)
    }

    fun clearTooltip() {
        printStackTrace(this)
        TooltipManager.tooltipShowing.clear()
        INGAME.setTooltipMessage(null)
    }

    fun tooltipAcquired() = TooltipManager.tooltipShowing[tooltipHash] ?: false
}