package net.torvald.terrarum

import com.badlogic.gdx.graphics.glutils.ShaderProgram


/**
 * Created by minjaesong on 2023-02-28
 */
object MacosGL32Shaders {
    fun createSpriteBatchShader(): ShaderProgram {
        return App.loadShaderFromClasspath("shaders/gl32spritebatch.vert", "shaders/gl32spritebatch.frag")
    }

    fun createShapeRendererShader(): ShaderProgram {
        return App.loadShaderFromClasspath("shaders/gl32shaperenderer.vert", "shaders/gl32shaperenderer.frag")
    }
    
}