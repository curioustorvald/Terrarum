package net.torvald.terrarum.gameactors

data class PhysProperties(
        val immobileBody: Boolean = false,
        var usePhysics: Boolean = true,
        val useStairs: Boolean = false
) {
    companion object {
        val HUMANOID_DEFAULT = PhysProperties(
                immobileBody = false,
                usePhysics = true,
                useStairs = true
        )
        /** e.g. dropped items, balls */
        val PHYSICS_OBJECT = PhysProperties(
                immobileBody = false,
                usePhysics = true,
                useStairs = false
        )
        /** e.g. voice maker */
        val IMMOBILE = PhysProperties(
                immobileBody = true,
                usePhysics = false,
                useStairs = false
        )
        /** e.g. camera */
        val MOBILE_OBJECT = PhysProperties(
                immobileBody = false,
                usePhysics = false,
                useStairs = false
        )
    }
}