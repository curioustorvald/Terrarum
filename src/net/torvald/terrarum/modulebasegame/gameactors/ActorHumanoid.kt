package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.jme3.math.FastMath
import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.itemproperties.GameItem
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.gameworld.time_t
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import org.dyn4j.geometry.Vector2
import java.util.*

/**
 * Humanoid actor class to provide same controlling function (such as work, jump)
 * Also applies unreal air friction for movement control
 *
 * For some actors that "HasAssembledSprite", sprite rows must be in this specific order:
 *  1. Idle
 *  2. Walk
 *  ...
 *
 * Created by minjaesong on 2016-10-24.
 */
open class ActorHumanoid(
        birth: time_t,
        death: time_t? = null,
        usePhysics: Boolean = true
) : ActorWBMovable(RenderOrder.MIDDLE, usePhysics = usePhysics), Controllable, Pocketed, Factionable, Luminous, LandHolder, HistoricalFigure {

    private val world: GameWorld?
        get() = Terrarum.ingame?.world


    var vehicleRiding: Controllable? = null // usually player only



    /** Must be set by PlayerFactory */
    override var inventory: ActorInventory = ActorInventory(this, 2000, ActorInventory.CAPACITY_MODE_WEIGHT) // default constructor


    /** Must be set by PlayerFactory */
    override var faction: HashSet<Faction> = HashSet()
    /**
     * Absolute tile index. index(x, y) = y * map.width + x
     * The arraylist will be saved in JSON format with GSON.
     */
    override var houseDesignation: ArrayList<Long>? = ArrayList()

    override fun addHouseTile(x: Int, y: Int) {
        if (houseDesignation != null) houseDesignation!!.add(LandUtil.getBlockAddr(world!!, x, y))
    }

    override fun removeHouseTile(x: Int, y: Int) {
        if (houseDesignation != null) houseDesignation!!.remove(LandUtil.getBlockAddr(world!!, x, y))
    }

    override fun clearHouseDesignation() {
        if (houseDesignation != null) houseDesignation!!.clear()
    }

    override var color: Color
        get() = Color(
                (actorValue.getAsFloat(AVKey.LUMR) ?: 0f) / LightmapRenderer.MUL_FLOAT,
                (actorValue.getAsFloat(AVKey.LUMG) ?: 0f) / LightmapRenderer.MUL_FLOAT,
                (actorValue.getAsFloat(AVKey.LUMB) ?: 0f) / LightmapRenderer.MUL_FLOAT,
                (actorValue.getAsFloat(AVKey.LUMA) ?: 0f) / LightmapRenderer.MUL_FLOAT
        )
        set(value) {
            actorValue[AVKey.LUMR] = value.r * LightmapRenderer.MUL_FLOAT
            actorValue[AVKey.LUMG] = value.g * LightmapRenderer.MUL_FLOAT
            actorValue[AVKey.LUMB] = value.b * LightmapRenderer.MUL_FLOAT
            actorValue[AVKey.LUMA] = value.a * LightmapRenderer.MUL_FLOAT
        }

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    override val lightBoxList: List<Hitbox>
        get() = arrayOf(Hitbox(2.0, 2.0, hitbox.width - 3, hitbox.height - 3)).toList() // things are asymmetric!!
        // use getter; dimension of the player may change by time.

    @Transient val BASE_DENSITY = 980.0

    companion object {
        //@Transient internal const val ACCEL_MULT_IN_FLIGHT: Double = 0.21
        @Transient internal const val WALK_ACCEL_BASE: Double = 0.67

        @Transient const val BASE_HEIGHT = 40
        // 0.33333 miliseconds
        @Transient const val BASE_ACTION_INTERVAL = 1.0 / 3.0

        @Transient const val SPRITE_ROW_IDLE = 0
        @Transient const val SPRITE_ROW_WALK = 1
    }

    ////////////////////////////////
    // MOVEMENT RELATED FUNCTIONS //
    ////////////////////////////////

    var axisX = 0f; protected set
    var axisY = 0f; protected set
    var axisRX = 0f; protected set
    var axisRY = 0f; protected set

    /** empirical value. */
    @Transient private val JUMP_ACCELERATION_MOD = 51.0 / 10000.0 // (170 * (17/MAX_JUMP_LENGTH)^2) / 10000.0
    @Transient private val WALK_FRAMES_TO_MAX_ACCEL = 6

    @Transient private val LEFT = 1
    @Transient private val RIGHT = 2

    @Transient private val KEY_NULL = -1

    /** how long the jump button has down, in frames */
    internal var jumpCounter = 0
    internal var jumpAcc = 0.0
    /** how long the walk button has down, in frames */
    internal var walkCounterX = 0
    internal var walkCounterY = 0
    @Transient private val MAX_JUMP_LENGTH = 25 // manages "heaviness" of the jump control. Higher = heavier

    private var readonly_totalX = 0.0
    private var readonly_totalY = 0.0

    internal var jumping = false
    internal var airJumpingAllowed = false

    internal var walkHeading: Int = 0

    @Transient private var prevHMoveKey = KEY_NULL
    @Transient private var prevVMoveKey = KEY_NULL

    @Transient private val AXIS_KEYBOARD = -13372f // leetz
    @Transient private val GAMEPAD_JUMP = 7

    var isUpDown = false; protected set
    var isDownDown = false; protected set
    var isLeftDown = false; protected set
    var isRightDown = false; protected set
    var isJumpDown = false; protected set
    protected inline val isGamer: Boolean
        get() = if (Terrarum.ingame == null) false else this == Terrarum.ingame!!.actorNowPlaying


    private val nullItem = object : GameItem() {
        override var dynamicID: Int = 0
        override val originalID = dynamicID
        override val isUnique: Boolean = false
        override var baseMass: Double = 0.0
        override var baseToolSize: Double? = null
        override var inventoryCategory = "should_not_be_seen"
        override val originalName: String = actorValue.getAsString(AVKey.NAME) ?: "(no name)"
        override var stackable = false
        override val isDynamic = false
        override val material = Material(0,0,0,0,0,0,0,0,0,0.0)
    }

    init {
        actorValue[AVKey.__HISTORICAL_BORNTIME] = birth
        death?.let { actorValue[AVKey.__HISTORICAL_DEADTIME] = death }
    }

    override fun update(delta: Float) {
        super.update(delta)

        if (vehicleRiding is IngamePlayer)
            throw Error("Attempted to 'ride' player object. ($vehicleRiding)")
        if (vehicleRiding != null && vehicleRiding == this)
            throw Error("Attempted to 'ride' itself. ($vehicleRiding)")



        // don't put this into keyPressed; execution order is important!
        updateGamerControlBox()

        processInput(delta)

        updateSprite(delta)

        if (isNoClip) {
            //grounded = true
        }

        // reset control box of AI
        if (!isGamer) {
            isUpDown = false
            isDownDown = false
            isLeftDown = false
            isRightDown = false
            isJumpDown = false
            axisX = 0f
            axisY = 0f
            axisRX = 0f
            axisRY = 0f
        }

        // update inventory items
        inventory.forEach {
            if (!inventory.itemEquipped.contains(it.item)) { // unequipped
                it.item.effectWhileInPocket(delta)
            }
            else { // equipped
                it.item.effectWhenEquipped(delta)
            }
        }
    }

    private fun updateGamerControlBox() {
        if (isGamer) {
            isUpDown = Gdx.input.isKeyPressed(AppLoader.getConfigInt("keyup"))
            isLeftDown = Gdx.input.isKeyPressed(AppLoader.getConfigInt("keyleft"))
            isDownDown = Gdx.input.isKeyPressed(AppLoader.getConfigInt("keydown"))
            isRightDown = Gdx.input.isKeyPressed(AppLoader.getConfigInt("keyright"))
            isJumpDown = Gdx.input.isKeyPressed(AppLoader.getConfigInt("keyjump"))

            if (Terrarum.controller != null) {
                axisX =  Terrarum.controller!!.getAxisValue(AppLoader.getConfigInt("joypadlstickx"))
                axisY =  Terrarum.controller!!.getAxisValue(AppLoader.getConfigInt("joypadlsticky"))
                axisRX = Terrarum.controller!!.getAxisValue(AppLoader.getConfigInt("joypadrstickx"))
                axisRY = Terrarum.controller!!.getAxisValue(AppLoader.getConfigInt("joypadrsticky"))

                // deadzonning
                if (Math.abs(axisX) < Terrarum.CONTROLLER_DEADZONE) axisX = 0f
                if (Math.abs(axisY) < Terrarum.CONTROLLER_DEADZONE) axisY = 0f
                if (Math.abs(axisRX) < Terrarum.CONTROLLER_DEADZONE) axisRX = 0f
                if (Math.abs(axisRY) < Terrarum.CONTROLLER_DEADZONE) axisRY = 0f

                isJumpDown = Gdx.input.isKeyPressed(AppLoader.getConfigInt("keyjump")) ||
                             Terrarum.controller!!.isButtonPressed(GAMEPAD_JUMP)
            }
        }
        else {
            isUpDown = axisY < 0f
            isDownDown = axisY > 0f
            isLeftDown = axisX < 0f
            isRightDown = axisX > 0f
        }
    }

    private inline val hasController: Boolean
        get() = if (isGamer) Terrarum.controller != null
                else true
    
    private fun processInput(delta: Float) {

        /**
         * L-R stop
         */
        if (hasController && !isWalkingH) {
            if (axisX == 0f) {
                walkHStop()
            }
        }
        // ↑F, ↑S
        if (isWalkingH && !isLeftDown && !isRightDown) {
            walkHStop()
            prevHMoveKey = KEY_NULL
        }
        /**
         * U-D stop
         */
        if (hasController) {
            if (axisY == 0f) {
                walkVStop()
            }
        }
        // ↑E
        // ↑D
        if (isNoClip && !isUpDown && !isDownDown) {
            walkVStop()
            prevVMoveKey = KEY_NULL
        }

        /**
         * Left/Right movement
         */

        if (hasController) {
            if (axisX != 0f) {
                walkHorizontal(axisX < 0f, axisX.abs())
            }
        }
        // ↑F, ↓S
        if (isRightDown && !isLeftDown) {
            walkHorizontal(false, AXIS_KEYBOARD)
            prevHMoveKey = AppLoader.getConfigInt("keyright")
        } // ↓F, ↑S
        else if (isLeftDown && !isRightDown) {
            walkHorizontal(true, AXIS_KEYBOARD)
            prevHMoveKey = AppLoader.getConfigInt("keyleft")
        } // ↓F, ↓S
        /*else if (isLeftDown && isRightDown) {
               if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)) {
                   walkHorizontal(false, AXIS_KEYBOARD)
                   prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)
               } else if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)) {
                   walkHorizontal(true, AXIS_KEYBOARD)
                   prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)
               }
           }*/

        /**
         * Up/Down movement
         */
        if (isNoClip || COLLISION_TEST_MODE) {
            if (hasController) {
                if (axisY != 0f) {
                    walkVertical(axisY < 0, axisY.abs())
                }
            }
            // ↑E, ↓D
            if (isDownDown && !isUpDown) {
                walkVertical(false, AXIS_KEYBOARD)
                prevVMoveKey = AppLoader.getConfigInt("keydown")
            } // ↓E, ↑D
            else if (isUpDown && !isDownDown) {
                walkVertical(true, AXIS_KEYBOARD)
                prevVMoveKey = AppLoader.getConfigInt("keyup")
            } // ↓E, ↓D
            /*else if (isUpDown && isDownDown) {
                if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)) {
                    walkVertical(false, AXIS_KEYBOARD)
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)
                } else if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)) {
                    walkVertical(true, AXIS_KEYBOARD)
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)
                }
            }*/
        }

        /**
         * Jump control
         */
        if (isJumpDown) {
            if (!isNoClip) {
                if (airJumpingAllowed ||
                    (!airJumpingAllowed && walledBottom)) {
                    jumping = true
                }
                jump()
            }
            else {
                walkVertical(true, AXIS_KEYBOARD)
            }
        }
        else {
            jumping = false
            jumpCounter = 0
            jumpAcc = 0.0
        }

    }

    override fun keyDown(keycode: Int): Boolean {
        return false
    }



    /**
     * This code directly controls VELOCITY for walking, called walkX and walkY.
     *
     * In theory, we must add ACCELERATION to the velocity, but unfortunately it's arduous task
     * with this simulation code base.
     *
     * Reason: we have naïve friction code that is not adaptive at all and to add proper walking code to
     * this code base, ACCELERATION must be changed (in other words, we must deal with JERK) accordingly
     * to the FRICTION.
     *
     * So I'm adding walkX/Y and getting the ActorWBMovable.setNewNextHitbox to use the velocity value of
     * walkX/Y + velocity, which is stored in variable moveDelta.
     *
     * Be warned.
     *
     * @param left (even if the game is joypad controlled, you must give valid value)
     * @param absAxisVal (set AXIS_KEYBOARD if keyboard controlled)
     * @author minjaesong
     */
    private fun walkHorizontal(left: Boolean, absAxisVal: Float) {


        if (avAcceleration.isNaN()) {
            throw Error("avAccelation is NaN")
        }


        if (left && walledLeft || !left && walledRight) return


        readonly_totalX =
                if (absAxisVal == AXIS_KEYBOARD)
                    avAcceleration * applyVelo(walkCounterX) * (if (left) -1f else 1f)
                else
                    avAcceleration * applyVelo(walkCounterX) * (if (left) -1f else 1f) * absAxisVal

        if (absAxisVal != AXIS_KEYBOARD)
            controllerMoveDelta?.x?.let { controllerMoveDelta!!.x = controllerMoveDelta!!.x.plus(readonly_totalX).bipolarClamp(avSpeedCap * absAxisVal) }
        else
            controllerMoveDelta?.x?.let { controllerMoveDelta!!.x = controllerMoveDelta!!.x.plus(readonly_totalX).bipolarClamp(avSpeedCap) }

        if (walkCounterX < 1000000) {
          walkCounterX += 1
        }

        isWalkingH = true



        // Heading flag
        walkHeading = if (left) LEFT else RIGHT
    }

    /**

     * @param up (even if the game is joypad controlled, you must give valid value)
     * *
     * @param absAxisVal (set AXIS_KEYBOARD if keyboard controlled)
     */
    private fun walkVertical(up: Boolean, absAxisVal: Float) {
        if (up && walledTop || !up && walledBottom) return


        if (avAcceleration.isNaN()) {
            throw Error("avAccelation is NaN")
        }


        readonly_totalY =
                if (absAxisVal == AXIS_KEYBOARD)
                    avAcceleration * applyVelo(walkCounterY) * (if (up) -1f else 1f)
                else
                    avAcceleration * applyVelo(walkCounterY) * (if (up) -1f else 1f) * absAxisVal

        if (absAxisVal != AXIS_KEYBOARD)
            controllerMoveDelta?.y?.let { controllerMoveDelta!!.y = controllerMoveDelta!!.y.plus(readonly_totalY).bipolarClamp(avSpeedCap * absAxisVal) }
        else
            controllerMoveDelta?.y?.let { controllerMoveDelta!!.y = controllerMoveDelta!!.y.plus(readonly_totalY).bipolarClamp(avSpeedCap) }

        if (walkCounterY < 1000000) {
            walkCounterY += 1
        }


        isWalkingV = true
    }

    private fun applyAccel(x: Int): Double {
        return if (x < WALK_FRAMES_TO_MAX_ACCEL)
            Math.sin(Math.PI * x / WALK_FRAMES_TO_MAX_ACCEL)
        else 0.0
    }

    private fun applyVelo(x: Int): Double {
        return if (x < WALK_FRAMES_TO_MAX_ACCEL)
            0.5 - 0.5 * Math.cos(Math.PI * x / WALK_FRAMES_TO_MAX_ACCEL)
        else 1.0
    }

    // stops; let the friction kick in by doing nothing to the velocity here
    private fun walkHStop() {
        walkCounterX = 0
        isWalkingH = false
    }

    // stops; let the friction kick in by doing nothing to the velocity here
    private fun walkVStop() {
        walkCounterY = 0
        isWalkingV = false
    }

    private fun getJumpAcc(pwr: Double, timedJumpCharge: Double): Double {
        return pwr * timedJumpCharge * JUMP_ACCELERATION_MOD * Math.sqrt(scale) // positive value
    }

    private var oldMAX_JUMP_LENGTH = -1 // init
    private var oldJUMPPOWER = -1.0 // init
    private var oldJUMPPOWERBUFF = -1.0 // init
    private var oldScale = -1.0
    private var oldDragCoefficient = -1.0
    var jumpAirTime: Double = -1.0
        get() {
            // compare all the affecting variables
            if (oldMAX_JUMP_LENGTH == MAX_JUMP_LENGTH &&
                oldJUMPPOWER == actorValue.getAsDouble(AVKey.JUMPPOWER)!! &&
                oldJUMPPOWERBUFF == actorValue.getAsDouble(AVKey.JUMPPOWERBUFF) ?: 1.0 &&
                oldScale == scale &&
                oldDragCoefficient == dragCoefficient) {
                return field
            }
            // if variables are changed, get new value, store it and return it
            else {
                oldMAX_JUMP_LENGTH = MAX_JUMP_LENGTH
                oldJUMPPOWER = actorValue.getAsDouble(AVKey.JUMPPOWER)!!
                oldJUMPPOWERBUFF = actorValue.getAsDouble(AVKey.JUMPPOWERBUFF) ?: 1.0
                oldScale = scale
                oldDragCoefficient = dragCoefficient


                var frames = 0

                var simYPos = 0.0
                var forceVec = Vector2(0.0, 0.0)
                var jmpCtr = 0
                while (true) {
                    if (jmpCtr < MAX_JUMP_LENGTH) jmpCtr++


                    val timedJumpCharge = jumpFunc(MAX_JUMP_LENGTH, jmpCtr)
                    forceVec.y -= getJumpAcc(jumpPower, timedJumpCharge)
                    forceVec.y += getDrag(forceVec).y

                    simYPos += forceVec.y // ignoring all the fluid drag OTHER THAN THE AIR


                    if ((simYPos >= 0.0 && frames > 0) || frames >= 1000) break


                    frames++
                }


                field = frames * (1.0 / Terrarum.PHYS_TIME_FRAME)
                // fixme: looks good but return value is wrong -- 2.25 seconds? when I jump it barely goes past 1 sec


                return field
            }
        }

    private val jumpPower: Double
        get() = actorValue.getAsDouble(AVKey.JUMPPOWER)!! * (actorValue.getAsDouble(AVKey.JUMPPOWERBUFF) ?: 1.0)

    private fun jumpFunc(len: Int, counter: Int): Double {
        // linear time mode
        val init = (len + 1) / 2.0
        var timedJumpCharge = init - init / len * counter
        if (timedJumpCharge < 0) timedJumpCharge = 0.0
        return timedJumpCharge
    }

    /**
     * See ./work_files/Jump power by pressing time.gcx
     *
     * TODO linear function (play Super Mario Bros. and you'll get what I'm talking about) -- SCRATCH THAT!
     */
    private fun jump() {
        if (jumping) {// && jumpable) {
            // increment jump counter
            if (jumpCounter < MAX_JUMP_LENGTH) jumpCounter += 1

            val timedJumpCharge = jumpFunc(MAX_JUMP_LENGTH, jumpCounter)

            jumpAcc = getJumpAcc(jumpPower, timedJumpCharge)

            controllerMoveDelta?.y?.let { controllerMoveDelta!!.y -= jumpAcc } // feed negative value to the vector
            // do not think of resetting this to zero when counter hit the ceiling; that's HOW NOT
            // newtonian physics work, stupid myself :(

        }
        // not sure we need this...
        /*else if (!jumpable) {
            jumpable = true  // this is kind of like "semaphore", we toggle it now
            grounded = false // just in case...
        }*/

        // release "jump key" of AIs
        if (jumpCounter >= MAX_JUMP_LENGTH && !isGamer) {
            isJumpDown = false
            jumping = false
            jumpCounter = 0
            jumpAcc = 0.0
        }
    }

    override fun onActorValueChange(key: String, value: Any?) {
        // quickslot implementation
        if (key == AVKey.__PLAYER_QUICKSLOTSEL && value != null) {
            // ONLY FOR HAND_GRIPs!!
            val quickBarItem = inventory.getQuickslot(actorValue.getAsInt(key)!!)?.item

            if (quickBarItem != null && quickBarItem.equipPosition == GameItem.EquipPosition.HAND_GRIP) {
                equipItem(quickBarItem)
            }

            // force update inventory UI
            try {
                ((Terrarum.ingame!! as Ingame).uiInventoryPlayer as UIInventoryFull).rebuildList()
            }
            catch (LateInitMyArse: kotlin.UninitializedPropertyAccessException) { }
        }
    }


    fun Float.abs() = FastMath.abs(this)

    private fun updateSprite(delta: Float) {
        sprite?.update(delta)
        spriteGlow?.update(delta)

        if (walledBottom && controllerMoveDelta?.x != 0.0) {
            //switch row
            sprite?.switchRow(SPRITE_ROW_WALK)
            spriteGlow?.switchRow(SPRITE_ROW_WALK)

            // set anim frame delay
            // 4f of the divider is a magic number, empirically decided
            if (this is HasAssembledSprite) {
                sprite?.delays?.set(SPRITE_ROW_WALK, scale.sqrt().toFloat() / (4f * (controllerMoveDelta?.x ?: 0.0001).abs().toFloat())) // FIXME empirical value
                spriteGlow?.delays?.set(SPRITE_ROW_WALK, scale.sqrt().toFloat() / (4f * (controllerMoveDelta?.x ?: 0.0001).abs().toFloat())) // FIXME empirical value

            }

            // flipping the sprite
            if (walkHeading == LEFT) {
                sprite?.flip(true, false)
                spriteGlow?.flip(true, false)
            }
            else {
                sprite?.flip(false, false)
                spriteGlow?.flip(false, false)
            }
        }
        else {
            sprite?.switchRow(SPRITE_ROW_IDLE)
            spriteGlow?.switchRow(SPRITE_ROW_IDLE)
        }
    }
}