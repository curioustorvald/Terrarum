package com.Torvald.Terrarum.TileProperties

import com.Torvald.Terrarum.Terrarum

/**
 * Created by minjaesong on 16-02-21.
 */
object TileNameCode {

    val AIR = 0

    val STONE = TilePropCodex.indexDamageToArrayAddr(1, 0)
    val STONE_QUARRIED = TilePropCodex.indexDamageToArrayAddr(1, 1)

    val DIRT = TilePropCodex.indexDamageToArrayAddr(2, 0)
    val GRASS = TilePropCodex.indexDamageToArrayAddr(2, 1)

    val PLANK_NORMAL = TilePropCodex.indexDamageToArrayAddr(3, 0)
    val PLANK_EBONY = TilePropCodex.indexDamageToArrayAddr(3, 1)
    val PLANK_BIRCH = TilePropCodex.indexDamageToArrayAddr(3, 2)
    val PLANK_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(3, 3)

    val TRUNK_NORMAL = TilePropCodex.indexDamageToArrayAddr(4, 0)
    val TRUNK_EBONY = TilePropCodex.indexDamageToArrayAddr(4, 1)
    val TRUNK_BIRCH = TilePropCodex.indexDamageToArrayAddr(4, 2)
    val TRUNK_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(4, 3)

    val SAND = TilePropCodex.indexDamageToArrayAddr(5, 0)
    val SAND_BEACH = TilePropCodex.indexDamageToArrayAddr(5, 1)
    val SAND_RED = TilePropCodex.indexDamageToArrayAddr(5, 2)
    val SAND_DESERT = TilePropCodex.indexDamageToArrayAddr(5, 3)
    val SAND_BLACK = TilePropCodex.indexDamageToArrayAddr(5, 4)
    val SAND_GREEN = TilePropCodex.indexDamageToArrayAddr(5, 5)

    val GRAVEL = TilePropCodex.indexDamageToArrayAddr(6, 0)
    val GRAVEL_GREY = TilePropCodex.indexDamageToArrayAddr(6, 1)

    val ORE_COPPER = TilePropCodex.indexDamageToArrayAddr(7, 0)
    val ORE_IRON = TilePropCodex.indexDamageToArrayAddr(7, 1)
    val ORE_GOLD = TilePropCodex.indexDamageToArrayAddr(7, 2)
    val ORE_SILVER = TilePropCodex.indexDamageToArrayAddr(7, 3)
    val ORE_ILMENITE = TilePropCodex.indexDamageToArrayAddr(7, 4)
    val ORE_AURICHALCUM = TilePropCodex.indexDamageToArrayAddr(7, 5)

    val RAW_RUBY = TilePropCodex.indexDamageToArrayAddr(8, 0)
    val RAW_EMERALD = TilePropCodex.indexDamageToArrayAddr(8, 1)
    val RAW_SAPPHIRE = TilePropCodex.indexDamageToArrayAddr(8, 2)
    val RAW_TOPAZ = TilePropCodex.indexDamageToArrayAddr(8, 3)
    val RAW_DIAMOND = TilePropCodex.indexDamageToArrayAddr(8, 4)
    val RAW_AMETHYST = TilePropCodex.indexDamageToArrayAddr(8, 5)

    val SNOW = TilePropCodex.indexDamageToArrayAddr(9, 0)
    val ICE_FRAGILE = TilePropCodex.indexDamageToArrayAddr(9, 1)
    val ICE_NATURAL = TilePropCodex.indexDamageToArrayAddr(9, 2)
    val ICE_MAGICAL = TilePropCodex.indexDamageToArrayAddr(9, 3)

    val PLATFORM_STONE = TilePropCodex.indexDamageToArrayAddr(10, 0)
    val PLATFORM_WOODEN = TilePropCodex.indexDamageToArrayAddr(10, 1)
    val PLATFORM_EBONY = TilePropCodex.indexDamageToArrayAddr(10, 2)
    val PLATFORM_BIRCH = TilePropCodex.indexDamageToArrayAddr(10, 3)
    val PLATFORM_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(10, 4)

