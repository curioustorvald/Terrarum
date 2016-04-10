package com.torvald.terrarum.tileproperties

import com.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-02-21.
 */
object TileNameCode {

    val AIR = 0

    val STONE = TilePropCodex.idDamageToIndex(1, 0)
    val STONE_QUARRIED = TilePropCodex.idDamageToIndex(1, 1)

    val DIRT = TilePropCodex.idDamageToIndex(2, 0)
    val GRASS = TilePropCodex.idDamageToIndex(2, 1)

    val PLANK_NORMAL = TilePropCodex.idDamageToIndex(3, 0)
    val PLANK_EBONY = TilePropCodex.idDamageToIndex(3, 1)
    val PLANK_BIRCH = TilePropCodex.idDamageToIndex(3, 2)
    val PLANK_BLOODROSE = TilePropCodex.idDamageToIndex(3, 3)

    val TRUNK_NORMAL = TilePropCodex.idDamageToIndex(4, 0)
    val TRUNK_EBONY = TilePropCodex.idDamageToIndex(4, 1)
    val TRUNK_BIRCH = TilePropCodex.idDamageToIndex(4, 2)
    val TRUNK_BLOODROSE = TilePropCodex.idDamageToIndex(4, 3)

    val SAND = TilePropCodex.idDamageToIndex(5, 0)
    val SAND_WHITE = TilePropCodex.idDamageToIndex(5, 1)
    val SAND_RED = TilePropCodex.idDamageToIndex(5, 2)
    val SAND_DESERT = TilePropCodex.idDamageToIndex(5, 3)
    val SAND_BLACK = TilePropCodex.idDamageToIndex(5, 4)
    val SAND_GREEN = TilePropCodex.idDamageToIndex(5, 5)

    val GRAVEL = TilePropCodex.idDamageToIndex(6, 0)
    val GRAVEL_GREY = TilePropCodex.idDamageToIndex(6, 1)

    val ORE_COPPER = TilePropCodex.idDamageToIndex(7, 0)
    val ORE_IRON = TilePropCodex.idDamageToIndex(7, 1)
    val ORE_GOLD = TilePropCodex.idDamageToIndex(7, 2)
    val ORE_SILVER = TilePropCodex.idDamageToIndex(7, 3)
    val ORE_ILMENITE = TilePropCodex.idDamageToIndex(7, 4)
    val ORE_AURICHALCUM = TilePropCodex.idDamageToIndex(7, 5)

    val RAW_RUBY = TilePropCodex.idDamageToIndex(8, 0)
    val RAW_EMERALD = TilePropCodex.idDamageToIndex(8, 1)
    val RAW_SAPPHIRE = TilePropCodex.idDamageToIndex(8, 2)
    val RAW_TOPAZ = TilePropCodex.idDamageToIndex(8, 3)
    val RAW_DIAMOND = TilePropCodex.idDamageToIndex(8, 4)
    val RAW_AMETHYST = TilePropCodex.idDamageToIndex(8, 5)

    val SNOW = TilePropCodex.idDamageToIndex(9, 0)
    val ICE_FRAGILE = TilePropCodex.idDamageToIndex(9, 1)
    val ICE_NATURAL = TilePropCodex.idDamageToIndex(9, 2)
    val ICE_MAGICAL = TilePropCodex.idDamageToIndex(9, 3)

    val PLATFORM_STONE = TilePropCodex.idDamageToIndex(10, 0)
    val PLATFORM_WOODEN = TilePropCodex.idDamageToIndex(10, 1)
    val PLATFORM_EBONY = TilePropCodex.idDamageToIndex(10, 2)
    val PLATFORM_BIRCH = TilePropCodex.idDamageToIndex(10, 3)
    val PLATFORM_BLOODROSE = TilePropCodex.idDamageToIndex(10, 4)

    val TORCH = TilePropCodex.idDamageToIndex(11, 0)
    val TORCH_FROST = TilePropCodex.idDamageToIndex(11, 1)

    val TORCH_OFF = TilePropCodex.idDamageToIndex(12, 0)
    val TORCH_FROST_OFF = TilePropCodex.idDamageToIndex(12, 1)

    val ILLUMINATOR_WHITE = TilePropCodex.idDamageToIndex(13, 0)
    val ILLUMINATOR_YELLOW = TilePropCodex.idDamageToIndex(13, 1)
    val ILLUMINATOR_ORANGE = TilePropCodex.idDamageToIndex(13, 2)
    val ILLUMINATOR_RED = TilePropCodex.idDamageToIndex(13, 3)
    val ILLUMINATOR_FUCHSIA = TilePropCodex.idDamageToIndex(13, 4)
    val ILLUMINATOR_PURPLE = TilePropCodex.idDamageToIndex(13, 5)
    val ILLUMINATOR_BLUE = TilePropCodex.idDamageToIndex(13, 6)
    val ILLUMINATOR_CYAN = TilePropCodex.idDamageToIndex(13, 7)
    val ILLUMINATOR_GREEN = TilePropCodex.idDamageToIndex(13, 8)
    val ILLUMINATOR_GREEN_DARK = TilePropCodex.idDamageToIndex(13, 9)
    val ILLUMINATOR_BROWN = TilePropCodex.idDamageToIndex(13, 10)
    val ILLUMINATOR_TAN = TilePropCodex.idDamageToIndex(13, 11)
    val ILLUMINATOR_GREY_LIGHT = TilePropCodex.idDamageToIndex(13, 12)
    val ILLUMINATOR_GREY_MED = TilePropCodex.idDamageToIndex(13, 13)
    val ILLUMINATOR_GREY_DARK = TilePropCodex.idDamageToIndex(13, 14)
    val ILLUMINATOR_BLACK = TilePropCodex.idDamageToIndex(13, 15)

