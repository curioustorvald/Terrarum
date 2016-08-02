package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

import java.util.HashMap

/**
 * Created by minjaesong on 16-01-15.
 */
object CommandDict {

    internal var dict: HashMap<String, ConsoleCommand> = hashMapOf(
            Pair("echo", Echo()),
            Pair("error", Error()),
            Pair("setav", SetAV()),
            Pair("qqq", QuitApp()),
            Pair("codex", CodexEdictis()),
            Pair("export", ExportMap()),
            Pair("gc", ForceGC()),
            Pair("getav", GetAV()),
            Pair("getlocale", GetLocale()),
            Pair("togglenoclip", ToggleNoClip()),
            Pair("nc", ToggleNoClip()),
            Pair("setlocale", SetLocale()),
            Pair("zoom", Zoom()),
            Pair("teleport", TeleportPlayer()),
            Pair("tp", TeleportPlayer()),
            Pair("cat", CatStdout()),
            Pair("exportav", ExportAV()),
            Pair("setgl", SetGlobalLightLevel()),
            Pair("getfaction", GetFactioning()),
            Pair("auth", Terrarum.ingame.auth),
            Pair("spawnball", SpawnPhysTestBall()),
            Pair("batch", Batch()),
            Pair("settime", SetTime()),
            Pair("gettime", GetTime()),
            Pair("settimedelta", SetTimeDelta()),
            Pair("help", Help()),
            Pair("version", Version()),
            Pair("seed", Seed()),

            // Test codes
            Pair("bulletintest", SetBulletin()),
            Pair("gsontest", GsonTest()),
            Pair("tips", PrintRandomTips()),
            Pair("langtest", LangTest()),
            Pair("musictest", MusicTest())
    )

    operator fun get(commandName: String): ConsoleCommand {
        return dict[commandName]!!
    }
}
