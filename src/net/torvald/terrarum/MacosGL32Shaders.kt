package net.torvald.terrarum

import com.badlogic.gdx.graphics.glutils.ShaderProgram


/**
 * Created by minjaesong on 2023-02-28
 */
object MacosGL32Shaders {
    fun createSpriteBatchShader(): ShaderProgram {
        val vertexShader = """
#version 150
in vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
in vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
uniform mat4 u_projTrans;
out vec4 v_color;
out vec2 v_texCoords;

void main() {
    v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
    v_color.a = v_color.a * (255.0/254.0);
    v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;
    gl_Position =  u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
        val fragmentShader = """
#version 150
#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif
in LOWP vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

void main() {
    fragColor = v_color * texture(u_texture, v_texCoords);
}
"""
        return App.loadShaderInline(vertexShader, fragmentShader)
    }


    fun createShapeRendererShader(): ShaderProgram {
        return App.loadShaderInline(createShadeRendererVertexShader(), createShapeRendererFragmentShader())
    }


    private fun createShadeRendererVertexShader(hasNormals: Boolean = false, hasColors: Boolean = true, numTexCoords: Int = 0): String {
        var shader = ("""
    #version 150
    in vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
    
    """.trimIndent()
                + (if (hasNormals) """
     in vec3 ${ShaderProgram.NORMAL_ATTRIBUTE};
     
     """.trimIndent()
        else "")
                + if (hasColors) """
     in vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
     
     """.trimIndent()
        else "")
        for (i in 0 until numTexCoords) {
            shader += """
            in vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}$i;
            
            """.trimIndent()
        }
        shader += """
            uniform mat4 u_projModelView;
            ${if (hasColors) "in vec4 v_col;\n" else ""}
            """.trimIndent()
        for (i in 0 until numTexCoords) {
            shader += "in vec2 v_tex$i;\n"
        }
        shader += """void main() {
   gl_Position = u_projModelView * ${ShaderProgram.POSITION_ATTRIBUTE};
"""
        if (hasColors) {
            shader += """   v_col = ${ShaderProgram.COLOR_ATTRIBUTE};
   v_col.a *= 255.0 / 254.0;
"""
        }
        for (i in 0 until numTexCoords) {
            shader += """   v_tex$i = ${ShaderProgram.TEXCOORD_ATTRIBUTE}$i;
"""
        }
        shader += """   gl_PointSize = 1.0;
}
"""
        return shader
    }

    private fun createShapeRendererFragmentShader(hasNormals: Boolean = false, hasColors: Boolean = true, numTexCoords: Int = 0): String {
        var shader = """
            #version 150
            #ifdef GL_ES
            precision mediump float;
            #endif
            
            """.trimIndent()
        if (hasColors) shader += "in vec4 v_col;\n"
        for (i in 0 until numTexCoords) {
            shader += "in vec2 v_tex$i;\n"
            shader += "uniform sampler2D u_sampler$i;\n"
        }
        shader += """void main() {
   gl_FragColor = ${if (hasColors) "v_col" else "vec4(1, 1, 1, 1)"}"""
        if (numTexCoords > 0) shader += " * "
        for (i in 0 until numTexCoords) {
            shader += if (i == numTexCoords - 1) {
                " texture2D(u_sampler$i,  v_tex$i)"
            }
            else {
                " texture2D(u_sampler$i,  v_tex$i) *"
            }
        }
        shader += ";\n}"
        return shader
    }
    
}