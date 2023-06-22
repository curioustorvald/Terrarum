package net.torvald.terrarum;

import com.badlogic.gdx.utils.JsonValue;
import net.torvald.terrarum.serialise.WriteConfig;
import net.torvald.terrarum.utils.JsonFetcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by minjaesong on 2023-06-22.
 */
public class Principii {

    public static KVHashMap gameConfig = new KVHashMap();

    public static String OSName = System.getProperty("os.name");

    public static String operationSystem;
    /** %appdata%/Terrarum, without trailing slash */
    public static String defaultDir;
    /** defaultDir + "/config.json" */
    public static String configDir;


    public static void getDefaultDirRoot() {
        String OS = OSName.toUpperCase();
        if (OS.contains("WIN")) {
            operationSystem = "WINDOWS";
            defaultDir = System.getenv("APPDATA") + "/Terrarum";
        }
        else if (OS.contains("OS X") || OS.contains("MACOS")) { // OpenJDK for mac will still report "Mac OS X" with version number "10.16", even on Big Sur and beyond
            operationSystem = "OSX";
            defaultDir = System.getProperty("user.home") + "/Library/Application Support/Terrarum";
        }
        else if (OS.contains("NUX") || OS.contains("NIX") || OS.contains("BSD")) {
            operationSystem = "LINUX";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
        }
        else if (OS.contains("SUNOS")) {
            operationSystem = "SOLARIS";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
        }
        else {
            operationSystem = "UNKNOWN";
            defaultDir = System.getProperty("user.home") + "/.Terrarum";
        }
    }


