package net.torvald.terrarum.console

import java.util.HashMap

/**
 * Created by minjaesong on 2016-01-15.
 */
object CommandDict {

    internal val dict: HashMap<String, ConsoleCommand> = hashMapOf(
            "echo" to Echo,
            "error" to EchoError,
            "setav" to SetAV,
            "qqq" to QuitApp,
            "codex" to CodexEdictis,
            "export" to ExportMap,
            "gc" to ForceGC,
            "getav" to GetAV,
            "getlocale" to GetLocale,
            "togglenoclip" to ToggleNoClip,
            "nc" to ToggleNoClip,
            "setlocale" to SetLocale,
            "zoom" to Zoom,
            "teleport" to Teleport,
            "tp" to Teleport,
            "cat" to CatStdout,
            "exportav" to ExportAV,
            "setgl" to SetGlobalLightOverride,
            "getfaction" to GetFactioning,
            "auth" to Authenticator,
            "batch" to Batch,
            "settime" to SetTime,
            "gettime" to GetTime,
            "settimedelta" to SetTimeDelta,
            "help" to Help,
            "version" to Version,
            "seed" to Seed,
            "println" to EchoConsole,
            "inventory" to Inventory,
            "avtracker" to AVTracker,
            "actorslist" to ActorsList,
            "setscale" to SetScale,
            "kill" to KillActor,
            "money" to MoneyDisp,

            // Test codes
            "bulletintest" to SetBulletin,
            "gsontest" to GsonTest,
            "tips" to PrintRandomTips,
            "langtest" to LangTest,
            "spawnball" to SpawnPhysTestBall,
            "spawntorch" to SpawnTikiTorch,
            "musictest" to MusicTest,
            "spawntapestry" to SpawnTapestry,
            "imtest" to JavaIMTest,


            /* !! */"exportlayer" to ExportLayerData,
            /* !! */"importlayer" to ImportLayerData
    )

    operator fun get(commandName: String): ConsoleCommand {
        return dict[commandName]!!
    }

    fun add(name: String, obj: ConsoleCommand) {
        dict[name] = obj
    }
}
