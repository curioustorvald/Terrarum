/**
 * Blue Noise texture created by Christoph Peters, released under CC0
 * http://momentsingraphics.de/BlueNoise.html
 */

#version 400
#ifdef GL_ES
precision mediump float;
#endif


vec4 gammaIn(vec4 col) {
    return pow(col, vec4(2.2));
}

vec4 gammaOut(vec4 col) {
    return pow(col, vec4(1.0 / 2.2));
}

in vec4 v_color; // unused!
in vec2 v_texCoords;

uniform sampler2D u_texture;
uniform sampler2D u_pattern;
uniform ivec2 rnd = ivec2(0,0);

uniform float quant = 255.0; // 64 steps -> 63.0; 256 steps -> 255.0

const vec2 boolean = vec2(0.0, 1.0);

out vec4 fragColor;

void main(void) {
    vec4 incolour = texture(u_texture, v_texCoords);
    fragColor = fma(incolour, boolean.yyyx, boolean.xxxy); // use quantised RGB but not the A
}