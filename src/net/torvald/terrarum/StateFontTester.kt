package net.torvald.terrarum

import net.torvald.imagefont.GameFontWhite
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

        Terrarum.gameLocale = "fiFI"

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

        g.drawString("ABCDEFGHIJKLM", 10f, 10f)
        g.drawString("NOPQRSTÜVWXYZ", 10f, 30f)

        g.drawString("abcdefghijklmno", 160f, 10f)
        g.drawString("pqrstuvwxyzßœ", 160f, 30f)

        g.drawString("1234567890", 320f, 10f)
        g.drawString("minimum kerning keming Nannu Namu", 320f, 30f)

        g.drawString("Syö salmiakkia perkele", 480f, 10f)

        /*val text = arrayOf(
                "Kedok Ketawa (The Laughing Mask) is a 1940 action film from the Dutch East Indies, in",
                "present-day Indonesia. After a young couple falls in love, the title character, a",
                "vigilante, helps them fight off criminals who have been sent to kidnap the woman by a",
                "rich man who wants her as his wife. It was the first film of Union Films, one of four",
                "new production houses established after the country's ailing film industry was revived",
                "by the success of Albert Balink's Terang Boelan. Kedok Ketawa was directed by Jo An",
                "Djan and stars Basoeki Resobowo, Fatimah, Oedjang (as the vigilante), S Poniman and",
                "Eddy Kock. Featuring fighting, comedy, and singing, and advertised as an \"Indonesian",
                "cocktail of violent actions ... and sweet romance\", the film received positive",
                "reviews, particularly for its cinematography. Following the success of the film, Union",
                "produced another six before being shut down in early 1942 during the Japanese",
                "occupation. Screened until at least August 1944, the film may be lost."
        )*/

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
                "διαφυλάξτε γενικά τη ζωή σας από βαθειά ψυχικά τραύματα",
                "Широкая электрификация южных губерний даст мощный толчок подъёму сельского хозяйства.",
                "Příliš žluťoučký kůň úpěl ďábelské ódy.    Árvíztűrő tükörfúrógép ",
                "Põdur Zagrebi tšellomängija-följetonist Ciqo külmetas kehvas garaažis ",
                "Victor jagt zwölf Boxkämpfer quer über den großen Sylter Deich",
                "Pijamalı hasta yağız şoföre çabucak güvendi.    Kŕdeľ ďatľov učí koňa žrať kôru.",
                "Voix ambiguë d'un cœur qui au zéphyr préfère les jattes de kiwi",
                "",
                "Discografia Siei se compune din șase albume de studio, șase albume live, treizeci și",
                "patru discuri single (inclusiv unsprezece ca și artist secundar), și cincisprezece",
                "videoclipuri. Până în octombrie 2014, a vândut 25 de milioane de cântece în întreaga lume.",
                "",
                "Málið snerist um ofbeldi lögregluþjóna gegn fjölskyldu blökkumanna, en afar sjaldgæft var",
                "á þessum árum að slík mál kæmu fyrir æðstu dómstig. Fordæmið tryggði framgang sambærilegra",
                "mála sem einkenndust af því að opinberir aðilar virtu ekki stjórnarskrárvarin réttindi einstaklingsins.",
                "",
                "Also supports:",
                "키스의 고유조건은 입술끼리 만나야 하고 특별한 기술은 필요치 않다.",
                "とりなくこゑす ゆめさませ みよあけわたる ひんかしを そらいろはえて おきつへに ほふねむれゐぬ もやのうち",
                "鳥啼く声す 夢覚ませ 見よ明け渡る 東を 空色栄えて 沖つ辺に 帆船群れゐぬ 靄の中 (using WenQuanYi)",
                ""
        )

        text.forEachIndexed { i, s ->
            g.drawString(s, 10f, 70f + 20 * i)
        }


    }

    override fun getID(): Int = Terrarum.STATE_ID_TEST_FONT
}