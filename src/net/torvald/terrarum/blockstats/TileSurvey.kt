package net.torvald.terrarum.blockstats

import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Created by minjaesong on 2016-02-01.
 */
object TileSurvey {

    data class SurveyProposal(
        val surveyIdentifier: String,
        val width: Int,
        val height: Int,
        val spatialGranularity: Int, // 1: survey every (w*h) tile, 2: survey every other (w*h/4) tile ...
        val temporalGranularity: Int, // 1: survey every frame, 2: every other frame ...
        val predicate: (GameWorld, Int, Int) -> Float
    )

    private val proposals = HashMap<String, SurveyProposal>()
    private val results = HashMap<String, Pair<Double, Float>>() // (ratio of accumulated data to total tile count / raw accumulated data)

    fun submitProposal(proposal: SurveyProposal) {
        proposals[proposal.surveyIdentifier] = proposal
    }

    fun withdrawProposal(surveyIdentifier: String) {
        proposals.remove(surveyIdentifier)
        results.remove(surveyIdentifier)
    }

    /**
     * Update tile stats from tiles on screen
     */
    fun update() {

        // Get stats on no-zoomed screen area. In other words, will behave as if screen zoom were 1.0
        // no matter how the screen is zoomed.
        val world = INGAME.world
        val player = (Terrarum.ingame!! as TerrarumIngame).actorNowPlaying
        if (player == null) return

        proposals.forEach { (_, proposal) ->

            if (INGAME.WORLD_UPDATE_TIMER % proposal.temporalGranularity == 0) {
                val for_x_start = floor(player.intTilewiseHitbox.centeredX - proposal.width / 2.0).toInt()
                val for_y_start = floor(player.intTilewiseHitbox.centeredY - proposal.height / 2.0).toInt()
                val for_x_end = ceil(player.intTilewiseHitbox.centeredX + proposal.width / 2.0).toInt()
                val for_y_end = ceil(player.intTilewiseHitbox.centeredY + proposal.height / 2.0).toInt()

                var akku = 0f

                for (y in for_y_start until for_y_end step proposal.spatialGranularity) {
                    for (x in for_x_start until for_x_end step proposal.spatialGranularity) {
                        akku += proposal.predicate(world, x, y)
                    }
                }

                val surveyedTileCount =
                    (proposal.width * proposal.height) / (proposal.spatialGranularity * proposal.spatialGranularity)
                val ratio = akku.toDouble() / surveyedTileCount

                results[proposal.surveyIdentifier] = ratio to akku
            }
        }
    }

    fun getRawCount(surveyIdentifier: String) = results[surveyIdentifier]?.second
    fun getRatio(surveyIdentifier: String) = results[surveyIdentifier]?.first
    fun getRatioAndRawCount(surveyIdentifier: String) = results[surveyIdentifier]

}
