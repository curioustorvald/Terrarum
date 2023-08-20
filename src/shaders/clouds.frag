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

const vec2 boolean = vec2(0.0, 1.0);

uniform vec2 inverseGamma = vec2(0.5, 2.0); // vec2(inverse gamma RGB, inverse gamma RGA)

void main() {
    vec4 inCol = v_color * texture(u_texture, v_texCoords);

    vec4 outCol = pow(inCol, inverseGamma.xxxy);

    fragColor = outCol;
}