package net.torvald.terrarum.modulebasegame.gameworld

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.roundInt
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameworld.FluidCodex
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid

/**
 * Created by minjaesong on 2016-08-03.
 */
object WorldSimulator {

    // FLUID-RELATED STUFFS //

    /**
     * In tiles;
     * square width/height = field * 2
     */
    const val FLUID_UPDATING_SQUARE_RADIUS = 64 // larger value will have dramatic impact on performance
    const private val DOUBLE_RADIUS = FLUID_UPDATING_SQUARE_RADIUS * 2

    private val fluidMap = Array(DOUBLE_RADIUS, { FloatArray(DOUBLE_RADIUS) })
    private val fluidTypeMap = Array(DOUBLE_RADIUS, { IntArray(DOUBLE_RADIUS) })

    const val FLUID_MAX_MASS = 1f // The normal, un-pressurized mass of a full water cell
    const val FLUID_MAX_COMP = 0.02f // How much excess water a cell can store, compared to the cell above it
    const val FLUID_MIN_MASS = 0.0001f //Ignore cells that are almost dry

    // END OF FLUID-RELATED STUFFS

    var updateXFrom = 0
    var updateXTo = 0
    var updateYFrom = 0
    var updateYTo = 0

    val colourNone = Color(0x808080FF.toInt())
    val colourWater = Color(0x66BBFFFF.toInt())

    private val world = (Terrarum.ingame!!.world)

    operator fun invoke(p: ActorHumanoid?, delta: Float) {
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

    }

    /**
     * displace fallable tiles. It is scanned bottom-left first. To achieve the sens ofreal
     * falling, each tiles are displaced by ONLY ONE TILE below.
     */
    fun displaceFallables(delta: Float) {
        /*for (y in updateYFrom..updateYTo) {
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
        }*/
    }


    fun disperseHeat(delta: Float) {

    }

    private fun purgeFluidMap() {
        for (y in 1..DOUBLE_RADIUS) {
            for (x in 1..DOUBLE_RADIUS) {
                fluidMap[y - 1][x - 1] = 0f
                fluidTypeMap[y - 1][x - 1] = Fluid.NULL
            }
        }
    }


    fun Int.isFallable() = BlockCodex[this].isFallable



}