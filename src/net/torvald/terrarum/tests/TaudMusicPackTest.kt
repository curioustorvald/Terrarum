package net.torvald.terrarum.tests

import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.audio.taudplay.BgmLanes
import net.torvald.terrarum.audio.taudplay.LaneDirector
import net.torvald.terrarum.audio.taudplay.MusicEnvironment
import net.torvald.terrarum.audio.taudplay.MusicPack
import net.torvald.terrarum.audio.taudplay.TaudAudioBank
import java.io.File
import java.util.concurrent.CountDownLatch
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The default music pack (basegame's audio/music/ingame) through the Taud music classes, headless: the manifest and
 * every lane sheet, the faders each kind of sheet sets, the playlist's picks, and songs pulled through
 * [TaudAudioBank] the way the mixer pulls them. Run from the project root.
 *
 * Created by minjaesong on 2026-11-06.
 */
private var fails = 0

private fun check(what: String, ok: Boolean) {
    println((if (ok) "ok   " else "FAIL ") + what)
    if (!ok) fails++
}

private fun near(a: Double, b: Double, eps: Double = 1e-3) = abs(a - b) < eps

fun main() {
    val dir = FileHandle(File("./assets/mods/basegame/audio/music/ingame"))
    val pack = MusicPack.load(dir)
    val byFile = pack.songs.associateBy { it.file.removeSuffix("_master.taud") }

    // ── the manifest ──
    check("pack: name, 9 songs, 5 themes", pack.name == "Terrarum Ingame Music Pack" && pack.songs.size == 9 && pack.themes.size == 5)
    check("pack: every song has its lane sheet", pack.songs.all { it.lanes != null })
    check("pack: 3 preferences, each one song ×2", pack.preferences.size == 3 && pack.preferences.all { it.prefer.size == 1 && it.prefer[0].second == 2.0 })
    check("sheets: kinds", byFile["Byrjing"]!!.lanes!!.kind == BgmLanes.Kind.COMBINATORIAL &&
        listOf("Middag", "Tysdag", "Verddag").all { byFile[it]!!.lanes!!.kind == BgmLanes.Kind.DUAL } &&
        listOf("Fredag", "Mondag", "Sundag", "Torsdag").all { byFile[it]!!.lanes!!.kind == BgmLanes.Kind.BIOME } &&
        byFile["Laurdag"]!!.lanes!!.kind == BgmLanes.Kind.GENERIC)
    check("sheets: titles", pack.songs.all { it.title == it.file.removeSuffix("_master.taud") })
    run {
        val req = com.badlogic.gdx.utils.JsonReader().parse(dir.child(MusicPack.MANIFEST).readString("UTF-8")).get("global_dsp_request")
        val want = (req?.getDouble("volume_amp", 1.0) ?: 1.0) * Math.pow(10.0, (req?.getDouble("volume_db", 0.0) ?: 0.0) / 20.0)
        check("dsp request: the manifest's ×${"%.4f".format(want)}", near(pack.dspRequest.gain, want, 1e-9))
    }
    check("dsp request: +3.01 dB = ×1.4142", near(MusicPack.DspRequest(3.01).gain, 1.41416, 1e-4))
    check("dsp request: every song of every playlist carries it", (pack.titlePlaylist().musicList + pack.ingamePlaylist().musicList)
        .all { near(it.preGain.toDouble(), pack.dspRequest.gain, 1e-6) })
    check("dsp request: volume_db and volume_amp multiply", near(MusicPack.DspRequest(-6.0, 2.0).gain, 2.0 * 0.501187, 1e-5))

    // ── dual: equal-power crossfade, core untouched ──
    val middag = LaneDirector(byFile["Middag"]!!.lanes!!, 32)
    val m0 = middag.start(0.0); val m1 = middag.gainsAt(1.0); val mh = middag.gainsAt(0.5)
    check("Middag x=0: open lanes 1, cave lanes 0", (0..6).all { m0[it] == 1.0 } && (7..12).all { near(m0[it], 0.0) })
    check("Middag x=1: open lanes 0, cave lanes 1", (0..6).all { near(m1[it], 0.0) } && (7..12).all { near(m1[it], 1.0) })
    check("Middag x=0.5: every lane 0.707", (0..12).all { near(mh[it], sqrt(0.5)) })
    check("Middag: follows surroundings, fades 1.5 s", middag.followsSurroundings && middag.fadeSeconds == 1.5)
    val tysdag = LaneDirector(byFile["Tysdag"]!!.lanes!!, 32)
    check("Tysdag: core lanes 1 at any x", listOf(0.0, 0.3, 1.0).all { x -> tysdag.gainsAt(x).let { it[0] == 1.0 && it[1] == 1.0 } })
    check("Tysdag x=0.3: open cos, cave sin", tysdag.gainsAt(0.3).let { near(it[2], Math.cos(0.3 * Math.PI / 2)) && near(it[8], Math.sin(0.3 * Math.PI / 2)) })

    // ── hints ──
    val laurdag = LaneDirector(byFile["Laurdag"]!!.lanes!!, 32)
    val l0 = laurdag.gainsAt(0.0); val l1 = laurdag.gainsAt(1.0)
    check("Laurdag open: sparkle -2 dB, beds 0 dB", near(l0[8], 0.7943) && l0[6] == 1.0 && l0[0] == 1.0)
    check("Laurdag cave: beds -4 dB, sparkle 0 dB", near(l1[6], 0.6310) && near(l1[7], 0.6310) && l1[8] == 1.0)
    check("Fredag open: as written", LaneDirector(byFile["Fredag"]!!.lanes!!, 32).gainsAt(0.0).all { it == 1.0 })

    // ── combinatorial: the draw ──
    val byrjing = byFile["Byrjing"]!!.lanes!!
    val draws = LaneDirector(byrjing, 64, Random(7))
    check("Byrjing: never follows surroundings, no fade", !draws.followsSurroundings && draws.fadeSeconds == 0.0)
    var drawOk = true
    val answerK = IntArray(2); val sparkleK = IntArray(3)
    repeat(4000) {
        val g = draws.start(0.0)
        val on = { lanes: IntRange -> lanes.count { g[it] > 0.0 } }
        if (on(0..7) != 1 || on(12..21) != 2 || on(22..25) != 1) drawOk = false
        if (on(8..11) > 1 || on(26..35) !in listOf(0, 2) || on(36..39) > 2) drawOk = false
        if ((40 until 64).any { g[it] != 0.0 }) drawOk = false
        val sp = on(36..39)
        if (sp == 2 && (36..39).any { g[it] != 0.0 && !near(g[it], 0.84) }) drawOk = false
        if (sp == 1 && (36..39).any { g[it] != 0.0 && g[it] != 1.0 }) drawOk = false
        answerK[on(8..11)]++
        sparkleK[sp]++
        if (draws.lastDraw.keys.toList() != listOf("tune", "answer", "pulse", "bass", "bed", "sparkle")) drawOk = false
    }
    check("Byrjing: 4000 draws, each one tune, one pulse pair, one bass, ≤1 answer, ≤1 bed pair, ≤2 sparkles at 0.84", drawOk)
    check("Byrjing: answer drawn 3 times in 4 (${answerK[1]}/4000)", abs(answerK[1] / 4000.0 - 0.75) < 0.03)
    check("Byrjing: sparkles 0/1/2 as 1:2:2 (${sparkleK.toList()})", abs(sparkleK[0] / 4000.0 - 0.2) < 0.03 && abs(sparkleK[2] / 4000.0 - 0.4) < 0.03)

    // ── the playlist's picks ──
    val rng = Random(11)
    var afterOther = 0; var sundagAfterOther = 0 // picks that followed a song other than Sundag
    fun picks(x: Double, biomes: Set<String>, n: Int = 3000): Map<String, Int> {
        MusicEnvironment.enclosure = x
        MusicEnvironment.biomes = biomes
        val pl = pack.playlist(pack.themes.keys.filter { it != MusicPack.THEME_TITLE }, random = rng)
        val counts = HashMap<String, Int>()
        var last: Any? = null
        var repeats = 0
        repeat(n) {
            val s = pl.queueNext()
            if (s === last) repeats++
            if (last != null && (last as net.torvald.terrarum.audio.AudioBank).name != "Sundag") {
                afterOther++
                if (s.name == "Sundag") sundagAfterOther++
            }
            last = s
            counts[s.name] = (counts[s.name] ?: 0) + 1
        }
        counts["repeats"] = repeats
        return counts
    }
    val open = picks(0.0, emptySet())
    check("open air: no cave piece, no title piece, no repeat ($open)", open["Mondag"] == null && open["Torsdag"] == null && open["Byrjing"] == null && open["repeats"] == 0)
    val cave = picks(1.0, emptySet())
    check("underground: no open-air piece ($cave)", cave["Sundag"] == null && cave["Fredag"] == null && cave["repeats"] == 0)
    check("lowlands: Sundag weighs 2, the others 1", pack.pickWeight(byFile["Sundag"]!!, 0.0, setOf("lowlands")) == 2.0 &&
        pack.pickWeight(byFile["Laurdag"]!!, 0.0, setOf("lowlands")) == 1.0 && pack.pickWeight(byFile["Sundag"]!!, 0.0, emptySet()) == 1.0)
    afterOther = 0; sundagAfterOther = 0
    picks(0.0, setOf("lowlands"), 20000)
    // six songs in the open air, weights 2 (Sundag) and 1 × 5, less the one just played (1): Sundag 2 in 6
    val pSundag = sundagAfterOther.toDouble() / afterOther
    check("lowlands: after another song, Sundag picked 2 times in 6 (${"%.3f".format(pSundag)})", abs(pSundag - 2.0 / 6.0) < 0.015)
    val title = pack.titlePlaylist()
    check("title playlist: Byrjing, continuous", title.musicList.map { it.name } == listOf("Byrjing") && title.diskJockeyingMode == "continuous" && title.queueNext().name == "Byrjing")

    // ── songs through TaudAudioBank, pulled like the mixer pulls them ──
    MusicEnvironment.biomes = emptySet()
    MusicEnvironment.enclosure = 0.0
    val bank = TaudAudioBank("Middag", dir.child("Middag_master.taud"), byFile["Middag"]!!.lanes)
    val bufL = FloatArray(1152); val bufR = FloatArray(1152)
    var silentReads = 0
    while (bank.currentStream == null && silentReads < 2000) { bank.readSamples(bufL, bufR); silentReads++; Thread.sleep(2) }
    check("bank: loads off the reading thread ($silentReads silent reads)", bank.currentStream != null)
    var energy = 0.0
    repeat(200) { if (bank.readSamples(bufL, bufR) > 0) bufL.forEach { energy += it * it } }
    check("bank: sounds once loaded", energy > 0.0)
    fun gains(): DoubleArray {
        val out = DoubleArray(13)
        val latch = CountDownLatch(1)
        bank.currentStream!!.runOnRenderThread { p -> for (v in 0 until 13) out[v] = p.getVoiceGain(v); latch.countDown() }
        while (latch.count > 0) { bank.readSamples(bufL, bufR) }
        return out
    }
    val g0 = gains()
    check("bank x=0: open arrangement up, cave down", (0..6).all { g0[it] == 1.0 } && (7..12).all { g0[it] == 0.0 })
    MusicEnvironment.enclosure = 1.0
    repeat(48000 * 20 / 1152) { bank.readSamples(bufL, bufR) } // 20 s: past the smoothing and the fade
    val g1 = gains()
    check("bank x→1 (20 s later): crossfaded to the cave arrangement", (0..6).all { g1[it] == 0.0 } && (7..12).all { g1[it] == 1.0 })
    bank.reset()
    check("bank: reset lets the song go", bank.currentStream == null && bank.currentPositionInSamples() == 0L)

    // every fader at 1 (Fredag in the open air): the bank is the plain player, bit for bit
    MusicEnvironment.enclosure = 0.0
    val fredag = TaudAudioBank("Fredag", dir.child("Fredag_master.taud"), byFile["Fredag"]!!.lanes)
    while (fredag.currentStream == null) { fredag.readSamples(bufL, bufR); Thread.sleep(2) }
    val direct = net.torvald.taudplay.TaudPlayer(dir.child("Fredag_master.taud").readBytes())
    direct.play()
    val want = FloatArray(48000 * 30 * 2)
    run {
        var at = 0
        while (at < want.size) { val b = direct.renderChunk() ?: break; System.arraycopy(b, 0, want, at, minOf(b.size, want.size - at)); at += b.size }
    }
    var same = true
    for (r in 0 until want.size / 2 / 1152) {
        fredag.readSamples(bufL, bufR)
        for (k in 0 until 1152) {
            val i = (r * 1152 + k) * 2
            if (bufL[k].toRawBits() != want[i].toRawBits() || bufR[k].toRawBits() != want[i + 1].toRawBits()) same = false
        }
    }
    check("bank: Fredag in the open air = the plain player, bit for bit (30 s)", same)
    fredag.reset()

    val verddag = TaudAudioBank("Verddag", dir.child("Verddag_master.taud"), byFile["Verddag"]!!.lanes)
    while (verddag.currentStream == null) { verddag.readSamples(bufL, bufR); Thread.sleep(2) }
    var frames = 0L
    var reads = 0
    while (true) {
        val got = verddag.readSamples(bufL, bufR)
        if (got <= 0) break
        if (verddag.currentStream != null) frames += got
        if (++reads > 48000 * 400 / 1152) break
    }
    check("bank: Verddag plays through and ends (${"%.2f".format(frames / 48000.0)} s)", abs(frames / 48000.0 - 172.09) < 0.1)
    check("bank: ended reads stay 0", verddag.readSamples(bufL, bufR) == 0)
    verddag.reset()

    val byrjingBank = TaudAudioBank("Byrjing", dir.child("Byrjing_master.taud"), byrjing)
    while (byrjingBank.currentStream == null) { byrjingBank.readSamples(bufL, bufR); Thread.sleep(2) }
    val draw1 = byrjingBank.currentDraw
    check("bank: Byrjing drew at its start ($draw1)", draw1["tune"]?.size == 1 && draw1["pulse"]?.size == 1)
    byrjingBank.dispose()

    println(if (fails == 0) "all passed" else "$fails FAILED")
    System.exit(if (fails == 0) 0 else 1)
}
