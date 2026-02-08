package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.gameactors.ActorMovingPlatform
import kotlin.math.cos
import kotlin.math.sin

/**
 * Test platform that randomly selects a movement pattern on spawn.
 *
 * Patterns:
 * - 0: Horizontal pingpong (sine-eased)
 * - 1: Vertical pingpong (sine-eased)
 * - 2: Clockwise circular (constant speed)
 * - 3: Counter-clockwise circular (constant speed)
 *
 * Created by minjaesong on 2022-03-02.
 */
class ActorTestPlatform : ActorMovingPlatform(8) {

    /** Movement pattern index (0-3). */
    private val pattern: Int = (0..1).random()

    /** Speed in pixels per tick (2.0 to 4.0). */
    private val speed: Double = 2.0 + Math.random() * 2.0

    /** Current phase angle in radians. */
    private var phase: Double = 0.0

    /**
     * Phase step per tick.
     *
     * For pingpong: peak speed = amplitude * phaseStep = speed
     *   period = 128 ticks (~2s), so phaseStep = 2*PI/128
     *   amplitude = speed / phaseStep
     *
     * For circular: speed = radius * phaseStep
     *   using same phaseStep, radius = speed / phaseStep
     */
    @Transient private val PERIOD_TICKS = 128.0
    @Transient private val phaseStep: Double = 2.0 * Math.PI / PERIOD_TICKS

    /** Amplitude for pingpong patterns, radius for circular patterns. */
    @Transient private val amplitude: Double = speed / phaseStep

    override fun updateImpl(delta: Float) {
        val oldPhase = phase
        phase += phaseStep

        when (pattern) {
            0 -> {
                // Horizontal pingpong: position = A * sin(phase)
                // Velocity = finite difference to prevent float drift
                val dx = amplitude * (sin(phase) - sin(oldPhase))
                contraptionVelocity.set(dx, 0.0)
            }
            1 -> {
                // Vertical pingpong: position = A * sin(phase)
                val dy = amplitude * (sin(phase) - sin(oldPhase))
                contraptionVelocity.set(0.0, dy)
            }
            2 -> {
                // Clockwise circular: position on circle (cos, sin)
                val dx = amplitude * (cos(phase) - cos(oldPhase))
                val dy = amplitude * (sin(phase) - sin(oldPhase))
                contraptionVelocity.set(dx, dy)
            }
            3 -> {
                // Counter-clockwise circular: negate Y component
                val dx = amplitude * (cos(phase) - cos(oldPhase))
                val dy = -(amplitude * (sin(phase) - sin(oldPhase)))
                contraptionVelocity.set(dx, dy)
            }
        }

        super.updateImpl(delta)
    }
}
