package net.torvald.terrarum.worlddrawer

import net.torvald.gdx.graphics.Cvec
import net.torvald.gdx.graphics.UnsafeCvecArray
import net.torvald.terrarum.blockproperties.Block.AIR
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.ui.abs
import net.torvald.terrarum.realestate.LandUtil
import kotlin.system.exitProcess

/**
 * Created by minjaesong on 2020-03-04
 */

internal class LightCalculatorContext(
        private val world: GameWorld,
        private val lightmap: UnsafeCvecArray,
        private val lanternMap: HashMap<BlockAddress, Cvec>
) {

    private val colourNull = Cvec(0)

    private val ambientAccumulator = Cvec(0f,0f,0f,0f)
    private val lightLevelThis = Cvec(0)
    private val fluidAmountToCol = Cvec(0)
    private val thisTileLuminosity = Cvec(0)
    private val thisTileOpacity = Cvec(0)
    private val thisTileOpacity2 = Cvec(0) // thisTileOpacity * sqrt(2)
    private val sunLight = Cvec(0)
    private var thisFluid = GameWorld.FluidInfo(Fluid.NULL, 0f)
    private var thisTerrain = 0
    private var thisWall = 0

    private fun getLightsAndShades(x: Int, y: Int) {
        val (x, y) = world.coerceXY(x, y)

        lightLevelThis.set(colourNull)
        thisTerrain = world.getTileFromTerrainRaw(x, y)
        thisFluid = world.getFluid(x, y)
        thisWall = world.getTileFromWallRaw(x, y)

        // regarding the issue #26
        // uncomment this if you're facing diabolically indescribable bugs
        /*try {
            val fuck = BlockCodex[thisTerrain].getLumCol(x, y)
        }
        catch (e: NullPointerException) {
            System.err.println("## NPE -- x: $x, y: $y, value: ${thisTerrain}")
            e.printStackTrace()
            // create shitty minidump
            System.err.println("MINIMINIDUMP START")
            for (xx in x - 16 until x + 16) {
                val raw = world.getTileFromTerrain(xx, y)
                val lsb = raw.and(0xff).toString(16).padStart(2, '0')
                val msb = raw.ushr(8).and(0xff).toString(16).padStart(2, '0')
                System.err.print(lsb)
                System.err.print(msb)
                System.err.print(" ")
            }
            System.err.println("\nMINIMINIDUMP END")

            exitProcess(1)
        }*/

        if (thisFluid.type != Fluid.NULL) {
            fluidAmountToCol.set(thisFluid.amount, thisFluid.amount, thisFluid.amount, thisFluid.amount)

            thisTileLuminosity.set(BlockCodex[thisTerrain].getLumCol(x, y))
            thisTileLuminosity.maxAndAssign(BlockCodex[thisFluid.type].getLumCol(x, y).mul(fluidAmountToCol)) // already been div by four
            thisTileOpacity.set(BlockCodex[thisTerrain].opacity)
            thisTileOpacity.maxAndAssign(BlockCodex[thisFluid.type].opacity.mul(fluidAmountToCol)) // already been div by four
        }
        else {
            thisTileLuminosity.set(BlockCodex[thisTerrain].getLumCol(x, y))
            thisTileOpacity.set(BlockCodex[thisTerrain].opacity)
        }

        thisTileOpacity2.set(thisTileOpacity); thisTileOpacity2.mul(1.41421356f)
        //sunLight.set(world.globalLight); sunLight.mul(DIV_FLOAT) // moved to fireRecalculateEvent()


        // open air || luminous tile backed by sunlight
        if ((thisTerrain == AIR && thisWall == AIR) || (thisTileLuminosity.nonZero() && thisWall == AIR)) {
            lightLevelThis.set(sunLight)
        }

        // blend lantern
        lightLevelThis.maxAndAssign(thisTileLuminosity).maxAndAssign(lanternMap[LandUtil.getBlockAddr(world, x, y)] ?: colourNull)
    }

    fun calculateAndAssign(worldX: Int, worldY: Int) {

        //if (inNoopMask(worldX, worldY)) return

        // O(9n) == O(n) where n is a size of the map

        getLightsAndShades(worldX, worldY)

        val x = worldX.convX()
        val y = worldY.convY()

        // calculate ambient
        /*  + * +  0 4 1
         *  * @ *  6 @ 7
         *  + * +  2 5 3
         *  sample ambient for eight points and apply attenuation for those
         *  maxblend eight values and use it
         */

        // will "overwrite" what's there in the lightmap if it's the first pass
        // takes about 2 ms on 6700K
        /* + */lightLevelThis.maxAndAssign(LightmapRenderer.darkenColoured(x - 1, y - 1, thisTileOpacity2))
        /* + */lightLevelThis.maxAndAssign(LightmapRenderer.darkenColoured(x + 1, y - 1, thisTileOpacity2))
        /* + */lightLevelThis.maxAndAssign(LightmapRenderer.darkenColoured(x - 1, y + 1, thisTileOpacity2))
        /* + */lightLevelThis.maxAndAssign(LightmapRenderer.darkenColoured(x + 1, y + 1, thisTileOpacity2))
        /* * */lightLevelThis.maxAndAssign(LightmapRenderer.darkenColoured(x, y - 1, thisTileOpacity))
        /* * */lightLevelThis.maxAndAssign(LightmapRenderer.darkenColoured(x, y + 1, thisTileOpacity))
        /* * */lightLevelThis.maxAndAssign(LightmapRenderer.darkenColoured(x - 1, y, thisTileOpacity))
        /* * */lightLevelThis.maxAndAssign(LightmapRenderer.darkenColoured(x + 1, y, thisTileOpacity))


        //return lightLevelThis.cpy() // it HAS to be a cpy(), otherwise all cells gets the same instance
        //setLightOf(lightmap, x, y, lightLevelThis.cpy())

        lightmap.setR(x, y, lightLevelThis.r)
        lightmap.setG(x, y, lightLevelThis.g)
        lightmap.setB(x, y, lightLevelThis.b)
        lightmap.setA(x, y, lightLevelThis.a)
    }

    private fun Cvec.maxAndAssign(other: Cvec): Cvec {
        // TODO investigate: if I use assignment instead of set(), it blackens like the vector branch.  --Torvald, 2019-06-07
        //                   that was because you forgot 'this.r/g/b/a = ' part, bitch. --Torvald, 2019-06-07
        this.r = if (this.r > other.r) this.r else other.r
        this.g = if (this.g > other.g) this.g else other.g
        this.b = if (this.b > other.b) this.b else other.b
        this.a = if (this.a > other.a) this.a else other.a

        return this
    }

    private fun Cvec.nonZero() = this.r.abs() > LightmapRenderer.epsilon ||
                                 this.g.abs() > LightmapRenderer.epsilon ||
                                 this.b.abs() > LightmapRenderer.epsilon ||
                                 this.a.abs() > LightmapRenderer.epsilon

    /** World coord to array coord */
    private inline fun Int.convX() = this - LightmapRenderer.for_x_start + LightmapRenderer.overscan_open
    /** World coord to array coord */
    private inline fun Int.convY() = this - LightmapRenderer.for_y_start + LightmapRenderer.overscan_open
}