package net.torvald.terrarum.itemproperties

import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gameactors.sqrt

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
    fun pickaxePower(material: Material): Float {
        return 4f * material.forceMod.toFloat().sqrt()
    }


    fun armorwhatever() { TODO() }
    fun yogafire() { TODO() }
}