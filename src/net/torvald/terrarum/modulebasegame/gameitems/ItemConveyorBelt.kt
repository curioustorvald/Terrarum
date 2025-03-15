package net.torvald.terrarum.modulebasegame.gameitems

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRange
import net.torvald.terrarum.modulebasegame.gameactors.ActorConveyors
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer

/**
 * Created by minjaesong on 2025-03-15.
 */
open class ItemConveyorBelt(originalID: ItemID) : GameItem(originalID) {
    override var baseMass = 10.0
    override var baseToolSize: Double? = null
    override var inventoryCategory = Category.FIXTURE
    override val canBeDynamic = false
    override val materialId = ""
    override var equipPosition = EquipPosition.HAND_GRIP

    override var originalName = "CONVEYOR_BELT"


    protected val AXLE_COUNT = 2


    private var currentStatus = 0 // 0: initial, 1+: place n-th axle

    private val occupiedPoints = HashSet<Point2i>()

    override fun startPrimaryUse(actor: ActorWithBody, delta: Float) = mouseInInteractableRange(actor) { _, _, mtx, mty ->
        val ret = when (currentStatus) {
            0 -> placeFirstAxle(mtx, mty)
            1 -> placeSecondAxleAndFinalise(mtx, mty)
            else -> {
                currentStatus = 0
                -1L
            }
        }

        currentStatus += 1

        ret
    }

    override fun startSecondaryUse(actor: ActorWithBody, delta: Float): Long {
        return when (currentStatus) {
            0 -> FixtureItemBase.fixturePickupFun(actor, delta)
            else -> {
                currentStatus = -1
                -1L
            }
        }
    }


    private fun placeFirstAxle(mx: Int, my: Int): Long {
        occupiedPoints.add(Point2i(mx, my))
        return 0L
    }

    private fun placeSecondAxleAndFinalise(mx: Int, my: Int): Long {
        Point2i(mx, my).let { p2 ->
            if (!occupiedPoints.contains(p2)) {
                occupiedPoints.add(p2)
            }
            else {
                currentStatus = -1
                return 0L
            }

            // sort occupiedPoints by its x value
            val points = occupiedPoints.toMutableList().also { it.sortBy { it.x } }.also { list ->
                // check for ROUNDWORLD
                val xMin = list[0].x
                val xMax = list[1].x
                // normalise it by making the x value for left spindle to negative
                if (xMin >= 0 && xMin < INGAME.world.width / 2 && xMax >= INGAME.world.width / 2) {
                    list[0].x -= INGAME.world.width
                }
            }

            val conveyors = ActorConveyors(points[0].x, points[0].y, points[1].x, points[1].y)
            conveyors.spawn((INGAME.actorNowPlaying as? IngamePlayer)?.uuid)
        }

        occupiedPoints.clear()
        currentStatus = -1

        return 1L
    }
}

