package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AIControlled
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.ai.ActorAI
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.itemproperties.Material

/**
 * @param ai AI class. Use LuaAIWrapper for Lua script
 *
 * Created by minjaesong on 2016-01-31.
 */
open class HumanoidNPC : ActorHumanoid, AIControlled, CanBeAnItem {

    var aiName: String = ""
    @Transient override lateinit var ai: ActorAI

    companion object {
        val DEFAULT_COLLISION_TYPE = COLLISION_DYNAMIC
    }

    protected constructor()

    constructor(ai: ActorAI, born: Long) : super(born) {
        this.ai = ai
        this.aiName = ai::class.java.canonicalName
    }

    init {
        collisionType = DEFAULT_COLLISION_TYPE
    }

    // we're having GameItem data so that this class could be somewhat universal
    override var itemData: GameItem = object : GameItem("actor:"+referenceID) {//GameItem(referenceID ?: forceAssignRefID!!) {
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
        override val isDynamic = false
        override val materialId = ""

        override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
            try {
                // place the actor to the world
                this@HumanoidNPC.setPosition(Terrarum.mouseX, Terrarum.mouseY)
                INGAME.queueActorAddition(this@HumanoidNPC)
                // successful
                return 1
            }
            catch (e: Exception) {
                e.printStackTrace()
                return -1
            }
        }

        init {
            isUnique = true
            stackable = false
            originalName = actorValue.getAsString(AVKey.NAME) ?: "NPC"
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
        ai.update(this, delta)
        super.update(delta)
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