package com.Torvald.Terrarum.TileProperties;

import com.Torvald.CSVFetcher;
import com.Torvald.Terrarum.GameMap.MapLayer;
import com.Torvald.Terrarum.GameMap.PairedMapLayer;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.List;

/**
 * Created by minjaesong on 16-02-16.
 */
public class TilePropCodex {

    private static TileProp[] tileProps;

    public TilePropCodex() {
        tileProps = new TileProp[MapLayer.RANGE * (PairedMapLayer.RANGE)];

        for (int i = 0; i < tileProps.length; i++) {
            tileProps[i] = new TileProp();
        }

        try {
            // todo verify CSV using pre-calculated SHA256 hash
            List<CSVRecord> records = CSVFetcher.readCSV("" +
                    "./src/com/Torvald/Terrarum/TileProperties/tileprop.csv");

            System.out.println("[TilePropCodex] Building tile properties table");

            records.forEach(record ->
                    setProp(tileProps[indexDamageToArrayAddr
                            (intVal(record, "id")
                            , intVal(record, "dmg"))
                            ], record
            ));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TileProp getProp(int index, int damage) {
        try {
            tileProps[indexDamageToArrayAddr(index, damage)].getId();
        }
        catch (NullPointerException e) {
            throw new NullPointerException("Tile prop with id " + index
                    + " and damage " + damage
                    + " does not exist.");
        }

        return tileProps[indexDamageToArrayAddr(index, damage)];
    }

    public static TileProp getProp(int rawIndex) {
        try {
            tileProps[rawIndex].getId();
        }
        catch (NullPointerException e) {
            throw new NullPointerException("Tile prop with raw id " + rawIndex
                    + " does not exist.");
        }

        return tileProps[rawIndex];
    }

    private static void setProp(TileProp prop, CSVRecord record) {
        prop.setName(record.get("name"));

        prop.setId(intVal(record, "id"));
        prop.setDamage(intVal(record, "dmg"));

        prop.setOpacity((char) intVal(record, "opacity"));
        prop.setStrength(intVal(record, "strength"));
        prop.setDensity(intVal(record, "dsty"));
        prop.setLuminosity((char) intVal(record, "lumcolor"));
        prop.setDrop(intVal(record, "drop"));
        prop.setDropDamage(intVal(record, "ddmg"));
        prop.setFriction(intVal(record, "friction"));

        prop.setFluid(boolVal(record, "fluid"));
        prop.setSolid(boolVal(record, "solid"));
        prop.setWallable(boolVal(record, "wall"));
        prop.setFallable(boolVal(record, "fall"));

        if (prop.isFluid()) prop.setMovementResistance(intVal(record, "movr"));

        System.out.print(formatNum3(prop.getId()) + ":" + formatNum2(prop.getDamage()));
        System.out.println("\t" + prop.getName());
    }

    private static int intVal(CSVRecord rec, String s) {
        int ret = -1;
        try { ret = Integer.decode(rec.get(s)); }
        catch (NullPointerException e) {}
        return ret;
    }

    private static boolean boolVal(CSVRecord rec, String s) {
        return !(intVal(rec, s) == 0);
    }

    public static int indexDamageToArrayAddr(int index, int damage) {
        return (index * (PairedMapLayer.RANGE) + damage);
    }

    private static String formatNum3(int i) {
        if (i < 10) return "00" + i;
        else if (i < 100) return "0" + i;
        else return String.valueOf(i);
    }

    private static String formatNum2(int i) {
        if (i < 10) return "0" + i;
        else return String.valueOf(i);
    }
}
