package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.HQRNG
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.gameparticles.ParticleVanishingSprite
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2016-06-17.
 */
internal class FixtureTikiTorch : FixtureBase, Luminous {

    private val rng = HQRNG()
    private val rndHash1 = rng.nextInt()
    private val rndHash2 = rng.nextInt()

    override var color: Cvec
        get() = BlockCodex[Block.TORCH].getLumCol(rndHash1, rndHash2)
        set(value) {
            throw UnsupportedOperationException()
        }

    override val lightBoxList: ArrayList<Hitbox> = ArrayList(1)

    constructor() : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 2),
            nameFun = { Lang["ITEM_TIKI_TORCH"] }
    ) {

        // loading textures
        CommonResourcePool.addToLoadingList("sprites-fixtures-tiki_torch.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/fixtures/tiki_torch.tga"), 16, 32)
        }
        CommonResourcePool.addToLoadingList("particles-tiki_smoke.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "particles/bigger_smoke.tga"), 16, 16)
        }
        CommonResourcePool.loadAll()

        density = 1200.0

        setHitboxDimension(16, 32, 0, 0)

        lightBoxList.add(Hitbox(6.0, 5.0, 4.0, 3.0))

        makeNewSprite(CommonResourcePool.getAsTextureRegionPack("sprites-fixtures-tiki_torch.tga"))
        sprite!!.setRowsAndFrames(1, 2)

        actorValue[AVKey.BASEMASS] = MASS
    }

    private var nextDelay = 0.25f
    private var spawnTimer = 0f

    override fun update(delta: Float) {
        super.update(delta)

        if (spawnTimer >= nextDelay) {
            (Terrarum.ingame as TerrarumIngame).addParticle(ParticleVanishingSprite(
                    CommonResourcePool.getAsTextureRegionPack("particles-tiki_smoke.tga"),
                    25f, true, hitbox.centeredX, hitbox.startY, false, rng.nextInt(256)
            ))

            spawnTimer -= nextDelay
            nextDelay = rng.nextFloat() * 0.25f + 0.25f

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