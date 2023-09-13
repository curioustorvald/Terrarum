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
const mat4 swizzler = mat4(
1.0,0.0,0.0,0.0,
0.0,1.0,0.0,0.0,
0.0,0.0,1.0,0.0,
0.0,0.0,0.0,1.0
);
uniform float quant = 255.0; // 64 steps -> 63.0; 256 steps -> 255.0

vec4 quantVec = vec4(quant);
vec4 invQuant = vec4(1.0 / quant);

out vec4 fragColor;

const vec2 boolean = vec2(0.0, 1.0);
const vec4 matrixNormaliser = vec4(0.5 / 256.0);

const vec2 patternsize = vec2(1.0/512.0, 1.0/512.0);


vec4 nearestColour(vec4 inColor) {
    return floor(quantVec * inColor) * invQuant;
}

vec4 getDitherredDot(vec4 inColor) {
    vec4 bayerThreshold = swizzler * vec4(matrixNormaliser + texture(u_pattern, (gl_FragCoord.xy + rnd) * patternsize));
    return nearestColour(fma(bayerThreshold, invQuant, inColor));
}

void main(void) {
    vec4 incolour = texture(u_texture, v_texCoords);
    vec4 selvec = getDitherredDot(incolour);
    vec4 outcol = fma(selvec, boolean.yyyx, boolean.xxxy); // use quantised RGB but not the A

    fragColor = outcol;
//    ivec4 bytes = ivec4(255.0 * outcol);
//    ivec4 mask = ivec4(0x55);
//    fragColor = (bytes ^ mask) / 255.0;
}