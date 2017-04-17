package net.torvald.terrarum.tileproperties

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-02-21.
 */
object Tile {

    val AIR = 0 // hard coded; this is the standard

    val STONE = TileCodex.idDamageToIndex(1, 0)
    val STONE_QUARRIED = TileCodex.idDamageToIndex(1, 1)
    val STONE_TILE_WHITE = TileCodex.idDamageToIndex(1, 2)
    val STONE_BRICKS = TileCodex.idDamageToIndex(1, 3)

    val DIRT = TileCodex.idDamageToIndex(2, 0)
    val GRASS = TileCodex.idDamageToIndex(2, 1)

    val PLANK_NORMAL = TileCodex.idDamageToIndex(3, 0)
    val PLANK_EBONY = TileCodex.idDamageToIndex(3, 1)
    val PLANK_BIRCH = TileCodex.idDamageToIndex(3, 2)
    val PLANK_BLOODROSE = TileCodex.idDamageToIndex(3, 3)

    val TRUNK_NORMAL = TileCodex.idDamageToIndex(4, 0)
    val TRUNK_EBONY = TileCodex.idDamageToIndex(4, 1)
    val TRUNK_BIRCH = TileCodex.idDamageToIndex(4, 2)
    val TRUNK_BLOODROSE = TileCodex.idDamageToIndex(4, 3)

    val SAND = TileCodex.idDamageToIndex(5, 0)
    val SAND_WHITE = TileCodex.idDamageToIndex(5, 1)
    val SAND_RED = TileCodex.idDamageToIndex(5, 2)
    val SAND_DESERT = TileCodex.idDamageToIndex(5, 3)
    val SAND_BLACK = TileCodex.idDamageToIndex(5, 4)
    val SAND_GREEN = TileCodex.idDamageToIndex(5, 5)

    val GRAVEL = TileCodex.idDamageToIndex(6, 0)
    val GRAVEL_GREY = TileCodex.idDamageToIndex(6, 1)

    val ORE_COPPER = TileCodex.idDamageToIndex(7, 0)
    val ORE_IRON = TileCodex.idDamageToIndex(7, 1)
    val ORE_GOLD = TileCodex.idDamageToIndex(7, 2)
    val ORE_SILVER = TileCodex.idDamageToIndex(7, 3)
    val ORE_ILMENITE = TileCodex.idDamageToIndex(7, 4)
    val ORE_AURICHALCUM = TileCodex.idDamageToIndex(7, 5)

    val RAW_RUBY = TileCodex.idDamageToIndex(8, 0)
    val RAW_EMERALD = TileCodex.idDamageToIndex(8, 1)
    val RAW_SAPPHIRE = TileCodex.idDamageToIndex(8, 2)
    val RAW_TOPAZ = TileCodex.idDamageToIndex(8, 3)
    val RAW_DIAMOND = TileCodex.idDamageToIndex(8, 4)
    val RAW_AMETHYST = TileCodex.idDamageToIndex(8, 5)

    val SNOW = TileCodex.idDamageToIndex(9, 0)
    val ICE_FRAGILE = TileCodex.idDamageToIndex(9, 1)
    val ICE_NATURAL = TileCodex.idDamageToIndex(9, 2)
    val ICE_MAGICAL = TileCodex.idDamageToIndex(9, 3)

    val GLASS_CRUDE = TileCodex.idDamageToIndex(9, 4)
    val GLASS_CLEAN = TileCodex.idDamageToIndex(9, 5)

    val PLATFORM_STONE = TileCodex.idDamageToIndex(10, 0)
    val PLATFORM_WOODEN = TileCodex.idDamageToIndex(10, 1)
    val PLATFORM_EBONY = TileCodex.idDamageToIndex(10, 2)
    val PLATFORM_BIRCH = TileCodex.idDamageToIndex(10, 3)
    val PLATFORM_BLOODROSE = TileCodex.idDamageToIndex(10, 4)

    val TORCH = TileCodex.idDamageToIndex(11, 0)
    val TORCH_FROST = TileCodex.idDamageToIndex(11, 1)

    val TORCH_OFF = TileCodex.idDamageToIndex(12, 0)
    val TORCH_FROST_OFF = TileCodex.idDamageToIndex(12, 1)

    val ILLUMINATOR_WHITE = TileCodex.idDamageToIndex(13, 0)
    val ILLUMINATOR_YELLOW = TileCodex.idDamageToIndex(13, 1)
    val ILLUMINATOR_ORANGE = TileCodex.idDamageToIndex(13, 2)
    val ILLUMINATOR_RED = TileCodex.idDamageToIndex(13, 3)
    val ILLUMINATOR_FUCHSIA = TileCodex.idDamageToIndex(13, 4)
    val ILLUMINATOR_PURPLE = TileCodex.idDamageToIndex(13, 5)
    val ILLUMINATOR_BLUE = TileCodex.idDamageToIndex(13, 6)
    val ILLUMINATOR_CYAN = TileCodex.idDamageToIndex(13, 7)
    val ILLUMINATOR_GREEN = TileCodex.idDamageToIndex(13, 8)
    val ILLUMINATOR_GREEN_DARK = TileCodex.idDamageToIndex(13, 9)
    val ILLUMINATOR_BROWN = TileCodex.idDamageToIndex(13, 10)
    val ILLUMINATOR_TAN = TileCodex.idDamageToIndex(13, 11)
    val ILLUMINATOR_GREY_LIGHT = TileCodex.idDamageToIndex(13, 12)
    val ILLUMINATOR_GREY_MED = TileCodex.idDamageToIndex(13, 13)
    val ILLUMINATOR_GREY_DARK = TileCodex.idDamageToIndex(13, 14)
    val ILLUMINATOR_BLACK = TileCodex.idDamageToIndex(13, 15)

