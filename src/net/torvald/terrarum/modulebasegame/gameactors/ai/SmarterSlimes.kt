package net.torvald.terrarum.modulebasegame.gameactors.ai

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.gameactors.HumanoidNPC
import net.torvald.terrarum.Second
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ai.ActorAI
import net.torvald.terrarum.modulebasegame.Ingame

/**
 * Slime's stupid AI but can adjust his jump power to smack you as fast as possible
 * by achieving "allostasis".
 *
 * Created by minjaesong on 2017-12-10.
 */
class SmarterSlimes : ActorAI {

    val memoryCells = IntArray(12, { 0 })
    // index 0: most recent memory
    // intentionally making it stupid by using less precise INT
    // also we're not discrimination different enemies, making it further dumb
    // stores "overshoot" amount (learn target) of x position

    var maxJumpDist: Double = -1.0

    var cooltime: Second = 5f

    override fun update(actor: Actor, delta: Float) {
        val actor = actor as HumanoidNPC


        // sensor: compare(my X pos, nearest enemy's X pos)
        maxJumpDist = actor.avSpeedCap * actor.jumpAirTime // speed * air_time
        // (to be precise, we need simulation just like jumpAirTime, but oh well; we like it LINEAR)


        // TEST: just target player
        val player = (Terrarum.ingame!! as Ingame).actorNowPlaying
        if (player == null) return

        val playerXPos = player.centrePosPoint.x
        val thisXPos = actor.centrePosPoint.x
        val xDiff = thisXPos - playerXPos



        // extrapolate from memories:
        // otherwise linear extp. except the slope is d of 0th and 2nd point



        if (xDiff > 0) {
            actor.moveLeft()
        }
    }


}