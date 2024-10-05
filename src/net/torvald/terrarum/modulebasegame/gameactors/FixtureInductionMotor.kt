package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInductionMotor.Companion.MASS
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIItemInventoryCellCommonRes.tooltipShowing
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/**
 * Created by minjaesong on 2024-10-03.
 */
class FixtureInductionMotor : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 3, 1),
        nameFun = { Lang["ITEM_INDUCTION_MOTOR"] }
    )

    override fun getBlockBoxPositions(posX: Int, posY: Int): List<Pair<Int, Int>> {
        return listOf(posX+1 to posY)
    }

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/induction_motor.tga")

        density = 7874.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE, TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = MASS

        setWireEmitterAt(0, 0, "axle")
        setWireEmitterAt(2, 0, "axle")
        setWireEmissionAt(0, 0, Vector2(16.0, 1024.0)) // speed and torque
        setWireEmissionAt(2, 0, Vector2(16.0, 1024.0)) // speed and torque
    }

    override fun updateSignal() {
        setWireEmissionAt(0, 0, Vector2(16.0, 1024.0)) // speed and torque
        setWireEmissionAt(2, 0, Vector2(16.0, 1024.0)) // speed and torque
    }

    override fun dispose() {
        tooltipShowing.remove(tooltipHash)
    }

    companion object {
        const val MASS = 20.0
    }
}

/**
 * Created by minjaesong on 2024-10-05.
 */
class FixtureGearbox : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 3, 3),
        nameFun = { Lang["ITEM_GEARBOX"] }
    )

    override fun getBlockBoxPositions(posX: Int, posY: Int): List<Pair<Int, Int>> {
        return listOf(posX+1 to posY+1)
    }

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/gearbox.tga")

        density = 7874.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, TILE_SIZE)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE, TILE_SIZE+1)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = MASS

        posVecs.forEach { (x, y) ->
            setWireEmitterAt(x, y, "axle")
            setWireSinkAt(x, y, "axle")
        }
    }

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        // animate the sprite
        val worldX = intTilewiseHitbox.startX.toInt()
        val worldY = intTilewiseHitbox.startY.toInt()

        val targetTick = (App.TICK_SPEED / speedMax).let {
            it.coerceIn(0.0, Long.MAX_VALUE.toDouble())
        }.roundToLong()

        val sprite = (sprite as SheetSpriteAnimation)

        val phaseShift = if (worldY % 2 == 0)
            (worldX % 2 == 1)
        else
            (worldX % 2 == 0)


        if (targetTick > 0) {
            (INGAME.WORLD_UPDATE_TIMER % (targetTick * 2)).let {
                sprite.currentFrame =
                    if (phaseShift)
                        (it < targetTick).toInt()
                    else
                        (it >= targetTick).toInt()
            }
        }
    }

    @Transient private var speedMax = 0.0

    private var maxSpeedLoc = ArrayList<Point2i>()
    private var minTorqueLoc = ArrayList<Point2i>()

    @Transient private val newMaxSpeedLoc = ArrayList<Point2i>()
    @Transient private val newMinTorqueLoc = ArrayList<Point2i>()

    override fun updateSignal() {
        var torqueMin = Double.POSITIVE_INFINITY
        newMaxSpeedLoc.clear()
        newMinTorqueLoc.clear()

        speedMax = 0.0
        posVecs.forEach {
            val vec = getWireStateAt(it.x, it.y, "axle")
            if (!maxSpeedLoc.contains(it) && vec.x >= speedMax) {
                newMaxSpeedLoc.add(Point2i(it.x, it.y))
                speedMax = vec.x
            }
            if (!minTorqueLoc.contains(it) && vec.y.absoluteValue >= ELECTRIC_EPSILON_GENERIC && vec.y <= torqueMin) {
                newMinTorqueLoc.add(Point2i(it.x, it.y))
                torqueMin = if (vec.y == Double.POSITIVE_INFINITY) 0.0 else vec.y
            }

            // FIXME: intTilewiseHitbox discrepancy with spawn position.
            val wx = it.x + intTilewiseHitbox.startX.toInt()
            val wy = it.y + intTilewiseHitbox.startY.toInt()
            print("$wx,$wy\t")

            if (maxSpeedLoc.contains(it))
                println("$it*\t$vec")
            else
                println("$it\t$vec")
        }
        println("--------")

        maxSpeedLoc.clear(); maxSpeedLoc.addAll(newMaxSpeedLoc)
        minTorqueLoc.clear(); minTorqueLoc.addAll(newMinTorqueLoc)

        posVecs.forEach { (x, y) ->
            setWireEmissionAt(x, y, Vector2(speedMax, torqueMin))
        }
    }

    override fun dispose() {
        tooltipShowing.remove(tooltipHash)
    }

    companion object {
        @Transient val posVecs = listOf(
            Point2i(1, 0),
            Point2i(0, 1),
            Point2i(2, 1),
            Point2i(1, 2),
        )
    }
}