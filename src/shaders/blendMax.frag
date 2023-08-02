#version 150
#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoord0;
in vec2 v_texCoord1;
uniform sampler2D u_texture; // world texture, has alpha value that is meaningful

uniform sampler2D tex1; // glow texture, SHOULD contain alpha of all 1.0
out vec4 fragColor;

vec2 boolean = vec2(0.0, 1.0);

void main(void) {
    vec4 colorTex0 = texture(u_texture, v_texCoord0); // lightmap (RGB) pre-mixed
    vec4 colorTex1 = texture(tex1, v_texCoord1); // lightmap (A) pre-mixed
//    fragColor = (max(colorTex0, colorTex1) * boolean.yyyx) + boolean.xxxy;
    fragColor = colorTex0;
}