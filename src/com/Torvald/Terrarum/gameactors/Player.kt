package com.torvald.terrarum.gameactors

import com.torvald.terrarum.gameactors.faction.Faction
import com.torvald.terrarum.gamecontroller.EnumKeyFunc
import com.torvald.terrarum.gamecontroller.KeyMap
import com.torvald.terrarum.mapdrawer.MapDrawer
import com.torvald.terrarum.Terrarum
import com.torvald.spriteanimation.SpriteAnimation
import com.jme3.math.FastMath
import org.lwjgl.input.Controller
import org.lwjgl.input.Controllers
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input
import org.newdawn.slick.SlickException
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */

class Player : ActorWithBody, Controllable, Pocketed, Factionable, Luminous, LandHolder {

    /**
     * empirical value.
     */
    // private transient final float JUMP_ACCELERATION_MOD = ???f / 10000f; //quadratic mode
    @Transient private val JUMP_ACCELERATION_MOD = 170f / 10000f //linear mode
    @Transient private val WALK_FRAMES_TO_MAX_ACCEL = 6

    @Transient private val LEFT = 1
    @Transient private val RIGHT = 2

    @Transient private val KEY_NULL = -1


    var vehicleRiding: Controllable? = null

    internal var jumpCounter = 0
    internal var walkPowerCounter = 0
    @Transient private val MAX_JUMP_LENGTH = 17 // use 17; in internal frames

    private var readonly_totalX = 0f
    private var readonly_totalY = 0f

    internal var jumping = false

    internal var walkHeading: Int = 0

    @Transient private var prevHMoveKey = KEY_NULL
    @Transient private var prevVMoveKey = KEY_NULL

    internal var noClip = false

    @Transient private val AXIS_POSMAX = 1.0f
    @Transient private val GAMEPAD_JUMP = 5

    @Transient private val TSIZE = MapDrawer.TILE_SIZE

    private val factionSet = HashSet<Faction>()

    @Transient private val BASE_DENSITY = 1020

    /** Must be set by PlayerFactory */
    override var inventory: ActorInventory = ActorInventory()

    /** Must be set by PlayerFactory */
    override var faction: HashSet<Faction> = HashSet()

    override var houseDesignation: ArrayList<Int>? = null

    override var luminosity: Int
        get() = actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
        set(value) {
            actorValue[AVKey.LUMINOSITY] = value
        }

    companion object {
        @Transient internal const val ACCEL_MULT_IN_FLIGHT = 0.48f
        @Transient internal const val WALK_STOP_ACCEL = 0.32f
        @Transient internal const val WALK_ACCEL_BASE = 0.32f

        @Transient const val PLAYER_REF_ID: Long = 0x51621D
        @Transient const val BASE_HEIGHT = 40
    }

    /**
     * Creates new Player instance with empty elements (sprites, actorvalue, etc.).

     * **Use PlayerFactory to build player!**

     * @throws SlickException
     */
    @Throws(SlickException::class)
    constructor() : super() {
        isVisible = true
        referenceID = PLAYER_REF_ID
        super.setDensity(BASE_DENSITY)
    }

    override fun update(gc: GameContainer, delta_t: Int) {
        if (vehicleRiding is Player)
            throw RuntimeException("Attempted to 'ride' " + "player object.")

        super.update(gc, delta_t)

        updateSprite(delta_t)

        updateMovementControl()

        if (noClip) {
            grounded = true
        }

    }

    /**

     * @param left (even if the game is joypad controlled, you must give valid value)
     * *
     * @param absAxisVal (set AXIS_POSMAX if keyboard controlled)
     */
    private fun walkHorizontal(left: Boolean, absAxisVal: Float) {
        //if ((!super.isWalledLeft() && left) || (!super.isWalledRight() && !left)) {
        readonly_totalX = veloX +
                actorValue.getAsFloat(AVKey.ACCEL)!! *
                        actorValue.getAsFloat(AVKey.ACCELMULT)!! *
                        FastMath.sqrt(scale) *
                        applyAccelRealism(walkPowerCounter) *
                        (if (left) -1 else 1).toFloat() *
                        absAxisVal

        veloX = readonly_totalX

        if (walkPowerCounter < WALK_FRAMES_TO_MAX_ACCEL) {
            walkPowerCounter += 1
        }

        // Clamp veloX
        veloX = absClamp(veloX, actorValue.getAsFloat(AVKey.SPEED)!!
                        * actorValue.getAsFloat(AVKey.SPEEDMULT)!!
                        * FastMath.sqrt(scale))

        // Heading flag
        if (left)
            walkHeading = LEFT
        else
            walkHeading = RIGHT
        //}
    }

