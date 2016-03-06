package com.Torvald.Terrarum.Actors;

import com.Torvald.Rand.Fudge3;
import com.Torvald.Terrarum.Actors.Faction.Faction;
import com.Torvald.Terrarum.GameControl.EnumKeyFunc;
import com.Torvald.Terrarum.GameControl.KeyMap;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.spriteAnimation.SpriteAnimation;
import com.jme3.math.FastMath;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;
import org.newdawn.slick.*;

import java.util.HashSet;

/**
 * Created by minjaesong on 15-12-31.
 */
public class Player extends ActorWithBody implements Controllable, Pocketed, Factionable, Luminous {

    @Nullable public Controllable vehicleRiding;

    int jumpCounter = 0;
    int walkPowerCounter = 0;
    private final int MAX_JUMP_LENGTH = 17; // use 17; in internal frames
    /**
     * experimental value.
     */
    // private final float JUMP_ACCELERATION_MOD = ???f / 10000f; //quadratic mode
    private final float JUMP_ACCELERATION_MOD = 170f / 10000f; //linear mode
    private final int WALK_FRAMES_TO_MAX_ACCEL = 6;

    public float readonly_totalX = 0, readonly_totalY = 0;

    boolean jumping = false;

    @NotNull int walkHeading;

    private final int LEFT = 1;
    private final int RIGHT = 2;

    private int prevHMoveKey = -1;
    private int prevVMoveKey = -1;
    private final int KEY_NULL = -1;

    static final float ACCEL_MULT_IN_FLIGHT = 0.48f;
    static final float WALK_STOP_ACCEL = 0.32f;
    static final float WALK_ACCEL_BASE = 0.32f;

    private boolean noClip = false;

    public static final long PLAYER_REF_ID = 0x51621D;

    private final float AXIS_POSMAX = 1.0f;
    private final int GAMEPAD_JUMP = 5;

    private final int TSIZE = MapDrawer.TILE_SIZE;

    private HashSet<Faction> factionSet = new HashSet<>();

    private final float BASE_BUOYANCY = 0.98f;


    /**
     * Creates new Player instance with empty elements (sprites, actorvalue, etc.). <br />
     *
     * <strong>Use PlayerBuildFactory to build player!</strong>
     *
     * @throws SlickException
     */
    public Player() throws SlickException {
        super();
        referenceID = PLAYER_REF_ID;
        setVisible(true);
        super.setBuoyancy(BASE_BUOYANCY);
    }

    @Override
    public void update(GameContainer gc, int delta_t) {
        if (vehicleRiding instanceof Player) throw new RuntimeException("Attempted to 'ride' " +
                "player object.");

        updatePhysicalInfos();
        super.update(gc, delta_t);

        updateSprite(delta_t);

        updateMovementControl();

        if (noClip) { super.setGrounded(true); }


    }

    private void updatePhysicalInfos() {
        super.setScale(actorValue.getAsFloat("scale"));
        super.setMass(actorValue.getAsFloat("basemass")
                * FastMath.pow(super.getScale(), 3));
    }

    /**
     *
     * @param left (even if the game is joypad controlled, you must give valid value)
     * @param absAxisVal (set AXIS_POSMAX if keyboard controlled)
     */
    private void walkHorizontal(boolean left, float absAxisVal) {
        //if ((!super.isWalledLeft() && left) || (!super.isWalledRight() && !left)) {
            readonly_totalX = super.getVeloX()
                    +
                    actorValue.getAsFloat("accel")
                            * actorValue.getAsFloat("accelmult")
                            * FastMath.sqrt(super.getScale())
                            * applyAccelRealism(walkPowerCounter)
                            * (left ? -1 : 1)
                            * absAxisVal;

            super.setVeloX(readonly_totalX);

            if (walkPowerCounter < WALK_FRAMES_TO_MAX_ACCEL) {
                walkPowerCounter += 1;
            }

            // Clamp veloX
            super.setVeloX(
                    absClamp(super.getVeloX()
                            , actorValue.getAsFloat("speed")
                                    * actorValue.getAsFloat("speedmult")
                                    * FastMath.sqrt(super.getScale())
                    )
            );

            // Heading flag
            if (left) walkHeading = LEFT;
            else      walkHeading = RIGHT;
        //}
    }

