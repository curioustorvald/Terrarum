package net.torvald.terrarum.blockproperties

/**
 * Created by minjaesong on 16-02-21.
 */
object Block {

    val AIR = 0 // hard coded; this is the standard

    val STONE = BlockCodex.idDamageToIndex(1, 0)
    val STONE_QUARRIED = BlockCodex.idDamageToIndex(1, 1)
    val STONE_TILE_WHITE = BlockCodex.idDamageToIndex(1, 2)
    val STONE_BRICKS = BlockCodex.idDamageToIndex(1, 3)

    val DIRT = BlockCodex.idDamageToIndex(2, 0)
    val GRASS = BlockCodex.idDamageToIndex(2, 1)

    val PLANK_NORMAL = BlockCodex.idDamageToIndex(3, 0)
    val PLANK_EBONY = BlockCodex.idDamageToIndex(3, 1)
    val PLANK_BIRCH = BlockCodex.idDamageToIndex(3, 2)
    val PLANK_BLOODROSE = BlockCodex.idDamageToIndex(3, 3)

    val TRUNK_NORMAL = BlockCodex.idDamageToIndex(4, 0)
    val TRUNK_EBONY = BlockCodex.idDamageToIndex(4, 1)
    val TRUNK_BIRCH = BlockCodex.idDamageToIndex(4, 2)
    val TRUNK_BLOODROSE = BlockCodex.idDamageToIndex(4, 3)

    val SAND = BlockCodex.idDamageToIndex(5, 0)
    val SAND_WHITE = BlockCodex.idDamageToIndex(5, 1)
    val SAND_RED = BlockCodex.idDamageToIndex(5, 2)
    val SAND_DESERT = BlockCodex.idDamageToIndex(5, 3)
    val SAND_BLACK = BlockCodex.idDamageToIndex(5, 4)
    val SAND_GREEN = BlockCodex.idDamageToIndex(5, 5)

    val GRAVEL = BlockCodex.idDamageToIndex(6, 0)
    val GRAVEL_GREY = BlockCodex.idDamageToIndex(6, 1)

    val ORE_COPPER = BlockCodex.idDamageToIndex(7, 0)
    val ORE_IRON = BlockCodex.idDamageToIndex(7, 1)
    val ORE_GOLD = BlockCodex.idDamageToIndex(7, 2)
    val ORE_SILVER = BlockCodex.idDamageToIndex(7, 3)
    val ORE_ILMENITE = BlockCodex.idDamageToIndex(7, 4)
    val ORE_AURICHALCUM = BlockCodex.idDamageToIndex(7, 5)

    val RAW_RUBY = BlockCodex.idDamageToIndex(8, 0)
    val RAW_EMERALD = BlockCodex.idDamageToIndex(8, 1)
    val RAW_SAPPHIRE = BlockCodex.idDamageToIndex(8, 2)
    val RAW_TOPAZ = BlockCodex.idDamageToIndex(8, 3)
    val RAW_DIAMOND = BlockCodex.idDamageToIndex(8, 4)
    val RAW_AMETHYST = BlockCodex.idDamageToIndex(8, 5)

    val SNOW = BlockCodex.idDamageToIndex(9, 0)
    val ICE_FRAGILE = BlockCodex.idDamageToIndex(9, 1)
    val ICE_NATURAL = BlockCodex.idDamageToIndex(9, 2)
    val ICE_MAGICAL = BlockCodex.idDamageToIndex(9, 3)

    val GLASS_CRUDE = BlockCodex.idDamageToIndex(9, 4)
    val GLASS_CLEAN = BlockCodex.idDamageToIndex(9, 5)

    val PLATFORM_STONE = BlockCodex.idDamageToIndex(10, 0)
    val PLATFORM_WOODEN = BlockCodex.idDamageToIndex(10, 1)
    val PLATFORM_EBONY = BlockCodex.idDamageToIndex(10, 2)
    val PLATFORM_BIRCH = BlockCodex.idDamageToIndex(10, 3)
    val PLATFORM_BLOODROSE = BlockCodex.idDamageToIndex(10, 4)

    val TORCH = BlockCodex.idDamageToIndex(11, 0)
    val TORCH_FROST = BlockCodex.idDamageToIndex(11, 1)

    val TORCH_OFF = BlockCodex.idDamageToIndex(12, 0)
    val TORCH_FROST_OFF = BlockCodex.idDamageToIndex(12, 1)

    val ILLUMINATOR_WHITE = BlockCodex.idDamageToIndex(13, 0)
    val ILLUMINATOR_YELLOW = BlockCodex.idDamageToIndex(13, 1)
    val ILLUMINATOR_ORANGE = BlockCodex.idDamageToIndex(13, 2)
    val ILLUMINATOR_RED = BlockCodex.idDamageToIndex(13, 3)
    val ILLUMINATOR_FUCHSIA = BlockCodex.idDamageToIndex(13, 4)
    val ILLUMINATOR_PURPLE = BlockCodex.idDamageToIndex(13, 5)
    val ILLUMINATOR_BLUE = BlockCodex.idDamageToIndex(13, 6)
    val ILLUMINATOR_CYAN = BlockCodex.idDamageToIndex(13, 7)
    val ILLUMINATOR_GREEN = BlockCodex.idDamageToIndex(13, 8)
    val ILLUMINATOR_GREEN_DARK = BlockCodex.idDamageToIndex(13, 9)
    val ILLUMINATOR_BROWN = BlockCodex.idDamageToIndex(13, 10)
    val ILLUMINATOR_TAN = BlockCodex.idDamageToIndex(13, 11)
    val ILLUMINATOR_GREY_LIGHT = BlockCodex.idDamageToIndex(13, 12)
    val ILLUMINATOR_GREY_MED = BlockCodex.idDamageToIndex(13, 13)
    val ILLUMINATOR_GREY_DARK = BlockCodex.idDamageToIndex(13, 14)
    val ILLUMINATOR_BLACK = BlockCodex.idDamageToIndex(13, 15)

