/* 
 * MapLoader version 1.2
 * Release date 2013-05-20
 * Copyright 2013 SKYHi14
 * 
 * The program is distributed in GNU GPL Licence version 3.
 * See http://www.gnu.org/licenses/gpl.html for information.
 */

package com.Torvald.Terrarum.GameMap;

import org.newdawn.slick.SlickException;

public class GameMap {

    //layers
    private volatile MapLayer layerWall;
    private volatile MapLayer layerTerrain;
    private volatile MapLayer layerWire;
    private volatile PairedMapLayer wallDamage;
    private volatile PairedMapLayer terrainDamage;

    //properties
    private int width;
    private int height;
    private int spawnX;
    private int spawnY;

    public static transient final int WALL = 0;
    public static transient final int TERRAIN = 1;
    public static transient final int WIRE = 2;

    //public World physWorld = new World( new Vec2(0, -TerrarumMain.game.gravitationalAccel) );
    //physics
    private float gravitation;
    private int globalLight;
    private WorldTime worldTime;

    public static transient final int TILES_SUPPORTED = MapLayer.RANGE * PairedMapLayer.RANGE;
    public static transient final byte BITS = 1; // 1 for Byte, 2 for Char, 4 for Int, 8 for Long
    public static transient final byte LAYERS = 4; // terrain, wall (terrainDamage + wallDamage), wire

    /**
     * @param width
     * @param height
     * @throws SlickException
     */
    public GameMap(int width, int height) throws SlickException {
        this.width = width;
        this.height = height;
        this.spawnX = width / 2;
        this.spawnY = 200;

        layerTerrain = new MapLayer(width, height);
        layerWall = new MapLayer(width, height);
        layerWire = new MapLayer(width, height);
        terrainDamage = new PairedMapLayer(width, height);
        wallDamage = new PairedMapLayer(width, height);

        globalLight = (char) 0;
        worldTime = new WorldTime();
    }

    public void setGravitation(float g) {
        gravitation = g;
    }

    /**
     * Get 2d array data of terrain
     *
     * @return byte[][] terrain layer
     */
    public byte[][] getTerrainArray() {
        return layerTerrain.data;
    }

    /**
     * Get 2d array data of wall
     *
     * @return byte[][] wall layer
     */
    public byte[][] getWallArray() {
        return layerWall.data;
    }

    /**
     * Get 2d array data of wire
     *
     * @return byte[][] wire layer
     */
    public byte[][] getWireArray() {
        return layerWire.data;
    }

    /**
     * Get paired array data of damage codes.
     * Format: 0baaaabbbb, aaaa for x = 0, 2, 4, ..., bbbb for x = 1, 3, 5, ...
     * @return byte[][] damage code pair
     */
    public byte[][] getDamageDataArray() {
        return terrainDamage.dataPair;
    }

    /**
     * Get MapLayer object of terrain
     *
     * @return MapLayer terrain layer
     */
    public MapLayer getLayerTerrain() {
        return layerTerrain;
    }

    public MapLayer getLayerWall() {
        return layerWall;
    }

    public MapLayer getLayerWire() {
        return layerWire;
    }

    public PairedMapLayer getTerrainDamage() {
        return terrainDamage;
    }

    public PairedMapLayer getWallDamage() {
        return wallDamage;
    }

    public int getTileFromWall(int x, int y) {
        return layerWall.getTile(x, y) * PairedMapLayer.RANGE + getWallDamage(x, y);
    }

    public int getTileFromTerrain(int x, int y) {
        return layerTerrain.getTile(x, y) * PairedMapLayer.RANGE + getTerrainDamage(x, y);
    }

    public int getTileFromWire(int x, int y) {
        return layerWire.getTile(x, y);
    }

    public int getWallDamage(int x, int y) {
        return wallDamage.getData(x, y);
    }

    public int getTerrainDamage(int x, int y) {
        return terrainDamage.getData(x, y);
    }

    /**
     * Set the tile of wall as specified, with damage value of zero.
     * @param x
     * @param y
     * @param combinedTilenum (tilenum * 16) + damage
     */
    public void setTileWall(int x, int y, int combinedTilenum) {
        setTileWall(x, y
                , (byte) (combinedTilenum / PairedMapLayer.RANGE)
                , combinedTilenum % PairedMapLayer.RANGE);
    }

    /**
     * Set the tile of wall as specified, with damage value of zero.
     * @param x
     * @param y
     * @param combinedTilenum (tilenum * 16) + damage
     */
    public void setTileTerrain(int x, int y, int combinedTilenum) {
        setTileTerrain(x, y
                , (byte) (combinedTilenum / PairedMapLayer.RANGE)
                , combinedTilenum % PairedMapLayer.RANGE);
    }

    public void setTileWall(int x, int y, byte tile, int damage) {
        layerWall.setTile(x, y, tile);
        wallDamage.setData(x, y, damage);
    }

    public void setTileTerrain(int x, int y, byte tile, int damage) {
        layerTerrain.setTile(x, y, tile);
        terrainDamage.setData(x, y, damage);
    }

    public void setTileWire(int x, int y, byte tile) {
        layerWire.data[y][x] = tile;
    }

    public int getTileFrom(int mode, int x, int y) {
        if (mode == TERRAIN) { return getTileFromTerrain(x, y); }
        else if (mode == WALL) { return getTileFromWall(x, y); }
        else if (mode == WIRE) { return getTileFromWire(x, y); }
        else throw new IllegalArgumentException("illegal mode input: " + String.valueOf(mode));
    }

    public void overwriteLayerWall(MapLayer layerData) {
        layerWall = layerData;
    }

    public void overwriteLayerTerrain(MapLayer layerData) {
        layerTerrain = layerData;
    }

    private int uint8ToInt32(byte x) {
        int ret;
        if ((x & 0b1000_0000) != 0) {
            ret = x & 0b1111_1111;
        } else {
            ret = x;
        }
        return ret;
    }

    public float getGravitation() {
        return gravitation;
    }

    public int getGlobalLight() {
        return globalLight;
    }

    public void setGlobalLight(int globalLight) {
        this.globalLight = globalLight;
    }

    public WorldTime getWorldTime() {
        return worldTime;
    }

    public void updateWorldTime(int delta) {
        worldTime.update(delta);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSpawnX() {
        return spawnX;
    }

    public int getSpawnY() {
        return spawnY;
    }
}