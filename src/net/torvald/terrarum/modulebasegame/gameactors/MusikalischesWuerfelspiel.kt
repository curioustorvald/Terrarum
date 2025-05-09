package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.modulebasegame.gameactors.PreludeInCMaj.toPianoRoll

/**
 * Implementation of Musikalisches Würfelspiel by W. A. Mozart
 *
 * Created by minjaesong on 2025-05-07.
 */
object MusikalischesWuerfelspiel {

    val NIL = 0L
    // Octave 1
    val C1 = 1L shl 0
    val Cs1 = 1L shl 1
    val Db1 = 1L shl 1
    val D1 = 1L shl 2
    val Ds1 = 1L shl 3
    val Eb1 = 1L shl 3
    val E1 = 1L shl 4
    val F1 = 1L shl 5
    val Fs1 = 1L shl 6
    val Gb1 = 1L shl 6
    val G1 = 1L shl 7
    val Gs1 = 1L shl 8
    val Ab1 = 1L shl 8
    val A1 = 1L shl 9
    val As1 = 1L shl 10
    val Bb1 = 1L shl 10
    val B1 = 1L shl 11
    // Octave 2
    val C2 = 1L shl 12
    val Cs2 = 1L shl 13
    val Db2 = 1L shl 13
    val D2 = 1L shl 14
    val Ds2 = 1L shl 15
    val Eb2 = 1L shl 15
    val E2 = 1L shl 16
    val F2 = 1L shl 17
    val Fs2 = 1L shl 18
    val Gb2 = 1L shl 18
    val G2 = 1L shl 19
    val Gs2 = 1L shl 20
    val Ab2 = 1L shl 20
    val A2 = 1L shl 21
    val As2 = 1L shl 22
    val Bb2 = 1L shl 22
    val B2 = 1L shl 23
    // Octave 3
    val C3 = 1L shl 24
    val Cs3 = 1L shl 25
    val Db3 = 1L shl 25
    val D3 = 1L shl 26
    val Ds3 = 1L shl 27
    val Eb3 = 1L shl 27
    val E3 = 1L shl 28
    val F3 = 1L shl 29
    val Fs3 = 1L shl 30
    val Gb3 = 1L shl 30
    val G3 = 1L shl 31
    val Gs3 = 1L shl 32
    val Ab3 = 1L shl 32
    val A3 = 1L shl 33
    val As3 = 1L shl 34
    val Bb3 = 1L shl 34
    val B3 = 1L shl 35
    // Octave 4
    val C4 = 1L shl 36
    val Cs4 = 1L shl 37
    val Db4 = 1L shl 37
    val D4 = 1L shl 38
    val Ds4 = 1L shl 39
    val Eb4 = 1L shl 39
    val E4 = 1L shl 40
    val F4 = 1L shl 41
    val Fs4 = 1L shl 42
    val Gb4 = 1L shl 42
    val G4 = 1L shl 43
    val Gs4 = 1L shl 44
    val Ab4 = 1L shl 44
    val A4 = 1L shl 45
    val As4 = 1L shl 46
    val Bb4 = 1L shl 46
    val B4 = 1L shl 47
    // Octave 5
    val C5 = 1L shl 48
    val Cs5 = 1L shl 49
    val Db5 = 1L shl 49
    val D5 = 1L shl 50
    val Ds5 = 1L shl 51
    val Eb5 = 1L shl 51
    val E5 = 1L shl 52
    val F5 = 1L shl 53
    val Fs5 = 1L shl 54
    val Gb5 = 1L shl 54
    val G5 = 1L shl 55
    val Gs5 = 1L shl 56
    val Ab5 = 1L shl 56
    val A5 = 1L shl 57
    val As5 = 1L shl 58
    val Bb5 = 1L shl 58
    val B5 = 1L shl 59
    // Octave 6
    val C6 = 1L shl 60

    private val TICK_DIVISOR = 10

