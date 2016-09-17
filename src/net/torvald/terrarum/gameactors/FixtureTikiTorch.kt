package net.torvald.terrarum.gameactors

import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.tileproperties.TileNameCode
import net.torvald.terrarum.tileproperties.TilePropCodex
import java.util.*

/**
 * Created by minjaesong on 16-06-17.
 */
class FixtureTikiTorch : FixtureBase(), Luminous {

    override var luminosity: Int
        get() = TilePropCodex.getProp(TileNameCode.TORCH).luminosity
        set(value) {
            throw UnsupportedOperationException()
        }
    override val lightBoxList: ArrayList<Hitbox>

    init {
        isVisible = true
        density = 1200.0

        setHitboxDimension(10, 24, 0, 0)

        lightBoxList = ArrayList(1)
        lightBoxList.add(Hitbox(3.0, 0.0, 4.0, 3.0))

        sprite = SpriteAnimation()
        sprite!!.setDimension(10, 27)
        sprite!!.setSpriteImage("res/graphics/sprites/fixtures/tiki_torch.png")
        sprite!!.setDelay(200)
        sprite!!.setRowsAndFrames(1, 1)
        sprite!!.setAsVisible()

        actorValue[AVKey.BASEMASS] = 1.0

        luminosity = TilePropCodex.getProp(TileNameCode.TORCH).luminosity
    }
}