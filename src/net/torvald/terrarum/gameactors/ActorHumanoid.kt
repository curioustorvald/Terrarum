package net.torvald.terrarum.gameactors

import com.jme3.math.FastMath
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.terrarum.gamecontroller.EnumKeyFunc
import net.torvald.terrarum.gamecontroller.KeyMap
import net.torvald.terrarum.gameitem.EquipPosition
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.realestate.RealEstateUtility
import org.dyn4j.geometry.Vector2
import org.lwjgl.input.Controller
import org.lwjgl.input.Controllers
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input
import java.util.*

/**
 * Humanoid actor class to provide same controlling function (such as work, jump)
 *
 * Created by minjaesong on 16-10-24.
 */
open class ActorHumanoid(birth: GameDate, death: GameDate? = null)
: HistoricalFigure(birth, death), Controllable, Pocketed, Factionable, Luminous, LandHolder {

    /** Must be set by PlayerFactory */
    override var inventory: ActorInventory = ActorInventory()

    override val itemEquipped = Array<InventoryItem?>(EquipPosition.INDEX_MAX + 1, { null })

    /** Must be set by PlayerFactory */
    override var faction: HashSet<Faction> = HashSet()
    /**
     * Absolute tile index. index(x, y) = y * map.width + x
     * The arraylist will be saved in JSON format with GSON.
     */
    override var houseDesignation: ArrayList<Long>? = ArrayList()

    override fun addHouseTile(x: Int, y: Int) {
        if (houseDesignation != null) houseDesignation!!.add(RealEstateUtility.getAbsoluteTileNumber(x, y))
    }

    override fun removeHouseTile(x: Int, y: Int) {
        if (houseDesignation != null) houseDesignation!!.remove(RealEstateUtility.getAbsoluteTileNumber(x, y))
    }

    override fun clearHouseDesignation() {
        if (houseDesignation != null) houseDesignation!!.clear()
    }

    /**
     * Recommended implementation:
     *
    override var luminosity: Int
    get() = actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
    set(value) {
    actorValue[AVKey.LUMINOSITY] = value
    }
     */
    override var luminosity: Int
        get() = actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
        set(value) {
            actorValue[AVKey.LUMINOSITY] = value
        }

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    override val lightBoxList: List<Hitbox>
        get() = arrayOf(Hitbox(0.0, 0.0, hitbox.width, hitbox.height)).toList() // use getter; dimension of the player may change by time.

    @Transient val BASE_DENSITY = 980.0

    companion object {
        @Transient internal const val ACCEL_MULT_IN_FLIGHT: Double = 0.21
        @Transient internal const val WALK_ACCEL_BASE: Double = 0.67

        @Transient const val BASE_HEIGHT = 40
    }

    ////////////////////////////////
    // MOVEMENT RELATED FUNCTIONS //
    ////////////////////////////////

    var gamepad: Controller? = null
    var axisX = 0f
    var axisY = 0f
    var axisRX = 0f
    var axisRY = 0f

    /** empirical value. */
    @Transient private val JUMP_ACCELERATION_MOD = 170.0 / 10000.0 //linear mode
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
    @Transient private val MAX_JUMP_LENGTH = 17 // use 17; in internal frames

    private var readonly_totalX = 0.0
    private var readonly_totalY = 0.0

    internal var jumping = false

    internal var walkHeading: Int = 0

    @Transient private var prevHMoveKey = KEY_NULL
    @Transient private var prevVMoveKey = KEY_NULL

    internal var noClip = false

    @Transient private val AXIS_POSMAX = 1.0f
    @Transient private val GAMEPAD_JUMP = 7

    protected var isUpDown = false
    protected var isDownDown = false
    protected var isLeftDown = false
    protected var isRightDown = false
    protected var isJumpDown = false
    protected val isGamer: Boolean
        get() = this is Player // FIXME true iff composed by PlayableActorDelegate


    private val nullItem = object : InventoryItem() {
        override val id: Int = 0
        override val equipPosition: Int = EquipPosition.NULL
        override var mass: Double = 0.0
        override var scale: Double = 1.0
    }

    override fun update(gc: GameContainer, delta: Int) {
        super.update(gc, delta)

        // don't put this into keyPressed; execution order is important!
        updateGamerControlBox(gc.input)

        updateMovementControl()
        updateSprite(delta)

        if (noClip) {
            grounded = true
        }

        // reset control box of AI
        if (!isGamer) {
            isUpDown = false
            isDownDown = false
            isLeftDown = false
            isRightDown = false
            isJumpDown = false
        }

        // update inventory items
        inventory.forEach { item, amount ->
            if (!itemEquipped.contains(item)) { // unequipped
                item.effectWhileInPocket(gc, delta)
            }
            else { // equipped
                item.effectWhenEquipped(gc, delta)
            }
        }
    }

    fun unequipItem(item: InventoryItem) {
        for (i in 0..itemEquipped.size - 1) {
            val it = itemEquipped[i]
            if (item == it) {
                it.effectWhenUnEquipped(gameContainer, updateDelta)
                itemEquipped[i] = null // remove from the array by nulling it
                break
            }
        }
    }

    fun equipItem(item: InventoryItem) {
        if (item.equipPosition >= 0) {
            itemEquipped[item.equipPosition] = item
        }
    }

    private fun updateGamerControlBox(input: Input) {
        if (isGamer) {
            isUpDown = isFuncDown(input, EnumKeyFunc.MOVE_UP)
            isLeftDown = isFuncDown(input, EnumKeyFunc.MOVE_LEFT)
            isDownDown = isFuncDown(input, EnumKeyFunc.MOVE_DOWN)
            isRightDown = isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)
            isJumpDown = isFuncDown(input, EnumKeyFunc.JUMP)
        }
    }

    private fun updateMovementControl() {
        if (!noClip) {
            if (grounded) {
                actorValue[AVKey.ACCELMULT] = 1.0
            } else {
                actorValue[AVKey.ACCELMULT] = ACCEL_MULT_IN_FLIGHT
            }
        } else {
            actorValue[AVKey.ACCELMULT] = 1.0
        }
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
        if (isGamer && Terrarum.hasController) {
            gamepad = Controllers.getController(0)
            axisX = gamepad!!.getAxisValue(0)
            axisY = gamepad!!.getAxisValue(1)
            axisRX = gamepad!!.getAxisValue(2)
            axisRY = gamepad!!.getAxisValue(3)

            // deadzonning
            if (Math.abs(axisX) < Terrarum.CONTROLLER_DEADZONE) axisX = 0f
            if (Math.abs(axisY) < Terrarum.CONTROLLER_DEADZONE) axisY = 0f
            if (Math.abs(axisRX) < Terrarum.CONTROLLER_DEADZONE) axisRX = 0f
            if (Math.abs(axisRY) < Terrarum.CONTROLLER_DEADZONE) axisRY = 0f
        }

        ///////////////////
        // MOUSE CONTROL //
        ///////////////////
        // PRIMARY/SECONDARY IS FIXED TO LEFT/RIGHT BUTTON //

        /**
         * Primary Use
         */
        // Left mouse
        if (isGamer && input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON)) {
            (itemEquipped[EquipPosition.HAND_GRIP] ?: nullItem).primaryUse(gc, delta)
        }

        // Right mouse
        if (isGamer && input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)) {
            (itemEquipped[EquipPosition.HAND_GRIP] ?: nullItem).secondaryUse(gc, delta)
        }

        /**
         * L-R stop
         */
        if (isGamer && Terrarum.hasController && !isWalkingH) {
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
        if (isGamer && Terrarum.hasController) {
            if (axisY == 0f) {
                walkVStop()
            }
        }
        // ↑E
        // ↑D
        if (isNoClip() && !isUpDown && !isDownDown) {
            walkVStop()
            prevVMoveKey = KEY_NULL
        }

        /**
         * Left/Right movement
         */

        if (isGamer && Terrarum.hasController) {
            if (axisX != 0f) {
                walkHorizontal(axisX < 0f, axisX.abs())
            }
        }
        // ↑F, ↓S
        if (isRightDown && !isLeftDown) {
            walkHorizontal(false, AXIS_POSMAX)
            prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)
        } // ↓F, ↑S
        else if (isLeftDown && !isRightDown) {
            walkHorizontal(true, AXIS_POSMAX)
            prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)
        } // ↓F, ↓S
        /*else if (isLeftDown && isRightDown) {
               if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)) {
                   walkHorizontal(false, AXIS_POSMAX)
                   prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)
               } else if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)) {
                   walkHorizontal(true, AXIS_POSMAX)
                   prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)
               }
           }*/

        /**
         * Up/Down movement
         */
        if (noClip) {
            if (isGamer && Terrarum.hasController) {
                if (axisY != 0f) {
                    walkVertical(axisY < 0, axisY.abs())
                }
            }
            // ↑E, ↓D
            if (isDownDown && !isUpDown) {
                walkVertical(false, AXIS_POSMAX)
                prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)
            } // ↓E, ↑D
            else if (isUpDown && !isDownDown) {
                walkVertical(true, AXIS_POSMAX)
                prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)
            } // ↓E, ↓D
            /*else if (isUpDown && isDownDown) {
                if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)) {
                    walkVertical(false, AXIS_POSMAX)
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)
                } else if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)) {
                    walkVertical(true, AXIS_POSMAX)
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)
                }
            }*/
        }

        /**
         * Jump control
         */
        if (isJumpDown || isGamer && Terrarum.hasController && gamepad!!.isButtonPressed(GAMEPAD_JUMP)) {
            if (!noClip) {
                if (grounded) {
                    jumping = true
                }
                jump()
            } else {
                walkVertical(true, AXIS_POSMAX)
            }
        } else {
            jumping = false
            jumpCounter = 0
            jumpAcc = 0.0
        }

    }

    override fun keyPressed(key: Int, c: Char) {

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
     * So I'm adding walkX/Y and getting the ActorWithBody.setNewNextHitbox to use the velocity value of
     * walkX/Y + velocity, which is stored in variable moveDelta.
     *
     * Be warned.
     *
     * @param left (even if the game is joypad controlled, you must give valid value)
     * @param absAxisVal (set AXIS_POSMAX if keyboard controlled)
     * @author minjaesong
     */
    private fun walkHorizontal(left: Boolean, absAxisVal: Float) {
        if ((!walledLeft && left) || (!walledRight && !left)) {
            readonly_totalX =
                    absMax( // keyboard
                            actorValue.getAsDouble(AVKey.ACCEL)!! *
                            actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                            Math.sqrt(scale) *
                            applyVelo(walkCounterX) *
                            (if (left) -1f else 1f)
                            , // gamepad
                            actorValue.getAsDouble(AVKey.ACCEL)!! *
                            actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                            Math.sqrt(scale) *
                            (if (left) -1f else 1f) * absAxisVal
                            // do not add applyVelo(walkCounterY) here, as it prevents player from moving with gamepad
                    )

            //applyForce(Vector2(readonly_totalX, 0.0))
            walkX += readonly_totalX
            walkX = absClamp(walkX, actorValue.getAsDouble(AVKey.SPEED)!! * actorValue.getAsDouble(AVKey.SPEEDMULT)!!)

            walkCounterX += 1

            isWalkingH = true
        }

        // Heading flag
        if (left)
            walkHeading = LEFT
        else
            walkHeading = RIGHT
    }

    /**

     * @param up (even if the game is joypad controlled, you must give valid value)
     * *
     * @param absAxisVal (set AXIS_POSMAX if keyboard controlled)
     */
    private fun walkVertical(up: Boolean, absAxisVal: Float) {
        readonly_totalY =
                absMax( // keyboard
                        actorValue.getAsDouble(AVKey.ACCEL)!! *
                        actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                        Math.sqrt(scale) *
                        applyVelo(walkCounterY) *
                        (if (up) -1f else 1f)
                        , // gamepad
                        actorValue.getAsDouble(AVKey.ACCEL)!! *
                        actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                        Math.sqrt(scale) *
                        (if (up) -1f else 1f) * absAxisVal
                )

        walkY += readonly_totalY
        walkY = absClamp(walkY, actorValue.getAsDouble(AVKey.SPEED)!! * actorValue.getAsDouble(AVKey.SPEEDMULT)!!)

        walkCounterY += 1

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

    /**
     * See ./work_files/Jump power by pressing time.gcx
     *
     * TODO linear function (play Super Mario Bros. and you'll get what I'm talking about)
     */
    private fun jump() {

        val len = MAX_JUMP_LENGTH.toFloat()
        val pwr = actorValue.getAsDouble(AVKey.JUMPPOWER)!! * (actorValue.getAsDouble(AVKey.JUMPPOWERMULT) ?: 1.0)
        val jumpLinearThre = 0.08

        fun jumpFunc(x: Int): Double {
            if (x >= len) return 0.0
            val ret = pwr - 0.02 * x

            if (ret < jumpLinearThre) return jumpLinearThre
            else return ret
        }

        if (jumping) {
            if (isGamer) { // jump power increases as the gamer hits JUMP longer time
                // increment jump counter
                if (jumpCounter < len) jumpCounter += 1

                // linear time mode
                val init = (len + 1) / 2.0
                var timedJumpCharge = init - init / len * jumpCounter
                if (timedJumpCharge < 0) timedJumpCharge = 0.0

                // one that uses jumpFunc(x)
                //val timedJumpCharge = jumpFunc(jumpCounter)

                jumpAcc = -pwr * timedJumpCharge * JUMP_ACCELERATION_MOD * Math.sqrt(scale) // positive value

                applyForce(Vector2(0.0, jumpAcc))
            }
            else { // no such minute control for AIs
                //veloY -= pwr * Math.sqrt(scale)
                jumpAcc = -pwr * Math.sqrt(scale)
            }
        }
    }

    private fun isFuncDown(input: Input, fn: EnumKeyFunc): Boolean {
        return input.isKeyDown(KeyMap.getKeyCode(fn))
    }

    private fun absClamp(i: Double, ceil: Double): Double {
        if (i > 0)
            return if (i > ceil) ceil else i
        else if (i < 0)
            return if (-i > ceil) -ceil else i
        else
            return 0.0
    }

    fun isNoClip(): Boolean {
        return noClip
    }

    fun setNoClip(b: Boolean) {
        noClip = b
    }

    fun Float.abs() = FastMath.abs(this)

    private fun updateSprite(delta: Int) {
        sprite!!.update(delta)
        if (spriteGlow != null) {
            spriteGlow!!.update(delta)
        }

        if (grounded) {
            if (walkHeading == LEFT) {
                sprite!!.flip(true, false)
                if (spriteGlow != null) {
                    spriteGlow!!.flip(true, false)
                }
            } else {
                sprite!!.flip(false, false)
                if (spriteGlow != null) {
                    spriteGlow!!.flip(false, false)
                }
            }
        }
    }
}