    /**
     *
     * @param up (even if the game is joypad controlled, you must give valid value)
     * @param absAxisVal (set AXIS_POSMAX if keyboard controlled)
     */
    private void walkVertical(boolean up, float absAxisVal) {
        readonly_totalY = super.getVeloY()
                +
                actorValue.getAsFloat("accel")
                        * actorValue.getAsFloat("accelmult")
                        * FastMath.sqrt(super.getScale())
                        * applyAccelRealism(walkPowerCounter)
                        * (up ? -1 : 1)
                        * absAxisVal;

        super.setVeloY(readonly_totalY);

        if (walkPowerCounter < WALK_FRAMES_TO_MAX_ACCEL) {
            walkPowerCounter += 1;
        }

        // Clamp veloX
        super.setVeloY(
                absClamp(super.getVeloY()
                        , actorValue.getAsFloat("speed")
                                * actorValue.getAsFloat("speedmult")
                                * FastMath.sqrt(super.getScale())
                )
        );
    }

    /**
     * For realistic accelerating while walking.
     *
     * Naïve 'veloX += 3' is actually like:
     *
     *  a
     *  |      ------------
     *  |
     *  |
     * 0+------············  t
     *
     * which is unrealistic, so this method tries to introduce some realism by doing:
     *
     *  a
     *  |           ------------
     *  |        ---
     *  |       -
     *  |    ---
     * 0+----··················· t
     *
     *
     * @param x
     */
    private float applyAccelRealism(int x) {
        return 0.5f + 0.5f * -FastMath.cos(10 * x / (WALK_FRAMES_TO_MAX_ACCEL * FastMath.PI));
    }

    private void walkHStop() {
        if (super.getVeloX() > 0) {
            super.setVeloX(super.getVeloX()
                    -
                    actorValue.getAsFloat("accel")
                    * actorValue.getAsFloat("accelmult")
                    * FastMath.sqrt(super.getScale())
            );

            // compensate overshoot
            if (super.getVeloX() < 0)
                super.setVeloX(0);
        }
        else if (super.getVeloX() < 0) {
            super.setVeloX(super.getVeloX()
                    +
                    actorValue.getAsFloat("accel")
                    * actorValue.getAsFloat("accelmult")
                    * FastMath.sqrt(super.getScale())
            );

            // compensate overshoot
            if (super.getVeloX() > 0)
                super.setVeloX(0);
        }
        else {
            super.setVeloX(0);
        }

        walkPowerCounter = 0;
    }

    private void walkVStop() {
        if (super.getVeloY() > 0) {
            super.setVeloY(super.getVeloY()
                    -
                    WALK_STOP_ACCEL
                            * actorValue.getAsFloat("accelmult")
                            * FastMath.sqrt(super.getScale())
            );

            // compensate overshoot
            if (super.getVeloY() < 0)
                super.setVeloY(0);
        }
        else if (super.getVeloY() < 0) {
            super.setVeloY(super.getVeloY()
                    +
                    WALK_STOP_ACCEL
                            * actorValue.getAsFloat("accelmult")
                            * FastMath.sqrt(super.getScale())
            );

            // compensate overshoot
            if (super.getVeloY() > 0)
                super.setVeloY(0);
        }
        else {
            super.setVeloY(0);
        }

        walkPowerCounter = 0;
    }

    private void updateMovementControl() {
        if (!noClip) {
            if (super.isGrounded()) {
                actorValue.set("accelmult", 1f);
            } else {
                actorValue.set("accelmult", ACCEL_MULT_IN_FLIGHT);
            }
        }
        else {
            actorValue.set("accelmult", 1f);
        }
    }

