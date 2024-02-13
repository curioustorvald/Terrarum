package net.torvald.terrarum.gameactors

data class PhysProperties(
    val immobileBody: Boolean = false,
    var usePhysics: Boolean = true,
    val useStairs: Boolean = false,
    val ignorePlatform: Boolean = true
) {
    companion object {
        fun HUMANOID_DEFAULT() = PhysProperties(
            immobileBody = false,
            usePhysics = true,
            useStairs = true,
            ignorePlatform = false,
        )
        /** e.g. dropped items, balls */
        fun PHYSICS_OBJECT() = PhysProperties(
            immobileBody = false,
            usePhysics = true,
            useStairs = false,
        )
        /** e.g. voice maker */
        fun IMMOBILE() = PhysProperties(
            immobileBody = true,
            usePhysics = false,
            useStairs = false,
        )
        /** e.g. camera */
        fun MOBILE_OBJECT() = PhysProperties(
            immobileBody = false,
            usePhysics = false,
            useStairs = false,
        )
    }
}