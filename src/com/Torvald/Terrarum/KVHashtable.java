package com.Torvald.Terrarum;

import java.util.Hashtable;
import java.util.Set;

/**
 * Created by minjaesong on 15-12-30.
 */
public class KVHashtable {

    private Hashtable<String, Object> hashtable;

    public KVHashtable() {
        hashtable = new Hashtable<>();
    }

    /**
     * Add key-value pair to the configuration table.
     * If key does not exist on the table, new key will be generated.
     * If key already exists, the value will be overwritten.
     *
     * @param key case insensitive
     * @param value
     */
    public void set(String key, Object value){
        hashtable.put(key.toLowerCase(), value);
    }

    /**
     * Get value using key from configuration table.
     *
     * @param key case insensitive
     * @return Object value
     */
    public Object get(String key){
        return hashtable.get(key.toLowerCase());
    }

    public float getAsFloat(String key) {
        return (float) get(key);
    }

    public String getAsString(String key) {
        return (String) get(key);
    }

    public boolean getAsBoolean(String key) {
        return (boolean) get(key);
    }

    public Set getKeySet() {
        return hashtable.keySet();
    }

}