    public static void main(String[] args) {

        boolean devMode = false;

        // if -ea flag is set, turn on all the debug prints
        try {
            assert false;
        }
        catch (AssertionError e) {
            devMode = true;
        }

        String extracmd = devMode ? " -ea" : "";
        String OS = OSName.toUpperCase();
        String CPUARCH = System.getProperty("os.arch").toUpperCase();
        if (OS.contains("WIN")) {
        }
        else if (OS.contains("OS X") || OS.contains("MACOS")) { // OpenJDK for mac will still report "Mac OS X" with version number "10.16", even on Big Sur and beyond
            extracmd += " -XstartOnFirstThread";
        }
        else {
            extracmd += " -Dswing.aatext=true -Dawt.useSystemAAFontSettings=lcd";
        }


        getDefaultDirRoot();
        configDir = defaultDir + "/config.json";

        initialiseConfig();
        readConfigJson();


        int xmx = getConfigInt("jvm_xmx");

        try {
            Process proc = Runtime.getRuntime().exec("java"+extracmd+" -Xms1G -Xmx"+xmx+"G -cp ./out/TerrarumBuild.jar net.torvald.terrarum.App");

            // TODO redirect proc's PrintStream to System.out
            int size = 0;
            byte[] buffer = new byte[1024];
            while ((size = proc.getInputStream().read(buffer)) != -1) {
                System.out.write(buffer, 0, size);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    // CONFIG //



    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static int getConfigInt(String key) {
        Object cfg = getConfigMaster(key);

        if (cfg instanceof Integer) return ((int) cfg);

        double value = (double) cfg;

        if (Math.abs(value % 1.0) < 0.00000001)
            return (int) Math.round(value);
        return ((int) cfg);
    }


    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static double getConfigDouble(String key) {
        Object cfg = getConfigMaster(key);
        return (cfg instanceof Integer) ? (((Integer) cfg) * 1.0) : ((double) (cfg));
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist.
     * *
     * @throws NullPointerException if the specified config simply does not exist.
     */
    public static String getConfigString(String key) {
        Object cfg = getConfigMaster(key);
        return ((String) cfg);
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist. If the default value is undefined, will return false.
     */
    public static boolean getConfigBoolean(String key) {
        try {
            Object cfg = getConfigMaster(key);
            return ((boolean) cfg);
        }
        catch (NullPointerException keyNotFound) {
            return false;
        }
    }

    /*public static int[] getConfigIntArray(String key) {
        Object cfg = getConfigMaster(key);
        if (cfg instanceof JsonArray) {
            JsonArray jsonArray = ((JsonArray) cfg).getAsJsonArray();
            //return IntArray(jsonArray.size(), { i -> jsonArray[i].asInt })
            int[] intArray = new int[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                intArray[i] = jsonArray.get(i).getAsInt();
            }
            return intArray;
        }
        else
            return ((int[]) cfg);
    }*/

    public static double[] getConfigDoubleArray(String key) {
        Object cfg = getConfigMaster(key);
        return ((double[]) cfg);
    }

    public static int[] getConfigIntArray(String key) {
        double[] a = getConfigDoubleArray(key);
        int[] r = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = ((int) a[i]);
        }
        return r;
    }

    /*public static String[] getConfigStringArray(String key) {
        Object cfg = getConfigMaster(key);
        if (cfg instanceof JsonArray) {
            JsonArray jsonArray = ((JsonArray) cfg).getAsJsonArray();
            //return IntArray(jsonArray.size(), { i -> jsonArray[i].asInt })
            String[] intArray = new String[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                intArray[i] = jsonArray.get(i).getAsString();
            }
            return intArray;
        }
        else
            return ((String[]) cfg);
    }*/

    /**
     * Get config from config file. If the entry does not exist, get from defaults; if the entry is not in the default, NullPointerException will be thrown
     */
    private static HashMap<String, Object> getDefaultConfig() {
        return DefaultConfig.INSTANCE.getHashMap();
    }

    private static Object getConfigMaster(String key1) {
        String key = key1.toLowerCase();

        Object config;
        try {
            config = gameConfig.get(key);
        }
        catch (NullPointerException e) {
            config = null;
        }

        Object defaults;
        try {
            defaults = getDefaultConfig().get(key);
        }
        catch (NullPointerException e) {
            defaults = null;
        }

        if (config == null) {
            if (defaults == null) {
                throw new NullPointerException("key not found: '" + key + "'");
            }
            else {
                return defaults;
            }
        }
        else {
            return config;
        }
    }

    public static void setConfig(String key, Object value) {
        gameConfig.set(key.toLowerCase(), value);
    }


    /**
     *
     * @return true on successful, false on failure.
     */
    private static Boolean readConfigJson() {
        System.out.println("Config file: " + configDir);


        try {
            // read from disk and build config from it
            JsonValue map = JsonFetcher.INSTANCE.invoke(configDir);

            // make config
            for (JsonValue entry = map.child; entry != null; entry = entry.next) {
                setToGameConfigForced(entry, null);
            }

            return true;
        }
        catch (IOException e) {
            // write default config to game dir. Call th.is method again to read config from it.
            try {
                createConfigJson();
            }
            catch (IOException e1) {
                System.out.println("[Bootstrap] Unable to write config.json file");
                e.printStackTrace();
            }

            return false;
        }

    }


    private static void createConfigJson() throws IOException {
        File configFile = new File(configDir);

        if (!configFile.exists() || configFile.length() == 0L) {
            WriteConfig.INSTANCE.invoke();
        }
    }

    /**
     * Reads DefaultConfig to populate the gameConfig
     */
    private static void initialiseConfig() {
        for (Map.Entry<String, Object> entry : DefaultConfig.INSTANCE.getHashMap().entrySet()) {
            gameConfig.set(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Will forcibly overwrite previously loaded config value.
     *
     * Key naming convention will be 'modName:propertyName'; if modName is null, the key will be just propertyName.
     *
     * @param value JsonValue (the key-value pair)
     * @param modName module name, nullable
     */
    public static void setToGameConfigForced(JsonValue value, String modName) {
        gameConfig.set((modName == null) ? value.name : modName+":"+value.name,
                value.isArray() ? value.asDoubleArray() :
                        value.isDouble() ? value.asDouble() :
                                value.isBoolean() ? value.asBoolean() :
                                        value.isLong() ? value.asInt() :
                                                value.asString()
        );
    }

    /**
     * Will not overwrite previously loaded config value.
     *
     * Key naming convention will be 'modName:propertyName'; if modName is null, the key will be just propertyName.
     *
     * @param value JsonValue (the key-value pair)
     * @param modName module name, nullable
     */
    public static void setToGameConfig(JsonValue value, String modName) {
        String key = (modName == null) ? value.name : modName+":"+value.name;
        if (gameConfig.get(key) == null) {
            gameConfig.set(key,
                    value.isArray() ? value.asDoubleArray() :
                            value.isDouble() ? value.asDouble() :
                                    value.isBoolean() ? value.asBoolean() :
                                            value.isLong() ? value.asInt() :
                                                    value.asString()
            );
        }
    }

}