package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.spriteanimation.AssembledSpriteAnimation
import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.realestate.LandUtil
import org.dyn4j.geometry.Vector2

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
open class ActorHumanoid : ActorWithBody, Controllable, Pocketed, Factionable, LandHolder, HistoricalFigure {

    protected constructor()

    constructor(birth: Long, death: Long? = null, physProp: PhysProperties = PhysProperties.HUMANOID_DEFAULT) : super() {
        actorValue[AVKey.__HISTORICAL_BORNTIME] = birth
        death?.let { actorValue[AVKey.__HISTORICAL_DEADTIME] = death }
        this.physProp = physProp
    }

    @Transient internal var vehicleRiding: Controllable? = null // usually player only
    var vehicleRidingActorID: ActorID? = null


    /** Must be set by PlayerFactory */
    override var inventory: ActorInventory = ActorInventory(this, 2000, FixtureInventory.CAPACITY_MODE_WEIGHT) // default constructor


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

    var actorValueColour: Cvec
        get() = Cvec(
                (actorValue.getAsFloat(AVKey.LUMR) ?: 0f),
                (actorValue.getAsFloat(AVKey.LUMG) ?: 0f),
                (actorValue.getAsFloat(AVKey.LUMB) ?: 0f),
                (actorValue.getAsFloat(AVKey.LUMA) ?: 0f)
        )
        set(value) {
            actorValue[AVKey.LUMR] = value.r
            actorValue[AVKey.LUMG] = value.g
            actorValue[AVKey.LUMB] = value.b
            actorValue[AVKey.LUMA] = value.a
        }

    var actorValueShade: Cvec
        get() = Cvec(
                (actorValue.getAsFloat(AVKey.OPAR) ?: 0f),
                (actorValue.getAsFloat(AVKey.OPAG) ?: 0f),
                (actorValue.getAsFloat(AVKey.OPAB) ?: 0f),
                (actorValue.getAsFloat(AVKey.OPAA) ?: 0f)
        )
        set(value) {
            actorValue[AVKey.OPAR] = value.r
            actorValue[AVKey.OPAG] = value.g
            actorValue[AVKey.OPAB] = value.b
            actorValue[AVKey.OPAA] = value.a
        }

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    @Transient override var lightBoxList: ArrayList<Lightbox> = arrayListOf(Lightbox(Hitbox(2.0, 2.0, baseHitboxW - 3.0, baseHitboxH - 3.0), Cvec(0)))
        // the actual values are update on the update()
    @Transient override var shadeBoxList: ArrayList<Lightbox> = arrayListOf(Lightbox(Hitbox(2.0, 2.0, baseHitboxW - 3.0, baseHitboxH - 3.0), Cvec(0)))
        // the actual values are update on the update()

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
    @Transient private val JUMP_ACCELERATION_MOD =  51.0 / 10000.0 // (170 * (17/MAX_JUMP_LENGTH)^2) / 10000.0 // bigger value = higher jump
    @Transient private val WALK_FRAMES_TO_MAX_ACCEL = 5 // how many frames it takes to reach maximum walking speed

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

    @Transient private var readonly_totalX = 0.0
    @Transient private var readonly_totalY = 0.0

    internal var jumping = false

    /** Make air-jumping (sometimes referred as "double jumping) work
     * Valid values: 0 or 2; 1 is same as 0, and because of the "failed" implementation, any value greater than
     * 2 will allow tremendous jump height if you control it right, so it's not recommended to use those values.
     */
    internal var airJumpingPoint: Int
        get() = actorValue.getAsInt(AVKey.AIRJUMPPOINT) ?: 0
        set(value) {
            actorValue[AVKey.AIRJUMPPOINT] = value
        }
    internal var airJumpingCount: Int
        get() = actorValue.getAsInt(AVKey.AIRJUMPCOUNT) ?: 0
        set(value) {
            actorValue[AVKey.AIRJUMPCOUNT] = value
        }

    internal var walkHeading: Int = 0

    @Transient private var prevHMoveKey = KEY_NULL
    @Transient private var prevVMoveKey = KEY_NULL

