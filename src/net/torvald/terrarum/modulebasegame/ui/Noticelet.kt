package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas

/**
 * Smaller notification for item pickup notice
 *
 * Created by minjaesong on 2024-01-22.
 */
class Noticelet : UICanvas() {

    data class Notice(
        var timeAddedMS: Long,
        val item: ItemID,
        var amount: Long,
        var akku: Float = 0f
    )

    private var fontCol: Color = Color.WHITE // assuming alpha of 1.0

    override var openCloseTime: Second = Notification.OPEN_CLOSE_TIME
    private val visibleTime = 5f

    private val LRmargin = 0f // there's "base value" of 8 px for LR (width of segment tile)



    override var width: Int = 500

    override var height: Int = 0


    internal val messageQueue = ArrayList<Notice>()

    private val timeGaugeCol = Color(0x707070ff)

    init {
        handler.alwaysUpdate = true
        setAsAlwaysVisible()
    }

    override fun updateImpl(delta: Float) {
        // update timer and animation
        messageQueue.forEach {
            it.akku += delta
            if (it.akku > despawnTime) toDelete.add(it)
        }

        toDelete.forEach {
            messageQueue.remove(it)
        }
        toDelete.clear()

        // make way for the mouse cursor
        /*
        if (Terrarum.mouseScreenX.toFloat() in Toolkit.drawWidthf * 0.25f..Toolkit.drawWidthf * 0.75f) {
            if (ypos == -1f && Terrarum.mouseScreenY < App.scr.halfhf - awayFromCentre)
                ypos = 1f
            else if (ypos == 1f && Terrarum.mouseScreenY > App.scr.halfhf + awayFromCentre)
                ypos = -1f
        }

         */
    }

    private val h = 24f
    private val gap = 8f
    private val awayFromCentre = 56f//120f

    private val toDelete = ArrayList<Notice>()

    private var ypos = 1f // 1: bottom, -1: top

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        val px = Toolkit.drawWidthf
        val py = App.scr.halfhf + awayFromCentre * ypos - (if (ypos < 0) h else 0f)

        messageQueue.forEachIndexed { index, notice ->
            drawNoticelet(batch, App.scr.tvSafeGraphicsWidth.toFloat(),  py + (h + gap) * index * ypos, notice)
        }
    }

    override fun dispose() {
    }

    private val despawnTime = openCloseTime + visibleTime + openCloseTime

    /**
     * @param x center point
     * @param y up point
     */
    private fun drawNoticelet(batch: SpriteBatch, x: Float, y: Float, notice: Notice) {
        val prop = ItemCodex[notice.item]!!
        val str = "${prop.name} (${notice.amount})"
        val strLen = App.fontGame.getWidth(str)
        val icon = ItemCodex.getItemImage(notice.item) ?: CommonResourcePool.getAsTextureRegion("itemplaceholder_16")
        val width = 4f + icon.regionWidth + 4 + strLen + 4
        val dx = x//((x - width) / 2).floorToFloat()
        val dy = y

        val opacity = if (notice.akku < openCloseTime)
            notice.akku / openCloseTime
        else if (notice.akku < openCloseTime + visibleTime)
            1f
        else
            1f - (notice.akku - visibleTime - openCloseTime) / openCloseTime

        Toolkit.drawBaloon(batch, dx, dy + 2, width, h - 4, opacity.coerceIn(0f, 1f))
        batch.draw(icon, dx + 4f, dy + ((h - icon.regionHeight) / 2).floorToFloat())
        App.fontGame.draw(batch, str, dx + 4f + icon.regionWidth + 4, dy)
    }

    fun sendNotification(item: ItemID, amount: Long) {
//        printdbg(this, "Picked up $item ($amount)")
        messageQueue.find { it.item == item }.let {
            if (it != null) {
                it.timeAddedMS = System.currentTimeMillis()
                it.amount += amount
                it.akku = openCloseTime
            }
            else {
                messageQueue.add(Notice(System.currentTimeMillis(), item, amount))
            }
        }
    }


}