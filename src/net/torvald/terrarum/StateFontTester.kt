package net.torvald.terrarum

import net.torvald.imagefont.GameFontImpl
import net.torvald.terrarum.langpack.Lang
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by minjaesong on 16-06-28.
 */
class StateFontTester : BasicGameState() {
    val textToPrint = """
ABCDEFGHIJKLM
NOPQRSTUVWXYZ

abcdefghijklm
nopqrstuvwxyz

1234567890
"""

    lateinit var canvas: Graphics

    //lateinit var segfont: Font

    lateinit var mtfont: Font

    override fun init(gc: GameContainer, game: StateBasedGame) {
        canvas = Graphics(1024, 1024)

    }

    override fun update(gc: GameContainer, game: StateBasedGame, delta: Int) {

    }

    override fun render(gc: GameContainer, game: StateBasedGame, g: Graphics) {

        g.background = Color(0x282828)
        g.font = Terrarum.fontGame


        g.drawString("ABCDEFGHIJKLMN", 10f, 10f)
        g.drawString("OPQRSTÜVWXYZÆŒ", 10f, 30f)

        g.drawString("abcdefghijklmno", 160f, 10f)
        g.drawString("pqrstuvwxyzßæœ", 160f, 30f)

        g.drawString("1234567890?!", 320f, 10f)
        //g.drawString("minimum kerning keming Narnu Namu", 320f, 30f)

        //g.drawString("Syö salmiakkia perkele", 480f, 10f)

        /*val text = arrayOf(
                "The bitmap font for game developers who seek good font that has real multilingual support,",
                "for free (as in freedom AND without cost).",
                "",
                "There are many bitmap fonts on the internet. You care for the multilingual support, but alas!",
                "most of them do not support your language, vector fonts take too much time to load, and even",
                "then their legibility suffers because fuck built-in antialias.",
                "You somehow found a fine one, and it makes your game look like a linux terminal, and you say:",
                "“Well, better than nothing *sigh*; No, it’s ugly.”",
                "You speak Japanese, and you wish to support it, but then このクソなfontは only good for Japanese,",
                "it is not even multilingual, and their English look ugly and inconsistent anyway.",
                "Eventually you just use different fonts together, and the result was always mildly infuriating.",
                "",
                "No more suffering. This font has everything you need.",
                "",
                "while (isVisible(BadFonts)) { ripAndTear(BadFonts).scope(Guts); }",
                "How multilingual? Real multilingual!",
                "",
                "Příliš žluťoučký kůň úpěl ďábelské ódy",
                "Victor jagt zwölf Boxkämpfer quer über den großen Sylter Deich",
                "διαφυλάξτε γενικά τη ζωή σας από βαθειά ψυχικά τραύματα",
                "ΔΙΑΦΥΛΆΞΤΕ ΓΕΝΙΚΆ ΤΗ ΖΩΉ ΣΑΣ ΑΠΌ ΒΑΘΕΙΆ ΨΥΧΙΚΆ ΤΡΑΎΜΑΤΑ",
                "Pack my box with five dozen liquor jugs",
                "Voix ambiguë d'un cœur qui au zéphyr préfère les jattes de kiwi",
                "정 참판 양반댁 규수 큰 교자 타고 혼례 치른 날    하얬다  도럄직한  퀡봹퉪헰",
                "Kæmi ný öxi hér, ykist þjófum nú bæði víl og ádrepa",
                "Árvíztűrő tükörfúrógép    Kŕdeľ ďatľov učí koňa žrať kôru",
                "とりなくこゑす ゆめさませ みよあけわたる ひんかしを そらいろはえて おきつへに ほふねむれゐぬ もやのうち",
                "鳥啼ク声ス 夢覚マセ 見ヨ明ク渡ル 東ヲ 空色栄エテ 沖ツ辺ニ 帆船群レヰヌ 靄ノ中",
                "Înjurând pițigăiat, zoofobul comandă vexat whisky și tequila",
                "Широкая электрификация южных губерний даст мощный толчок подъёму сельского хозяйства",
                "Pijamalı hasta yağız şoföre çabucak güvendi",
                "Also supports: ‛Unicode’ „quotation marks“—dashes…「括弧」‼",
                "ASCII  Latin-1  Latin_Ext-A  Latin_Ext-B  Greek  Cyrillic  CJK-Ideo  Kana  Hangul_Syllables",
                "",
                "…not seeing your language/writing system? Let me know on the Issue Tracker!"
        )*/
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
                "Intel) и x64 (използвано от Microsoft)."
        )
        val SP = "${0x3000.toChar()}${0x3000.toChar()}"

        /*val text = arrayOf(
                "${0xe006.toChar()} ${Lang["GAME_INVENTORY_USE"p]}$SP${0xe011.toChar()}..${0xe019.toChar()} ${Lang["GAME_INVENTORY_REGISTER"]}$SP${0xe034.toChar()} ${Lang["GAME_INVENTORY_DROP"]}"
        )*/

        Terrarum.gameLocale = "bgBG"

        text.forEachIndexed { i, s ->
            g.drawString(s, 10f, 70f + 20 * i)
        }




        /*val text = arrayOf(
                "ru: Широкая электрификация южных губерний даст мощный толчок подъёму сельского хозяйства",
                "bg: Под южно дърво, цъфтящо в синьо, бягаше малко пухкаво зайче",
                "sr: Ајшо, лепото и чежњо, за љубав срца мога дођи у Хаџиће на кафу"
        )*/
        /*val text = arrayOf(
                "……退魔の剣に選ばれし ハイラルの勇者よ",
                "その たゆまぬ努力と 結実せに剣技を認め……",
                "女神ハイリアの名において祝福を授けん……",
                "空を舞い 時を回り 黄昏に染まろうとも……",
                "結ばれし剣は 勇者の魂と共に……",
                "さらなる力が そなたと そして退魔の剣に宿らんことを……"
        )*/


        /*(0..text.size - 1).forEach {
            g.drawString(text[it], 10f, 70f + 20 * it)
        }*/

    }

    override fun getID(): Int = Terrarum.STATE_ID_TEST_FONT
}