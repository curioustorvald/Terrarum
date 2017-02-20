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

        /*segfont = SpriteSheetFont(
                SpriteSheet("./assets/graphics/fonts/24-seg_red.tga", 22, 31),
                ' '
        )*/

        mtfont = SpriteSheetFont(
                SpriteSheet("./assets/graphics/fonts/mt-32.tga", 12, 16),
                0.toChar()
        )
    }

    override fun update(gc: GameContainer, game: StateBasedGame, delta: Int) {

    }

    override fun render(gc: GameContainer, game: StateBasedGame, g: Graphics) {
        //g.font = Terrarum.fontGame

        /*val text = arrayOf(
                Lang["APP_WARNING_HEALTH_AND_SAFETY"],
                "",
                "90’ 10’ 20” 50 cm",
                "",
                "",
                Lang["MENU_LABEL_PRESS_ANYKEY_CONTINUE"],
                "DGB금융지주의 자회사. 대구광역시에서 쓰는 교통카드인 원패스와 탑패스 그리고 만악의 근원 대경교통카드를 판매 및 정산하고 있다. 본사는",
                "Atlantic Records, it features production from Nick Hexum of 311, Tony Kanal of No Doubt, and Sublime producer Paul Leary."
        )

        for (i in 0..text.size - 1) {
            g.drawString(text[i], 10f, 10f + (g.font.lineHeight * i))
        }*/

        //g.font = Terrarum.fontSmallNumbers
        //g.font = segfont
        //g.font = mtfont
        g.background = Color(0x282828)
        g.font = Terrarum.fontGame

        val line = "    **** TERRAN BASIC V0.5 ****    "

        g.drawString("ABCDEFGHIJKLMN", 10f, 10f)
        g.drawString("OPQRSTÜVWXYZÆŒ", 10f, 30f)

        g.drawString("abcdefghijklmno", 160f, 10f)
        g.drawString("pqrstuvwxyzßæœ", 160f, 30f)

        g.drawString("1234567890?!", 320f, 10f)
        g.drawString("minimum kerning keming Narnu Namu", 320f, 30f)

        g.drawString("Syö salmiakkia perkele", 480f, 10f)

        val text = arrayOf(
                "The Olympic marmot (Marmota olympus) is a rodent in the squirrel family, Sciuridae.",
                "It lives only in the U.S. state of Washington, at middle elevations on the Olympic Peninsula.",
                "About the size of a domestic cat, an adult weighs around 8 kg (18 lb) in summer.",
                "",
                "Brná je část statutárního a krajského města Ústí nad Labem v České republice, spadající",
                "pod městský obvod Ústí nad Labem-Střekov. Nachází se asi pět kilometrů jižně od centra",
                "města v Českém středohoří na pravém břehu řeky Labe.",
                "",
                "Malaysia er en forholdsvis ung stat. Sin endelige udstrækning fik den først i 1965 efter,",
                "at Singapore trak sig ud. Staten blev grundlagt ved en sammenslutning af flere tidligere",
                "britiske besiddelser, foreløbigt i 1957 og endeligt i 1963.",
                "",
                "Το θάλλιο συνοδεύει κυρίως θειούχα ορυκτά βασικών μετάλλων, όπως ο σφαλερίτης, ο σιδηροπυρίτης",
                "και ο γαληνίτης ενώ αναφέρονται και εμφανίσεις του σε κονδύλους μαγγανίου στους βυθούς των ωκεανών.",
                "διαφυλάξτε γενικά τη ζωή σας από βαθειά ψυχικά τραύματα",
                "ΔΙΑΦΥΛΆΞΤΕ ΓΕΝΙΚΆ ΤΗ ΖΩΉ ΣΑΣ ΑΠΌ ΒΑΘΕΙΆ ΨΥΧΙΚΆ ΤΡΑΎΜΑΤΑ",
                "Широкая электрификация южных губерний даст мощный толчок подъёму сельского хозяйства.",
                "Příliš žluťoučký kůň úpěl ďábelské ódy.    Árvíztűrő tükörfúrógép.",
                "Victor jagt zwölf Boxkämpfer quer über den großen Sylter Deich.",
                "Pijamalı hasta yağız şoföre çabucak güvendi.    Kŕdeľ ďatľov učí koňa žrať kôru.",
                "Voix ambiguë d'un cœur qui au zéphyr préfère les jattes de kiwi.",
                "Înjurând pițigăiat, zoofobul comandă vexat whisky și tequila.",
                "",
                "sjaldgæft    ekki stjórnarskrárvarin",
                "",
                "Also supports:",
                "‛Unicode’ „quotation marks“—dashes…‼",
                "으웽~. 얘! 위에 이 애 우유의 양 외워와! 아오~ 왜요? 어여! 예...  웬 초콜릿? 제가 원했던 건 뻥튀기 쬐끔과 의류예요. 얘야, 왜 또 불평?  퀡뙔풿횂",
                "とりなくこゑす ゆめさませ みよあけわたる ひんかしを そらいろはえて おきつへに ほふねむれゐぬ もやのうち",
                "鳥啼く声す 夢覚ませ 見よ明け渡る 東を 空色栄えて 沖つ辺に 帆船群れゐぬ 靄の中",
                ""
        )

        text.forEachIndexed { i, s ->
            g.drawString(s, 10f, 70f + 20 * i)
        }

        /*val text = arrayOf(
                "ru: Широкая электрификация южных губерний даст мощный толчок подъёму сельского хозяйства",
                "bg: Под южно дърво, цъфтящо в синьо, бягаше малко пухкаво зайче",
                "sr: Ајшо, лепото и чежњо, за љубав срца мога дођи у Хаџиће на кафу"
        )

        (0..2).forEach {
            g.drawString(text[it], 10f, 70f + 20 * it)
        }*/

    }

    override fun getID(): Int = Terrarum.STATE_ID_TEST_FONT
}