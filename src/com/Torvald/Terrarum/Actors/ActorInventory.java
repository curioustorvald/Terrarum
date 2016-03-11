package com.Torvald.Terrarum.Actors;

import com.Torvald.Terrarum.GameItem.InventoryItem;
import com.Torvald.Terrarum.GameItem.ItemCodex;
import com.sun.istack.internal.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Created by minjaesong on 16-01-15.
 */
public class ActorInventory {

    @Nullable private int capacityByCount;
    @Nullable private int capacityByWeight;
    private int capacityMode;

    /**
     * &lt;ReferenceID, Amounts&gt;
     */
    private HashMap<Long, Integer> itemList;

    public final transient int CAPACITY_MODE_COUNT = 1;
    public final transient int CAPACITY_MODE_WEIGHT = 2;

    /**
     * Construct new inventory with specified capacity.
     * @param capacity if is_weight is true, killogramme value is required, counts of items otherwise.
     * @param is_weight
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
            //weight += item.getWeight();
            weight += ItemCodex.getItem(item.getKey()).getWeight()
                    * item.getValue();
        }

        return weight;
    }

    public float getTotalCount() {
        int count = 0;

        for (Map.Entry<Long, Integer> item : itemList.entrySet()) {
            //weight += item.getWeight();
            count += item.getValue();
        }

        return count;
    }

    public void appendToPocket(InventoryItem item) {
        long key = item.getItemID();
        if (itemList.containsKey(key))
            itemList.put(key, itemList.get(key) + 1);
        else
            itemList.put(key, 1);
    }

    /**
     * Check whether the itemList contains too many items
     * @return
     */
    public boolean isEncumbered() {
        if (getCapacityMode() == CAPACITY_MODE_WEIGHT) {
            return (capacityByWeight < getTotalWeight());
        }
        else {
            return (capacityByCount < getTotalWeight());
        }
    }
}
