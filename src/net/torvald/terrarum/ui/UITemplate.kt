package net.torvald.terrarum.ui

/**
 * Created by minjaesong on 2024-01-10.
 */
abstract class UITemplate(val parent: UICanvas) {

    abstract fun getUIitems(): List<UIItem>

}