package com.Torvald.Terrarum.Actors.Faction;

import java.util.HashSet;

/**
 * Created by minjaesong on 16-02-15.
 */
public class Faction {

    private String factionName;
    private HashSet<String> factionAmicable;
    private HashSet<String> factionNeutral;
    private HashSet<String> factionHostile;
    private HashSet<String> factionFearful;

    public Faction(String factionName) {
        this.factionName = factionName;
        factionAmicable = new HashSet<>();
        factionNeutral = new HashSet<>();
        factionHostile = new HashSet<>();
        factionFearful = new HashSet<>();
    }

    public String getFactionName() {
        return factionName;
    }

    public void renewFactionName(String factionName) {
        this.factionName = factionName;
    }

    public HashSet<String> getFactionFearful() {
        return factionFearful;
    }

    public void setFactionFearful(HashSet<String> factionFearful) {
        this.factionFearful = factionFearful;
    }

    public HashSet<String> getFactionAmicable() {
        return factionAmicable;
    }

    public void setFactionAmicable(HashSet<String> factionAmicable) {
        this.factionAmicable = factionAmicable;
    }

    public HashSet<String> getFactionNeutral() {
        return factionNeutral;
    }

    public void setFactionNeutral(HashSet<String> factionNeutral) {
        this.factionNeutral = factionNeutral;
    }

    public HashSet<String> getFactionHostile() {
        return factionHostile;
    }

    public void setFactionHostile(HashSet<String> factionHostile) {
        this.factionHostile = factionHostile;
    }

    public void addFactionAmicable(String faction) {
        factionAmicable.add(faction);
    }

    public void addFactionNeutral(String faction) {
        factionNeutral.add(faction);
    }

    public void addFactionHostile(String faction) {
        factionHostile.add(faction);
    }

    public void addFactionFearful(String faction) {
        factionFearful.add(faction);
    }

    public void removeFactionAmicable(String faction) {
        factionAmicable.remove(faction);
    }

    public void removeFactionNeutral(String faction) {
        factionNeutral.remove(faction);
    }

    public void removeFactionHostile(String faction) {
        factionHostile.remove(faction);
    }

    public void removeFactionFearful(String faction) {
        factionFearful.remove(faction);
    }

}
