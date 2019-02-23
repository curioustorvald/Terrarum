
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.JsonWriter
import java.io.File

/**
 * Created by minjaesong on 2019-02-23.
 */

val locales = arrayOf(
        "en",
        "frFR",
        "es",
        "de",
        "it",
        "ptBR",
        "ptPT",
        "ruRU",
        "elGR",
        "trTR",
        "daDK",
        "noNB",
        "svSE",
        "nlNL",
        "plPL",
        "fiFI",
        "jaJP",
        "zhCN",
        "zhTW",
        "koKR",
        "csCZ",
        "huHU",
        "roRO",
        "thTH",
        "bgBG",
        "heIL",
        "ar",
        "bsBS",
        "msMS",
        "idID"
)
val delimiter = '\t'
val input = arrayOf(
        "CONTEXT_ITEM_MAP	[Noun]	Map	Carte	Mapa	Karte	Mappa	Mapa	Mapa	Карта	Χαρτης	Harita	Kort	Kart	Karta	Kaart	Mapa	Kartta	地図	地图	地圖	지도	Mapa	Térkép	Hartă	แผนที่	Карта	מפה	خريطة	Mapa	Peta	Peta",
        "MENU_LABEL_MENU	We have \"Main Menu\" but don't have generic \"Menu\"	Menu	Menu	Menú	Menü	Menu	Menu	Menu	Меню	Μενού	Menü	Menu	Meny	Meny	Menu	Menu	Valikko	メニュー	菜单	功能表	메뉴	Nabídka	Menü	Meniu	เมนู	Меню	תפריט	القائمة	Meni	Menu	Menu",
        "GAME_INVENTORY_REGISTER	[Infinitive/tutorial verb] to register an item onto a keyboard shortcut, as favourites, etc.	Register	Inscrire	Registrar	Registrieren	Registrare	Registrar	Registar	Зарегистрировать	Εγγραφή		Registrer	Registrer	Registrera	Registreren	Zarejestruj	Rekisteröidä	登録する	注册	註冊	등록하기	Zaregistrovat									"
)


val inTable = input.map { it.split(delimiter) }

fun main() {
    locales.forEachIndexed { index, it ->
        val file = File("./assets/mods/basegame/locales/$it/game.json")

        println("Locale: $it")

        if (file.exists()) {
            val jsonObject = JsonFetcher(file)

            inTable.forEach { record ->
                val key = record[0]
                val value = record[index + 2]
                if (value.isNotBlank()) {
                    jsonObject.addProperty(key, value)
                }
            }

            JsonWriter.writeToFile(jsonObject, file.absolutePath)
        }
    }
}