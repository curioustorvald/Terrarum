package net.torvald.terrarum.gameactors

import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import java.util.*

/**
 * Created by minjaesong on 16-06-17.
 */
internal class FixtureTikiTorch : FixtureBase(), Luminous {

    override var luminosity: Int
        get() = BlockCodex[Block.TORCH].luminosity
        set(value) {
            throw UnsupportedOperationException()
        }
    override val lightBoxList: ArrayList<Hitbox>

    init {
        density = 1200.0

        setHitboxDimension(10, 24, 0, 0)

        lightBoxList = ArrayList(1)
        lightBoxList.add(Hitbox(3.0, 0.0, 4.0, 3.0))

        makeNewSprite(10, 27, "assets/graphics/sprites/fixtures/tiki_torch.tga")
        sprite!!.delay = 200
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = 1.0
    }
}