    /**

     * @param up (even if the game is joypad controlled, you must give valid value)
     * *
     * @param absAxisVal (set AXIS_POSMAX if keyboard controlled)
     */
    private fun walkVertical(up: Boolean, absAxisVal: Float) {
        readonly_totalY = veloY +
                actorValue.getAsFloat(AVKey.ACCEL)!! *
                        actorValue.getAsFloat(AVKey.ACCELMULT)!! *
                        FastMath.sqrt(scale) *
                        applyAccelRealism(walkPowerCounter) *
                        (if (up) -1 else 1).toFloat() *
                        absAxisVal

        veloY = readonly_totalY

        if (walkPowerCounter < WALK_FRAMES_TO_MAX_ACCEL) {
            walkPowerCounter += 1
        }

        // Clamp veloX
        veloY = absClamp(veloY, actorValue.getAsFloat(AVKey.SPEED)!!
                        * actorValue.getAsFloat(AVKey.SPEEDMULT)!!
                        * FastMath.sqrt(scale))
    }

    /**
     * For realistic accelerating while walking.

     * Naïve 'veloX += 3' is actually like:

     * a
     * |      ------------
     * |
     * |
     * 0+------············  t

     * which is unrealistic, so this method tries to introduce some realism by doing:

     * a
     * |           ------------
     * |        ---
     * |       -
     * |    ---
     * 0+----··················· t


     * @param x
     */
    private fun applyAccelRealism(x: Int): Float {
        return 0.5f + 0.5f * -FastMath.cos(10 * x / (WALK_FRAMES_TO_MAX_ACCEL * FastMath.PI))
    }

    private fun walkHStop() {
        if (veloX > 0) {
            veloX -= actorValue.getAsFloat(AVKey.ACCEL)!! *
                    actorValue.getAsFloat(AVKey.ACCELMULT)!! *
                    FastMath.sqrt(scale)

            // compensate overshoot
            if (veloX < 0) veloX = 0f
        } else if (veloX < 0) {
            veloX += actorValue.getAsFloat(AVKey.ACCEL)!! *
                    actorValue.getAsFloat(AVKey.ACCELMULT)!! *
                    FastMath.sqrt(scale)

            // compensate overshoot
            if (veloX > 0) veloX = 0f
        } else {
            veloX = 0f
        }

        walkPowerCounter = 0
    }

    private fun walkVStop() {
        if (veloY > 0) {
            veloY -= WALK_STOP_ACCEL *
                    actorValue.getAsFloat(AVKey.ACCELMULT)!! *
                    FastMath.sqrt(scale)

            // compensate overshoot
            if (veloY < 0)
                veloY = 0f
        } else if (veloY < 0) {
            veloY += WALK_STOP_ACCEL *
                    actorValue.getAsFloat(AVKey.ACCELMULT)!! *
                    FastMath.sqrt(scale)

            // compensate overshoot
            if (veloY > 0) veloY = 0f
        } else {
            veloY = 0f
        }

        walkPowerCounter = 0
    }

    private fun updateMovementControl() {
        if (!noClip) {
            if (grounded) {
                actorValue.set(AVKey.ACCELMULT, 1f)
            } else {
                actorValue.set(AVKey.ACCELMULT, ACCEL_MULT_IN_FLIGHT)
            }
        } else {
            actorValue.set(AVKey.ACCELMULT, 1f)
        }
    }

