package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.IngameInstance
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.gameparticles.ParticleVanishingText
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2016-06-17.
 */
internal class FixtureTikiTorch(nameFun: () -> String) : FixtureBase(BlockBox(BlockBox.NO_COLLISION, 1, 2), nameFun = nameFun), Luminous {

    private val rng = HQRNG()
    private val rndHash1: Int
    private val rndHash2: Int

    override var color: Cvec
        get() = BlockCodex[Block.TORCH].getLumCol(rndHash1, rndHash2)
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

        rndHash1 = rng.nextInt()
        rndHash2 = rng.nextInt()
    }

    private var nextDelay = 0.4f
    private var spawnTimer = 0f

    override fun update(delta: Float) {
        super.update(delta)

        if (spawnTimer >= nextDelay) {
            val s = rng.nextInt(1, 1000).toString()
            (Terrarum.ingame as TerrarumIngame).addParticle(ParticleVanishingText(s, hitbox.centeredX, hitbox.startY + 10))

            spawnTimer -= nextDelay
            nextDelay = rng.nextFloat() * 0.4f + 0.4f
        }

        spawnTimer += delta
    }

    override fun drawBody(batch: SpriteBatch) {
        super.drawBody(batch)
    }

    companion object {
        const val MASS = 1.0
    }
}