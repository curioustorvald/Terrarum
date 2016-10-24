package net.torvald.terrarum.gameactors

import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.faction.Faction
import net.torvald.terrarum.gamecontroller.EnumKeyFunc
import net.torvald.terrarum.gamecontroller.KeyMap
import net.torvald.terrarum.mapdrawer.MapDrawer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ActorHumanoid
import net.torvald.terrarum.ui.UIQuickBar
import org.dyn4j.geometry.Vector2
import org.lwjgl.input.Controller
import org.lwjgl.input.Controllers
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Input
import org.newdawn.slick.SlickException
import java.util.*

/**
 * Game player (YOU!)
 *
 * Created by minjaesong on 16-03-14.
 */

class Player(born: GameDate) : ActorHumanoid(born) {

    var vehicleRiding: Controllable? = null

    internal val quickBarRegistration = IntArray(UIQuickBar.SLOT_COUNT, { -1 })

    companion object {
        @Transient const val PLAYER_REF_ID: Int = 0x51621D
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
            throw Error("Attempted to 'ride' player object. ($vehicleRiding)")
        if (vehicleRiding != null && (vehicleRiding == this))
            throw Error("Attempted to 'ride' itself. ($vehicleRiding)")

        super.update(gc, delta)
    }

}