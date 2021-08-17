package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.gameparticles.ParticleVanishingSprite
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
        // loading textures
        CommonResourcePool.addToLoadingList("sprites-fixtures-tiki_torch.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/tiki_torch.tga"), 16, 32)
        }
        CommonResourcePool.addToLoadingList("particles-tiki_smoke.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "particles/tiki_smoke.tga"), 10, 10)
        }
        CommonResourcePool.loadAll()

        density = 1200.0

        setHitboxDimension(16, 32, 0, 0)

        lightBoxList = ArrayList(1)
        lightBoxList.add(Hitbox(6.0, 5.0, 4.0, 3.0))

        makeNewSprite(CommonResourcePool.getAsTextureRegionPack("sprites-fixtures-tiki_torch.tga"))
        sprite!!.setRowsAndFrames(1, 2)

        actorValue[AVKey.BASEMASS] = MASS

        rndHash1 = rng.nextInt()
        rndHash2 = rng.nextInt()

    }

    private var nextDelay = 0.4f
    private var spawnTimer = 0f

    override fun update(delta: Float) {
        super.update(delta)

        if (spawnTimer >= nextDelay) {
            (Terrarum.ingame as TerrarumIngame).addParticle(ParticleVanishingSprite(
                    CommonResourcePool.getAsTextureRegionPack("particles-tiki_smoke.tga"),
                    0.25f, hitbox.centeredX, hitbox.startY + 10, rng.nextInt(256)
            ))

            spawnTimer -= nextDelay
            nextDelay = rng.nextFloat() * 0.4f + 0.4f

            sprite?.delays?.set(0, rng.nextFloat() * 0.4f + 0.1f)
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