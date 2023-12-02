package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.ActorWithBody.Companion.GAME_TO_SI_ACC
import net.torvald.terrarum.sqr
import net.torvald.terrarum.sqrt
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2017-04-17.
 */
object Calculate {
    /**
     * Pickaxe power per action (swing)
     *
     * @return arbrtrary unit
     *
     * See: work_files/Pickaxe Power.xlsx
     *
     * TODO Newtons as unit?
     */
    @JvmStatic fun pickaxePower(actor: ActorWithBody, material: Material?): Float {
        return (4.0 * (material?.forceMod?.toDouble() ?: 0.15) * (actor.avStrength / 1000.0)).toFloat()
    }

    @JvmStatic fun hatchetPower(actor: ActorWithBody, material: Material?): Float {
        return (1.0 * (material?.forceMod?.toDouble() ?: 0.15) * (actor.avStrength / 1000.0)).toFloat()
    }

    private val fallDamageDampenMult = (32.0 / 1176.0).sqr()
    @JvmStatic fun collisionDamage(actor: ActorWithBody, movement: Vector2): Double {
        return actor.mass * (movement.magnitude / (10.0 / Terrarum.PHYS_TIME_FRAME).sqr()) *
                fallDamageDampenMult * // dampen factor (magic number)
                (actor.actorValue.getAsDouble(AVKey.FALLDAMPENMULT) ?: 1.0) * // additional dampen factor (actorvalue)
                GAME_TO_SI_ACC // unit conversion
    }


    fun armorwhatever() { TODO() }
    fun yogafire() { TODO() }
}