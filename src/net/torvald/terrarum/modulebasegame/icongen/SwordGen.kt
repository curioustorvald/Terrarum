package net.torvald.terrarum.modulebasegame.icongen

import com.badlogic.gdx.graphics.Color

/**
 * Created by minjaesong on 2020-02-11.
 */
object SwordGen {

    operator fun invoke(
            size: Int, accessories: IconGenOverlays, colour: Color, type: SwordGenType,
            straightness: Double, roughness: Double) {



    }

    //private fun getBaseArmingSwordMesh

}

enum class SwordGenType {
    ArmingSword, TwoHanded, Mace
}

// dummy class plz del
class IconGenOverlays {}