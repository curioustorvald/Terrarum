package com.Torvald.Terrarum.Actors;

/**
 * Created by minjaesong on 16-01-15.
 */
public interface Pocketed {

    public ActorInventory getInventory();

    public void overwriteInventory(ActorInventory inventory);

}
