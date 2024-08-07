package net.torvald.terrarum


/**
 * Created by minjaesong on 2018-06-21.
 */
abstract class ModuleEntryPoint {
    abstract fun invoke()
    abstract fun dispose()
    open fun getTitleScreen(batch: FlippingSpriteBatch): IngameInstance? = null
}