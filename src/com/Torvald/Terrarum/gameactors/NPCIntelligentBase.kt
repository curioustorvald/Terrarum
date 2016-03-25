package com.torvald.terrarum.gameactors

import com.torvald.random.HQRNG
import com.torvald.terrarum.gameactors.ai.ActorAI
import com.torvald.terrarum.gameactors.faction.Faction
import com.torvald.terrarum.gameitem.InventoryItem
import com.torvald.terrarum.Terrarum
import org.newdawn.slick.GameContainer
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */
open class NPCIntelligentBase : ActorWithBody()
        , AIControlled, Pocketed, CanBeStoredAsItem, Factionable, LandHolder {

    override var itemData: InventoryItem = object : InventoryItem {
        override var itemID = HQRNG().nextLong()

        override var mass: Float
            get() = actorValue.get("mass") as Float
            set(value) {
                actorValue.set("mass", value)
            }

        override var scale: Float
            get() = actorValue.get("scale") as Float
            set(value) {
                actorValue.set("scale", value)
            }

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
    override var inventory: ActorInventory = ActorInventory()

    private val factionSet = HashSet<Faction>()

    override var referenceID: Long = HQRNG().nextLong()

    override var faction: HashSet<Faction> = HashSet()

    override var houseDesignation: ArrayList<Int>? = null
    /**
     * Absolute tile index. index(x, y) = y * map.width + x
     * The arraylist will be saved in JSON format with GSON.
     */
    private var houseTiles = ArrayList<Int>()

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