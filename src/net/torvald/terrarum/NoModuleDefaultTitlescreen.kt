package net.torvald.terrarum

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.ui.Toolkit

/**
 * Created by minjaesong on 2021-12-11.
 */
class NoModuleDefaultTitlescreen(batch: SpriteBatch) : IngameInstance(batch) {

    private val wot = """No Module is currently loaded.
Please reconfigure your Load Order on:

Derzeit ist kein Modul geladen.
Bitte konfigurieren Sie Ihren Ladeauftrag neu auf:

Actualmente no hay ningún módulo cargado.
Vuelva a configurar su orden de carga en:

Moduulia ei ole ladattu tällä hetkellä.
Määritä lataustilauksesi uudelleen:

Aucun module n’est actuellement chargé.
Veuillez reconfigurer votre ordre de chargement sur :

現在ロードされたモジュールがありません。
次のパスでロードオーダーを再設定してください。

현재 불러와진 모듈이 없습니다.
다음의 경로에서 불러오기 순서를 재설정하십시오.

В настоящее время модуль не загружен.
Измените конфигурацию вашего порядка загрузки на:

ไม่มีการโหลดโมดูลในขณะนี้
โปรดกำหนดค่าคำสั่งซื้อการโหลดของคุณใหม่เมื่อ:

当前未加载任何模块。请重新配置您的加载顺序：
當前未加載任何模塊。請重新配置您的加載順序：

\c\a assets/mods/LoadOrder.csv""".split('\n')

    private val maxtw = wot.maxOf { App.fontGameFBO.getWidth(it) }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, App.scr.width, App.scr.height, true)

    private var init = false

    override fun render(updateRate: Float) {
        gdxClearAndSetBlend(0f, 0f, 0f, 0f)

        if (!init) {
            val lh = 20f
            val pbreak = 8f
            val th = lh * wot.size

            val heights = FloatArray(wot.size)

            // build y-pos map for strings
            wot.tail().forEachIndexed { index, s ->
                heights[index + 1] = heights[index] + (if (s.isBlank()) pbreak else lh)
            }

            // vertically centre the above
            val centering = (App.scr.hf - heights.last() - App.fontGameFBO.lineHeight) / 2f

            fbo.inAction(null, null) {
                gdxClearAndSetBlend(.094f, .094f, .094f, 1f)
                batch.inUse {
                    batch.color = Color.WHITE
                    wot.forEachIndexed { index, s ->
                        if (s.startsWith('\\')) {
                            val tagsSplit = s.indexOfFirst { it == ' ' }
                            val tagsBulk = s.substring(0, tagsSplit)

                            val tags = tagsBulk.split('\\').filter { it.isNotBlank() }
                            val text = s.substring(tagsSplit + 1)

                            if (tags.contains("a")) batch.color = Toolkit.Theme.COL_HIGHLIGHT else batch.color = Color.WHITE

                            if (tags.contains("c"))
                                App.fontGameFBO.draw(batch, text, (Toolkit.drawWidth - App.fontGameFBO.getWidth(text)) / 2f, heights[index] + centering)
                            else
                                App.fontGameFBO.draw(batch, text, (Toolkit.drawWidth - maxtw) / 2f, heights[index] + centering)
                        }
                        else {
                            batch.color = Color.WHITE
                            App.fontGameFBO.draw(batch, s, (Toolkit.drawWidth - maxtw) / 2f, heights[index] + centering)
                        }
                    }
                }
            }
        }

        batch.inUse {
            batch.color = Color.WHITE
            batch.draw(fbo.colorBufferTexture, 0f, 0f)
        }
    }

    override fun dispose() {
        super.dispose()

        fbo.dispose()
    }
}