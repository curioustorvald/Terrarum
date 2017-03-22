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

        g.background = Color(0x282828)
        g.font = Terrarum.fontGame


        g.drawString("ABCDEFGHIJKLMN", 10f, 10f)
        g.drawString("OPQRSTÜVWXYZÆŒ", 10f, 30f)

        g.drawString("abcdefghijklmno", 160f, 10f)
        g.drawString("pqrstuvwxyzßæœ", 160f, 30f)

        g.drawString("1234567890?!", 320f, 10f)
        //g.drawString("minimum kerning keming Narnu Namu", 320f, 30f)

        //g.drawString("Syö salmiakkia perkele", 480f, 10f)

        val text = arrayOf(
                "The bitmap font for game developers who seek good font that has real multilingual support,",
                "for free (as in freedom AND without cost).",
                "",
                "There are many bitmap fonts on the internet. You care for the multilingual support, but alas!",
                "most of them does not support your language, vector fonts takes too much time to be loaded,",
                "even then their legibility suffers because fuck built-in antialias.",
                "You somehow found a good font, and it makes your game look like a linux terminal, and you say:",
                "“what the fuck? Is this a game or should I rm -rf this shit‽”",
                "You speak Japanese, and you wish to support it, but then このクソなfontは only good for Japanese,",
                "and it is not multilingual, and you don't have a time for this shenanigan.",
                "Eventually you give up, saying “fuck it!” and just use the fonts that do not match well.",
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
                "정 참판 양반댁 규수 큰 교자 타고 혼례 치른 날    뚫훍뚫훍뚫(읗) 뚫훍뚫훍뚫(읗) 뚫훍뚫훍뚫 따다다",
                "Kæmi ný öxi hér, ykist þjófum nú bæði víl og ádrepa",
                "Árvíztűrő tükörfúrógép    Kŕdeľ ďatľov učí koňa žrať kôru",
                "とりなくこゑす ゆめさませ みよあけわたる ひんかしを そらいろはえて おきつへに ほふねむれゐぬ もやのうち",
                "鳥啼く声す 夢覚ませ 見よ明け渡る 東を 空色栄えて 沖つ辺に 帆船群れゐぬ 靄の中",
                "Înjurând pițigăiat, zoofobul comandă vexat whisky și tequila",
                "Широкая электрификация южных губерний даст мощный толчок подъёму сельского хозяйства",
                "Pijamalı hasta yağız şoföre çabucak güvendi",
                "Also supports: ‛Unicode’ „quotation marks“—dashes…「括弧」‼",
                "ASCII  Latin-1  Latin_Ext-A  Latin_Ext-B  Greek  Cyrillic  CJK-Ideo  Kana  Hangul_Syllables  (More coming!)",
                ""
        )

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