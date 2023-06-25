package net.torvald.terrarum;

import com.badlogic.gdx.utils.JsonValue;
import net.torvald.terrarum.utils.JsonFetcher;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrapper that launches the bundled JVM and injects VM configs such as -Xmx
 *
 * Created by minjaesong on 2023-06-22.
 */
public class Principii {

    private static KVHashMap gameConfig = new KVHashMap();

    private static String OSName = System.getProperty("os.name");

    private static String operationSystem;
    /** %appdata%/Terrarum, without trailing slash */
    private static String defaultDir;
    /** defaultDir + "/config.json" */
    private static String configDir;


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
        String runtimeRoot;
        String runtimeArch;
        if (!CPUARCH.equals("AMD64") && !CPUARCH.equals("X86_64") && !CPUARCH.equals("AARCH64")) { // macOS Rosetta2 reports X86_64
            System.err.println("Unsupported CPU architecture: " + CPUARCH);
            System.exit(1);
            return;
        }
        else {
            runtimeArch = (CPUARCH.equals("AMD64") || CPUARCH.equals("X86_64")) ? "x86" : "arm";
        }

        if (OS.contains("WIN")) {
            runtimeRoot = "runtime-windows-" + runtimeArch;
        }
        else if (OS.contains("OS X") || OS.contains("MACOS")) { // OpenJDK for mac will still report "Mac OS X" with version number "10.16", even on Big Sur and beyond
            runtimeRoot = "runtime-osx-" + runtimeArch;
            extracmd += " -XstartOnFirstThread";
        }
        else {
            runtimeRoot = "runtime-linux-" + runtimeArch;
            extracmd += " -Dswing.aatext=true -Dawt.useSystemAAFontSettings=lcd";
        }

        String runtime = new File("out/"+runtimeRoot+"/bin/java").getAbsolutePath();
        System.out.println("Runtime path: "+runtime);



        getDefaultDirRoot();
        configDir = defaultDir + "/config.json";

        initialiseConfig();
        readConfigJson();


        int xmx = getConfigInt("jvm_xmx");
        String userDefinedExtraCmd = getConfigString("jvm_extra_cmd").trim();
        if (!userDefinedExtraCmd.isEmpty()) userDefinedExtraCmd = " "+userDefinedExtraCmd;



        try {
            String[] cmd = (runtime+extracmd+userDefinedExtraCmd+" -Xms1G -Xmx"+xmx+"G -cp ./out/TerrarumBuild.jar net.torvald.terrarum.App").split(" ");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();
            System.exit(pb.start().waitFor());
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        catch (InterruptedException e) {
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
    private static int getConfigInt(String key) {
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
    private static double getConfigDouble(String key) {
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
    private static String getConfigString(String key) {
        Object cfg = getConfigMaster(key);
        return ((String) cfg);
    }

    /**
     * Return config from config set. If the config does not exist, default value will be returned.
     * @param key
     * *
     * @return Config from config set or default config if it does not exist. If the default value is undefined, will return false.
     */
    private static boolean getConfigBoolean(String key) {
        try {
            Object cfg = getConfigMaster(key);
            return ((boolean) cfg);
        }
        catch (NullPointerException keyNotFound) {
            return false;
        }
    }

    /*private static int[] getConfigIntArray(String key) {
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

    private static double[] getConfigDoubleArray(String key) {
        Object cfg = getConfigMaster(key);
        return ((double[]) cfg);
    }

    private static int[] getConfigIntArray(String key) {
        double[] a = getConfigDoubleArray(key);
        int[] r = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = ((int) a[i]);
        }
        return r;
    }

    /*private static String[] getConfigStringArray(String key) {
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
            e.printStackTrace();

            return false;
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
    private static void setToGameConfigForced(JsonValue value, String modName) {
        gameConfig.set((modName == null) ? value.name : modName+":"+value.name,
                value.isArray() ? value.asDoubleArray() :
                        value.isDouble() ? value.asDouble() :
                                value.isBoolean() ? value.asBoolean() :
                                        value.isLong() ? value.asInt() :
                                                value.asString()
        );
    }


}