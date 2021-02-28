package net.torvald.terrarum.blockproperties

/**
 * Created by minjaesong on 2016-02-21.
 */
object Block {

    const val AIR = "basegame:0" // hard coded; this is the standard

    const val STONE = "basegame:16"
    const val STONE_QUARRIED = "basegame:17"
    const val STONE_TILE_WHITE = "basegame:18"
    const val STONE_BRICKS = "basegame:19"
    const val STONE_SLATE = "basegame:20"
    const val STONE_MARBLE = "basegame:21"

    const val DIRT = "basegame:32"
    const val GRASS = "basegame:33"
    const val GRASSWALL = "basegame:34"

    const val PLANK_NORMAL = "basegame:48"
    const val PLANK_EBONY = "basegame:49"
    const val PLANK_BIRCH = "basegame:50"
    const val PLANK_BLOODROSE = "basegame:51"

    const val TRUNK_NORMAL = "basegame:64"
    const val TRUNK_EBONY = "basegame:65"
    const val TRUNK_BIRCH = "basegame:66"
    const val TRUNK_BLOODROSE = "basegame:67"

    const val SAND = "basegame:80"
    const val SAND_WHITE = "basegame:81"
    const val SAND_RED = "basegame:82"
    const val SAND_DESERT = "basegame:83"
    const val SAND_BLACK = "basegame:84"
    const val SAND_GREEN = "basegame:85"

    const val GRAVEL = "basegame:96"
    const val GRAVEL_GREY = "basegame:97"

    const val ORE_COPPER = "basegame:112"
    const val ORE_IRON = "basegame:113"
    const val ORE_GOLD = "basegame:114"
    const val ORE_SILVER = "basegame:115"
    const val ORE_ILMENITE = "basegame:116"
    const val ORE_AURICHALCUM = "basegame:117"

    const val RAW_RUBY = "basegame:128"
    const val RAW_EMERALD = "basegame:129"
    const val RAW_SAPPHIRE = "basegame:130"
    const val RAW_TOPAZ = "basegame:131"
    const val RAW_DIAMOND = "basegame:132"
    const val RAW_AMETHYST = "basegame:133"

    const val SNOW = "basegame:144"
    const val ICE_FRAGILE = "basegame:145"
    const val ICE_NATURAL = "basegame:146"
    const val ICE_MAGICAL = "basegame:147"

    const val GLASS_CRUDE = "basegame:148"
    const val GLASS_CLEAN = "basegame:149"

    const val PLATFORM_STONE = "basegame:160"
    const val PLATFORM_WOODEN = "basegame:161"
    const val PLATFORM_EBONY = "basegame:162"
    const val PLATFORM_BIRCH = "basegame:163"
    const val PLATFORM_BLOODROSE = "basegame:164"

    const val TORCH = "basegame:176"
    const val TORCH_FROST = "basegame:177"

    const val TORCH_OFF = "basegame:192"
    const val TORCH_FROST_OFF = "basegame:193"

    const val ILLUMINATOR_WHITE = "basegame:208"
    const val ILLUMINATOR_YELLOW = "basegame:209"
    const val ILLUMINATOR_ORANGE = "basegame:210"
    const val ILLUMINATOR_RED = "basegame:211"
    const val ILLUMINATOR_FUCHSIA = "basegame:212"
    const val ILLUMINATOR_PURPLE = "basegame:213"
    const val ILLUMINATOR_BLUE = "basegame:214"
    const val ILLUMINATOR_CYAN = "basegame:215"
    const val ILLUMINATOR_GREEN = "basegame:216"
    const val ILLUMINATOR_GREEN_DARK = "basegame:217"
    const val ILLUMINATOR_BROWN = "basegame:218"
    const val ILLUMINATOR_TAN = "basegame:219"
    const val ILLUMINATOR_GREY_LIGHT = "basegame:220"
    const val ILLUMINATOR_GREY_MED = "basegame:221"
    const val ILLUMINATOR_GREY_DARK = "basegame:222"
    const val ILLUMINATOR_BLACK = "basegame:223"

    const val ILLUMINATOR_WHITE_OFF = "basegame:224"
    const val ILLUMINATOR_YELLOW_OFF = "basegame:225"
    const val ILLUMINATOR_ORANGE_OFF = "basegame:226"
    const val ILLUMINATOR_RED_OFF = "basegame:227"
    const val ILLUMINATOR_FUCHSIA_OFF = "basegame:228"
    const val ILLUMINATOR_PURPLE_OFF = "basegame:229"
    const val ILLUMINATOR_BLUE_OFF = "basegame:230"
    const val ILLUMINATOR_CYAN_OFF = "basegame:231"
    const val ILLUMINATOR_GREEN_OFF = "basegame:232"
    const val ILLUMINATOR_GREEN_DARK_OFF = "basegame:233"
    const val ILLUMINATOR_BROWN_OFF = "basegame:234"
    const val ILLUMINATOR_TAN_OFF = "basegame:235"
    const val ILLUMINATOR_GREY_LIGHT_OFF = "basegame:236"
    const val ILLUMINATOR_GREY_MED_OFF = "basegame:237"
    const val ILLUMINATOR_GREY_DARK_OFF = "basegame:238"
    const val ILLUMINATOR_BLACK_OFF = "basegame:239"

    const val SANDSTONE = "basegame:240"
    const val SANDSTONE_WHITE = "basegame:241"
    const val SANDSTONE_RED = "basegame:242"
    const val SANDSTONE_DESERT = "basegame:243"
    const val SANDSTONE_BLACK = "basegame:244"
    const val SANDSTONE_GREEN = "basegame:245"

    const val LANTERN = "basegame:256"
    const val SUNSTONE = "basegame:257"
    const val DAYLIGHT_CAPACITOR = "basegame:258"


    const val ACTORBLOCK_NO_COLLISION = "basegame:4091"
    const val ACTORBLOCK_FULL_COLLISION = "basegame:4092"
    const val ACTORBLOCK_ALLOW_MOVE_DOWN = "basegame:4093"
    const val ACTORBLOCK_NO_PASS_RIGHT = "basegame:4094"
    const val ACTORBLOCK_NO_PASS_LEFT = "basegame:4095"


    const val LAVA = "basegame:4094"
    const val WATER = "basegame:4095"

    const val NULL = "basegame:-1"

    val actorblocks = listOf(
            ACTORBLOCK_NO_COLLISION,
            ACTORBLOCK_FULL_COLLISION,
            ACTORBLOCK_ALLOW_MOVE_DOWN,
            ACTORBLOCK_NO_PASS_RIGHT,
            ACTORBLOCK_NO_PASS_LEFT
    )
}