    private val diceTable = arrayOf(
        intArrayOf(96,32,69,40,148,104,152,119,98,3,54),
        intArrayOf(22,6,95,17,74,157,60,84,142,87,130),
        intArrayOf(141,128,158,113,163,27,171,114,42,165,10),
        intArrayOf(41,63,13,85,45,167,53,50,156,61,103),
        intArrayOf(105,146,153,161,80,154,99,140,75,135,28),
        intArrayOf(122,46,55,2,97,68,133,86,129,47,37),
        intArrayOf(11,134,110,159,36,118,21,169,62,147,106),
        intArrayOf(30,81,24,100,107,91,127,94,123,33,5),
        intArrayOf(70,117,66,90,25,138,16,120,65,102,35),
        intArrayOf(121,39,139,176,143,71,155,88,77,4,20),
        intArrayOf(26,126,15,7,64,150,57,48,19,31,108),
        intArrayOf(9,56,132,34,125,29,175,166,82,164,92),
        intArrayOf(122,174,73,67,76,101,43,51,137,144,12),
        intArrayOf(49,18,58,160,136,162,168,115,38,59,124),
        intArrayOf(109,116,145,52,1,23,89,72,149,173,44),
        intArrayOf(14,83,79,170,93,151,172,111,8,78,131),
    )

    fun generateRoll(): List<Long> {
        val ret = ArrayList<Long>()

        for (i in 0 until 16) {
            val roll = (Math.random() * 11).toInt()
            ret.addAll(BARS[diceTable[i][roll]])
        }

        return ret
    }

