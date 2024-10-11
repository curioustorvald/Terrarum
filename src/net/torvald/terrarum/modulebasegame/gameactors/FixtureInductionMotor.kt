package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInductionMotor.Companion.MASS
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import kotlin.math.absoluteValue

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

    companion object {
        const val MASS = 20.0
    }
}

/**
 * Created by minjaesong on 2024-10-05.
 */
class FixtureGearbox : Electric, Reorientable {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 3, 3),
        nameFun = { Lang["ITEM_GEARBOX"] }
    )

    override fun getBlockBoxPositions(posX: Int, posY: Int): List<Pair<Int, Int>> {
        return listOf(posX+1 to posY+1)
    }

    override fun spawnCustomGetSpawningOffset() = 0 to 1

    override fun orientClockwise() {
        orientation = 1 - orientation
        reorient(); setEmitterAndSink(); updateSignal()
    }

    override fun orientAnticlockwise() {
        orientation = 1 - orientation
        reorient(); setEmitterAndSink(); updateSignal()
    }

    override var orientation = 0 // 0 or 1

    private fun reorient() {
        (sprite as SheetSpriteAnimation).currentRow = orientation
    }

    private fun setEmitterAndSink() {
        clearStatus()
        when (orientation) {
            0 -> {
                posVecsIn.forEach { (x, y) ->
                    setWireSinkAt(x, y, "axle")
                }
                posVecsOut.forEach { (x, y) ->
                    setWireEmitterAt(x, y, "axle")
                }
            }
            1 -> {
                posVecsIn.forEach { (x, y) ->
                    setWireEmitterAt(x, y, "axle")
                }
                posVecsOut.forEach { (x, y) ->
                    setWireSinkAt(x, y, "axle")
                }
            }
        }
    }

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/gearbox.tga")

        density = 7874.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE, TILE_SIZE+1)).let {
            it.setRowsAndFrames(2,1)
        }

        actorValue[AVKey.BASEMASS] = MASS

        setEmitterAndSink()
    }


    override fun updateImpl(delta: Float) {
        // re-position the hitbox because wtf
        worldBlockPos?.let { (x, y) ->
            val x = (x+1) * TILE_SIZED; val y = (y+1) * TILE_SIZED
            hitbox.setFromTwoPoints(x, y, x + TILE_SIZED, y + TILE_SIZED)
        }

        super.updateImpl(delta)
    }

    @Transient private var speedMax = 0.0

    override fun updateSignal() {
        val a = when (orientation) {
            0 -> posVecsIn
            1 -> posVecsOut
            else -> throw InternalError()
        }
        val b = when (orientation) {
            0 -> posVecsOut
            1 -> posVecsIn
            else -> throw InternalError()
        }


        var torqueMin = Double.POSITIVE_INFINITY

        speedMax = 0.0
        a.forEach {
            val vec = getWireStateAt(it.x, it.y, "axle")
            if (vec.x.absoluteValue >= ELECTRIC_EPSILON_GENERIC && vec.x >= speedMax) {
                speedMax = vec.x
            }
            if (vec.y.absoluteValue >= ELECTRIC_EPSILON_GENERIC && vec.y <= torqueMin) {
                torqueMin = if (vec.y == Double.POSITIVE_INFINITY) 0.0 else vec.y
            }
        }

        if (torqueMin == Double.POSITIVE_INFINITY) torqueMin = 0.0


        b.forEach { (x, y) ->
            setWireEmissionAt(x, y, Vector2(speedMax, torqueMin))
        }
    }

    companion object {
        @Transient val posVecsIn = listOf(
//            Point2i(1, 0),
            Point2i(0, 1),
            Point2i(2, 1),
//            Point2i(1, 2),
        )

        @Transient val posVecsOut = listOf(
            Point2i(1, 0),
//            Point2i(0, 1),
//            Point2i(2, 1),
            Point2i(1, 2),
        )
    }
}