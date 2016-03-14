package com.Torvald.Terrarum.TileProperties;

import com.Torvald.Terrarum.Terrarum;

/**
 * Created by minjaesong on 16-02-21.
 */
public class TileNameCode {

    public static final int AIR = 0;

    public static final int STONE =          TilePropCodex.indexDamageToArrayAddr(1, 0);
    public static final int STONE_QUARRIED = TilePropCodex.indexDamageToArrayAddr(1, 1);

    public static final int DIRT =  TilePropCodex.indexDamageToArrayAddr(2, 0);
    public static final int GRASS = TilePropCodex.indexDamageToArrayAddr(2, 1);

    public static final int PLANK_NORMAL =    TilePropCodex.indexDamageToArrayAddr(3, 0);
    public static final int PLANK_EBONY =     TilePropCodex.indexDamageToArrayAddr(3, 1);
    public static final int PLANK_BIRCH =     TilePropCodex.indexDamageToArrayAddr(3, 2);
    public static final int PLANK_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(3, 3);

    public static final int TRUNK_NORMAL =    TilePropCodex.indexDamageToArrayAddr(4, 0);
    public static final int TRUNK_EBONY =     TilePropCodex.indexDamageToArrayAddr(4, 1);
    public static final int TRUNK_BIRCH =     TilePropCodex.indexDamageToArrayAddr(4, 2);
    public static final int TRUNK_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(4, 3);

    public static final int SAND =        TilePropCodex.indexDamageToArrayAddr(5, 0);
    public static final int SAND_BEACH =  TilePropCodex.indexDamageToArrayAddr(5, 1);
    public static final int SAND_RED =    TilePropCodex.indexDamageToArrayAddr(5, 2);
    public static final int SAND_DESERT = TilePropCodex.indexDamageToArrayAddr(5, 3);
    public static final int SAND_BLACK =  TilePropCodex.indexDamageToArrayAddr(5, 4);
    public static final int SAND_GREEN =  TilePropCodex.indexDamageToArrayAddr(5, 5);

    public static final int GRAVEL =          TilePropCodex.indexDamageToArrayAddr(6, 0);
    public static final int GRAVEL_GREY =     TilePropCodex.indexDamageToArrayAddr(6, 1);

    public static final int ORE_COPPER =      TilePropCodex.indexDamageToArrayAddr(7, 0);
    public static final int ORE_IRON =        TilePropCodex.indexDamageToArrayAddr(7, 1);
    public static final int ORE_GOLD =        TilePropCodex.indexDamageToArrayAddr(7, 2);
    public static final int ORE_SILVER =      TilePropCodex.indexDamageToArrayAddr(7, 3);
    public static final int ORE_ILMENITE =    TilePropCodex.indexDamageToArrayAddr(7, 4);
    public static final int ORE_AURICHALCUM = TilePropCodex.indexDamageToArrayAddr(7, 5);

    public static final int RAW_RUBY =     TilePropCodex.indexDamageToArrayAddr(8, 0);
    public static final int RAW_EMERALD =  TilePropCodex.indexDamageToArrayAddr(8, 1);
    public static final int RAW_SAPPHIRE = TilePropCodex.indexDamageToArrayAddr(8, 2);
    public static final int RAW_TOPAZ =    TilePropCodex.indexDamageToArrayAddr(8, 3);
    public static final int RAW_DIAMOND =  TilePropCodex.indexDamageToArrayAddr(8, 4);
    public static final int RAW_AMETHYST = TilePropCodex.indexDamageToArrayAddr(8, 5);

    public static final int SNOW =        TilePropCodex.indexDamageToArrayAddr(9, 0);
    public static final int ICE_FRAGILE = TilePropCodex.indexDamageToArrayAddr(9, 1);
    public static final int ICE_NATURAL = TilePropCodex.indexDamageToArrayAddr(9, 2);
    public static final int ICE_MAGICAL = TilePropCodex.indexDamageToArrayAddr(9, 3);

    public static final int PLATFORM_STONE =     TilePropCodex.indexDamageToArrayAddr(10, 0);
    public static final int PLATFORM_WOODEN =    TilePropCodex.indexDamageToArrayAddr(10, 1);
    public static final int PLATFORM_EBONY =     TilePropCodex.indexDamageToArrayAddr(10, 2);
    public static final int PLATFORM_BIRCH =     TilePropCodex.indexDamageToArrayAddr(10, 3);
    public static final int PLATFORM_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(10, 4);