    override fun processInput(input: Input) {
        var gamepad: Controller? = null
        var axisX = 0f
        var axisY = 0f
        var axisRX = 0f
        var axisRY = 0f
        if (Terrarum.hasController) {
            gamepad = Controllers.getController(0)
            axisX = gamepad!!.getAxisValue(0)
            axisY = gamepad.getAxisValue(1)
            axisRX = gamepad.getAxisValue(2)
            axisRY = gamepad.getAxisValue(3)

            if (Math.abs(axisX) < Terrarum.CONTROLLER_DEADZONE) axisX = 0f
            if (Math.abs(axisY) < Terrarum.CONTROLLER_DEADZONE) axisY = 0f
            if (Math.abs(axisRX) < Terrarum.CONTROLLER_DEADZONE) axisRX = 0f
            if (Math.abs(axisRY) < Terrarum.CONTROLLER_DEADZONE) axisRY = 0f
        }

        /**
         * L-R stop
         */
        if (Terrarum.hasController) {
            if (axisX == 0f) {
                walkHStop()
            }
        } else {
            // ↑F, ↑S
            if (!isFuncDown(input, EnumKeyFunc.MOVE_LEFT) && !isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
                walkHStop()
                prevHMoveKey = KEY_NULL
            }
        }
        /**
         * U-D stop
         */
        if (Terrarum.hasController) {
            if (axisY == 0f) {
                walkVStop()
            }
        } else {
            // ↑E
            // ↑D
            if (isNoClip()
                    && !isFuncDown(input, EnumKeyFunc.MOVE_UP)
                    && !isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                walkVStop()
                prevVMoveKey = KEY_NULL
            }
        }

        /**
         * Left/Right movement
         */

        if (Terrarum.hasController) {
            if (axisX != 0f) {
                walkHorizontal(axisX < 0, AXIS_POSMAX)
            }
        } else {
            // ↑F, ↓S
            if (isFuncDown(input, EnumKeyFunc.MOVE_RIGHT) && !isFuncDown(input, EnumKeyFunc.MOVE_LEFT)) {
                walkHorizontal(false, AXIS_POSMAX)
                prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)
            } else if (isFuncDown(input, EnumKeyFunc.MOVE_LEFT) && !isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
                walkHorizontal(true, AXIS_POSMAX)
                prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)
            } else if (isFuncDown(input, EnumKeyFunc.MOVE_LEFT) && isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
                if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)) {
                    walkHorizontal(false, AXIS_POSMAX)
                    prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)
                } else if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)) {
                    walkHorizontal(true, AXIS_POSMAX)
                    prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)
                }
            }// ↓F, ↓S
            // ↓F, ↑S
        }

        /**
         * Up/Down movement
         */
        if (noClip) {
            if (Terrarum.hasController) {
                if (axisY != 0f) {
                    walkVertical(axisY > 0, AXIS_POSMAX)
                }
            } else {
                // ↑E
                // ↓D
                if (isFuncDown(input, EnumKeyFunc.MOVE_DOWN) && !isFuncDown(input, EnumKeyFunc.MOVE_UP)) {
                    walkVertical(false, AXIS_POSMAX)
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)
                } else if (isFuncDown(input, EnumKeyFunc.MOVE_UP) && !isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                    walkVertical(true, AXIS_POSMAX)
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)
                } else if (isFuncDown(input, EnumKeyFunc.MOVE_UP) && isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                    if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)) {
                        walkVertical(false, AXIS_POSMAX)
                        prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)
                    } else if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)) {
                        walkVertical(true, AXIS_POSMAX)
                        prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)
                    }
                }// ↓E
                // ↓D
                // ↓E
                // ↑D
            }
        }

        /**
         * Jump control
         */
        if (isFuncDown(input, EnumKeyFunc.JUMP) || Terrarum.hasController && gamepad!!.isButtonPressed(GAMEPAD_JUMP)) {
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
        }

    }

    override fun keyPressed(key: Int, c: Char) {

    }

    /**
     * See ./work_files/Jump\ power\ by\ pressing\ time.gcx
     */
    private fun jump() {
        if (jumping) {
            val len = MAX_JUMP_LENGTH.toFloat()
            val pwr = actorValue.getAsFloat(AVKey.JUMPPOWER)!! * (actorValue.getAsFloat(AVKey.JUMPPOWERMULT) ?: 1f)

            // increment jump counter
            if (jumpCounter < len) jumpCounter += 1

            // linear time mode
            val init = (len + 1) / 2f
            var timedJumpCharge = init - init / len * jumpCounter
            if (timedJumpCharge < 0) timedJumpCharge = 0f

            val jumpAcc = pwr * timedJumpCharge * JUMP_ACCELERATION_MOD * FastMath.sqrt(scale)

            veloY -= jumpAcc

            // try concave mode?
        }

        // for mob ai:
        //super.setVeloY(veloY
        //        -
        //        pwr * FastMath.sqrt(scale)
        //);
    }

    private fun jumpFuncLin(pwr: Float, len: Float): Float {
        return -(pwr / len) * jumpCounter
    }

    private fun jumpFuncSqu(pwr: Float, len: Float): Float {
        return pwr / (len * len) * (jumpCounter - len * jumpCounter - len) - pwr
    }

    private fun jumpFuncExp(pwr: Float, len: Float): Float {
        val a = FastMath.pow(pwr + 1, 1 / len)
        return -FastMath.pow(a, len) + 1
    }

    private fun isFuncDown(input: Input, fn: EnumKeyFunc): Boolean {
        return input.isKeyDown(KeyMap.getKeyCode(fn))
    }

    private fun absClamp(i: Float, ceil: Float): Float {
        if (i > 0)
            return if (i > ceil) ceil else i
        else if (i < 0)
            return if (-i > ceil) -ceil else i
        else
            return 0f
    }

    private fun updateSprite(delta_t: Int) {
        sprite!!.update(delta_t)
        if (spriteGlow != null) {
            spriteGlow!!.update(delta_t)
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

    fun isNoClip(): Boolean {
        return noClip
    }

    fun setNoClip(b: Boolean) {
        noClip = b
    }

    override fun addHouseTile(x: Int, y: Int) {
        throw UnsupportedOperationException()
    }

    override fun removeHouseTile(x: Int, y: Int) {
        throw UnsupportedOperationException()
    }

    override fun clearHouseDesignation() {
        throw UnsupportedOperationException()
    }

}