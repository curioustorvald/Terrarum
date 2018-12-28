package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.ui.UICanvas

/**
 * Furnitures that opens its own UI when right-clicked/interact-key-downed
 *
 * It is recommended to override dispose() as follows:
 * ```
 * override fun dispose() {
 *     closeUI() // UI will be closed when the furniture is being destroyed while the UI is opened
 *     super.dispose()
 * }
 * ```
 *
 * Created by minjaesong on 2018-12-29.
 */
interface Interactable {

    val ui: UICanvas

    fun openUI()
    fun closeUI()

}