    val ILLUMINATOR_WHITE_OFF = TilePropCodex.idDamageToIndex(14, 0)
    val ILLUMINATOR_YELLOW_OFF = TilePropCodex.idDamageToIndex(14, 1)
    val ILLUMINATOR_ORANGE_OFF = TilePropCodex.idDamageToIndex(14, 2)
    val ILLUMINATOR_RED_OFF = TilePropCodex.idDamageToIndex(14, 3)
    val ILLUMINATOR_FUCHSIA_OFF = TilePropCodex.idDamageToIndex(14, 4)
    val ILLUMINATOR_PURPLE_OFF = TilePropCodex.idDamageToIndex(14, 5)
    val ILLUMINATOR_BLUE_OFF = TilePropCodex.idDamageToIndex(14, 6)
    val ILLUMINATOR_CYAN_OFF = TilePropCodex.idDamageToIndex(14, 7)
    val ILLUMINATOR_GREEN_OFF = TilePropCodex.idDamageToIndex(14, 8)
    val ILLUMINATOR_GREEN_DARK_OFF = TilePropCodex.idDamageToIndex(14, 9)
    val ILLUMINATOR_BROWN_OFF = TilePropCodex.idDamageToIndex(14, 10)
    val ILLUMINATOR_TAN_OFF = TilePropCodex.idDamageToIndex(14, 11)
    val ILLUMINATOR_GREY_LIGHT_OFF = TilePropCodex.idDamageToIndex(14, 12)
    val ILLUMINATOR_GREY_MED_OFF = TilePropCodex.idDamageToIndex(14, 13)
    val ILLUMINATOR_GREY_DARK_OFF = TilePropCodex.idDamageToIndex(14, 14)
    val ILLUMINATOR_BLACK_OFF = TilePropCodex.idDamageToIndex(14, 15)

    val SANDSTONE = TilePropCodex.idDamageToIndex(15, 0)
    val SANDSTONE_WHITE = TilePropCodex.idDamageToIndex(15, 1)
    val SANDSTONE_RED = TilePropCodex.idDamageToIndex(15, 2)
    val SANDSTONE_DESERT = TilePropCodex.idDamageToIndex(15, 3)
    val SANDSTONE_BLACK = TilePropCodex.idDamageToIndex(15, 4)
    val SANDSTONE_GREEN = TilePropCodex.idDamageToIndex(15, 5)

    val WATER_1 =  TilePropCodex.idDamageToIndex(255, 0)
    val WATER_2 =  TilePropCodex.idDamageToIndex(255, 1)
    val WATER_3 =  TilePropCodex.idDamageToIndex(255, 2)
    val WATER_4 =  TilePropCodex.idDamageToIndex(255, 3)
    val WATER_5 =  TilePropCodex.idDamageToIndex(255, 4)
    val WATER_6 =  TilePropCodex.idDamageToIndex(255, 5)
    val WATER_7 =  TilePropCodex.idDamageToIndex(255, 6)
    val WATER_8 =  TilePropCodex.idDamageToIndex(255, 7)
    val WATER_9 =  TilePropCodex.idDamageToIndex(255, 8)
    val WATER_10 = TilePropCodex.idDamageToIndex(255, 9)
    val WATER_11 = TilePropCodex.idDamageToIndex(255, 10)
    val WATER_12 = TilePropCodex.idDamageToIndex(255, 11)
    val WATER_13 = TilePropCodex.idDamageToIndex(255, 12)
    val WATER_14 = TilePropCodex.idDamageToIndex(255, 13)
    val WATER_15 = TilePropCodex.idDamageToIndex(255, 14)
    val WATER =    TilePropCodex.idDamageToIndex(255, 15)

    val LAVA_1 =  TilePropCodex.idDamageToIndex(254, 0)
    val LAVA_2 =  TilePropCodex.idDamageToIndex(254, 1)
    val LAVA_3 =  TilePropCodex.idDamageToIndex(254, 2)
    val LAVA_4 =  TilePropCodex.idDamageToIndex(254, 3)
    val LAVA_5 =  TilePropCodex.idDamageToIndex(254, 4)
    val LAVA_6 =  TilePropCodex.idDamageToIndex(254, 5)
    val LAVA_7 =  TilePropCodex.idDamageToIndex(254, 6)
    val LAVA_8 =  TilePropCodex.idDamageToIndex(254, 7)
    val LAVA_9 =  TilePropCodex.idDamageToIndex(254, 8)
    val LAVA_10 = TilePropCodex.idDamageToIndex(254, 9)
    val LAVA_11 = TilePropCodex.idDamageToIndex(254, 10)
    val LAVA_12 = TilePropCodex.idDamageToIndex(254, 11)
    val LAVA_13 = TilePropCodex.idDamageToIndex(254, 12)
    val LAVA_14 = TilePropCodex.idDamageToIndex(254, 13)
    val LAVA_15 = TilePropCodex.idDamageToIndex(254, 14)
    val LAVA =    TilePropCodex.idDamageToIndex(254, 15)

    val NULL = 4096
}