    val TORCH = TilePropCodex.indexDamageToArrayAddr(11, 0)
    val TORCH_FROST = TilePropCodex.indexDamageToArrayAddr(11, 1)

    val TORCH_OFF = TilePropCodex.indexDamageToArrayAddr(12, 0)
    val TORCH_FROST_OFF = TilePropCodex.indexDamageToArrayAddr(12, 1)

    val ILLUMINATOR_WHITE = TilePropCodex.indexDamageToArrayAddr(13, 0)
    val ILLUMINATOR_YELLOW = TilePropCodex.indexDamageToArrayAddr(13, 1)
    val ILLUMINATOR_ORANGE = TilePropCodex.indexDamageToArrayAddr(13, 2)
    val ILLUMINATOR_RED = TilePropCodex.indexDamageToArrayAddr(13, 3)
    val ILLUMINATOR_FUCHSIA = TilePropCodex.indexDamageToArrayAddr(13, 4)
    val ILLUMINATOR_PURPLE = TilePropCodex.indexDamageToArrayAddr(13, 5)
    val ILLUMINATOR_BLUE = TilePropCodex.indexDamageToArrayAddr(13, 6)
    val ILLUMINATOR_CYAN = TilePropCodex.indexDamageToArrayAddr(13, 7)
    val ILLUMINATOR_GREEN = TilePropCodex.indexDamageToArrayAddr(13, 8)
    val ILLUMINATOR_GREEN_DARK = TilePropCodex.indexDamageToArrayAddr(13, 9)
    val ILLUMINATOR_BROWN = TilePropCodex.indexDamageToArrayAddr(13, 10)
    val ILLUMINATOR_TAN = TilePropCodex.indexDamageToArrayAddr(13, 11)
    val ILLUMINATOR_GREY_LIGHT = TilePropCodex.indexDamageToArrayAddr(13, 12)
    val ILLUMINATOR_GREY_MED = TilePropCodex.indexDamageToArrayAddr(13, 13)
    val ILLUMINATOR_GREY_DARK = TilePropCodex.indexDamageToArrayAddr(13, 14)
    val ILLUMINATOR_BLACK = TilePropCodex.indexDamageToArrayAddr(13, 15)

    val ILLUMINATOR_WHITE_OFF = TilePropCodex.indexDamageToArrayAddr(14, 0)
    val ILLUMINATOR_YELLOW_OFF = TilePropCodex.indexDamageToArrayAddr(14, 1)
    val ILLUMINATOR_ORANGE_OFF = TilePropCodex.indexDamageToArrayAddr(14, 2)
    val ILLUMINATOR_RED_OFF = TilePropCodex.indexDamageToArrayAddr(14, 3)
    val ILLUMINATOR_FUCHSIA_OFF = TilePropCodex.indexDamageToArrayAddr(14, 4)
    val ILLUMINATOR_PURPLE_OFF = TilePropCodex.indexDamageToArrayAddr(14, 5)
    val ILLUMINATOR_BLUE_OFF = TilePropCodex.indexDamageToArrayAddr(14, 6)
    val ILLUMINATOR_CYAN_OFF = TilePropCodex.indexDamageToArrayAddr(14, 7)
    val ILLUMINATOR_GREEN_OFF = TilePropCodex.indexDamageToArrayAddr(14, 8)
    val ILLUMINATOR_GREEN_DARK_OFF = TilePropCodex.indexDamageToArrayAddr(14, 9)
    val ILLUMINATOR_BROWN_OFF = TilePropCodex.indexDamageToArrayAddr(14, 10)
    val ILLUMINATOR_TAN_OFF = TilePropCodex.indexDamageToArrayAddr(14, 11)
    val ILLUMINATOR_GREY_LIGHT_OFF = TilePropCodex.indexDamageToArrayAddr(14, 12)
    val ILLUMINATOR_GREY_MED_OFF = TilePropCodex.indexDamageToArrayAddr(14, 13)
    val ILLUMINATOR_GREY_DARK_OFF = TilePropCodex.indexDamageToArrayAddr(14, 14)
    val ILLUMINATOR_BLACK_OFF = TilePropCodex.indexDamageToArrayAddr(14, 15)

