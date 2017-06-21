package net.torvald.terrarum.gameactors

import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarum.gameactors.ai.ActorAI
import net.torvald.terrarum.gameactors.ai.LuaAIWrapper
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.Material

/**
 * @param ai AI class. Use LuaAIWrapper for Lua script
 *
 * Created by minjaesong on 16-01-31.
 */
open class HumanoidNPC(
        override val ai: ActorAI, // it's there for written-in-Kotlin, "hard-wired" AIs
        born: GameDate
) : ActorHumanoid(born), AIControlled, CanBeAnItem {

    constructor(luaAi: LuaAIWrapper, born: GameDate) : this(luaAi as ActorAI, born) {
        luaAi.attachActor(this)
    }

    companion object {
        val DEFAULT_COLLISION_TYPE = ActorWithPhysics.COLLISION_DYNAMIC
    }

    init {
        collisionType = DEFAULT_COLLISION_TYPE
    }

    // we're having GameItem data so that this class could be somewhat universal
    override var itemData: GameItem = object : GameItem() {
        override var dynamicID = referenceID
        override val originalID = dynamicID
        override val isUnique = true
        override var baseMass: Double
            get() = actorValue.getAsDouble(AVKey.BASEMASS)!!
            set(value) { actorValue[AVKey.BASEMASS] = value }
        override var baseToolSize: Double? = 0.0
        override var scale: Double
            get() = actorValue.getAsDouble(AVKey.SCALE)!!
            set(value) {
                actorValue[AVKey.SCALE] = value
            }
        override var inventoryCategory = "npc"
        override val originalName: String = actorValue.getAsString(AVKey.NAME) ?: "NPC"
        override var stackable = true
        override val isDynamic = false
        override val material = Material(0,0,0,0,0,0,0,0,0,0.0)

        override fun secondaryUse(delta: Float): Boolean {
            try {
                // place the actor to the world
                this@HumanoidNPC.setPosition(TerrarumGDX.mouseX, TerrarumGDX.mouseY)
                TerrarumGDX.ingame!!.addNewActor(this@HumanoidNPC)
                // successful
                return true
            }
            catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
    }

    override fun getItemWeight(): Double {
        return mass
    }

    override fun stopUpdateAndDraw() {
        isUpdate = false
        isVisible = false
    }

    override fun resumeUpdateAndDraw() {
        isUpdate = true
        isVisible = true
    }

    override fun update(delta: Float) {
        super.update(delta)
        ai.update(delta)
    }

    override fun moveLeft(amount: Float) { // hit the buttons on the controller box
        axisX = -amount
    }

    override fun moveRight(amount: Float) { // hit the buttons on the controller box
        axisX = amount
    }

    override fun moveUp(amount: Float) { // hit the buttons on the controller box
        axisY = -amount
    }

    override fun moveDown(amount: Float) { // hit the buttons on the controller box
        axisY = amount
    }

    override fun moveJump(amount: Float) { // hit the buttons on the controller box
        isJumpDown = true
    }

    /** fly toward arbitrary angle  WARNING: the map is looped! */
    override fun moveTo(bearing: Double) {
        // if your NPC should fly, override this
        throw UnsupportedOperationException("Humans cannot fly :p")
    }

    /** fly toward arbitrary coord  WARNING: the map is looped! */
    override fun moveTo(toX: Double, toY: Double) {
        // if your NPC should fly, override this
        throw UnsupportedOperationException("Humans cannot fly :p")
    }
}