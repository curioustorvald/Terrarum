package com.Torvald.Terrarum.Actors;

import com.Torvald.Terrarum.GameItem.InventoryItem;
import com.sun.istack.internal.Nullable;

import java.util.LinkedList;

/**
 * Created by minjaesong on 16-01-15.
 */
public class ActorInventory {

    @Nullable private int capacityByCount;
    @Nullable private int capacityByWeight;
    private int capacityMode;

    private LinkedList<InventoryItem> pocket;

    public final int CAPACITY_MODE_COUNT = 1;
    public final int CAPACITY_MODE_WEIGHT = 2;

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
     * Get reference to the pocket
     * @return
     */
    public LinkedList<InventoryItem> getPocket() {
        return pocket;
    }

    /**
     * Get clone of the pocket
     * @return
     */
    public LinkedList<InventoryItem> getCopyOfPocket() {
        return (LinkedList<InventoryItem>) (pocket.clone());
    }

    public float getTotalWeight() {
        float weight = 0;

        for (InventoryItem item : pocket) {
            weight += item.getWeight();
        }

        return weight;
    }

    public float getTotalCount() {
        int count = 0;

        for (InventoryItem item : pocket) {
            count += 1;
        }

        return count;
    }

    public void appendToPocket(InventoryItem item) {
        pocket.add(item);
    }

    /**
     * Check whether the pocket contains too many items
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