    @Transient private val AXIS_KEYBOARD = -13372f // leetz

    var isUpDown = false; protected set
    var isDownDown = false; protected set
    var downDownVirtually = false; internal set
    var isLeftDown = false; protected set
    var isRightDown = false; protected set
    var isJumpDown = false; protected set
    var isJumpJustDown = false; protected set // TODO if jump key is held in current update frame
    protected inline val isGamer: Boolean
        get() = if (Terrarum.ingame == null) false else this == INGAME.actorNowPlaying

    private var jumpJustPressedLatched = false

    @Transient private val nullItem = object : GameItem("item@basegame:0") {
        override val isUnique: Boolean = false
        override var baseMass: Double = 0.0
        override var baseToolSize: Double? = null
        override var inventoryCategory = "should_not_be_seen"
        override val originalName: String = actorValue.getAsString(AVKey.NAME) ?: "(no name)"
        override var stackable = false
        override val isDynamic = false
        override val materialId = ""
    }

    override fun update(delta: Float) {
        // update lightbox
        lightBoxList[0].light = actorValueColour
        shadeBoxList[0].light = actorValueShade


        super.update(delta)

        if (vehicleRiding is IngamePlayer)
            throw Error("Attempted to 'ride' player object. ($vehicleRiding)")
        if (vehicleRiding != null && vehicleRiding == this)
            throw Error("Attempted to 'ride' itself. ($vehicleRiding)")



        // don't put this into keyPressed; execution order is important!
        updateGamerControlBox()

        if (!isPickedUp) {
            processInput(delta)
        }

        updateSprite(delta)

        if (isNoClip) {
            //grounded = true
//            platformToIgnore = null
            downDownVirtually = false
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
            if (!inventory.itemEquipped.contains(it.itm)) { // unequipped
                ItemCodex[it.itm]!!.effectWhileInPocket(this, delta)
            }
            else { // equipped
                ItemCodex[it.itm]!!.effectWhileEquipped(this, delta)
            }
        }
    }

    private fun updateGamerControlBox() {
        if (isGamer) {
            isUpDown = Gdx.input.isKeyPressed(App.getConfigInt("control_key_up"))
            isLeftDown = Gdx.input.isKeyPressed(App.getConfigInt("control_key_left"))
            isDownDown = Gdx.input.isKeyPressed(App.getConfigInt("control_key_down"))
            isRightDown = Gdx.input.isKeyPressed(App.getConfigInt("control_key_right"))
            isJumpDown = Gdx.input.isKeyPressed(App.getConfigInt("control_key_jump"))

            val gamepad = App.gamepad

            if (gamepad != null) {
                axisX = gamepad.getAxis(App.getConfigInt("control_gamepad_axislx"))
                axisY = gamepad.getAxis(App.getConfigInt("control_gamepad_axisly"))
                axisRX = gamepad.getAxis(App.getConfigInt("control_gamepad_axisrx"))
                axisRY = gamepad.getAxis(App.getConfigInt("control_gamepad_axisry"))

                isJumpDown = Gdx.input.isKeyPressed(App.getConfigInt("control_key_jump")) ||
                             gamepad.getButton(App.getConfigInt("control_gamepad_ltrigger"))
            }

            if (isJumpJustDown && jumpJustPressedLatched) {
                isJumpJustDown = false
            }
            else if (isJumpDown && !jumpJustPressedLatched) {
                isJumpJustDown = true
                jumpJustPressedLatched = true
            }
            else if (!isJumpDown) {
                jumpJustPressedLatched = false
            }

        }
        else {
            isUpDown = axisY < 0f
            isDownDown = axisY > 0f
            isLeftDown = axisX < 0f
            isRightDown = axisX > 0f
        }

        // platform-related hacks
        // allow latching down downDownVirtually only when standing on a platform AND not jumping upwards
        val occupyingTileHasPlatform = bodyTiles.filterNotNull().any { it.isPlatform }
        val feetTileHasPlatform = feetTiles.filterNotNull().any { it.isPlatform }
        val feetTileIsAllPlatform = feetTiles.filterNotNull().all { it.isPlatform }
        if (isDownDown && feetTileIsAllPlatform && (controllerV?.y ?: 0.0) >= 0.0) {// ||
//            occupyingTileHasPlatform && !feetTileHasPlatform) { // FIXME commenting this out enables platform-ladder but falldown gets slowed down if the body passes thru the platform but I think this behav might be beneficial for player?
//            downDownVirtually = true
        }
        if (downDownVirtually && !occupyingTileHasPlatform && !feetTileIsAllPlatform) {
//            downDownVirtually = false
        }
        // TODO just disable "snap to ground" on collision solver if {player's body overlaps with the platform/downDownVirtually}?
        // the point is: disable snap (or don't consider offending tiles as solid) for certain Y-pos only, tiles on Y+1 are still solid
    }

