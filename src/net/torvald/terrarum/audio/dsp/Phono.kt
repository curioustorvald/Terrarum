package net.torvald.terrarum.audio.dsp

import net.torvald.terrarum.ModMgr
import java.io.File

/**
 * Crackle and pops of the phonographs.
 *
 * Created by minjaesong on 2024-01-24.
 */
class Phono(ir: File, crossfeed: Float, gain: Float) : LoFi(
    ModMgr.getFile("basegame", "audio/effects/static/phono_pops.ogg"),
    ir, crossfeed, gain
)

/**
 * Hiss of the magnetic tape.
 *
 * Created by minjaesong on 2024-01-24.
 */
class Tape(ir: File, crossfeed: Float, gain: Float) : LoFi(
    ModMgr.getFile("basegame", "audio/effects/static/tape_hiss.ogg"),
    ir, crossfeed, gain
)

/**
 * Static noise of the fictional Holotape, based on RCA Holotape and not Fallout Holotape.
 *
 * You can argue "high-tech storage medium like Holotape should hold digital audio" but where's the fun in that?
 *
 * Created by minjaesong on 2024-01-24.
 */
class Holo(ir: File, crossfeed: Float, gain: Float) : LoFi(
    ModMgr.getFile("basegame", "audio/effects/static/film_pops.ogg"),
    ir, crossfeed, gain
)