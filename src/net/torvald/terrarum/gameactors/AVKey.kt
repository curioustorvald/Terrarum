package net.torvald.terrarum.gameactors

/**
 * See [res/raw/Creature_raw_doc.md] for information about raw.
 *
 * Created by minjaesong on 16-04-02.
 */
object AVKey {
    const val MULT = "mult"

    /** pixels per frame
     * walking/running speed
     */
    const val SPEED = "speed"
    const val SPEEDMULT = "speed$MULT"
    /** pixels per frame squared
     * acceleration of the movement (e.g. running, flying, driving, etc.)
     */
    const val ACCEL = "accel"
    const val ACCELMULT = "accel$MULT"
    const val SCALE = "scale"
    /** pixels */
    const val BASEHEIGHT = "baseheight"
    /** kilogrammes */
    const val BASEMASS = "basemass"
    /** pixels per frame */
    const val JUMPPOWER = "jumppower"
    const val JUMPPOWERMULT = "jumppower$MULT"

    /** Int
     * "Default" value of 1 000
     */
    const val STRENGTH = "strength"
    const val ENCUMBRANCE = "encumbrance"
    /** 30-bit RGB (Int)
     * 0000 0010000000 0010000000 0010000000
     *      ^ Red      ^ Green    ^ Blue
     */
    const val LUMINOSITY = "luminosity"
    const val PHYSIQUEMULT = "physique$MULT"
    const val DRAGCOEFF = "dragcoeff"

    /** String
     * e.g. Jarppi
     */
    const val NAME = "name"

    /** String
     * e.g. Duudson
     */
    const val RACENAME = "racename"
    /** String
     * e.g. Duudsonit
     */
    const val RACENAMEPLURAL = "racenameplural"
    /** killogrammes
     * will affect attack strength, speed and inventory label
     * (see "Attack momentum calculator.numbers")
     * e.g. Hatchet (tiny)
     */
    const val TOOLSIZE = "toolsize"
    /** Boolean
     * whether the player can talk with it
     */
    const val INTELLIGENT = "intelligent"

    /** (unit TBA)
     * base defence point of the species
     */
    const val BASEDEFENCE = "basedefence"
    /** (unit TBA)
     * current defence point of worn armour(s)
     */
    const val ARMOURDEFENCE = "armourdefence"
    const val ARMOURDEFENCEMULT = "armourdefence$MULT"



    const val _PLAYER_QUICKBARSEL = "__quickbarselection"
}