package net.torvald.terrarum


/**
 * See REFERENCING.md
 */
object ReferencingRanges {

    val TILES = 0..65535 // 65 536 blocks
    val WALLS = 65536..131071 // 65 536 walls
    val ITEMS_STATIC = 135168..0x0F_FFFF // 913 408 items
    val ITEMS_DYNAMIC = 0x10_0000..0x0FFF_FFFF // 267 386 880 pseudo-items
    val ACTORS =  0x1000_0000..0x7FFE_FFFF

    // there is a gap between 0x7FFF_0000..0x7FFF_BFFF

    // IDs doesn't effect the render order at all, but we're kinda enforcing these ID ranging.
    // However, these two wire-related actor will break the rule. But as we want them to render on top of others
    // in the same render orders, we're giveng them relatively high IDs for them.
    val ACTORS_WIRES   = 0x7FFF_C000..0x7FFF_EFFF // Rendered front--wires
    val ACTORS_WIRES_HELPER = 0x7FFF_F000..0x7FFF_FEFF // Rendered overlay--wiring port icons and logic gates

    val ACTORS_INTERNAL_USE = 0x7FFF_FF00..0x7FFF_FFFF // Actor ID 0x7FFF_FFFF is pre-assigned to the block cursor!


    val PREFIX_DYNAMICITEM = "dyn"
    val PREFIX_ACTORITEM = "actor"
    val PREFIX_VIRTUALTILE = "virt"
}