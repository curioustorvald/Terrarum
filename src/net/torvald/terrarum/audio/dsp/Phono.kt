package net.torvald.terrarum.audio.dsp

import net.torvald.terrarum.ModMgr

/**
 * Crackle and pops of the phonographs.
 *
 * Created by minjaesong on 2024-01-24.
 */
class Phono(irModule: String, irPath: String, crossfeed: Float, gain: Float, saturationLim: Float) : LoFi(
    "basegame", "audio/effects/static/phono_pops.ogg",
    irModule, irPath, crossfeed, gain, saturationLim
)

/**
 * Hiss of the magnetic tape.
 *
 * Created by minjaesong on 2024-01-24.
 */
class Tape(irModule: String, irPath: String, crossfeed: Float, gain: Float, saturationLim: Float) : LoFi(
    "basegame", "audio/effects/static/tape_hiss.ogg",
    irModule, irPath, crossfeed, gain, saturationLim
)

/**
 * Static noise of the fictional Holotape, based on RCA Holotape and not Fallout Holotape.
 *
 * You can argue "high-tech storage medium like Holotape should hold digital audio" but where's the fun in that?
 *
 * Created by minjaesong on 2024-01-24.
 */
class Holo(irModule: String, irPath: String, crossfeed: Float, gain: Float, saturationLim: Float) : LoFi(
    "basegame", "audio/effects/static/film_pops_lowpass.ogg",
    irModule, irPath, crossfeed, gain, saturationLim
)