    public static final int TORCH =        TilePropCodex.indexDamageToArrayAddr(11, 0);
    public static final int TORCH_FROST =  TilePropCodex.indexDamageToArrayAddr(11, 1);

    public static final int TORCH_OFF =        TilePropCodex.indexDamageToArrayAddr(12, 0);
    public static final int TORCH_FROST_OFF =  TilePropCodex.indexDamageToArrayAddr(12, 1);

    public static final int ILLUMINATOR_WHITE =      TilePropCodex.indexDamageToArrayAddr(13, 0);
    public static final int ILLUMINATOR_YELLOW =     TilePropCodex.indexDamageToArrayAddr(13, 1);
    public static final int ILLUMINATOR_ORANGE =     TilePropCodex.indexDamageToArrayAddr(13, 2);
    public static final int ILLUMINATOR_RED =        TilePropCodex.indexDamageToArrayAddr(13, 3);
    public static final int ILLUMINATOR_FUCHSIA =    TilePropCodex.indexDamageToArrayAddr(13, 4);
    public static final int ILLUMINATOR_PURPLE =     TilePropCodex.indexDamageToArrayAddr(13, 5);
    public static final int ILLUMINATOR_BLUE =       TilePropCodex.indexDamageToArrayAddr(13, 6);
    public static final int ILLUMINATOR_CYAN =       TilePropCodex.indexDamageToArrayAddr(13, 7);
    public static final int ILLUMINATOR_GREEN =      TilePropCodex.indexDamageToArrayAddr(13, 8);
    public static final int ILLUMINATOR_GREEN_DARK = TilePropCodex.indexDamageToArrayAddr(13, 9);
    public static final int ILLUMINATOR_BROWN =      TilePropCodex.indexDamageToArrayAddr(13, 10);
    public static final int ILLUMINATOR_TAN =        TilePropCodex.indexDamageToArrayAddr(13, 11);
    public static final int ILLUMINATOR_GREY_LIGHT = TilePropCodex.indexDamageToArrayAddr(13, 12);
    public static final int ILLUMINATOR_GREY_MED =   TilePropCodex.indexDamageToArrayAddr(13, 13);
    public static final int ILLUMINATOR_GREY_DARK =  TilePropCodex.indexDamageToArrayAddr(13, 14);
    public static final int ILLUMINATOR_BLACK =      TilePropCodex.indexDamageToArrayAddr(13, 15);

    public static final int ILLUMINATOR_WHITE_OFF =      TilePropCodex.indexDamageToArrayAddr(14, 0);
    public static final int ILLUMINATOR_YELLOW_OFF =     TilePropCodex.indexDamageToArrayAddr(14, 1);
    public static final int ILLUMINATOR_ORANGE_OFF =     TilePropCodex.indexDamageToArrayAddr(14, 2);
    public static final int ILLUMINATOR_RED_OFF =        TilePropCodex.indexDamageToArrayAddr(14, 3);
    public static final int ILLUMINATOR_FUCHSIA_OFF =    TilePropCodex.indexDamageToArrayAddr(14, 4);
    public static final int ILLUMINATOR_PURPLE_OFF =     TilePropCodex.indexDamageToArrayAddr(14, 5);
    public static final int ILLUMINATOR_BLUE_OFF =       TilePropCodex.indexDamageToArrayAddr(14, 6);
    public static final int ILLUMINATOR_CYAN_OFF =       TilePropCodex.indexDamageToArrayAddr(14, 7);
    public static final int ILLUMINATOR_GREEN_OFF =      TilePropCodex.indexDamageToArrayAddr(14, 8);
    public static final int ILLUMINATOR_GREEN_DARK_OFF = TilePropCodex.indexDamageToArrayAddr(14, 9);
    public static final int ILLUMINATOR_BROWN_OFF =      TilePropCodex.indexDamageToArrayAddr(14, 10);
    public static final int ILLUMINATOR_TAN_OFF =        TilePropCodex.indexDamageToArrayAddr(14, 11);
    public static final int ILLUMINATOR_GREY_LIGHT_OFF = TilePropCodex.indexDamageToArrayAddr(14, 12);
    public static final int ILLUMINATOR_GREY_MED_OFF =   TilePropCodex.indexDamageToArrayAddr(14, 13);
    public static final int ILLUMINATOR_GREY_DARK_OFF =  TilePropCodex.indexDamageToArrayAddr(14, 14);
    public static final int ILLUMINATOR_BLACK_OFF =      TilePropCodex.indexDamageToArrayAddr(14, 15);

