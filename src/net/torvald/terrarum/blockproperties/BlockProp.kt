package net.torvald.terrarum.blockproperties

import net.torvald.gdx.graphics.Cvec
import net.torvald.random.XXHash32
import net.torvald.terrarum.*
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.fmod

/**
 * Created by minjaesong on 2016-02-16.
 */
class BlockProp : TaggedProp {

    var id: ItemID = ""
    var numericID: Int = -1

    var nameKey: String = ""

    var shadeColR = 0f
    var shadeColG = 0f
    var shadeColB = 0f
    var shadeColA = 0f

    var opacity = Cvec()

    fun getOpacity(channel: Int) = when (channel) {
        0 -> shadeColR
        1 -> shadeColG
        2 -> shadeColB
        3 -> shadeColA
        else -> throw IllegalArgumentException("Invalid channel $channel")
    }

    var strength: Int = 0
    var density: Int = 0

    /** isSolid is NOT SAME AS !isOpaqueis
     * Like, don't ever use this vars to tell this block should be removed by water or something,
     * because PLANTS ARE ACTORS, TREES ARE BLOCKS, stupid myself!
     */
    var isSolid: Boolean = false
    //var isClear: Boolean = false
    var isPlatform: Boolean = false
    var isWallable: Boolean = false
    var isVertFriction: Boolean = false


    internal var baseLumColR = 0f // base value used to calculate dynamic luminosity
    internal var baseLumColG = 0f // base value used to calculate dynamic luminosity
    internal var baseLumColB = 0f // base value used to calculate dynamic luminosity
    internal var baseLumColA = 0f // base value used to calculate dynamic luminosity
    internal val baseLumCol = Cvec(0)
    //var lumColR = 0f // memoised value of dynamic luminosity
    //var lumColG = 0f // memoised value of dynamic luminosity
    //var lumColB = 0f // memoised value of dynamic luminosity
    //var lumColA = 0f // memoised value of dynamic luminosity
    internal val _lumCol = Cvec(0)
    // X- and Y-value must be treated properly beforehand! (use GameWorld.coerceXY())
    fun getLumCol(x: Int, y: Int) = if (dynamicLuminosityFunction == 0) {
        baseLumCol
    } else {
        val offset = XXHash32.hashGeoCoord(x, y).fmod(BlockCodex.DYNAMIC_RANDOM_CASES)
        BlockCodex[BlockCodex.tileToVirtual[id]!![offset]]._lumCol
    }

    fun getLumCol(x: Int, y: Int, channel: Int): Float = if (dynamicLuminosityFunction == 0) {
        baseLumCol.lane(channel)
    } else {
        val offset = XXHash32.hashGeoCoord(x, y).fmod(BlockCodex.DYNAMIC_RANDOM_CASES)
        BlockCodex[BlockCodex.tileToVirtual[id]!![offset]]._lumCol.lane(channel)
    }

    override fun hasTag(s: String) = tags.contains(s)

    /**
     * @param luminosity
     */
    //inline val luminosity: Cvec
    //    get() = BlockPropUtil.getDynamicLumFunc(internalLumCol, dynamicLuminosityFunction)

    //fun getLum(channel: Int) = lumCol.getElem(channel)

    var drop: ItemID = ""
    var world: ItemID = ""

    var maxSupport: Int = -1 // couldn't use NULL at all...

    var friction: Int = 0

    var dynamicLuminosityFunction: Int = 0

    var material: String = ""

    var reflectance = 0f // the exact colour of the reflected light depends on the texture

    @Transient var rngBase0 = Math.random().toFloat() // initial cycle phase (xxxxFuncX)
    @Transient var rngBase1 = Math.random().toFloat() // flicker P0, etc
    @Transient var rngBase2 = Math.random().toFloat() // flicker P1, etc

    @Transient var tags = HashSet<String>()

    /**
     * Mainly intended to be used by third-party modules
     */
    val extra = Codex()

    var isActorBlock: Boolean = false

    /**
     * Is this tile should be treated as "solid" for tile connecting.
     */
    val isSolidForTileCnx: Boolean
        get() = if (tags.contains("DORENDER") || !isActorBlock) isSolid else false

}