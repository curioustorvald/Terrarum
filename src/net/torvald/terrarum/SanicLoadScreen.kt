package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.jme3.math.FastMath
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.max

/**
 * Created by minjaesong on 2017-07-13.
 */
object SanicLoadScreen : LoadScreenBase() {

    init {
        App.disposables.add(this)
    }

    private var arrowObjPos = 0f // 0 means at starting position, regardless of screen position
    private var arrowObjGlideOffsetX = 0f
    private var arrowObjGlideSize = 0f
    private val arrowGlideSpeed: Float; get() = App.scr.width * 2f // pixels per sec
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
                max(
                        App.fontGame.getWidth(Lang["MENU_IO_LOADING"]),
                        App.fontGame.getWidth(Lang["ERROR_GENERIC_TEXT"])
                ),
                App.fontGame.lineHeight.toInt(),
                false
        )

        arrowObjTex = Texture(Gdx.files.internal("assets/graphics/test_loading_arrow_atlas.tga"))
        arrowObjGlideOffsetX = -arrowObjTex.width.toFloat()

        textOverlayTex = Texture(Gdx.files.internal("assets/graphics/test_loading_text_tint.tga"))
    }


    val textX: Float; get() = (App.scr.width * 0.72f).floorToFloat()

    private var genuineSonic = false // the "NOW LOADING..." won't appear unless the arrow first run passes it

    private var messageBackgroundColour = Color(0x404040ff)
    private var messageForegroundColour = Color.WHITE

    override fun render(delta: Float) {
        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())

        val delta = Gdx.graphics.deltaTime

        glideDispY = App.scr.height - 100f - App.fontGame.lineHeight
        arrowObjGlideSize = arrowObjTex.width + 2f * App.scr.width



        gdxClearAndEnableBlend(.063f, .070f, .086f, 1f)

        textFbo.inAction(null, null) {
            gdxClearAndEnableBlend(0f, 0f, 0f, 0f)
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
        val textWidth = App.fontGame.getWidth(textToPrint).toFloat()

        if (!doContextChange) {
            // draw text to FBO
            textFbo.inAction(camera, App.batch) {
                App.batch.inUse {


                    blendNormalStraightAlpha(App.batch)
                    App.fontGame
                    it.color = Color.WHITE


                    App.fontGame.draw(it, textToPrint, ((textFbo.width - textWidth) / 2).toInt().toFloat(), 0f)


                    blendMul(App.batch)
                    // draw colour overlay, flipped
                    it.draw(textOverlayTex,
                            (textFbo.width - textWidth) / 2f,
                            0f,
                            textWidth,
                            App.fontGame.lineHeight
                    )
                }
            }


            App.batch.inUse {
                initViewPort(App.scr.width, App.scr.height) // dunno, no render without this
                it.projectionMatrix = camera.combined
                blendNormalStraightAlpha(App.batch)


                // almost black background
                it.color = Color(0x181818ff)
                Toolkit.fillArea(it, 0, 0, App.scr.width, App.scr.height)


                it.color = Color.WHITE

                // draw text FBO to screen
                val textTex = textFbo.colorBufferTexture
                textTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

                // --> original text
                if (genuineSonic) {
                    it.color = Color.WHITE
                    it.draw(textTex, textX, glideDispY - 2f + textTex.height, textTex.width.toFloat(), -textTex.height.toFloat())
                }

                // --> ghost
                it.color = getPulseEffCol()

                if (it.color.a != 0f) genuineSonic = true

                val drawWidth = getPulseEffWidthMul() * textTex.width
                val drawHeight = getPulseEffWidthMul() * textTex.height
                it.draw(textTex,
                        textX - (drawWidth - textTex.width) / 2f,
                        glideDispY - 2f - (drawHeight - textTex.height) / 2f + drawHeight,
                        drawWidth,
                        -drawHeight
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
                Toolkit.fillArea(it, 0, 60, App.scr.width, 40 + (messages.size) * App.fontGame.lineHeight.toInt())

                // log messages
                it.color = messageForegroundColour
                messages.reversed().forEachIndexed { i, s ->
                    App.fontGame.draw(it,
                            s,
                            App.scr.tvSafeGraphicsWidth + 16f,
                            80f + (messages.size - i - 1) * App.fontGame.lineHeight
                    )
                }
            }
        }
        else {
            App.batch.inUse {
                // recycling part of the draw code //

                initViewPort(App.scr.width, App.scr.height) // dunno, no render without this
                it.projectionMatrix = camera.combined
                blendNormalStraightAlpha(App.batch)



                // message backgrounds
                it.color = messageBackgroundColour
                Toolkit.fillArea(it, 0, 60, App.scr.width, 40 + (messages.size) * App.fontGame.lineHeight.toInt())

                // log messages
                it.color = messageForegroundColour
                messages.reversed().forEachIndexed { i, s ->
                    App.fontGame.draw(it,
                            s,
                            App.scr.tvSafeGraphicsWidth + 16f,
                            80f + (messages.size - i - 1) * App.fontGame.lineHeight
                    )
                }
            }

            App.batch.flush()
        }


        // replaces super.render()
        if (doContextChange) {
            Thread.sleep(80)
            App.setScreen(screenToLoad!!)
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