package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.GdxRuntimeException
import org.lwjgl.opengl.GL11

class TerrarumGLinfo {
    private var _initialised = false

    // some JVMs don't have this property, but they probably don't have "sun.misc.Unsafe" either, so it's no big issue \_(ツ)_/
    val MINIMAL_GL_VERSION = 320

    var GL_VERSION = -1; private set
        get() = if (_initialised) field else throw UninitializedPropertyAccessException()
    var GL_MAX_TEXTURE_SIZE = -1; private set
        get() = if (_initialised) field else throw UninitializedPropertyAccessException()

    fun create() {
        _initialised = true


        val glInfo = Gdx.graphics.glVersion.debugVersionString

        GL_VERSION = Gdx.graphics.glVersion.majorVersion * 100 + Gdx.graphics.glVersion.minorVersion * 10 +
                               Gdx.graphics.glVersion.releaseVersion

        println("GL_VERSION = $GL_VERSION")
        println("GL info:\n$glInfo") // debug info



        if (GL_VERSION < MINIMAL_GL_VERSION) {
            // TODO notify properly
            throw GdxRuntimeException("Graphics device not capable -- device's GL_VERSION: $GL_VERSION, required: $MINIMAL_GL_VERSION")
        }

        GL_MAX_TEXTURE_SIZE = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE)
        println("Maximum Texture Size: $GL_MAX_TEXTURE_SIZE")
    }

}