    // original idea by W. A. Mozart (https://dice.humdrum.org/)
    // "creative" transcription generated using Claude 3.7 Sonnet
    private val BARS = arrayOf(
        /* Bar 00 */ sixSemiquavers(NIL, NIL, NIL, NIL, NIL, NIL),
        /* Bar 01 */ sixSemiquavers(G2 or F4, NIL, D2 or D4, NIL, G2 or G4, NIL),
        /* Bar 02 */ sixSemiquavers(B1 or G2 or A3, NIL, Fs3, G3, B3, G4),
        /* Bar 03 */ sixSemiquavers(C2 or E1 or G4, NIL, C4, NIL, E4, NIL),
        /* Bar 04 */ sixSemiquavers(C2 or G2 or B3, NIL, G3, A3, B3, D4),
        /* Bar 05 */ sixSemiquavers(D2 or Fs2 or D4, NIL, A3, NIL, D4, NIL),
        /* Bar 06 */ sixSemiquavers(D2 or A2 or Fs4, NIL, D4, E4, Fs4, A4),
        /* Bar 07 */ sixSemiquavers(G2 or B4, NIL, D4, NIL, G4, NIL),
        /* Bar 08 */ sixSemiquavers(G1 or G2 or B3, NIL, D3, NIL, G3, NIL),
        /* Bar 09 */ sixSemiquavers(C2 or E4, NIL, G3, G4, E4, C4),
        /* Bar 10 */ sixSemiquavers(G1 or B1 or G3, NIL, D3, NIL, G3, NIL),
        /* Bar 11 */ sixSemiquavers(A1 or C2 or A3, NIL, E3, A3, C4, E4),
        /* Bar 12 */ sixSemiquavers(D1 or D2 or Fs4, NIL, A3, D4, Fs4, A4),
        /* Bar 13 */ sixSemiquavers(G1 or G3, A3, B3, G3, D4, B3),
        /* Bar 14 */ sixSemiquavers(C2 or C4, B3, A3, G3, Fs3, E3),
        /* Bar 15 */ sixSemiquavers(D2 or Fs3, G3, A3, B3, C4, D4),
        /* Bar 16 */ sixSemiquavers(G1 or B1 or D4, B3, A3, G3, A3, B3),
        /* Bar 17 */ sixSemiquavers(C2 or C4, D4, E4, C4, G4, E4),
        /* Bar 18 */ sixSemiquavers(G1 or G4, F4, Eb4, D4, C4, B3),
        /* Bar 19 */ sixSemiquavers(C2 or C4, D4, Eb4, C4, G3, Eb3),
        /* Bar 20 */ sixSemiquavers(G1 or G3, F3, Eb3, D3, C3, B2),
        /* Bar 21 */ sixSemiquavers(C2 or C3, D3, Eb3, F3, G3, A3),
        /* Bar 22 */ sixSemiquavers(G1 or B3, C4, D4, G3, B3, G3),
        /* Bar 23 */ sixSemiquavers(C3 or E3 or C4, D4, E4, G3, C4, G3),
        /* Bar 24 */ sixSemiquavers(G2 or B2 or G3, D3, G3, B3, G3, D3),
        /* Bar 25 */ sixSemiquavers(C3 or Eb3 or C4, NIL, G3, NIL, Eb4, NIL),
        /* Bar 26 */ sixSemiquavers(Fs3 or A3 or C4, NIL, D3, NIL, C4, NIL),
        /* Bar 27 */ sixSemiquavers(G2 or D3 or B3, NIL, G3, NIL, D4, NIL),
        /* Bar 28 */ sixSemiquavers(G1 or B2 or G3, NIL, D3, NIL, G3, NIL),
        /* Bar 29 */ sixSemiquavers(C2 or E4, NIL, G3, G4, E4, C4),
        /* Bar 30 */ sixSemiquavers(G1 or B1 or G3, NIL, D3, NIL, G3, NIL),
        /* Bar 31 */ sixSemiquavers(A1 or C2 or A3, NIL, E3, A3, C4, E4),
        /* Bar 32 */ sixSemiquavers(D1 or D2 or Fs4, NIL, A3, D4, Fs4, A4),
        /* Bar 33 */ sixSemiquavers(G1 or G3, A3, B3, G3, D4, B3),
        /* Bar 34 */ sixSemiquavers(C2 or C4, B3, A3, G3, Fs3, E3),
        /* Bar 35 */ sixSemiquavers(D2 or Fs3, G3, A3, B3, C4, D4),
        /* Bar 36 */ sixSemiquavers(G1 or B1 or D4, B3, A3, G3, Fs3, G3),
        /* Bar 37 */ sixSemiquavers(C2 or E3, G3, C4, E4, G4, C5),
        /* Bar 38 */ sixSemiquavers(G1 or B1 or D5, NIL, B4, G4, D4, B3),
        /* Bar 39 */ sixSemiquavers(C2 or E3, G3, C4, E4, G4, C5),
        /* Bar 40 */ sixSemiquavers(G1 or B1 or D5, NIL, B4, G4, D4, B3),
        /* Bar 41 */ sixSemiquavers(A1 or C2 or E3, A3, C4, E4, A4, C5),
        /* Bar 42 */ sixSemiquavers(E2 or G2 or E5, NIL, B4, G4, E4, B3),
        /* Bar 43 */ sixSemiquavers(A1 or A2 or C5, NIL, A4, E4, C4, A3),
        /* Bar 44 */ sixSemiquavers(D2 or Fs2 or A4, NIL, Fs4, D4, A3, Fs3),
        /* Bar 45 */ sixSemiquavers(G1 or G2 or B3, D4, G4, B4, G4, D4),
        /* Bar 46 */ sixSemiquavers(G1 or B1 or G4, F4, E4, D4, C4, B3),
        /* Bar 47 */ sixSemiquavers(C2 or C4, D4, E4, G4, C5, G4),
        /* Bar 48 */ sixSemiquavers(G1 or B1 or F4, D4, B3, G3, D3, B2),
        /* Bar 49 */ restPattern(G2 or B2 or G3, G2 or G3, B1 or B2),
        /* Bar 50 */ restPattern(C2 or C3, C2 or C3, D2 or D3),
        /* Bar 51 */ restPattern(D2 or D3, D2 or D3, D1 or D2),
        /* Bar 52 */ restPattern(G1 or G2, G1 or G2, G1 or G2),
        /* Bar 53 */ sixSemiquavers(G2 or G4, NIL, D4, NIL, B3, NIL),
        /* Bar 54 */ sixSemiquavers(C2 or C4, NIL, G3, NIL, E3, NIL),
        /* Bar 55 */ sixSemiquavers(D2 or D4, NIL, A3, NIL, Fs3, NIL),
        /* Bar 56 */ sixSemiquavers(G1 or G3, NIL, D3, NIL, B2, NIL),
        /* Bar 57 */ sixSemiquavers(G2 or G4, NIL, D4, NIL, B3, NIL),
        /* Bar 58 */ sixSemiquavers(C2 or C4, NIL, G3, NIL, E3, NIL),
        /* Bar 59 */ sixSemiquavers(D2 or D4, NIL, A3, NIL, Fs3, NIL),
        /* Bar 60 */ sixSemiquavers(G1 or G3, NIL, D3, NIL, B2, NIL),
        /* Bar 61 */ sixSemiquavers(C3 or C4, D4, E4, G3, C4, E4),
        /* Bar 62 */ sixSemiquavers(G2 or G3, A3, B3, D3, G3, B3),
        /* Bar 63 */ sixSemiquavers(C3 or C4, D4, E4, G3, C4, E4),
        /* Bar 64 */ sixSemiquavers(G2 or G3, A3, B3, D3, G3, B3),
        /* Bar 65 */ sixSemiquavers(C3 or C4, G3, E4, C4, G4, E4),
        /* Bar 66 */ sixSemiquavers(G2 or G3, D3, B3, G3, D4, B3),
        /* Bar 67 */ sixSemiquavers(C3 or C4, G3, E4, C4, G4, E4),
        /* Bar 68 */ sixSemiquavers(G2 or G3, D3, B3, G3, D4, B3),
        /* Bar 69 */ sixSemiquavers(A1 or C4, A3, E4, C4, A4, E4),
        /* Bar 70 */ sixSemiquavers(E2 or B3, G3, E4, B3, G4, E4),
        /* Bar 71 */ sixSemiquavers(A1 or C4, A3, E4, C4, A4, E4),
        /* Bar 72 */ sixSemiquavers(D2 or A3, Fs3, D4, A3, Fs4, D4),
        /* Bar 73 */ sixSemiquavers(G1 or B3, G3, D4, B3, G4, D4),
        /* Bar 74 */ sixSemiquavers(G1 or G3, D3, B3, G3, D4, B3),
        /* Bar 75 */ sixSemiquavers(C2 or C4, G3, E4, C4, G4, E4),
        /* Bar 76 */ sixSemiquavers(G1 or D3, B2, G3, D3, B3, G3),
        /* Bar 77 */ sixSemiquavers(C2 or E3, G3, C4, E3, G3, C4),
        /* Bar 78 */ sixSemiquavers(G1 or D3, G3, B3, D3, G3, B3),
        /* Bar 79 */ restPattern(C2 or C3, C2 or C3, D2 or D3),
        /* Bar 80 */ restPattern(D2 or D3, D2 or D3, D1 or D2),
        /* Bar 81 */ sixSemiquavers(G2 or G4, NIL, D4, NIL, B3, NIL),
        /* Bar 82 */ sixSemiquavers(C2 or C4, NIL, G3, NIL, E3, NIL),
        /* Bar 83 */ sixSemiquavers(D2 or D4, NIL, A3, NIL, Fs3, NIL),
        /* Bar 84 */ sixSemiquavers(G1 or G3, NIL, D3, NIL, B2, NIL),
        /* Bar 85 */ sixSemiquavers(C3 or C4, D4, E4, G3, C4, E4),
        /* Bar 86 */ sixSemiquavers(G2 or G3, A3, B3, D3, G3, B3),
        /* Bar 87 */ sixSemiquavers(A2 or A3, B3, C4, E3, A3, C4),
        /* Bar 88 */ sixSemiquavers(D2 or D3, E3, Fs3, A2, D3, Fs3),
        /* Bar 89 */ sixSemiquavers(G2 or G3, A3, B3, D3, G3, B3),
        /* Bar 90 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 91 */ sixSemiquavers(C3 or C4, B3, C4, G3, E3, C3),
        /* Bar 92 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 93 */ sixSemiquavers(C3 or C4, B3, C4, G3, E3, C3),
        /* Bar 94 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 95 */ sixSemiquavers(C3 or C4, B3, C4, G3, E3, C3),
        /* Bar 96 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 97 */ sixSemiquavers(C3 or C4, B3, C4, E3, G3, C4),
        /* Bar 98 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 99 */ restPattern(C2 or C3, G2 or C3, C3 or E3),
        /* Bar 100 */ restPattern(G1 or G2, G2 or B2, G2 or D3),
        /* Bar 101 */ restPattern(G2 or G3, G2 or B3, G2 or D4),
        /* Bar 102 */ restPattern(C2 or C4, C2 or E4, C2 or G4),
        /* Bar 103 */ restPattern(D2 or A4, D2 or D4, D2 or Fs4),
        /* Bar 104 */ restPattern(G1 or G4, G1 or B3, G1 or D4),
        /* Bar 105 */ sixSemiquavers(G2 or G4, NIL, D4, NIL, B3, NIL),
        /* Bar 106 */ sixSemiquavers(C2 or C4, NIL, G3, NIL, E3, NIL),
        /* Bar 107 */ sixSemiquavers(D2 or D4, NIL, A3, NIL, Fs3, NIL),
        /* Bar 108 */ sixSemiquavers(G1 or G3, NIL, D3, NIL, B2, NIL),
        /* Bar 109 */ sixSemiquavers(G2 or G4, NIL, D4, NIL, B3, NIL),
        /* Bar 110 */ sixSemiquavers(C2 or C4, NIL, G3, NIL, E3, NIL),
        /* Bar 111 */ sixSemiquavers(D2 or D4, NIL, A3, NIL, Fs3, NIL),
        /* Bar 112 */ sixSemiquavers(G1 or G3, NIL, D3, NIL, B2, NIL),
        /* Bar 113 */ sixSemiquavers(C3 or C4, D4, E4, G3, C4, E4),
        /* Bar 114 */ sixSemiquavers(G2 or G3, A3, B3, D3, G3, B3),
        /* Bar 115 */ sixSemiquavers(C3 or C4, D4, E4, G3, C4, E4),
        /* Bar 116 */ sixSemiquavers(G2 or G3, A3, B3, D3, G3, B3),
        /* Bar 117 */ sixSemiquavers(C3 or C4, G3, E4, C4, G4, E4),
        /* Bar 118 */ sixSemiquavers(G2 or G3, D3, B3, G3, D4, B3),
        /* Bar 119 */ sixSemiquavers(C3 or C4, G3, E4, C4, G4, E4),
        /* Bar 120 */ sixSemiquavers(G2 or G3, D3, B3, G3, D4, B3),
        /* Bar 121 */ sixSemiquavers(A1 or C4, A3, E4, C4, A4, E4),
        /* Bar 122 */ sixSemiquavers(E2 or B3, G3, E4, B3, G4, E4),
        /* Bar 123 */ sixSemiquavers(A1 or C4, A3, E4, C4, A4, E4),
        /* Bar 124 */ sixSemiquavers(D2 or A3, Fs3, D4, A3, Fs4, D4),
        /* Bar 125 */ sixSemiquavers(G1 or B3, G3, D4, B3, G4, D4),
        /* Bar 126 */ sixSemiquavers(G1 or G3, D3, B3, G3, D4, B3),
        /* Bar 127 */ sixSemiquavers(C2 or C4, G3, E4, C4, G4, E4),
        /* Bar 128 */ sixSemiquavers(G1 or D3, B2, G3, D3, B3, G3),
        /* Bar 129 */ sixSemiquavers(C2 or E3, G3, C4, E3, G3, C4),
        /* Bar 130 */ sixSemiquavers(G1 or D3, G3, B3, D3, G3, B3),
        /* Bar 131 */ restPattern(C2 or C3, C2 or C3, D2 or D3),
        /* Bar 132 */ restPattern(D2 or D3, D2 or D3, D1 or D2),
        /* Bar 133 */ sixSemiquavers(G2 or G4, NIL, D4, NIL, B3, NIL),
        /* Bar 134 */ sixSemiquavers(C2 or C4, NIL, G3, NIL, E3, NIL),
        /* Bar 135 */ sixSemiquavers(D2 or D4, NIL, A3, NIL, Fs3, NIL),
        /* Bar 136 */ sixSemiquavers(G1 or G3, NIL, D3, NIL, B2, NIL),
        /* Bar 137 */ sixSemiquavers(C3 or C4, D4, E4, G3, C4, E4),
        /* Bar 138 */ sixSemiquavers(G2 or G3, A3, B3, D3, G3, B3),
        /* Bar 139 */ sixSemiquavers(A2 or A3, B3, C4, E3, A3, C4),
        /* Bar 140 */ sixSemiquavers(D2 or D3, E3, Fs3, A2, D3, Fs3),
        /* Bar 141 */ sixSemiquavers(G2 or G3, A3, B3, D3, G3, B3),
        /* Bar 142 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 143 */ sixSemiquavers(C3 or C4, B3, C4, G3, E3, C3),
        /* Bar 144 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 145 */ sixSemiquavers(C3 or C4, B3, C4, G3, E3, C3),
        /* Bar 146 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 147 */ sixSemiquavers(C3 or C4, B3, C4, G3, E3, C3),
        /* Bar 148 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 149 */ sixSemiquavers(C3 or C4, B3, C4, E3, G3, C4),
        /* Bar 150 */ sixSemiquavers(G2 or G3, Fs3, G3, B2, D3, G3),
        /* Bar 151 */ sixSemiquavers(C3 or C4, G3, E4, C4, G4, E4),
        /* Bar 152 */ sixSemiquavers(G2 or G3, D3, B3, G3, D4, B3),
        /* Bar 153 */ sixSemiquavers(C3 or C4, G3, E4, C4, G4, E4),
        /* Bar 154 */ sixSemiquavers(G2 or G3, D3, B3, G3, D4, B3),
        /* Bar 155 */ sixSemiquavers(A1 or C4, A3, E4, C4, A4, E4),
        /* Bar 156 */ sixSemiquavers(E2 or B3, G3, E4, B3, G4, E4),
        /* Bar 157 */ sixSemiquavers(A1 or C4, A3, E4, C4, A4, E4),
        /* Bar 158 */ sixSemiquavers(D2 or A3, Fs3, D4, A3, Fs4, D4),
        /* Bar 159 */ sixSemiquavers(G1 or B3, G3, D4, B3, G4, D4),
        /* Bar 160 */ sixSemiquavers(G1 or G3, D3, B3, G3, D4, B3),
        /* Bar 161 */ sixSemiquavers(C2 or C4, G3, E4, C4, G4, E4),
        /* Bar 162 */ sixSemiquavers(G1 or D3, B2, G3, D3, B3, G3),
        /* Bar 163 */ sixSemiquavers(C2 or E3, G3, C4, E3, G3, C4),
        /* Bar 164 */ sixSemiquavers(G1 or D3, G3, B3, D3, G3, B3),
        /* Bar 165 */ restPattern(C2 or C3, C2 or C3, D2 or D3),
        /* Bar 166 */ restPattern(D2 or D3, D2 or D3, D1 or D2),
        /* Bar 167 */ sixSemiquavers(G2 or B2 or G3, NIL, D3, G3, B3, D4),
        /* Bar 168 */ sixSemiquavers(C2 or C4, NIL, G3, C4, E4, G4),
        /* Bar 169 */ sixSemiquavers(D2 or Fs4, NIL, D4, A3, D4, Fs4),
        /* Bar 170 */ sixSemiquavers(G1 or G2 or G4, NIL, D4, B3, G3, D3),
        /* Bar 171 */ sixSemiquavers(C2 or C4, D4, E4, G3, C4, E4),
        /* Bar 172 */ sixSemiquavers(G1 or G3, A3, B3, D3, G3, B3),
        /* Bar 173 */ sixSemiquavers(D2 or D4, E4, Fs4, A3, D4, Fs4),
        /* Bar 174 */ sixSemiquavers(G1 or G4, D4, B3, G3, D3, B2),
        /* Bar 175 */ sixSemiquavers(C2 or C3, G2, E3, C3, G2, E2),
        /* Bar 176 */ sixSemiquavers(G1 or G2, D2, B1, G1, G2, B2),
    )

    private fun sixSemiquavers(ns1: Long, ns2: Long, ns3: Long, ns4: Long, ns5: Long, ns6: Long): List<Long> {
        return toPianoRoll(
            ns1 to TICK_DIVISOR,
            ns2 to TICK_DIVISOR,
            ns3 to TICK_DIVISOR,
            ns4 to TICK_DIVISOR,
            ns5 to TICK_DIVISOR,
            ns6 to TICK_DIVISOR
        )
    }

    private fun fourSemiQuaver(ns1: Long, ns2: Long, ns3: Long, ns4: Long, ns5: Long): List<Long> {
        return toPianoRoll(
            ns1 to TICK_DIVISOR,
            ns2 to TICK_DIVISOR,
            ns3 to TICK_DIVISOR,
            ns4 to TICK_DIVISOR,
            ns5 to TICK_DIVISOR * 2
        )
    }

    private fun restPattern(ns1: Long, ns2: Long, ns3: Long): List<Long> {
        return toPianoRoll(
            ns1 to TICK_DIVISOR * 2,
            NIL to TICK_DIVISOR,
            ns2 to TICK_DIVISOR,
            ns3 to TICK_DIVISOR * 2
        )
    }

}