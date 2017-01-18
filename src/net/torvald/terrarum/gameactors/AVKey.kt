package net.torvald.terrarum.gameactors

/**
 * See [res/raw/Creature_raw_doc.md] for information about raw.
 *
 * Created by minjaesong on 16-04-02.
 */
object AVKey {
    const val BUFF = "buff"

    /** pixels per frame
     * walking/running speed
     */
    const val SPEED = "speed"
    const val SPEEDBUFF = "$SPEED$BUFF"
    /** pixels per frame squared
     * acceleration of the movement (e.g. running, flying, driving, etc.)
     */
    const val ACCEL = "accel"
    const val ACCELBUFF = "$ACCEL$BUFF"
    /** internal value used by ActorHumanoid to implement friction in walkfunction */
    const val SCALE = "scale"
    const val SCALEBUFF = "$SCALE$BUFF" // aka PHYSIQUE
    /** pixels */
    const val BASEHEIGHT = "baseheight"
    /** kilogrammes */
    const val BASEMASS = "basemass"
    /** pixels per frame */
    const val JUMPPOWER = "jumppower"
    const val JUMPPOWERBUFF = "$JUMPPOWER$BUFF"

    /** Int
     * "Default" value of 1 000
     */
    const val STRENGTH = "strength"
    const val STRENGTHBUFF = "$STRENGTH$BUFF"
    const val ENCUMBRANCE = "encumbrance"
    /** 30-bit RGB (Int)
     * 0000 0010000000 0010000000 0010000000
     *      ^ Red      ^ Green    ^ Blue
     */
    const val LUMINOSITY = "luminosity"
    const val DRAGCOEFF = "dragcoeff"

    /** String
     * e.g. Jarppi
     */
    const val NAME = "name"

    /** String
     * e.g. Duudsoni
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
    const val ARMOURDEFENCEBUFF = "$ARMOURDEFENCE$BUFF"

    const val MAGICREGENRATE = "magicregenrate"
    const val MAGICREGENRATEBUFF = "$MAGICREGENRATE$BUFF"


    /** String
     * UUID for certain fixtures
     */
    const val UUID = "uuid"



    const val __PLAYER_QUICKBARSEL = "__quickbarselection"
}