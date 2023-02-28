#version 150
#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture; // world texture, has alpha value that is meaningful

uniform sampler2D tex1; // glow texture, SHOULD contain alpha of all 1.0
out vec4 fragColor;

void main(void) {
    vec4 colorTex0 = texture2D(u_texture, v_texCoords); // lightmap (RGB) pre-mixed
    vec4 colorTex1 = texture2D(tex1, v_texCoords); // lightmap (A) pre-mixed

    fragColor = vec4(max(colorTex0.rgb, colorTex1.rgb), colorTex0.a);
}