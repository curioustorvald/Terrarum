package net.torvald.terrarum.gamemap

import net.torvald.random.HQRNG
import net.torvald.terrarum.gameactors.Player
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.tileproperties.TileNameCode
import net.torvald.terrarum.tileproperties.TilePropCodex
import org.newdawn.slick.Graphics

/**
 * Created by minjaesong on 16-08-03.
 */
object WorldSimulator {
    /**
     * In tiles;
     * square width/height = field * 2
     */
    const val FLUID_UPDATING_SQUARE_RADIUS = 128
    const private val DOUBLE_RADIUS = FLUID_UPDATING_SQUARE_RADIUS * 2

    private val fluidMap = Array<IntArray>(DOUBLE_RADIUS, { IntArray(DOUBLE_RADIUS) })

    const val DISPLACE_CAP = 4
    const val FLUID_MAX = 16

    operator fun invoke(world: GameWorld, p: Player, delta: Int) {
        moveFluids(world, p, delta)
    }

    /**
     * displace fluids. Note that the code assumes the gravity pulls things downward ONLY,
     * which means you'll need to modify the code A LOT if you're going to implement zero- or
     * reverse-gravity.
     */
    fun moveFluids(world: GameWorld, p: Player, delta: Int) {
        val updateXFrom = p.hitbox.centeredX.div(MapDrawer.TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundInt()
        val updateYFrom = p.hitbox.centeredY.div(MapDrawer.TILE_SIZE).minus(FLUID_UPDATING_SQUARE_RADIUS).roundInt()
        val updateXTo = updateXFrom + 1 * FLUID_UPDATING_SQUARE_RADIUS
        val updateYTo = updateYFrom + 1 * FLUID_UPDATING_SQUARE_RADIUS

        /**
         * @return amount of fluid actually drained.
         * (intended drainage - this) will give you how much fluid is not yet drained.
         */
        fun drain(x: Int, y: Int, amount: Int): Int {
            val displacement = Math.min(fluidMap[y - updateYFrom][x - updateXFrom], amount)

            fluidMap[y - updateYFrom][x - updateXFrom] -= displacement

            return displacement
        }

        fun pour(x: Int, y: Int, amount: Int) {
            fun pourInternal(xpos: Int, ypos: Int, volume: Int): Int {
                var spil = 0

                val addrX = xpos - updateXFrom
                val addrY = ypos - updateYFrom

                if (addrX >= 0 && addrY >= 0 && addrX < DOUBLE_RADIUS && addrY < DOUBLE_RADIUS) {
                    fluidMap[addrY][addrX] += volume
                    if (fluidMap[addrY][addrX] > FLUID_MAX) {
                        spil = fluidMap[addrY][addrX] - FLUID_MAX
                        fluidMap[addrY][addrX] = FLUID_MAX
                    }
                }

                return spil
            }

            // pour the fluid
            var spillage = pourInternal(x, y, amount)

            if (spillage == 0) return

            // deal with the spillage

            val tileUp = world.getTileFromTerrain(x - updateXFrom, y - updateYFrom - 1)
            val tileDown = world.getTileFromTerrain(x - updateXFrom, y - updateYFrom + 1)

            // try to fill downward
            if (tileDown != null && !tileDown.isSolid()) {
                spillage = pourInternal(x, y + 1, spillage)
            }
            // else, try to fill upward. if there is no space, just discard
            if (tileUp != null && !tileUp.isSolid()) {
                pourInternal(x, y - 1, spillage)
            }
        }

        purgeFluidMap()

        /////////////////////////////////////////////////////////////
        // displace fluids. Record displacements into the fluidMap //
        /////////////////////////////////////////////////////////////
        for (y in updateYFrom..updateYTo) {
            for (x in updateXFrom..updateXTo) {
                val tile = world.getTileFromTerrain(x, y)
                val tileBottom = world.getTileFromTerrain(x, y + 1)
                val tileLeft = world.getTileFromTerrain(x - 1, y)
                val tileRight = world.getTileFromTerrain(x + 1, y)
                if (tile != null && tile.isFluid()) {

                    // move down if not obstructed
                    if (tileBottom != null && !tileBottom.isSolid()) {
                        val drainage = drain(x, y, DISPLACE_CAP)
                        pour(x, y + 1, drainage)
                    }
                    // left and right both open (null is considered as open)
                    else if ((tileLeft != null && tileRight != null && !tileLeft.isSolid() && !tileRight.isSolid()) ||
                            tileLeft == null && tileRight == null) {
                        // half-breaker
                        val moreToTheRight = HQRNG().nextBoolean()
                        val displacement = drain(x, y, DISPLACE_CAP)

                        if (displacement.isEven()) {
                            pour(x - 1, y, displacement shr 1)
                            pour(x + 1, y, displacement shr 1)
                        }
                        else {
                            pour(x - 1, y, (displacement shr 1) + if (moreToTheRight) 0 else 1)
                            pour(x + 1, y, (displacement shr 1) + if (moreToTheRight) 1 else 0)
                        }
                    }
                    // left open (null is considered as open)
                    else if ((tileLeft != null && !tileLeft.isSolid()) || tileLeft == null) {
                        val displacement = drain(x, y, DISPLACE_CAP)
                        pour(x - 1, y, displacement)
                    }
                    // right open (null is considered as open)
                    else if ((tileRight != null && !tileRight.isSolid()) || tileRight == null) {
                        val displacement = drain(x, y, DISPLACE_CAP)
                        pour(x + 1, y, displacement)
                    }
                    // nowhere open; do default (fill top)
                    else {
                        pour(x, y - 1, DISPLACE_CAP)
                    }
                }
            }
        }
        /////////////////////////////////////////////////////
        // replace fluids in the map according to fluidMap //
        /////////////////////////////////////////////////////
        for (y in 0..fluidMap.size - 1) {
            for (x in 0..fluidMap[0].size - 1) {
                placeFluid(world, updateXFrom + x, updateYFrom + y, WATER, fluidMap[y][x].minus(1))
                // FIXME test code: deals with water only!
            }
        }

    }

    fun drawFluidMapDebug(p: Player, g: Graphics) {
        for (y in 0..fluidMap.size - 1) {
            for (x in 0..fluidMap[0].size - 1) {

            }
        }
    }

    private fun purgeFluidMap() {
        for (y in 1..DOUBLE_RADIUS)
            for (x in 1..DOUBLE_RADIUS)
                fluidMap[y - 1][x - 1] = 0
    }

    fun Int.isFluid() = TilePropCodex.getProp(this).isFluid
    fun Int.isSolid() = TilePropCodex.getProp(this).isSolid
    //fun Int.viscosity() = TilePropCodex.getProp(this).
    fun Int.fluidLevel() = this % FLUID_MAX
    fun Int.isEven() = (this and 0x01) == 0

    private fun placeFluid(world: GameWorld, x: Int, y: Int, tileFluid: Int, amount: Int) {
        if (world.layerTerrain.isInBound(x, y)) {
            if (amount > 0 && !world.getTileFromTerrain(x, y)!!.isSolid()) {
                world.setTileTerrain(x, y, amount.minus(1).plus(tileFluid))
            }
            else if (amount == 0 && world.getTileFromTerrain(x, y)!!.isFluid()) {
                world.setTileTerrain(x, y, TileNameCode.AIR)
            }
        }
    }

    val LAVA = TileNameCode.LAVA_1
    val WATER = TileNameCode.WATER_1
}