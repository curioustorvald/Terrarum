/**
 * Blue Noise texture created by Christoph Peters, released under CC0
 * http://momentsingraphics.de/BlueNoise.html
 */


#ifdef GL_ES
precision mediump float;
#endif


vec4 gammaIn(vec4 col) {
    return pow(col, vec4(2.2));
}

vec4 gammaOut(vec4 col) {
    return pow(col, vec4(1.0 / 2.2));
}

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;
uniform sampler2D u_pattern;
uniform ivec2 rnd = ivec2(0,0);
uniform mat4 swizzler = mat4(
1.0,0.0,0.0,0.0,
0.0,1.0,0.0,0.0,
0.0,0.0,1.0,0.0,
0.0,0.0,0.0,1.0
);
uniform float quant = 1.0; // 64 steps -> 63.0; 256 steps -> 255.0

vec4 quantVec = vec4(quant);
vec4 invQuant = vec4(1.0 / quant);

out vec4 fragColor;

const vec2 boolean = vec2(0.0, 1.0);
const vec2 patternsize = vec2(1.0/512.0, 1.0/512.0);


float nearestAlpha(float alpha) {
    return min(1.0, max(0.0, floor(alpha + 0.5)));
}

void main(void) {
    vec4 particleCol = vec4(v_color.rgb, smoothstep(0.0, 1.0, v_color.a));
    vec4 inColor = particleCol * texture(u_texture, v_texCoords);

    float bayerThreshold = (swizzler * vec4(texture(u_pattern, (gl_FragCoord.xy + rnd) * patternsize))).r;
    float alpha = nearestAlpha((inColor.a * 256.0 / 255.0) - (1.0 / 255.0) + (bayerThreshold - 0.5));

    fragColor = inColor * boolean.yyyx + alpha * boolean.xxxy;
}