    public static final int SANDSTONE        = TilePropCodex.indexDamageToArrayAddr(15, 0);
    public static final int SANDSTONE_WHITE  = TilePropCodex.indexDamageToArrayAddr(15, 1);
    public static final int SANDSTONE_RED    = TilePropCodex.indexDamageToArrayAddr(15, 2);
    public static final int SANDSTONE_DESERT = TilePropCodex.indexDamageToArrayAddr(15, 3);
    public static final int SANDSTONE_BLACK  = TilePropCodex.indexDamageToArrayAddr(15, 4);
    public static final int SANDSTONE_GREEN  = TilePropCodex.indexDamageToArrayAddr(15, 5);

    public static final int WATER_1 =  TilePropCodex.indexDamageToArrayAddr(254, 0);
    public static final int WATER_2 =  TilePropCodex.indexDamageToArrayAddr(254, 1);
    public static final int WATER_3 =  TilePropCodex.indexDamageToArrayAddr(254, 2);
    public static final int WATER_4 =  TilePropCodex.indexDamageToArrayAddr(254, 3);
    public static final int WATER_5 =  TilePropCodex.indexDamageToArrayAddr(254, 4);
    public static final int WATER_6 =  TilePropCodex.indexDamageToArrayAddr(254, 5);
    public static final int WATER_7 =  TilePropCodex.indexDamageToArrayAddr(254, 6);
    public static final int WATER_8 =  TilePropCodex.indexDamageToArrayAddr(254, 7);
    public static final int WATER_9 =  TilePropCodex.indexDamageToArrayAddr(254, 8);
    public static final int WATER_10 = TilePropCodex.indexDamageToArrayAddr(254, 9);
    public static final int WATER_11 = TilePropCodex.indexDamageToArrayAddr(254, 10);
    public static final int WATER_12 = TilePropCodex.indexDamageToArrayAddr(254, 11);
    public static final int WATER_13 = TilePropCodex.indexDamageToArrayAddr(254, 12);
    public static final int WATER_14 = TilePropCodex.indexDamageToArrayAddr(254, 13);
    public static final int WATER_15 = TilePropCodex.indexDamageToArrayAddr(254, 14);
    public static final int WATER =    TilePropCodex.indexDamageToArrayAddr(254, 15);

    public static final int LAVA_1 =  TilePropCodex.indexDamageToArrayAddr(255, 0);
    public static final int LAVA_2 =  TilePropCodex.indexDamageToArrayAddr(255, 1);
    public static final int LAVA_3 =  TilePropCodex.indexDamageToArrayAddr(255, 2);
    public static final int LAVA_4 =  TilePropCodex.indexDamageToArrayAddr(255, 3);
    public static final int LAVA_5 =  TilePropCodex.indexDamageToArrayAddr(255, 4);
    public static final int LAVA_6 =  TilePropCodex.indexDamageToArrayAddr(255, 5);
    public static final int LAVA_7 =  TilePropCodex.indexDamageToArrayAddr(255, 6);
    public static final int LAVA_8 =  TilePropCodex.indexDamageToArrayAddr(255, 7);
    public static final int LAVA_9 =  TilePropCodex.indexDamageToArrayAddr(255, 8);
    public static final int LAVA_10 = TilePropCodex.indexDamageToArrayAddr(255, 9);
    public static final int LAVA_11 = TilePropCodex.indexDamageToArrayAddr(255, 10);
    public static final int LAVA_12 = TilePropCodex.indexDamageToArrayAddr(255, 11);
    public static final int LAVA_13 = TilePropCodex.indexDamageToArrayAddr(255, 12);
    public static final int LAVA_14 = TilePropCodex.indexDamageToArrayAddr(255, 13);
    public static final int LAVA_15 = TilePropCodex.indexDamageToArrayAddr(255, 14);
    public static final int LAVA =    TilePropCodex.indexDamageToArrayAddr(255, 15);

}
