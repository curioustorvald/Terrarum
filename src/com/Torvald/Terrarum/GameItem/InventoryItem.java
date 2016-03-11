package com.Torvald.Terrarum.GameItem;

import org.newdawn.slick.GameContainer;

/**
 * Created by minjaesong on 16-01-16.
 */
public interface InventoryItem {

    /**
     * Get internal ID of an Item.
     *   0-4096: Tiles
     *   4097-32767: Various items
     *   >=32768: Actor RefID
     * @return
     */
    long getItemID();

    /**
     * Weight of the item
     * @return
     */
    float getWeight();

    /**
     * Effects applied while in pocket
     * @param gc
     * @param delta_t
     */
    void effectWhileInPocket(GameContainer gc, int delta_t);

    /**
     * Effects applied immediately only once if picked up
     * @param gc
     * @param delta_t
     */
    void effectWhenPickedUp(GameContainer gc, int delta_t);

    /**
     * Effects applied while primary button (usually left mouse button) is down
     * @param gc
     * @param delta_t
     */
    void primaryUse(GameContainer gc, int delta_t);

    /**
     * Effects applied while secondary button (usually right mouse button) is down
     * @param gc
     * @param delta_t
     */
    void secondaryUse(GameContainer gc, int delta_t);

    /**
     * Effects applied immediately only once if thrown from pocket
     * @param gc
     * @param delta_t
     */
    void effectWhenThrownAway(GameContainer gc, int delta_t);

}
