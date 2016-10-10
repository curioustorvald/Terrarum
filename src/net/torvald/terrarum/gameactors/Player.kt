package net.torvald.terrarum.gameactors

import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.terrarum.gamecontroller.EnumKeyFunc
import net.torvald.terrarum.gamecontroller.KeyMap
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.UIQuickBar
import org.dyn4j.geometry.Vector2
import org.lwjgl.input.Controller
import org.lwjgl.input.Controllers
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input
import org.newdawn.slick.SlickException
import java.util.*

/**
 * Created by minjaesong on 16-03-14.
 */

class Player(born: GameDate) : HistoricalFigure(born), Controllable, Pocketed, Factionable, Luminous, LandHolder {

    /**
     * empirical value.
     */
    @Transient private val JUMP_ACCELERATION_MOD = 170.0 / 10000.0 //linear mode
    @Transient private val WALK_FRAMES_TO_MAX_ACCEL = 6

    @Transient private val LEFT = 1
    @Transient private val RIGHT = 2

    @Transient private val KEY_NULL = -1


    var vehicleRiding: Controllable? = null

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

    @Transient private val TSIZE = MapDrawer.TILE_SIZE

    @Transient private val BASE_DENSITY = 980.0

    /** Must be set by PlayerFactory */
    override var inventory: ActorInventory = ActorInventory()
    internal val quickBarRegistration = IntArray(UIQuickBar.SLOT_COUNT, { -1 })

    /** Must be set by PlayerFactory */
    override var faction: HashSet<Faction> = HashSet()

    override var houseDesignation: ArrayList<Long>? = null

    override var luminosity: Int
        get() = actorValue.getAsInt(AVKey.LUMINOSITY) ?: 0
        set(value) {
            actorValue[AVKey.LUMINOSITY] = value
        }
    override val lightBoxList: List<Hitbox>
        get() = arrayOf(Hitbox(0.0, 0.0, hitbox.width, hitbox.height)).toList() // use getter; dimension of the player may change by time.

    var gamepad: Controller? = null
    var axisX = 0f
    var axisY = 0f
    var axisRX = 0f
    var axisRY = 0f

    companion object {
        @Transient internal const val ACCEL_MULT_IN_FLIGHT: Double = 0.21
        @Transient internal const val WALK_ACCEL_BASE: Double = 0.67

        @Transient const val PLAYER_REF_ID: Int = 0x51621D
        @Transient const val BASE_HEIGHT = 40
    }

    /**
     * Creates new Player instance with empty elements (sprites, actorvalue, etc.).

     * **Use PlayerFactory to build player!**

     * @throws SlickException
     */
    init {
        isVisible = true
        referenceID = PLAYER_REF_ID // forcibly set ID
        density = BASE_DENSITY
        collisionType = KINEMATIC

        try {
            gamepad = Controllers.getController(0)
        }
        catch (e: IndexOutOfBoundsException) {
            println("[Player] gamepad not detected.")
        }
    }

    override fun update(gc: GameContainer, delta: Int) {
        if (vehicleRiding is Player)
            throw RuntimeException("Attempted to 'ride' " + "player object.")

        super.update(gc, delta)

        updateSprite(delta)

        updateMovementControl()

        if (noClip) {
            grounded = true
        }

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

        walkCounterX = 0
        isWalkingH = false
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

        // for mob ai:
        //super.setVeloY(veloY
        //        -
        //        pwr * Math.sqrt(scale)
        //);
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
        if (Terrarum.hasController) {
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

        /**
         * L-R stop
         */
        if (Terrarum.hasController && !isWalkingH) {
            if (axisX == 0f) {
                walkHStop()
            }
        }
        // ↑F, ↑S
        if (isWalkingH && !isFuncDown(input, EnumKeyFunc.MOVE_LEFT) && !isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
            walkHStop()
            prevHMoveKey = KEY_NULL
        }
        /**
         * U-D stop
         */
        if (Terrarum.hasController) {
            if (axisY == 0f) {
                walkVStop()
            }
        }
        // ↑E
        // ↑D
        if (isNoClip()
                && !isFuncDown(input, EnumKeyFunc.MOVE_UP)
                && !isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
            walkVStop()
            prevVMoveKey = KEY_NULL
        }

        /**
         * Left/Right movement
         */

        if (Terrarum.hasController) {
            if (axisX != 0f) {
                walkHorizontal(axisX < 0f, axisX.abs())
            }
        }
        // ↑F, ↓S
        if (isFuncDown(input, EnumKeyFunc.MOVE_RIGHT) && !isFuncDown(input, EnumKeyFunc.MOVE_LEFT)) {
            walkHorizontal(false, AXIS_POSMAX)
            prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)
        } // ↓F, ↑S
        else if (isFuncDown(input, EnumKeyFunc.MOVE_LEFT) && !isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
            walkHorizontal(true, AXIS_POSMAX)
            prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)
        } // ↓F, ↓S
        /*else if (isFuncDown(input, EnumKeyFunc.MOVE_LEFT) && isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
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
            if (Terrarum.hasController) {
                if (axisY != 0f) {
                    walkVertical(axisY < 0, axisY.abs())
                }
            }
            // ↑E, ↓D
            if (isFuncDown(input, EnumKeyFunc.MOVE_DOWN) && !isFuncDown(input, EnumKeyFunc.MOVE_UP)) {
                walkVertical(false, AXIS_POSMAX)
                prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)
            } // ↓E, ↑D
            else if (isFuncDown(input, EnumKeyFunc.MOVE_UP) && !isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                walkVertical(true, AXIS_POSMAX)
                prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)
            } // ↓E, ↓D
            /*else if (isFuncDown(input, EnumKeyFunc.MOVE_UP) && isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
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
            jumpAcc = 0.0
        }

    }

    override fun keyPressed(key: Int, c: Char) {

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

    fun Float.abs() = FastMath.abs(this)
}