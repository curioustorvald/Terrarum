package com.Torvald.Terrarum.Actors;

import com.Torvald.Terrarum.Actors.AI.ActorAI;
import com.Torvald.Terrarum.Actors.Faction.Faction;
import com.Torvald.Terrarum.GameItem.InventoryItem;
import com.Torvald.Terrarum.Terrarum;
import org.newdawn.slick.GameContainer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by minjaesong on 16-01-31.
 */
public class NPCIntelligentBase extends ActorWithBody implements AIControlled, Pocketed, CanBeStoredAsItem,
        Factionable, Landholder {

    private InventoryItem itemData; // keep it for extendibility, like Carriers in SC1
    private transient ActorAI ai;
    private ActorInventory inventory;

    private HashSet<Faction> factionSet = new HashSet<>();

    /**
     * Absolute tile index. index(x, y) = y * map.width + x <br />
     * The arraylist will be saved in JSON format with GSON.
     */
    private ArrayList<Integer> houseTiles = new ArrayList<>();

    @Override
    public void assignFaction(Faction f) {
        factionSet.add(f);
    }

    @Override
    public void unassignFaction(Faction f) {
        factionSet.remove(f);
    }

    @Override
    public HashSet<Faction> getAssignedFactions() {
        return factionSet;
    }

    @Override
    public void clearFactionAssigning() {
        factionSet.clear();
    }

    @Override
    public void attachItemData() {
        itemData = new InventoryItem() {
            @Override
            public long getItemID() {
                return 0;
            }

            @Override
            public float getWeight() {
                return 0;
            }

            @Override
            public void effectWhileInPocket(GameContainer gc, int delta_t) {

            }

            @Override
            public void effectWhenPickedUp(GameContainer gc, int delta_t) {

            }

            @Override
            public void primaryUse(GameContainer gc, int delta_t) {

            }

            @Override
            public void secondaryUse(GameContainer gc, int delta_t) {

            }

            @Override
            public void effectWhenThrownAway(GameContainer gc, int delta_t) {

            }
        };
    }

    @Override
    public float getItemWeight() {
        return super.getMass();
    }

    @Override
    public ArrayList<Integer> getHouseDesignation() {
        return houseTiles;
    }

    @Override
    public void setHouseDesignation(ArrayList<Integer> list) {
        houseTiles = list;
    }

    @Override
    public void addHouseTile(int x, int y) {
        houseTiles.add(Terrarum.game.map.width * y + x);
    }

    @Override
    public void removeHouseTile(int x, int y) {
        houseTiles.remove(new Integer(Terrarum.game.map.width * y + x));
    }

    @Override
    public void clearHouseDesignation() {
        houseTiles.clear();
    }

    @Override
    public void stopUpdateAndDraw() {
        super.setUpdate(false);
        super.setVisible(false);
    }

    @Override
    public void resumeUpdateAndDraw() {
        super.setUpdate(true);
        super.setVisible(true);
    }

    @Override
    public InventoryItem getItemData() {
        return itemData;
    }

    @Override
    public ActorInventory getInventory() {
        return null;
    }

    @Override
    public void overwriteInventory(ActorInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void attachAI(ActorAI ai) {
        this.ai = ai;
    }
}
