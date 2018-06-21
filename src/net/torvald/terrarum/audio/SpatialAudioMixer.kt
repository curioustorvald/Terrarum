package net.torvald.terrarum.audio

/**
 * Mixes spacial audio sources to multiple channels
 *
 *
 * Channels and their mapping:
 *
 * Notation: (front/rear.subwoofer/top-front)
 * Plugs: G-Front (green), K-Rear (black), Y-Centre/Subwoofer (yellow), E-Side (grey)
 *        e.g. E-RC,NULL means GREY jack outputs REAR-CENTRE to its left and nothing to its right channel.
 *
 * = Headphones: Binaural
 * = Stereo ---------- (2/0.0/0): G-FL,FR
 * = Quadraphonic ---- (2/2.0/0): G-FL,FR; K-RL,RR
 * = 4.0 ------------- (3/1.0/0): G-FL,FR; Y-FC,RC
 * = 5.1 ------------- (3/2.1/0): G-FL,FR; Y-FC,SW; K-RL,RR
 * = 6.1 ------------- (3/3.1/0): G-FL,FR; Y-FC,SW; K-RL,RR, E-RC,RC
 * = 7.1 ------------- (3/4.1/0): G-FL,FR; Y-FC,SW; K-RL,RR, E-SL,SR
 * = Dolby Atmos*5.1.2 (3/2.1/2): G-FL,FR; Y-FC,SW; K-RL,RR, E-TL,TR
 *
 * = Extra options:
 * = No centre speaker (5.1 and above)
 * = No subwoofer (5.1 and above)
 *
 * Channel uses:
 *
 * - Front and centre: usual thingies
 * - Rear: weather/ambient if 4.0, channel phantoming otherwise
 * - Top/Side: weather/ambient
 * - Side: extreme pan for front channels
 * - Centre: interface/UI
 *
 * * If both side and rear speakers are not there, play weather/ambient to the stereo speakers but NOT TO THE CENTRE
 * * For non-existent speakers, use channel phantoming
 *
 * Note: 5.1.2 does NOT output Dolby-compatible signals. It's just a customised 8 channel setup.
 *
 * @see spatialAudioMixMat.xlsx
 */
object SpatialAudioMixer {

    const val PANNING_THREE = 0.708f
    const val PANNING_FOUR_POINT_FIVE = 0.596f
    const val PANNING_SIX = 0.5f

    fun getPanning(coefficient: Float, panningLaw: Float = PANNING_FOUR_POINT_FIVE): Float {
        val k = panningLaw.toDouble()
        val a = 2.0 - 2.0 * k
        val b = 4.0 * k - 1.0

        return (coefficient*a*a + coefficient*b).toFloat()
    }


    const val PRESET_QUADRAPHONIC = """
FL=1,0,0,0,0,0,0,0
FC=0.5,0.5,0,0,0,0,0,0
FR=0,1,0,0,0,0,0,0
RL=0,0,0,0,0.75,0.25,0,0
RR=0,0,0,0,0.25,0.75,0,0
SL=0.25,0,0,0,0.75,0,0,0
RC=0,0,0,0,0.5,0.5,0,0
SR=0,0.25,0,0,0,0.75,0,0
AMB=0.25,0.25,0,0,0.25,0.25,0,0
LFE=0,0,0,0,0.5,0.5,0,0
"""
    const val PRESET_FIVE_POINT_ONE = """
FL=1,0,0,0,0,0,0,0
FC=0,0,1,0,0,0,0,0
FR=0,1,0,0,0,0,0,0
RL=0,0,0,0,0.75,0.25,0,0
RR=0,0,0,0,0.25,0.75,0,0
SL=0.25,0,0,0,0.75,0,0,0
RC=0,0,0,0,0.5,0.5,0,0
SR=0,0.25,0,0,0,0.75,0,0
AMB=0.25,0.25,0,0,0.25,0.25,0,0
LFE=0,0,0,1,0,0,0,0
"""

    const val PRESET_SEVEN_POINT_ONE = """
FL=1,0,0,0,0,0,0,0
FC=0,0,1,0,0,0,0,0
FR=0,1,0,0,0,0,0,0
SL=0,0,0,0,0,0,1,0
SR=0,0,0,0,0,0,0,1
RL=0,0,0,0,1,0,0,0
RC=0,0,0,0,0.5,0.5,0,0
RR=0,0,0,0,0,1,0,0
AMB=0.125,0.125,0,0,0.125,0.125,0.25,0.25
LFE=0,0,0,1,0,0,0,0
"""

    const val PRESET_FIVE_POINT_ONE_POINT_TWO = """
FL=1,0,0,0,0,0,0,0
FC=0,0,1,0,0,0,0,0
FR=0,1,0,0,0,0,0,0
RL=0,0,0,0,0.75,0.25,0,0
RR=0,0,0,0,0.25,0.75,0,0
SL=0.25,0,0,0,0.75,0,0,0
RC=0,0,0,0,0.5,0.5,0,0
RR=0,0.25,0,0,0,0.75,0,0
AMB=0,0,0,0,0.125,0.125,0.375,0.375
LFE=0,0,0,1,0,0,0,0
"""

    const val PRESET_FOUR_POINT_ONE = """
FL=0.75,0.25,0,0,0,0,0,0
FC=0,0,1,0,0,0,0,0
FR=0.25,0.75,0,0,0,0,0,0
SL=1,0,0,0,0,0,0,0
SR=0,1,0,0,0,0,0,0
RL=0.5,0,0,0,0.5,0,0,0
RC=0,0,0,0,1,0,0,0
RR=0,0.5,0,0,0.5,0,0,0
AMB=0.25,0.25,0,0,0.5,0,0,0
LFE=0,0,0,1,0,0,0,0
"""

    const val PRESET_SIX_POINT_ONE = """
FL=1,0,0,0,0,0,0,0
FC=0,0,1,0,0,0,0,0
FR=0,1,0,0,0,0,0,0
RL=0,0,0,0,0.75,0.25,0,0
RR=0,0,0,0,0.25,0.75,0,0
SL=0.25,0,0,0,0.75,0,0,0
RC=0,0,0,0,0,0,1,0
SR=0,0.25,0,0,0,0.75,0,0
AMB=0.25,0.25,0,0,0.25,0.25,0,0
LFE=0,0,0,1,0,0,0,0
"""

    const val PRESET_STEREO = """
FL=0.75,0.25,0,0,0,0,0,0
FC=0.5,0.5,0,0,0,0,0,0
FR=0.25,0.75,0,0,0,0,0,0
SL=1,0,0,0,0,0,0,0
SR=0,1,0,0,0,0,0,0
RL=1,0,0,0,0,0,0,0
RC=0.5,0.5,0,0,0,0,0,0
RR=0,1,0,0,0,0,0,0
AMB=0.5,0.5,0,0,0,0,0,0
LFE=0.5,0.5,0,0,0,0,0,0
"""



}