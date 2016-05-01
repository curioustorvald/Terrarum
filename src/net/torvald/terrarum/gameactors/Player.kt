package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.terrarum.gamecontroller.EnumKeyFunc
import net.torvald.terrarum.gamecontroller.KeyMap
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.spriteanimation.SpriteAnimation
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
    @Transient private val JUMP_ACCELERATION_MOD = 170.0 / 10000.0 //linear mode
    @Transient private val WALK_FRAMES_TO_MAX_ACCEL = 6

    @Transient private val LEFT = 1
    @Transient private val RIGHT = 2

    @Transient private val KEY_NULL = -1


    var vehicleRiding: Controllable? = null

    internal var jumpCounter = 0
    internal var walkPowerCounter = 0
    @Transient private val MAX_JUMP_LENGTH = 17 // use 17; in internal frames

    private var readonly_totalX = 0.0
    private var readonly_totalY = 0.0

    internal var jumping = false

    internal var walkHeading: Int = 0

    @Transient private var prevHMoveKey = KEY_NULL
    @Transient private var prevVMoveKey = KEY_NULL

    internal var noClip = false

    @Transient private val AXIS_POSMAX = 1.0f
    @Transient private val GAMEPAD_JUMP = 5

    @Transient private val TSIZE = MapDrawer.TILE_SIZE

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
        @Transient internal const val ACCEL_MULT_IN_FLIGHT: Double = 0.31
        @Transient internal const val WALK_ACCEL_BASE: Double = 0.67

        @Transient const val PLAYER_REF_ID: Int = 0x51621D
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
        readonly_totalX = veloX +
                          actorValue.getAsDouble(AVKey.ACCEL)!! *
                          actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                          Math.sqrt(scale) *
                          applyAccelRealism(walkPowerCounter) *
                          (if (left) -1 else 1).toFloat() *
                          absAxisVal

        veloX = readonly_totalX

        if (walkPowerCounter < WALK_FRAMES_TO_MAX_ACCEL) {
            walkPowerCounter += 1
        }

        // Clamp veloX
        veloX = absClamp(veloX, actorValue.getAsDouble(AVKey.SPEED)!!
                        * actorValue.getAsDouble(AVKey.SPEEDMULT)!!
                        * Math.sqrt(scale))

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
        readonly_totalY = veloY +
                actorValue.getAsDouble(AVKey.ACCEL)!! *
                        actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                        Math.sqrt(scale) *
                        applyAccelRealism(walkPowerCounter) *
                        (if (up) -1 else 1).toFloat() *
                        absAxisVal

        veloY = readonly_totalY

        if (walkPowerCounter < WALK_FRAMES_TO_MAX_ACCEL) {
            walkPowerCounter += 1
        }

        // Clamp veloX
        veloY = absClamp(veloY, actorValue.getAsDouble(AVKey.SPEED)!!
                        * actorValue.getAsDouble(AVKey.SPEEDMULT)!!
                        * Math.sqrt(scale))
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
    private fun applyAccelRealism(x: Int): Double {
        return 0.5 + 0.5 * -Math.cos(10 * x / (WALK_FRAMES_TO_MAX_ACCEL * Math.PI))
    }

    // stops; let the friction kick in by doing nothing to the velocity here
    private fun walkHStop() {
        /*if (veloX > 0) {
            veloX -= actorValue.getAsDouble(AVKey.ACCEL)!! *
                    actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                    Math.sqrt(scale)

            // compensate overshoot
            if (veloX < 0) veloX = 0f
        } else if (veloX < 0) {
            veloX += actorValue.getAsDouble(AVKey.ACCEL)!! *
                    actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                    Math.sqrt(scale)

            // compensate overshoot
            if (veloX > 0) veloX = 0f
        } else {
            veloX = 0f
        }*/

        //veloX = 0f

        walkPowerCounter = 0
    }

    // stops; let the friction kick in by doing nothing to the velocity here
    private fun walkVStop() {
        /*if (veloY > 0) {
            veloY -= WALK_STOP_ACCEL *
                    actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                    Math.sqrt(scale)

            // compensate overshoot
            if (veloY < 0)
                veloY = 0f
        } else if (veloY < 0) {
            veloY += WALK_STOP_ACCEL *
                    actorValue.getAsDouble(AVKey.ACCELMULT)!! *
                    Math.sqrt(scale)

            // compensate overshoot
            if (veloY > 0) veloY = 0f
        } else {
            veloY = 0f
        }*/

        ///veloY = 0f

        walkPowerCounter = 0
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
            } // ↓F, ↑S
            else if (isFuncDown(input, EnumKeyFunc.MOVE_LEFT) && !isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
                walkHorizontal(true, AXIS_POSMAX)
                prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)
            } // ↓F, ↓S
            else if (isFuncDown(input, EnumKeyFunc.MOVE_LEFT) && isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
                if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)) {
                    walkHorizontal(false, AXIS_POSMAX)
                    prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)
                } else if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)) {
                    walkHorizontal(true, AXIS_POSMAX)
                    prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)
                }
            }
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
                // ↑E, ↓D
                if (isFuncDown(input, EnumKeyFunc.MOVE_DOWN) && !isFuncDown(input, EnumKeyFunc.MOVE_UP)) {
                    walkVertical(false, AXIS_POSMAX)
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)
                } // ↓E, ↑D
                else if (isFuncDown(input, EnumKeyFunc.MOVE_UP) && !isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                    walkVertical(true, AXIS_POSMAX)
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)
                } // ↓E, ↓D
                else if (isFuncDown(input, EnumKeyFunc.MOVE_UP) && isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                    if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)) {
                        walkVertical(false, AXIS_POSMAX)
                        prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)
                    } else if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)) {
                        walkVertical(true, AXIS_POSMAX)
                        prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)
                    }
                }
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
            val pwr = actorValue.getAsDouble(AVKey.JUMPPOWER)!! * (actorValue.getAsDouble(AVKey.JUMPPOWERMULT) ?: 1.0)

            // increment jump counter
            if (jumpCounter < len) jumpCounter += 1

            // linear time mode
            val init = (len + 1) / 2.0
            var timedJumpCharge = init - init / len * jumpCounter
            if (timedJumpCharge < 0) timedJumpCharge = 0.0

            val jumpAcc = pwr * timedJumpCharge * JUMP_ACCELERATION_MOD * Math.sqrt(scale)

            veloY -= jumpAcc
        }

        // for mob ai:
        //super.setVeloY(veloY
        //        -
        //        pwr * Math.sqrt(scale)
        //);
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