    val SANDSTONE = TilePropCodex.indexDamageToArrayAddr(15, 0)
    val SANDSTONE_WHITE = TilePropCodex.indexDamageToArrayAddr(15, 1)
    val SANDSTONE_RED = TilePropCodex.indexDamageToArrayAddr(15, 2)
    val SANDSTONE_DESERT = TilePropCodex.indexDamageToArrayAddr(15, 3)
    val SANDSTONE_BLACK = TilePropCodex.indexDamageToArrayAddr(15, 4)
    val SANDSTONE_GREEN = TilePropCodex.indexDamageToArrayAddr(15, 5)

    val WATER_1 = TilePropCodex.indexDamageToArrayAddr(254, 0)
    val WATER_2 = TilePropCodex.indexDamageToArrayAddr(254, 1)
    val WATER_3 = TilePropCodex.indexDamageToArrayAddr(254, 2)
    val WATER_4 = TilePropCodex.indexDamageToArrayAddr(254, 3)
    val WATER_5 = TilePropCodex.indexDamageToArrayAddr(254, 4)
    val WATER_6 = TilePropCodex.indexDamageToArrayAddr(254, 5)
    val WATER_7 = TilePropCodex.indexDamageToArrayAddr(254, 6)
    val WATER_8 = TilePropCodex.indexDamageToArrayAddr(254, 7)
    val WATER_9 = TilePropCodex.indexDamageToArrayAddr(254, 8)
    val WATER_10 = TilePropCodex.indexDamageToArrayAddr(254, 9)
    val WATER_11 = TilePropCodex.indexDamageToArrayAddr(254, 10)
    val WATER_12 = TilePropCodex.indexDamageToArrayAddr(254, 11)
    val WATER_13 = TilePropCodex.indexDamageToArrayAddr(254, 12)
    val WATER_14 = TilePropCodex.indexDamageToArrayAddr(254, 13)
    val WATER_15 = TilePropCodex.indexDamageToArrayAddr(254, 14)
    val WATER = TilePropCodex.indexDamageToArrayAddr(254, 15)

    val LAVA_1 = TilePropCodex.indexDamageToArrayAddr(255, 0)
    val LAVA_2 = TilePropCodex.indexDamageToArrayAddr(255, 1)
    val LAVA_3 = TilePropCodex.indexDamageToArrayAddr(255, 2)
    val LAVA_4 = TilePropCodex.indexDamageToArrayAddr(255, 3)
    val LAVA_5 = TilePropCodex.indexDamageToArrayAddr(255, 4)
    val LAVA_6 = TilePropCodex.indexDamageToArrayAddr(255, 5)
    val LAVA_7 = TilePropCodex.indexDamageToArrayAddr(255, 6)
    val LAVA_8 = TilePropCodex.indexDamageToArrayAddr(255, 7)
    val LAVA_9 = TilePropCodex.indexDamageToArrayAddr(255, 8)
    val LAVA_10 = TilePropCodex.indexDamageToArrayAddr(255, 9)
    val LAVA_11 = TilePropCodex.indexDamageToArrayAddr(255, 10)
    val LAVA_12 = TilePropCodex.indexDamageToArrayAddr(255, 11)
    val LAVA_13 = TilePropCodex.indexDamageToArrayAddr(255, 12)
    val LAVA_14 = TilePropCodex.indexDamageToArrayAddr(255, 13)
    val LAVA_15 = TilePropCodex.indexDamageToArrayAddr(255, 14)
    val LAVA = TilePropCodex.indexDamageToArrayAddr(255, 15)

}
