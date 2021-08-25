package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameworld.GameEconomy
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension

/**
 * Created by minjaesong on 2021-08-25.
 */
open class ReadWorld(val ingame: TerrarumIngame) {

    open fun invoke(worldIndex: Int, metadata: JsonValue, worlddata: JsonValue) {
        val json = Json()
        val world = GameWorldExtension(
                worldIndex,
                worlddata.getInt("width"),
                worlddata.getInt("height"),
                metadata.getLong("creation_t"),
                metadata.getLong("lastplay_t"),
                metadata.getInt("playtime_t")
        )

        //world.economy = json.fromJson(GameEconomy::class.java, worlddata.get("basegame.economy").)
    }

}