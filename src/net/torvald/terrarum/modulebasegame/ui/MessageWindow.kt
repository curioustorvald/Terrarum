package net.torvald.terrarum.modulebasegame.ui


/**
 * Created by minjaesong on 2016-01-27.
 */
/*class MessageWindow(override var width: Int, isBlackVariant: Boolean) : UICanvas() {

    private val segment = if (isBlackVariant) SEGMENT_BLACK else SEGMENT_WHITE

    var messagesList = arrayOf("", "")
    override var height: Int = 0

    private var fontCol: Color = if (!isBlackVariant) Color.BLACK else Color.WHITE
    private val GLYPH_HEIGHT = AppLoader.fontGame.lineHeight

    override var openCloseTime: Second = OPEN_CLOSE_TIME

    private val LRmargin = 0f // there's "base value" of 8 px for LR (width of segment tile)


    fun setMessage(messagesList: Array<String>) {
        this.messagesList = messagesList
    }

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormal(batch)

        val textWidth = messagesList.map { AppLoader.fontGame.getWidth(it) }.sorted()[1]

        batch.color = Color.WHITE

        batch.draw(segment.get(1, 0), segment.tileW.toFloat(), 0f, 2 * LRmargin + textWidth, segment.tileH.toFloat())
        batch.draw(segment.get(0, 0), 0f, 0f)
        batch.draw(segment.get(2, 0), 2 * LRmargin + textWidth, 0f)

        messagesList.forEachIndexed { index, s ->
            AppLoader.fontGame.draw(batch, s, segment.tileW + LRmargin, (segment.tileH - AppLoader.fontGame.lineHeight) / 2f)
        }

        AppLoader.printdbg(this, "render")

    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }

    companion object {
        // private int messagesShowingIndex = 0;
        val MESSAGES_DISPLAY = 2
        val OPEN_CLOSE_TIME = 0.16f


        // will be disposed by Terrarum (application main instance)
        val SEGMENT_BLACK = TextureRegionPack("assets/graphics/gui/message_black.tga", 8, 56)
        val SEGMENT_WHITE = TextureRegionPack("assets/graphics/gui/message_white.tga", 8, 56)
    }
}*/
