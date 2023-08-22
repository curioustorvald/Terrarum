#version 400

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

in LOWP vec4 v_color; // lightCol.rgb + cloud's alpha
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

const vec2 boolean = vec2(0.0, 1.0);

uniform vec2 gamma = vec2(10, 2.0); // vec2(gamma for RGB, gamma for A)

uniform LOWP vec4 shadeCol;

void main() {
    // r: bw diffuse map, g: normal, b: normal, a: bw diffuse alpha
    vec4 inCol = texture(u_texture, v_texCoords);
    vec4 rawCol = pow(inCol, gamma.xxxy);

    // do gradient mapping here
    vec4 outCol = fma(mix(shadeCol, v_color, rawCol.r), boolean.yyyx, rawCol * boolean.xxxy);

    fragColor = outCol * fma(v_color, boolean.xxxy, boolean.yyyx);
}