    public void processInput(Input input) {
        Controller gamepad = null;
        float axisX = 0, axisY = 0, axisRX = 0, axisRY = 0;
        if (Terrarum.hasController) {
            gamepad = Controllers.getController(0);
            axisX = gamepad.getAxisValue(0);
            axisY = gamepad.getAxisValue(1);
            axisRX = gamepad.getAxisValue(2);
            axisRY = gamepad.getAxisValue(3);

            if (Math.abs(axisX) < Terrarum.CONTROLLER_DEADZONE) axisX = 0;
            if (Math.abs(axisY) < Terrarum.CONTROLLER_DEADZONE) axisY = 0;
            if (Math.abs(axisRX) < Terrarum.CONTROLLER_DEADZONE) axisRX = 0;
            if (Math.abs(axisRY) < Terrarum.CONTROLLER_DEADZONE) axisRY = 0;
        }

        /**
         * L-R stop
         */
        if (Terrarum.hasController) {
            if (axisX == 0) {
                walkHStop();
            }
        }
        else {
            // ↑F, ↑S
            if (!isFuncDown(input, EnumKeyFunc.MOVE_LEFT)
                    && !isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
                walkHStop();
                prevHMoveKey = KEY_NULL;
            }
        }
        /**
         * U-D stop
         */
        if (Terrarum.hasController) {
            if (axisY == 0) {
                walkVStop();
            }
        }
        else {
            // ↑E
            // ↑D
            if (isNoClip()
                    && !isFuncDown(input, EnumKeyFunc.MOVE_UP)
                    && !isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                walkVStop();
                prevVMoveKey = KEY_NULL;
            }
        }

        /**
         * Left/Right movement
         */

        if (Terrarum.hasController) {
            if (axisX != 0) {
                walkHorizontal(axisX < 0, AXIS_POSMAX);
            }
        }
        else {
            // ↑F, ↓S
            if (isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)
                    && !isFuncDown(input, EnumKeyFunc.MOVE_LEFT)) {
                walkHorizontal(false, AXIS_POSMAX);
                prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT);
            }
            // ↓F, ↑S
            else if (isFuncDown(input, EnumKeyFunc.MOVE_LEFT)
                    && !isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
                walkHorizontal(true, AXIS_POSMAX);
                prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT);
            }
            // ↓F, ↓S
            else if (isFuncDown(input, EnumKeyFunc.MOVE_LEFT)
                    && isFuncDown(input, EnumKeyFunc.MOVE_RIGHT)) {
                if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT)) {
                    walkHorizontal(false, AXIS_POSMAX);
                    prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT);
                }
                else if (prevHMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_RIGHT)) {
                    walkHorizontal(true, AXIS_POSMAX);
                    prevHMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_LEFT);
                }
            }
        }

        /**
         * Up/Down movement
         */
        if (noClip) {
            if (Terrarum.hasController) {
                if (axisY != 0) {
                    walkVertical(axisY > 0, AXIS_POSMAX);
                }
            }
            else {
                // ↑E
                // ↓D
                if (isFuncDown(input, EnumKeyFunc.MOVE_DOWN)
                        && !isFuncDown(input, EnumKeyFunc.MOVE_UP)) {
                    walkVertical(false, AXIS_POSMAX);
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN);
                }
                // ↓E
                // ↑D
                else if (isFuncDown(input, EnumKeyFunc.MOVE_UP)
                        && !isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                    walkVertical(true, AXIS_POSMAX);
                    prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP);
                }
                // ↓E
                // ↓D
                else if (isFuncDown(input, EnumKeyFunc.MOVE_UP)
                        && isFuncDown(input, EnumKeyFunc.MOVE_DOWN)) {
                    if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP)) {
                        walkVertical(false, AXIS_POSMAX);
                        prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN);
                    }
                    else if (prevVMoveKey == KeyMap.getKeyCode(EnumKeyFunc.MOVE_DOWN)) {
                        walkVertical(true, AXIS_POSMAX);
                        prevVMoveKey = KeyMap.getKeyCode(EnumKeyFunc.MOVE_UP);
                    }
                }
            }
        }

        /**
         * Jump control
         */
        if (isFuncDown(input, EnumKeyFunc.JUMP)
                || (Terrarum.hasController && gamepad.isButtonPressed(GAMEPAD_JUMP))) {
            if (!noClip) {
                if (super.isGrounded()) {
                    jumping = true;
                }
                jump();
            }
            else {
                walkVertical(true, AXIS_POSMAX);
            }
        }
        else {
            jumping = false;
            jumpCounter = 0;
        }

    }

    public void keyPressed(int key, char c) {

    }

    /**
     * See ./work_files/Jump\ power\ by\ pressing\ time.gcx
     */
    private void jump() {
        if (jumping) {
            float len = MAX_JUMP_LENGTH;
            float pwr = actorValue.getAsFloat("jumppower");

            // increment jump counter
            if (jumpCounter < len) jumpCounter += 1;
            // quadratic time (convex) mode
            /*
            float sumT = (jumpCounter * (jumpCounter + 1)) / 2f;
            float timedJumpCharge = ((len + 1) / 2f) - (sumT / len);
            if (timedJumpCharge < 0) timedJumpCharge = 0;

            float jumpAcc = pwr * timedJumpCharge * JUMP_ACCELERATION_MOD;

            super.setVeloY(super.getVeloY()
                    - jumpAcc
            );
            */

            // linear time mode
            float init = (len + 1) / 2f;
            float timedJumpCharge = init - (init / len) * jumpCounter;
            if (timedJumpCharge < 0) timedJumpCharge = 0;

            float jumpAcc = pwr * timedJumpCharge * JUMP_ACCELERATION_MOD
                    * FastMath.sqrt(getScale());

            super.setVeloY(super.getVeloY()
                    - jumpAcc
            );

            // concave mode?
        }

        // for mob AI:
        //super.setVeloY(super.getVeloY()
        //        -
        //        pwr * FastMath.sqrt(super.getScale())
        //);
    }

    private float jumpFuncLin(float pwr, float len) {
        return -(pwr / len) * jumpCounter;
    }

    private float jumpFuncSqu(float pwr, float len) {
        return (pwr / (len * len))
                * (jumpCounter - len)
                * (jumpCounter - len) // square
                - pwr;
    }

    private float jumpFuncExp(float pwr, float len) {
        float a = FastMath.pow(pwr + 1, 1 / len);
        return -FastMath.pow(a, len) + 1;
    }

    private boolean isFuncDown(Input input, EnumKeyFunc fn) {
        return input.isKeyDown(KeyMap.getKeyCode(fn));
    }

    private float absClamp(float i, float ceil) {
        if (i > 0)
            return (i > ceil) ? ceil : i;
        else if (i < 0)
            return (-i > ceil) ? -ceil : i;
        else
            return 0;
    }

    private void updateSprite(int delta_t) {
        sprite.update(delta_t);
        if (spriteGlow != null) {
            spriteGlow.update(delta_t);
        }

        if (super.isGrounded()) {
            if (walkHeading == LEFT) {
                sprite.flip(true, false);
                if (spriteGlow != null) {
                    spriteGlow.flip(true, false);
                }
            }
            else {
                sprite.flip(false, false);
                if (spriteGlow != null) {
                    spriteGlow.flip(false, false);
                }
            }
        }
    }

    @Override
    public ActorInventory getInventory() {
        return inventory;
    }

    @Override
    public void overwriteInventory(ActorInventory inventory) {
        this.inventory = inventory;
    }

    public boolean isNoClip() {
        return noClip;
    }

    public void setNoClip(boolean b) {
        noClip = b;
    }

    public ActorValue getActorValue() {
        return actorValue;
    }

    public SpriteAnimation getSpriteGlow() {
        return spriteGlow;
    }

    @Override
    public void assignFaction(Faction f) {
        factionSet.add(f);
    }

    @Override
    public void unassignFaction(Faction f) {
        factionSet.remove(f);
    }

    @Override
    public HashSet<Faction> getAssignedFactions() {
        return factionSet;
    }

    @Override
    public void clearFactionAssigning() {
        factionSet.clear();
    }

    @Override
    public void setLuminance(char RGB) {
        actorValue.set("luminosity", (int) RGB);
    }

    @Override
    public char getLuminance() {
        return actorValue.hasKey("luminosity") ?
               (char) actorValue.getAsInt("luminosity") : 0;
    }
}
