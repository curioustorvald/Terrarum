package net.torvald.terrarum.itemproperties

/**
 * To be used with items AND TILES (electricity resistance, thermal conductivity)
 *
 * Created by minjaesong on 2016-03-18.
 */
data class Material (
    //var maxEdge: Int, // i think weapSharpnessMod would cut it // arbitrary unit
    var hardness: Int, // arbitrary unit
    var density: Int, // grams per litre

    // impact force: force applied by sudden strike, e.g. hammer/axe/sword strike
    var impactRigidness: Int, // arbitrary unit  (rigid <-> soft)  a weapon made of soft material will inflict less damage
    var impactFractureForce: Int, // pascal (N/m^2); if the item (e.g. sword) receives a force that exceeds this value, the item will be destroyed

    // compressive force: force applied by exerting pressure on an object, e.g. sword/spear stab
    var compressiveRigidness: Int,  // arbitrary unit  (rigid <-> soft)  a weapon made of soft material will inflict less damage
    var compressiveFractureForce: Int, // pascal (N/m^2); if the item (e.g. sword) receives a force that exceeds this value, the item will be destroyed

    // remarks:
    //      we won't need elasticity, even if we have glass
    // some examples:
    //      - glass sword works as the material has high compressive fracture, but prone to shatter
    //        (hit mobs 5-6 times and it's gone) as it shatters easily as it has low impact fracture



    var electricityResistance: Int, // ohm
    var thermalConductivity: Int, // pascal (N/m^2); if the item (e.g. sword) receives a force that exceeds this value, the item will be destroyed



    var forceMod: Int, // arbitrary unit. See Pickaxe_Power.xlsx
    var armourMod: Double // multiplier
)