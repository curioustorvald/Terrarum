/**
 * Blue Noise texture created by Christoph Peters, released under CC0
 * http://momentsingraphics.de/BlueNoise.html
 */

#version 120
#ifdef GL_ES
    precision mediump float;
#endif


vec4 gammaIn(vec4 col) {
    return pow(col, vec4(2.2));
}

vec4 gammaOut(vec4 col) {
    return pow(col, vec4(1.0 / 2.2));
}

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_pattern;

float quant = 63.0; // 64 steps -> 63.0; 256 steps -> 255.0
vec4 quantiser = vec4(quant);
vec4 quantiserDivider = vec4(1.0 / quant);

vec2 boolean = vec2(0.0, 1.0);
vec4 halfvec = vec4(0.5);

vec2 patternsize = vec2(1.0/512.0, 1.0/512.0);

vec4 nearestColour(vec4 inColor) {
    return floor(quantiser * inColor + halfvec) * quantiserDivider;
}

vec4 getDitherredDot(vec4 inColor) {
    vec4 bayerThreshold = vec4(texture2D(u_pattern, gl_FragCoord.xy * patternsize) - 0.5);
    return nearestColour(inColor + bayerThreshold * quantiserDivider);
}


void main(void) {
    // create texture coordinates based on pixelSize //
    vec4 inColor = v_color * texture2D(u_texture, v_texCoords);
    vec4 selvec = getDitherredDot(inColor);

//    gl_FragColor = inColor * boolean.yyyx + boolean.xxxy;
    gl_FragColor = selvec * boolean.yyyx + inColor * boolean.xxxy; // use quantised RGB but not the A
}