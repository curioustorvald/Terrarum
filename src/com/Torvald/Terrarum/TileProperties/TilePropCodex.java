package com.Torvald.Terrarum.TileProperties;

import com.Torvald.CSVFetcher;
import com.Torvald.Terrarum.GameMap.MapLayer;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.List;

/**
 * Created by minjaesong on 16-02-16.
 */
public class TilePropCodex {

    private static TileProp[] tileProps;

    public TilePropCodex() {
        tileProps = new TileProp[MapLayer.TILES_SUPPORTED];

        for (int i = 0; i < tileProps.length; i++) {
            tileProps[i] = new TileProp();
        }

        try {
            List<CSVRecord> records = CSVFetcher.readCSV("" +
                    "./src/com/Torvald/Terrarum/TileProperties/propdata" +
                    ".csv");

            System.out.println("[TilePropCodex] Building tile properties table");

            records.forEach(record ->
                    setProp(tileProps[intVal(record, "id")], record
            ));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TileProp getProp(int index) {
        try {
            tileProps[index].getId();
        }
        catch (NullPointerException e) {
            throw new NullPointerException("Tile prop with id " + String.valueOf(index)
                    + " does not exist.");
        }

        return tileProps[index];
    }

    private void setProp(TileProp prop, CSVRecord record) {
        prop.setName(record.get("name"));

        prop.setId(intVal(record, "id"));

        prop.setOpacity(intVal(record, "opacity"));
        prop.setStrength(intVal(record, "strength"));
        prop.setLuminosity(intVal(record, "lumcolor"));
        prop.setDrop(intVal(record, "drop"));
        prop.setFriction(intVal(record, "friction"));

        prop.setFluid(boolVal(record, "fluid"));
        prop.setSolid(boolVal(record, "solid"));
        prop.setWallable(boolVal(record, "wall"));
        prop.setFallable(boolVal(record, "fall"));

        if (prop.isFluid()) prop.setOpacity(intVal(record, "opacity"));

        System.out.print(prop.getId());
        System.out.println("\t" + prop.getName());
    }

    private int intVal(CSVRecord rec, String s) {
        return Integer.decode(rec.get(s));
    }

    private boolean boolVal(CSVRecord rec, String s) {
        return !(intVal(rec, s) == 0);
    }
}