    private inline val hasController: Boolean
        get() = if (isGamer) App.gamepad != null
                else true

    private var playerJumpKeyHeldDown = false

    private fun processInput(delta: Float) {

        // Good control is simple: it move as the player meant: if I push the stick forward, it goes forward, rather than
        //                         the way your character is looking. Think of the SM64

        /**
         * L-R stop
         */
        if (hasController && !isWalkingH) {
            if (axisX == 0f) {
                walkHStop()
            }
        }
        // ↑F, ↑S
        if (isWalkingH && !isLeftDown && !isRightDown && axisX == 0f) {
            walkHStop()
            prevHMoveKey = KEY_NULL
        }
        /**
         * U-D stop
         */
        // ↑E
        // ↑D
        if (isNoClip && !isUpDown && !isDownDown && axisY == 0f) {
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
            prevHMoveKey = App.getConfigInt("control_key_right")
        } // ↓F, ↑S
        else if (isLeftDown && !isRightDown) {
            walkHorizontal(true, AXIS_KEYBOARD)
            prevHMoveKey = App.getConfigInt("control_key_left")
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
                    walkVertical(axisY > 0, axisY.abs())
                }
            }
            // ↑E, ↓D
            if (isDownDown && !isUpDown) {
                walkVertical(false, AXIS_KEYBOARD)
                prevVMoveKey = App.getConfigInt("control_key_down")
            } // ↓E, ↑D
            else if (isUpDown && !isDownDown) {
                walkVertical(true, AXIS_KEYBOARD)
                prevVMoveKey = App.getConfigInt("control_key_up")
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
         * Jump-key control
         */
        if (isJumpDown) {
            if (!isNoClip) {

                // wall-kick jumps // (think of Mario DS!)

                if (isJumpJustDown && !jumping && (walledLeft || walledRight)) {
                    printdbg(this, "Wallkicking detection test print")
                }

                // perpendicular jumps //

                // make airjumping work by resetting some jump-related variables
                if (!playerJumpKeyHeldDown && (walledBottom || airJumpingCount < airJumpingPoint)) {
                    jumping = true

                    // make airjumping work
                    if (airJumpingCount < airJumpingPoint) {
                        //printdbg(this, "airjump!")
                        // reset jumping-related variables
                        jumpCounter = 0
                        airJumpingCount += 1
                        // also reset the velocity because otherwise the jump is not truly reset
                        jumpAcc = 0.0
                        //controllerV?.y = 0.0
                    }
                }

                // acutally launch the player in the air
                jump()
            }
            else {
                walkVertical(true, AXIS_KEYBOARD)
            }

            playerJumpKeyHeldDown = true
        }
        else {
            jumping = false
            jumpCounter = 0
            jumpAcc = 0.0
            playerJumpKeyHeldDown = false

            // reset airjump counter
            if (walledBottom) {
                airJumpingCount = 0
            }
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
     * @param left (even if the game is gamepad controlled, you must give valid value)
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
            controllerV?.x?.let { controllerV!!.x = controllerV!!.x.plus(readonly_totalX).bipolarClamp(avSpeedCap * absAxisVal) }
        else
            controllerV?.x?.let { controllerV!!.x = controllerV!!.x.plus(readonly_totalX).bipolarClamp(avSpeedCap) }

        if (walkCounterX <= WALK_FRAMES_TO_MAX_ACCEL) {
          walkCounterX += 1
        }

        isWalkingH = true



        // Heading flag
        walkHeading = if (left) LEFT else RIGHT
    }

    /**

     * @param up (even if the game is gamepad controlled, you must give valid value)
     * *
     * @param absAxisVal (set AXIS_KEYBOARD if keyboard controlled)
     */
    private fun walkVertical(up: Boolean, absAxisVal: Float) {


        if (avAcceleration.isNaN()) {
            throw Error("avAccelation is NaN")
        }


//        if (up && walledTop || !up && walledBottom) return
        readonly_totalY =
                if (absAxisVal == AXIS_KEYBOARD)
                    avAcceleration * applyVelo(walkCounterY) * (if (up) -1f else 1f)
                else
                    avAcceleration * applyVelo(walkCounterY) * (if (up) -1f else 1f) * absAxisVal

        if (absAxisVal != AXIS_KEYBOARD)
            controllerV?.y?.let { controllerV!!.y = controllerV!!.y.plus(readonly_totalY).bipolarClamp(avSpeedCap * absAxisVal) }
        else
            controllerV?.y?.let { controllerV!!.y = controllerV!!.y.plus(readonly_totalY).bipolarClamp(avSpeedCap) }

        if (walkCounterY < 1000000) {
            walkCounterY += 1
        }


        isWalkingV = true


        // determine a platform to ignore
        /*world?.let { world ->
            var hasPlatformOnTheFeet = false
            forEachFeetTile { if (it?.isPlatform == true) hasPlatformOnTheFeet = true }

            if (hasPlatformOnTheFeet) {
                // equation copied verbatim from the ActorWthBody.forEachFeetTile
                val y = hitbox.endY.plus(1.0).div(TerrarumAppConfiguration.TILE_SIZE).floorToInt()
                var wxStart = hIntTilewiseHitbox.startX.toInt()
                var wxEnd = wxStart
                // scan to the left
                while (BlockCodex[world.getTileFromTerrain(wxStart, y)].isPlatform) wxStart -= 1
                // scan to the right
                while (BlockCodex[world.getTileFromTerrain(wxEnd, y)].isPlatform) wxEnd += 1

                platformToIgnore = Hitbox.fromTwoPoints(
                        (wxStart+1) * TILE_SIZED,
                        y * TILE_SIZED,
                        wxEnd * TILE_SIZED,
                        (y+1) * TILE_SIZED,
                        true
                )
            }

        }*/
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
//        platformToIgnore = null
    }

    private fun getJumpAcc(pwr: Double, timedJumpCharge: Double): Double {
        return pwr * timedJumpCharge * JUMP_ACCELERATION_MOD * Math.pow(scale, 0.25) // positive value
    }

    @Transient private var oldMAX_JUMP_LENGTH = -1 // init
    @Transient private var oldJUMPPOWER = -1.0 // init
    @Transient private var oldJUMPPOWERBUFF = -1.0 // init
    @Transient private var oldScale = -1.0
    @Transient private var oldDragCoefficient = -1.0
    // used by some AIs
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
     */
    private fun jump() {
        if (jumping) {// && jumpable) {
            // increment jump counter
            if (jumpCounter < MAX_JUMP_LENGTH) jumpCounter += 1

            val timedJumpCharge = jumpFunc(MAX_JUMP_LENGTH, jumpCounter)

            jumpAcc = getJumpAcc(jumpPower, timedJumpCharge)

            controllerV?.y?.let { controllerV!!.y -= jumpAcc } // feed negative value to the vector
            // do not think of resetting this to zero when counter hit the ceiling; that's HOW NOT
            // newtonian physics work, stupid myself :(

        }
        // not sure we need this...
        /*else if (!jumpable) {
            jumpable = true  // this is kind of like "semaphore", we toggle it now
            grounded = false // just in case...
        }*/

        // release "jump key" (of AIs?)
        if (jumpCounter >= MAX_JUMP_LENGTH) {
            if (isGamer) {
                playerJumpKeyHeldDown = false
            }

            isJumpDown = false
            jumping = false
            jumpCounter = 0
            jumpAcc = 0.0
        }
    }

    override fun onActorValueChange(key: String, value: Any?) {
        // make quickslot work
        if (key == AVKey.__PLAYER_QUICKSLOTSEL && value != null) {
            // ONLY FOR HAND_GRIPs!!
            val quickBarItem = ItemCodex[inventory.getQuickslotItem(actorValue.getAsInt(key)!!)?.itm]

            if (quickBarItem != null && quickBarItem.equipPosition == GameItem.EquipPosition.HAND_GRIP) {
                equipItem(quickBarItem)
            }
            else {
                unequipSlot(GameItem.EquipPosition.HAND_GRIP)
            }

            // force update inventory UI, but when the pie menu is not open (pie menu constantly writes to the actorvalue, which will rebuildList()
            /*try {
                if (!(Terrarum.ingame!! as TerrarumIngame).uiPieMenu.isVisible) {
                    ((Terrarum.ingame!! as TerrarumIngame).uiInventoryPlayer as UIInventoryFull).rebuildList()
                }
            }
            catch (LateInitMyArse: kotlin.UninitializedPropertyAccessException) {
            }*/
            // commented; works without it
        }
    }

    override fun getSpriteHead(): TextureRegion? {
        return if (this is IngamePlayer)
            this.spriteHeadTexture
        else if (this is HasAssembledSprite)
            this.spriteHeadTexture
        else super.getSpriteHead()
    }

    fun Float.abs() = FastMath.abs(this)

    open fun updateSprite(delta: Float) {
        sprite?.update(delta)
        spriteGlow?.update(delta)

        if (walledBottom && controllerV?.x != 0.0) {
            //switch row
            if (this is HasAssembledSprite) {
                (sprite as? AssembledSpriteAnimation)?.currentAnimation = "ANIM_RUN"
                (spriteGlow as? AssembledSpriteAnimation)?.currentAnimation = "ANIM_RUN"
            }
            else {
                (sprite as? SheetSpriteAnimation)?.switchRow(SPRITE_ROW_WALK)
                (spriteGlow as? SheetSpriteAnimation)?.switchRow(SPRITE_ROW_WALK)
            }

            // set anim frame delay
            if (this is HasAssembledSprite) {
                try {
                    val baseDelay = animDesc!!.getAnimByFrameName("ANIM_RUN").delay
                    val moveSpeedMult = (controllerV?.x ?: 0.0).abs().coerceAtLeast(0.001).toFloat() / 1.414f // FIXME empirical value
                    val stride = scale.toFloat()
                    val maxMoveSpeed = scale.sqrt().toFloat() // ActorWithBody uses scale.sqrt() for determining walk acceleration
                    val scaleCompensation = stride / maxMoveSpeed

                    val finalDelay = baseDelay * (scaleCompensation / moveSpeedMult)

                    (sprite as? AssembledSpriteAnimation)?.overrideDelay = finalDelay
                    (spriteGlow as? AssembledSpriteAnimation)?.overrideDelay = finalDelay
                }
                catch (e: NullPointerException) {
                    println(animDesc!!.animations.keys.joinToString())

                    throw e
                }
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

            if (this is HasAssembledSprite) {
                (sprite as? AssembledSpriteAnimation)?.currentAnimation = "ANIM_IDLE"
                (spriteGlow as? AssembledSpriteAnimation)?.currentAnimation = "ANIM_IDLE"
                (sprite as? AssembledSpriteAnimation)?.overrideDelay = 0f
                (spriteGlow as? AssembledSpriteAnimation)?.overrideDelay = 0f
            }
            else {
                (sprite as? SheetSpriteAnimation)?.switchRow(SPRITE_ROW_IDLE)
                (spriteGlow as? SheetSpriteAnimation)?.switchRow(SPRITE_ROW_IDLE)
            }
        }
    }
}