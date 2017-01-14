package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

import java.util.HashMap

/**
 * Created by minjaesong on 16-01-15.
 */
object CommandDict {

    internal var dict: HashMap<String, ConsoleCommand> = hashMapOf(
            Pair("echo", Echo),
            Pair("error", EchoError),
            Pair("setav", SetAV),
            Pair("qqq", QuitApp),
            Pair("codex", CodexEdictis),
            Pair("export", ExportMap),
            Pair("gc", ForceGC),
            Pair("getav", GetAV),
            Pair("getlocale", GetLocale),
            Pair("togglenoclip", ToggleNoClip),
            Pair("nc", ToggleNoClip),
            Pair("setlocale", SetLocale),
            Pair("zoom", Zoom),
            Pair("teleport", Teleport),
            Pair("tp", Teleport),
            Pair("cat", CatStdout),
            Pair("exportav", ExportAV),
            Pair("setgl", SetGlobalLightOverride),
            Pair("getfaction", GetFactioning),
            Pair("auth", Authenticator),
            Pair("batch", Batch),
            Pair("settime", SetTime),
            Pair("gettime", GetTime),
            Pair("settimedelta", SetTimeDelta),
            Pair("help", Help),
            Pair("version", Version),
            Pair("seed", Seed),
            Pair("println", EchoConsole),
            Pair("inventory", Inventory),
            Pair("avtracker", AVTracker),
            Pair("actorslist", ActorsList),

            // Test codes
            Pair("bulletintest", SetBulletin),
            Pair("gsontest", GsonTest),
            Pair("tips", PrintRandomTips),
            Pair("langtest", LangTest),
            Pair("testgetlight", TestGetLight),
            Pair("spawnball", SpawnPhysTestBall),
            Pair("spawntorch", SpawnTikiTorch),
            Pair("musictest", MusicTest),
            Pair("spawntapestry", SpawnTapestry)
    )

    operator fun get(commandName: String): ConsoleCommand {
        return dict[commandName]!!
    }
}