    val ILLUMINATOR_WHITE_OFF = BlockCodex.idDamageToIndex(14, 0)
    val ILLUMINATOR_YELLOW_OFF = BlockCodex.idDamageToIndex(14, 1)
    val ILLUMINATOR_ORANGE_OFF = BlockCodex.idDamageToIndex(14, 2)
    val ILLUMINATOR_RED_OFF = BlockCodex.idDamageToIndex(14, 3)
    val ILLUMINATOR_FUCHSIA_OFF = BlockCodex.idDamageToIndex(14, 4)
    val ILLUMINATOR_PURPLE_OFF = BlockCodex.idDamageToIndex(14, 5)
    val ILLUMINATOR_BLUE_OFF = BlockCodex.idDamageToIndex(14, 6)
    val ILLUMINATOR_CYAN_OFF = BlockCodex.idDamageToIndex(14, 7)
    val ILLUMINATOR_GREEN_OFF = BlockCodex.idDamageToIndex(14, 8)
    val ILLUMINATOR_GREEN_DARK_OFF = BlockCodex.idDamageToIndex(14, 9)
    val ILLUMINATOR_BROWN_OFF = BlockCodex.idDamageToIndex(14, 10)
    val ILLUMINATOR_TAN_OFF = BlockCodex.idDamageToIndex(14, 11)
    val ILLUMINATOR_GREY_LIGHT_OFF = BlockCodex.idDamageToIndex(14, 12)
    val ILLUMINATOR_GREY_MED_OFF = BlockCodex.idDamageToIndex(14, 13)
    val ILLUMINATOR_GREY_DARK_OFF = BlockCodex.idDamageToIndex(14, 14)
    val ILLUMINATOR_BLACK_OFF = BlockCodex.idDamageToIndex(14, 15)

    val SANDSTONE = BlockCodex.idDamageToIndex(15, 0)
    val SANDSTONE_WHITE = BlockCodex.idDamageToIndex(15, 1)
    val SANDSTONE_RED = BlockCodex.idDamageToIndex(15, 2)
    val SANDSTONE_DESERT = BlockCodex.idDamageToIndex(15, 3)
    val SANDSTONE_BLACK = BlockCodex.idDamageToIndex(15, 4)
    val SANDSTONE_GREEN = BlockCodex.idDamageToIndex(15, 5)

    val LANTERN = BlockCodex.idDamageToIndex(16, 0)
    val SUNSTONE = BlockCodex.idDamageToIndex(16, 1)
    val DAYLIGHT_CAPACITOR = BlockCodex.idDamageToIndex(16, 2)

    val WATER_1 =  BlockCodex.idDamageToIndex(255, 0)
    val WATER_2 =  BlockCodex.idDamageToIndex(255, 1)
    val WATER_3 =  BlockCodex.idDamageToIndex(255, 2)
    val WATER_4 =  BlockCodex.idDamageToIndex(255, 3)
    val WATER_5 =  BlockCodex.idDamageToIndex(255, 4)
    val WATER_6 =  BlockCodex.idDamageToIndex(255, 5)
    val WATER_7 =  BlockCodex.idDamageToIndex(255, 6)
    val WATER_8 =  BlockCodex.idDamageToIndex(255, 7)
    val WATER_9 =  BlockCodex.idDamageToIndex(255, 8)
    val WATER_10 = BlockCodex.idDamageToIndex(255, 9)
    val WATER_11 = BlockCodex.idDamageToIndex(255, 10)
    val WATER_12 = BlockCodex.idDamageToIndex(255, 11)
    val WATER_13 = BlockCodex.idDamageToIndex(255, 12)
    val WATER_14 = BlockCodex.idDamageToIndex(255, 13)
    val WATER_15 = BlockCodex.idDamageToIndex(255, 14)
    val WATER =    BlockCodex.idDamageToIndex(255, 15)

    val LAVA_1 =  BlockCodex.idDamageToIndex(254, 0)
    val LAVA_2 =  BlockCodex.idDamageToIndex(254, 1)
    val LAVA_3 =  BlockCodex.idDamageToIndex(254, 2)
    val LAVA_4 =  BlockCodex.idDamageToIndex(254, 3)
    val LAVA_5 =  BlockCodex.idDamageToIndex(254, 4)
    val LAVA_6 =  BlockCodex.idDamageToIndex(254, 5)
    val LAVA_7 =  BlockCodex.idDamageToIndex(254, 6)
    val LAVA_8 =  BlockCodex.idDamageToIndex(254, 7)
    val LAVA_9 =  BlockCodex.idDamageToIndex(254, 8)
    val LAVA_10 = BlockCodex.idDamageToIndex(254, 9)
    val LAVA_11 = BlockCodex.idDamageToIndex(254, 10)
    val LAVA_12 = BlockCodex.idDamageToIndex(254, 11)
    val LAVA_13 = BlockCodex.idDamageToIndex(254, 12)
    val LAVA_14 = BlockCodex.idDamageToIndex(254, 13)
    val LAVA_15 = BlockCodex.idDamageToIndex(254, 14)
    val LAVA =    BlockCodex.idDamageToIndex(254, 15)

    val NULL = -1
}
