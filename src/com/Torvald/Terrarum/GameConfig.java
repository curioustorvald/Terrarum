package com.Torvald.Terrarum;

import java.util.Hashtable;

/**
 * Created by minjaesong on 15-12-30.
 */
public class GameConfig {

    private Hashtable<String, Object> configTable;

    public GameConfig() {
        this.configTable = new Hashtable<String, Object>();
    }

    /**
     * Add key-value pair to the configuration table.
     *
     * @param key
     * @param value
     */
    public void addKey(String key, Object value){
        configTable.put(key, value);
    }

    /**
     * Get value using key from configuration table.
     *
     * @param key
     * @return Object value
     */
    public Object get(String key){
        return configTable.get(key);
    }
}
