package com.torvald.terrarum.gameactors

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-03-14.
 */
interface Glowing {
    fun drawGlow(gc: GameContainer, g: Graphics)

    fun updateGlowSprite(gc: GameContainer, delta: Int)
}