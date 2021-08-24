package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-08-23.
 */
class WriteWorld(val ingame: TerrarumIngame) {

    open fun invoke(): String {
        val world = ingame.world
        return "{${world.getJsonFields().joinToString(",\n")}}"
    }


}
