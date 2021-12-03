package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.random.Fudge3
import net.torvald.random.HQRNG
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import kotlin.math.pow

/**
 * Created by minjaesong on 2019-03-17.
 */
object WeaponMeleeCore {

    val SQRT2 = Math.sqrt(2.0)

    private val dF3 = Fudge3(HQRNG())
    private val randomiser = doubleArrayOf(0.94, 0.96, 0.98, 1.0, 1.02, 1.04, 1.06)

    private fun randomise() = dF3.rollForArray(randomiser)
    private fun getAttackMomentum(weapon: WeaponMeleeBase, actor: ActorHumanoid) =
            weapon.mass * weapon.material.density * weapon.velocityMod * actor.scale.pow(SQRT2) // TODO multiply racial strength from RaceCodex
    fun getAttackPower(weapon: WeaponMeleeBase, actor: ActorHumanoid, actee: ActorHumanoid) =
            getAttackMomentum(weapon, actor) * randomise() * maxOf(1.0, (actee.hitbox.endY - actor.hitbox.startY) / actee.hitbox.height)

}

abstract class WeaponMeleeBase(originalID: ItemID) : GameItem(originalID) {
    abstract val velocityMod: Double
}