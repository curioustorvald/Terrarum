package net.torvald.terrarum


/**
 * See REFERENCING.md
 */
object ReferencingRanges {

    val TILES = 0..4095
    val WALLS = 4096..8191
    val WIRES = 8192..8447
    val ITEMS_STATIC = 8448..0x0F_FFFF
    val ITEMS_DYNAMIC = 0x10_0000..0x0FFF_FFFF
    val ACTORS = 0x1000_0000..0x7FFF_FFFF

    val ACTORS_BEHIND  = 0x1000_0000..0x1FFF_FFFF // Rendered behind (e.g. tapestries)
    val ACTORS_MIDDLE  = 0x2000_0000..0x4FFF_FFFF // Regular actors (e.g. almost all of them)
    val ACTORS_MIDTOP  = 0x5000_0000..0x5FFF_FFFF // Special (e.g. weapon swung, bullets, dropped item, particles)
    val ACTORS_FRONT   = 0x6000_0000..0x6FFF_FFFF // Rendered front (e.g. fake tile)
    val ACTORS_OVERLAY = 0x7000_0000..0x7FFF_FFFF // Rendered as screen overlay, not affected by light nor environment overlays

    val VIRTUAL_TILES = -2 downTo -65536 // index of -1 breaks things for some reason :(

}