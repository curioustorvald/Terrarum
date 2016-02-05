package com.Torvald.Terrarum.Actors;

import java.util.Hashtable;
import java.util.Set;

/**
 * Created by minjaesong on 16-01-03.
 */
public class ActorValue {

    private Hashtable<String, Object> configTable;

    public ActorValue() {
        configTable = new Hashtable<>();
    }

    /**
     * Add key-value pair to the configuration table.
     *
     * @param key case insensitive
     * @param value
     */
    public void set(String key, Object value){
        configTable.put(key.toLowerCase(), value);
    }

    /**
     * Get value using key from configuration table.
     *
     * @param key case insensitive
     * @return Object value
     */
    public Object get(String key){
        return configTable.get(key.toLowerCase());
    }

    public Set getKeySet() {
        return configTable.keySet();
    }

}