    val ILLUMINATOR_WHITE_OFF = TileCodex.idDamageToIndex(14, 0)
    val ILLUMINATOR_YELLOW_OFF = TileCodex.idDamageToIndex(14, 1)
    val ILLUMINATOR_ORANGE_OFF = TileCodex.idDamageToIndex(14, 2)
    val ILLUMINATOR_RED_OFF = TileCodex.idDamageToIndex(14, 3)
    val ILLUMINATOR_FUCHSIA_OFF = TileCodex.idDamageToIndex(14, 4)
    val ILLUMINATOR_PURPLE_OFF = TileCodex.idDamageToIndex(14, 5)
    val ILLUMINATOR_BLUE_OFF = TileCodex.idDamageToIndex(14, 6)
    val ILLUMINATOR_CYAN_OFF = TileCodex.idDamageToIndex(14, 7)
    val ILLUMINATOR_GREEN_OFF = TileCodex.idDamageToIndex(14, 8)
    val ILLUMINATOR_GREEN_DARK_OFF = TileCodex.idDamageToIndex(14, 9)
    val ILLUMINATOR_BROWN_OFF = TileCodex.idDamageToIndex(14, 10)
    val ILLUMINATOR_TAN_OFF = TileCodex.idDamageToIndex(14, 11)
    val ILLUMINATOR_GREY_LIGHT_OFF = TileCodex.idDamageToIndex(14, 12)
    val ILLUMINATOR_GREY_MED_OFF = TileCodex.idDamageToIndex(14, 13)
    val ILLUMINATOR_GREY_DARK_OFF = TileCodex.idDamageToIndex(14, 14)
    val ILLUMINATOR_BLACK_OFF = TileCodex.idDamageToIndex(14, 15)

    val SANDSTONE = TileCodex.idDamageToIndex(15, 0)
    val SANDSTONE_WHITE = TileCodex.idDamageToIndex(15, 1)
    val SANDSTONE_RED = TileCodex.idDamageToIndex(15, 2)
    val SANDSTONE_DESERT = TileCodex.idDamageToIndex(15, 3)
    val SANDSTONE_BLACK = TileCodex.idDamageToIndex(15, 4)
    val SANDSTONE_GREEN = TileCodex.idDamageToIndex(15, 5)

    val LANTERN = TileCodex.idDamageToIndex(16, 0)
    val SUNSTONE = TileCodex.idDamageToIndex(16, 1)
    val DAYLIGHT_CAPACITOR = TileCodex.idDamageToIndex(16, 2)

    val WATER_1 =  TileCodex.idDamageToIndex(255, 0)
    val WATER_2 =  TileCodex.idDamageToIndex(255, 1)
    val WATER_3 =  TileCodex.idDamageToIndex(255, 2)
    val WATER_4 =  TileCodex.idDamageToIndex(255, 3)
    val WATER_5 =  TileCodex.idDamageToIndex(255, 4)
    val WATER_6 =  TileCodex.idDamageToIndex(255, 5)
    val WATER_7 =  TileCodex.idDamageToIndex(255, 6)
    val WATER_8 =  TileCodex.idDamageToIndex(255, 7)
    val WATER_9 =  TileCodex.idDamageToIndex(255, 8)
    val WATER_10 = TileCodex.idDamageToIndex(255, 9)
    val WATER_11 = TileCodex.idDamageToIndex(255, 10)
    val WATER_12 = TileCodex.idDamageToIndex(255, 11)
    val WATER_13 = TileCodex.idDamageToIndex(255, 12)
    val WATER_14 = TileCodex.idDamageToIndex(255, 13)
    val WATER_15 = TileCodex.idDamageToIndex(255, 14)
    val WATER =    TileCodex.idDamageToIndex(255, 15)

    val LAVA_1 =  TileCodex.idDamageToIndex(254, 0)
    val LAVA_2 =  TileCodex.idDamageToIndex(254, 1)
    val LAVA_3 =  TileCodex.idDamageToIndex(254, 2)
    val LAVA_4 =  TileCodex.idDamageToIndex(254, 3)
    val LAVA_5 =  TileCodex.idDamageToIndex(254, 4)
    val LAVA_6 =  TileCodex.idDamageToIndex(254, 5)
    val LAVA_7 =  TileCodex.idDamageToIndex(254, 6)
    val LAVA_8 =  TileCodex.idDamageToIndex(254, 7)
    val LAVA_9 =  TileCodex.idDamageToIndex(254, 8)
    val LAVA_10 = TileCodex.idDamageToIndex(254, 9)
    val LAVA_11 = TileCodex.idDamageToIndex(254, 10)
    val LAVA_12 = TileCodex.idDamageToIndex(254, 11)
    val LAVA_13 = TileCodex.idDamageToIndex(254, 12)
    val LAVA_14 = TileCodex.idDamageToIndex(254, 13)
    val LAVA_15 = TileCodex.idDamageToIndex(254, 14)
    val LAVA =    TileCodex.idDamageToIndex(254, 15)

    val NULL = -1
}
