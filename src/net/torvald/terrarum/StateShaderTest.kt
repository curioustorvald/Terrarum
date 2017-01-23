package net.torvald.terrarum

import net.torvald.terrarum.Terrarum.Companion.STATE_ID_TEST_SHADER
import org.lwjgl.opengl.*
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.opengl.renderer.SGL
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import shader.Shader
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.ARBShaderObjects
import com.sun.xml.internal.ws.streaming.XMLStreamReaderUtil.close
import jdk.nashorn.internal.runtime.ScriptingFunctions.readLine
import net.torvald.terrarum.gameworld.fmod
import org.newdawn.slick.Color
import org.newdawn.slick.opengl.TextureImpl
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream







/**
 * Created by SKYHi14 on 2017-01-23.
 */
class StateShaderTest : BasicGameState() {
    override fun getID() = STATE_ID_TEST_SHADER



    override fun init(container: GameContainer?, game: StateBasedGame?) {
    }

    private lateinit var shaderTest: Shader
    private lateinit var testImage: Image

    override fun enter(container: GameContainer?, game: StateBasedGame?) {
        shaderTest = Shader.makeShader("./assets/test.vert", "./assets/test.frag")

        testImage = Image("./assets/test_texture.tga")
        //testImage = Image("./logo_repository.png")
    }

    override fun update(container: GameContainer?, game: StateBasedGame?, delta: Int) {
        Terrarum.appgc.setTitle("${Terrarum.NAME} — F: ${Terrarum.appgc.fps}")
    }

    override fun render(container: GameContainer?, game: StateBasedGame?, g: Graphics?) {
        val x = 10f
        val y = 10f
        val width = testImage.width
        val height = testImage.height
        val textureWidth = testImage.textureWidth
        val textureHeight = testImage.textureHeight
        val textureOffsetX = testImage.textureOffsetX
        val textureOffsetY = testImage.textureOffsetY


        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
        // bind texture
        // glBegin(SGL.GL_QUADS)
        // do shader
        // glEnd()



        //testImage.bind()

        GL11.glBegin(GL11.GL_QUADS)

        //GL20.glUseProgram(0)
        shaderTest.startShader()
        //GL13.glActiveTexture(testImage.texture.textureID)
        //GL11.glBindTexture(GL13.GL_TEXTURE0, testImage.texture.textureID)
        //testImage.bind()
        shaderTest.setUniformIntVariable("u_texture", 0)

        /*GL11.glTexCoord2f(textureOffsetX, textureOffsetY)
        GL11.glVertex3f(x, y, 0f)
        GL11.glTexCoord2f(textureOffsetX, textureOffsetY + textureHeight)
        GL11.glVertex3f(x, y + height, 0f)
        GL11.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY + textureHeight)
        GL11.glVertex3f(x + width, y + height, 0f)
        GL11.glTexCoord2f(textureOffsetX + textureWidth, textureOffsetY)
        GL11.glVertex3f(x + width, y, 0f)*/

        g!!.color = Color.orange
        g!!.fillRect(10f, 10f, 512f, 512f)

        GL20.glUseProgram(0)
        GL11.glEnd()
    }
}