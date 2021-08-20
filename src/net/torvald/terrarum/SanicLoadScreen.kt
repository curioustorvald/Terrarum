package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.jme3.math.FastMath
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2017-07-13.
 */
object SanicLoadScreen : LoadScreenBase() {

    init {
        AppLoader.disposableSingletonsPool.add(this)
    }

    private var arrowObjPos = 0f // 0 means at starting position, regardless of screen position
    private var arrowObjGlideOffsetX = 0f
    private var arrowObjGlideSize = 0f
    private val arrowGlideSpeed: Float; get() = AppLoader.screenSize.screenW * 2f // pixels per sec
    private lateinit var arrowObjTex: Texture
    private var glideTimer = 0f
    private var glideDispY = 0f
    private var arrowColours = arrayOf(
            Color(0xff4c4cff.toInt()),
            Color(0xffd24cff.toInt()),
            Color(0x4cb5ffff.toInt())
    )

    private lateinit var textOverlayTex: Texture
    private lateinit var textFbo: FrameBuffer

    private val ghostMaxZoomX = 1.25f
    private val ghostAlphaMax = 1f


    override fun show() {
        glideTimer = 0f

        super.show()

        textFbo = FrameBuffer(
                Pixmap.Format.RGBA4444,
                maxOf(
                        AppLoader.fontGame.getWidth(Lang["MENU_IO_LOADING"]),
                        AppLoader.fontGame.getWidth(Lang["ERROR_GENERIC_TEXT"])
                ),
                AppLoader.fontGame.lineHeight.toInt(),
                true
        )

        arrowObjTex = Texture(Gdx.files.internal("assets/graphics/test_loading_arrow_atlas.tga"))
        arrowObjGlideOffsetX = -arrowObjTex.width.toFloat()

        textOverlayTex = Texture(Gdx.files.internal("assets/graphics/test_loading_text_tint.tga"))
    }


    val textX: Float; get() = (AppLoader.screenSize.screenW * 0.72f).floor()

    private var genuineSonic = false // the "NOW LOADING..." won't appear unless the arrow first run passes it  (it's totally not a GenuineIntel tho)

    private var messageBackgroundColour = Color(0x404040ff)
    private var messageForegroundColour = Color.WHITE

    override fun render(delta: Float) {
        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())

        val delta = Gdx.graphics.deltaTime

        glideDispY = AppLoader.screenSize.screenH - 100f - AppLoader.fontGame.lineHeight
        arrowObjGlideSize = arrowObjTex.width + 2f * AppLoader.screenSize.screenW



        gdxClearAndSetBlend(.094f, .094f, .094f, 0f)

        textFbo.inAction(null, null) {
            gdxClearAndSetBlend(0f, 0f, 0f, 0f)
        }

        // update arrow object
        if (!errorTrapped) {
            glideTimer += delta
            // reset timer
            if (glideTimer >= arrowObjGlideSize / arrowGlideSpeed) {
                glideTimer -= arrowObjGlideSize / arrowGlideSpeed

                // change screen WHEN the timer is reset.
                // In other words, the arrow must hit the goal BEFORE context change take place
                if (screenToLoad?.gameInitialised ?: false) {
                    doContextChange = true
                }
            }
            arrowObjPos = glideTimer * arrowGlideSpeed
        }
        else {
            genuineSonic = true
            arrowObjPos = 0f
        }


        val textToPrint = if (errorTrapped) Lang["ERROR_GENERIC_TEXT"] else Lang["MENU_IO_LOADING"]
        val textWidth = AppLoader.fontGame.getWidth(textToPrint).toFloat()

