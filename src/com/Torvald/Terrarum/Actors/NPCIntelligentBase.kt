package com.Torvald.Terrarum.Actors

import com.Torvald.Rand.HQRNG
import com.Torvald.Terrarum.Actors.AI.ActorAI
import com.Torvald.Terrarum.Actors.Faction.Faction
import com.Torvald.Terrarum.GameItem.InventoryItem
import com.Torvald.Terrarum.Terrarum
import org.newdawn.slick.GameContainer
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */
open class NPCIntelligentBase : ActorWithBody()
        , AIControlled, Pocketed, CanBeStoredAsItem, Factionable, LandHolder {

    override var itemData: InventoryItem = object : InventoryItem {
        override var itemID = HQRNG().nextLong()

        override var weight = 0f

        override fun effectWhileInPocket(gc: GameContainer, delta_t: Int) {

        }

        override fun effectWhenPickedUp(gc: GameContainer, delta_t: Int) {

        }

        override fun primaryUse(gc: GameContainer, delta_t: Int) {

        }

        override fun secondaryUse(gc: GameContainer, delta_t: Int) {

        }

        override fun effectWhenThrownAway(gc: GameContainer, delta_t: Int) {

        }
    }

    @Transient private var ai: ActorAI? = null
    override var inventory: ActorInventory? = null

    private val factionSet = HashSet<Faction>()

    override var referenceID: Long = HQRNG().nextLong()

    override var faction: HashSet<Faction>? = null

    override var houseDesignation: ArrayList<Int>? = null
    /**
     * Absolute tile index. index(x, y) = y * map.width + x
     * The arraylist will be saved in JSON format with GSON.
     */
    private var houseTiles = ArrayList<Int>()

    override fun assignFaction(f: Faction) {
        factionSet.add(f)
    }

    override fun unassignFaction(f: Faction) {
        factionSet.remove(f)
    }

    override fun attachItemData() {

    }

    override fun getItemWeight(): Float {
        return mass
    }

    override fun addHouseTile(x: Int, y: Int) {
        houseTiles.add(Terrarum.game.map.width * y + x)
    }

    override fun removeHouseTile(x: Int, y: Int) {
        houseTiles.remove(Terrarum.game.map.width * y + x)
    }

    override fun clearHouseDesignation() {
        houseTiles.clear()
    }

    override fun stopUpdateAndDraw() {
        isUpdate = false
        isVisible = false
    }

    override fun resumeUpdateAndDraw() {
        isUpdate = true
        isVisible = true
    }

    override fun attachAI(ai: ActorAI) {
        this.ai = ai
    }
}