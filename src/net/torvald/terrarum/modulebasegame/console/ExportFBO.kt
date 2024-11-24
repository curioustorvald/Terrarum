package net.torvald.terrarum.modulebasegame.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.glutils.Float16FrameBuffer
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.BufferUtils
import net.torvald.reflection.extortField
import net.torvald.terrarum.App
import net.torvald.terrarum.ccG
import net.torvald.terrarum.ccO
import net.torvald.terrarum.ccW
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.unicode.BULLET
import net.torvald.unicode.EMDASH
import java.nio.ByteBuffer
import java.util.zip.Deflater
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation


/**
 * Created by minjaesong on 2024-11-24.
 */
internal object ExportFBO : ConsoleCommand {

    private val cmds: HashMap<String, KFunction<*>> = HashMap()
    private val helpMessages: List<Pair<String, String>>

    init {
        val helpMsgs: HashMap<String, String> = HashMap()

        ExportFBO::class.declaredFunctions.filter { it.hasAnnotation<ExportFBOCmd>() }.forEach {
            cmds[it.name.lowercase()] = it
            helpMsgs[it.name.lowercase()] = it.findAnnotation<ExportFBOCmd>()!!.description
        }

        helpMessages = helpMsgs.keys.toList().sorted().map {
            it to helpMsgs[it]!!
        }
    }

    override fun execute(args: Array<String>) {
        if (args.size != 3) { printUsage(); return }
        val fn = cmds[args[1].lowercase()]
        if (fn == null) { printUsage(); return }

        try {
            val filename = "${args[2]}-${args[1]}.png"
            val fileHandle = Gdx.files.absolute("${App.defaultDir}/Exports/$filename")

            val fbo = fn.call(this) as FrameBuffer

            // cannot use createFromFrameBuffer because the specific FBO must be bound in a way we can control
            val pixels: ByteBuffer = BufferUtils.newByteBuffer(fbo.width * fbo.height * 4)
            Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fbo.framebufferHandle)
            Gdx.gl.glReadPixels(0, 0, fbo.width, fbo.height, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, pixels)
            Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0)

            val pixmap: Pixmap = Pixmap(fbo.width, fbo.height, Pixmap.Format.RGBA8888)
            BufferUtils.copy(pixels, pixmap.pixels, pixels.capacity())

            PixmapIO.writePNG(fileHandle, pixmap, Deflater.DEFAULT_COMPRESSION, true)

            Echo("Framebuffer exported to$ccG Exports/$filename")
        }
        catch (e: Throwable) {
            EchoError("Could not retrieve the framebuffer: ${e.message}")
            System.err.println(e)
            return
        }
    }

    override fun printUsage() {
        Echo("Usage: exportfbo <identifier> <filename without extension>")
        Echo("Available identifiers are:")

        helpMessages.forEach { (name, desc) ->
            Echo("  $BULLET $ccG$name $ccW$EMDASH $ccO$desc")
        }
    }

    @ExportFBOCmd("Main RGB channel of the IngameRenderer without lighting")
    fun fborgb(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboRGB")!!
    }

    @ExportFBOCmd("Main A channel of the IngameRenderer without lighting")
    fun fboa(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboA")!!
    }

    @ExportFBOCmd("Main Emissive channel of the IngameRenderer without lighting")
    fun fboemissive(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboEmissive")!!
    }

    @ExportFBOCmd("Framebuffer for render-behind actors used for creating shallow shadow effects")
    fun fborgbactorsbehind(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboRGBactorsBehind")!!
    }

    @ExportFBOCmd("Framebuffer for render-middle actors used for creating large shadow effects")
    fun fborgbactorsmiddle(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboRGBactorsMiddle")!!
    }

    @ExportFBOCmd("Framebuffer for terrain blocks used for creating large shadow effects")
    fun fborgbterrain(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboRGBterrain")!!
    }

    @ExportFBOCmd("Framebuffer for render-behind actors used for creating shallow shadow effects")
    fun fborgbactorsbehindshadow(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboRGBactorsBehindShadow")!!
    }

    @ExportFBOCmd("Framebuffer for render-middle actors used for creating large shadow effects")
    fun fborgbactorsmiddleshadow(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboRGBactorsMiddleShadow")!!
    }

    @ExportFBOCmd("Framebuffer for terrain blocks used for creating large shadow effects")
    fun fborgbterrainshadow(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboRGBterrainShadow")!!
    }

    @ExportFBOCmd("Framebuffer for wall blocks")
    fun fborgbwall(): FrameBuffer {
        return IngameRenderer.extortField<Float16FrameBuffer>("fboRGBwall")!!
    }
}

internal annotation class ExportFBOCmd(val description: String)