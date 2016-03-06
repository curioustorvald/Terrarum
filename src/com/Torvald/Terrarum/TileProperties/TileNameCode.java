package com.Torvald.Terrarum.TileProperties;

import com.Torvald.Terrarum.Terrarum;

/**
 * Created by minjaesong on 16-02-21.
 */
public class TileNameCode {

    public static final int AIR = 0;

    public static final int STONE = TilePropCodex.indexDamageToArrayAddr(1, 0);
    public static final int STONE_QUARRIED = TilePropCodex.indexDamageToArrayAddr(1, 1);
    public static final int DIRT = TilePropCodex.indexDamageToArrayAddr(2, 0);
    public static final int GRASS = TilePropCodex.indexDamageToArrayAddr(2, 1);

    public static final int PLANK_NORMAL = TilePropCodex.indexDamageToArrayAddr(3, 0);
    public static final int PLANK_EBONY = TilePropCodex.indexDamageToArrayAddr(3, 1);
    public static final int PLANK_BIRCH = TilePropCodex.indexDamageToArrayAddr(3, 2);
    public static final int PLANK_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(3, 3);

    public static final int TRUNK_NORMAL = TilePropCodex.indexDamageToArrayAddr(4, 0);
    public static final int TRUNK_EBONY = TilePropCodex.indexDamageToArrayAddr(4, 1);
    public static final int TRUNK_BIRCH = TilePropCodex.indexDamageToArrayAddr(4, 2);
    public static final int TRUNK_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(4, 3);

    public static final int SAND = TilePropCodex.indexDamageToArrayAddr(5, 0);
    public static final int SAND_BEACH = TilePropCodex.indexDamageToArrayAddr(5, 1);
    public static final int SAND_RED = TilePropCodex.indexDamageToArrayAddr(5, 2);
    public static final int SAND_DESERT = TilePropCodex.indexDamageToArrayAddr(5, 3);
    public static final int SAND_BLACK = TilePropCodex.indexDamageToArrayAddr(5, 4);

    public static final int GRAVEL = TilePropCodex.indexDamageToArrayAddr(6, 0);
    public static final int GRAVEL_GREY = TilePropCodex.indexDamageToArrayAddr(6, 1);

    public static final int ORE_COPPER = TilePropCodex.indexDamageToArrayAddr(7, 0);
    public static final int ORE_IRON = TilePropCodex.indexDamageToArrayAddr(7, 1);
    public static final int ORE_GOLD = TilePropCodex.indexDamageToArrayAddr(7, 2);
    public static final int ORE_SILVER = TilePropCodex.indexDamageToArrayAddr(7, 3);
    public static final int ORE_ILMENITE = TilePropCodex.indexDamageToArrayAddr(7, 4);
    public static final int ORE_AURICHALCUM = TilePropCodex.indexDamageToArrayAddr(7, 5);

    public static final int RAW_RUBY = TilePropCodex.indexDamageToArrayAddr(8, 0);
    public static final int RAW_EMERALD = TilePropCodex.indexDamageToArrayAddr(8, 1);
    public static final int RAW_SAPPHIRE = TilePropCodex.indexDamageToArrayAddr(8, 2);
    public static final int RAW_TOPAZ = TilePropCodex.indexDamageToArrayAddr(8, 3);
    public static final int RAW_DIAMOND = TilePropCodex.indexDamageToArrayAddr(8, 4);
    public static final int RAW_AMETHYST = TilePropCodex.indexDamageToArrayAddr(8, 5);

    public static final int SNOW = TilePropCodex.indexDamageToArrayAddr(9, 0);
    public static final int ICE_FRAGILE = TilePropCodex.indexDamageToArrayAddr(9, 1);
    public static final int ICE_NATURAL = TilePropCodex.indexDamageToArrayAddr(9, 2);
    public static final int ICE_MAGICAL = TilePropCodex.indexDamageToArrayAddr(9, 3);

    public static final int PLATFORM_STONE = TilePropCodex.indexDamageToArrayAddr(10, 0);
    public static final int PLATFORM_WOODEN = TilePropCodex.indexDamageToArrayAddr(10, 1);
    public static final int PLATFORM_EBONY = TilePropCodex.indexDamageToArrayAddr(10, 2);
    public static final int PLATFORM_BIRCH = TilePropCodex.indexDamageToArrayAddr(10, 3);
    public static final int PLATFORM_BLOODROSE = TilePropCodex.indexDamageToArrayAddr(10, 4);

    public static final int TORCH = TilePropCodex.indexDamageToArrayAddr(11, 0);

    public static final int WATER = TilePropCodex.indexDamageToArrayAddr(254, 15);
    public static final int LAVA = TilePropCodex.indexDamageToArrayAddr(255, 15);

}
