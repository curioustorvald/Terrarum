package com.Torvald.Terrarum;

import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

/**
 * Created by minjaesong on 15-12-30.
 */
public class KVHashMap {

    private HashMap<String, Object> hashMap;

    public KVHashMap() {
        hashMap = new HashMap<>();
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
        hashMap.put(key.toLowerCase(), value);
    }

    /**
     * Get value using key from configuration table.
     *
     * @param key case insensitive
     * @return Object value
     */
    public Object get(String key){
        return hashMap.get(key.toLowerCase());
    }

    public int getAsInt(String key) {
        return (int) get(key);
    }

    public float getAsFloat(String key) {
        Object value = get(key);
        if (value instanceof Integer) return ((Integer) value).floatValue();
        else if (value instanceof JsonPrimitive) return ((JsonPrimitive) value).getAsFloat();
        return (float) value;
    }

    public String getAsString(String key) {
        Object value = get(key);
        if (value instanceof JsonPrimitive) return ((JsonPrimitive) value).getAsString();
        return (String) value;
    }

    public boolean getAsBoolean(String key) {
        Object value = get(key);
        if (value instanceof JsonPrimitive) return ((JsonPrimitive) value).getAsBoolean();
        return (boolean) value;
    }

    public boolean hasKey(String key) {
        return hashMap.containsKey(key);
    }

    public Set getKeySet() {
        return hashMap.keySet();
    }

}