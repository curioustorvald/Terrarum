package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.gameactors.*

/**
 * Created by SKYHi14 on 2017-04-17.
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
    fun pickaxePower(actor: ActorHumanoid, material: Material): Float {
        return (4.0 * material.forceMod.toDouble().sqrt() * (actor.avStrength / 1000.0)).toFloat()
    }


    fun armorwhatever() { TODO() }
    fun yogafire() { TODO() }
}