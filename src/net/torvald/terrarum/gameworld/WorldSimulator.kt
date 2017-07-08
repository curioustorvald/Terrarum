package net.torvald.terrarum.gameworld

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AnyPlayer
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex

/**
 * Created by minjaesong on 16-08-03.
 */
object WorldSimulator {
    /**
     * In tiles;
     * square width/height = field * 2
     */
    const val FLUID_UPDATING_SQUARE_RADIUS = 64 // larger value will have dramatic impact on performance
    const private val DOUBLE_RADIUS = FLUID_UPDATING_SQUARE_RADIUS * 2

    private val fluidMap = Array<ByteArray>(DOUBLE_RADIUS, { ByteArray(DOUBLE_RADIUS) })
    private val fluidTypeMap = Array<ByteArray>(DOUBLE_RADIUS, { ByteArray(DOUBLE_RADIUS) })

    const val DISPLACE_CAP = 4
    const val FLUID_MAX = 16

    var updateXFrom = 0
    var updateXTo = 0
    var updateYFrom = 0
    var updateYTo = 0

    val colourNone = Color(0x808080FF.toInt())
    val colourWater = Color(0x66BBFFFF.toInt())

    private val world = Terrarum.ingame!!.world

    operator fun invoke(p: AnyPlayer?, delta: Float) {
        if (p != null) {
            updateXFrom = p.hitbox.centeredX.div(FeaturesDrawer.TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundInt()
            updateYFrom = p.hitbox.centeredY.div(FeaturesDrawer.TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundInt()
            updateXTo = updateXFrom + DOUBLE_RADIUS
            updateYTo = updateYFrom + DOUBLE_RADIUS
        }

        moveFluids(delta)
        displaceFallables(delta)
    }

    /**
     * displace fluids. Note that the code assumes the gravity pulls things downward ONLY,
     * which means you'll need to modify the code A LOT if you're going to implement zero- or
     * reverse-gravity.
     *
     * Procedure: CP world fluidmap -> sim on fluidmap -> CP fluidmap world
     * TODO multithread
     */
    fun moveFluids(delta: Float) {
        ////////////////////
        // build fluidmap //
        ////////////////////
        purgeFluidMap()
        worldToFluidMap(world)


        /////////////////////////////////////////////////////////////
        // displace fluids. Record displacements into the fluidMap //
        /////////////////////////////////////////////////////////////
        for (y in updateYFrom..updateYTo) {
            for (x in updateXFrom..updateXTo) {
                val tile = world.getTileFromTerrain(x, y) ?: Block.STONE
                val tileBottom = world.getTileFromTerrain(x, y + 1) ?: Block.STONE
                val tileLeft = world.getTileFromTerrain(x - 1, y) ?: Block.STONE
                val tileRight = world.getTileFromTerrain(x + 1, y) ?: Block.STONE
                if (tile.isFluid()) {

                    // move down if not obstructed
                    /*if (!tileBottom.isSolid()) {
                        val drainage = drain(world, x, y, DISPLACE_CAP)
                        pour(world, x, y + 1, drainage)
                    }
                    // left and right both open
                    else if (!tileLeft.isSolid() && !tileRight.isSolid()) {
                        // half-breaker
                        val moreToTheRight = HQRNG().nextBoolean()
                        val displacement = drain(world, x, y, DISPLACE_CAP)

                        if (displacement.isEven()) {
                            pour(world, x - 1, y, displacement shr 1)
                            pour(world, x + 1, y, displacement shr 1)
                        }
                        else {
                            pour(world, x - 1, y, (displacement shr 1) + if (moreToTheRight) 0 else 1)
                            pour(world, x + 1, y, (displacement shr 1) + if (moreToTheRight) 1 else 0)
                        }
                    }
                    // left open
                    else if (!tileLeft.isSolid()) {
                        val displacement = drain(world, x, y, DISPLACE_CAP)
                        pour(world, x - 1, y, displacement)
                    }
                    // right open
                    else if (!tileRight.isSolid()) {
                        val displacement = drain(world, x, y, DISPLACE_CAP)
                        pour(world, x + 1, y, displacement)
                    }
                    // nowhere open; do default (fill top)
                    else {
                        pour(world, x, y - 1, DISPLACE_CAP)
                    }*/
                    if (!tileBottom.isSolid()) {
                        pour(x, y + 1, drain(x, y, FLUID_MAX))
                    }

                }
            }
        }


        /////////////////////////////////////////////////////
        // replace fluids in the map according to fluidMap //
        /////////////////////////////////////////////////////
        fluidMapToWorld(world)

    }

    /**
     * displace fallable tiles. It is scanned bottom-left first. To achieve the sens ofreal
     * falling, each tiles are displaced by ONLY ONE TILE below.
     */
    fun displaceFallables(delta: Float) {
        for (y in updateYFrom..updateYTo) {
            for (x in updateXFrom..updateXTo) {
                val tile = world.getTileFromTerrain(x, y) ?: Block.STONE
                val tileBelow = world.getTileFromTerrain(x, y + 1) ?: Block.STONE

                if (tile.isFallable()) {
                    // displace fluid. This statement must precede isSolid()
                    if (tileBelow.isFluid()) {
                        // remove tileThis to create air pocket
                        world.setTileTerrain(x, y, Block.AIR)

                        pour(x, y, drain(x, y, tileBelow.fluidLevel().toInt()))
                        // place our tile
                        world.setTileTerrain(x, y + 1, tile)
                    }
                    else if (!tileBelow.isSolid()) {
                        world.setTileTerrain(x, y, Block.AIR)
                        world.setTileTerrain(x, y + 1, tile)
                    }
                }
            }
        }
    }

    fun drawFluidMapDebug(batch: SpriteBatch) {
        batch.color = colourWater

        for (y in 0..fluidMap.size - 1) {
            for (x in 0..fluidMap[0].size - 1) {
                val data = fluidMap[y][x]
                if (BlocksDrawer.tileInCamera(x + updateXFrom, y + updateYFrom)) {
                    if (data == 0.toByte())
                        batch.color = colourNone
                    else
                        batch.color = colourWater

                    Terrarum.fontSmallNumbers.draw(batch,
                            data.toString(),
                            updateXFrom.plus(x).times(FeaturesDrawer.TILE_SIZE).toFloat()
                            + if (data < 10) 4f else 0f,
                            updateYFrom.plus(y).times(FeaturesDrawer.TILE_SIZE) + 4f
                    )
                }


                //if (data > 0) println(data)
            }
        }
    }

    private fun purgeFluidMap() {
        for (y in 1..DOUBLE_RADIUS) {
            for (x in 1..DOUBLE_RADIUS) {
                fluidMap[y - 1][x - 1] = 0
                fluidTypeMap[y - 1][x - 1] = 0
            }
        }
    }

    private fun worldToFluidMap(world: GameWorld) {
        for (y in updateYFrom..updateYTo) {
            for (x in updateXFrom..updateXTo) {
                val tile = world.getTileFromTerrain(x, y) ?: Block.STONE
                if (tile.isFluid()) {
                    fluidMap[y - updateYFrom][x - updateXFrom] = tile.fluidLevel().toByte()
                    fluidTypeMap[y - updateYFrom][x - updateXFrom] = tile.fluidType().toByte()
                }
            }
        }
    }

    private fun fluidMapToWorld(world: GameWorld) {
        for (y in 0..fluidMap.size - 1) {
            for (x in 0..fluidMap[0].size - 1) {
                placeFluid(world, updateXFrom + x, updateYFrom + y
                        , FluidCodex.FLUID_WATER, fluidMap[y][x] - 1
                )
                // FIXME test code: deals with water only!
            }
        }
    }

    fun Int.isFluid() = BlockCodex[this].isFluid
    fun Int.isSolid() = this.fluidLevel() == FLUID_MAX || BlockCodex[this].isSolid
    //fun Int.viscosity() = BlockCodex[this].
    fun Int.fluidLevel() = if (!this.isFluid()) 0 else (this % FLUID_MAX).plus(1)
    fun Int.fluidType() = (this / 16) // 0 - 255, 255 being water, 254 being lava
    fun Int.isEven() = (this and 0x01) == 0
    fun Int.isFallable() = BlockCodex[this].isFallable

    private fun placeFluid(world: GameWorld, x: Int, y: Int, fluidType: Byte, amount: Int) {
        if (world.layerTerrain.isInBound(x, y)) {
            if (amount > 0 && !world.getTileFromTerrain(x, y)!!.isSolid()) {
                world.setTileTerrain(x, y, fluidType, amount - 1)
            }
            else if (amount == 0 && world.getTileFromTerrain(x, y)!!.isFluid()) {
                world.setTileTerrain(x, y, Block.AIR)
            }
        }
    }

    /**
     * @param x and y: world tile coord
     * @return amount of fluid actually drained.
     * (intended drainage - this) will give you how much fluid is not yet drained.
     * TODO add fluidType support
     */
    private fun drain(x: Int, y: Int, amount: Int): Int {
        val displacement = Math.min(fluidMap[y - updateYFrom][x - updateXFrom].toInt(), amount)

        fluidMap[y - updateYFrom][x - updateXFrom] =
                (fluidMap[y - updateYFrom][x - updateXFrom] - displacement).toByte()

        return displacement
    }

    /**
     * @param x and y: world tile coord
     * TODO add fluidType support
     */
    private fun pour(x: Int, y: Int, amount: Int) {
        /**
         * @param x and y: world tile coord
         * @return spillage
         * TODO add fluidType support
         */
        fun pourInternal(worldXpos: Int, worldYPos: Int, volume: Int): Int {
            var spil = 0

            val addrX = worldXpos - updateXFrom
            val addrY = worldYPos - updateYFrom

            if (addrX >= 0 && addrY >= 0 && addrX < DOUBLE_RADIUS && addrY < DOUBLE_RADIUS) {
                fluidMap[addrY][addrX] = (fluidMap[addrY][addrX] + volume).toByte()
                if (fluidMap[addrY][addrX] > FLUID_MAX) {
                    spil = fluidMap[addrY][addrX] - FLUID_MAX
                    fluidMap[addrY][addrX] = FLUID_MAX.toByte()
                }
            }

            return spil
        }

        // pour the fluid
        var spillage = pourInternal(x, y, amount)

        if (spillage <= 0) return

        // deal with the spillage

        val tileUp = world.getTileFromTerrain(x - updateXFrom, y - updateYFrom - 1)
        val tileDown = world.getTileFromTerrain(x - updateXFrom, y - updateYFrom + 1)

        // try to fill downward
        if (tileDown != null && !tileDown.isSolid()) {
            spillage = pourInternal(x, y + 1, spillage)
        }
        // else, try to fill upward. if there is no space, just discard
        if (spillage >= 0 && tileUp != null && !tileUp.isSolid()) {
            pourInternal(x, y - 1, spillage)
        }
    }
}