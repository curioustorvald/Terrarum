package net.torvald.terrarum.audio.taudplay

/**
 * What the music knows of the player's surroundings. The game writes it (the ingame's update; the title screen sets
 * the open air); the Taud songs read it from the audio thread.
 *
 * Created by minjaesong on 2026-11-06.
 */
object MusicEnvironment {
    /**
     * 0 = in the open … 1 = enclosed in rock: the balance the game sets between the open and the cave convolvers
     * (TerrarumIngame's 73×73 audio reflection survey), unsmoothed. Every `dual` song crossfades its arrangements
     * by it, and every song with hints leans on it; each song smooths it by itself.
     */
    @Volatile var enclosure: Double = 0.0

    /**
     * The biomes the player is in, by the music pack's names (`_muspackmanifest.json`'s `probabilities`, e.g.
     * "lowlands", "mountains", "deep_underground"). Nothing in the game classifies biomes at run time yet, so this
     * stays empty unless something sets it; the pack's preferences then simply do not apply.
     */
    @Volatile var biomes: Set<String> = emptySet()

    /** Whether the player counts as underground when a song is picked. */
    val isEnclosed: Boolean get() = enclosure >= 0.5
}
