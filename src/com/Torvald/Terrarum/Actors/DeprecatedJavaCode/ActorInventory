package com.Torvald.Terrarum.Actors;

import com.Torvald.Terrarum.GameItem.InventoryItem;
import com.Torvald.Terrarum.GameItem.ItemCodex;
import com.Torvald.Terrarum.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by minjaesong on 16-01-15.
 */
public class ActorInventory {

    private @Nullable int capacityByCount;
    private @Nullable int capacityByWeight;
    private int capacityMode;

    /**
     * &lt;ReferenceID, Amounts&gt;
     */
    private HashMap<Long, Integer> itemList;

    public transient final int CAPACITY_MODE_COUNT = 1;
    public transient final int CAPACITY_MODE_WEIGHT = 2;

    /**
     * Construct new inventory with specified capacity.
     * @param capacity if is_weight is true, killogramme value is required, counts of items otherwise.
     * @param is_weight whether encumbrance should be calculated upon the weight of the inventory. False to use item counts.
     */
    public ActorInventory(int capacity, boolean is_weight) {
        if (is_weight) {
            capacityByWeight = capacity;
            capacityMode = CAPACITY_MODE_WEIGHT;
        }
        else{
            capacityByCount = capacity;
            capacityMode = CAPACITY_MODE_COUNT;
        }
    }

    /**
     * Get capacity of inventory
     * @return
     */
    public int getCapacity() {
        if (capacityMode == CAPACITY_MODE_WEIGHT) {
            return capacityByWeight;
        }
        else {
            return capacityByCount;
        }
    }

    public int getCapacityMode() {
        return capacityMode;
    }

    /**
     * Get reference to the itemList
     * @return
     */
    public Map<Long, Integer> getItemList() {
        return itemList;
    }

    /**
     * Get clone of the itemList
     * @return
     */
    public Map getCopyOfItemList() {
        return (Map) (itemList.clone());
    }

    public float getTotalWeight() {
        float weight = 0;

        for (Map.Entry<Long, Integer> item : itemList.entrySet()) {
            weight += ItemCodex.getItem(item.getKey()).getWeight()
                    * item.getValue();
        }

        return weight;
    }

    public int getTotalCount() {
        int count = 0;

        for (Map.Entry<Long, Integer> item : itemList.entrySet()) {
            count += item.getValue();
        }

        return count;
    }

    public int getTotalUniqueCount() {
        return itemList.entrySet().size();
    }

    public void appendToPocket(InventoryItem item) {
        appendToPocket(item, 1);
    }

    public void appendToPocket(InventoryItem item, int count) {
        long key = item.getItemID();

        // if (key == Player.PLAYER_REF_ID)
        //     throw new IllegalArgumentException("Attempted to put player into the inventory.");

        if (itemList.containsKey(key))
            // increment amount if it already has specified item
            itemList.put(key, itemList.get(key) + count);
        else
            // add new entry if it does not have specified item
            itemList.put(key, count);
    }

    /**
     * Check whether the itemList contains too many items
     * @return
     */
    public boolean isEncumbered() {
        if (getCapacityMode() == CAPACITY_MODE_WEIGHT) {
            return (capacityByWeight < getTotalWeight());
        }
        else if (getCapacityMode() == CAPACITY_MODE_COUNT) {
            return (capacityByCount < getTotalWeight());
        }
        else {
            throw new UnsupportedOperationException("capacity mode not valid.");
        }
    }
}
