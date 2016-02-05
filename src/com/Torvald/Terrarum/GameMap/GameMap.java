/* 
 * MapLoader version 1.2
 * Release date 2013-05-20
 * Copyright 2013 SKYHi14
 * 
 * The program is distributed in GNU GPL Licence version 3.
 * See http://www.gnu.org/licenses/gpl.html for information.
 */

package com.Torvald.Terrarum.GameMap;

import com.sun.istack.internal.NotNull;
import org.newdawn.slick.SlickException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Spliterator;
import java.util.function.Consumer;

public class GameMap {

    //layers
    private MapLayer layerWall;
    private MapLayer layerTerrain;
    private MapLayer layerWire;

    //properties
    public int width;
    public int height;
    public int spawnX;
    public int spawnY;
    int offset;

    public LinkedList<MapPoint> houseDesignation;

    //public World physWorld = new World( new Vec2(0, -TerrarumMain.game.gravitationalAccel) );
    //physics
    @NotNull
    private float gravitation;

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

    public int getTileFromWall(int x, int y) {
        return uint8ToInt32(layerWall.data[y][x]);
    }

    public int getTileFromTerrain(int x, int y) {
        return uint8ToInt32(layerTerrain.data[y][x]);
    }

    public int getTileFromWire(int x, int y) {
        return uint8ToInt32(layerWire.data[y][x]);
    }

    private int uint8ToInt32(byte x) {
        int ret;
        if ((x & 0b1000_0000) != 0) {
            ret = (x & 0b0111_1111) | (x & 0b1000_0000);
        } else {
            ret = x;
        }
        return ret;
    }

    public float getGravitation() {
        return gravitation;
    }
}