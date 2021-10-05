package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.sqrt

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
        return (4.0 * (material?.forceMod?.toDouble() ?: 1.0).sqrt() * (actor.avStrength / 1000.0)).toFloat()
    }


    fun armorwhatever() { TODO() }
    fun yogafire() { TODO() }
}