        if (!doContextChange) {
            // draw text to FBO
            textFbo.inAction(camera, AppLoader.batch) {
                AppLoader.batch.inUse {


                    blendNormal(AppLoader.batch)
                    AppLoader.fontGame
                    it.color = Color.WHITE


                    AppLoader.fontGame.draw(it, textToPrint, ((textFbo.width - textWidth) / 2).toInt().toFloat(), 0f)


                    blendMul(AppLoader.batch)
                    // draw colour overlay, flipped
                    it.draw(textOverlayTex,
                            (textFbo.width - textWidth) / 2f,
                            AppLoader.fontGame.lineHeight,
                            textWidth,
                            -AppLoader.fontGame.lineHeight
                    )
                }
            }


            AppLoader.batch.inUse {
                initViewPort(AppLoader.screenSize.screenW, AppLoader.screenSize.screenH) // dunno, no render without this
                it.projectionMatrix = camera.combined
                blendNormal(AppLoader.batch)


                // almost black background
                it.color = Color(0x181818ff)
                it.fillRect(0f, 0f, AppLoader.screenSize.screenWf, AppLoader.screenSize.screenHf)


                it.color = Color.WHITE

                // draw text FBO to screen
                val textTex = textFbo.colorBufferTexture
                textTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

                // --> original text
                if (genuineSonic) {
                    it.color = Color.WHITE
                    it.draw(textTex, textX, glideDispY - 2f)
                }

                // --> ghost
                it.color = getPulseEffCol()

                if (it.color.a != 0f) genuineSonic = true

                val drawWidth = getPulseEffWidthMul() * textTex.width
                val drawHeight = getPulseEffWidthMul() * textTex.height
                it.draw(textTex,
                        textX - (drawWidth - textTex.width) / 2f,
                        glideDispY - 2f - (drawHeight - textTex.height) / 2f,
                        drawWidth,
                        drawHeight
                )


                // draw coloured arrows
                if (!errorTrapped) {
                    arrowColours.forEachIndexed { index, color ->
                        it.color = color
                        it.draw(arrowObjTex, arrowObjPos + arrowObjGlideOffsetX + arrowObjTex.width * index, glideDispY)
                    }
                }




                // message backgrounds
                it.color = messageBackgroundColour
                it.fillRect(0f, 60f, AppLoader.screenSize.screenWf, 40f + (messages.size) * AppLoader.fontGame.lineHeight)

                // log messages
                it.color = messageForegroundColour
                messages.forEachIndexed { i, s ->
                    AppLoader.fontGame.draw(it,
                            s,
                            AppLoader.screenSize.tvSafeGraphicsWidth + 16f,
                            80f + (messages.size - i - 1) * AppLoader.fontGame.lineHeight
                    )
                }
            }
        }
        else {
            AppLoader.batch.inUse {
                // recycling part of the draw code //

                initViewPort(AppLoader.screenSize.screenW, AppLoader.screenSize.screenH) // dunno, no render without this
                it.projectionMatrix = camera.combined
                blendNormal(AppLoader.batch)



                // message backgrounds
                it.color = messageBackgroundColour
                it.fillRect(0f, 60f, AppLoader.screenSize.screenWf, 40f + (messages.size) * AppLoader.fontGame.lineHeight)

                // log messages
                it.color = messageForegroundColour
                messages.forEachIndexed { i, s ->
                    AppLoader.fontGame.draw(it,
                            s,
                            AppLoader.screenSize.tvSafeGraphicsWidth + 16f,
                            80f + (messages.size - i - 1) * AppLoader.fontGame.lineHeight
                    )
                }
            }

            AppLoader.batch.flush()
        }


        // replaces super.render()
        if (doContextChange) {
            Thread.sleep(80)
            AppLoader.setScreen(screenToLoad!!)
        }
    }

    private fun getPulseEffCol(): Color {
        if (arrowObjPos + arrowObjTex.width * 3f < textX)
            return Color(1f, 1f, 1f, 0f)
        else {
            // ref point: top-left of arrow drawn to the screen, 0 being start of the RAIL
            val scaleStart = textX - arrowObjTex.width * 3f
            val scaleEnd = arrowObjGlideSize - arrowObjTex.width * 3f
            val scale = (arrowObjPos - scaleStart) / (scaleEnd - scaleStart)

            val alpha = FastMath.interpolateLinear(scale, ghostAlphaMax, 0f)

            return Color(1f, 1f, 1f, alpha)
        }
    }

    private fun getPulseEffWidthMul(): Float {
        if (arrowObjPos + arrowObjTex.width * 3f < textX)
            return 1f
        else {
            // ref point: top-left of arrow drawn to the screen, 0 being start of the RAIL
            val scaleStart = textX - arrowObjTex.width * 3f
            val scaleEnd = arrowObjGlideSize - arrowObjTex.width * 3f
            val scale = (arrowObjPos - scaleStart) / (scaleEnd - scaleStart)

            return FastMath.interpolateLinear(scale, 1f, ghostMaxZoomX)
        }
    }

    override fun dispose() {
        arrowObjTex.dispose()
        textFbo.dispose()
        textOverlayTex.dispose()
    }

    override fun hide() {
    }
}