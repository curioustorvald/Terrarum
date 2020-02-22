package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*

/**
 * Created by minjaesong on 2016-06-17.
 */
internal class FixtureTikiTorch : FixtureBase(BlockBox(BlockBox.NO_COLLISION, 1, 2)), Luminous {

    override var color: Cvec
        get() = BlockCodex[Block.TORCH].lumCol
        set(value) {
            throw UnsupportedOperationException()
        }

    override val lightBoxList: ArrayList<Hitbox>

    init {
        density = 1200.0

        setHitboxDimension(16, 32, 0, 0)

        lightBoxList = ArrayList(1)
        lightBoxList.add(Hitbox(6.0, 5.0, 4.0, 3.0))

        makeNewSprite(TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/tiki_torch.tga"), 16, 32))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = MASS
    }

    companion object {
        const val MASS = 1.0
    }
}