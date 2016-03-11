package com.Torvald.Terrarum.GameItem;

import java.util.HashMap;

/**
 * Created by minjaesong on 16-03-11.
 */
public class ItemCodex {

    /**
     * &lt;ItemID or RefID for Actor, TheItem&gt;
     * Will return corresponding Actor if ID >= 32768
     */
    private static HashMap<Long, InventoryItem> itemCodex;

    public static InventoryItem getItem(long code) {
        return itemCodex.get(code);
    }

}
