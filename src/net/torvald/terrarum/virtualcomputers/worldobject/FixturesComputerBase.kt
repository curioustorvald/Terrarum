package net.torvald.terrarum.virtualcomputers.worldobject

import net.torvald.terrarum.gameactors.FixturesBase
import java.security.SecureRandom
import java.util.*

/**
 * Created by minjaesong on 16-09-08.
 */
abstract class FixturesComputerBase() : FixturesBase() {

    init {
        actorValue["memslot0"] = -1 // -1 indicates mem slot is empty
        actorValue["memslot1"] = -1 // put index of item here
        actorValue["memslot2"] = -1 // ditto.
        actorValue["memslot3"] = -1 // do.

        actorValue["processor"] = -1 // do.

        actorValue["sda0"] = "none" // 'UUID rendered as String' or "none"
        actorValue["sda1"] = "none"
        actorValue["sda2"] = "none"
        actorValue["sda3"] = "none"
        actorValue["fd1"] = "none"
        actorValue["fd2"] = "none"
        actorValue["fd3"] = "none"
        actorValue["fd4"] = "none"

        actorValue["uuid"] = UUID.randomUUID().toString()

        collisionFlag = COLLISION_PLATFORM
    }

    val processorCycle: Int // number of Lua statement to process per tick (1/100 s)
        get() = ComputerPartsCodex.getProcessorCycles(actorValue.getAsInt("processor") ?: -1)
    val memSize: Int // max: 8 GB
        get() {
            var size = 0
            for (i in 0..3)
                size += ComputerPartsCodex.getRamSize(actorValue.getAsInt("memSlot$i") ?: -1)

            return size
        }

    var terminal: FixturesBase? = null


    // API-specific


}