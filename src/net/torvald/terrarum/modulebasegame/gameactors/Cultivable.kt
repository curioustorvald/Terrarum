package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.gameitems.PickaxeCore
import net.torvald.terrarum.modulebasegame.worldgenerator.Treegen
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2024-02-03.
 */
open class Cultivable: FixtureBase {

    open var currentGrowth = 0f
    @Transient var maxGrowth: Int = 0; private set
    @Transient open val growthPerTick = 1f
    @Transient open val growthRandomness = 0.33333334f
    open var growthBonusMult = 1f

    override fun canSpawnOnThisFloor(itemID: ItemID) = BlockCodex[itemID].hasTag("CULTIVABLE")

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 1, 2),
        nameFun = { " " }
    )

    constructor(maxGrowth: Int) : super(
        BlockBox(BlockBox.NO_COLLISION, 1, 2),
        nameFun = { " " }
    ) {
        this.maxGrowth = maxGrowth
    }

    fun tickGrowthCounter() {
        val worldTimeDelta = INGAME.world.worldTime.timeDelta
        val rnd = 1f + (((Math.random() * 2.0) - 1.0) * growthRandomness)
        currentGrowth += growthPerTick * worldTimeDelta * growthBonusMult * rnd.toFloat()
    }

    open fun tryToSpawnMaturePlant() {

    }

}


/**
 * Created by minjaesong on 2024-02-03.
 */
open class SaplingBase(val species: Int) : Cultivable(72000) {
    private val variant = (0..3).random()
    init {
        CommonResourcePool.addToLoadingList("basegame/sprites/saplings.tga") {
            TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/saplings.tga"), 16, 32)
        }
        CommonResourcePool.loadAll()

        makeNewSprite(CommonResourcePool.getAsTextureRegionPack("basegame/sprites/saplings.tga")).let {
            it.setRowsAndFrames(4,4)
        }
    }

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        // these have to run every frame to make the sprite static
        (sprite as SheetSpriteAnimation).currentRow = species
        (sprite as SheetSpriteAnimation).currentFrame = variant

        // check for soil
        val groundTile = INGAME.world.getTileFromTerrain(intTilewiseHitbox.startX.toInt(), intTilewiseHitbox.endY.toInt() + 1)
        if (BlockCodex[groundTile].hasNoTagOf("CULTIVABLE")) {
            despawnHook = {
                printdbg(this, "Sapling despawn!")
                PickaxeCore.dropItem("item@basegame:${160 + species}", intTilewiseHitbox.canonicalX.toInt(), intTilewiseHitbox.canonicalY.toInt())
            }
            flagDespawn()
        }

        if (!flagDespawn) {
            tickGrowthCounter()

//            printdbg(this, "growth=$currentGrowth/$maxGrowth")

            if (currentGrowth >= maxGrowth) {
                tryToSpawnMaturePlant()
            }
        }
    }
    private var treeHasBeenGrown = false
    override fun tryToSpawnMaturePlant() {
        if (INGAME.WORLD_UPDATE_TIMER % 3 == 2) {
            val size = if (Math.random() < 0.1) 2 else 1
            val result = Treegen.plantTree(INGAME.world, intTilewiseHitbox.startX.toInt(), intTilewiseHitbox.endY.toInt() + 1, species, size)

            if (result) {
                treeHasBeenGrown = true
                flagDespawn()
            }
        }
    }

    /**
     * this function will be called when:
     * 1. player removes a sapling that has not yet matured
     * 2. the sapling is matured and the tree is about to be spawned
     */

    override fun despawn() {

        if (canBeDespawned) {
            printdbg(this, "despawn at T${INGAME.WORLD_UPDATE_TIMER}: ${nameFun()}")
//            printStackTrace(this)

            // remove filler block
            if (!treeHasBeenGrown) {
                forEachBlockbox { x, y, _, _ ->
                    world!!.setTileTerrain(x, y, Block.AIR, true)
                }
            }

            worldBlockPos = null
            mainUI?.dispose()

            this.isVisible = false

            despawnHook(this)
        }
        else {
            printdbg(this, "failed to despawn at T${INGAME.WORLD_UPDATE_TIMER}: ${nameFun()}")
            printdbg(this, "cannot despawn a fixture with non-empty inventory")
        }
    }
}

class SaplingOak : SaplingBase(0)
class SaplingEbony : SaplingBase(1)
class SaplingBirch : SaplingBase(2)
class SaplingRosewood : SaplingBase(3)
