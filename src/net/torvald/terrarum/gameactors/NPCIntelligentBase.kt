package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameactors.ai.ActorAI
import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.realestate.RealEstateUtility.getAbsoluteTileNumber
import org.newdawn.slick.GameContainer
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */
open class NPCIntelligentBase : ActorWithBody()
        , AIControlled, Pocketed, CanBeAnItem, Factionable, LandHolder {

    override var actorAI: ActorAI = object : ActorAI {
        // TODO fully establish ActorAI so that I can implement AI here
    }
    override var inventory: ActorInventory = ActorInventory()
    override var faction: HashSet<Faction> = HashSet()
    override var houseDesignation: ArrayList<Long>? = null

    // we're having InventoryItem data so that this class could be somewhat universal
    override var itemData: InventoryItem = object : InventoryItem {
        override var itemID = referenceID

        override var mass: Double
            get() = actorValue.getAsDouble(AVKey.BASEMASS)!!
            set(value) {
                actorValue[AVKey.BASEMASS] = value
            }

        override var scale: Double
            get() = actorValue.getAsDouble(AVKey.SCALE)!!
            set(value) {
                actorValue[AVKey.SCALE] = value
            }

        override fun effectWhileInPocket(gc: GameContainer, delta_t: Int) {

        }

        override fun effectWhenPickedUp(gc: GameContainer, delta_t: Int) {

        }

        override fun primaryUse(gc: GameContainer, delta_t: Int) {
            // TODO do not allow primary_use
        }

        override fun secondaryUse(gc: GameContainer, delta_t: Int) {
            // TODO place this Actor to the world
        }

        override fun effectWhenThrown(gc: GameContainer, delta_t: Int) {

        }
    }

    override fun getItemWeight(): Double {
        return mass
    }

    override fun addHouseTile(x: Int, y: Int) {
        if (houseDesignation != null) houseDesignation!!.add(getAbsoluteTileNumber(x, y))
    }

    override fun removeHouseTile(x: Int, y: Int) {
        if (houseDesignation != null) houseDesignation!!.remove(getAbsoluteTileNumber(x, y))
    }

    override fun clearHouseDesignation() {
        if (houseDesignation != null) houseDesignation!!.clear()
    }

    override fun stopUpdateAndDraw() {
        isUpdate = false
        isVisible = false
    }

    override fun resumeUpdateAndDraw() {
        isUpdate = true
        isVisible = true
    }


}