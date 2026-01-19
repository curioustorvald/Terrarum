package net.torvald.terrarum.gameactors

/**
 * Marker interface for actors that serve as "world update anchors".
 *
 * Actors implementing this interface cause world simulation (fluids, wires, tile updates)
 * to be active in their vicinity, regardless of camera position.
 *
 * All implementations must also extend [ActorWithBody].
 *
 * Created by minjaesong on 2026-01-19.
 */
interface WorldUpdater
