package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarumsansbitmap.gdx.GameFontBase


/**
 * Created by minjaesong on 2017-06-11.
 */
class TestTestTest : ApplicationAdapter() {

    lateinit var batch: SpriteBatch
    lateinit var img: Texture

    lateinit var gameFont: BitmapFont

    override fun create() {
        batch = SpriteBatch()
        img = Texture("assets/test_texture.tga")

        gameFont = GameFontBase("assets/graphics/fonts")
        //gameFont = BitmapFont()

    }

    val text = arrayOf(
            "x64またはx86-64とは、x86アーキテクチャを64ビットに拡張した命令セットアーキテクチャ。",
            "実際には、AMDが発表したAMD64命令セット、続けてインテルが採用したIntel 64命令セット (かつてIA-32eまたはEM64Tと呼ばれていた)",
            "などを含む、各社のAMD64互換命令セットの総称である。x86命令セットと互換性を持っていることから、広義にはx86にx64を含む場合がある。",
            "",
            "x86-64는 x86 명령어 집합 아키텍처의 64비트 모임이다. x86-64 명령어 집합은 에뮬레이션 없이 인텔의 x86를 지원하며 AMD64로 이름 붙인",
            "AMD에 의해 고안되었다. 이 아키텍처는 인텔 64라는 이름으로 인텔에 의해 복제되기도 했다. (옘힐, 클래카마스 기술, CT, IA-32e, EM64T 등으로",
            "불렸음) 이로써 x86-64 또는 x64의 이름을 일상적으로 사용하기에 이르렀다.",
            "",
            "x86-64 (также AMD64/Intel64/EM64T) — 64-битное расширение, набор команд для архитектуры x86, разработанное",
            "компанией AMD, позволяющее выполнять программы в 64-разрядном режиме. Это расширение архитектуры x86 с",
            "почти полной обратной совместимостью.",
            "",
            "Επίσης η x86-64 έχει καταχωρητές γενικής χρήσης 64-bit και πολλές άλλες βελτιώσεις. Η αρχική προδιαγραφή",
            "δημιουργήθηκε από την AMD και έχει υλοποιηθεί από την AMD, την Intel, τη VIA και άλλες εταιρείες. Διατηρεί πλήρη",
            "συμβατότητα προς τα πίσω με κώδικα 32-bit.",
            "",
            "x86-64 (簡稱x64) 是64位版本的x86指令集，向后相容於16位及32位的x86架構。x64於1999年由AMD設計，AMD首次公開",
            "64位元集以擴充給x86，稱為「AMD64」。其後也為英特爾所採用，現時英特爾稱之為「Intel 64」，在之前曾使用過「Clackamas",
            "Technology」 (CT)、「IA-32e」及「EM64T」",
            "",
            "x86-64, ou x64, est une extension du jeu d'instructions x86 d'Intel, introduite par la société AMD avec la gamme",
            "AMD64. Intel utilisera cette extension en l'appelant initialement EM64T renommé aujourd'hui en Intel 64.",
            "",
            "Amd64 (також x86-64/intel64/em64t/x64) — 64-бітова архітектура мікропроцесора і відповідний набір інструкцій,",
            "розроблені компанією AMD. Це розширення архітектури x86 з повною зворотною сумісністю.",
            "",
            "x86-64 е наименованието на наборът от 64-битови разширения към x86 процесорната архитектура. Като синоним",
            "на това наименование, се използват и съкращенията AMD64 (използвано от AMD), EM64T и IA-32e (използвани от",
            "Intel) и x64 (използвано от Microsoft).",
            "",
            "เอกซ์86-64 (x86-64) เป็นชื่อของสถาปัตยกรรมคอมพิวเตอร์สำหรับไมโครโพรเซสเซอร์แบบ 64 บิต และชุดคำสั่งที่ใช้งานด้วยกัน x86-64",
            "เป็นส่วนขยายของสถาปัตยกรรมแบบ x86 ออกแบบโดยบริษัท AMD และใช้ชื่อทางการค้าว่า AMD64 ในภายหลังบริษัทอินเทลได้นำสถาปัตยกร",
            "รมนี้ไปใช้ใต้ชื่อการค้าว่า Intel 64 หรือ EM64T ซึ่งชื่อทั่วไปที่ใช้กันโดยไม่อิงกับชื่อการค้าคือ x86-64 หรือ x64"
    )

    override fun render() {
        Gdx.gl.glClearColor(.157f, .157f, .157f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        Gdx.graphics.setTitle("$GAME_NAME — F: ${Gdx.graphics.framesPerSecond}")

        (gameFont as GameFontBase).reload("bg")

        batch.inBatch {

            text.forEachIndexed { index, s ->
                gameFont.color = Color(1f, 1f, 1f, 1f)
                gameFont.draw(batch, s, 10f, 10 + (20 * text.size) - 20f * index)
            }

        }
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }


    private inline fun SpriteBatch.inBatch(action: () -> Unit) {
        this.begin()
        action()
        this.end()
    }
}

fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = LwjglApplicationConfiguration()
    //config.useGL30 = true
    config.vSyncEnabled = false
    config.resizable = false
    config.width = 1072
    config.height = 742
    config.foregroundFPS = 9999
    LwjglApplication(TestTestTest(), config)
}