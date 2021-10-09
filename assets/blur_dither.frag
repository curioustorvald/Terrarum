/**
 * Blue Noise texture created by Christoph Peters, released under CC0
 * http://momentsingraphics.de/BlueNoise.html
 */
#version 120
#ifdef GL_ES
    precision mediump float;
#endif

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


uniform vec2 iResolution;
uniform float flip;
uniform vec2 direction;

vec4 blur(sampler2D image, vec2 uv, vec2 resolution, vec2 direction) {
    vec4 color = vec4(0.0);
    vec2 off1 = vec2(1.3846153846) * direction;
    vec2 off2 = vec2(3.2307692308) * direction;
    color += texture2D(image, uv) * 0.2270270270;
    color += texture2D(image, uv + (off1 / resolution)) * 0.3162162162;
    color += texture2D(image, uv - (off1 / resolution)) * 0.3162162162;
    color += texture2D(image, uv + (off2 / resolution)) * 0.0702702703;
    color += texture2D(image, uv - (off2 / resolution)) * 0.0702702703;
    return color;
}

void main() {
    vec2 uv = vec2(gl_FragCoord.xy / iResolution.xy);
    if (flip == 1.0) { uv.y = 1.0 - uv.y; }

    vec4 inColor = blur(u_texture, uv, iResolution.xy, direction);
    vec4 selvec = getDitherredDot(inColor);

    gl_FragColor = selvec; // quantise all four RGBA
}