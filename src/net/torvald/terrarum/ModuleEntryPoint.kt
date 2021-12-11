package net.torvald.terrarum

import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Created by minjaesong on 2018-06-21.
 */
abstract class ModuleEntryPoint {
    abstract fun invoke()
    abstract fun dispose()
    open fun getTitleScreen(batch: SpriteBatch): IngameInstance? = null
}