package net.torvald.terrarum


/**
 * See REFERENCING.md
 */
object ReferencingRanges {

    val TILES = 0..65535 // 65 536 blocks
    val WALLS = 65536..131071 // 65 536 walls
    val ITEMS_STATIC = 135168..0x0F_FFFF // 913 408 items
    val ITEMS_DYNAMIC = 0x10_0000..0x0FFF_FFFF // 267 386 880 pseudo-items
    val ACTORS = 0x1000_0000..0x7FFF_FFFF // too much actors

    // Actor IDs are assigned in 256 groups, single actor can have 256 sub-actors
    val ACTORS_BEHIND  = 0x1000_0000..0x1FFF_FFFF // Rendered behind (e.g. tapestries)
    val ACTORS_MIDDLE  = 0x2000_0000..0x4FFF_FFFF // Regular actors (e.g. almost all of them)
    val ACTORS_MIDTOP  = 0x5000_0000..0x5FFF_FFFF // Special (e.g. weapon swung, bullets, dropped item, particles)
    val ACTORS_FRONT   = 0x6000_0000..0x6EFF_FFFF // Rendered front (e.g. fake tile)

    val ACTORS_WIRES   = 0x6FFF_0000..0x6FFF_FFFF // Rendered front--wires
    val ACTORS_WIRES_HELPER = 0x7000_0000..0x7000_FFFF // Rendered overlay--wiring port icons and logic gates

    val ACTORS_OVERLAY = 0x7001_0000..0x7FFF_FFFF // Rendered as screen overlay, not affected by light nor environment overlays

    val PREFIX_DYNAMICITEM = "dyn"
    val PREFIX_ACTORITEM = "actor"
    val PREFIX_VIRTUALTILE = "virt"
}