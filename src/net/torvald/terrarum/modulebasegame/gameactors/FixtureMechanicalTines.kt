package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.audio.audiobank.AudioBankMusicBox
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2024-04-15.
 */
class FixtureMechanicalTines : Electric {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        nameFun = { Lang["ITEM_MECHANICAL_TINES"] }
    )

    @Transient private val audioBank = AudioBankMusicBox()

    @Transient private val track = App.audioMixer.getFreeTrackNoMatterWhat()

    init {
        track.trackingTarget = this
        track.currentTrack = audioBank
        track.play()


        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/mechanical_tines.tga")
        density = 1400.0
        setHitboxDimension(TILE_SIZE * 2, TILE_SIZE * 2, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE * 2, TILE_SIZE * 2)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0


        setWireSinkAt(0, 1, "digital_bit")
        setWireSinkAt(1, 1, "network")

        despawnHook = {
            track.stop()
            track.currentTrack = null
            track.trackingTarget = null
            audioBank.dispose()
        }
    }

    @Transient private var testRollCursor = 0

    override fun updateSignal() {
        // TODO update using network port


        if (isSignalHigh(0, 1)) {
            // advance every tick
            audioBank.sendMessage(testNotes[testRollCursor])
            testRollCursor = (testRollCursor + 1) % testNotes.size
        }
    }



    companion object {
        @Transient private val TICK_DIVISOR = 10

        @Transient val testNotes = List(16*TICK_DIVISOR) { 0L } +
                prel(24,28,31,36,40) +
                prel(24,26,33,38,41) +
                prel(23,26,31,38,41) +
                prel(24,28,31,36,40) +
                prel(24,28,33,40,45) +
                prel(24,26,30,33,38) +
                prel(23,26,31,38,43) +
                prel(23,24,28,31,36) +
                prel(21,24,28,31,36) +
                prel(14,21,26,30,36) +
                prel(19,23,26,31,35) +
                prel(19,22,28,31,37) +
                prel(17,21,26,33,38) +
                prel(17,20,26,29,35) +
                prel(16,19,24,31,36) +
                prel(16,17,21,24,29) +
                prel(14,17,21,24,29) +
                prel( 7,14,19,23,29) +
                prel(12,16,19,24,28) +
                prel(12,19,22,24,28) +
                prel( 5,17,21,24,28) +
                prel( 6,12,21,24,27) +
                prel( 8,17,23,24,26) +
                prel( 7,17,19,23,26) +
                prel( 7,16,19,24,28) +
                prel( 7,14,19,24,29) +
                prel( 7,14,19,23,29) +
                prel( 7,15,21,24,30) +
                prel( 7,16,19,24,31) +
                prel( 7,14,19,24,29) +
                prel( 7,14,19,23,29) +
                prel( 0,12,19,22,28) +
                end1( 0,12,17,21,24,29,21,17,14) +
                end2( 0,11,31,35,38,41,26,29,28) +
                end3( 0,12,28,31,36) + List(16*TICK_DIVISOR - 5) { 0L }

        private fun prel(n1: Int, n2: Int, n3: Int, n4: Int, n5: Int): List<Long> {
            return toPianoRoll2(
                1L shl n1, 1L shl n2, 1L shl n3, 1L shl n4, 1L shl n5, 1L shl n3, 1L shl n4, 1L shl n5,
                1L shl n1, 1L shl n2, 1L shl n3, 1L shl n4, 1L shl n5, 1L shl n3, 1L shl n4, 1L shl n5)
        }

        private fun end1(n1: Int, n2: Int, n3: Int, n4: Int, n5: Int, n6: Int, n7: Int, n8: Int, n9: Int): List<Long> {
            return toPianoRoll2(
                1L shl n1, 1L shl n2, 1L shl n3, 1L shl n4, 1L shl n5, 1L shl n6, 1L shl n5, 1L shl n4,
                1L shl n5, 1L shl n7, 1L shl n8, 1L shl n7, 1L shl n8, 1L shl n9, 1L shl n8, 1L shl n9)
        }

        private fun end2(n1: Int, n2: Int, n3: Int, n4: Int, n5: Int, n6: Int, n7: Int, n8: Int, n9: Int): List<Long> {
            return toPianoRoll(
                1L shl n1 to TICK_DIVISOR, 1L shl n2 to TICK_DIVISOR+1, 1L shl n3 to TICK_DIVISOR+1, 1L shl n4 to TICK_DIVISOR+1,
                1L shl n5 to TICK_DIVISOR+1, 1L shl n6 to TICK_DIVISOR+2, 1L shl n5 to TICK_DIVISOR+2, 1L shl n4 to TICK_DIVISOR+2,
                1L shl n5 to TICK_DIVISOR+3, 1L shl n4 to TICK_DIVISOR+3, 1L shl n3 to TICK_DIVISOR+4, 1L shl n4 to TICK_DIVISOR+4,
                1L shl n7 to TICK_DIVISOR+6, 1L shl n8 to TICK_DIVISOR+8, 1L shl n9 to TICK_DIVISOR+16, 1L shl n7 to TICK_DIVISOR+32)
        }

        private fun end3(vararg ns: Int): List<Long> {
            return ns.map { 1L shl it } // arpeggiate
        }

        private fun toPianoRoll(vararg noteAndLen: Pair<Long, Int>): List<Long> {
            val ret = MutableList<Long>(noteAndLen.sumOf { it.second }) { 0 }
            var c = 0
            noteAndLen.forEach { (note, len) ->
                ret[c] = note
                c += len
            }
            return ret
        }

        private fun toPianoRoll(vararg notes: Long) = List<Long>(notes.size * TICK_DIVISOR) {
            if (it % TICK_DIVISOR == 0) notes[it / TICK_DIVISOR] else 0
        }

        private fun toPianoRoll2(vararg notes: Long) = List<Long>(notes.size * TICK_DIVISOR) {
            if (it % TICK_DIVISOR == 0) notes[it / TICK_DIVISOR] else 0
        }.let { it.subList(0, 1) + List<Long>(3) { 0L } + it.subList(1, it.size) }
    }
}